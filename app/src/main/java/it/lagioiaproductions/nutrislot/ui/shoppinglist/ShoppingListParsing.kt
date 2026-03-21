package it.lagioiaproductions.nutrislot.ui.shoppinglist

import it.lagioiaproductions.nutrislot.ui.shared.inferMealVisualInfo
import it.lagioiaproductions.nutrislot.ui.shared.normalizeMealUiLine
import it.lagioiaproductions.nutrislot.ui.shared.protectConnectedMealPhrases
import it.lagioiaproductions.nutrislot.ui.shared.restoreConnectedMealPhrases
import it.lagioiaproductions.nutrislot.ui.shared.shouldAppendMealContinuation

internal data class ParsedShoppingEntry(
    val isExtra: Boolean,
    val leadingEmoji: String,
    val options: List<ParsedShoppingOption>
)

internal data class ParsedShoppingOption(
    val label: String,
    val detailTags: List<String>,
    val emoji: String
)

internal fun parseShoppingEntry(rawText: String): ParsedShoppingEntry {
    val normalized = rawText.normalizeShoppingText()
    val isExtra = normalized.startsWith("+")
    val cleaned = normalized.removePrefix("+").trim()

    val chunks = explodeMealChunks(cleaned)
    val parsedOptions = chunks.map(::parseShoppingOption)
    val leadingEmoji = parsedOptions.firstOrNull()?.emoji ?: "🛒"

    return ParsedShoppingEntry(
        isExtra = isExtra,
        leadingEmoji = leadingEmoji,
        options = parsedOptions
    )
}

private fun explodeMealChunks(text: String): List<String> {
    val normalized = text.normalizeShoppingText()
    if (normalized.isBlank()) return emptyList()

    val sections = extractStructuredSections(normalized)
    val candidates = sections.ifEmpty { listOf(normalized) }

    return candidates
        .flatMap(::splitAlternativeChunks)
        .map(::restoreConnectedMealPhrases)
        .map { it.normalizeShoppingText() }
        .filter { it.isNotBlank() }
}

private fun extractStructuredSections(text: String): List<String> {
    val rawLines = protectConnectedMealPhrases(text)
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }

    if (rawLines.size <= 1 && "+" !in text) {
        return emptyList()
    }

    val sections = mutableListOf<String>()
    var currentSection = mutableListOf<String>()

    fun flushSection() {
        val joined = currentSection
            .joinToString(separator = " ")
            .normalizeMealUiLine()

        val restored = restoreConnectedMealPhrases(joined)
        if (restored.isNotBlank()) {
            sections += restored
        }
        currentSection = mutableListOf()
    }

    rawLines.forEach { rawLine ->
        val line = rawLine
            .removeLeadingMealSlotHeading()
            .normalizeShoppingText()

        if (line.isBlank()) return@forEach

        if (line == "+") {
            flushSection()
            return@forEach
        }

        val previous = currentSection.lastOrNull()
        if (previous != null && shouldAppendMealContinuation(previous, line)) {
            currentSection[currentSection.lastIndex] = "$previous $line"
                .normalizeMealUiLine()
        } else {
            currentSection += line
        }
    }

    flushSection()
    return sections
}

private fun splitAlternativeChunks(text: String): List<String> {
    val normalized = text.normalizeShoppingText()
    val protectedText = protectConnectedMealPhrases(normalized)

    val splitByStrongAlternative = protectedText
        .split(STRONG_ALTERNATIVE_REGEX)
        .map { it.normalizeShoppingText() }
        .filter { it.isNotBlank() }

    if (splitByStrongAlternative.size > 1) {
        return splitByStrongAlternative.flatMap(::splitSlashAlternatives)
    }

    val splitBySlash = splitSlashAlternatives(protectedText)
    if (splitBySlash.size > 1) {
        return splitBySlash
    }

    val splitBySimpleOr = splitSimpleOrAlternatives(protectedText)
    if (splitBySimpleOr.size > 1) {
        return splitBySimpleOr
    }

    return listOf(protectedText)
}

private fun splitSlashAlternatives(text: String): List<String> {
    if ('/' !in text) return listOf(text)

    val parts = text
        .split('/')
        .map { it.normalizeShoppingText() }
        .filter { it.isNotBlank() }

    val shouldSplit =
        parts.size > 1 &&
                parts.count { ALL_QUANTITY_REGEX.containsMatchIn(restoreConnectedMealPhrases(it)) } >= 2 &&
                parts.all { part -> restoreConnectedMealPhrases(part).any(Char::isLetter) }

    return if (shouldSplit) parts else listOf(text)
}

private fun splitSimpleOrAlternatives(text: String): List<String> {
    val parts = text
        .split(Regex("\\s+o\\s+", RegexOption.IGNORE_CASE))
        .map { it.normalizeShoppingText() }
        .filter { it.isNotBlank() }

    val shouldSplit =
        parts.size > 1 &&
                parts.all { part ->
                    val restored = restoreConnectedMealPhrases(part).lowercase()
                    ALL_QUANTITY_REGEX.containsMatchIn(restored) ||
                            restored.startsWith("yogurt") ||
                            restored.startsWith("latte") ||
                            restored.startsWith("pane") ||
                            restored.startsWith("panino") ||
                            restored.startsWith("piadina") ||
                            restored.startsWith("frisella")
                }

    return if (shouldSplit) parts else listOf(text)
}

private fun parseShoppingOption(rawOption: String): ParsedShoppingOption {
    val normalized = rawOption.normalizeShoppingText()

    val rawNotes = PARENTHESIS_REGEX.findAll(normalized)
        .map { it.groupValues[1].normalizeShoppingText() }
        .filter { it.isNotBlank() }
        .toMutableList()

    val isOilOnly = INLINE_OIL_TAG_REGEX.containsMatchIn(normalized) &&
            normalized.lowercase().replace(" ", "").contains("olio")

    var working = normalized
        .replace(PARENTHESIS_REGEX, "")
        .normalizeShoppingText()

    val tags = mutableListOf<String>()

    LEADING_QUANTITY_REGEX.find(working)?.let { match ->
        tags += match.value.normalizeShoppingText()
        working = working.removePrefix(match.value).normalizeShoppingText()
    }

    val inlineOilTags = INLINE_OIL_TAG_REGEX.findAll(working)
        .map { it.value.normalizeShoppingText() }
        .toList()

    tags += inlineOilTags

    working = INLINE_OIL_TAG_REGEX
        .replace(working, " ")
        .normalizeShoppingText()

    TRAILING_QUANTITY_REGEX.find(working)?.let { match ->
        val value = match.value.normalizeShoppingText()
        if (value.isNotBlank()) {
            tags += value
            working = working.removeSuffix(match.value).normalizeShoppingText()
        }
    }

    val extraNoteTags = rawNotes.partition { QUANTITY_LIKE_NOTE_REGEX.matches(it) }
    tags += extraNoteTags.first
    tags += extraNoteTags.second

    val fallbackLabel = when {
        isOilOnly -> "Olio EVO"
        else -> normalized
    }

    val label = working
        .replace("/", " / ")
        .replace(Regex("\\s+/\\s+"), " / ")
        .replace(Regex("\\be\\s*/\\s*o\\b", RegexOption.IGNORE_CASE), "e/o")
        .replace(Regex("\\s+"), " ")
        .trim()
        .trim(',', ';', '-', ' ')
        .replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }
        .ifBlank { fallbackLabel }

    val emoji = when {
        isOilOnly -> "🫒"
        else -> inferMealVisualInfo(label).emoji
    }

    return ParsedShoppingOption(
        label = label,
        detailTags = tags.distinct(),
        emoji = emoji
    )
}

private fun String.removeLeadingMealSlotHeading(): String {
    val trimmed = trim()
    return when (trimmed.lowercase()) {
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
        "cena" -> ""

        else -> trimmed
    }
}

private fun String.normalizeShoppingText(): String {
    return replace(Regex("\\s+"), " ").trim()
}

private val PARENTHESIS_REGEX = Regex("\\(([^)]+)\\)")

private val ALL_QUANTITY_REGEX = Regex(
    pattern = "(N\\.?\\s*\\d+|\\d+[.,]?\\d*\\s*(kg|g|mg|l|ml|cl|pz))\\b",
    option = RegexOption.IGNORE_CASE
)

private val INLINE_OIL_TAG_REGEX = Regex(
    pattern = "\\b(?:N\\.?\\s*\\d+\\s*(?:cucchiai?|cucchiaio|cucchiaini?|cucchiaino)|\\d+[.,]?\\d*\\s*(?:kg|g|mg|l|ml|cl))\\s+di\\s+olio(?:\\s+e\\s*v\\s*o|\\s+evo|\\s+extravergine(?:\\s+d['’]oliva)?)?\\b",
    option = RegexOption.IGNORE_CASE
)

private val LEADING_QUANTITY_REGEX = Regex(
    pattern = "^(N\\.?\\s*\\d+|\\d+[.,]?\\d*\\s*(kg|g|mg|l|ml|cl|pz))\\b\\s*",
    option = RegexOption.IGNORE_CASE
)

private val TRAILING_QUANTITY_REGEX = Regex(
    pattern = "\\b(N\\.?\\s*\\d+|\\d+[.,]?\\d*\\s*(kg|g|mg|l|ml|cl|pz))$",
    option = RegexOption.IGNORE_CASE
)

private val QUANTITY_LIKE_NOTE_REGEX = Regex(
    pattern = "^(\\d+[.,]?\\d*\\s*(kg|g|mg|l|ml|cl|pz)|N\\.?\\s*\\d+|circa\\s+N\\.?\\s*\\d+.*)$",
    option = RegexOption.IGNORE_CASE
)

private val STRONG_ALTERNATIVE_REGEX = Regex(
    pattern = "\\s+(?:oppure|in alternativa|alternativa)\\s*:?\\s+",
    option = RegexOption.IGNORE_CASE
)