package it.lagioiaproductions.nutrislot.ui.weeklyplan

import java.text.Normalizer
import kotlin.math.min

object WeeklyQuantityChecklistBuilder {
    private const val MAX_WEEKLY_CHECKLIST_ITEMS = 8

    private val gramsOrMlPattern = Regex(
        pattern = "\\b(\\d+(?:[.,]\\d+)?)\\s*(g|gr|grammi|ml)\\s*(?:di\\s+)?([a-zA-ZàèéìòùÀÈÉÌÒÙ' /]{2,})",
        option = RegexOption.IGNORE_CASE
    )

    private val countedFoodPattern = Regex(
        pattern = "\\b(\\d+)\\s+([a-zA-ZàèéìòùÀÈÉÌÒÙ' /]{2,})",
        option = RegexOption.IGNORE_CASE
    )

    private val checklistMeasurePrefixes = listOf(
        "scatoletta di ", "scatolette di ", "vasetto di ", "vasetti di ",
        "cucchiaio di ", "cucchiai di ", "cucchiaino di ", "cucchiaini di ",
        "fetta di ", "fette di ", "porzione di ", "porzioni di ", "pezzo di ", "pezzi di "
    )

    private val checklistNoiseWords = setOf(
        "colazione", "pranzo", "cena", "spuntino", "spuntino mattina", "spuntino pomeriggio",
        "giorno", "settimana", "volta", "volte", "opzione", "opzioni", "scelta", "libero", "qb", "q b"
    )

    private val ignoredChecklistKeys = setOf(
        "verdure", "ortaggi", "insalata", "frutta", "olio", "olio evo",
        "acqua", "sale", "spezie", "limone", "te", "caffe"
    )

    fun build(slots: List<WeeklySlotUi>): List<WeeklyQuantityChecklistItemUi> {
        if (slots.isEmpty()) return emptyList()

        val plannedEntriesByKey = linkedMapOf<String, MutableList<ChecklistEntryDraft>>()
        val consumedCountByKey = linkedMapOf<String, Int>()

        slots.forEach { slot ->
            val slotEntries = extractChecklistEntriesFromMealText(slot.displayedMealText)

            slotEntries.forEach { entry ->
                plannedEntriesByKey.getOrPut(entry.key) { mutableListOf() }.add(entry)
            }

            if (slot.isActuallyCompletedThisWeek) {
                slotEntries.forEach { entry ->
                    consumedCountByKey[entry.key] = (consumedCountByKey[entry.key] ?: 0) + entry.consumedIncrement
                }
            }
        }

        return plannedEntriesByKey
            .map { (key, entries) ->
                val preferredEntry = entries
                    .groupBy { entry -> entry.title.lowercase() to (entry.portionText?.lowercase() ?: "") }
                    .maxByOrNull { (_, groupedEntries) -> groupedEntries.size }
                    ?.value
                    ?.firstOrNull()
                    ?: entries.first()

                val targetTimes = entries.sumOf { it.targetIncrement }
                val consumedTimes = min(consumedCountByKey[key] ?: 0, targetTimes)

                WeeklyQuantityChecklistItemUi(
                    id = key,
                    title = preferredEntry.title,
                    portionText = preferredEntry.portionText,
                    targetTimes = targetTimes,
                    consumedTimes = consumedTimes
                )
            }
            .filter(::shouldShowChecklistItem)
            .sortedWith(compareBy({ it.isCompleted }, { -it.targetTimes }, { it.title }))
            .take(MAX_WEEKLY_CHECKLIST_ITEMS)
    }

    private fun shouldShowChecklistItem(item: WeeklyQuantityChecklistItemUi): Boolean {
        val normalizedKey = normalizeChecklistKey(item.title)
        if (normalizedKey.isBlank()) return false
        if (normalizedKey in ignoredChecklistKeys) return false
        return item.portionText != null || item.targetTimes >= 2
    }

    private fun extractChecklistEntriesFromMealText(mealText: String): List<ChecklistEntryDraft> {
        val components = parseMealStructuredSections(mealText)
            .flatMap { it.components }

        return components.mapNotNull(::extractChecklistEntryFromComponent)
    }

    private fun extractChecklistEntryFromComponent(
        component: ParsedMealComponent
    ): ChecklistEntryDraft? {
        val componentLabel = component.alternatives
            .map(::normalizeChecklistSource)
            .filter { it.isNotBlank() }
            .joinToString(separator = " / ")
            .trim()

        if (componentLabel.isBlank()) return null
        if (isChecklistNoise(componentLabel) || isNutritionLine(componentLabel)) return null

        val weeklyTarget = component.weeklyQuantityNotes
            .firstNotNullOfOrNull(::extractWeeklyTargetCount)

        val mealPortionNote = component.mealQuantityNotes
            .firstOrNull()
            ?.let(::normalizeMealQuantityNote)

        gramsOrMlPattern.find(componentLabel)?.let { match ->
            val amount = match.groupValues[1].replace(",", ".").trim()
            val unit = normalizeMeasurementUnit(match.groupValues[2])
            val rawFood = cleanupFoodTail(match.groupValues[3])
            val title = formatChecklistTitle(rawFood)
            if (title.isBlank()) return null

            return ChecklistEntryDraft(
                key = normalizeChecklistKey(title),
                title = title,
                portionText = mealPortionNote ?: "$amount $unit",
                targetIncrement = weeklyTarget ?: 1,
                consumedIncrement = 1
            )
        }

        countedFoodPattern.find(componentLabel)?.let { match ->
            val quantity = match.groupValues[1].trim()
            val rawPhrase = cleanupFoodTail(match.groupValues[2])
            if (rawPhrase.isBlank() || isChecklistNoise(rawPhrase) || isNutritionLine(rawPhrase)) return null

            val refined = refineCountedFoodPhrase(quantity, rawPhrase) ?: return null
            return ChecklistEntryDraft(
                key = normalizeChecklistKey(refined.title),
                title = refined.title,
                portionText = mealPortionNote ?: refined.portionText,
                targetIncrement = weeklyTarget ?: 1,
                consumedIncrement = 1
            )
        }

        val title = formatChecklistTitle(cleanupFoodTail(componentLabel))
        if (title.isBlank() || isChecklistNoise(title) || isNutritionLine(title)) return null

        if (weeklyTarget == null && mealPortionNote == null) {
            return null
        }

        return ChecklistEntryDraft(
            key = normalizeChecklistKey(title),
            title = title,
            portionText = mealPortionNote,
            targetIncrement = weeklyTarget ?: 1,
            consumedIncrement = 1
        )
    }

    private fun extractWeeklyTargetCount(note: String): Int? {
        val normalized = note
            .lowercase()
            .replace("’", "'")
            .replace(Regex("\\s+"), " ")
            .trim()

        return when {
            normalized.contains("a settimana") -> {
                Regex("(?:n\\.?\\s*)?(\\d+)").find(normalized)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: Regex("max\\s*(\\d+)").find(normalized)?.groupValues?.getOrNull(1)?.toIntOrNull()
            }
            else -> null
        }
    }

    private fun normalizeMealQuantityNote(note: String): String {
        return note
            .replace(Regex("\\bnel\\s+pasto\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim(',', ';')
            .ifBlank { note }
    }

    private fun refineCountedFoodPhrase(quantity: String, phrase: String): RefinedChecklistPhrase? {
        val cleanedPhrase = phrase.replace(Regex("\\s+"), " ").trim()
        if (cleanedPhrase.isBlank() || isChecklistNoise(cleanedPhrase) || isNutritionLine(cleanedPhrase)) return null

        val lowered = cleanedPhrase.lowercase()
        val measurePrefix = checklistMeasurePrefixes.firstOrNull { lowered.startsWith(it) }
        if (measurePrefix != null) {
            val tail = lowered.removePrefix(measurePrefix).removePrefix("di ").trim()
            val title = formatChecklistTitle(cleanupFoodTail(tail))
            if (title.isBlank() || isChecklistNoise(title) || isNutritionLine(title)) return null
            return RefinedChecklistPhrase(title = title, portionText = "$quantity ${measurePrefix.trim()}")
        }

        val title = formatChecklistTitle(cleanupFoodTail(cleanedPhrase))
        if (title.isBlank() || isNutritionLine(title)) return null
        return RefinedChecklistPhrase(title = title, portionText = "$quantity ${cleanedPhrase.lowercase()}")
    }

    private fun cleanupFoodTail(raw: String): String {
        return raw
            .substringBefore(" con ")
            .substringBefore(" accompagnato")
            .substringBefore(" a scelta")
            .substringBefore(" q.b")
            .substringBefore("(")
            .trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("^(di|del|della|dei|degli|delle)\\s+"), "")
            .replace(Regex("\\b(al|alla|ai|alle|con|e)\\b.*$"), "")
            .trim()
            .removeSuffix(".")
    }

    private fun normalizeChecklistSource(raw: String): String {
        return raw
            .replace(Regex("\\s+"), " ")
            .trim()
            .removeSuffix(".")
    }

    private fun formatChecklistTitle(raw: String): String {
        val cleaned = raw.lowercase().replace(Regex("\\s+"), " ").trim()
        if (cleaned.isBlank()) return ""
        return cleaned.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { token ->
                token.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase() else char.toString()
                }
            }
    }

    private fun normalizeChecklistKey(raw: String): String {
        return Normalizer.normalize(raw.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace("'", " ")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun normalizeMeasurementUnit(raw: String): String {
        return when (raw.lowercase()) {
            "gr", "grammi" -> "g"
            else -> raw.lowercase()
        }
    }

    private fun isChecklistNoise(text: String): Boolean {
        val normalized = normalizeChecklistKey(text)
        if (normalized.isBlank()) return true
        return checklistNoiseWords.any { noise -> normalized == noise || normalized.startsWith("$noise ") }
    }

    private data class ChecklistEntryDraft(
        val key: String,
        val title: String,
        val portionText: String?,
        val targetIncrement: Int,
        val consumedIncrement: Int
    )

    private data class RefinedChecklistPhrase(
        val title: String,
        val portionText: String
    )
}