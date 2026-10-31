package com.example.service

import android.app.Notification
import android.content.Context
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.EdgePreferencesRepository

class EdgeNotificationListenerService : NotificationListenerService() {

    private lateinit var repository: EdgePreferencesRepository

    override fun onCreate() {
        super.onCreate()
        repository = EdgePreferencesRepository.getInstance(applicationContext)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        refreshProcessingState()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        refreshProcessingState()
    }

    /** Ein Vorgang läuft, sobald eine fremde App eine Benachrichtigung mit Fortschrittsanzeige zeigt. */
    private fun isProcessingNotification(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName == packageName) return false
        val extras = sbn.notification?.extras ?: return false
        val indeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)
        val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val progress = extras.getInt(Notification.EXTRA_PROGRESS, 0)
        return indeterminate || (max > 0 && progress < max)
    }

    private fun refreshProcessingState() {
        val active = try {
            activeNotifications?.any { isProcessingNotification(it) } ?: false
        } catch (_: Exception) {
            false // Listener (noch) nicht verbunden
        }
        val wasActive = repository.runtimeState.value.processingActive
        if (active == wasActive) return
        repository.setProcessingActive(active)
        if (active && repository.configFlow.value.isProcessingPulseEnabled) {
            EdgeOverlayService.startOverlay(applicationContext)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        // Ignore our own ongoing service notification
        if (sbn.packageName == packageName) return

        // Laufende Vorgänge (Fortschrittsbalken) für das blaue Pulsieren auswerten
        refreshProcessingState()

        // Ignore ongoing foreground service notifications (music player, downloads, etc.)
        if (sbn.isOngoing) return

        val notifSettings = repository.notificationSettingsFlow.value
        if (!notifSettings.isNotificationAlertEnabled) return

        // DND Ruhezeiten prüfen: In der Ruhephase wird kein Kantenlicht ausgelöst
        val isDnd = repository.scheduleConfigFlow.value.isCurrentlyInDnd()
        if (isDnd) return

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (notifSettings.onlyWhenScreenOff && powerManager.isInteractive) {
            return
        }

        val extras = sbn.notification?.extras
        val title = extras?.getString(Notification.EXTRA_TITLE)
            ?: extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: ""
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        val contactRules = repository.contactRulesFlow.value
        val appRules = repository.appRulesFlow.value
        val baseConfig = repository.configFlow.value

        // 1. Check for specific Contact match (in title or text)
        val matchedContact = contactRules.firstOrNull { rule ->
            rule.isEnabled && rule.contactIdentifier.isNotBlank() &&
                    (title.contains(rule.contactIdentifier, ignoreCase = true) ||
                            text.contains(rule.contactIdentifier, ignoreCase = true))
        }

        if (matchedContact != null) {
            EdgeOverlayService.triggerNotificationAlert(
                context = applicationContext,
                paletteId = matchedContact.paletteId,
                useCustomColors = matchedContact.useCustomColors,
                customColorsCsv = matchedContact.customColors.joinToString(","),
                lineStyle = matchedContact.lineStyle.name,
                durationSeconds = matchedContact.durationSeconds,
                wakeScreen = notifSettings.wakeScreenOnNotification
            )
            return
        }

        // 2. Check for App-specific rule
        val matchedApp = appRules.firstOrNull { rule ->
            rule.packageName == sbn.packageName && rule.isEnabled
        }

        if (matchedApp != null) {
            EdgeOverlayService.triggerNotificationAlert(
                context = applicationContext,
                paletteId = matchedApp.paletteId,
                useCustomColors = matchedApp.useCustomColors,
                customColorsCsv = matchedApp.customColors.joinToString(","),
                lineStyle = matchedApp.lineStyle.name,
                durationSeconds = matchedApp.durationSeconds,
                wakeScreen = notifSettings.wakeScreenOnNotification
            )
            return
        }

        // 3. Fallback to default user edge configuration
        EdgeOverlayService.triggerNotificationAlert(
            context = applicationContext,
            paletteId = baseConfig.selectedPaletteId,
            useCustomColors = baseConfig.useCustomColors,
            customColorsCsv = baseConfig.customColors.joinToString(","),
            lineStyle = baseConfig.lineStyle.name,
            durationSeconds = notifSettings.defaultAlertDurationSeconds,
            wakeScreen = notifSettings.wakeScreenOnNotification
        )
    }
}
