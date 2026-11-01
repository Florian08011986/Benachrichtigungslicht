package com.example.model

/** Visualisierung entlang des Bildschirmrands – ausschließlich linienförmig. */
enum class LineStyle(val displayName: String, val description: String) {
    RING("Ring", "Ruhiger, durchgehender Rahmen in einer Farbe"),
    AURA("KI-Aura", "Keine Kontur – Licht zieht weich ins Display")
}

/** Wann eine Benachrichtigung den Rand aufleuchten lässt. */
enum class LightWhen(val displayName: String) {
    SCREEN_ACTIVE("Bei aktivem Display"),
    LOCKED("Im Sperrmodus"),
    BOTH("Beides")
}

/** Auswahl-Farbfelder für Ring-, App- und Anruf-Farben (je eine Farbe). */
val COLOR_SWATCHES: List<Long> = listOf(
    0xFF25D366L, // Grün
    0xFF00C853L, // Smaragd
    0xFF00E5FFL, // Cyan
    0xFF2F7BFFL, // Blau
    0xFF1A3DFFL, // Königsblau
    0xFF7C4DFFL, // Violett
    0xFFD500F9L, // Magenta
    0xFFFF4081L, // Pink
    0xFFFF1744L, // Rot
    0xFFFF6D00L, // Orange
    0xFFD97757L, // Terrakotta
    0xFFFFC400L, // Gelb
    0xFFFFFFFFL  // Weiß
)

enum class NotchType(val displayName: String) {
    NONE("Keine (Gerader Rand)"),
    INFINITY_U("Infinity-U Notch"),
    INFINITY_V("Infinity-V Notch"),
    PUNCH_HOLE_CENTER("Kamera-Loch Mitte"),
    PUNCH_HOLE_LEFT("Kamera-Loch Links"),
    PILL_ISLAND("Pille / Dynamische Insel")
}

enum class AnimationMode(val displayName: String) {
    ROTATE("Bewegen"),
    PULSE("Pulsieren"),
    ROTATE_AND_PULSE("Bewegen + Pulsieren")
}

/** Verlaufskurve eines Pulses (Eingang: Phase 0..1, Ausgang: Pegel 0..1). */
enum class PulseCurve(val displayName: String, val description: String) {
    SINE("Sanft", "Gleichmäßige Sinuswelle"),
    BREATH("Atmen", "Ein- und Ausatmen mit kurzer Ruhe oben und unten"),
    GLIMMER("Glimmen", "Ganz leichtes, ruhiges Auf und Ab")
}

/** Komplettprofil: Stil + Farbe + Bewegung + Rand in einem Tipp. */
data class LightProfile(
    val id: String,
    val name: String,
    val description: String,
    val paletteId: Int,
    val lineStyle: LineStyle,
    val animationMode: AnimationMode,
    val animationSpeed: Float,
    val pulseSpeed: Float,
    val pulseCurve: PulseCurve,
    val pulseStrength: Float,
    val borderThickness: Float,
    val glowReach: Float,
    val glowEnabled: Boolean = true
)

object LightProfiles {
    val ALL: List<LightProfile> = listOf(
        LightProfile(
            id = "ki_atem_blau",
            name = "KI-Atem Blau",
            description = "Blau-violette Aura zieht weich ins Display, Farben wandern langsam, atmet ca. alle 2,5 s",
            paletteId = 48,
            lineStyle = LineStyle.AURA,
            animationMode = AnimationMode.ROTATE_AND_PULSE,
            animationSpeed = 0.9f,
            pulseSpeed = 0.4f,
            pulseCurve = PulseCurve.BREATH,
            pulseStrength = 0.55f,
            borderThickness = 10f,
            glowReach = 160f
        ),
        LightProfile(
            id = "agenten_glow",
            name = "Agenten-Glow",
            description = "Türkis-blauer, randloser Schein, steht still und atmet ruhig ca. alle 3 s",
            paletteId = 49,
            lineStyle = LineStyle.AURA,
            animationMode = AnimationMode.PULSE,
            animationSpeed = 1f,
            pulseSpeed = 0.33f,
            pulseCurve = PulseCurve.BREATH,
            pulseStrength = 0.45f,
            borderThickness = 8f,
            glowReach = 110f
        )
    )
}

/** Eigenes, neutrales Spektrum für die erste Stufe des Zwei-Stufen-Starts. */
val TWO_STAGE_INTRO_COLORS: List<Long> = listOf(0xFFFF5A6EL, 0xFFFFC23DL, 0xFF35D49AL, 0xFF4C8DFFL)

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
    val opacityPercent: Int = 100,      // 10..100 % Deckkraft (100 = verdeckt Inhalt vollständig)
    val selectedPaletteId: Int = 0,
    val useCustomColors: Boolean = false,
    val customColors: List<Long> = listOf(0xFF00E5FFL, 0xFFD500F9L, 0xFFFF1744L),
    val lineStyle: LineStyle = LineStyle.RING,
    val ringColor: Long = 0xFF00E5FFL,  // Grundfarbe des Rings (Apps ohne eigene Farbe)
    val callColor: Long = 0xFF2F7BFFL,  // Farbe bei eingehenden Anrufen
    val glowReach: Float = 120f,        // px, wie weit Glow/Aura ins Display reicht
    val borderThickness: Float = 18f,   // px
    val animationSpeed: Float = 5f,     // 0.2f..20f (Bewegungstempo, 1x = 36°/s)
    val animationMode: AnimationMode = AnimationMode.ROTATE,
    val pulseSpeed: Float = 0.5f,       // Pulse pro Sekunde, 0.125f..2f
    val pulseCurve: PulseCurve = PulseCurve.SINE,
    val pulseStrength: Float = 0.6f,    // 0.1..1.0, Anteil der Helligkeit, der beim Puls abtaucht
    val isLivePreviewEnabled: Boolean = true,
    val isProcessingPulseEnabled: Boolean = false,
    val isTwoStageStartEnabled: Boolean = false, // beim Auslösen kurz Mehrfarb-Spektrum, dann Zielfarben
    val cornerRadiusTop: Float = 70f,   // px
    val cornerRadiusBottom: Float = 70f,// px
    val notchConfig: NotchConfig = NotchConfig(),
    val isGlowEnabled: Boolean = true,
    val isCallerAlertActive: Boolean = false, // Set when phone is ringing
    val colorOverride: Long? = null       // Laufzeit: Einzelfarbe für Alarm (App/Anruf), nicht gespeichert
)
