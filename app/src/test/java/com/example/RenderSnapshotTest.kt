package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.example.model.AnimationMode
import com.example.model.EdgeConfig
import com.example.model.LineStyle
import com.example.rendering.EdgeLightRenderer
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * Rendert jeden Linienstil in eine echte Bitmap (Robolectric NATIVE-Grafik) und speichert PNGs
 * unter build/snapshots zur Sichtprüfung. Prüft zusätzlich, dass die Aura weich ausläuft.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RenderSnapshotTest {

    private val w = 540
    private val h = 1200

    private fun renderFrame(config: EdgeConfig, frames: Int = 30): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val renderer = EdgeLightRenderer()
        renderer.updateDimensions(w.toFloat(), h.toFloat(), config)
        var t = 1_000_000_000L
        repeat(frames) {
            bmp.eraseColor(Color.TRANSPARENT)
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.rgb(28, 28, 32)) // dunkler „Display-Inhalt“
            renderer.render(canvas, config, t)
            t += 33_000_000L
        }
        return bmp
    }

    private fun save(bmp: Bitmap, name: String) {
        val dir = File("build/snapshots").apply { mkdirs() }
        FileOutputStream(File(dir, "$name.png")).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun renderAllStyles() {
        for (style in LineStyle.entries) {
            val cfg = EdgeConfig(
                lineStyle = style,
                selectedPaletteId = 48,
                animationMode = AnimationMode.ROTATE,
                glowReach = 140f,
                borderThickness = 10f
            )
            save(renderFrame(cfg), "style_${style.name}")
        }
    }

    @Test
    fun aura_fadesSoftlyIntoDisplay() {
        val cfg = EdgeConfig(lineStyle = LineStyle.AURA, selectedPaletteId = 48, glowReach = 140f, borderThickness = 10f)
        val bmp = renderFrame(cfg)
        save(bmp, "aura_profile")
        // Helligkeit entlang einer horizontalen Linie in Bildschirmmitte vom linken Rand nach innen
        val y = h / 2
        val lum = (0 until 200).map { x ->
            val c = bmp.getPixel(x, y)
            (Color.red(c) + Color.green(c) + Color.blue(c)) / 3f
        }
        var maxStep = 0f
        for (x in 1 until lum.size) maxStep = maxOf(maxStep, kotlin.math.abs(lum[x] - lum[x - 1]))
        println("AURA luminance x=0..199 step10: " + lum.filterIndexed { i, _ -> i % 10 == 0 }.map { it.toInt() })
        println("AURA max neighbour step: $maxStep")
        assertTrue("Rand soll deutlich heller als Inhalt sein", lum[2] > lum[190] + 40f)
        assertTrue("Keine harte Kante (max. Sprung $maxStep)", maxStep < 18f)
    }

    @Test
    fun twoStageStart_showsSpectrumFirst_thenTargetColors() {
        val cfg = EdgeConfig(
            lineStyle = LineStyle.RING, selectedPaletteId = 48, isGlowEnabled = false,
            animationMode = AnimationMode.PULSE, isTwoStageStartEnabled = true, borderThickness = 16f
        )
        val renderer = EdgeLightRenderer()
        renderer.updateDimensions(w.toFloat(), h.toFloat(), cfg)
        renderer.startIntro()
        fun frameAt(seconds: Float, t0: Long): Bitmap {
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.rgb(28, 28, 32))
            renderer.render(canvas, cfg, t0 + (seconds * 1e9f).toLong())
            return bmp
        }
        val t0 = 5_000_000_000L
        frameAt(0f, t0)                         // Intro beginnt
        val early = frameAt(0.4f, t0)           // Stufe 1: Spektrum
        save(early, "intro_stage1")
        val late = frameAt(2.4f, t0)            // nach Intro: Zielpalette
        save(late, "intro_done")
        // Unterschiedliche Farben am selben Randpunkt → Intro hat sichtbar gewirkt
        var differing = 0
        for (y in 100 until h - 100 step 50) {
            if (early.getPixel(4, y) != late.getPixel(4, y)) differing++
        }
        println("INTRO differing edge samples: $differing")
        assertTrue("Intro-Farben unterscheiden sich nicht von Zielfarben", differing >= 5)
    }

    @Test
    fun opacity100_coversContent() {
        val cfg = EdgeConfig(lineStyle = LineStyle.RING, selectedPaletteId = 48, opacityPercent = 100, borderThickness = 16f, isGlowEnabled = false)
        val bmp = renderFrame(cfg)
        val c = bmp.getPixel(4, h / 2)
        println("RING opacity100 edge pixel: r=${Color.red(c)} g=${Color.green(c)} b=${Color.blue(c)}")
        // Inhalt ist (28,28,32); voll deckender Kern muss die Palettenfarbe zeigen, nicht den Inhalt
        assertTrue("Kern deckt nicht", Color.blue(c) > 150)
    }

    @Test
    fun ring_usesRingColor_andAppColorOverride_andStaysSteady() {
        val green = 0xFF25D366L
        val orange = 0xFFD97757L
        val base = EdgeConfig(
            lineStyle = LineStyle.RING, ringColor = green, isGlowEnabled = false,
            borderThickness = 16f, animationMode = AnimationMode.ROTATE_AND_PULSE
        )
        // Grundfarbe
        val g = renderFrame(base).getPixel(4, h / 2)
        println("RING base pixel: ${Integer.toHexString(g)}")
        assertTrue("Ring nicht grün", Color.green(g) > 180 && Color.red(g) < 80)

        // App-Farbe (Alarm) überschreibt Grundfarbe; Alarm erzwingt beim Ring KEIN Pulsieren
        val alert = base.copy(colorOverride = orange, isCallerAlertActive = true)
        val renderer = EdgeLightRenderer()
        renderer.updateDimensions(w.toFloat(), h.toFloat(), alert)
        val samples = (0 until 6).map { i ->
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.rgb(28, 28, 32))
            renderer.render(canvas, alert, 1_000_000_000L + i * 150_000_000L)
            bmp.getPixel(4, h / 2)
        }
        println("RING alert pixels: ${samples.map { Integer.toHexString(it) }}")
        assertTrue("App-Farbe nicht orange", Color.red(samples[0]) > 180 && Color.blue(samples[0]) < 120)
        assertTrue("Ring flackert/pulsiert", samples.distinct().size == 1)
    }
}
