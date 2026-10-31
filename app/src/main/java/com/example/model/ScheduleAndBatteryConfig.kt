package com.example.model

import java.util.Calendar

data class EdgeScheduleConfig(
    val isScheduleEnabled: Boolean = false,
    val startHour: Int = 22,
    val startMinute: Int = 0,
    val endHour: Int = 7,
    val endMinute: Int = 0,
    val activeDays: Set<Int> = setOf(
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
        Calendar.SATURDAY,
        Calendar.SUNDAY
    )
) {
    /**
     * Prüft, ob der aktuelle Zeitpunkt innerhalb der konfigurierten Ruhezeit liegt.
     */
    fun isCurrentlyInDnd(calendar: Calendar = Calendar.getInstance()): Boolean {
        if (!isScheduleEnabled) return false

        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
        if (!activeDays.contains(currentDay)) return false

        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute

        return if (startMinutes <= endMinutes) {
            // Gleicher Tag (z. B. 09:00 bis 17:00)
            currentMinutes in startMinutes..endMinutes
        } else {
            // Über Mitternacht (z. B. 22:00 bis 07:00)
            currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }
}

data class BatterySaverConfig(
    val isBatterySaverEnabled: Boolean = true,
    val triggerThresholdPercent: Int = 20, // 5% .. 50%
    val reduceFpsTo30: Boolean = true,     // Reduziert von 60/120 FPS auf 30 FPS
    val dimIntensityPercent: Int = 50,     // 20% .. 80%
    val disableGlowFilter: Boolean = true  // Schaltet MaskFilter/Aura zur GPU-Schonung ab
) {
    fun shouldApplyBatterySaver(
        currentLevel: Int,
        isPowerSaveMode: Boolean,
        isCharging: Boolean
    ): Boolean {
        if (!isBatterySaverEnabled) return false
        if (isCharging) return false // Beim Laden volle Leistung
        return isPowerSaveMode || (currentLevel in 1..triggerThresholdPercent)
    }
}

data class BatteryStatus(
    val levelPercent: Int = 100,
    val isCharging: Boolean = false,
    val isPowerSaveMode: Boolean = false
)
