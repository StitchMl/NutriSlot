package it.lagioiaproductions.nutrislot.ui.weeklyplan

import java.text.Normalizer
import kotlin.math.min

object WeeklyQuantityChecklistBuilder {
    private const val MAX_WEEKLY_CHECKLIST_ITEMS = 8

    private val gramsOrMlPattern = Regex(
        pattern = "\\b(\\d+(?:[.,]\\d+)?)\\s*(g|gr|grammi|ml)\\s*(?:di\\s+)?([a-zA-ZàèéìòùÀÈÉÌÒÙ' ]{2,})",
        option = RegexOption.IGNORE_CASE
    )

    private val countedFoodPattern = Regex(
        pattern = "\\b(\\d+)\\s+([a-zA-ZàèéìòùÀÈÉÌÒÙ' ]{2,})",
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
                .distinctBy { it.key }

            slotEntries.forEach { entry ->
                plannedEntriesByKey.getOrPut(entry.key) { mutableListOf() }.add(entry)
            }

            if (slot.isActuallyCompletedThisWeek) {
                slotEntries.forEach { entry ->
                    consumedCountByKey[entry.key] = (consumedCountByKey[entry.key] ?: 0) + 1
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

                WeeklyQuantityChecklistItemUi(
                    id = key,
                    title = preferredEntry.title,
                    portionText = preferredEntry.portionText,
                    targetTimes = entries.size,
                    consumedTimes = min(consumedCountByKey[key] ?: 0, entries.size)
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
        val segments = mealText
            .stripMealNutritionBlock()
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace("•", "\n")
            .split("\n", "+", ";", ",")
            .map { it.trim() }
            .filter { it.isNotBlank() && !isNutritionLine(it) }

        val entries = buildList {
            segments.forEach { segment ->
                extractChecklistEntryFromSegment(segment)?.let(::add)
            }
        }

        return entries.distinctBy { it.key }
    }

    private fun extractChecklistEntryFromSegment(segment: String): ChecklistEntryDraft? {
        val normalizedSegment = segment.replace(Regex("\\s+"), " ").trim()
        if (normalizedSegment.isBlank() || isChecklistNoise(normalizedSegment) || isNutritionLine(normalizedSegment)) {
            return null
        }

        gramsOrMlPattern.find(normalizedSegment)?.let { match ->
            val amount = match.groupValues[1].replace(",", ".").trim()
            val unit = normalizeMeasurementUnit(match.groupValues[2])
            val rawFood = cleanupFoodTail(match.groupValues[3])
            val title = formatChecklistTitle(rawFood)
            if (title.isBlank()) return null
            return ChecklistEntryDraft(
                key = normalizeChecklistKey(title),
                title = title,
                portionText = "$amount $unit"
            )
        }

        countedFoodPattern.find(normalizedSegment)?.let { match ->
            val quantity = match.groupValues[1].trim()
            val rawPhrase = cleanupFoodTail(match.groupValues[2])
            if (rawPhrase.isBlank() || isChecklistNoise(rawPhrase) || isNutritionLine(rawPhrase)) return null
            val refined = refineCountedFoodPhrase(quantity, rawPhrase) ?: return null
            return ChecklistEntryDraft(
                key = normalizeChecklistKey(refined.title),
                title = refined.title,
                portionText = refined.portionText
            )
        }

        return null
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
            .substringBefore(" oppure ")
            .substringBefore(" con ")
            .substringBefore(" accompagnato")
            .substringBefore(" a scelta")
            .substringBefore(" q.b")
            .substringBefore("(")
            .trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("^(di|del|della|dei|degli|delle)\\s+"), "")
            .replace(Regex("\\b(al|alla|ai|alle|con|e|oppure)\\b.*$"), "")
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

    private data class ChecklistEntryDraft(val key: String, val title: String, val portionText: String?)
    private data class RefinedChecklistPhrase(val title: String, val portionText: String)
}