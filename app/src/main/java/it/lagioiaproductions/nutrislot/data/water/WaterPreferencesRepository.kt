package it.lagioiaproductions.nutrislot.data.water

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import it.lagioiaproductions.nutrislot.notifications.water.WaterReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

private val Context.waterDataStore by preferencesDataStore(name = "water_tracker_preferences")

class WaterPreferencesRepository(
    private val context: Context
) {
    private object Keys {
        val targetMl = intPreferencesKey("water_target_ml")
        val consumedMl = intPreferencesKey("water_consumed_ml")
        val remindersEnabled = booleanPreferencesKey("water_reminders_enabled")
        val reminderIntervalMinutes = intPreferencesKey("water_reminder_interval_minutes")
        val containerPresets = stringPreferencesKey("water_container_presets")
        val lastTrackedDayKey = stringPreferencesKey("water_last_tracked_day_key")
    }

    companion object {
        private const val MAX_PRESETS = 3
        private val DEFAULT_PRESETS = listOf(250, 500, 600)
    }

    val preferencesFlow: Flow<WaterStoredPreferences> = context.waterDataStore.data.map { prefs ->
        WaterStoredPreferences(
            targetMl = (prefs[Keys.targetMl] ?: 2000).coerceAtLeast(0),
            consumedMl = (prefs[Keys.consumedMl] ?: 0).coerceAtLeast(0),
            remindersEnabled = prefs[Keys.remindersEnabled] ?: false,
            reminderIntervalMinutes = (prefs[Keys.reminderIntervalMinutes] ?: 90).coerceAtLeast(15),
            containerPresets = decodePresets(prefs[Keys.containerPresets]),
            lastTrackedDayKey = prefs[Keys.lastTrackedDayKey] ?: ""
        )
    }

    suspend fun ensureCurrentDay() {
        context.waterDataStore.edit { prefs ->
            syncDayIfNeeded(prefs)
            if (prefs[Keys.containerPresets].isNullOrBlank()) {
                prefs[Keys.containerPresets] = encodePresets(DEFAULT_PRESETS)
            }
        }
    }

    suspend fun addConsumedMl(
        amountMl: Int,
        saveAsPresetIfMissing: Boolean
    ) {
        val normalizedAmount = amountMl.coerceAtLeast(1)

        context.waterDataStore.edit { prefs ->
            syncDayIfNeeded(prefs)

            val currentConsumed = (prefs[Keys.consumedMl] ?: 0).coerceAtLeast(0)
            prefs[Keys.consumedMl] = currentConsumed + normalizedAmount

            if (saveAsPresetIfMissing) {
                val currentPresets = decodePresets(prefs[Keys.containerPresets])
                if (normalizedAmount !in currentPresets) {
                    prefs[Keys.containerPresets] =
                        encodePresets(appendPreset(currentPresets, normalizedAmount))
                }
            }
        }
    }

    suspend fun removeConsumedMl(amountMl: Int) {
        val normalizedAmount = amountMl.coerceAtLeast(1)

        context.waterDataStore.edit { prefs ->
            syncDayIfNeeded(prefs)

            val currentConsumed = (prefs[Keys.consumedMl] ?: 0).coerceAtLeast(0)
            prefs[Keys.consumedMl] = (currentConsumed - normalizedAmount).coerceAtLeast(0)
        }
    }

    suspend fun resetConsumedMl() {
        context.waterDataStore.edit { prefs ->
            prefs[Keys.lastTrackedDayKey] = currentDayKey()
            prefs[Keys.consumedMl] = 0
        }
    }

    suspend fun setTargetMl(targetMl: Int) {
        context.waterDataStore.edit { prefs ->
            syncDayIfNeeded(prefs)
            prefs[Keys.targetMl] = targetMl.coerceAtLeast(0)
        }
    }

    suspend fun setReminderConfig(
        enabled: Boolean,
        intervalMinutes: Int
    ) {
        val normalizedInterval = intervalMinutes.coerceAtLeast(15)

        context.waterDataStore.edit { prefs ->
            syncDayIfNeeded(prefs)
            prefs[Keys.remindersEnabled] = enabled
            prefs[Keys.reminderIntervalMinutes] = normalizedInterval
        }

        if (enabled) {
            WaterReminderScheduler.schedule(context, normalizedInterval)
        } else {
            WaterReminderScheduler.cancel(context)
        }
    }

    suspend fun addContainerPreset(amountMl: Int) {
        val normalizedAmount = amountMl.coerceAtLeast(50)

        context.waterDataStore.edit { prefs ->
            syncDayIfNeeded(prefs)

            val currentPresets = decodePresets(prefs[Keys.containerPresets])
            prefs[Keys.containerPresets] =
                encodePresets(appendPreset(currentPresets, normalizedAmount))
        }
    }

    suspend fun removeContainerPreset(amountMl: Int) {
        context.waterDataStore.edit { prefs ->
            syncDayIfNeeded(prefs)

            val updated = decodePresets(prefs[Keys.containerPresets])
                .filterNot { it == amountMl }

            prefs[Keys.containerPresets] = encodePresets(
                updated.ifEmpty { DEFAULT_PRESETS }
            )
        }
    }

    private fun syncDayIfNeeded(prefs: MutablePreferences) {
        val today = currentDayKey()
        val storedDay = prefs[Keys.lastTrackedDayKey]

        if (storedDay != today) {
            prefs[Keys.lastTrackedDayKey] = today
            prefs[Keys.consumedMl] = 0
        }
    }

    private fun currentDayKey(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        return "$year-$dayOfYear"
    }

    private fun decodePresets(raw: String?): List<Int> {
        if (raw.isNullOrBlank()) return DEFAULT_PRESETS

        val values = raw.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .map { it.coerceAtLeast(50) }
            .distinct()

        return values.ifEmpty { DEFAULT_PRESETS }.takeLast(MAX_PRESETS)
    }

    private fun encodePresets(values: List<Int>): String {
        return values
            .map { it.coerceAtLeast(50) }
            .distinct()
            .takeLast(MAX_PRESETS)
            .joinToString(",")
    }

    private fun appendPreset(
        current: List<Int>,
        amountMl: Int
    ): List<Int> {
        val normalized = amountMl.coerceAtLeast(50)
        return (current.filterNot { it == normalized } + normalized).takeLast(MAX_PRESETS)
    }
}