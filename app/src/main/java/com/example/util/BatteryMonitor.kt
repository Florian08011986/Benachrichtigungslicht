package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import com.example.model.BatteryStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BatteryMonitor(private val context: Context) {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val _status = MutableStateFlow(queryInitialStatus())
    val status: StateFlow<BatteryStatus> = _status.asStateFlow()

    private var isRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            updateStatus(intent)
        }
    }

    private fun queryInitialStatus(): BatteryStatus {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return calculateStatus(intent)
    }

    private fun updateStatus(intent: Intent?) {
        _status.value = calculateStatus(intent)
    }

    private fun calculateStatus(intent: Intent?): BatteryStatus {
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val statusExtra = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val levelPercent = if (level >= 0 && scale > 0) {
            ((level / scale.toFloat()) * 100).toInt()
        } else {
            100
        }

        val isCharging = statusExtra == BatteryManager.BATTERY_STATUS_CHARGING ||
                statusExtra == BatteryManager.BATTERY_STATUS_FULL

        val isPowerSave = powerManager?.isPowerSaveMode ?: false

        return BatteryStatus(
            levelPercent = levelPercent,
            isCharging = isCharging,
            isPowerSaveMode = isPowerSave
        )
    }

    fun startListening() {
        if (!isRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            }
            context.registerReceiver(receiver, filter)
            isRegistered = true
            updateStatus(null)
        }
    }

    fun stopListening() {
        if (isRegistered) {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isRegistered = false
        }
    }
}
