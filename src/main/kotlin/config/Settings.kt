package de.vanfanel.joustmania.config

import de.vanfanel.joustmania.types.Config
import de.vanfanel.joustmania.types.Config.Companion.DEFAULT_CONFIG
import de.vanfanel.joustmania.types.Language
import de.vanfanel.joustmania.types.Sensibility
import de.vanfanel.joustmania.util.CustomThreadDispatcher
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

private val logger = KotlinLogging.logger {}

private const val SETTINGS_FILE_NAME = "settings.json"

private fun getSettingsFile(): File? {
    try {
        val jarUrl = object {}.javaClass.protectionDomain.codeSource.location
        val parentDirectory = File(jarUrl.toURI()).parentFile
        val file = File(parentDirectory, SETTINGS_FILE_NAME)
        if (file.exists()) {
            return file
        }
        file.createNewFile()
        file.writeText(text = Json.encodeToString(serializer = Config.serializer(), value = DEFAULT_CONFIG))
        return file
    } catch (e: Exception) {
        logger.error(e) { "Could not access config file" }
    }
    return null
}

object Settings {
    var currentConfig: Config = DEFAULT_CONFIG

    private val _currentConfigFlow = MutableStateFlow(DEFAULT_CONFIG)
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
        CoroutineScope(CustomThreadDispatcher.DEBUG_UI).launch {
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

    fun setSortToddlerGameOptionRoundDuration(duration: Int) {
        currentConfig = currentConfig.copy(sortToddlerGameOptions = currentConfig.sortToddlerGameOptions.copy(roundDuration = duration))
        saveSettings()
    }

    fun setSortToddlerGameOptionAmountOfRounds(amount: Int) {
        currentConfig = currentConfig.copy(sortToddlerGameOptions = currentConfig.sortToddlerGameOptions.copy(amountOfRounds = amount))
        saveSettings()
    }
}



