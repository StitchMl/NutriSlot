package it.lagioiaproductions.nutrislot.ui.weeklyplan

import it.lagioiaproductions.nutrislot.ui.shared.normalizeMealUiLine

private const val MOJIBAKE_BULLET = "\u00E2\u20AC\u00A2"
private const val MOJIBAKE_EN_DASH = "\u00E2\u20AC\u201C"
private const val MOJIBAKE_EM_DASH = "\u00E2\u20AC\u201D"
private const val MOJIBAKE_APOSTROPHE = "\u00E2\u20AC\u2122"

private val MEAL_PARSER_BULLET_TOKENS = listOf(
    "\u2022",
    "-",
    "\u2013",
    "\u2014",
    MOJIBAKE_BULLET,
    MOJIBAKE_EN_DASH,
    MOJIBAKE_EM_DASH
)

private val MEAL_PARSER_LINE_BREAK_TOKENS = listOf(
    "\u2022",
    MOJIBAKE_BULLET
)

private val MEAL_PARSER_BULLET_PREFIX_REGEX = Regex(
    pattern = "^(?:${MEAL_PARSER_BULLET_TOKENS.joinToString(separator = "|", transform = Regex::escape)})\\s*"
)

private val NUTRITION_LINE_PREFIXES = listOf(
    "nutrienti",
    "tot kcal",
    "tot. kcal",
    "tot g proteine",
    "tot. g proteine",
    "tot g carboidrati",
    "tot. g carboidrati",
    "tot g fibre",
    "tot. g fibre",
    "tot g grassi",
    "tot. g grassi",
    "tot g lipidi",
    "tot. g lipidi"
)

private val STANDALONE_MEAL_HEADINGS = setOf(
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
    "cena"
)

private val ALTERNATIVE_PREFIX_REGEX = Regex(
    pattern = "^(?:oppure|in alternativa|alternativa)\\s*:?\\s+",
    option = RegexOption.IGNORE_CASE
)

private val ALTERNATIVE_MARKER_REGEX = Regex(
    pattern = "\\b(oppure|in alternativa|alternativa)\\s*:\\s*",
    option = RegexOption.IGNORE_CASE
)

internal fun normalizeMealParserLines(text: String): List<String> {
    return MEAL_PARSER_LINE_BREAK_TOKENS
        .fold(
            text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
        ) { acc, token ->
            acc.replace(token, "\n$token ")
        }
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

internal fun mealLineStartsWithBullet(rawLine: String): Boolean {
    return MEAL_PARSER_BULLET_PREFIX_REGEX.containsMatchIn(rawLine)
}

internal fun normalizeMealParserLine(rawLine: String): String? {
    val strippedLine = MEAL_PARSER_BULLET_TOKENS
        .fold(rawLine.trim()) { acc, token -> acc.removePrefix(token) }
        .trim()
        .normalizeMealUiLine()

    return strippedLine.takeIf { it.isNotBlank() }
}

internal fun String.normalizeMealParserMatchable(): String {
    return lowercase()
        .replace(MOJIBAKE_APOSTROPHE, "'")
        .replace("\u2019", "'")
        .normalizeMealUiLine()
}

internal fun String.stripMealNutritionBlock(): String {
    val normalized = this
        .replace("\r\n", "\n")
        .replace("\r", "\n")

    val lines = normalized.lines()
    if (lines.size > 1) {
        val keptLines = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) {
                keptLines += line
                continue
            }

            if (isNutritionLine(trimmed)) {
                break
            }

            keptLines += line
        }

        val joined = keptLines.joinToString(separator = "\n").trim()
        if (joined.isNotBlank()) {
            return joined
        }
    }

    return normalized
        .replace(Regex("(?is)\\bNutrienti\\s*:.*$"), "")
        .replace(
            Regex(
                "(?is)\\bTot\\.?\\s*(?:kcal|g\\s+proteine|g\\s+carboidrati|g\\s+fibre|g\\s+grassi|g\\s+lipidi)\\b.*$"
            ),
            ""
        )
        .trim()
}

internal fun isNutritionLine(line: String): Boolean {
    val normalized = line
        .normalizeMealParserMatchable()
        .replace(Regex("\\s+"), " ")
        .trim()

    return NUTRITION_LINE_PREFIXES.any(normalized::startsWith)
}

internal fun isStandaloneMealHeading(line: String): Boolean {
    return line.lowercase() in STANDALONE_MEAL_HEADINGS
}

internal fun isStandaloneAlternativeSeparatorLine(line: String): Boolean {
    return line == "/" ||
            line.equals("oppure", ignoreCase = true) ||
            line.equals("alternativa", ignoreCase = true) ||
            line.equals("in alternativa", ignoreCase = true)
}

internal fun startsWithAlternativePrefix(text: String): Boolean {
    return ALTERNATIVE_PREFIX_REGEX.containsMatchIn(text)
}

internal fun removeAlternativePrefix(text: String): String {
    return ALTERNATIVE_PREFIX_REGEX.replace(text, "").normalizeMealUiLine()
}

internal fun String.normalizeAlternativeMarkers(): String {
    return replace(ALTERNATIVE_MARKER_REGEX, "$1 ")
}

internal fun restoreCollapsedWhitespace(text: String): String {
    return text.replace(Regex("\\s+"), " ").trim()
}
