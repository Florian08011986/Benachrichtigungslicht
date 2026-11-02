package com.example

import com.example.model.EdgeColorPalette
import com.example.model.LightProfiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Prüft, dass jedes Komplettprofil auf eine existierende KI-Palette und gültige Wertebereiche zeigt. */
class LightProfilesTest {

    @Test
    fun profiles_referenceExistingKiPalettes() {
        LightProfiles.ALL.forEach { profile ->
            val preset = EdgeColorPalette.PRESETS.firstOrNull { it.id == profile.paletteId }
            assertTrue("Palette ${profile.paletteId} fehlt", preset != null)
            assertEquals("KI", preset!!.category)
        }
    }

    @Test
    fun profiles_valuesWithinUiSliderRanges() {
        LightProfiles.ALL.forEach { p ->
            assertTrue(p.animationSpeed in 0.2f..20f)
            assertTrue(p.pulseSpeed in 0.125f..2f)
            assertTrue(p.borderThickness in 4f..60f)
        }
    }

    @Test
    fun paletteIds_areUnique() {
        val ids = EdgeColorPalette.PRESETS.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}
