package com.example.service

import android.app.KeyguardManager
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

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        // Ignore our own ongoing service notification
        if (sbn.packageName == packageName) return

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

        // Gesperrter Bildschirm: Ring muss sichtbar sein, daher Display immer aufwecken
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val isLocked = keyguardManager.isKeyguardLocked
        val wakeScreen = notifSettings.wakeScreenOnNotification || isLocked

        // 1. Check for specific Contact match (in title or text)
        val matchedContact = contactRules.firstOrNull { rule ->
            rule.isEnabled && rule.contactIdentifier.isNotBlank() &&
                    (title.contains(rule.contactIdentifier, ignoreCase = true) ||
                            text.contains(rule.contactIdentifier, ignoreCase = true))
        }

        // 2. App-Regel der absendenden App (bestimmt Randfarbe und Muster)
        val matchedApp = appRules.firstOrNull { rule ->
            rule.packageName == sbn.packageName && rule.isEnabled
        }

        // Normaler VIP-Kontakt ohne Herz: eigene Farbe und eigenes Muster (unverändertes Verhalten)
        if (matchedContact != null && !matchedContact.showCenterHeart) {
            EdgeOverlayService.triggerNotificationAlert(
                context = applicationContext,
                paletteId = matchedContact.paletteId,
                useCustomColors = matchedContact.useCustomColors,
                customColorsCsv = matchedContact.customColors.joinToString(","),
                shapeType = matchedContact.shapeType.name,
                customEmoji = matchedContact.customEmoji,
                durationSeconds = matchedContact.durationSeconds,
                wakeScreen = wakeScreen
            )
            replaceNotificationOnLockScreen(sbn, isLocked)
            return
        }

        // Herz-Kontakt (egal in welcher App): Rand bleibt in der App-Farbe,
        // das rote Herz erscheint nur bei gesperrtem Bildschirm in der Displaymitte.
        val showHeart = matchedContact != null && isLocked

        if (matchedApp != null || showHeart) {
            val colorSource = matchedApp
            EdgeOverlayService.triggerNotificationAlert(
                context = applicationContext,
                paletteId = colorSource?.paletteId ?: matchedContact!!.paletteId,
                useCustomColors = colorSource?.useCustomColors ?: matchedContact!!.useCustomColors,
                customColorsCsv = (colorSource?.customColors ?: matchedContact!!.customColors).joinToString(","),
                shapeType = colorSource?.shapeType?.name ?: matchedContact!!.shapeType.name,
                customEmoji = colorSource?.customEmoji ?: matchedContact!!.customEmoji,
                durationSeconds = colorSource?.durationSeconds ?: matchedContact!!.durationSeconds,
                wakeScreen = wakeScreen,
                centerHeart = showHeart,
                showBorder = colorSource != null
            )
            replaceNotificationOnLockScreen(sbn, isLocked)
        }

        // Apps ohne eigene Regel sind nicht verknüpft: kein Licht, Benachrichtigung bleibt unangetastet.
    }

    // Auf dem Sperrbildschirm ersetzt das Licht die Benachrichtigung: Sie wird endgültig entfernt.
    private fun replaceNotificationOnLockScreen(sbn: StatusBarNotification, isLocked: Boolean) {
        if (!isLocked) return
        try {
            cancelNotification(sbn.key)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
