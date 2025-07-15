package com.menhaz.senvia.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.menhaz.senvia.MainActivity
import com.menhaz.senvia.R
import java.net.URL
import java.net.URLEncoder
import java.net.HttpURLConnection
import com.menhaz.senvia.storage.DataStoreManager
import com.menhaz.senvia.storage.MessageLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import android.telephony.SmsManager
import java.util.concurrent.atomic.AtomicBoolean

class ServiceController : Service() {

    companion object {
        private const val TAG = "ServiceController"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "sms_forwarding_persistent"

        // Actions
        const val ACTION_START_SERVICE = "com.menhaz.senvia.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.menhaz.senvia.STOP_SERVICE"
        const val ACTION_FORWARD_SMS = "com.menhaz.senvia.FORWARD_SMS"

        // Extra keys
        const val EXTRA_SENDER = "sender"
        const val EXTRA_MESSAGE = "message"

        // Retry configuration
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 2000L

        // Static service status tracking
        private val isRunning = AtomicBoolean(false)

        fun isServiceRunning(): Boolean = isRunning.get()
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isServiceRunning = AtomicBoolean(false)

    // Statistics
    private var messagesForwarded = 0
    private var lastForwardTime = 0L

    // Log cleanup job
    private var logCleanupJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        isRunning.set(true)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called with action: ${intent?.action}")

        when (intent?.action) {
            ACTION_START_SERVICE -> {
                startPersistentService()
            }
            ACTION_STOP_SERVICE -> {
                stopPersistentService()
                return START_NOT_STICKY
            }
            ACTION_FORWARD_SMS -> {
                val sender = intent.getStringExtra(EXTRA_SENDER) ?: "Unknown"
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: ""
                handleSmsForwarding(sender, message)
            }
            else -> {
                Log.w(TAG, "Unknown action: ${intent?.action}")
            }
        }

        return START_STICKY // Restart if killed by system
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        isServiceRunning.set(false)
        isRunning.set(false)
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startPersistentService() {
        if (isServiceRunning.get()) {
            Log.d(TAG, "Service already running")
            return
        }

        Log.i(TAG, "Starting persistent SMS forwarding service")
        isServiceRunning.set(true)
        isRunning.set(true)

        // Start as foreground service
        startForeground(NOTIFICATION_ID, createServiceNotification())

        // Update service state in DataStore
        serviceScope.launch {
            DataStoreManager.setServiceRunning(this@ServiceController, true)
        }

        // Start periodic log cleanup
        startLogCleanupTask()

        Log.i(TAG, "Persistent SMS forwarding service started successfully")
    }

    private fun stopPersistentService() {
        Log.i(TAG, "Stopping persistent SMS forwarding service")
        isServiceRunning.set(false)

        // Stop log cleanup task
        logCleanupJob?.cancel()

        serviceScope.launch {
            DataStoreManager.setServiceRunning(this@ServiceController, false)
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Handle SMS forwarding request
     */
    private fun handleSmsForwarding(sender: String, message: String) {
        if (!isServiceRunning.get()) {
            Log.w(TAG, "Service not running, ignoring SMS forwarding request")
            return
        }

        Log.d(TAG, "Processing SMS forwarding request from: $sender")

        serviceScope.launch {
            try {
                // Check if service should process messages
                val serviceEnabled = DataStoreManager.isServiceRunning(this@ServiceController).first()
                if (!serviceEnabled) {
                    Log.d(TAG, "Service disabled in settings, skipping forwarding")
                    return@launch
                }

                // Apply keyword filtering
                if (!shouldForwardMessage(message)) {
                    Log.d(TAG, "Message filtered out by keywords")
                    return@launch
                }

                // Forward to configured destination
                val currentDestination = DataStoreManager.getCurrentDestination(this@ServiceController).first()

                when (currentDestination) {
                    "telegram" -> forwardToTelegram(sender, message)
                    "phone" -> forwardToPhone(sender, message)
                    else -> {
                        Log.w(TAG, "No valid destination configured: $currentDestination")
                        logFailedMessage(sender, message, "No destination configured")
                    }
                }

                // Update statistics
                messagesForwarded++
                lastForwardTime = System.currentTimeMillis()
                updateNotification()

            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS forwarding", e)
                logFailedMessage(sender, message, "Processing error: ${e.message}")
            }
        }
    }

    /**
     * Check if message should be forwarded based on keyword filters
     */
    private suspend fun shouldForwardMessage(message: String): Boolean {
        val filterKeywords = DataStoreManager.getFilterKeywords(this).first()

        // If no filters set, forward all messages
        if (filterKeywords.isEmpty()) {
            return true
        }

        // Forward if message contains any of the keywords
        return filterKeywords.any { keyword ->
            message.contains(keyword, ignoreCase = true)
        }
    }

    /**
     * Forward SMS to Telegram
     */
    private suspend fun forwardToTelegram(sender: String, message: String) {
        val botToken = DataStoreManager.getBotToken(this).first()
        val chatId = DataStoreManager.getChatId(this).first()

        if (botToken.isBlank() || chatId.isBlank()) {
            Log.w(TAG, "Telegram not configured properly")
            logFailedMessage(sender, message, "Telegram not configured")
            return
        }

        Log.d(TAG, "Forwarding SMS to Telegram")

        var attempt = 0
        var lastException: Exception? = null

        while (attempt < MAX_RETRIES) {
            try {
                attempt++
                val success = sendToTelegram(botToken, chatId, sender, message)
                if (success) {
                    Log.i(TAG, "SMS forwarded to Telegram successfully")
                    logSuccessfulMessage(sender, message, "telegram")
                    return
                }

                if (attempt < MAX_RETRIES) {
                    kotlinx.coroutines.delay(RETRY_DELAY_MS * attempt)
                }
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Attempt $attempt failed: ${e.message}")

                if (attempt < MAX_RETRIES) {
                    kotlinx.coroutines.delay(RETRY_DELAY_MS * attempt)
                }
            }
        }

        Log.e(TAG, "All retry attempts failed for Telegram", lastException)
        logFailedMessage(sender, message, "Telegram send failed after $MAX_RETRIES attempts")
    }

    /**
     * Forward SMS to phone number
     */
    private suspend fun forwardToPhone(sender: String, message: String) {
        val phoneNumber = DataStoreManager.getPhoneNumber(this).first()

        if (phoneNumber.isBlank()) {
            Log.w(TAG, "Phone number not configured")
            logFailedMessage(sender, message, "Phone number not configured")
            return
        }

        Log.d(TAG, "Forwarding SMS to phone number: $phoneNumber")

        try {
            sendSmsMessage(phoneNumber, sender, message)
            Log.i(TAG, "SMS forwarded to phone successfully")
            logSuccessfulMessage(sender, message, "phone")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to forward SMS to phone", e)
            logFailedMessage(sender, message, "SMS send failed: ${e.message}")
        }
    }

    /**
     * Send message to Telegram
     */
    private suspend fun sendToTelegram(botToken: String, chatId: String, sender: String, message: String): Boolean {
        return try {
            val text = "SMS Forward\nFrom: $sender\n\n$message"
            val encodedText = URLEncoder.encode(text, "UTF-8")
            val url = URL("https://api.telegram.org/bot$botToken/sendMessage?chat_id=$chatId&text=$encodedText")
            
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                Log.d(TAG, "SMS forwarded to Telegram successfully")
                true
            } else {
                Log.w(TAG, "Telegram API error: $responseCode")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending to Telegram: ${e.message}")
            false
        }
    }

    /**
     * Send SMS message
     */
    private fun sendSmsMessage(phoneNumber: String, sender: String, message: String) {
        try {
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                this.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val fullMessage = "📨 SMS Forward\n📞 From: $sender\n\n$message"

            // Validate phone number format
            if (!isValidPhoneNumber(phoneNumber)) {
                throw IllegalArgumentException("Invalid phone number format: $phoneNumber")
            }

            Log.d(TAG, "Attempting to send SMS to: $phoneNumber")

            // Create pending intents for delivery confirmation
            val sentIntent = android.app.PendingIntent.getBroadcast(
                this, 0,
                android.content.Intent("SMS_SENT"),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val deliveredIntent = android.app.PendingIntent.getBroadcast(
                this, 0,
                android.content.Intent("SMS_DELIVERED"),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            // Split message if it's too long
            val parts = smsManager.divideMessage(fullMessage)

            if (parts.size == 1) {
                smsManager.sendTextMessage(phoneNumber, null, fullMessage, sentIntent, deliveredIntent)
            } else {
                val sentIntents = arrayListOf(sentIntent)
                val deliveredIntents = arrayListOf(deliveredIntent)
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, deliveredIntents)
            }

            Log.i(TAG, "SMS send request submitted to system")

        } catch (e: SecurityException) {
            Log.e(TAG, "SMS permission denied - check SEND_SMS permission", e)
            throw e
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid SMS parameters", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
            throw e
        }
    }

    /**
     * Validate phone number format
     */
    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        val cleanNumber = phoneNumber.replace(Regex("[^\\d+]"), "")
        return cleanNumber.isNotEmpty() && (cleanNumber.startsWith("+") || cleanNumber.length >= 10)
    }

    /**
     * Log successful message
     */
    private suspend fun logSuccessfulMessage(sender: String, message: String, destination: String) {
        val messageLog = MessageLog(
            timestamp = System.currentTimeMillis(),
            sender = sender,
            message = message,
            destination = destination,
            status = "sent"
        )
        DataStoreManager.saveMessageLog(this@ServiceController, messageLog)
    }

    /**
     * Log failed message
     */
    private suspend fun logFailedMessage(sender: String, message: String, error: String) {
        val messageLog = MessageLog(
            timestamp = System.currentTimeMillis(),
            sender = sender,
            message = "$message (Error: $error)",
            destination = "unknown",
            status = "failed"
        )
        DataStoreManager.saveMessageLog(this@ServiceController, messageLog)
    }

    /**
     * Create notification channel for the service
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SMS Forwarding Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps SMS forwarding service running in background"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Create service notification
     */
    private fun createServiceNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SMS Forwarding Active")
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    /**
     * Update notification with current stats
     */
    private fun updateNotification() {
        try {
            val notification = createServiceNotification()
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update notification", e)
        }
    }

    /**
     * Get notification text based on current state
     */
    private fun getNotificationText(): String {
        return if (messagesForwarded > 0) {
            "Forwarded $messagesForwarded messages"
        } else {
            "Service running"
        }
    }

    /**
     * Start periodic log cleanup task
     */
    private fun startLogCleanupTask() {
        logCleanupJob?.cancel() // Cancel any existing task

        logCleanupJob = serviceScope.launch {
            while (isActive) {
                try {
                    // Check if auto-delete is enabled
                    val autoDeleteEnabled = DataStoreManager.isAutoDeleteLogsEnabled(this@ServiceController).first()

                    if (autoDeleteEnabled) {
                        Log.d(TAG, "Running automatic log cleanup")
                        DataStoreManager.deleteOldLogs(this@ServiceController)
                    }

                    // Wait 24 hours before next cleanup
                    kotlinx.coroutines.delay(24 * 60 * 60 * 1000L)

                } catch (e: Exception) {
                    Log.e(TAG, "Error during log cleanup", e)
                    // Wait 1 hour before retrying if there's an error
                    kotlinx.coroutines.delay(60 * 60 * 1000L)
                }
            }
        }
    }
}
