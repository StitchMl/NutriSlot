package it.lagioiaproductions.nutrislot.ui.shoppinglist

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

    val chunks = splitAlternativeChunks(cleaned)
    val parsedOptions = chunks.map(::parseShoppingOption)
    val leadingEmoji = parsedOptions.firstOrNull()?.emoji ?: "🛒"

    return ParsedShoppingEntry(
        isExtra = isExtra,
        leadingEmoji = leadingEmoji,
        options = parsedOptions
    )
}

private fun splitAlternativeChunks(text: String): List<String> {
    val normalized = text.normalizeShoppingText()

    val splitByOppure = normalized
        .split(Regex("\\s+oppure\\s+", RegexOption.IGNORE_CASE))
        .map { it.normalizeShoppingText() }
        .filter { it.isNotBlank() }

    if (splitByOppure.size > 1) {
        return splitByOppure
    }

    val quantityCount = ALL_QUANTITY_REGEX.findAll(normalized).count()

    if (quantityCount >= 2) {
        val splitBySimpleOr = normalized
            .split(Regex("\\s+o\\s+", RegexOption.IGNORE_CASE))
            .map { it.normalizeShoppingText() }
            .filter { it.isNotBlank() }

        if (splitBySimpleOr.size > 1) {
            return splitBySimpleOr
        }
    }

    return listOf(normalized)
}

private fun parseShoppingOption(rawOption: String): ParsedShoppingOption {
    val normalized = rawOption.normalizeShoppingText()

    val rawNotes = PARENTHESIS_REGEX.findAll(normalized)
        .map { it.groupValues[1].normalizeShoppingText() }
        .filter { it.isNotBlank() }
        .toMutableList()

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
        .ifBlank { normalized }

    return ParsedShoppingOption(
        label = label,
        detailTags = tags.distinct(),
        emoji = emojiForProduct(label)
    )
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

private fun emojiForProduct(label: String): String {
    val text = label.lowercase()

    return when {
        "pesce" in text || "salmone" in text || "tonno" in text -> "🐟"
        "latte" in text || "yogurt" in text -> "🥛"
        "lattuga" in text || "insalata" in text || "verdura" in text -> "🥬"
        "pomodoro" in text -> "🍅"
        "carota" in text -> "🥕"
        "pasta" in text || "spaghetti" in text || "riso" in text || "couscous" in text || "orzo" in text || "farro" in text -> "🍝"
        "cereali" in text || "cornflakes" in text || "muesli" in text || "granola" in text -> "🥣"
        "mela" in text -> "🍎"
        "pera" in text -> "🍐"
        "banana" in text -> "🍌"
        "pane" in text -> "🍞"
        "uova" in text || "uovo" in text -> "🥚"
        "mandorle" in text || "nocciole" in text || "noci" in text || "frutta secca" in text -> "🥜"
        "avocado" in text -> "🥑"
        "caffè" in text || "caffe" in text -> "☕"
        "formaggio" in text || "parmigiano" in text || "primo sale" in text || "ricotta" in text || "mozzarella" in text -> "🧀"
        "pollo" in text || "carne" in text -> "🍗"
        "frutto" in text || "frutta" in text -> "🍓"
        "miele" in text -> "🍯"
        "acqua" in text -> "💧"
        "detersivo" in text -> "🧴"
        else -> "🛒"
    }
}