package com.menhaz.senvia.core

import android.content.Context
import android.content.Intent
import android.util.Log
import com.menhaz.senvia.service.ServiceController
import com.menhaz.senvia.storage.DataStoreManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class ServiceManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "ServiceManager"

        @Volatile
        private var INSTANCE: ServiceManager? = null

        fun getInstance(context: Context): ServiceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ServiceManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Start the SMS forwarding service
     */
    suspend fun startService(): ServiceResult {
        return try {
            Log.d(TAG, "Starting SMS forwarding service")

            // Check if service is properly configured
            val configResult = checkServiceConfiguration()
            if (!configResult.isValid) {
                Log.w(TAG, "Service not properly configured: ${configResult.reason}")
                return ServiceResult.Error("Service not configured: ${configResult.reason}")
            }

            // Start the persistent service
            val serviceIntent = Intent(context, ServiceController::class.java).apply {
                action = ServiceController.ACTION_START_SERVICE
            }

            context.startForegroundService(serviceIntent)

            // Update service running state
            DataStoreManager.setServiceRunning(context, true)

            Log.i(TAG, "SMS forwarding service started successfully")
            ServiceResult.Success("Service started successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start service", e)
            ServiceResult.Error("Failed to start service: ${e.message}")
        }
    }

    /**
     * Stop the SMS forwarding service
     */
    suspend fun stopService(): ServiceResult {
        return try {
            Log.d(TAG, "Stopping SMS forwarding service")

            val serviceIntent = Intent(context, ServiceController::class.java).apply {
                action = ServiceController.ACTION_STOP_SERVICE
            }

            context.startService(serviceIntent)

            // Update service running state
            DataStoreManager.setServiceRunning(context, false)

            Log.i(TAG, "SMS forwarding service stopped successfully")
            ServiceResult.Success("Service stopped successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop service", e)
            ServiceResult.Error("Failed to stop service: ${e.message}")
        }
    }

    /**
     * Restart the SMS forwarding service
     */
    suspend fun restartService(): ServiceResult {
        Log.d(TAG, "Restarting SMS forwarding service")

        val stopResult = stopService()
        if (stopResult is ServiceResult.Error) {
            return stopResult
        }

        // Small delay to ensure service is properly stopped
        delay(500)

        return startService()
    }

    /**
     * Check if the service is currently running
     * Using internal state tracking instead of deprecated getRunningServices
     */
    fun isServiceRunning(): Boolean {
        return try {
            // Use static flag from service to check if it's running
            // This avoids the deprecated getRunningServices call
            ServiceController.isServiceRunning()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking service status", e)
            false
        }
    }

    /**
     * Get comprehensive service status
     */
    suspend fun getServiceStatus(): ServiceStatus {
        val isActuallyRunning = isServiceRunning()
        val shouldBeRunning = DataStoreManager.isServiceRunning(context).first()
        val configResult = checkServiceConfiguration()

        return ServiceStatus(
            isRunning = isActuallyRunning,
            shouldBeRunning = shouldBeRunning,
            isConfigured = configResult.isValid,
            configurationIssue = if (!configResult.isValid) configResult.reason else null,
            needsSync = isActuallyRunning != shouldBeRunning
        )
    }

    /**
     * Get real-time service status as Flow
     */
    fun getServiceStatusFlow(): Flow<ServiceStatus> {
        return combine(
            DataStoreManager.isServiceRunning(context),
            DataStoreManager.getCurrentDestination(context),
            DataStoreManager.getBotToken(context),
            DataStoreManager.getChatId(context),
            DataStoreManager.getPhoneNumber(context)
        ) { shouldBeRunning, destination, botToken, chatId, phoneNumber ->
            val isActuallyRunning = isServiceRunning()
            val isConfigured = when (destination) {
                "telegram" -> botToken.isNotBlank() && chatId.isNotBlank()
                "phone" -> phoneNumber.isNotBlank()
                else -> false
            }

            ServiceStatus(
                isRunning = isActuallyRunning,
                shouldBeRunning = shouldBeRunning,
                isConfigured = isConfigured,
                configurationIssue = if (!isConfigured) "No destination configured" else null,
                needsSync = isActuallyRunning != shouldBeRunning
            )
        }
    }

    /**
     * Sync service state - ensure actual state matches desired state
     */
    suspend fun syncServiceState(): ServiceResult {
        val status = getServiceStatus()

        return when {
            !status.isConfigured -> {
                if (status.isRunning) stopService()
                ServiceResult.Error("Service not configured properly")
            }
            status.needsSync -> {
                if (status.shouldBeRunning && !status.isRunning) {
                    Log.i(TAG, "Service should be running but isn't - starting")
                    startService()
                } else if (!status.shouldBeRunning && status.isRunning) {
                    Log.i(TAG, "Service is running but shouldn't be - stopping")
                    stopService()
                } else {
                    ServiceResult.Success("Service state synchronized")
                }
            }
            else -> ServiceResult.Success("Service state is already synchronized")
        }
    }

    /**
     * Check if service configuration is valid
     */
    private suspend fun checkServiceConfiguration(): ConfigurationResult {
        val currentDestination = DataStoreManager.getCurrentDestination(context).first()

        return when (currentDestination) {
            "telegram" -> {
                val botToken = DataStoreManager.getBotToken(context).first()
                val chatId = DataStoreManager.getChatId(context).first()

                when {
                    botToken.isBlank() -> ConfigurationResult(false, "Bot token not configured")
                    chatId.isBlank() -> ConfigurationResult(false, "Chat ID not configured")
                    else -> ConfigurationResult(true, "Telegram configured")
                }
            }
            "phone" -> {
                val phoneNumber = DataStoreManager.getPhoneNumber(context).first()

                if (phoneNumber.isBlank()) {
                    ConfigurationResult(false, "Phone number not configured")
                } else {
                    ConfigurationResult(true, "Phone configured")
                }
            }
            else -> ConfigurationResult(false, "No destination selected")
        }
    }
}

/**
 * Result of service operations
 */
sealed class ServiceResult {
    data class Success(val message: String) : ServiceResult()
    data class Error(val message: String) : ServiceResult()
}

/**
 * Current service status
 */
data class ServiceStatus(
    val isRunning: Boolean,
    val shouldBeRunning: Boolean,
    val isConfigured: Boolean,
    val configurationIssue: String? = null,
    val needsSync: Boolean = false
)

/**
 * Configuration check result
 */
private data class ConfigurationResult(
    val isValid: Boolean,
    val reason: String
)
