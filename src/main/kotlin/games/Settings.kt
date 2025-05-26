package de.vanfanel.joustmania.games

import de.vanfanel.joustmania.games.Config.Companion.DEFAULT_CONFIG
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

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
                return Language.valueOf(value?.uppercase() ?: "")
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
                return Sensibility.valueOf(value?.uppercase() ?: "")
            } catch (e: IllegalArgumentException) {
                logger.error(e) { "Ignore unknown enum value with length: ${value?.length}" }
                return null
            }
        }
    }
}

@Serializable
data class Config(
    val sensibility: Sensibility,
    val language: Language,
    val enableAP: Boolean
) {
    companion object {
        val DEFAULT_CONFIG = Config(sensibility = Sensibility.MEDIUM, language = Language.EN, enableAP = false)
    }
}

private const val SETTINGS_FILE_NAME = "settings.json"

private fun getSettingsFile(): File? {
    try {
        val jarUrl = object {}.javaClass.protectionDomain.codeSource.location
        val parentDirectory = File(jarUrl.toURI()).parentFile
        val file = File(parentDirectory, SETTINGS_FILE_NAME)
        if (file.exists()) {
            return file
        }
        // create file
        file.createNewFile()
        file.writeText(text = Json.encodeToString(serializer = Config.serializer(), value = DEFAULT_CONFIG))
        return file
    } catch (e: Exception) {
        logger.error(e) { "Could not access config file" }
    }
    return null
}

object Settings {
    lateinit var currentConfig: Config

    private val _currentConfigFlow = MutableSharedFlow<Config>(replay = 1)
    val currentConfigFlow: Flow<Config> = _currentConfigFlow

    init {
        loadSettings()
    }

    private fun loadSettings() {
        var loadedConfig: Config? = null
        try {
            getSettingsFile()?.let {
                loadedConfig = Json.decodeFromString(Config.serializer(), it.readText())
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to load config file. Set current config to default" }
        } finally {
            currentConfig = loadedConfig ?: DEFAULT_CONFIG
        }
    }

    private fun saveSettings() {
        try {
            val string: String = Json.encodeToString(Config.serializer(), currentConfig)
            val file = getSettingsFile()
            if (file == null) {
                logger.error { "Could not load config file" }
                return
            }
            file.writeText(string)
            logger.debug { "Write new Config to file. New config: $currentConfig" }
        } catch (e: Exception) {
            logger.error(e) { "Could not save config. Error: ${e.message}" }
        }
        CoroutineScope(Dispatchers.IO).launch {
            _currentConfigFlow.emit(currentConfig)
        }
    }

    fun setLanguage(language: Language) {
        currentConfig = currentConfig.copy(language = language)
        saveSettings()
    }

    fun setSensibility(sensibility: Sensibility) {
        currentConfig = currentConfig.copy(sensibility = sensibility)
        saveSettings()
    }

    fun getSensibility(): Sensibility {
        return currentConfig.sensibility
    }
}



