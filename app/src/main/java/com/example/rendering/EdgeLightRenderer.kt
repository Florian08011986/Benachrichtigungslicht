package com.example.rendering

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.Rect
import android.graphics.SweepGradient
import com.example.model.BorderShapeType
import com.example.model.EdgeColorPalette
import com.example.model.EdgeConfig
import com.example.model.NotchType
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class EdgeLightRenderer {

    private var width: Float = 0f
    private var height: Float = 0f

    // Reusable Paths
    private val borderPath = Path()
    private val cutoutPath = Path()
    private val combinedPath = Path()
    private val pathMeasure = PathMeasure()

    // Shape template paths (normalized around origin 0,0)
    private val heartTemplatePath = Path()
    private val starTemplatePath = Path()
    private val diamondTemplatePath = Path()
    private val sunTemplatePath = Path()
    private val transformedShapePath = Path()

    // Paints
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL_AND_STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private val centerHeartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFE5173A.toInt()
    }

    // Gradient & Transform
    private var sweepGradient: SweepGradient? = null
    private val matrix = Matrix()
    private val shapeMatrix = Matrix()
    private val pos = FloatArray(2)
    private val tan = FloatArray(2)
    private val textBounds = Rect()

    // Animation State
    private var currentRotationAngle: Float = 0f
    private var lastRenderTimeNanos: Long = 0L

    // Cached configuration to detect changes
    private var cachedPaletteId: Int = -1
    private var cachedUseCustomColors: Boolean = false
    private var cachedCustomColors: List<Long> = emptyList()
    private var cachedColorsArray: IntArray = intArrayOf()

    init {
        initVectorTemplates()
    }

    private fun initVectorTemplates() {
        // Heart template (unit size ~ 1.0)
        heartTemplatePath.reset()
        heartTemplatePath.moveTo(0f, 0.35f)
        heartTemplatePath.cubicTo(-0.6f, -0.3f, -1.0f, 0.4f, 0f, 1.0f)
        heartTemplatePath.cubicTo(1.0f, 0.4f, 0.6f, -0.3f, 0f, 0.35f)
        heartTemplatePath.close()

        // 5-Pointed Star template
        starTemplatePath.reset()
        val numPoints = 5
        val outerRadius = 1.0f
        val innerRadius = 0.45f
        for (i in 0 until numPoints * 2) {
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val angle = (i * PI / numPoints) - (PI / 2)
            val x = (r * cos(angle)).toFloat()
            val y = (r * sin(angle)).toFloat()
            if (i == 0) starTemplatePath.moveTo(x, y) else starTemplatePath.lineTo(x, y)
        }
        starTemplatePath.close()

        // Diamond template
        diamondTemplatePath.reset()
        diamondTemplatePath.moveTo(0f, -1.0f)
        diamondTemplatePath.lineTo(0.75f, 0f)
        diamondTemplatePath.lineTo(0f, 1.0f)
        diamondTemplatePath.lineTo(-0.75f, 0f)
        diamondTemplatePath.close()

        // Sun template
        sunTemplatePath.reset()
        sunTemplatePath.addCircle(0f, 0f, 0.55f, Path.Direction.CW)
        for (i in 0 until 8) {
            val angle = (i * PI / 4.0)
            val x1 = (0.65f * cos(angle)).toFloat()
            val y1 = (0.65f * sin(angle)).toFloat()
            val x2 = (1.0f * cos(angle)).toFloat()
            val y2 = (1.0f * sin(angle)).toFloat()
            sunTemplatePath.moveTo(x1, y1)
            sunTemplatePath.lineTo(x2, y2)
        }
    }

    fun updateDimensions(newWidth: Float, newHeight: Float, config: EdgeConfig) {
        if (newWidth <= 0f || newHeight <= 0f) return
        val sizeChanged = (width != newWidth || height != newHeight)
        width = newWidth
        height = newHeight

        if (sizeChanged) {
            invalidateShader(config)
        }
        buildBorderPaths(config)
    }

    private fun invalidateShader(config: EdgeConfig) {
        val rawColors: List<Long> = if (config.useCustomColors) {
            config.customColors.ifEmpty { listOf(0xFF00E5FFL, 0xFFD500F9L) }
        } else {
            EdgeColorPalette.getPreset(config.selectedPaletteId).colors
        }

        // Ensure closed loop for seamless 360-degree sweep gradient
        val closedList = if (rawColors.first() != rawColors.last()) {
            rawColors + rawColors.first()
        } else {
            rawColors
        }

        cachedColorsArray = closedList.map { it.toInt() }.toIntArray()
        cachedPaletteId = config.selectedPaletteId
        cachedUseCustomColors = config.useCustomColors
        cachedCustomColors = config.customColors

        if (width > 0f && height > 0f) {
            sweepGradient = SweepGradient(width / 2f, height / 2f, cachedColorsArray, null)
            strokePaint.shader = sweepGradient
            glowPaint.shader = sweepGradient
            shapePaint.shader = sweepGradient
        }
    }

    private fun checkShaderUpdate(config: EdgeConfig) {
        val colorsDiffer = (config.selectedPaletteId != cachedPaletteId) ||
                (config.useCustomColors != cachedUseCustomColors) ||
                (config.useCustomColors && config.customColors != cachedCustomColors) ||
                sweepGradient == null

        if (colorsDiffer) {
            invalidateShader(config)
        }
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
    }

    fun render(
        canvas: Canvas,
        config: EdgeConfig,
        nowNanos: Long,
        batteryDimMultiplier: Float = 1.0f,
        disableGlow: Boolean = false
    ) {
        if (width <= 0f || height <= 0f) return

        checkShaderUpdate(config)

        // Calculate time delta for smooth motion
        if (lastRenderTimeNanos == 0L) {
            lastRenderTimeNanos = nowNanos
        }
        val deltaSeconds = ((nowNanos - lastRenderTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.1f)
        lastRenderTimeNanos = nowNanos

        // Caller alert boosts speed and intensity
        val speedMultiplier = if (config.isCallerAlertActive) config.animationSpeed * 2.5f else config.animationSpeed
        val degreesPerSecond = 36f * speedMultiplier
        currentRotationAngle = (currentRotationAngle + degreesPerSecond * deltaSeconds) % 360f

        // Rotate sweep gradient
        matrix.reset()
        matrix.setRotate(currentRotationAngle, width / 2f, height / 2f)
        sweepGradient?.setLocalMatrix(matrix)

        // Base alpha calculation modulated by user brightness setting
        val userBrightness = (config.brightnessPercent.coerceIn(10, 100) / 100f)
        val baseAlpha = if (config.isCallerAlertActive) {
            (180 + (75 * sin(nowNanos / 100_000_000.0)).toInt()).coerceIn(120, 255)
        } else {
            255
        }
        val alertAlpha = (baseAlpha * userBrightness * batteryDimMultiplier.coerceIn(0.1f, 1.0f)).toInt().coerceIn(15, 255)

        if (config.showBorder) {
            if (config.shapeType == BorderShapeType.CONTINUOUS_LINE) {
                renderContinuousLine(canvas, config, alertAlpha, disableGlow)
            } else {
                renderBorderShapes(canvas, config, alertAlpha)
            }
        }

        if (config.showCenterHeart) {
            renderCenterHeart(canvas, nowNanos)
        }
    }

    // Rotes, sanft pulsierendes Herz in der Displaymitte
    private fun renderCenterHeart(canvas: Canvas, nowNanos: Long) {
        val pulse = 1f + 0.08f * sin(nowNanos / 250_000_000.0).toFloat()
        val radius = min(width, height) * 0.22f * pulse
        // Die Herzvorlage reicht vertikal von ca. -0.1 bis 1.0, optische Mitte bei ca. 0.45
        transformedShapePath.reset()
        shapeMatrix.reset()
        shapeMatrix.postScale(radius, radius)
        shapeMatrix.postTranslate(width / 2f, height / 2f - 0.45f * radius)
        heartTemplatePath.transform(shapeMatrix, transformedShapePath)
        canvas.drawPath(transformedShapePath, centerHeartPaint)
    }

    private fun renderContinuousLine(
        canvas: Canvas,
        config: EdgeConfig,
        alpha: Int,
        disableGlow: Boolean = false
    ) {
        val thickness = if (config.isCallerAlertActive) config.borderThickness * 1.3f else config.borderThickness

        // Optional outer neon glow effect (suppressed in battery saving mode)
        if (config.isGlowEnabled && !disableGlow) {
            glowPaint.strokeWidth = thickness * 1.8f
            glowPaint.alpha = (alpha * 0.45f).toInt()
            try {
                glowPaint.maskFilter = BlurMaskFilter(thickness * 0.45f, BlurMaskFilter.Blur.NORMAL)
            } catch (_: Exception) {
                // Hardware acceleration fallback
                glowPaint.maskFilter = null
            }
            canvas.drawPath(combinedPath, glowPaint)
        }

        // Core sharp beam
        strokePaint.strokeWidth = thickness
        strokePaint.alpha = alpha
        strokePaint.maskFilter = null
        canvas.drawPath(combinedPath, strokePaint)
    }

    private fun renderBorderShapes(canvas: Canvas, config: EdgeConfig, alpha: Int) {
        val size = if (config.isCallerAlertActive) config.shapeSize * 1.25f else config.shapeSize
        val spacing = config.shapeSpacing.coerceAtLeast(10f)
        val stepDistance = size + spacing

        pathMeasure.setPath(borderPath, false)
        val pathLength = pathMeasure.length
        if (pathLength <= 0f) return

        val count = (pathLength / stepDistance).toInt()
        if (count <= 0) return

        // Dynamic moving phase offset
        val phaseOffset = (currentRotationAngle / 360f) * pathLength

        shapePaint.alpha = alpha
        textPaint.textSize = size
        textPaint.alpha = alpha

        for (i in 0 until count) {
            val dist = (i * stepDistance + phaseOffset) % pathLength
            pathMeasure.getPosTan(dist, pos, tan)

            val px = pos[0]
            val py = pos[1]
            val angleRad = atan2(tan[1].toDouble(), tan[0].toDouble())
            val angleDeg = Math.toDegrees(angleRad).toFloat()

            when (config.shapeType) {
                BorderShapeType.CONTINUOUS_LINE -> { /* Handled separately */ }

                BorderShapeType.HEART -> {
                    drawScaledVector(canvas, heartTemplatePath, px, py, size / 2f, angleDeg)
                }
                BorderShapeType.STAR -> {
                    drawScaledVector(canvas, starTemplatePath, px, py, size / 2f, angleDeg)
                }
                BorderShapeType.DIAMOND -> {
                    drawScaledVector(canvas, diamondTemplatePath, px, py, size / 2f, angleDeg)
                }
                BorderShapeType.SUN -> {
                    drawScaledVector(canvas, sunTemplatePath, px, py, size / 2f, angleDeg)
                }
                BorderShapeType.EMOJI_COOL -> {
                    drawEmojiAt(canvas, "😎", px, py, angleDeg)
                }
                BorderShapeType.EMOJI_SLEEP -> {
                    drawEmojiAt(canvas, "💤", px, py, angleDeg)
                }
                BorderShapeType.CUSTOM_EMOJI -> {
                    val emoji = config.customEmoji.ifBlank { "✨" }
                    drawEmojiAt(canvas, emoji, px, py, angleDeg)
                }
            }
        }

        // If punch hole cutout is present, draw shapes or stroke around it as well
        if (!cutoutPath.isEmpty) {
            pathMeasure.setPath(cutoutPath, false)
            val cutLength = pathMeasure.length
            if (cutLength > 0f) {
                val cutCount = (cutLength / stepDistance).toInt().coerceAtLeast(1)
                for (j in 0 until cutCount) {
                    val dist = (j * stepDistance + phaseOffset) % cutLength
                    pathMeasure.getPosTan(dist, pos, tan)
                    val px = pos[0]
                    val py = pos[1]
                    val angleDeg = Math.toDegrees(atan2(tan[1].toDouble(), tan[0].toDouble())).toFloat()

                    when (config.shapeType) {
                        BorderShapeType.HEART -> drawScaledVector(canvas, heartTemplatePath, px, py, size * 0.4f, angleDeg)
                        BorderShapeType.STAR -> drawScaledVector(canvas, starTemplatePath, px, py, size * 0.4f, angleDeg)
                        BorderShapeType.DIAMOND -> drawScaledVector(canvas, diamondTemplatePath, px, py, size * 0.4f, angleDeg)
                        BorderShapeType.SUN -> drawScaledVector(canvas, sunTemplatePath, px, py, size * 0.4f, angleDeg)
                        BorderShapeType.EMOJI_COOL -> drawEmojiAt(canvas, "😎", px, py, angleDeg, scale = 0.8f)
                        BorderShapeType.EMOJI_SLEEP -> drawEmojiAt(canvas, "💤", px, py, angleDeg, scale = 0.8f)
                        BorderShapeType.CUSTOM_EMOJI -> drawEmojiAt(canvas, config.customEmoji.ifBlank { "✨" }, px, py, angleDeg, scale = 0.8f)
                        else -> {}
                    }
                }
            }
        }
    }

    private fun drawScaledVector(canvas: Canvas, template: Path, cx: Float, cy: Float, radius: Float, angleDeg: Float) {
        transformedShapePath.reset()
        shapeMatrix.reset()
        shapeMatrix.postScale(radius, radius)
        shapeMatrix.postRotate(angleDeg + 90f) // Orient upright relative to perimeter tangent
        shapeMatrix.postTranslate(cx, cy)
        template.transform(shapeMatrix, transformedShapePath)
        canvas.drawPath(transformedShapePath, shapePaint)
    }

    private fun drawEmojiAt(canvas: Canvas, emoji: String, cx: Float, cy: Float, angleDeg: Float, scale: Float = 1.0f) {
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(angleDeg + 90f)
        if (scale != 1.0f) {
            canvas.scale(scale, scale)
        }
        textPaint.getTextBounds(emoji, 0, emoji.length, textBounds)
        val yOffset = textBounds.height() / 2f
        canvas.drawText(emoji, 0f, yOffset, textPaint)
        canvas.restore()
    }
}
