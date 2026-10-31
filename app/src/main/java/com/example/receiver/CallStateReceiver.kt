package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.example.data.EdgePreferencesRepository
import com.example.service.EdgeOverlayService

class CallStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val repository = EdgePreferencesRepository.getInstance(context)
        val isCallerIdEnabled = repository.configFlow.value.isCallerIdAlertEnabled

        if (!isCallerIdEnabled) return

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                EdgeOverlayService.triggerCallerAlert(context, ringing = true)
            }
            TelephonyManager.EXTRA_STATE_IDLE,
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                EdgeOverlayService.triggerCallerAlert(context, ringing = false)
            }
        }
    }
}
