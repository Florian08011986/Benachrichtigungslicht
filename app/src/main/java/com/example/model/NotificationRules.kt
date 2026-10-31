package com.example.model

data class AppEdgeRule(
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean = true,
    val paletteId: Int = 0,
    val useCustomColors: Boolean = false,
    val customColors: List<Long> = emptyList(),
    val lineStyle: LineStyle = LineStyle.RING,
    val durationSeconds: Int = 6
)

data class ContactEdgeRule(
    val id: String,
    val contactIdentifier: String, // Contact Name or Phone Number substring (e.g. "Mama", "+49...")
    val isEnabled: Boolean = true,
    val paletteId: Int = 41, // e.g. Crimson Red or custom
    val useCustomColors: Boolean = false,
    val customColors: List<Long> = emptyList(),
    val lineStyle: LineStyle = LineStyle.COMET,
    val durationSeconds: Int = 8
)

data class NotificationEdgeSettings(
    val isNotificationAlertEnabled: Boolean = true,
    val wakeScreenOnNotification: Boolean = true,
    val defaultAlertDurationSeconds: Int = 6,
    val onlyWhenScreenOff: Boolean = false,
    val repeatPulseCount: Int = 1,
    val pulseUntilDismissed: Boolean = false // Rand atmet weiter, bis die Benachrichtigung entfernt ist
)

object DefaultAppRules {
    fun getDefaultRules(): List<AppEdgeRule> = listOf(
        AppEdgeRule(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            isEnabled = true,
            paletteId = 3, // Matrix / Neon Green
            lineStyle = LineStyle.RING,
            durationSeconds = 6
        ),
        AppEdgeRule(
            packageName = "org.telegram.messenger",
            appName = "Telegram",
            isEnabled = true,
            paletteId = 2, // Electric Cyan
            lineStyle = LineStyle.RING,
            durationSeconds = 6
        ),
        AppEdgeRule(
            packageName = "com.instagram.android",
            appName = "Instagram",
            isEnabled = true,
            paletteId = 5, // Synthwave Sunset / Magenta
            lineStyle = LineStyle.RING,
            durationSeconds = 6
        ),
        AppEdgeRule(
            packageName = "com.google.android.apps.messaging",
            appName = "Messages (SMS)",
            isEnabled = true,
            paletteId = 0, // Cyber Neon
            lineStyle = LineStyle.COMET,
            durationSeconds = 6
        ),
        AppEdgeRule(
            packageName = "com.google.android.gm",
            appName = "Gmail",
            isEnabled = true,
            paletteId = 41, // Blood Crimson
            lineStyle = LineStyle.RING,
            durationSeconds = 5
        ),
        AppEdgeRule(
            packageName = "com.snapchat.android",
            appName = "Snapchat",
            isEnabled = true,
            paletteId = 42, // Acid Yellow
            lineStyle = LineStyle.RUNNING_LIGHTS,
            durationSeconds = 6
        )
    )
}
