package com.example.ui

import android.app.Application
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.EdgePreferencesRepository
import com.example.model.AnimationMode
import com.example.model.AppEdgeRule
import com.example.model.BatterySaverConfig
import com.example.model.BatteryStatus
import com.example.model.ContactEdgeRule
import com.example.model.EdgeConfig
import com.example.model.EdgeScheduleConfig
import com.example.model.LightProfile
import com.example.model.LineStyle
import com.example.model.PulseCurve
import com.example.model.NotchConfig
import com.example.model.NotificationEdgeSettings
import com.example.service.CallerIdMonitorService
import com.example.service.EdgeOverlayService
import com.example.util.BatteryMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EdgeDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EdgePreferencesRepository.getInstance(application)
    private val batteryMonitor = BatteryMonitor(application).apply { startListening() }

    val config: StateFlow<EdgeConfig> = repository.configFlow
    val notificationSettings: StateFlow<NotificationEdgeSettings> = repository.notificationSettingsFlow
    val appRules: StateFlow<List<AppEdgeRule>> = repository.appRulesFlow
    val contactRules: StateFlow<List<ContactEdgeRule>> = repository.contactRulesFlow
    val scheduleConfig: StateFlow<EdgeScheduleConfig> = repository.scheduleConfigFlow
    val batterySaverConfig: StateFlow<BatterySaverConfig> = repository.batterySaverConfigFlow
    val batteryStatus: StateFlow<BatteryStatus> = batteryMonitor.status

    override fun onCleared() {
        super.onCleared()
        batteryMonitor.stopListening()
        repository.setPreviewActive(false)
    }

    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(getApplication())
    }

    fun isNotificationListenerEnabled(): Boolean {
        val context = getApplication<Application>()
        val packageNames = NotificationManagerCompat.getEnabledListenerPackages(context)
        return packageNames.contains(context.packageName)
    }

    fun setOverlayEnabled(enabled: Boolean) {
        val context = getApplication<Application>()
        if (enabled && !canDrawOverlays()) {
            return
        }

        repository.updateOverlayEnabled(enabled)
        if (enabled) {
            EdgeOverlayService.startOverlay(context)
        } else {
            EdgeOverlayService.stopOverlay(context)
        }
    }

    fun setCallerIdAlertEnabled(enabled: Boolean) {
        val context = getApplication<Application>()
        repository.updateCallerIdEnabled(enabled)
        if (enabled) {
            CallerIdMonitorService.start(context)
        } else {
            CallerIdMonitorService.stop(context)
        }
    }

    fun triggerTestCallerPulse(active: Boolean) {
        val context = getApplication<Application>()
        repository.setCallerAlertActive(active)
        EdgeOverlayService.triggerCallerAlert(context, ringing = active)
    }

    fun triggerTestNotification(
        paletteId: Int,
        useCustomColors: Boolean,
        customColors: List<Long>,
        lineStyle: LineStyle,
        durationSeconds: Int
    ) {
        val context = getApplication<Application>()
        EdgeOverlayService.triggerNotificationAlert(
            context = context,
            paletteId = paletteId,
            useCustomColors = useCustomColors,
            customColorsCsv = customColors.joinToString(","),
            lineStyle = lineStyle.name,
            durationSeconds = durationSeconds,
            wakeScreen = notificationSettings.value.wakeScreenOnNotification
        )
    }

    fun updateOpacity(percent: Int) {
        repository.updateOpacity(percent)
    }

    fun triggerCurrentTestAlert(durationSeconds: Int = 5) {
        val current = config.value
        triggerTestNotification(
            paletteId = current.selectedPaletteId,
            useCustomColors = current.useCustomColors,
            customColors = current.customColors,
            lineStyle = current.lineStyle,
            durationSeconds = durationSeconds
        )
    }

    fun selectPreset(paletteId: Int) {
        repository.updatePalette(paletteId)
    }

    fun setCustomColors(colors: List<Long>) {
        repository.updateCustomColors(colors)
    }

    fun updateLineStyle(lineStyle: LineStyle? = null, tailLength: Float? = null, glowReach: Float? = null) {
        repository.updateLineStyle(lineStyle = lineStyle, tailLength = tailLength, glowReach = glowReach)
    }

    fun updateGeometry(
        borderThickness: Float? = null,
        animationSpeed: Float? = null,
        cornerRadiusTop: Float? = null,
        cornerRadiusBottom: Float? = null,
        glowEnabled: Boolean? = null
    ) {
        repository.updateGeometry(
            borderThickness = borderThickness,
            animationSpeed = animationSpeed,
            cornerRadiusTop = cornerRadiusTop,
            cornerRadiusBottom = cornerRadiusBottom,
            glowEnabled = glowEnabled
        )
    }

    fun updateMotion(
        animationMode: AnimationMode? = null,
        pulseSpeed: Float? = null,
        pulseCurve: PulseCurve? = null,
        pulseStrength: Float? = null
    ) {
        repository.updateMotion(animationMode, pulseSpeed, pulseCurve, pulseStrength)
    }

    fun applyLightProfile(profile: LightProfile) {
        repository.applyLightProfile(profile)
    }

    /** Live-Vorschau am echten Bildschirmrand ein-/ausschalten (nur solange die App sichtbar ist). */
    fun setPreviewActive(active: Boolean) {
        val context = getApplication<Application>()
        if (active) {
            if (!canDrawOverlays()) return
            repository.setPreviewActive(true)
            EdgeOverlayService.startOverlay(context)
        } else {
            repository.setPreviewActive(false)
        }
    }

    fun setLivePreviewEnabled(enabled: Boolean) {
        repository.updateLivePreviewEnabled(enabled)
        setPreviewActive(enabled)
    }

    fun setTwoStageStartEnabled(enabled: Boolean) {
        repository.updateTwoStageStartEnabled(enabled)
        if (enabled) replayIntro()
    }

    /** Spielt den Zwei-Stufen-Start sofort am echten Display ab (Live-Vorschau muss sichtbar sein). */
    fun replayIntro() {
        val context = getApplication<Application>()
        if (!canDrawOverlays()) return
        EdgeOverlayService.startOverlay(context)
        repository.requestIntroReplay()
    }

    fun setProcessingPulseEnabled(enabled: Boolean) {
        repository.updateProcessingPulseEnabled(enabled)
    }

    /** Zeigt das blaue Verarbeitungs-Pulsieren für [seconds] Sekunden am echten Display. */
    fun testProcessingPulse(seconds: Long = 8L) {
        val context = getApplication<Application>()
        if (!canDrawOverlays()) return
        repository.setProcessingTestActive(true)
        EdgeOverlayService.startOverlay(context)
        viewModelScope.launch {
            delay(seconds * 1000L)
            repository.setProcessingTestActive(false)
        }
    }

    fun updateNotchConfig(notchConfig: NotchConfig) {
        repository.updateNotchConfig(notchConfig)
    }

    fun updateNotificationSettings(settings: NotificationEdgeSettings) {
        repository.updateNotificationSettings(settings)
    }

    fun updateScheduleConfig(scheduleConfig: EdgeScheduleConfig) {
        repository.updateScheduleConfig(scheduleConfig)
    }

    fun updateBatterySaverConfig(batterySaverConfig: BatterySaverConfig) {
        repository.updateBatterySaverConfig(batterySaverConfig)
    }

    fun saveAppRule(rule: AppEdgeRule) {
        repository.saveAppRule(rule)
    }

    fun removeAppRule(packageName: String) {
        repository.removeAppRule(packageName)
    }

    fun saveContactRule(rule: ContactEdgeRule) {
        repository.saveContactRule(rule)
    }

    fun removeContactRule(ruleId: String) {
        repository.removeContactRule(ruleId)
    }
}
