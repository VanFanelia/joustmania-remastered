package de.vanfanel.joustmania.hardware

import de.vanfanel.joustmania.hardware.PSMoveButton.Companion.calculatedPressedButtons
import de.vanfanel.joustmania.types.MacAddress
import de.vanfanel.joustmania.types.Ticker
import io.github.oshai.kotlinlogging.KotlinLogging
import io.thp.psmove.PSMove
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds


enum class PSMoveButton(val rawValue: Int = 0) {
    TRIANGLE (rawValue = 1 shl 4),
    CIRCLE(rawValue = 1 shl 5),
    CROSS(rawValue = 1 shl 6),
    SQUARE(rawValue = 1 shl 7),
    SELECT(rawValue = 1 shl 8),
    START(rawValue = 1 shl 11),
    PLAYSTATION(rawValue = 1 shl 16),
    MOVE_MENU(rawValue = 1 shl 19),
    TRIGGER(rawValue = 1 shl 20);

    companion object {
        private val simpleButtons = setOf(TRIANGLE, CIRCLE, CROSS, SQUARE, SELECT, START, PLAYSTATION, MOVE_MENU)

        fun calculatedPressedButtons(buttonState: Int, triggerValue: Int): Set<PSMoveButton> {
            val buttons = mutableSetOf<PSMoveButton>()

            for (button in simpleButtons) {
                if ((buttonState and button.rawValue) != 0) {
                    buttons.add(button)
                }
            }

            if ((buttonState and TRIGGER.rawValue) != 0 && triggerValue >= 128) {
                buttons.add(TRIGGER)
            }

            return buttons
        }
    }

}

/**
 * Object sends commands to every connected PSMove Controller
 */
@OptIn(ExperimentalCoroutinesApi::class)
object PSMoveButtonObserver {
    private val logger = KotlinLogging.logger {}
    private val buttonObserverTicker = Ticker(5.milliseconds)

    fun startButtonObserver() {
        buttonObserverTicker.start()
    }

    fun stopButtonObserver() {
        buttonObserverTicker.stop()
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            buttonObserverTicker.start()
            buttonObserverTicker.tick.collect {
                PSMoveBluetoothConnectionWatcher.getCurrentConnectedPSMove().map { move ->
                    val poll = move.poll()
                    if (poll > 0) {
                        val buttons = move._buttons
                        val trigger = move._trigger
                        val buttonPressed = calculatedPressedButtons(buttons, trigger)

                        (getPressedButtonFLow(move.getMacAddress())).tryEmit(buttonPressed)
                    }
                }
            }
        }

        // observer Buttons for buttonClickEvents
        CoroutineScope(Dispatchers.IO).launch {

            PSMoveBluetoothConnectionWatcher.bluetoothConnectedPSMoves.flatMapLatest { newMoves ->
                newMoves.asFlow().flatMapMerge { move ->
                    move.buttonPressFlow.map { buttons -> Pair(move, buttons) }
                        .onStart { buttonsWithPressedState[move.getMacAddress()] = mutableSetOf() }
                        .onCompletion { buttonsWithPressedState[move.getMacAddress()]?.clear() }
                }
            }.collect {
                val oldButtonPressList = buttonsWithPressedState[it.first.getMacAddress()] ?: return@collect
                val newButtonPressList = it.second
                oldButtonPressList.addAll(it.second)
                //check if released
                val buttonsReleased = oldButtonPressList - newButtonPressList
                if (buttonsReleased.isNotEmpty()) {
                    getClickButtonFlow(it.first.getMacAddress()).tryEmit(buttonsReleased)
                    oldButtonPressList.removeAll(buttonsReleased)
                }
            }
        }

    }
}

private val buttonsWithPressedState: MutableMap<MacAddress, MutableSet<PSMoveButton>> = mutableMapOf()
private val buttonPressedFlowMap: MutableMap<MacAddress, MutableSharedFlow<Set<PSMoveButton>>> = mutableMapOf()
private val buttonClickFlowMap: MutableMap<MacAddress, MutableSharedFlow<Set<PSMoveButton>>> = mutableMapOf()

private fun getPressedButtonFLow(address: MacAddress): MutableSharedFlow<Set<PSMoveButton>> {
    if (buttonPressedFlowMap.containsKey(address)) {
        return buttonPressedFlowMap[address]!!
    } else {
        buttonPressedFlowMap[address] = MutableSharedFlow(
            replay = 0,
            extraBufferCapacity = 1
        )
        return buttonPressedFlowMap[address]!!
    }
}

private fun getClickButtonFlow(address: MacAddress): MutableSharedFlow<Set<PSMoveButton>> {
    if (buttonClickFlowMap.containsKey(address)) {
        return buttonClickFlowMap[address]!!
    } else {
        buttonClickFlowMap[address] = MutableSharedFlow(
            replay = 0,
            extraBufferCapacity = 1
        )
        return buttonClickFlowMap[address]!!
    }
}

val PSMove.buttonPressFlow: Flow<Set<PSMoveButton>>
    get() {
        return getPressedButtonFLow(this.getMacAddress()).distinctUntilChanged()
    }

fun PSMove.getTriggerClickFlow(): Flow<Unit> = getClickButtonFlow(this.getMacAddress()).filter { it.contains(PSMoveButton.TRIGGER) }.map { }