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
        .map(::restoreProtectedPhrases)
        .map { it.normalizeShoppingText() }
        .filter { it.isNotBlank() }
}

private fun extractStructuredSections(text: String): List<String> {
    val rawLines = text
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
            .replace(Regex("\\s+"), " ")
            .trim()

        if (joined.isNotBlank()) {
            sections += joined
        }
        currentSection = mutableListOf()
    }

    rawLines.forEach { rawLine ->
        val line = rawLine.removeLeadingMealSlotHeading().trim()
        if (line.isBlank()) return@forEach

        if (line == "+") {
            flushSection()
            return@forEach
        }

        val previous = currentSection.lastOrNull()
        if (previous != null && shouldAppendShoppingLine(previous, line)) {
            currentSection[currentSection.lastIndex] = "$previous $line"
                .replace(Regex("\\s+"), " ")
                .trim()
        } else {
            currentSection += line
        }
    }

    flushSection()
    return sections
}

private fun splitAlternativeChunks(text: String): List<String> {
    val normalized = text.normalizeShoppingText()
    val protectedText = protectConnectedPhrases(normalized)

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
                parts.count { ALL_QUANTITY_REGEX.containsMatchIn(restoreProtectedPhrases(it)) } >= 2 &&
                parts.all { part -> restoreProtectedPhrases(part).any(Char::isLetter) }

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
                    val restored = restoreProtectedPhrases(part)
                    ALL_QUANTITY_REGEX.containsMatchIn(restored) ||
                            restored.lowercase().startsWith("yogurt") ||
                            restored.lowercase().startsWith("latte")
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
        .replace(Regex("\\s+"), " ")
        .trim()
        .trim(',', ';', '-', ' ')
        .replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }
        .ifBlank { fallbackLabel }

    return ParsedShoppingOption(
        label = label,
        detailTags = tags.distinct(),
        emoji = emojiForProduct(label)
    )
}

private fun shouldAppendShoppingLine(
    previous: String,
    current: String
): Boolean {
    val normalizedCurrent = current.lowercase()
    if (
        normalizedCurrent.startsWith("oppure") ||
        normalizedCurrent.startsWith("in alternativa") ||
        normalizedCurrent.startsWith("nb")
    ) {
        return false
    }

    val firstChar = current.firstOrNull() ?: return false

    val currentLooksLikeContinuation =
        firstChar.isLowerCase() ||
                firstChar.isDigit() ||
                firstChar == '(' ||
                current.length <= 18

    val previousLooksOpen =
        previous.endsWith(",") ||
                previous.endsWith(":") ||
                previous.endsWith("/") ||
                previous.endsWith("-") ||
                previous.endsWith(" o") ||
                previous.endsWith(" oppure")

    return currentLooksLikeContinuation || previousLooksOpen
}

private fun String.removeLeadingMealSlotHeading(): String {
    val trimmed = trim()
    return when (trimmed.lowercase()) {
        "colazione",
        "spuntino mattina",
        "pranzo",
        "spuntino pomeridiano",
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

private val PROTECTED_PHRASES = listOf(
    "scuro o integrale",
    "integrale o scuro",
    "pane scuro o integrale",
    "pane integrale o scuro",
    "cotta e/o cruda",
    "cotte e/o crude",
    "cotto e/o crudo",
    "cotti e/o crudi",
    "cruda e/o cotta",
    "crude e/o cotte",
    "caffè latte"
)

private fun protectConnectedPhrases(text: String): String {
    var result = text
    PROTECTED_PHRASES.forEachIndexed { index, phrase ->
        val placeholder = "__KEEP_${index}__"
        result = Regex(Regex.escape(phrase), RegexOption.IGNORE_CASE)
            .replace(result) { placeholder }
    }
    return result
}

private fun restoreProtectedPhrases(text: String): String {
    var result = text
    PROTECTED_PHRASES.forEachIndexed { index, phrase ->
        result = result.replace("__KEEP_${index}__", phrase)
    }
    return result
}

private fun emojiForProduct(label: String): String {
    val text = label.lowercase()

    return when {
        "panino" in text || "panini" in text -> "🥪"
        "piadina" in text -> "🌯"
        "frisella" in text || "friselle" in text -> "🥯"
        "toast" in text -> "🧇"
        "pancake" in text -> "🥞"
        "insalatona" in text || "insalata" in text -> "🥗"
        "pesce" in text || "salmone" in text || "tonno" in text || "sgombro" in text -> "🐟"
        "latte" in text || "yogurt" in text || "kefir" in text -> "🥛"
        "lattuga" in text || "verdura" in text || "rughetta" in text || "zucchine" in text || "songino" in text -> "🥬"
        "pomodoro" in text || "pomodorini" in text -> "🍅"
        "carota" in text || "carote" in text -> "🥕"
        "pasta" in text || "spaghetti" in text || "riso" in text || "couscous" in text || "orzo" in text || "farro" in text -> "🍝"
        "cereali" in text || "cornflakes" in text || "muesli" in text || "granola" in text -> "🥣"
        "mela" in text -> "🍎"
        "pera" in text -> "🍐"
        "banana" in text -> "🍌"
        "pane" in text -> "🍞"
        "uova" in text || "uovo" in text || "frittata" in text -> "🥚"
        "mandorle" in text || "nocciole" in text || "noci" in text || "frutta secca" in text || "arachidi" in text -> "🥜"
        "avocado" in text -> "🥑"
        "caffè" in text || "caffe" in text -> "☕"
        "formaggio" in text || "parmigiano" in text || "primo sale" in text || "ricotta" in text || "mozzarella" in text || "feta" in text || "philadelphia" in text -> "🧀"
        "pollo" in text || "tacchino" in text || "hamburger" in text || "carne" in text || "affettato" in text || "bresaola" in text -> "🍗"
        "frutto" in text || "frutta" in text || "fragole" in text || "kiwi" in text || "arancia" in text -> "🍓"
        "miele" in text || "marmellata" in text -> "🍯"
        "cioccolato" in text -> "🍫"
        "olio" in text || "olive" in text -> "🫒"
        "acqua" in text -> "💧"
        "detersivo" in text -> "🧴"
        else -> "🛒"
    }
}