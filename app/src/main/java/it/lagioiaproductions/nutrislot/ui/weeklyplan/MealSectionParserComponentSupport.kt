package it.lagioiaproductions.nutrislot.ui.weeklyplan

import it.lagioiaproductions.nutrislot.domain.model.WeeklyFrequencyTargetSupport
import it.lagioiaproductions.nutrislot.ui.shared.normalizeMealUiLine

private val EXAMPLE_PREFIX_REGEX = Regex(
    pattern = "^(?:es\\.?|ad\\s+es\\.?)\\s*",
    option = RegexOption.IGNORE_CASE
)

private val QUANTIFIED_MEAL_NOTE_REGEX = Regex(
    pattern = """(?:n\.?\s*\d+|max\s*\d+|almeno\s*\d+|\d+(?:[.,]\d+)?)""",
    options = setOf(RegexOption.IGNORE_CASE)
)

internal fun renderComponentLine(component: ParsedMealComponent): String {
    val main = component.alternatives.joinToString(separator = " / ").normalizeMealUiLine()
    if (main.isBlank()) return ""

    val notes = mutableListOf<String>()

    if (component.mealQuantityNotes.isNotEmpty()) {
        notes += component.mealQuantityNotes.distinct()
    }

    if (component.weeklyQuantityNotes.isNotEmpty()) {
        notes += component.weeklyQuantityNotes.distinct()
    }

    if (component.exampleNotes.isNotEmpty()) {
        notes += "es. ${component.exampleNotes.distinct().joinToString(separator = "; ")}"
    }

    if (component.genericNotes.isNotEmpty()) {
        notes += component.genericNotes.distinct()
    }

    return if (notes.isEmpty()) {
        main
    } else {
        "$main (${notes.joinToString(separator = "; ")})"
    }
}

internal fun parseComponentSegment(rawSegment: String): ParsedComponentParseResult? {
    var working = rawSegment
        .normalizeBreadQualifierShorthand()
        .normalizeMealUiLine()
    if (working.isBlank()) return null

    val attachAsAlternativeToPrevious = startsWithAlternativePrefix(working)
    if (attachAsAlternativeToPrevious) {
        working = removeAlternativePrefix(working)
    }

    val notesExtraction = extractParentheticalNotes(working)
    val alternatives = splitTopLevelAlternatives(notesExtraction.baseText)
        .map(::restoreCollapsedWhitespace)
        .map(String::normalizeMealUiLine)
        .filter { it.isNotBlank() }

    if (alternatives.isEmpty()) return null

    val component = MutableParsedMealComponent(
        alternatives = alternatives.toMutableList()
    )

    notesExtraction.notes.forEach { rawNote ->
        classifyParentheticalNote(rawNote, component)
    }

    return ParsedComponentParseResult(
        component = component,
        attachAsAlternativeToPrevious = attachAsAlternativeToPrevious
    )
}

internal fun MutableParsedMealComponent.merge(other: MutableParsedMealComponent) {
    alternatives += other.alternatives
    exampleNotes += other.exampleNotes
    genericNotes += other.genericNotes
    weeklyQuantityNotes += other.weeklyQuantityNotes
    mealQuantityNotes += other.mealQuantityNotes

    alternatives.deduplicateInPlace()
    exampleNotes.deduplicateInPlace()
    genericNotes.deduplicateInPlace()
    weeklyQuantityNotes.deduplicateInPlace()
    mealQuantityNotes.deduplicateInPlace()
}

internal fun MutableParsedMealComponent.toImmutable(): ParsedMealComponent {
    return ParsedMealComponent(
        alternatives = alternatives.distinct(),
        exampleNotes = exampleNotes.distinct(),
        genericNotes = genericNotes.distinct(),
        weeklyQuantityNotes = weeklyQuantityNotes.distinct(),
        mealQuantityNotes = mealQuantityNotes.distinct()
    )
}

private fun classifyParentheticalNote(
    rawNote: String,
    component: MutableParsedMealComponent
) {
    val normalized = rawNote
        .trim()
        .removePrefix("(")
        .removeSuffix(")")
        .normalizeMealUiLine()

    if (normalized.isBlank()) return

    val matchable = normalized.normalizeMealParserMatchable()

    when {
        EXAMPLE_PREFIX_REGEX.containsMatchIn(matchable) -> {
            val examplePayload = EXAMPLE_PREFIX_REGEX.replace(matchable, "").normalizeMealUiLine()
            component.exampleNotes += examplePayload.ifBlank { normalized }
        }

        WeeklyFrequencyTargetSupport.parseFrequencyTargetRule(matchable) != null -> {
            component.weeklyQuantityNotes += normalized
        }

        matchable.contains("nel pasto") && QUANTIFIED_MEAL_NOTE_REGEX.containsMatchIn(matchable) -> {
            component.mealQuantityNotes += normalized
        }

        else -> {
            component.genericNotes += normalized
        }
    }
}

private fun extractParentheticalNotes(text: String): ParentheticalExtraction {
    val notes = mutableListOf<String>()
    val baseBuilder = StringBuilder()
    val noteBuilder = StringBuilder()
    var depth = 0

    text.forEach { char ->
        when {
            char == '(' -> {
                if (depth == 0) {
                    noteBuilder.clear()
                } else {
                    noteBuilder.append(char)
                }
                depth += 1
            }

            char == ')' && depth > 0 -> {
                depth -= 1
                if (depth == 0) {
                    val note = noteBuilder.toString().normalizeMealUiLine()
                    if (note.isNotBlank()) {
                        notes += note
                    }
                } else {
                    noteBuilder.append(char)
                }
            }

            depth > 0 -> {
                noteBuilder.append(char)
            }

            else -> {
                baseBuilder.append(char)
            }
        }
    }

    return ParentheticalExtraction(
        baseText = baseBuilder.toString()
            .normalizeAlternativeMarkers()
            .normalizeBreadQualifierShorthand()
            .normalizeMealUiLine(),
        notes = notes
    )
}

private fun <T> MutableList<T>.deduplicateInPlace() {
    val distinctValues = distinct()
    clear()
    addAll(distinctValues)
}

private data class ParentheticalExtraction(
    val baseText: String,
    val notes: List<String>
)

internal data class ParsedComponentParseResult(
    val component: MutableParsedMealComponent,
    val attachAsAlternativeToPrevious: Boolean
)

internal data class MutableParsedMealComponent(
    val alternatives: MutableList<String> = mutableListOf(),
    val exampleNotes: MutableList<String> = mutableListOf(),
    val genericNotes: MutableList<String> = mutableListOf(),
    val weeklyQuantityNotes: MutableList<String> = mutableListOf(),
    val mealQuantityNotes: MutableList<String> = mutableListOf()
)
