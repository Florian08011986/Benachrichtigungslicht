package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkOledBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

/**
 * Ersetzt die Mini-Handy-Vorschau: Schaltet die echte Randbeleuchtung am Display ein,
 * solange die App sichtbar ist. Jede Änderung wirkt sofort am realen Bildschirmrand.
 */
@Composable
fun LivePreviewCard(
    livePreviewEnabled: Boolean,
    hasOverlayAccess: Boolean,
    onToggle: (Boolean) -> Unit,
    onRequestOverlayAccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_preview_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Live-Vorschau am Display",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (livePreviewEnabled && hasOverlayAccess)
                            "Aktiv – jede Änderung siehst du sofort am echten Bildschirmrand."
                        else
                            "Zeigt Einstellungen direkt an deinem Handy-Rand, solange die App offen ist.",
                        color = TextMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Switch(
                    checked = livePreviewEnabled && hasOverlayAccess,
                    onCheckedChange = { checked ->
                        if (checked && !hasOverlayAccess) onRequestOverlayAccess() else onToggle(checked)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonCyan,
                        checkedTrackColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.testTag("live_preview_switch")
                )
            }

            if (!hasOverlayAccess) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRequestOverlayAccess,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkOledBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("„Über anderen Apps einblenden“ erlauben", fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Beim Verlassen der App endet die Vorschau automatisch.",
                    color = Color(0xFF64748B),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
