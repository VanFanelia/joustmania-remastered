package de.vanfanel.joustmania.hardware.psmove

data class RawMovingData(
    val accelerationX: Float,
    val accelerationY: Float,
    val accelerationZ: Float,
    val total: Double,
    val change: Double,
)