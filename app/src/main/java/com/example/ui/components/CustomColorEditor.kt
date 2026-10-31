package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CustomColorEditor(
    colors: List<Long>,
    onColorsChanged: (List<Long>) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSlot by remember { mutableIntStateOf(0) }

    val activeColors = if (colors.size < 2) listOf(0xFF00E5FFL, 0xFFD500F9L) else colors
    val safeSlot = selectedSlot.coerceIn(0, activeColors.size - 1)
    val currentColorLong = activeColors[safeSlot]

    val red = ((currentColorLong shr 16) and 0xFF).toInt()
    val green = ((currentColorLong shr 8) and 0xFF).toInt()
    val blue = (currentColorLong and 0xFF).toInt()

    fun updateCurrentColor(r: Int, g: Int, b: Int) {
        val newColorLong = (0xFF000000L or ((r.toLong() and 0xFF) shl 16) or ((g.toLong() and 0xFF) shl 8) or (b.toLong() and 0xFF))
        val updated = activeColors.toMutableList()
        updated[safeSlot] = newColorLong
        onColorsChanged(updated)
    }

    val quickColors = listOf(
        0xFF00E5FFL, 0xFFD500F9L, 0xFFFF1744L, 0xFF00FF66L,
        0xFFFFD700L, 0xFFFF007FL, 0xFF7928CAL, 0xFF4CC9F0L,
        0xFFFF5722L, 0xFFE2FF00L, 0xFFFFFFFFL, 0xFF9D4EDDL
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("custom_color_editor"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Eigener Farbverlauf-Designer",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Combined Gradient Strip Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = activeColors.map { Color(it.toInt()) }
                        )
                    )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Farbpunkte 2, 3 oder 4
            Text(
                text = "Farbverlauf-Stopps (${activeColors.size} aktiv)",
                color = TextSecondary,
                style = MaterialTheme.typography.labelMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                activeColors.forEachIndexed { index, colorVal ->
                    val isSlotActive = index == safeSlot
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSlotActive) DarkSurfaceVariant else DarkBorder)
                            .border(
                                width = if (isSlotActive) 2.dp else 1.dp,
                                color = if (isSlotActive) NeonCyan else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedSlot = index }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorVal.toInt()))
                                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Punkt ${index + 1}",
                                color = if (isSlotActive) NeonCyan else TextPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Farbpunkte hinzufügen oder entfernen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (activeColors.size < 4) {
                            val newStops = activeColors + listOf(0xFF00FF66L)
                            onColorsChanged(newStops)
                            selectedSlot = newStops.size - 1
                        }
                    },
                    enabled = activeColors.size < 4,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
                ) {
                    Text("+ Punkt", color = NeonCyan)
                }

                Button(
                    onClick = {
                        if (activeColors.size > 2) {
                            val newStops = activeColors.dropLast(1)
                            onColorsChanged(newStops)
                            if (selectedSlot >= newStops.size) {
                                selectedSlot = newStops.size - 1
                            }
                        }
                    },
                    enabled = activeColors.size > 2,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
                ) {
                    Text("- Punkt", color = NeonMagenta)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // RGB Regler für gewählten Farbpunkt
            Text(
                text = "Farbe für Punkt ${safeSlot + 1}: #%06X".format(0xFFFFFF and currentColorLong.toInt()),
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Red Slider
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "R", color = Color(0xFFFF5252), modifier = Modifier.width(24.dp), fontWeight = FontWeight.Bold)
                Slider(
                    value = red.toFloat(),
                    onValueChange = { updateCurrentColor(it.toInt(), green, blue) },
                    valueRange = 0f..255f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFF5252), activeTrackColor = Color(0xFFFF5252))
                )
                Text(text = "$red", color = TextSecondary, modifier = Modifier.width(36.dp))
            }

            // Green Slider
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "G", color = Color(0xFF69F0AE), modifier = Modifier.width(24.dp), fontWeight = FontWeight.Bold)
                Slider(
                    value = green.toFloat(),
                    onValueChange = { updateCurrentColor(red, it.toInt(), blue) },
                    valueRange = 0f..255f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF69F0AE), activeTrackColor = Color(0xFF69F0AE))
                )
                Text(text = "$green", color = TextSecondary, modifier = Modifier.width(36.dp))
            }

            // Blue Slider
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "B", color = Color(0xFF448AFF), modifier = Modifier.width(24.dp), fontWeight = FontWeight.Bold)
                Slider(
                    value = blue.toFloat(),
                    onValueChange = { updateCurrentColor(red, green, it.toInt()) },
                    valueRange = 0f..255f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF448AFF), activeTrackColor = Color(0xFF448AFF))
                )
                Text(text = "$blue", color = TextSecondary, modifier = Modifier.width(36.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick color chips
            Text(
                text = "Schnellauswahl-Farbtöne",
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                quickColors.forEach { qc ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(qc.toInt()))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            .clickable {
                                val r = ((qc shr 16) and 0xFF).toInt()
                                val g = ((qc shr 8) and 0xFF).toInt()
                                val b = (qc and 0xFF).toInt()
                                updateCurrentColor(r, g, b)
                            }
                    )
                }
            }
        }
    }
}
