package de.vanfanel.joustmania.types

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable

private val logger = KotlinLogging.logger {}

data class SensibilitySettings(
    val warningThreshold: Float,
    val deathThreshold: Float
)

@Serializable
enum class Language {
    EN,
    DE;

    companion object {
        fun parseLanguage(value: String?): Language? {
            try {
                return valueOf(value?.uppercase() ?: "")
            } catch (e: IllegalArgumentException) {
                logger.error(e) { "Ignore unknown enum value with length: ${value?.length}" }
                return null
            }
        }
    }
}

@Serializable
enum class Sensibility {
    VERY_LOW,
    LOW,
    MEDIUM,
    HIGH,
    VERY_HIGH;

    fun getSensibilityValues(): SensibilitySettings {
        return when (this) {
            VERY_LOW -> SensibilitySettings(warningThreshold = 1.7f, deathThreshold = 2.0f)
            LOW -> SensibilitySettings(warningThreshold = 1.6f, deathThreshold = 1.9f)
            MEDIUM -> SensibilitySettings(warningThreshold = 1.5f, deathThreshold = 1.8f)
            HIGH -> SensibilitySettings(warningThreshold = 1.4f, deathThreshold = 1.7f)
            VERY_HIGH -> SensibilitySettings(warningThreshold = 1.3f, deathThreshold = 1.6f)
        }
    }

    companion object {
        fun parseSensibility(value: String?): Sensibility? {
            try {
                return valueOf(value?.uppercase() ?: "")
            } catch (e: IllegalArgumentException) {
                logger.error(e) { "Ignore unknown enum value with length: ${value?.length}" }
                return null
            }
        }
    }
}

@Serializable
data class SortToddlerGameOptions(
    val roundDuration: Int,
    val amountOfRounds: Int
)

@Serializable
data class Config(
    val sensibility: Sensibility,
    val language: Language,
    val enableAP: Boolean,
    val sortToddlerGameOptions: SortToddlerGameOptions
) {
    companion object {
        val DEFAULT_CONFIG = Config(
            sensibility = Sensibility.MEDIUM,
            language = Language.EN,
            enableAP = false,
            sortToddlerGameOptions = SortToddlerGameOptions(roundDuration = 30, amountOfRounds = 10)
        )
    }
}