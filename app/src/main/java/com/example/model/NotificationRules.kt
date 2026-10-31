package com.example.model

data class AppEdgeRule(
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean = true,
    val paletteId: Int = 0,
    val useCustomColors: Boolean = false,
    val customColors: List<Long> = emptyList(),
    val shapeType: BorderShapeType = BorderShapeType.CONTINUOUS_LINE,
    val customEmoji: String = "",
    val durationSeconds: Int = 6
)

data class ContactEdgeRule(
    val id: String,
    val contactIdentifier: String, // Contact Name or Phone Number substring (e.g. "Mama", "+49...")
    val isEnabled: Boolean = true,
    val paletteId: Int = 41, // e.g. Crimson Red or custom
    val useCustomColors: Boolean = false,
    val customColors: List<Long> = emptyList(),
    val shapeType: BorderShapeType = BorderShapeType.HEART,
    val customEmoji: String = "💖",
    val durationSeconds: Int = 8
)

data class NotificationEdgeSettings(
    val isNotificationAlertEnabled: Boolean = true,
    val wakeScreenOnNotification: Boolean = true,
    val defaultAlertDurationSeconds: Int = 6,
    val onlyWhenScreenOff: Boolean = false,
    val repeatPulseCount: Int = 1
)

object DefaultAppRules {
    fun getDefaultRules(): List<AppEdgeRule> = listOf(
        AppEdgeRule(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            isEnabled = true,
            paletteId = 3, // Matrix / Neon Green
            shapeType = BorderShapeType.CONTINUOUS_LINE,
            durationSeconds = 6
        ),
        AppEdgeRule(
            packageName = "org.telegram.messenger",
            appName = "Telegram",
            isEnabled = true,
            paletteId = 2, // Electric Cyan
            shapeType = BorderShapeType.CONTINUOUS_LINE,
            durationSeconds = 6
        ),
        AppEdgeRule(
            packageName = "com.instagram.android",
            appName = "Instagram",
            isEnabled = true,
            paletteId = 5, // Synthwave Sunset / Magenta
            shapeType = BorderShapeType.CONTINUOUS_LINE,
            durationSeconds = 6
        ),
        AppEdgeRule(
            packageName = "com.google.android.apps.messaging",
            appName = "Messages (SMS)",
            isEnabled = true,
            paletteId = 0, // Cyber Neon
            shapeType = BorderShapeType.STAR,
            durationSeconds = 6
        ),
        AppEdgeRule(
            packageName = "com.google.android.gm",
            appName = "Gmail",
            isEnabled = true,
            paletteId = 41, // Blood Crimson
            shapeType = BorderShapeType.CONTINUOUS_LINE,
            durationSeconds = 5
        ),
        AppEdgeRule(
            packageName = "com.snapchat.android",
            appName = "Snapchat",
            isEnabled = true,
            paletteId = 42, // Acid Yellow
            shapeType = BorderShapeType.SUN,
            durationSeconds = 6
        )
    )
}
