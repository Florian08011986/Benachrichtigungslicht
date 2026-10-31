package com.example.model

enum class BorderShapeType(val displayName: String, val iconLabel: String) {
    CONTINUOUS_LINE("Dauerhafter Leuchtstreifen", "━"),
    HEART("Herzen", "💖"),
    STAR("Sterne", "⭐"),
    DIAMOND("Diamanten", "💎"),
    SUN("Sonnen", "🌞"),
    EMOJI_COOL("Coole Brille", "😎"),
    EMOJI_SLEEP("Schlaf-Zzz", "💤"),
    CUSTOM_EMOJI("Eigenes Emoji", "✨")
}

enum class NotchType(val displayName: String) {
    NONE("Keine (Gerader Rand)"),
    INFINITY_U("Infinity-U Notch"),
    INFINITY_V("Infinity-V Notch"),
    PUNCH_HOLE_CENTER("Kamera-Loch Mitte"),
    PUNCH_HOLE_LEFT("Kamera-Loch Links"),
    PILL_ISLAND("Pille / Dynamische Insel")
}

enum class AnimationMode(val displayName: String) {
    ROTATE("Drehen"),
    PULSE("Pulsieren"),
    ROTATE_AND_PULSE("Drehen + Pulsieren")
}

/** Feste Blau-Palette für das „Verarbeitung läuft“-Pulsieren. */
val PROCESSING_PULSE_COLORS: List<Long> = listOf(0xFF1E6BFFL, 0xFF00C2FFL, 0xFF6A5CFFL)

data class NotchConfig(
    val notchType: NotchType = NotchType.NONE,
    val notchWidth: Float = 160f,       // px in 0..400
    val notchHeight: Float = 80f,       // px in 0..250
    val notchOffsetX: Float = 0f,       // px offset from center/left
    val notchOffsetY: Float = 0f,       // px offset from top
    val notchRadius: Float = 24f        // corner radius of notch/hole
)

data class EdgeConfig(
    val isOverlayEnabled: Boolean = false,
    val isCallerIdAlertEnabled: Boolean = false,
    val brightnessPercent: Int = 100,   // 10..100%
    val selectedPaletteId: Int = 0,
    val useCustomColors: Boolean = false,
    val customColors: List<Long> = listOf(0xFF00E5FFL, 0xFFD500F9L, 0xFFFF1744L),
    val shapeType: BorderShapeType = BorderShapeType.CONTINUOUS_LINE,
    val customEmoji: String = "⚡",
    val shapeSize: Float = 36f,         // px
    val shapeSpacing: Float = 42f,      // px between shapes
    val borderThickness: Float = 18f,   // px
    val animationSpeed: Float = 5f,     // 0.2f..20f (Drehgeschwindigkeit, 1x = 36°/s)
    val animationMode: AnimationMode = AnimationMode.ROTATE,
    val pulseSpeed: Float = 0.5f,       // Pulse pro Sekunde, 0.125f..2f
    val isLivePreviewEnabled: Boolean = true,
    val isProcessingPulseEnabled: Boolean = false,
    val cornerRadiusTop: Float = 70f,   // px
    val cornerRadiusBottom: Float = 70f,// px
    val notchConfig: NotchConfig = NotchConfig(),
    val isGlowEnabled: Boolean = true,
    val isCallerAlertActive: Boolean = false // Set when phone is ringing
)
