package de.vanfanel.joustmania.games

import kotlinx.serialization.Serializable

@Serializable
data class SetSensitivity(val sensitivity: String)

@Serializable
data class GetSensitivity(val sensitivity: String)

@Serializable
data class GetLanguage(val language: String)

@Serializable
data class SetLanguage(val language: String)

@Serializable
data class SetGameMode(val gameMode: String)