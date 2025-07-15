package com.menhaz.senvia.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.menhaz.senvia.storage.DataStoreManager
import com.menhaz.senvia.core.ServiceManager
import com.menhaz.senvia.core.ServiceResult
import com.menhaz.senvia.core.ServiceStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.menhaz.senvia.storage.MessageLog
import android.util.Log

class SettingsViewModel(private val context: Context) : ViewModel() {

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    private val serviceManager by lazy { ServiceManager.getInstance(context) }

    // Expose bot token and chat ID as StateFlow
    private val _botToken = MutableStateFlow("")
    val botToken: StateFlow<String> = _botToken.asStateFlow()

    private val _chatId = MutableStateFlow("")
    val chatId: StateFlow<String> = _chatId.asStateFlow()

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    // Filter keywords functionality
    private val _filterKeywords = MutableStateFlow<Set<String>>(emptySet())
    val filterKeywords: StateFlow<Set<String>> = _filterKeywords.asStateFlow()

    // Destination preferences
    private val _telegramEnabled = MutableStateFlow(true)
    val telegramEnabled: StateFlow<Boolean> = _telegramEnabled.asStateFlow()

    private val _phoneEnabled = MutableStateFlow(true)
    val phoneEnabled: StateFlow<Boolean> = _phoneEnabled.asStateFlow()

    // Current destination selection
    private val _currentDestination = MutableStateFlow("telegram")
    val currentDestination: StateFlow<String> = _currentDestination.asStateFlow()

    // Message logs
    private val _messageLogs = MutableStateFlow<List<MessageLog>>(emptyList())
    val messageLogs: StateFlow<List<MessageLog>> = _messageLogs.asStateFlow()

    // Service running state - now connected to real service status
    private val _serviceRunning = MutableStateFlow(true)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning.asStateFlow()

    // Real service status from ServiceManager
    private val _realServiceStatus = MutableStateFlow(ServiceStatus(
        isRunning = false,
        shouldBeRunning = false,
        isConfigured = false
    ))
    val realServiceStatus: StateFlow<ServiceStatus> = _realServiceStatus.asStateFlow()

    // Auto-start preference
    private val _autoStartEnabled = MutableStateFlow(true)
    val autoStartEnabled: StateFlow<Boolean> = _autoStartEnabled.asStateFlow()

    // Auto-delete logs preference
    private val _autoDeleteLogsEnabled = MutableStateFlow(true)
    val autoDeleteLogsEnabled: StateFlow<Boolean> = _autoDeleteLogsEnabled.asStateFlow()

    init {
        // Collect DataStore values separately but efficiently
        viewModelScope.launch {
            DataStoreManager.getBotToken(context).collect {
                _botToken.value = it
            }
        }
        viewModelScope.launch {
            DataStoreManager.getChatId(context).collect {
                _chatId.value = it
            }
        }
        viewModelScope.launch {
            DataStoreManager.getPhoneNumber(context).collect {
                _phoneNumber.value = it
            }
        }
        viewModelScope.launch {
            DataStoreManager.getFilterKeywords(context).collect {
                _filterKeywords.value = it
            }
        }
        viewModelScope.launch {
            DataStoreManager.isTelegramEnabled(context).collect {
                _telegramEnabled.value = it
            }
        }
        viewModelScope.launch {
            DataStoreManager.isPhoneEnabled(context).collect {
                _phoneEnabled.value = it
            }
        }
        viewModelScope.launch {
            DataStoreManager.getCurrentDestination(context).collect {
                _currentDestination.value = it
            }
        }
        viewModelScope.launch {
            DataStoreManager.getMessageLogs(context).collect {
                _messageLogs.value = it
            }
        }
        viewModelScope.launch {
            DataStoreManager.isServiceRunning(context).collect {
                _serviceRunning.value = it
            }
        }
        viewModelScope.launch {
            DataStoreManager.isAutoStartEnabled(context).collect {
                _autoStartEnabled.value = it
            }
        }
        viewModelScope.launch {
            DataStoreManager.isAutoDeleteLogsEnabled(context).collect {
                _autoDeleteLogsEnabled.value = it
            }
        }

        // Defer service status collection to avoid blocking startup
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000) // Wait 1 second before starting service operations
            serviceManager.getServiceStatusFlow()
                .distinctUntilChanged()
                .debounce(300) // Wait 300ms before emitting to prevent rapid updates
                .collect { status ->
                    _realServiceStatus.value = status
                }
        }

        // Defer periodic service sync even more
        viewModelScope.launch {
            kotlinx.coroutines.delay(5000) // Wait 5 seconds before starting periodic sync
            while (true) {
                try {
                    serviceManager.syncServiceState()
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing service state", e)
                }
                kotlinx.coroutines.delay(120000) // Sync every 2 minutes
            }
        }
    }

    suspend fun saveBotToken(token: String) {
        _botToken.value = token
        DataStoreManager.saveBotToken(context, token)
    }

    suspend fun saveChatId(chatId: String) {
        _chatId.value = chatId
        DataStoreManager.saveChatId(context, chatId)
    }

    suspend fun savePhoneNumber(phoneNumber: String) {
        _phoneNumber.value = phoneNumber
        DataStoreManager.savePhoneNumber(context, phoneNumber)
    }

    fun addKeyword(keyword: String) {
        viewModelScope.launch {
            val current = _filterKeywords.value
            if (keyword.isNotBlank() && !current.contains(keyword)) {
                DataStoreManager.saveFilterKeywords(context, current + keyword)
            }
        }
    }

    fun addKeywords(keywords: List<String>) {
        viewModelScope.launch {
            val current = _filterKeywords.value
            val newKeywords = keywords.filter { it.isNotBlank() && !current.contains(it) }
            if (newKeywords.isNotEmpty()) {
                DataStoreManager.saveFilterKeywords(context, current + newKeywords)
            }
        }
    }

    fun removeKeyword(keyword: String) {
        viewModelScope.launch {
            val current = _filterKeywords.value
            if (current.contains(keyword)) {
                DataStoreManager.saveFilterKeywords(context, current - keyword)
            }
        }
    }

    fun clearKeywords() {
        viewModelScope.launch {
            DataStoreManager.saveFilterKeywords(context, emptySet())
        }
    }

    fun setTelegramEnabled(enabled: Boolean) {
        viewModelScope.launch {
            DataStoreManager.setTelegramEnabled(context, enabled)
        }
    }

    fun setPhoneEnabled(enabled: Boolean) {
        viewModelScope.launch {
            DataStoreManager.setPhoneEnabled(context, enabled)
        }
    }

    fun setCurrentDestination(destination: String) {
        _currentDestination.value = destination
        viewModelScope.launch {
            DataStoreManager.setCurrentDestination(context, destination)

            // Auto-start service when destination is configured (Smart UX)
            if (destination != "none" && destination.isNotBlank()) {
                try {
                    Log.d(TAG, "Auto-starting service for destination: $destination")
                    val result = serviceManager.startService()
                    when (result) {
                        is ServiceResult.Success -> {
                            Log.i(TAG, "Service auto-started successfully for $destination")
                        }
                        is ServiceResult.Error -> {
                            Log.w(TAG, "Failed to auto-start service: ${result.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error auto-starting service", e)
                }
            }
        }
    }

    fun setServiceRunning(running: Boolean) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Setting service running: $running")

                val result = if (running) {
                    serviceManager.startService()
                } else {
                    serviceManager.stopService()
                }

                when (result) {
                    is ServiceResult.Success -> {
                        Log.i(TAG, "Service operation successful: ${result.message}")
                    }
                    is ServiceResult.Error -> {
                        Log.e(TAG, "Service operation failed: ${result.message}")
                        // Revert the UI state if service operation failed
                        // The DataStore value will be updated by the service manager
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in setServiceRunning", e)
            }
        }
    }

    fun setAutoStartEnabled(enabled: Boolean) {
        viewModelScope.launch {
            DataStoreManager.setAutoStartEnabled(context, enabled)
        }
    }

    fun restartService() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Restarting service")
                val result = serviceManager.restartService()

                when (result) {
                    is ServiceResult.Success -> {
                        Log.i(TAG, "Service restart successful: ${result.message}")
                    }
                    is ServiceResult.Error -> {
                        Log.e(TAG, "Service restart failed: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error restarting service", e)
            }
        }
    }

    fun syncServiceState() {
        viewModelScope.launch {
            try {
                serviceManager.syncServiceState()
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing service state", e)
            }
        }
    }

    fun setAutoDeleteLogsEnabled(enabled: Boolean) {
        _autoDeleteLogsEnabled.value = enabled
        viewModelScope.launch {
            DataStoreManager.setAutoDeleteLogsEnabled(context, enabled)
        }
    }

    fun deleteOldLogs() {
        viewModelScope.launch {
            try {
                DataStoreManager.deleteOldLogs(context)
                Log.i(TAG, "Old logs deleted successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting old logs", e)
            }
        }
    }

    fun sendTestMessage() {
        viewModelScope.launch {
            try {
                val currentDest = _currentDestination.value

                // Check if service is running and configured
                val status = serviceManager.getServiceStatus()
                if (!status.isRunning || !status.isConfigured) {
                    Log.w(TAG, "Cannot send test message - service not ready: $status")

                    val errorLog = MessageLog(
                        timestamp = System.currentTimeMillis(),
                        sender = "senvia",
                        message = "❌ Test failed: Service not running or configured properly",
                        destination = currentDest,
                        status = "failed"
                    )
                    DataStoreManager.saveMessageLog(context, errorLog)
                    return@launch
                }

                // Send actual test message through the service
                val serviceIntent = Intent(context, com.menhaz.senvia.service.ServiceController::class.java).apply {
                    action = com.menhaz.senvia.service.ServiceController.ACTION_FORWARD_SMS
                    putExtra(com.menhaz.senvia.service.ServiceController.EXTRA_SENDER, "senvia")
                    putExtra(com.menhaz.senvia.service.ServiceController.EXTRA_MESSAGE, "Test ping from senvia app")
                }
                context.startService(serviceIntent)

                Log.i(TAG, "Test message initiated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending test message", e)

                val errorLog = MessageLog(
                    timestamp = System.currentTimeMillis(),
                    sender = "senvia",
                    message = "❌ Test failed: ${e.message}",
                    destination = _currentDestination.value,
                    status = "failed"
                )
                DataStoreManager.saveMessageLog(context, errorLog)
            }
        }
    }



    fun clearAllLogs() {
        viewModelScope.launch {
            try {
                DataStoreManager.clearAllMessageLogs(context)
                Log.i(TAG, "All message logs cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing logs", e)
            }
        }
    }
}

// Factory for SettingsViewModel to pass Context
class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
