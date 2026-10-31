package com.example.ui.components

import android.graphics.Canvas
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EdgeConfig
import com.example.rendering.EdgeLightRenderer
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.NeonCyan

@Composable
fun EdgeScreenPreview(
    config: EdgeConfig,
    modifier: Modifier = Modifier
) {
    val renderer = remember { EdgeLightRenderer() }
    var frameTimeNanos by remember { mutableLongStateOf(0L) }

    // Continuous 60/120fps frame loop for preview
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameTimeNanos = it }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF030509))
            .border(1.dp, DarkBorder, RoundedCornerShape(24.dp))
            .testTag("edge_screen_preview"),
        contentAlignment = Alignment.Center
    ) {
        // Inner phone preview frame
        Box(
            modifier = Modifier
                .padding(vertical = 14.dp)
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF000000))
                .border(2.dp, Color(0xFF1E2638), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("preview_canvas")
            ) {
                // Ensure frameTime is observed
                val currentTime = frameTimeNanos
                val nativeCanvas: Canvas = drawContext.canvas.nativeCanvas

                val w = size.width
                val h = size.height
                if (w > 0f && h > 0f) {
                    // Scale configuration to miniature preview screen
                    val scaleFactor = w / 360f
                    val scaledConfig = config.copy(
                        borderThickness = (config.borderThickness * scaleFactor).coerceAtLeast(4f),
                        shapeSize = (config.shapeSize * scaleFactor).coerceAtLeast(10f),
                        shapeSpacing = (config.shapeSpacing * scaleFactor).coerceAtLeast(8f),
                        cornerRadiusTop = (config.cornerRadiusTop * scaleFactor).coerceAtLeast(8f),
                        cornerRadiusBottom = (config.cornerRadiusBottom * scaleFactor).coerceAtLeast(8f),
                        notchConfig = config.notchConfig.copy(
                            notchWidth = config.notchConfig.notchWidth * scaleFactor,
                            notchHeight = config.notchConfig.notchHeight * scaleFactor,
                            notchOffsetX = config.notchConfig.notchOffsetX * scaleFactor,
                            notchOffsetY = config.notchConfig.notchOffsetY * scaleFactor,
                            notchRadius = config.notchConfig.notchRadius * scaleFactor
                        )
                    )

                    renderer.updateDimensions(w, h, scaledConfig)
                    renderer.render(nativeCanvas, scaledConfig, currentTime)
                }
            }

            // Mockup center content
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "LIVE-VORSCHAU",
                    color = NeonCyan.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
