package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.EdgePreferencesRepository
import com.example.model.BatterySaverConfig
import com.example.model.BatteryStatus
import com.example.model.BorderShapeType
import com.example.model.EdgeConfig
import com.example.model.EdgeScheduleConfig
import com.example.rendering.EdgeLightRenderer
import com.example.util.BatteryMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class EdgeOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: EdgeOverlayView? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var configJob: Job? = null
    private var notifAlertJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var repository: EdgePreferencesRepository
    private lateinit var batteryMonitor: BatteryMonitor

    private var isTemporaryAlertActive = false
    private var temporaryConfig: EdgeConfig? = null

    companion object {
        const val CHANNEL_ID = "edge_lighting_overlay_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.service.ACTION_START_OVERLAY"
        const val ACTION_STOP = "com.example.service.ACTION_STOP_OVERLAY"
        const val ACTION_TRIGGER_CALLER_ALERT = "com.example.service.ACTION_TRIGGER_CALLER_ALERT"
        const val ACTION_CLEAR_CALLER_ALERT = "com.example.service.ACTION_CLEAR_CALLER_ALERT"
        const val ACTION_SHOW_NOTIF_ALERT = "com.example.service.ACTION_SHOW_NOTIF_ALERT"

        const val EXTRA_PALETTE_ID = "extra_palette_id"
        const val EXTRA_USE_CUSTOM_COLORS = "extra_use_custom_colors"
        const val EXTRA_CUSTOM_COLORS_CSV = "extra_custom_colors_csv"
        const val EXTRA_SHAPE_TYPE = "extra_shape_type"
        const val EXTRA_CUSTOM_EMOJI = "extra_custom_emoji"
        const val EXTRA_DURATION_SECONDS = "extra_duration_seconds"
        const val EXTRA_WAKE_SCREEN = "extra_wake_screen"
        const val EXTRA_CENTER_HEART = "extra_center_heart"
        const val EXTRA_SHOW_BORDER = "extra_show_border"

        fun startOverlay(context: Context) {
            val intent = Intent(context, EdgeOverlayService::class.java).apply {
                action = ACTION_START
            }
            startServiceCompat(context, intent)
        }

        fun stopOverlay(context: Context) {
            val intent = Intent(context, EdgeOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun triggerCallerAlert(context: Context, ringing: Boolean) {
            val intent = Intent(context, EdgeOverlayService::class.java).apply {
                action = if (ringing) ACTION_TRIGGER_CALLER_ALERT else ACTION_CLEAR_CALLER_ALERT
            }
            startServiceCompat(context, intent)
        }

        fun triggerNotificationAlert(
            context: Context,
            paletteId: Int,
            useCustomColors: Boolean,
            customColorsCsv: String,
            shapeType: String,
            customEmoji: String,
            durationSeconds: Int,
            wakeScreen: Boolean,
            centerHeart: Boolean = false,
            showBorder: Boolean = true
        ) {
            val intent = Intent(context, EdgeOverlayService::class.java).apply {
                action = ACTION_SHOW_NOTIF_ALERT
                putExtra(EXTRA_PALETTE_ID, paletteId)
                putExtra(EXTRA_USE_CUSTOM_COLORS, useCustomColors)
                putExtra(EXTRA_CUSTOM_COLORS_CSV, customColorsCsv)
                putExtra(EXTRA_SHAPE_TYPE, shapeType)
                putExtra(EXTRA_CUSTOM_EMOJI, customEmoji)
                putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
                putExtra(EXTRA_WAKE_SCREEN, wakeScreen)
                putExtra(EXTRA_CENTER_HEART, centerHeart)
                putExtra(EXTRA_SHOW_BORDER, showBorder)
            }
            startServiceCompat(context, intent)
        }

        private fun startServiceCompat(context: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = EdgePreferencesRepository.getInstance(applicationContext)
        batteryMonitor = BatteryMonitor(applicationContext).apply { startListening() }
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                isTemporaryAlertActive = false
                temporaryConfig = null
                removeOverlay()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TRIGGER_CALLER_ALERT -> {
                repository.setCallerAlertActive(true)
                ensureOverlayAttached()
            }
            ACTION_CLEAR_CALLER_ALERT -> {
                repository.setCallerAlertActive(false)
                if (!isTemporaryAlertActive) {
                    removeOverlay()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
            ACTION_SHOW_NOTIF_ALERT -> {
                // Prüfen ob DND Ruhezeit aktiv ist
                val isDnd = repository.scheduleConfigFlow.value.isCurrentlyInDnd()
                if (!isDnd) {
                    handleNotificationAlert(intent)
                }
            }
            ACTION_START -> {
                ensureOverlayAttached()
            }
        }

        startAsForeground()
        observeSystemState()
        return START_STICKY
    }

    private fun handleNotificationAlert(intent: Intent?) {
        if (intent == null) return

        val paletteId = intent.getIntExtra(EXTRA_PALETTE_ID, 0)
        val useCustom = intent.getBooleanExtra(EXTRA_USE_CUSTOM_COLORS, false)
        val customColorsCsv = intent.getStringExtra(EXTRA_CUSTOM_COLORS_CSV) ?: ""
        val shapeName = intent.getStringExtra(EXTRA_SHAPE_TYPE) ?: BorderShapeType.CONTINUOUS_LINE.name
        val customEmoji = intent.getStringExtra(EXTRA_CUSTOM_EMOJI) ?: ""
        val durationSeconds = intent.getIntExtra(EXTRA_DURATION_SECONDS, 6).coerceIn(2, 30)
        val wakeScreen = intent.getBooleanExtra(EXTRA_WAKE_SCREEN, true)
        val centerHeart = intent.getBooleanExtra(EXTRA_CENTER_HEART, false)
        val showBorder = intent.getBooleanExtra(EXTRA_SHOW_BORDER, true)

        val customColors = customColorsCsv.split(",")
            .mapNotNull { it.trim().toLongOrNull() }

        val shape = try {
            BorderShapeType.valueOf(shapeName)
        } catch (_: Exception) {
            BorderShapeType.CONTINUOUS_LINE
        }

        val baseConfig = repository.configFlow.value
        temporaryConfig = baseConfig.copy(
            selectedPaletteId = paletteId,
            useCustomColors = useCustom,
            customColors = if (useCustom && customColors.isNotEmpty()) customColors else baseConfig.customColors,
            shapeType = shape,
            customEmoji = customEmoji.ifBlank { baseConfig.customEmoji },
            isCallerAlertActive = true,
            showCenterHeart = centerHeart,
            showBorder = showBorder
        )

        isTemporaryAlertActive = true

        if (wakeScreen) {
            acquireWakeLock(durationSeconds)
        }

        ensureOverlayAttached()
        overlayView?.updateConfig(temporaryConfig ?: baseConfig)

        notifAlertJob?.cancel()
        notifAlertJob = serviceScope.launch {
            delay(durationSeconds * 1000L)
            isTemporaryAlertActive = false
            temporaryConfig = null
            releaseWakeLock()

            removeOverlay()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun acquireWakeLock(durationSeconds: Int) {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            releaseWakeLock()
            @Suppress("DEPRECATION")
            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "EdgeLighting:NotifWakeLock"
            )
            wakeLock?.acquire(durationSeconds * 1000L)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startAsForeground() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureOverlayAttached() {
        if (!Settings.canDrawOverlays(this)) {
            return
        }

        if (overlayView == null) {
            overlayView = EdgeOverlayView(this)

            val layoutParams = WindowManager.LayoutParams().apply {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                format = PixelFormat.TRANSLUCENT

                // Critical: Pass 100% touches through to underlying apps and lock screen
                this.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED

                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT

                // Draw edge lighting to absolute screen borders including notch
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                }
            }

            try {
                windowManager?.addView(overlayView, layoutParams)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            it.stopAnimation()
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }
    }

    private fun observeSystemState() {
        configJob?.cancel()
        configJob = serviceScope.launch {
            combine(
                repository.configFlow,
                repository.scheduleConfigFlow,
                repository.batterySaverConfigFlow,
                batteryMonitor.status
            ) { config, schedule, batterySaver, batteryStatus ->
                SystemState(config, schedule, batterySaver, batteryStatus)
            }.collect { state ->
                val isDnd = state.schedule.isCurrentlyInDnd()
                val isLowBatteryMode = state.batterySaver.shouldApplyBatterySaver(
                    state.batteryStatus.levelPercent,
                    state.batteryStatus.isPowerSaveMode,
                    state.batteryStatus.isCharging
                )

                if (!isTemporaryAlertActive) {
                    val shouldBeVisible = (state.config.isOverlayEnabled || state.config.isCallerAlertActive) && !isDnd
                    if (!shouldBeVisible) {
                        removeOverlay()
                        if (!state.config.isCallerAlertActive) {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            stopSelf()
                        }
                    } else {
                        ensureOverlayAttached()
                        overlayView?.setBatterySaverMode(
                            active = isLowBatteryMode,
                            reduceFps = state.batterySaver.reduceFpsTo30,
                            dimMultiplier = state.batterySaver.dimIntensityPercent / 100f,
                            disableGlow = state.batterySaver.disableGlowFilter
                        )
                        overlayView?.updateConfig(state.config)
                    }
                } else {
                    // Alert is active: apply battery settings even to alert
                    overlayView?.setBatterySaverMode(
                        active = isLowBatteryMode,
                        reduceFps = state.batterySaver.reduceFpsTo30,
                        dimMultiplier = state.batterySaver.dimIntensityPercent / 100f,
                        disableGlow = state.batterySaver.disableGlowFilter
                    )
                }
            }
        }
    }

    private data class SystemState(
        val config: EdgeConfig,
        val schedule: EdgeScheduleConfig,
        val batterySaver: BatterySaverConfig,
        val batteryStatus: BatteryStatus
    )

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_service_title),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.overlay_service_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_service_title))
            .setContentText(getString(R.string.overlay_service_desc))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        batteryMonitor.stopListening()
        releaseWakeLock()
        notifAlertJob?.cancel()
        serviceScope.cancel()
        removeOverlay()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

class EdgeOverlayView(context: Context) : View(context) {

    private val renderer = EdgeLightRenderer()
    private var config: EdgeConfig = EdgeConfig()
    private var isAnimating: Boolean = false

    // Battery Saver Parameters
    private var isBatterySaverActive: Boolean = false
    private var reduceFpsTo30: Boolean = true
    private var batteryDimMultiplier: Float = 1.0f
    private var disableGlow: Boolean = false

    private val animationRunnable = object : Runnable {
        override fun run() {
            if (isAnimating) {
                postInvalidateOnAnimation()
                if (isBatterySaverActive && reduceFpsTo30) {
                    // Frame rate throttling: 33ms ~ 30 FPS to save power
                    postDelayed(this, 33L)
                } else {
                    // Full 60/120 FPS
                    postOnAnimation(this)
                }
            }
        }
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        isFocusable = false
        isClickable = false
        fitsSystemWindows = false
    }

    fun setBatterySaverMode(active: Boolean, reduceFps: Boolean, dimMultiplier: Float, disableGlow: Boolean) {
        val changed = (isBatterySaverActive != active ||
                reduceFpsTo30 != reduceFps ||
                batteryDimMultiplier != dimMultiplier ||
                this.disableGlow != disableGlow)

        isBatterySaverActive = active
        reduceFpsTo30 = reduceFps
        batteryDimMultiplier = dimMultiplier
        this.disableGlow = disableGlow

        if (changed) {
            postInvalidateOnAnimation()
        }
    }

    fun updateConfig(newConfig: EdgeConfig) {
        val geometryChanged = (config.borderThickness != newConfig.borderThickness) ||
                (config.cornerRadiusTop != newConfig.cornerRadiusTop) ||
                (config.cornerRadiusBottom != newConfig.cornerRadiusBottom) ||
                (config.notchConfig != newConfig.notchConfig)

        config = newConfig
        if (geometryChanged && width > 0 && height > 0) {
            renderer.buildBorderPaths(config)
        }
        postInvalidateOnAnimation()
        startAnimation()
    }

    private fun startAnimation() {
        if (!isAnimating) {
            isAnimating = true
            postOnAnimation(animationRunnable)
        }
    }

    fun stopAnimation() {
        isAnimating = false
        removeCallbacks(animationRunnable)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            renderer.updateDimensions(w.toFloat(), h.toFloat(), config)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer.render(
            canvas = canvas,
            config = config,
            nowNanos = System.nanoTime(),
            batteryDimMultiplier = if (isBatterySaverActive) batteryDimMultiplier else 1.0f,
            disableGlow = if (isBatterySaverActive) disableGlow else false
        )
    }
}
