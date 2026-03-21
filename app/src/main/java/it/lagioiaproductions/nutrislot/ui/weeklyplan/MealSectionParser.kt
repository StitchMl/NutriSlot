package it.lagioiaproductions.nutrislot.ui.weeklyplan

import it.lagioiaproductions.nutrislot.ui.shared.MealVisualInfo
import it.lagioiaproductions.nutrislot.ui.shared.inferMealVisualInfo
import it.lagioiaproductions.nutrislot.ui.shared.normalizeMealUiLine
import it.lagioiaproductions.nutrislot.ui.shared.protectConnectedMealPhrases
import it.lagioiaproductions.nutrislot.ui.shared.restoreConnectedMealPhrases
import it.lagioiaproductions.nutrislot.ui.shared.shouldAppendMealContinuation

internal data class ParsedMealSectionUi(
    val lines: List<String>,
    val visualInfo: MealVisualInfo
)

internal fun parseMealSections(text: String): List<List<String>> {
    return parseMealSectionVisuals(text).map { it.lines }
}

internal fun parseMealSectionVisuals(text: String): List<ParsedMealSectionUi> {
    val rawLines = protectConnectedMealPhrases(text)
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .replace("•", "\n• ")
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }

    if (rawLines.isEmpty()) {
        return emptyList()
    }

    val sections = mutableListOf<List<String>>()
    var currentSection = mutableListOf<String>()

    fun flushSection() {
        val restoredLines = currentSection
            .map(::restoreConnectedMealPhrases)
            .map(String::normalizeMealUiLine)
            .filter { it.isNotBlank() }

        if (restoredLines.isNotEmpty()) {
            sections += restoredLines
        }
        currentSection = mutableListOf()
    }

    rawLines.forEach { rawLine ->
        val normalizedLine = rawLine
            .trim()
            .removePrefix("•")
            .removePrefix("-")
            .removePrefix("–")
            .removePrefix("—")
            .trim()
            .normalizeMealUiLine()

        if (normalizedLine.isBlank()) {
            return@forEach
        }

        if (normalizedLine == "+") {
            flushSection()
            return@forEach
        }

        if (isStandaloneMealHeading(normalizedLine)) {
            return@forEach
        }

        val previous = currentSection.lastOrNull()
        if (previous != null && shouldAppendMealContinuation(previous, normalizedLine)) {
            currentSection[currentSection.lastIndex] = "$previous $normalizedLine"
                .normalizeMealUiLine()
        } else {
            currentSection += normalizedLine
        }
    }

    flushSection()

    return sections.map { lines ->
        ParsedMealSectionUi(
            lines = lines,
            visualInfo = inferMealVisualInfo(lines.joinToString(separator = " "))
        )
    }
}

private fun isStandaloneMealHeading(line: String): Boolean {
    return when (line.lowercase()) {
        "colazione",
        "spuntino mattina",
        "spuntino di meta mattina",
        "spuntino meta mattina",
        "meta mattina",
        "pranzo",
        "spuntino pomeridiano",
        "spuntino pomeriggio",
        "spuntino del pomeriggio",
        "pomeriggio",
        "cena" -> true

        else -> false
    }
}