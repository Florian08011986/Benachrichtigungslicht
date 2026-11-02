package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.InstalledApps

/** Eine Zeile der App-Farbenliste: Icon, Name, aktuelle Farbe (oder „Grundfarbe“). */
@Composable
fun AppColorRow(
    packageName: String?,
    label: String,
    color: Long?,
    fallbackColor: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val icon = remember(packageName) {
        packageName?.let { InstalledApps.icon(context, it)?.toBitmap(96, 96)?.asImageBitmap() }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("app_color_${packageName ?: "calls"}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(32.dp))
        } else {
            Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Call, contentDescription = null, tint = NeonCyan)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (color != null) "Eigene Farbe" else "Grundfarbe des Rings",
                color = if (color != null) TextSecondary else TextMuted,
                style = MaterialTheme.typography.labelSmall
            )
        }
        ColorDot(color ?: fallbackColor)
    }
}

/** Dialog: Farbe einer App bzw. der Anrufe wählen, testen oder zurücksetzen. */
@Composable
fun AppColorDialog(
    title: String,
    current: Long?,
    allowReset: Boolean,
    onSelect: (Long) -> Unit,
    onReset: () -> Unit,
    onTest: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Farbe wählen – wirkt sofort.", style = MaterialTheme.typography.bodySmall)
                ColorSwatches(selected = current, onSelect = onSelect)
                Spacer(modifier = Modifier.height(4.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = { current?.let(onTest) }, enabled = current != null) { Text("Am Display testen") }
        },
        dismissButton = {
            Row {
                if (allowReset && current != null) {
                    TextButton(onClick = onReset) { Text("Grundfarbe") }
                }
                TextButton(onClick = onDismiss) { Text("Fertig") }
            }
        }
    )
}
