package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AnimationMode
import com.example.model.AppEdgeRule
import com.example.model.BatterySaverConfig
import com.example.model.ContactEdgeRule
import com.example.model.DefaultAppRules
import com.example.model.EdgeConfig
import com.example.model.EdgeScheduleConfig
import com.example.model.LightProfile
import com.example.model.LightWhen
import com.example.model.LineStyle
import com.example.model.PulseCurve
import com.example.model.NotchConfig
import com.example.model.NotchType
import com.example.model.NotificationEdgeSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "edge_lighting_preferences")

/** Flüchtiger Overlay-Status: Live-Vorschau (App im Vordergrund) und laufende Vorgänge. */
data class OverlayRuntimeState(
    val previewActive: Boolean = false,
    val processingActive: Boolean = false,
    val processingTestActive: Boolean = false,
    val introNonce: Long = 0L // jede Erhöhung spielt den Zwei-Stufen-Start einmal ab
)

class EdgePreferencesRepository private constructor(context: Context) {

    private val applicationContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Temporary caller ID alert override flag (in-memory)
    private val _callerAlertActive = MutableStateFlow(false)
    val callerAlertActive: StateFlow<Boolean> = _callerAlertActive.asStateFlow()

    fun setCallerAlertActive(active: Boolean) {
        _callerAlertActive.value = active
    }

    // Flüchtiger Laufzeitstatus (nicht persistiert): Live-Vorschau & Verarbeitungs-Puls
    private val _runtimeState = MutableStateFlow(OverlayRuntimeState())
    val runtimeState: StateFlow<OverlayRuntimeState> = _runtimeState.asStateFlow()

    fun setPreviewActive(active: Boolean) {
        _runtimeState.value = _runtimeState.value.copy(previewActive = active)
    }

    fun setProcessingActive(active: Boolean) {
        _runtimeState.value = _runtimeState.value.copy(processingActive = active)
    }

    fun setProcessingTestActive(active: Boolean) {
        _runtimeState.value = _runtimeState.value.copy(processingTestActive = active)
    }

    fun requestIntroReplay() {
        _runtimeState.value = _runtimeState.value.copy(introNonce = _runtimeState.value.introNonce + 1)
    }

    // Benachrichtigungen, deren Licht bis zum Wegwischen läuft (Schlüssel = StatusBarNotification.key)
    private val pendingAlertKeys = mutableSetOf<String>()

    @Synchronized
    fun addPendingAlertKey(key: String) {
        pendingAlertKeys.add(key)
    }

    /** Entfernt den Schlüssel; liefert true, wenn danach keine offene Benachrichtigung mehr leuchten soll. */
    @Synchronized
    fun removePendingAlertKey(key: String): Boolean {
        val removed = pendingAlertKeys.remove(key)
        return removed && pendingAlertKeys.isEmpty()
    }

    @Synchronized
    fun clearPendingAlertKeys() {
        pendingAlertKeys.clear()
    }

    object Keys {
        val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val CALLER_ID_ENABLED = booleanPreferencesKey("caller_id_enabled")
        val PALETTE_ID = intPreferencesKey("palette_id")
        val USE_CUSTOM_COLORS = booleanPreferencesKey("use_custom_colors")
        val CUSTOM_COLORS_CSV = stringPreferencesKey("custom_colors_csv")
        val LINE_STYLE = stringPreferencesKey("line_style")
        val RING_COLOR = longPreferencesKey("ring_color")
        val CALL_COLOR = longPreferencesKey("call_color")
        val GLOW_REACH = floatPreferencesKey("glow_reach")
        val PULSE_CURVE = stringPreferencesKey("pulse_curve")
        val PULSE_STRENGTH = floatPreferencesKey("pulse_strength")
        val BORDER_THICKNESS = floatPreferencesKey("border_thickness")
        val ANIMATION_SPEED = floatPreferencesKey("animation_speed")
        val CORNER_RADIUS_TOP = floatPreferencesKey("corner_radius_top")
        val CORNER_RADIUS_BOTTOM = floatPreferencesKey("corner_radius_bottom")
        val NOTCH_TYPE = stringPreferencesKey("notch_type")
        val NOTCH_WIDTH = floatPreferencesKey("notch_width")
        val NOTCH_HEIGHT = floatPreferencesKey("notch_height")
        val NOTCH_OFFSET_X = floatPreferencesKey("notch_offset_x")
        val NOTCH_OFFSET_Y = floatPreferencesKey("notch_offset_y")
        val NOTCH_RADIUS = floatPreferencesKey("notch_radius")
        val GLOW_ENABLED = booleanPreferencesKey("glow_enabled")
        val OPACITY_PERCENT = intPreferencesKey("brightness_percent") // Schlüssel beibehalten: bisheriger Wert wird übernommen
        val ANIMATION_MODE = stringPreferencesKey("animation_mode")
        val PULSE_SPEED = floatPreferencesKey("pulse_speed")
        val LIVE_PREVIEW_ENABLED = booleanPreferencesKey("live_preview_enabled")
        val PROCESSING_PULSE_ENABLED = booleanPreferencesKey("processing_pulse_enabled")
        val TWO_STAGE_START = booleanPreferencesKey("two_stage_start")

        // Notification Edge Light Keys
        val NOTIF_ALERT_ENABLED = booleanPreferencesKey("notif_alert_enabled")
        val NOTIF_WAKE_SCREEN = booleanPreferencesKey("notif_wake_screen")
        val NOTIF_DURATION = intPreferencesKey("notif_duration")
        val NOTIF_ONLY_SCREEN_OFF = booleanPreferencesKey("notif_only_screen_off")
        val NOTIF_UNTIL_DISMISSED = booleanPreferencesKey("notif_until_dismissed")
        val NOTIF_LIGHT_WHEN = stringPreferencesKey("notif_light_when")
        val APP_RULES_RAW = stringPreferencesKey("app_rules_raw")
        val CONTACT_RULES_RAW = stringPreferencesKey("contact_rules_raw")

        // Schedule / DND Keys
        val SCHEDULE_ENABLED = booleanPreferencesKey("schedule_enabled")
        val SCHEDULE_START_HOUR = intPreferencesKey("schedule_start_hour")
        val SCHEDULE_START_MINUTE = intPreferencesKey("schedule_start_minute")
        val SCHEDULE_END_HOUR = intPreferencesKey("schedule_end_hour")
        val SCHEDULE_END_MINUTE = intPreferencesKey("schedule_end_minute")
        val SCHEDULE_DAYS_CSV = stringPreferencesKey("schedule_days_csv")

        // Battery Saver Keys
        val BATTERY_SAVER_ENABLED = booleanPreferencesKey("battery_saver_enabled")
        val BATTERY_THRESHOLD = intPreferencesKey("battery_threshold")
        val BATTERY_REDUCE_FPS = booleanPreferencesKey("battery_reduce_fps")
        val BATTERY_DIM_INTENSITY = intPreferencesKey("battery_dim_intensity")
        val BATTERY_DISABLE_GLOW = booleanPreferencesKey("battery_disable_glow")
    }

    val configFlow: StateFlow<EdgeConfig> = applicationContext.dataStore.data
        .map { prefs ->
            val customColorsList = prefs[Keys.CUSTOM_COLORS_CSV]
                ?.split(",")
                ?.mapNotNull { it.trim().toLongOrNull() }
                ?.takeIf { it.isNotEmpty() }
                ?: listOf(0xFF00E5FFL, 0xFFD500F9L, 0xFFFF1744L)

            val lineStyle = parseEnum(prefs[Keys.LINE_STYLE], LineStyle.RING)

            val notchType = try {
                NotchType.valueOf(prefs[Keys.NOTCH_TYPE] ?: NotchType.NONE.name)
            } catch (_: Exception) {
                NotchType.NONE
            }

            val notchConfig = NotchConfig(
                notchType = notchType,
                notchWidth = prefs[Keys.NOTCH_WIDTH] ?: 160f,
                notchHeight = prefs[Keys.NOTCH_HEIGHT] ?: 80f,
                notchOffsetX = prefs[Keys.NOTCH_OFFSET_X] ?: 0f,
                notchOffsetY = prefs[Keys.NOTCH_OFFSET_Y] ?: 0f,
                notchRadius = prefs[Keys.NOTCH_RADIUS] ?: 24f
            )

            EdgeConfig(
                isOverlayEnabled = prefs[Keys.OVERLAY_ENABLED] ?: false,
                isCallerIdAlertEnabled = prefs[Keys.CALLER_ID_ENABLED] ?: false,
                opacityPercent = prefs[Keys.OPACITY_PERCENT] ?: 100,
                selectedPaletteId = prefs[Keys.PALETTE_ID] ?: 0,
                useCustomColors = prefs[Keys.USE_CUSTOM_COLORS] ?: false,
                customColors = customColorsList,
                lineStyle = lineStyle,
                ringColor = prefs[Keys.RING_COLOR] ?: 0xFF00E5FFL,
                callColor = prefs[Keys.CALL_COLOR] ?: 0xFF2F7BFFL,
                glowReach = prefs[Keys.GLOW_REACH] ?: 120f,
                borderThickness = prefs[Keys.BORDER_THICKNESS] ?: 18f,
                animationSpeed = prefs[Keys.ANIMATION_SPEED] ?: 5f,
                animationMode = try {
                    AnimationMode.valueOf(prefs[Keys.ANIMATION_MODE] ?: AnimationMode.ROTATE.name)
                } catch (_: Exception) {
                    AnimationMode.ROTATE
                },
                pulseSpeed = prefs[Keys.PULSE_SPEED] ?: 0.5f,
                pulseCurve = parseEnum(prefs[Keys.PULSE_CURVE], PulseCurve.SINE),
                pulseStrength = prefs[Keys.PULSE_STRENGTH] ?: 0.6f,
                isLivePreviewEnabled = prefs[Keys.LIVE_PREVIEW_ENABLED] ?: true,
                isProcessingPulseEnabled = prefs[Keys.PROCESSING_PULSE_ENABLED] ?: false,
                isTwoStageStartEnabled = prefs[Keys.TWO_STAGE_START] ?: false,
                cornerRadiusTop = prefs[Keys.CORNER_RADIUS_TOP] ?: 70f,
                cornerRadiusBottom = prefs[Keys.CORNER_RADIUS_BOTTOM] ?: 70f,
                notchConfig = notchConfig,
                isGlowEnabled = prefs[Keys.GLOW_ENABLED] ?: true,
                isCallerAlertActive = _callerAlertActive.value
            )
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = EdgeConfig()
        )

    val notificationSettingsFlow: StateFlow<NotificationEdgeSettings> = applicationContext.dataStore.data
        .map { prefs ->
            NotificationEdgeSettings(
                isNotificationAlertEnabled = prefs[Keys.NOTIF_ALERT_ENABLED] ?: true,
                wakeScreenOnNotification = prefs[Keys.NOTIF_WAKE_SCREEN] ?: true,
                defaultAlertDurationSeconds = prefs[Keys.NOTIF_DURATION] ?: 6,
                onlyWhenScreenOff = prefs[Keys.NOTIF_ONLY_SCREEN_OFF] ?: false,
                // Übernahme der alten Option „nur bei ausgeschaltetem Bildschirm“ → Sperrmodus
                lightWhen = parseEnum(
                    prefs[Keys.NOTIF_LIGHT_WHEN],
                    if (prefs[Keys.NOTIF_ONLY_SCREEN_OFF] == true) LightWhen.LOCKED else LightWhen.BOTH
                ),
                pulseUntilDismissed = prefs[Keys.NOTIF_UNTIL_DISMISSED] ?: false
            )
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = NotificationEdgeSettings()
        )

    val appRulesFlow: StateFlow<List<AppEdgeRule>> = applicationContext.dataStore.data
        .map { prefs ->
            val raw = prefs[Keys.APP_RULES_RAW]
            if (raw.isNullOrBlank()) {
                DefaultAppRules.getDefaultRules()
            } else {
                deserializeAppRules(raw)
            }
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = DefaultAppRules.getDefaultRules()
        )

    val contactRulesFlow: StateFlow<List<ContactEdgeRule>> = applicationContext.dataStore.data
        .map { prefs ->
            val raw = prefs[Keys.CONTACT_RULES_RAW]
            if (raw.isNullOrBlank()) {
                emptyList()
            } else {
                deserializeContactRules(raw)
            }
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val scheduleConfigFlow: StateFlow<EdgeScheduleConfig> = applicationContext.dataStore.data
        .map { prefs ->
            val daysCsv = prefs[Keys.SCHEDULE_DAYS_CSV]
            val daysSet = if (daysCsv.isNullOrBlank()) {
                setOf(
                    Calendar.MONDAY,
                    Calendar.TUESDAY,
                    Calendar.WEDNESDAY,
                    Calendar.THURSDAY,
                    Calendar.FRIDAY,
                    Calendar.SATURDAY,
                    Calendar.SUNDAY
                )
            } else {
                daysCsv.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
            }

            EdgeScheduleConfig(
                isScheduleEnabled = prefs[Keys.SCHEDULE_ENABLED] ?: false,
                startHour = prefs[Keys.SCHEDULE_START_HOUR] ?: 22,
                startMinute = prefs[Keys.SCHEDULE_START_MINUTE] ?: 0,
                endHour = prefs[Keys.SCHEDULE_END_HOUR] ?: 7,
                endMinute = prefs[Keys.SCHEDULE_END_MINUTE] ?: 0,
                activeDays = daysSet
            )
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = EdgeScheduleConfig()
        )

    val batterySaverConfigFlow: StateFlow<BatterySaverConfig> = applicationContext.dataStore.data
        .map { prefs ->
            BatterySaverConfig(
                isBatterySaverEnabled = prefs[Keys.BATTERY_SAVER_ENABLED] ?: true,
                triggerThresholdPercent = prefs[Keys.BATTERY_THRESHOLD] ?: 20,
                reduceFpsTo30 = prefs[Keys.BATTERY_REDUCE_FPS] ?: true,
                dimIntensityPercent = prefs[Keys.BATTERY_DIM_INTENSITY] ?: 50,
                disableGlowFilter = prefs[Keys.BATTERY_DISABLE_GLOW] ?: true
            )
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = BatterySaverConfig()
        )

    fun updateOverlayEnabled(enabled: Boolean) {
        scope.launch {
            applicationContext.dataStore.edit { it[Keys.OVERLAY_ENABLED] = enabled }
        }
    }

    fun updateCallerIdEnabled(enabled: Boolean) {
        scope.launch {
            applicationContext.dataStore.edit { it[Keys.CALLER_ID_ENABLED] = enabled }
        }
    }

    fun updateOpacity(percent: Int) {
        val clamped = percent.coerceIn(10, 100)
        scope.launch {
            applicationContext.dataStore.edit { it[Keys.OPACITY_PERCENT] = clamped }
        }
    }

    fun updatePalette(paletteId: Int) {
        scope.launch {
            applicationContext.dataStore.edit {
                it[Keys.PALETTE_ID] = paletteId
                it[Keys.USE_CUSTOM_COLORS] = false
            }
        }
    }

    fun updateCustomColors(colors: List<Long>) {
        scope.launch {
            applicationContext.dataStore.edit {
                it[Keys.CUSTOM_COLORS_CSV] = colors.joinToString(",")
                it[Keys.USE_CUSTOM_COLORS] = true
            }
        }
    }

    fun updateLineStyle(
        lineStyle: LineStyle? = null,
        glowReach: Float? = null
    ) {
        scope.launch {
            applicationContext.dataStore.edit { prefs ->
                lineStyle?.let { prefs[Keys.LINE_STYLE] = it.name }
                glowReach?.let { prefs[Keys.GLOW_REACH] = it }
            }
        }
    }

    fun updateGeometry(
        borderThickness: Float? = null,
        animationSpeed: Float? = null,
        cornerRadiusTop: Float? = null,
        cornerRadiusBottom: Float? = null,
        glowEnabled: Boolean? = null
    ) {
        scope.launch {
            applicationContext.dataStore.edit { prefs ->
                borderThickness?.let { prefs[Keys.BORDER_THICKNESS] = it }
                animationSpeed?.let { prefs[Keys.ANIMATION_SPEED] = it }
                cornerRadiusTop?.let { prefs[Keys.CORNER_RADIUS_TOP] = it }
                cornerRadiusBottom?.let { prefs[Keys.CORNER_RADIUS_BOTTOM] = it }
                glowEnabled?.let { prefs[Keys.GLOW_ENABLED] = it }
            }
        }
    }

    fun updateMotion(
        animationMode: AnimationMode? = null,
        pulseSpeed: Float? = null,
        pulseCurve: PulseCurve? = null,
        pulseStrength: Float? = null
    ) {
        scope.launch {
            applicationContext.dataStore.edit { prefs ->
                animationMode?.let { prefs[Keys.ANIMATION_MODE] = it.name }
                pulseSpeed?.let { prefs[Keys.PULSE_SPEED] = it }
                pulseCurve?.let { prefs[Keys.PULSE_CURVE] = it.name }
                pulseStrength?.let { prefs[Keys.PULSE_STRENGTH] = it }
            }
        }
    }

    /** Setzt Stil, Farbe, Bewegung und Rand eines Komplettprofils in einem Schreibvorgang. */
    fun applyLightProfile(profile: LightProfile) {
        scope.launch {
            applicationContext.dataStore.edit { prefs ->
                prefs[Keys.PALETTE_ID] = profile.paletteId
                prefs[Keys.USE_CUSTOM_COLORS] = false
                prefs[Keys.LINE_STYLE] = profile.lineStyle.name
                prefs[Keys.ANIMATION_MODE] = profile.animationMode.name
                prefs[Keys.ANIMATION_SPEED] = profile.animationSpeed
                prefs[Keys.PULSE_SPEED] = profile.pulseSpeed
                prefs[Keys.PULSE_CURVE] = profile.pulseCurve.name
                prefs[Keys.PULSE_STRENGTH] = profile.pulseStrength
                prefs[Keys.GLOW_REACH] = profile.glowReach
                prefs[Keys.BORDER_THICKNESS] = profile.borderThickness
                prefs[Keys.GLOW_ENABLED] = profile.glowEnabled
            }
        }
    }

    fun updateLivePreviewEnabled(enabled: Boolean) {
        scope.launch {
            applicationContext.dataStore.edit { it[Keys.LIVE_PREVIEW_ENABLED] = enabled }
        }
    }

    fun updateRingColor(color: Long) {
        scope.launch { applicationContext.dataStore.edit { it[Keys.RING_COLOR] = color } }
    }

    fun updateCallColor(color: Long) {
        scope.launch { applicationContext.dataStore.edit { it[Keys.CALL_COLOR] = color } }
    }

    fun updateTwoStageStartEnabled(enabled: Boolean) {
        scope.launch {
            applicationContext.dataStore.edit { it[Keys.TWO_STAGE_START] = enabled }
        }
    }

    fun updateProcessingPulseEnabled(enabled: Boolean) {
        scope.launch {
            applicationContext.dataStore.edit { it[Keys.PROCESSING_PULSE_ENABLED] = enabled }
        }
    }

    fun updateNotchConfig(notchConfig: NotchConfig) {
        scope.launch {
            applicationContext.dataStore.edit { prefs ->
                prefs[Keys.NOTCH_TYPE] = notchConfig.notchType.name
                prefs[Keys.NOTCH_WIDTH] = notchConfig.notchWidth
                prefs[Keys.NOTCH_HEIGHT] = notchConfig.notchHeight
                prefs[Keys.NOTCH_OFFSET_X] = notchConfig.notchOffsetX
                prefs[Keys.NOTCH_OFFSET_Y] = notchConfig.notchOffsetY
                prefs[Keys.NOTCH_RADIUS] = notchConfig.notchRadius
            }
        }
    }

    fun updateNotificationSettings(settings: NotificationEdgeSettings) {
        scope.launch {
            applicationContext.dataStore.edit { prefs ->
                prefs[Keys.NOTIF_ALERT_ENABLED] = settings.isNotificationAlertEnabled
                prefs[Keys.NOTIF_WAKE_SCREEN] = settings.wakeScreenOnNotification
                prefs[Keys.NOTIF_DURATION] = settings.defaultAlertDurationSeconds
                prefs[Keys.NOTIF_ONLY_SCREEN_OFF] = settings.onlyWhenScreenOff
                prefs[Keys.NOTIF_LIGHT_WHEN] = settings.lightWhen.name
                prefs[Keys.NOTIF_UNTIL_DISMISSED] = settings.pulseUntilDismissed
            }
        }
    }

    fun updateScheduleConfig(scheduleConfig: EdgeScheduleConfig) {
        scope.launch {
            applicationContext.dataStore.edit { prefs ->
                prefs[Keys.SCHEDULE_ENABLED] = scheduleConfig.isScheduleEnabled
                prefs[Keys.SCHEDULE_START_HOUR] = scheduleConfig.startHour
                prefs[Keys.SCHEDULE_START_MINUTE] = scheduleConfig.startMinute
                prefs[Keys.SCHEDULE_END_HOUR] = scheduleConfig.endHour
                prefs[Keys.SCHEDULE_END_MINUTE] = scheduleConfig.endMinute
                prefs[Keys.SCHEDULE_DAYS_CSV] = scheduleConfig.activeDays.joinToString(",")
            }
        }
    }

    fun updateBatterySaverConfig(batterySaverConfig: BatterySaverConfig) {
        scope.launch {
            applicationContext.dataStore.edit { prefs ->
                prefs[Keys.BATTERY_SAVER_ENABLED] = batterySaverConfig.isBatterySaverEnabled
                prefs[Keys.BATTERY_THRESHOLD] = batterySaverConfig.triggerThresholdPercent
                prefs[Keys.BATTERY_REDUCE_FPS] = batterySaverConfig.reduceFpsTo30
                prefs[Keys.BATTERY_DIM_INTENSITY] = batterySaverConfig.dimIntensityPercent
                prefs[Keys.BATTERY_DISABLE_GLOW] = batterySaverConfig.disableGlowFilter
            }
        }
    }

    fun saveAppRule(rule: AppEdgeRule) {
        scope.launch {
            val current = appRulesFlow.value.toMutableList()
            val index = current.indexOfFirst { it.packageName == rule.packageName }
            if (index >= 0) {
                current[index] = rule
            } else {
                current.add(rule)
            }
            applicationContext.dataStore.edit { prefs ->
                prefs[Keys.APP_RULES_RAW] = serializeAppRules(current)
            }
        }
    }

    fun removeAppRule(packageName: String) {
        scope.launch {
            val current = appRulesFlow.value.filterNot { it.packageName == packageName }
            applicationContext.dataStore.edit { prefs ->
                prefs[Keys.APP_RULES_RAW] = serializeAppRules(current)
            }
        }
    }

    fun saveContactRule(rule: ContactEdgeRule) {
        scope.launch {
            val current = contactRulesFlow.value.toMutableList()
            val index = current.indexOfFirst { it.id == rule.id }
            if (index >= 0) {
                current[index] = rule
            } else {
                current.add(rule)
            }
            applicationContext.dataStore.edit { prefs ->
                prefs[Keys.CONTACT_RULES_RAW] = serializeContactRules(current)
            }
        }
    }

    fun removeContactRule(ruleId: String) {
        scope.launch {
            val current = contactRulesFlow.value.filterNot { it.id == ruleId }
            applicationContext.dataStore.edit { prefs ->
                prefs[Keys.CONTACT_RULES_RAW] = serializeContactRules(current)
            }
        }
    }

    private fun serializeAppRules(rules: List<AppEdgeRule>): String {
        return rules.joinToString("\n") { r ->
            listOf(
                r.packageName,
                r.appName,
                r.isEnabled,
                r.paletteId,
                r.useCustomColors,
                r.customColors.joinToString(";"),
                r.lineStyle.name,
                " ", // reserviert (früher Emoji) – hält das 9-Felder-Format kompatibel
                r.durationSeconds
            ).joinToString("|||")
        }
    }

    private fun deserializeAppRules(raw: String): List<AppEdgeRule> {
        return raw.lines().mapNotNull { line ->
            val p = line.split("|||")
            if (p.size >= 9) {
                val customCol = p[5].split(";")
                    .mapNotNull { it.trim().toLongOrNull() }
                val style = parseEnum(p[6], LineStyle.RING)
                AppEdgeRule(
                    packageName = p[0],
                    appName = p[1],
                    isEnabled = p[2].toBooleanStrictOrNull() ?: true,
                    paletteId = p[3].toIntOrNull() ?: 0,
                    useCustomColors = p[4].toBooleanStrictOrNull() ?: false,
                    customColors = customCol,
                    lineStyle = style,
                    durationSeconds = p[8].toIntOrNull() ?: 6
                )
            } else null
        }
    }

    private fun serializeContactRules(rules: List<ContactEdgeRule>): String {
        return rules.joinToString("\n") { r ->
            listOf(
                r.id,
                r.contactIdentifier,
                r.isEnabled,
                r.paletteId,
                r.useCustomColors,
                r.customColors.joinToString(";"),
                r.lineStyle.name,
                " ", // reserviert (früher Emoji) – hält das 9-Felder-Format kompatibel
                r.durationSeconds
            ).joinToString("|||")
        }
    }

    private fun deserializeContactRules(raw: String): List<ContactEdgeRule> {
        return raw.lines().mapNotNull { line ->
            val p = line.split("|||")
            if (p.size >= 9) {
                val customCol = p[5].split(";")
                    .mapNotNull { it.trim().toLongOrNull() }
                val style = parseEnum(p[6], LineStyle.RING)
                ContactEdgeRule(
                    id = p[0].ifBlank { UUID.randomUUID().toString() },
                    contactIdentifier = p[1],
                    isEnabled = p[2].toBooleanStrictOrNull() ?: true,
                    paletteId = p[3].toIntOrNull() ?: 41,
                    useCustomColors = p[4].toBooleanStrictOrNull() ?: false,
                    customColors = customCol,
                    lineStyle = style,
                    durationSeconds = p[8].toIntOrNull() ?: 8
                )
            } else null
        }
    }

    /** Liest einen Enum-Namen tolerant; unbekannte/alte Werte (z. B. frühere Figuren) fallen auf [default] zurück. */
    private inline fun <reified T : Enum<T>> parseEnum(name: String?, default: T): T =
        if (name == null) default else enumValues<T>().firstOrNull { it.name == name } ?: default

    companion object {
        @Volatile
        private var INSTANCE: EdgePreferencesRepository? = null

        fun getInstance(context: Context): EdgePreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EdgePreferencesRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
