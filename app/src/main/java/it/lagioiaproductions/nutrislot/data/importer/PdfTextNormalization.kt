package it.lagioiaproductions.nutrislot.data.importer

import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import java.text.Normalizer

internal object PdfImportTextNormalization {

    const val EXPECTED_WEEKLY_SLOTS = 35
    const val TOTAL_WEEK_DAYS = 7f
    const val LINE_MERGE_TOLERANCE = 4.5f
    const val CONTINUATION_MIN_SLOT_HEADINGS = 8

    private const val BROKEN_RIGHT_APOSTROPHE = "\u00E2\u20AC\u2122"
    private const val BROKEN_EN_DASH = "\u00E2\u20AC\u201C"
    private const val BROKEN_EM_DASH = "\u00E2\u20AC\u201D"
    private const val BROKEN_BULLET = "\u00E2\u20AC\u2022"

    val weekDayAliases: Map<WeekDay, List<String>> = mapOf(
        WeekDay.MONDAY to listOf("lunedi", "lun"),
        WeekDay.TUESDAY to listOf("martedi", "mar"),
        WeekDay.WEDNESDAY to listOf("mercoledi", "mer"),
        WeekDay.THURSDAY to listOf("giovedi", "gio"),
        WeekDay.FRIDAY to listOf("venerdi", "ven"),
        WeekDay.SATURDAY to listOf("sabato", "sab"),
        WeekDay.SUNDAY to listOf("domenica", "dom")
    )

    val mealSlotAliases: Map<MealSlotType, List<String>> = mapOf(
        MealSlotType.BREAKFAST to listOf("colazione"),
        MealSlotType.MORNING_SNACK to listOf(
            "spuntino mattina",
            "spuntino di meta mattina",
            "spuntino meta mattina",
            "meta mattina"
        ),
        MealSlotType.LUNCH to listOf("pranzo"),
        MealSlotType.AFTERNOON_SNACK to listOf(
            "spuntino pomeridiano",
            "spuntino pomeriggio",
            "spuntino del pomeriggio",
            "pomeriggio"
        ),
        MealSlotType.DINNER to listOf("cena")
    )

    fun repairExtractedText(text: String): String {
        if (text.isBlank()) return text

        val repaired = if (text.any { it == '\u00C3' || it == '\u00E2' }) {
            runCatching {
                text.toByteArray(Charsets.ISO_8859_1).toString(Charsets.UTF_8)
            }.getOrDefault(text)
        } else {
            text
        }

        return repaired
            .replace('\u00A0', ' ')
            .replace(BROKEN_RIGHT_APOSTROPHE, "'")
            .replace(BROKEN_EN_DASH, "-")
            .replace(BROKEN_EM_DASH, "-")
            .replace(BROKEN_BULLET, "-")
    }

    fun normalizeMealText(text: String): String {
        return repairExtractedText(text)
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n")
    }

    fun normalizeForMatching(text: String): String {
        return Normalizer.normalize(repairExtractedText(text).lowercase(), Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .replace('\u00A0', ' ')
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    fun matchWeekDayAtStart(normalizedLine: String): WeekDay? {
        return weekDayAliases.entries.firstOrNull { entry ->
            entry.value.any { alias ->
                normalizedLine == alias ||
                        normalizedLine.startsWith("$alias ") ||
                        normalizedLine.startsWith("$alias:") ||
                        normalizedLine.startsWith("$alias -")
            }
        }?.key
    }

    fun isWeekDayHeaderLine(normalizedLine: String): Boolean {
        return weekDayAliases.values.flatten().any { alias -> normalizedLine == alias }
    }

    fun matchMealSlotHeading(
        originalLine: String,
        normalizedLine: String
    ): SlotHeadingMatch? {
        mealSlotAliases.forEach { (slot, aliases) ->
            aliases.forEach { alias ->
                if (normalizedLine == alias) {
                    return SlotHeadingMatch(slot = slot, inlineText = null)
                }

                val prefixes = listOf("$alias:", "$alias -")
                prefixes.firstOrNull { prefix -> normalizedLine.startsWith(prefix) }?.let {
                    val inlineText = repairExtractedText(originalLine)
                        .substringAfter(":", missingDelimiterValue = originalLine)
                        .substringAfter(" - ", missingDelimiterValue = originalLine)
                        .trim()

                    return SlotHeadingMatch(
                        slot = slot,
                        inlineText = inlineText.takeIf { it.isNotBlank() }
                    )
                }
            }
        }

        return null
    }

    fun countDistinctWeekDays(normalizedPageText: String): Int {
        return weekDayAliases.entries.count { (_, aliases) ->
            aliases.any { alias -> normalizedPageText.contains(alias) }
        }
    }

    fun countMealSlotHeadingOccurrences(normalizedPageText: String): Int {
        return mealSlotAliases.values.flatten().sumOf { alias ->
            "\\b${Regex.escape(alias)}\\b".toRegex().findAll(normalizedPageText).count()
        }
    }

    fun computeWeeklyHeaderScore(normalizedPageText: String): Int {
        val weekdaysScore = countDistinctWeekDays(normalizedPageText) * 10
        val slotsScore = countMealSlotHeadingOccurrences(normalizedPageText)
        return weekdaysScore + slotsScore
    }
}
