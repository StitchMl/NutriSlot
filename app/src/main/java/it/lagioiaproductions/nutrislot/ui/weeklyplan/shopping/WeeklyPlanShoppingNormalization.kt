package it.lagioiaproductions.nutrislot.ui.weeklyplan.shopping

import it.lagioiaproductions.nutrislot.ui.weeklyplan.parser.isNutritionLine
import it.lagioiaproductions.nutrislot.ui.weeklyplan.parser.isStandaloneMealHeading
import it.lagioiaproductions.nutrislot.ui.weeklyplan.parser.stripMealNutritionBlock

private val BREAD_ONLY_REGEX = Regex(
    pattern = """^(?:\d+(?:[.,]\d+)?\s*(?:kg|g|gr|grammi?|ml|l)\s+di\s+)?pane\s+(scuro|integrale)\b.*$""",
    options = setOf(RegexOption.IGNORE_CASE)
)

internal fun collapseBreadVariantsForShopping(alternatives: List<String>): List<String> {
    if (alternatives.size < 2) return alternatives

    val breadAlternatives = alternatives.filter { BREAD_ONLY_REGEX.matches(it) }
    val nonBreadAlternatives = alternatives.filterNot { BREAD_ONLY_REGEX.matches(it) }

    if (breadAlternatives.size < 2) return alternatives

    val qualifiers = breadAlternatives.mapNotNull { alternative ->
        BREAD_ONLY_REGEX.matchEntire(alternative)?.groupValues?.getOrNull(1)?.lowercase()
    }.distinct()

    if (!qualifiers.containsAll(listOf("scuro", "integrale"))) {
        return alternatives
    }

    val normalizedBread = breadAlternatives.first()
        .replace(Regex("""\bscuro\b""", RegexOption.IGNORE_CASE), "scuro/integrale")
        .replace(Regex("""\bintegrale\b""", RegexOption.IGNORE_CASE), "scuro/integrale")
        .replace(
            Regex("""\bscuro/integrale\b\s*/\s*\bscuro/integrale\b""", RegexOption.IGNORE_CASE),
            "scuro/integrale"
        )
        .replace(Regex("""\s+"""), " ")
        .trim()

    return nonBreadAlternatives + normalizedBread
}

internal fun normalizeShoppingItems(items: List<String>): List<String> {
    return items
        .mapNotNull { line ->
            val cleaned = line
                .stripMealNutritionBlock()
                .replace(Regex("^[-+\\u2022\\s]+"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
                .removeSuffix(".")
                .trim()

            cleaned.takeIf {
                it.isNotBlank() &&
                        !isNutritionLine(it) &&
                        !isStandaloneMealHeading(it)
            }
        }
        .distinct()
}

internal fun normalizeShoppingText(text: String): String {
    return text
        .replace(Regex("\\s+"), " ")
        .trim()
}
