package dev.farred.fayyadhos.tvnext

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PerformanceProfileSelectorTest {
    @Test fun fourGigabyteClassUsesBalancedThirtyFpsBudget() {
        val profile = PerformanceProfileSelector.select(4_096)
        assertEquals("Balanced", profile.name)
        assertEquals(30, profile.targetFps)
        assertEquals(1.0f, profile.sceneScale)
        assertFalse(profile.realTimeShadows)
        assertFalse(profile.particleEffects)
    }

    @Test fun lowMemoryDeviceUsesLiteBudget() {
        val profile = PerformanceProfileSelector.select(2_048)
        assertEquals("Lite", profile.name)
        assertEquals(30, profile.targetFps)
        assertEquals(0.75f, profile.sceneScale)
        assertFalse(profile.realTimeShadows)
        assertFalse(profile.particleEffects)
    }
}
