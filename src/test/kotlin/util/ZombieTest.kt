package util

import de.vanfanel.joustmania.games.Zombie
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ZombieTest {
    @Test
    fun `test getMaxAmountOfZombies for 1 to 20 players`() {
        val expected = mapOf(
            1 to 1, 2 to 1, 3 to 1, 4 to 1, 5 to 1,
            6 to 2, 7 to 2, 8 to 2, 9 to 2,
            10 to 3, 11 to 3, 12 to 3, 13 to 3, 14 to 3,
            15 to 4, 16 to 4, 17 to 4, 18 to 4, 19 to 4,
            20 to 5, 21 to 5
        )
        for (players in 1..21) {
            val result = Zombie.getMaxAmountOfZombies(players)
            assertEquals(expected[players], result, "Fehler bei Spieleranzahl $players")
        }
    }

    @Test
    fun `test getMinAmountOfZombies for 1 to 21 players`() {
        val expected = mapOf(
            1 to 1, 2 to 1, 3 to 1, 4 to 1, 5 to 1,
            6 to 1, 7 to 1, 8 to 1, 9 to 1,
            10 to 2, 11 to 2, 12 to 2, 13 to 2, 14 to 2,
            15 to 2, 16 to 2, 17 to 2, 18 to 2, 19 to 2,
            20 to 3, 21 to 3
        )
        for (players in 1..21) {
            val result = Zombie.getMinAmountOfZombies(players)
            assertEquals(expected[players], result, "Fehler bei Spieleranzahl $players")
        }
    }
}