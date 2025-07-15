package com.menhaz.senvia.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.menhaz.senvia.service.ServiceController
import com.menhaz.senvia.storage.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SystemManager : BroadcastReceiver() {

    companion object {
        private const val TAG = "SystemManager"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Received non-boot intent: ${intent.action}")
            return
        }

        Log.d(TAG, "Device boot completed, checking if service should auto-start")

        // Use coroutine scope to check user preferences
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if service was running before reboot
                val wasServiceRunning = DataStoreManager.isServiceRunning(context).first()
                val autoStartEnabled = true // Default to true for now, will be implemented in DataStoreManager

                Log.d(TAG, "Service was running: $wasServiceRunning, Auto-start enabled: $autoStartEnabled")

                // Start service if it was running before reboot or if auto-start is enabled
                if (wasServiceRunning || autoStartEnabled) {
                    // Check if we have valid configuration
                    val currentDestination = DataStoreManager.getCurrentDestination(context).first()
                    val botToken = DataStoreManager.getBotToken(context).first()
                    val chatId = DataStoreManager.getChatId(context).first()
                    val phoneNumber = DataStoreManager.getPhoneNumber(context).first()

                    val isConfigured = when (currentDestination) {
                        "telegram" -> botToken.isNotBlank() && chatId.isNotBlank()
                        "phone" -> phoneNumber.isNotBlank()
                        else -> false
                    }

                    if (isConfigured) {
                        Log.i(TAG, "Starting SMS forwarding service after boot")
                        val serviceIntent = Intent(context, ServiceController::class.java).apply {
                            action = ServiceController.ACTION_START_SERVICE
                        }
                        context.startForegroundService(serviceIntent)
                    } else {
                        Log.w(TAG, "Service not configured properly, skipping auto-start")
                        // Reset service running state if not configured
                        DataStoreManager.setServiceRunning(context, false)
                    }
                } else {
                    Log.d(TAG, "Service was not running before reboot and auto-start is disabled")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during boot receiver execution", e)
            }
        }
    }
}
