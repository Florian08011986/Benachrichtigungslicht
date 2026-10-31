package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.example.data.EdgePreferencesRepository
import com.example.service.CallerIdMonitorService
import com.example.service.EdgeOverlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != "android.intent.action.QUICKBOOT_POWERON") {
            return
        }

        val repository = EdgePreferencesRepository.getInstance(context)
        CoroutineScope(Dispatchers.Main).launch {
            val config = repository.configFlow.first()

            if (config.isCallerIdAlertEnabled) {
                CallerIdMonitorService.start(context)
            }
        }
    }
}
