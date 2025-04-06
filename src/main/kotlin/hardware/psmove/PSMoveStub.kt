package de.vanfanel.joustmania.hardware.psmove

import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.types.MoveColor
import de.vanfanel.joustmania.types.Ticker
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Date
import kotlin.math.floor
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

data class ColorAnimation(
    val colorToSet: List<MoveColor>,
    val durationInMS: Long
) {
    fun calculateNextColor(elapsedTime: Long): MoveColor {
        val relativeElapsedTime = elapsedTime % durationInMS
        val percentOfTime: Float = relativeElapsedTime.toFloat() / durationInMS.toFloat()
        val position: Float = percentOfTime * colorToSet.size.toFloat()
        val lowerIndex = floor(position).toInt()
        val upperIndex = (lowerIndex + 1) % colorToSet.size
        val upperWeight: Float = position - lowerIndex

        val redChange: Int = colorToSet[upperIndex].red - colorToSet[lowerIndex].red
        val newRed = (colorToSet[lowerIndex].red + floor(redChange * upperWeight).toInt()).min(255)

        val greenChange: Int = colorToSet[upperIndex].green - colorToSet[lowerIndex].green
        val newGreen = (colorToSet[lowerIndex].green + floor(greenChange * upperWeight).toInt()).min(255)

        val blueChange: Int = colorToSet[upperIndex].blue - colorToSet[lowerIndex].blue
        val newBlue = (colorToSet[lowerIndex].blue + floor(blueChange * upperWeight).toInt()).min(255)

        return MoveColor(red = newRed, green = newGreen, blue = newBlue)
    }
}

private fun Int.min(min: Int): Int {
    return min(this, min)
}

class PSMoveStub(val macAddress: MacAddress) {
    private val logger = KotlinLogging.logger {}
    private val buttonObserverTicker = Ticker(5.milliseconds)
    private val colorChangeTicker = Ticker(50.milliseconds)
    private var colorAnimation: ColorAnimation? = null
    private var animationStarted: Long = 0
    private var lastTick: Long = 0

    private val buttonsWithPressedState: MutableSet<PSMoveButton> = mutableSetOf()
    private val _buttonPressedFlow: MutableSharedFlow<Set<PSMoveButton>> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = 1
    )
    private val _buttonClickFlow: MutableSharedFlow<Set<PSMoveButton>> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = 1
    )

    init {
        CoroutineScope(Dispatchers.IO).launch {
            buttonObserverTicker.tick.collect {
                try {
                    val buttonsPressed = PSMoveApi.pollMoveButtons(macAddress)
                    if (buttonsPressed != null) {
                        _buttonPressedFlow.tryEmit(buttonsPressed)
                    }
                } catch (e: MoveNotFoundException) {
                    logger.error(e) { "Could not poll move buttons for $macAddress. start cancel stub" }
                    cancel()
                }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            colorChangeTicker.tick.collect {
                try {
                    colorAnimation?.let { animation ->
                        if (lastTick == 0L) {
                            setCurrentColor(colorToSet = animation.colorToSet.firstOrNull() ?: MoveColor.BLACK, clearAnimation = false)
                            val now = Instant.now().toEpochMilli()
                            lastTick = now
                            animationStarted = now
                        } else {
                            val color = animation.calculateNextColor(elapsedTime = lastTick - animationStarted)
                            setCurrentColor(colorToSet = color, clearAnimation = false)
                            lastTick = Instant.now().toEpochMilli()
                        }
                    }
                } catch (e: MoveNotFoundException) {
                    logger.error(e) { "Could not set color animation for $macAddress. start cancel stub" }
                    cancel()
                }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            _buttonPressedFlow.collect {
                val oldButtonPressList = buttonsWithPressedState
                val newButtonPressList = it
                oldButtonPressList.addAll(it)
                //check if released
                val buttonsReleased = oldButtonPressList - newButtonPressList
                if (buttonsReleased.isNotEmpty()) {
                    _buttonClickFlow.tryEmit(buttonsReleased)
                    oldButtonPressList.removeAll(buttonsReleased)
                }
            }
        }
    }

    val buttonPressFlow: Flow<Set<PSMoveButton>>
        get() {
            return _buttonPressedFlow.distinctUntilChanged().onStart {
                delay(50)
                buttonObserverTicker.start()
                colorChangeTicker.start()
            }
        }

    val getTriggerClickFlow: Flow<Unit> = _buttonClickFlow.filter {
        it.contains(
            PSMoveButton.TRIGGER
        )
    }.map { }

    private val lasClicksTimestamps: MutableMap<PSMoveButton, Long> = mutableMapOf(
        PSMoveButton.SQUARE to 0L,
        PSMoveButton.CROSS to 0L,
        PSMoveButton.TRIANGLE to 0L,
        PSMoveButton.CIRCLE to 0L,
        PSMoveButton.MOVE_MENU to 0L,
        PSMoveButton.PLAYSTATION to 0L,
        PSMoveButton.START to 0L,
        PSMoveButton.SELECT to 0L,
        PSMoveButton.TRIANGLE to 0L,
    )

    val getSquareCrossTriangleCircleClickFlow: Flow<Unit> = _buttonClickFlow.filter { buttonSet ->
        val now = Date().time
        buttonSet.forEach { button ->
            lasClicksTimestamps[button] = now
        }

        return@filter (now - (lasClicksTimestamps[PSMoveButton.SQUARE] ?: 0)) < 200 &&
                (now - (lasClicksTimestamps[PSMoveButton.CROSS] ?: 0)) < 200 &&
                (now - (lasClicksTimestamps[PSMoveButton.TRIANGLE] ?: 0)) < 200 &&
                (now - (lasClicksTimestamps[PSMoveButton.CIRCLE] ?: 0)) < 200
    }.map { }

    fun setCurrentColor(colorToSet: MoveColor, clearAnimation: Boolean = true) {
        if (clearAnimation) {
            colorAnimation = null
            lastTick = 0L
        }
        PSMoveApi.setColor(macAddress = this.macAddress, colorToSet = colorToSet)
    }

    fun setColorAnimation(animation: ColorAnimation) {
        colorAnimation = animation
        lastTick = 0L
    }

    fun setNotActivatedInLobbyColor() {
        PSMoveApi.setColor(macAddress = this.macAddress, colorToSet = MoveColor.ORANGE_INACTIVE)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        return this.macAddress == (other as PSMoveStub).macAddress
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}