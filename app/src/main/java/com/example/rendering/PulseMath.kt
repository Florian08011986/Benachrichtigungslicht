package com.example.rendering

import com.example.model.PulseCurve
import kotlin.math.PI
import kotlin.math.cos

/**
 * Reine Mathematik für Puls und weichen Lichtverlauf – ohne Android-Abhängigkeit, daher unit-testbar.
 */
object PulseMath {

    /** Sehr weiche S-Kurve (Ken Perlin), stetig in 1. und 2. Ableitung → kein „Ruck“ an Umkehrpunkten. */
    fun smootherStep(x: Float): Float {
        val t = x.coerceIn(0f, 1f)
        return t * t * t * (t * (t * 6f - 15f) + 10f)
    }

    /** Pegel 0..1 für eine Phase 0..1 je nach Kurve. */
    fun level(curve: PulseCurve, phase: Float): Float {
        val p = ((phase % 1f) + 1f) % 1f
        val raw = when (curve) {
            PulseCurve.SINE, PulseCurve.GLIMMER -> (0.5f - 0.5f * cos(2.0 * PI * p)).toFloat()
            PulseCurve.BREATH -> when {
                p < 0.40f -> smootherStep(p / 0.40f)              // Einatmen
                p < 0.50f -> 1f                                   // kurze Ruhe oben
                p < 0.92f -> 1f - smootherStep((p - 0.50f) / 0.42f) // Ausatmen
                else -> 0f                                        // kurze Ruhe unten
            }
        }
        return raw.coerceIn(0f, 1f) // Float-Rundung an Kurvengrenzen abfangen
    }

    /**
     * Helligkeitsfaktor 0..1. [strength] = Anteil, der beim Ausatmen abtaucht.
     * „Glimmen“ nutzt nur ein Drittel der Stärke für ein ganz ruhiges Auf und Ab.
     */
    fun intensity(curve: PulseCurve, level: Float, strength: Float): Float {
        val s = strength.coerceIn(0f, 1f) * (if (curve == PulseCurve.GLIMMER) 0.35f else 1f)
        return (1f - s * (1f - level.coerceIn(0f, 1f))).coerceIn(0f, 1f)
    }

    /**
     * Deckkraft je Lichtschicht für einen weichen, konturlosen Verlauf vom Rand ins Display.
     * Schicht k (1..n) reicht bis Tiefe k/n · Reichweite. Zielprofil: f(d) = peak · (1 − d)².
     * Herleitung: Transmission im Intervall (t[k−1], t[k]) = Π_{j≥k}(1 − a_j) = 1 − f(t[k−1]).
     */
    fun glowLayerAlphas(layers: Int, peak: Float = 0.92f): FloatArray {
        val n = layers.coerceAtLeast(2)
        fun f(d: Float): Float {
            val r = (1f - d).coerceIn(0f, 1f)
            return peak.coerceIn(0f, 0.99f) * r * r
        }
        val alphas = FloatArray(n)
        for (k in 1..n) {
            val tPrev = (k - 1).toFloat() / n
            val tCur = k.toFloat() / n
            val transPrev = 1f - f(tPrev)
            val transCur = 1f - f(tCur)
            alphas[k - 1] = (1f - transPrev / transCur).coerceIn(0f, 1f)
        }
        return alphas
    }
}
