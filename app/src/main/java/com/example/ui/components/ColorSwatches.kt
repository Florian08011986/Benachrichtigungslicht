package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.COLOR_SWATCHES

/** Auswahl genau einer Farbe aus festen Farbfeldern. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorSwatches(
    selected: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
    colors: List<Long> = COLOR_SWATCHES
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        colors.forEach { c ->
            val isSelected = selected == c
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(c))
                    .border(if (isSelected) 3.dp else 1.dp, if (isSelected) Color.White else Color(0x33FFFFFF), CircleShape)
                    .clickable { onSelect(c) }
                    .testTag("swatch_${java.lang.Long.toHexString(c)}"),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Ausgewählt",
                        tint = if (c == 0xFFFFFFFFL) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/** Kleiner Farbpunkt zur Anzeige der aktuellen Farbe. */
@Composable
fun ColorDot(color: Long?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (color != null) Color(color) else Color.Transparent)
            .border(1.dp, Color(0x55FFFFFF), CircleShape)
    )
}
