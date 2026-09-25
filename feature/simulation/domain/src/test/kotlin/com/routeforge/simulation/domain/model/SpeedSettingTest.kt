package com.routeforge.simulation.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SpeedSettingTest {
    @Test
    fun `walking preset converts 5 kmh to meters per second`() {
        assertEquals(1.3888889f, SpeedSetting.Preset(SpeedPresetType.WALKING).metersPerSecond, 0.0001f)
    }

    @Test
    fun `bicycle preset converts 15 kmh to meters per second`() {
        assertEquals(4.1666665f, SpeedSetting.Preset(SpeedPresetType.BICYCLE).metersPerSecond, 0.0001f)
    }

    @Test
    fun `motorcycle preset converts 60 kmh to meters per second`() {
        assertEquals(16.666666f, SpeedSetting.Preset(SpeedPresetType.MOTORCYCLE).metersPerSecond, 0.0001f)
    }

    @Test
    fun `car preset converts 80 kmh to meters per second`() {
        assertEquals(22.222221f, SpeedSetting.Preset(SpeedPresetType.CAR).metersPerSecond, 0.0001f)
    }

    @Test
    fun `manual speed carries its value through unchanged`() {
        assertEquals(3.5f, SpeedSetting.Manual(3.5f).metersPerSecond)
    }
}
