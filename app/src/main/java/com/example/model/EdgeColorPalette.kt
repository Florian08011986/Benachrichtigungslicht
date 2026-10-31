package com.example.model

data class ColorPreset(
    val id: Int,
    val name: String,
    val category: String,
    val colors: List<Long>
)

object EdgeColorPalette {

    val PRESETS: List<ColorPreset> = listOf(
        // Cyberpunk & Neon (1 - 10)
        ColorPreset(0, "Cyber Neon", "Neon", listOf(0xFF00E5FFL, 0xFFD500F9L, 0xFFFF1744L)),
        ColorPreset(1, "Tokyo Drift", "Neon", listOf(0xFFFF0055L, 0xFF7928CAL, 0xFF00DFD8L)),
        ColorPreset(2, "Electric Cyan", "Neon", listOf(0xFF00F5D4L, 0xFF00BBF9L, 0xFF4361EEL)),
        ColorPreset(3, "Matrix Pulse", "Neon", listOf(0xFF00FF66L, 0xFF00CC44L, 0xFF003311L)),
        ColorPreset(4, "Miami Vice", "Neon", listOf(0xFFFF007FL, 0xFF00F0FFL, 0xFFFFDD00L)),
        ColorPreset(5, "Synthwave Sunset", "Neon", listOf(0xFFF72585L, 0xFF7209B7L, 0xFF3A0CA3L, 0xFF4CC9F0L)),
        ColorPreset(6, "Laser Violet", "Neon", listOf(0xFFBF00FFL, 0xFF8A2BE2L, 0xFF4B0082L)),
        ColorPreset(7, "Radioactive Lime", "Neon", listOf(0xFFCCFF00L, 0xFF00FF66L, 0xFF00FFFFL)),
        ColorPreset(8, "Vapor Purple", "Neon", listOf(0xFF9D4EDDL, 0xFFC77DFEL, 0xFFE0AAFFL)),
        ColorPreset(9, "Cyber Gold", "Neon", listOf(0xFFFFD700L, 0xFFFF8C00L, 0xFFFF0080L)),

        // Cosmic & Fantasy (11 - 20)
        ColorPreset(10, "Galaxy Nebula", "Cosmic", listOf(0xFF6B11B0L, 0xFFC026D3L, 0xFF3B82F6L)),
        ColorPreset(11, "Aurora Borealis", "Cosmic", listOf(0xFF05FFA1L, 0xFF00EAFFL, 0xFFB900FFL)),
        ColorPreset(12, "Deep Space", "Cosmic", listOf(0xFF0F0C29L, 0xFF302B63L, 0xFF24243EL, 0xFF7F00FFL)),
        ColorPreset(13, "Starlight Prism", "Cosmic", listOf(0xFFE2E8F0L, 0xFF67E8F9L, 0xFFC084FCL)),
        ColorPreset(14, "Black Hole Corona", "Cosmic", listOf(0xFFFF4500L, 0xFFFF8C00L, 0xFFFFD700L)),
        ColorPreset(15, "Cosmic Ray", "Cosmic", listOf(0xFF8B5CF6L, 0xFFEC4899L, 0xFF3B82F6L)),
        ColorPreset(16, "Supernova", "Cosmic", listOf(0xFFFFEE00L, 0xFFFF0055L, 0xFF9900EFL)),
        ColorPreset(17, "Eclipse Shadow", "Cosmic", listOf(0xFF1E1B4BL, 0xFF4338CAL, 0xFF6366F1L)),
        ColorPreset(18, "Orion Belt", "Cosmic", listOf(0xFF06B6D4L, 0xFF3B82F6L, 0xFF6366F1L)),
        ColorPreset(19, "Dark Matter", "Cosmic", listOf(0xFF4A0404L, 0xFF770737L, 0xFF9B111EL)),

        // Elements & Nature (21 - 30)
        ColorPreset(20, "Inferno Fire", "Elements", listOf(0xFFFF0000L, 0xFFFF7700L, 0xFFFFDD00L)),
        ColorPreset(21, "Deep Ocean Abyss", "Elements", listOf(0xFF002244L, 0xFF005582L, 0xFF00A896L)),
        ColorPreset(22, "Emerald Forest", "Elements", listOf(0xFF02C39AL, 0xFF00A896L, 0xFF028090L)),
        ColorPreset(23, "Volcanic Magma", "Elements", listOf(0xFF6A040FL, 0xFFD00000L, 0xFFFFBA08L)),
        ColorPreset(24, "Glacier Ice", "Elements", listOf(0xFFE0FBFCL, 0xFF98C1D9L, 0xFF3D5A80L)),
        ColorPreset(25, "Thunder Storm", "Elements", listOf(0xFF2B2D42L, 0xFF8D99AEL, 0xFFEDF2F4L, 0xFF00F5D4L)),
        ColorPreset(26, "Solar Flare", "Elements", listOf(0xFFFFBE0BL, 0xFFFB5607L, 0xFFFF006EL)),
        ColorPreset(27, "Jungle Flora", "Elements", listOf(0xFF10B981L, 0xFF84CC16L, 0xFFEAB308L)),
        ColorPreset(28, "Coral Reef", "Elements", listOf(0xFFFF6B6BL, 0xFF4ECDC4L, 0xFFFFE66DL)),
        ColorPreset(29, "Poison Ivy", "Elements", listOf(0xFF22C55EL, 0xFF15803DL, 0xFF14532DL)),

        // Pastel & Aesthetic (31 - 40)
        ColorPreset(30, "Cotton Candy", "Pastel", listOf(0xFFFFC6FFL, 0xFFBDB2FFL, 0xFFA0C4FFL)),
        ColorPreset(31, "Peach Sorbet", "Pastel", listOf(0xFFFFDAC1L, 0xFFFFB7B2L, 0xFFFF9AA2L)),
        ColorPreset(32, "Lavender Dream", "Pastel", listOf(0xFFE2C2FFL, 0xFFD0BCFFL, 0xFFCCC2DCL)),
        ColorPreset(33, "Mint Macaron", "Pastel", listOf(0xFFC7F9CCL, 0xFF80ED99L, 0xFF57CC99L)),
        ColorPreset(34, "Rose Quartz", "Pastel", listOf(0xFFFDE2E4L, 0xFFFFCAD4L, 0xFFB5E2FAL)),
        ColorPreset(35, "Warm Vanilla", "Pastel", listOf(0xFFFFE5D9L, 0xFFFFCAD4L, 0xFFF4ACB7L)),
        ColorPreset(36, "Sky Whisper", "Pastel", listOf(0xFFD8F3DCL, 0xFFB7E4C7L, 0xFF95D5B2L)),
        ColorPreset(37, "Sakura Bloom", "Pastel", listOf(0xFFFFB3C6L, 0xFFFF8FABL, 0xFFFB6F92L)),
        ColorPreset(38, "Lemon Chiffon", "Pastel", listOf(0xFFFEF9C3L, 0xFFFEF08AL, 0xFFFACC15L)),
        ColorPreset(39, "Lilac Mist", "Pastel", listOf(0xFFDDD6FEL, 0xFFC4B5FDL, 0xFFA78BFAL)),

        // Vibrant & Exotic (41 - 48)
        ColorPreset(40, "Rainbow Spectrum", "Vibrant", listOf(0xFFFF0000L, 0xFFFFFF00L, 0xFF00FF00L, 0xFF00FFFFL, 0xFF0000FFL, 0xFFFF00FFL)),
        ColorPreset(41, "Blood Crimson", "Vibrant", listOf(0xFF990000L, 0xFFFF0033L, 0xFFFF5555L)),
        ColorPreset(42, "Acid Yellow", "Vibrant", listOf(0xFFE2FF00L, 0xFFFFD600L, 0xFFFF8500L)),
        ColorPreset(43, "Royal Amethyst", "Vibrant", listOf(0xFF4A0E4EL, 0xFF880E4FL, 0xFFE91E63L)),
        ColorPreset(44, "Hyper Orange", "Vibrant", listOf(0xFFFF5722L, 0xFFFF9800L, 0xFFFFC107L)),
        ColorPreset(45, "Glitch Cyan Magenta", "Vibrant", listOf(0xFF00FFFFL, 0xFF000000L, 0xFFFF00FFL)),
        ColorPreset(46, "Golden Dragon", "Vibrant", listOf(0xFFFFD700L, 0xFFFF4500L, 0xFFB22222L)),
        ColorPreset(47, "Quantum Plasma", "Vibrant", listOf(0xFF7928CAL, 0xFFFF0080L, 0xFF0070F3L))
    )

    fun getPreset(id: Int): ColorPreset {
        return PRESETS.firstOrNull { it.id == id } ?: PRESETS[0]
    }
}
