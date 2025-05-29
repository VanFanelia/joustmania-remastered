package de.vanfanel.joustmania.hardware.psmove

import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.types.MoveColor
import de.vanfanel.joustmania.types.PSMoveBatteryLevel
import de.vanfanel.joustmania.types.Ticker
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.time.Duration.Companion.seconds

data class ColorAnimation(
    val colorToSet: List<MoveColor>,
    val durationInMS: Long,
    val loop: Boolean = true
) {
    fun calculateNextColor(elapsedTime: Long): MoveColor {
        if (colorToSet.isEmpty()) {
            return MoveColor.BLACK
        }

        if (colorToSet.size == 1) {
            return colorToSet.first()
        }

        val relativeElapsedTime = elapsedTime % durationInMS
        val percentOfTime: Float = relativeElapsedTime.toFloat() / durationInMS.toFloat()
        val positionModifier = if (loop) 0 else 1
        val position: Float = percentOfTime * (colorToSet.size.toFloat() - positionModifier)
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
    private val movementObserverTicker = Ticker(5.milliseconds)
    private val batteryLevelTicker = Ticker(60.seconds)
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

    private val _accelerationFlow: MutableSharedFlow<RawMovingData> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = 1
    )

    private val _batteryLevelFlow: MutableSharedFlow<PSMoveBatteryLevel?> = MutableStateFlow(null)
    val batteryLevelFlow: Flow<PSMoveBatteryLevel?> = _batteryLevelFlow


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
            var oldChange = 0.0
            movementObserverTicker.tick.collect {
                try {
                    val lastMovement = PSMoveApi.pollMovement(macAddress, oldChange) ?: return@collect
                    oldChange = lastMovement.change
                    _accelerationFlow.tryEmit(lastMovement)
                } catch (e: MoveNotFoundException) {
                    logger.error(e) { "Could not poll move buttons for $macAddress. start cancel stub" }
                    cancel()
                }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            batteryLevelTicker.tick.collect {
                try {
                    checkBatteryLevel()
                } catch (e: MoveNotFoundException) {
                    logger.error(e) { "Could not get battery state of move controller" }
                    cancel()
                }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            colorChangeTicker.tick.collect {
                try {
                    colorAnimation?.let { animation ->
                        if (!animation.loop && animationStarted > 0 && Instant.now()
                                .toEpochMilli() > (animationStarted + animation.durationInMS)
                        ) {
                            clearAnimation()
                            return@collect
                        }
                        if (lastTick == 0L) {
                            setCurrentColor(
                                colorToSet = animation.colorToSet.firstOrNull() ?: MoveColor.BLACK,
                                clearAnimation = false
                            )
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

        batteryLevelTicker.start()
        CoroutineScope(Dispatchers.IO).launch {
            checkBatteryLevel()
        }
    }

    val buttonPressFlow: Flow<Set<PSMoveButton>>
        get() {
            return _buttonPressedFlow.distinctUntilChanged().onStart {
                delay(50)
                buttonObserverTicker.start()
                colorChangeTicker.start()
                movementObserverTicker.start()
            }
        }

    val accelerationFlow: Flow<RawMovingData> = _accelerationFlow.onStart {
        delay(50)
        colorChangeTicker.start()
        movementObserverTicker.start()
    }

    val getTriggerClickFlow: Flow<Unit> = _buttonClickFlow.filter {
        it.contains(
            PSMoveButton.TRIGGER
        )
    }.map { }

    private suspend fun checkBatteryLevel() {
        val batteryLevel = PSMoveApi.getBatteryLevel(macAddress = macAddress)
        _batteryLevelFlow.tryEmit(batteryLevel)
    }

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

    fun clearAnimation() {
        colorAnimation = null
        lastTick = 0L
        animationStarted = 0L
    }

    fun setCurrentColor(colorToSet: MoveColor, clearAnimation: Boolean = true) {
        if (clearAnimation) {
            clearAnimation()
        }
        PSMoveApi.setColor(macAddress = this.macAddress, colorToSet = colorToSet)
    }

    fun setColorAnimation(animation: ColorAnimation) {
        clearAnimation()
        PSMoveApi.setColor(
            macAddress = this.macAddress,
            colorToSet = animation.colorToSet.firstOrNull() ?: MoveColor.BLACK
        )
        colorAnimation = animation
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