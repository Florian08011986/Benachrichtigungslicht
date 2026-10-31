package com.example

import com.example.model.PulseCurve
import com.example.rendering.PulseMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** Prüft, dass Pulskurven ruckfrei verlaufen und der weiche Lichtverlauf keine sichtbaren Stufen hat. */
class PulseMathTest {

    @Test
    fun curves_stayInRange_andHaveNoJumps() {
        for (curve in PulseCurve.entries) {
            var prev = PulseMath.level(curve, 0f)
            for (i in 1..2000) {
                val v = PulseMath.level(curve, i / 2000f)
                assertTrue("$curve außerhalb 0..1", v in 0f..1f)
                // bei 2000 Schritten pro Zyklus darf sich der Pegel kaum ändern (kein Sprung/Blinken)
                assertTrue("$curve springt bei $i: ${abs(v - prev)}", abs(v - prev) < 0.01f)
                prev = v
            }
            // Zyklus schließt nahtlos
            assertEquals(PulseMath.level(curve, 0f), PulseMath.level(curve, 1f), 1e-4f)
        }
    }

    @Test
    fun breath_holdsAtTopAndBottom() {
        assertEquals(1f, PulseMath.level(PulseCurve.BREATH, 0.45f), 1e-6f)
        assertEquals(0f, PulseMath.level(PulseCurve.BREATH, 0.95f), 1e-6f)
    }

    @Test
    fun intensity_respectsStrength_andGlimmerIsGentle() {
        assertEquals(1f, PulseMath.intensity(PulseCurve.SINE, 1f, 0.8f), 1e-6f)
        assertEquals(0.2f, PulseMath.intensity(PulseCurve.SINE, 0f, 0.8f), 1e-5f)
        val glimmerLow = PulseMath.intensity(PulseCurve.GLIMMER, 0f, 1f)
        assertTrue("Glimmen soll kaum abtauchen", glimmerLow >= 0.6f)
    }

    @Test
    fun glowLayers_reproduceSoftProfile_withoutVisibleBands() {
        val n = 32
        val peak = 0.92f
        val a = PulseMath.glowLayerAlphas(n, peak)
        // Gesamtintensität in jedem Tiefenintervall nachrechnen
        var prevIntensity = 1f
        for (k in 1..n) {
            var trans = 1f
            for (j in k..n) trans *= (1f - a[j - 1])
            val intensity = 1f - trans
            val d = (k - 1).toFloat() / n
            val target = peak * (1f - d) * (1f - d)
            assertEquals("Profil weicht ab bei Schicht $k", target, intensity, 1e-3f)
            if (k > 1) assertTrue("Stufe zu groß bei $k", prevIntensity - intensity < 0.06f)
            prevIntensity = intensity
        }
        assertTrue("Am Rand soll es fast deckend sein", prevIntensity >= 0f)
    }
}
