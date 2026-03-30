package it.lagioiaproductions.nutrislot.ui.shared

import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

internal class AppBridgeWeightStore(
    private val prefs: SharedPreferences,
    private val key: String
) {

    fun load(): List<WeightEntryUi> {
        val rawJson = prefs.getString(key, null)?.takeIf { it.isNotBlank() } ?: return emptyList()
        val jsonArray = runCatching { JSONArray(rawJson) }.getOrNull() ?: return emptyList()

        return buildList {
            for (index in 0 until jsonArray.length()) {
                jsonArray.optJSONObject(index)
                    ?.toWeightEntryUi()
                    ?.let(::add)
            }
        }.sortedByDescending { it.createdAtEpochMillis }
    }

    fun persist(entries: List<WeightEntryUi>) {
        val jsonArray = JSONArray().apply {
            entries.forEach { entry ->
                put(entry.toJson())
            }
        }

        prefs.edit {
            putString(key, jsonArray.toString())
        }
    }

    fun computeNextEntryId(entries: List<WeightEntryUi>): Long {
        return entries.maxOfOrNull { it.id }?.plus(1L) ?: 1L
    }

    private fun JSONObject.toWeightEntryUi(): WeightEntryUi? {
        val id = optLong("id", -1L)
        val weightKg = optDouble("weightKg", Double.NaN).toFloat()
        val dateKey = optString("dateKey").trim()
        val note = optString("note").trim()
        val createdAtEpochMillis = optLong("createdAtEpochMillis", -1L)

        if (id <= 0L || !weightKg.isFinite() || weightKg <= 0f || dateKey.isBlank()) {
            return null
        }

        return WeightEntryUi(
            id = id,
            weightKg = weightKg,
            dateKey = dateKey,
            note = note,
            createdAtEpochMillis = createdAtEpochMillis.takeIf { it > 0L } ?: id
        )
    }

    private fun WeightEntryUi.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("weightKg", weightKg.toDouble())
            put("dateKey", dateKey)
            put("note", note)
            put("createdAtEpochMillis", createdAtEpochMillis)
        }
    }
}
