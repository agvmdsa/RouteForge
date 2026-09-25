package com.routeforge.simulation.domain.model

private const val KMH_TO_MPS = 1000.0 / 3600.0

enum class SpeedPresetType(val kilometersPerHour: Double) {
    WALKING(5.0),
    BICYCLE(15.0),
    MOTORCYCLE(60.0),
    CAR(80.0),
}

/** FR-016: a named preset or a manual value, in meters/second, applicable to both route playback and joystick. */
sealed class SpeedSetting {
    abstract val metersPerSecond: Float

    data class Preset(
        val type: SpeedPresetType,
    ) : SpeedSetting() {
        override val metersPerSecond: Float = (type.kilometersPerHour * KMH_TO_MPS).toFloat()
    }

    data class Manual(
        override val metersPerSecond: Float,
    ) : SpeedSetting()

    companion object {
        val DEFAULT: SpeedSetting = Preset(SpeedPresetType.WALKING)
    }
}
