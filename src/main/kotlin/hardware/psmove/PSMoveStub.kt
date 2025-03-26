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
                delay(50);
                buttonObserverTicker.start()
            }
        }

    val getTriggerClickFlow: Flow<Unit> = _buttonClickFlow.filter {
        it.contains(
            PSMoveButton.TRIGGER
        )
    }.map { }


    fun setCurrentColor(colorToSet: MoveColor) {
        PSMoveApi.setColor(macAddress = this.macAddress, colorToSet = colorToSet)
    }

    fun setNotActivatedInLobbyColor() {
        PSMoveApi.setColor(macAddress = this.macAddress, colorToSet = MoveColor.ORANGE_INACTIVE)
    }

}