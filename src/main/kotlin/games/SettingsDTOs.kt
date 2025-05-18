package de.vanfanel.joustmania.games

import kotlinx.serialization.Serializable

@Serializable
data class SetSensitivity(val sensitivity: String)

@Serializable
data class GetSensitivity(val sensitivity: String)
