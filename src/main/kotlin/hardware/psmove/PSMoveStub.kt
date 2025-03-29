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
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds

class PSMoveStub(val macAddress: MacAddress) {
    private val logger = KotlinLogging.logger {}
    private val buttonObserverTicker = Ticker(5.milliseconds)

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

    fun setCurrentColor(colorToSet: MoveColor) {
        PSMoveApi.setColor(macAddress = this.macAddress, colorToSet = colorToSet)
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