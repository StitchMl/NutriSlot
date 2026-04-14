package it.lagioiaproductions.nutrislot.ui.weeklyplan

import it.lagioiaproductions.nutrislot.ui.weeklyplan.parser.MutableParsedMealComponent
import it.lagioiaproductions.nutrislot.ui.weeklyplan.parser.isNutritionLine
import it.lagioiaproductions.nutrislot.ui.weeklyplan.parser.isStandaloneAlternativeSeparatorLine
import it.lagioiaproductions.nutrislot.ui.weeklyplan.parser.isStandaloneMealHeading
import it.lagioiaproductions.nutrislot.ui.weeklyplan.parser.mealLineStartsWithBullet
import it.lagioiaproductions.nutrislot.ui.weeklyplan.parser.merge
import it.lagioiaproductions.nutrislot.ui.weeklyplan.parser.normalizeMealParserLine
import it.lagioiaproductions.nutrislot.ui.weeklyplan.parser.normalizeMealParserLines
import it.lagioiaproductions.nutrislot.ui.weeklyplan.parser.parseComponentSegment
import it.lagioiaproductions.nutrislot.ui.weeklyplan.parser.renderComponentLine
import it.lagioiaproductions.nutrislot.ui.weeklyplan.parser.stripMealNutritionBlock
import it.lagioiaproductions.nutrislot.ui.weeklyplan.parser.toImmutable

internal data class MealVisualInfo(
    val emoji: String,
    val semanticKey: String
)

internal data class ParsedMealSectionUi(
    val lines: List<String>,
    val visualInfo: MealVisualInfo
)

internal data class ParsedMealComponent(
    val alternatives: List<String>,
    val exampleNotes: List<String>,
    val genericNotes: List<String>,
    val weeklyQuantityNotes: List<String>,
    val mealQuantityNotes: List<String>
)

internal data class ParsedMealStructuredSection(
    val components: List<ParsedMealComponent>
)

internal fun parseMealStructuredSections(text: String): List<ParsedMealStructuredSection> {
    val rawLines = normalizeMealParserLines(text.stripMealNutritionBlock())
    if (rawLines.isEmpty()) return emptyList()

    val collector = MealStructuredSectionCollector()
    rawLines.forEach(collector::consumeRawLine)
    return collector.build()
}

internal fun parseMealSectionVisuals(text: String): List<ParsedMealSectionUi> {
    return parseMealStructuredSections(text).map { section ->
        val renderedLines = section.components
            .map(::renderComponentLine)
            .filter { it.isNotBlank() }

        ParsedMealSectionUi(
            lines = renderedLines,
            visualInfo = inferMealVisualInfo(renderedLines.joinToString(separator = " "))
        )
    }
}

private class MealStructuredSectionCollector {
    private val sections = mutableListOf<ParsedMealStructuredSection>()
    private val currentComponents = mutableListOf<MutableParsedMealComponent>()
    private var forceNextAsAlternative = false

    fun consumeRawLine(rawLine: String) {
        val normalizedLine = normalizeMealParserLine(rawLine) ?: return

        when {
            normalizedLine == "+" -> {
                forceNextAsAlternative = false
                return
            }

            isStandaloneMealHeading(normalizedLine) || isNutritionLine(normalizedLine) -> {
                return
            }

            isStandaloneAlternativeSeparatorLine(normalizedLine) -> {
                forceNextAsAlternative = true
                return
            }
        }

        if (mealLineStartsWithBullet(rawLine) && currentComponents.isNotEmpty()) {
            flushSection()
        }

        val additiveSegments = splitTopLevelAdditives(normalizedLine)
        if (additiveSegments.isEmpty()) return

        additiveSegments.forEach(::consumeSegment)
    }

    fun build(): List<ParsedMealStructuredSection> {
        flushSection()
        return sections.toList()
    }

    private fun consumeSegment(segment: String) {
        val parsed = parseComponentSegment(segment) ?: return
        val shouldAttachToPrevious =
            (forceNextAsAlternative || parsed.attachAsAlternativeToPrevious) &&
                    currentComponents.isNotEmpty()

        if (shouldAttachToPrevious) {
            currentComponents.last().merge(parsed.component)
        } else {
            currentComponents += parsed.component
        }

        forceNextAsAlternative = false
    }

    private fun flushSection() {
        val immutableComponents = currentComponents
            .map { component -> component.toImmutable() }
            .filter { component -> component.alternatives.isNotEmpty() }

        if (immutableComponents.isNotEmpty()) {
            sections += ParsedMealStructuredSection(components = immutableComponents)
        }

        currentComponents.clear()
        forceNextAsAlternative = false
    }
}
