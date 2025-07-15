
package com.menhaz.senvia.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object DataStoreManager {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    private val BOT_TOKEN_KEY = stringPreferencesKey("bot_token")
    private val CHAT_ID_KEY = stringPreferencesKey("chat_id")
    private val PHONE_NUMBER_KEY = stringPreferencesKey("phone_number")
    private val FILTER_KEYWORDS_KEY = stringSetPreferencesKey("filter_keywords")
    private val MESSAGE_LOGS_KEY = stringPreferencesKey("message_logs")
    private val TELEGRAM_ENABLED_KEY = stringPreferencesKey("telegram_enabled")
    private val PHONE_ENABLED_KEY = stringPreferencesKey("phone_enabled")
    private val CURRENT_DESTINATION_KEY = stringPreferencesKey("current_destination")
    private val SERVICE_RUNNING_KEY = stringPreferencesKey("service_running")
    private val AUTO_START_ENABLED_KEY = stringPreferencesKey("auto_start_enabled")
    private val AUTO_DELETE_LOGS_KEY = stringPreferencesKey("auto_delete_logs")
    
    private val gson = Gson()

    fun getBotToken(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[BOT_TOKEN_KEY] ?: ""
        }
    }

    suspend fun saveBotToken(context: Context, token: String) {
        context.dataStore.edit { settings ->
            settings[BOT_TOKEN_KEY] = token
        }
    }

    fun getChatId(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[CHAT_ID_KEY] ?: ""
        }
    }

    suspend fun saveChatId(context: Context, chatId: String) {
        context.dataStore.edit { settings ->
            settings[CHAT_ID_KEY] = chatId
        }
    }

    fun getFilterKeywords(context: Context): Flow<Set<String>> {
        return context.dataStore.data.map { preferences ->
            preferences[FILTER_KEYWORDS_KEY] ?: emptySet()
        }
    }

    suspend fun saveFilterKeywords(context: Context, keywords: Set<String>) {
        context.dataStore.edit { settings ->
            settings[FILTER_KEYWORDS_KEY] = keywords
        }
    }

    fun getPhoneNumber(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[PHONE_NUMBER_KEY] ?: ""
        }
    }

    suspend fun savePhoneNumber(context: Context, phoneNumber: String) {
        context.dataStore.edit { settings ->
            settings[PHONE_NUMBER_KEY] = phoneNumber
        }
    }

    fun getMessageLogs(context: Context): Flow<List<MessageLog>> {
        return context.dataStore.data.map { preferences ->
            val json = preferences[MESSAGE_LOGS_KEY] ?: "[]"
            val type = object : TypeToken<List<MessageLog>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        }
    }

    suspend fun saveMessageLog(context: Context, messageLog: MessageLog) {
        context.dataStore.edit { settings ->
            val currentJson = settings[MESSAGE_LOGS_KEY] ?: "[]"
            val type = object : TypeToken<List<MessageLog>>() {}.type
            val currentList: List<MessageLog> = gson.fromJson(currentJson, type) ?: emptyList()
            val updatedList = (currentList + messageLog).takeLast(100) // Keep last 100 messages
            val json = gson.toJson(updatedList)
            settings[MESSAGE_LOGS_KEY] = json
        }
    }

    suspend fun clearAllMessageLogs(context: Context) {
        context.dataStore.edit { settings ->
            settings[MESSAGE_LOGS_KEY] = "[]"
        }
    }

    fun isTelegramEnabled(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[TELEGRAM_ENABLED_KEY]?.toBoolean() ?: true
        }
    }

    suspend fun setTelegramEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[TELEGRAM_ENABLED_KEY] = enabled.toString()
        }
    }

    fun isPhoneEnabled(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[PHONE_ENABLED_KEY]?.toBoolean() ?: true
        }
    }

    suspend fun setPhoneEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[PHONE_ENABLED_KEY] = enabled.toString()
        }
    }

    fun getCurrentDestination(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[CURRENT_DESTINATION_KEY] ?: "telegram"
        }
    }

    suspend fun setCurrentDestination(context: Context, destination: String) {
        context.dataStore.edit { settings ->
            settings[CURRENT_DESTINATION_KEY] = destination
        }
    }

    fun isServiceRunning(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[SERVICE_RUNNING_KEY]?.toBoolean() ?: true
        }
    }

    suspend fun setServiceRunning(context: Context, running: Boolean) {
        context.dataStore.edit { settings ->
            settings[SERVICE_RUNNING_KEY] = running.toString()
        }
    }

    fun isAutoStartEnabled(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[AUTO_START_ENABLED_KEY]?.toBoolean() ?: true
        }
    }

    suspend fun setAutoStartEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[AUTO_START_ENABLED_KEY] = enabled.toString()
        }
    }

    fun isAutoDeleteLogsEnabled(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[AUTO_DELETE_LOGS_KEY]?.toBoolean() ?: true // Default enabled
        }
    }

    suspend fun setAutoDeleteLogsEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[AUTO_DELETE_LOGS_KEY] = enabled.toString()
        }
    }

    /**
     * Delete logs older than 30 days
     */
    suspend fun deleteOldLogs(context: Context) {
        context.dataStore.edit { settings ->
            val currentJson = settings[MESSAGE_LOGS_KEY] ?: "[]"
            val type = object : TypeToken<List<MessageLog>>() {}.type
            val allLogs: List<MessageLog> = gson.fromJson(currentJson, type) ?: emptyList()
            
            val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
            val recentLogs = allLogs.filter { it.timestamp >= thirtyDaysAgo }
            
            val updatedJson = gson.toJson(recentLogs)
            settings[MESSAGE_LOGS_KEY] = updatedJson
        }
    }
}

data class MessageLog(
    val timestamp: Long,
    val sender: String,
    val message: String,
    val destination: String, // "telegram" or "phone"
    val status: String // "sent", "failed"
)
