package com.example.noticanswer

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.settingsDataStore by preferencesDataStore(name = "notification_settings")

object SettingsRepository {

    private val AUTO_ENABLED = booleanPreferencesKey("auto_enabled")
    private val START_HOUR = intPreferencesKey("start_hour")
    private val END_HOUR = intPreferencesKey("end_hour")
    private val COUNT = intPreferencesKey("count")
    private val QUESTIONS_PER_SESSION = intPreferencesKey("questions_per_session")
    private val MIN_INTERVAL_MINUTES = intPreferencesKey("min_interval_minutes")
    private val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
    private val QUIET_START_HOUR = intPreferencesKey("quiet_start_hour")
    private val QUIET_END_HOUR = intPreferencesKey("quiet_end_hour")

    fun settingsFlow(context: Context): Flow<NotificationSettings> {
        return context.settingsDataStore.data.map { prefs ->
            NotificationSettings(
                autoEnabled = prefs[AUTO_ENABLED] ?: false,
                startHour = prefs[START_HOUR] ?: 10,
                endHour = prefs[END_HOUR] ?: 22,
                count = prefs[COUNT] ?: 3,
                questionsPerSession = prefs[QUESTIONS_PER_SESSION] ?: 1,
                minIntervalMinutes = prefs[MIN_INTERVAL_MINUTES] ?: 30,
                quietHoursEnabled = prefs[QUIET_HOURS_ENABLED] ?: false,
                quietStartHour = prefs[QUIET_START_HOUR] ?: 22,
                quietEndHour = prefs[QUIET_END_HOUR] ?: 6
            )
        }
    }

    suspend fun getSettings(context: Context): NotificationSettings {
        return settingsFlow(context).first()
    }

    suspend fun saveSettings(
        context: Context,
        settings: NotificationSettings
    ) {
        context.settingsDataStore.edit { prefs ->
            prefs[AUTO_ENABLED] = settings.autoEnabled
            prefs[START_HOUR] = settings.startHour
            prefs[END_HOUR] = settings.endHour
            prefs[COUNT] = settings.count
            prefs[QUESTIONS_PER_SESSION] = settings.questionsPerSession
            prefs[MIN_INTERVAL_MINUTES] = settings.minIntervalMinutes
            prefs[QUIET_HOURS_ENABLED] = settings.quietHoursEnabled
            prefs[QUIET_START_HOUR] = settings.quietStartHour
            prefs[QUIET_END_HOUR] = settings.quietEndHour
        }
    }

    suspend fun setAutoEnabled(
        context: Context,
        enabled: Boolean
    ) {
        context.settingsDataStore.edit { prefs ->
            prefs[AUTO_ENABLED] = enabled
        }
    }
}