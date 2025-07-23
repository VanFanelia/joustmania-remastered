package de.vanfanel.joustmania.types

/**
 * Represents the buttons available on a PlayStation Move controller.
 */
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