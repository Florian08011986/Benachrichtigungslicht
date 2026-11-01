package com.example.rendering

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.graphics.SweepGradient
import com.example.model.AnimationMode
import com.example.model.EdgeColorPalette
import com.example.model.EdgeConfig
import com.example.model.LineStyle
import com.example.model.NotchType
import com.example.model.PulseCurve
import com.example.model.TWO_STAGE_INTRO_COLORS
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.min

/**
 * Zeichnet die Randbeleuchtung – ausschließlich linienförmige Stile entlang des Rands.
 *
 * - Weicher Schein/Aura: nicht überlappende, abgerundete Ringe mit vorberechnetem Helligkeitsprofil
 *   (kein BlurMaskFilter: laut Android-Doku wird setMaskFilter() mit Hardware-Beschleunigung nicht unterstützt).
 * - Ring: ruhige, durchgehende Linie in einer Farbe (Grundfarbe oder Alarm-Farbe der App/des Anrufs).
 */
class EdgeLightRenderer {

    private var width: Float = 0f
    private var height: Float = 0f
    private var cx: Float = 0f
    private var cy: Float = 0f

    // Geometrie
    private val borderPath = Path()
    private val cutoutPath = Path()
    private val combinedPath = Path()
    private val pathMeasure = PathMeasure()
    private var perimeter: Float = 0f
    private var cornerTop: Float = 0f
    private var cornerBottom: Float = 0f

    // Bogenlänge → Winkel (Grad, monoton steigend)
    private val angleLut = FloatArray(LUT_SIZE + 1)
    private val pos = FloatArray(2)

    // Weiche Ringe (Cache je Reichweite/Schichtzahl)
    private var ringPaths: Array<Path> = emptyArray()
    private var ringProfile: FloatArray = FloatArray(0)
    private var ringDepth: FloatArray = FloatArray(0)
    private var ringWidths: FloatArray = FloatArray(0)
    private var ringKeyReach: Float = -1f
    private var ringKeyLayers: Int = -1
    private var ringKeyPeak: Float = -1f
    private val rectF = RectF()
    private val radii = FloatArray(8)

    // Paints
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }

    // Farben
    private var paletteColors: IntArray = intArrayOf()
    private var ringSweep: SweepGradient? = null
    private val gradientMatrix = Matrix()
    private var cachedResolved: List<Long> = emptyList()


    // Zwei-Stufen-Start: erst Mehrfarb-Spektrum, dann weiche Überblendung in die Zielfarben
    private val introColors: IntArray = TWO_STAGE_INTRO_COLORS.map { it.toInt() }.toIntArray()
    private var introPending: Boolean = false
    private var introStartNanos: Long = 0L
    private var activeColors: IntArray = intArrayOf()
    private var introSweep: SweepGradient? = null

    // Animationszustand
    private var travel: Float = 0f          // 0..1 Position entlang des Umfangs
    private var pulsePhase: Float = 0f      // 0..1
    private var lastRenderTimeNanos: Long = 0L

    companion object {
        private const val LUT_SIZE = 720
        private const val AURA_LAYERS = 32
        private const val LITE_LAYERS = 12
        private const val GLOW_LAYERS = 20
        private const val CALLER_PULSE_HZ = 1.4f
        private const val INTRO_HOLD_S = 0.9f   // Stufe 1: Spektrum stehen lassen
        private const val INTRO_BLEND_S = 1.1f  // Stufe 2: weich in Zielfarben überblenden
    }

    /** Spielt den Zwei-Stufen-Start beim nächsten Frame ab. */
    fun startIntro() {
        introPending = true
    }

    fun updateDimensions(newWidth: Float, newHeight: Float, config: EdgeConfig) {
        if (newWidth <= 0f || newHeight <= 0f) return
        val sizeChanged = (width != newWidth || height != newHeight)
        width = newWidth
        height = newHeight
        cx = width / 2f
        cy = height / 2f
        if (sizeChanged) invalidateColors(config)
        buildBorderPaths(config)
    }

    /**
     * Farbquelle: 1) Einzelfarbe des Alarms (App/Anruf), 2) Ring = Grundfarbe, 3) Aura = Palette/Verlauf.
     */
    private fun resolveColors(config: EdgeConfig): List<Long> {
        config.colorOverride?.let { return listOf(it, it) }
        if (config.lineStyle == LineStyle.RING) return listOf(config.ringColor, config.ringColor)
        return if (config.useCustomColors) {
            config.customColors.ifEmpty { listOf(0xFF00E5FFL, 0xFFD500F9L) }
        } else {
            EdgeColorPalette.getPreset(config.selectedPaletteId).colors
        }
    }

    private fun invalidateColors(config: EdgeConfig) {
        val raw = resolveColors(config)
        val closed = if (raw.first() != raw.last()) raw + raw.first() else raw
        paletteColors = closed.map { it.toInt() }.toIntArray()
        activeColors = paletteColors
        cachedResolved = raw
        if (width > 0f && height > 0f) {
            ringSweep = SweepGradient(cx, cy, paletteColors, null)
        }
    }

    private fun checkColorUpdate(config: EdgeConfig) {
        if (ringSweep == null || resolveColors(config) != cachedResolved) invalidateColors(config)
    }

    /** Farbe im Intro-Spektrum an Position u (0..1, geschlossen). */
    private fun introAt(u: Float): Int {
        val c = introColors
        val x = (((u % 1f) + 1f) % 1f) * c.size
        val i = floor(x).toInt() % c.size
        return lerpColor(c[i], c[(i + 1) % c.size], x - floor(x))
    }

    /**
     * Aktualisiert [activeColors] für den Zwei-Stufen-Start. Liefert true, solange das Intro läuft.
     * Mischung: 0 während der Haltezeit, danach smootherStep → 1.
     */
    private fun updateIntro(nowNanos: Long): Boolean {
        if (introPending) {
            introPending = false
            introStartNanos = nowNanos
        }
        if (introStartNanos == 0L) return false
        val elapsed = (nowNanos - introStartNanos) / 1_000_000_000f
        if (elapsed >= INTRO_HOLD_S + INTRO_BLEND_S) {
            introStartNanos = 0L
            activeColors = paletteColors
            return false
        }
        val mix = PulseMath.smootherStep((elapsed - INTRO_HOLD_S) / INTRO_BLEND_S)
        val n = paletteColors.size
        activeColors = IntArray(n) { i ->
            val u = if (n > 1) i.toFloat() / (n - 1) else 0f
            lerpColor(introAt(u), paletteColors[i], mix)
        }
        introSweep = SweepGradient(cx, cy, activeColors, null)
        return true
    }

    private fun lerpColor(a: Int, b: Int, f: Float): Int {
        fun ch(shift: Int) = (((a shr shift) and 0xFF) + (((b shr shift) and 0xFF) - ((a shr shift) and 0xFF)) * f).toInt()
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    fun buildBorderPaths(config: EdgeConfig) {
        if (width <= 0f || height <= 0f) return

        val inset = config.borderThickness / 2f
        val left = inset
        val top = inset
        val right = width - inset
        val bottom = height - inset
        val w = right - left
        val h = bottom - top

        val maxRadius = min(w / 2f, h / 2f)
        val rTop = config.cornerRadiusTop.coerceIn(0f, maxRadius)
        val rBottom = config.cornerRadiusBottom.coerceIn(0f, maxRadius)

        borderPath.reset()
        cutoutPath.reset()

        // Construct border contour with optional notch
        val notch = config.notchConfig
        val centerX = width / 2f

        // Start at top-left curve endpoint
        borderPath.moveTo(left, top + rTop)
        // Top-left corner arc
        if (rTop > 0f) {
            borderPath.quadTo(left, top, left + rTop, top)
        } else {
            borderPath.lineTo(left, top)
        }

        // Top Edge with Notch interpolation
        when (notch.notchType) {
            NotchType.NONE -> {
                borderPath.lineTo(right - rTop, top)
            }
            NotchType.INFINITY_U -> {
                val nCenterX = (centerX + notch.notchOffsetX).coerceIn(left + rTop + notch.notchWidth / 2f, right - rTop - notch.notchWidth / 2f)
                val nWidth = notch.notchWidth.coerceIn(40f, 350f)
                val nHeight = notch.notchHeight.coerceIn(20f, 200f)
                val nLeft = nCenterX - nWidth / 2f
                val nRight = nCenterX + nWidth / 2f

                borderPath.lineTo(nLeft, top)
                // Smooth U curve
                borderPath.cubicTo(
                    nLeft + nWidth * 0.15f, top + nHeight,
                    nRight - nWidth * 0.15f, top + nHeight,
                    nRight, top
                )
                borderPath.lineTo(right - rTop, top)
            }
            NotchType.INFINITY_V -> {
                val nCenterX = (centerX + notch.notchOffsetX).coerceIn(left + rTop + notch.notchWidth / 2f, right - rTop - notch.notchWidth / 2f)
                val nWidth = notch.notchWidth.coerceIn(30f, 300f)
                val nHeight = notch.notchHeight.coerceIn(20f, 200f)
                val nLeft = nCenterX - nWidth / 2f
                val nRight = nCenterX + nWidth / 2f

                borderPath.lineTo(nLeft, top)
                borderPath.lineTo(nCenterX, top + nHeight)
                borderPath.lineTo(nRight, top)
                borderPath.lineTo(right - rTop, top)
            }
            NotchType.PUNCH_HOLE_CENTER -> {
                borderPath.lineTo(right - rTop, top)
                // Add center camera hole cutout
                val holeX = centerX + notch.notchOffsetX
                val holeY = top + notch.notchOffsetY + notch.notchRadius
                cutoutPath.addCircle(holeX, holeY, notch.notchRadius.coerceIn(10f, 60f), Path.Direction.CW)
            }
            NotchType.PUNCH_HOLE_LEFT -> {
                borderPath.lineTo(right - rTop, top)
                // Add left camera hole cutout
                val holeX = left + rTop + notch.notchOffsetX + notch.notchRadius
                val holeY = top + notch.notchOffsetY + notch.notchRadius
                cutoutPath.addCircle(holeX, holeY, notch.notchRadius.coerceIn(10f, 60f), Path.Direction.CW)
            }
            NotchType.PILL_ISLAND -> {
                borderPath.lineTo(right - rTop, top)
                // Add Pill / Dynamic Island cutout
                val pillX = centerX + notch.notchOffsetX
                val pillY = top + notch.notchOffsetY + notch.notchHeight / 2f
                val pWidth = notch.notchWidth.coerceIn(60f, 400f)
                val pHeight = notch.notchHeight.coerceIn(20f, 150f)
                val pRadius = notch.notchRadius.coerceIn(6f, pHeight / 2f)

                val pillLeft = pillX - pWidth / 2f
                val pillTop = pillY - pHeight / 2f
                val pillRight = pillX + pWidth / 2f
                val pillBottom = pillY + pHeight / 2f
                cutoutPath.addRoundRect(pillLeft, pillTop, pillRight, pillBottom, pRadius, pRadius, Path.Direction.CW)
            }
        }

        // Top-right corner arc
        if (rTop > 0f) {
            borderPath.quadTo(right, top, right, top + rTop)
        } else {
            borderPath.lineTo(right, top)
        }

        // Right Edge
        borderPath.lineTo(right, bottom - rBottom)

        // Bottom-right corner arc
        if (rBottom > 0f) {
            borderPath.quadTo(right, bottom, right - rBottom, bottom)
        } else {
            borderPath.lineTo(right, bottom)
        }

        // Bottom Edge
        borderPath.lineTo(left + rBottom, bottom)

        // Bottom-left corner arc
        if (rBottom > 0f) {
            borderPath.quadTo(left, bottom, left, bottom - rBottom)
        } else {
            borderPath.lineTo(left, bottom)
        }

        borderPath.close()

        combinedPath.reset()
        combinedPath.addPath(borderPath)
        if (!cutoutPath.isEmpty) {
            combinedPath.addPath(cutoutPath)
        }

        cornerTop = rTop + inset
        cornerBottom = rBottom + inset
        buildAngleLut()
    }

    /** Bogenlänge→Winkel-Tabelle; Winkel werden „entfaltet“, damit sie entlang des Umlaufs monoton steigen. */
    private fun buildAngleLut() {
        pathMeasure.setPath(borderPath, false)
        perimeter = pathMeasure.length
        if (perimeter <= 0f) return
        var prev = 0f
        var offset = 0f
        for (i in 0..LUT_SIZE) {
            pathMeasure.getPosTan(perimeter * i / LUT_SIZE, pos, null)
            var a = Math.toDegrees(atan2((pos[1] - cy).toDouble(), (pos[0] - cx).toDouble())).toFloat()
            if (i > 0) {
                a += offset
                while (a < prev - 180f) { a += 360f; offset += 360f }
                while (a > prev + 180f) { a -= 360f; offset -= 360f }
            }
            angleLut[i] = a
            prev = a
        }
        ringKeyReach = -1f // Ringe passend zur neuen Geometrie neu bauen
    }

    /**
     * Verschachtelte Lichtschichten: Schicht k reicht vom Rand bis Tiefe d_k = k/n · Reichweite.
     * Nur ihre Innenkante liegt im Bild und ist abgerundet (Radius wächst mit der Tiefe) → keine Nähte,
     * keine diagonalen Ecken. Deckkraft je Schicht aus [PulseMath.glowLayerAlphas] → Profil peak · (1 − d)².
     */
    private fun ensureRings(reach: Float, layers: Int, peak: Float) {
        if (reach == ringKeyReach && layers == ringKeyLayers && peak == ringKeyPeak && ringPaths.size == layers) return
        ringKeyReach = reach
        ringKeyLayers = layers
        ringKeyPeak = peak
        ringProfile = PulseMath.glowLayerAlphas(layers, peak)
        ringDepth = FloatArray(layers) { k -> (k + 1f) / layers }
        ringWidths = FloatArray(layers) { k -> ringDepth[k] * reach }
        ringPaths = Array(layers) { k ->
            val d = ringWidths[k]
            val half = d / 2f
            val maxR = (min(width, height) / 2f - half).coerceAtLeast(0f)
            val rt = (cornerTop + d * 0.6f + half).coerceIn(0f, maxR)
            val rb = (cornerBottom + d * 0.6f + half).coerceIn(0f, maxR)
            radii[0] = rt; radii[1] = rt; radii[2] = rt; radii[3] = rt
            radii[4] = rb; radii[5] = rb; radii[6] = rb; radii[7] = rb
            rectF.set(half, half, width - half, height - half)
            Path().apply { addRoundRect(rectF, radii, Path.Direction.CW) }
        }
    }

    fun render(
        canvas: Canvas,
        config: EdgeConfig,
        nowNanos: Long,
        batteryDimMultiplier: Float = 1.0f,
        disableGlow: Boolean = false
    ) {
        if (width <= 0f || height <= 0f) return
        checkColorUpdate(config)
        if (perimeter <= 0f) buildAngleLut()
        if (perimeter <= 0f) return

        // Zeitschritt (auf 50 ms begrenzt → kein Sprung nach Pausen)
        if (lastRenderTimeNanos == 0L) lastRenderTimeNanos = nowNanos
        val dt = ((nowNanos - lastRenderTimeNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
        lastRenderTimeNanos = nowNanos

        val alert = config.isCallerAlertActive
        val moves = config.animationMode != AnimationMode.PULSE || alert
        // Ring leuchtet ruhig (kein Pulsieren); Puls/Atem nur für die KI-Aura
        val pulses = config.lineStyle != LineStyle.RING && (config.animationMode != AnimationMode.ROTATE || alert)

        // Bewegung: 1x = eine Runde in 10 s
        if (moves) {
            val speed = if (alert) config.animationSpeed * 2.5f else config.animationSpeed
            travel = (travel + 0.1f * speed.coerceIn(0.05f, 40f) * dt) % 1f
        }

        // Puls
        val intensity = if (pulses) {
            val curve = if (alert) PulseCurve.SINE else config.pulseCurve
            val hz = if (alert) CALLER_PULSE_HZ else config.pulseSpeed.coerceIn(0.05f, 4f)
            pulsePhase = (pulsePhase + hz * dt) % 1f
            PulseMath.intensity(curve, PulseMath.level(curve, pulsePhase), if (alert) 0.5f else config.pulseStrength)
        } else 1f

        // Ring immer voll deckend (intensive Farbe); Deckkraft-Regler nur für die KI-Aura
        val opacity = if (config.lineStyle == LineStyle.RING) 1f else config.opacityPercent.coerceIn(10, 100) / 100f
        val baseAlpha = 255f * opacity * batteryDimMultiplier.coerceIn(0.1f, 1f)
        val glowOn = config.isGlowEnabled && !disableGlow
        val thickness = if (alert) config.borderThickness * 1.3f else config.borderThickness
        val reach = config.glowReach.coerceIn(10f, 600f)

        val introRunning = updateIntro(nowNanos)

        // Farb-/Maskenverlauf für diesen Frame wählen
        val shader: SweepGradient = when (config.lineStyle) {
            LineStyle.RING, LineStyle.AURA -> {
                val sweep = (if (introRunning) introSweep else ringSweep) ?: return
                gradientMatrix.setRotate(travel * 360f, cx, cy)
                sweep.setLocalMatrix(gradientMatrix)
                sweep
            }
        }
        linePaint.shader = shader
        ringPaint.shader = shader

        when (config.lineStyle) {
            LineStyle.AURA -> {
                drawRings(canvas, reach, if (disableGlow) LITE_LAYERS else AURA_LAYERS, 0.92f, baseAlpha, intensity)
                // hauchdünne Kante – definiert den Rand, ohne harte Kontur
                drawLine(canvas, borderPath, thickness * 0.35f, baseAlpha * intensity * 0.55f)
            }
            LineStyle.RING -> {
                if (glowOn) drawRings(canvas, reach, GLOW_LAYERS, 0.6f, baseAlpha, intensity)
                drawLine(canvas, combinedPath, thickness, baseAlpha * intensity)
            }
        }
    }

    private fun drawLine(canvas: Canvas, path: Path, strokeWidth: Float, alpha: Float) {
        linePaint.strokeWidth = strokeWidth.coerceAtLeast(1f)
        linePaint.alpha = alpha.toInt().coerceIn(0, 255)
        canvas.drawPath(path, linePaint)
    }

    /** Weicher Schein; beim Pulsieren tauchen tiefe Ringe stärker ab → das Licht „atmet“ ins Display. */
    private fun drawRings(canvas: Canvas, reach: Float, layers: Int, peak: Float, baseAlpha: Float, intensity: Float) {
        ensureRings(reach, layers, peak)
        for (k in ringPaths.indices.reversed()) { // tiefste (breiteste) Schicht zuerst
            val breath = (1f - (1f - intensity) * (0.25f + 0.75f * ringDepth[k])).coerceIn(0f, 1f)
            val a = (baseAlpha * ringProfile[k] * breath).toInt().coerceIn(0, 255)
            if (a == 0) continue
            ringPaint.alpha = a
            ringPaint.strokeWidth = ringWidths[k]
            canvas.drawPath(ringPaths[k], ringPaint)
        }
    }
}
