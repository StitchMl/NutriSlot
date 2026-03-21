package it.lagioiaproductions.nutrislot.ui.shared

import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

internal class AppBridgeCalorieJournalStore(
    private val prefs: SharedPreferences,
    private val key: String
) {

    fun load(): Map<String, CalorieDayLogUi> {
        val raw = prefs.getString(key, null) ?: return emptyMap()

        return runCatching {
            val root = JSONObject(raw)
            val result = linkedMapOf<String, CalorieDayLogUi>()
            val keys = root.keys()

            while (keys.hasNext()) {
                val dayKey = keys.next()
                val dayObject = root.optJSONObject(dayKey) ?: continue
                result[dayKey] = dayObject.toDayLogUi()
            }

            result.toMap()
        }.getOrDefault(emptyMap())
    }

    fun persist(journal: Map<String, CalorieDayLogUi>) {
        val root = JSONObject()

        journal.forEach { (dayKey, dayLog) ->
            root.put(dayKey, dayLog.toJson())
        }

        prefs.edit {
            putString(key, root.toString())
        }
    }

    fun computeNextEntryId(journal: Map<String, CalorieDayLogUi>): Long {
        return journal.values
            .flatMap { it.entries }
            .maxOfOrNull { it.id }
            ?.plus(1L)
            ?: 1L
    }

    private fun JSONObject.toDayLogUi(): CalorieDayLogUi {
        val goalKcal = if (has("goalKcal") && !isNull("goalKcal")) {
            optInt("goalKcal").takeIf { it > 0 }
        } else {
            null
        }

        val entriesJson = optJSONArray("entries") ?: JSONArray()
        val entries = buildList {
            for (index in 0 until entriesJson.length()) {
                val entryObject = entriesJson.optJSONObject(index) ?: continue
                add(entryObject.toEntryUi())
            }
        }.sortedByDescending { it.id }

        return CalorieDayLogUi(
            goalKcal = goalKcal,
            entries = entries
        )
    }

    private fun JSONObject.toEntryUi(): CalorieJournalEntryUi {
        return CalorieJournalEntryUi(
            id = optLong("id"),
            title = optString("title"),
            subtitle = optString("subtitle"),
            calories = optInt("calories"),
            protein = optInt("protein"),
            carbs = optInt("carbs"),
            fibre = optInt("fibre"),
            section = optString("section")
                .takeIf { it.isNotBlank() }
                ?.let { enumName ->
                    runCatching { CalorieJournalSection.valueOf(enumName) }.getOrNull()
                }
                ?: CalorieJournalSection.SNACK,
            timeLabel = optString("timeLabel"),
            sourceLabel = optString("sourceLabel"),
            plannerConsumptionId = optString("plannerConsumptionId")
                .takeIf { it.isNotBlank() }
        )
    }

    private fun CalorieDayLogUi.toJson(): JSONObject {
        return JSONObject().apply {
            put("goalKcal", goalKcal ?: JSONObject.NULL)
            put(
                "entries",
                JSONArray().apply {
                    entries.forEach { entry ->
                        put(entry.toJson())
                    }
                }
            )
        }
    }

    private fun CalorieJournalEntryUi.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("subtitle", subtitle)
            put("calories", calories)
            put("protein", protein)
            put("carbs", carbs)
            put("fibre", fibre)
            put("section", section.name)
            put("timeLabel", timeLabel)
            put("sourceLabel", sourceLabel)
            put("plannerConsumptionId", plannerConsumptionId ?: JSONObject.NULL)
        }
    }
}