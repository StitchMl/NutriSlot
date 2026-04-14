package it.lagioiaproductions.nutrislot.ui.shoppinglist.parsing

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
    val sanitizedSource = rawText
        .stripShoppingNutritionBlock()
        .normalizeBreadQualifierShorthand()
        .normalizeShoppingText()

    val isExtra = sanitizedSource.startsWith("+")
    val cleaned = sanitizedSource.removePrefix("+").trim()

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
    val normalized = text
        .normalizeBreadQualifierShorthand()
        .normalizeShoppingText()
    if (normalized.isBlank()) return emptyList()

    val sections = extractStructuredSections(normalized)
    val candidates = sections.ifEmpty { listOf(normalized) }

    return candidates
        .flatMap(::splitAlternativeChunks)
        .map(::restoreProtectedPhrases)
        .map(String::normalizeShoppingText)
        .filter(String::isNotBlank)
}

private fun extractStructuredSections(text: String): List<String> {
    val rawLines = protectConnectedPhrases(
        text.stripShoppingNutritionBlock()
    )
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .lines()
        .map(String::trim)
        .filter(String::isNotBlank)

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

        val restored = restoreProtectedPhrases(joined)
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
        if (isNutritionLine(line)) return@forEach

        if (line == "+") {
            flushSection()
            return@forEach
        }

        if (isStandaloneShoppingAlternativeSeparator(line)) {
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
    val normalized = text
        .normalizeBreadQualifierShorthand()
        .normalizeShoppingText()
    val protectedText = protectConnectedPhrases(normalized)

    val splitByStrongAlternative = protectedText
        .split(STRONG_ALTERNATIVE_REGEX)
        .map(String::normalizeShoppingText)
        .filter(String::isNotBlank)

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
    splitSlashWithSharedCarbGroup(text.normalizeBreadQualifierShorthand())?.let { grouped ->
        return grouped
    }

    if (!SPACED_SLASH_ALTERNATIVE_REGEX.containsMatchIn(text)) {
        return listOf(text)
    }

    val parts = text
        .split(SPACED_SLASH_ALTERNATIVE_REGEX)
        .map(String::normalizeShoppingText)
        .filter(String::isNotBlank)

    val restoredParts = parts.map(::restoreProtectedPhrases)
    val shouldSplit =
        parts.size > 1 &&
                (
                        restoredParts.count { part -> ALL_QUANTITY_REGEX.containsMatchIn(part) } >= 2 ||
                                restoredParts.all { part ->
                                    part.any(Char::isLetter) &&
                                            (
                                                    part.contains("pane", ignoreCase = true) ||
                                                            part.contains("panino", ignoreCase = true)
                                                    )
                                }
                        )

    return if (shouldSplit) parts else listOf(text)
}

private fun splitSimpleOrAlternatives(text: String): List<String> {
    val normalized = text.normalizeBreadQualifierShorthand()

    val parts = normalized
        .split(Regex("\\s+o\\s+", RegexOption.IGNORE_CASE))
        .map(String::normalizeShoppingText)
        .filter(String::isNotBlank)

    val shouldSplit =
        parts.size > 1 &&
                parts.all { part ->
                    val restored = restoreProtectedPhrases(part).lowercase()
                    ALL_QUANTITY_REGEX.containsMatchIn(restored) ||
                            restored.startsWith("yogurt") ||
                            restored.startsWith("latte") ||
                            restored.startsWith("pane") ||
                            restored.startsWith("panino") ||
                            restored.startsWith("piadina") ||
                            restored.startsWith("frisella")
                }

    return if (shouldSplit) parts else listOf(normalized)
}

private fun parseShoppingOption(rawOption: String): ParsedShoppingOption {
    val normalized = rawOption
        .stripShoppingNutritionBlock()
        .normalizeBreadQualifierShorthand()
        .normalizeShoppingText()

    val rawNotes = PARENTHESIS_REGEX.findAll(normalized)
        .map { match -> match.groupValues[1].normalizeShoppingText() }
        .filter(String::isNotBlank)
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
        .map { match -> match.value.normalizeShoppingText() }
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

    val extraNoteTags = rawNotes.partition { note -> QUANTITY_LIKE_NOTE_REGEX.matches(note) }
    tags += extraNoteTags.first
    tags += extraNoteTags.second

    val fallbackLabel = when {
        isOilOnly -> "Olio EVO"
        else -> normalized
    }

    val label = working
        .replace("/", " / ")
        .replace(Regex("\\be\\s*/\\s*o\\b", RegexOption.IGNORE_CASE), "e/o")
        .replace(Regex("\\s+/\\s+"), " / ")
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

private fun isStandaloneShoppingAlternativeSeparator(line: String): Boolean {
    return line == "/" ||
            line.equals("oppure", ignoreCase = true) ||
            line.equals("alternativa", ignoreCase = true) ||
            line.equals("in alternativa", ignoreCase = true)
}

private fun shouldAppendShoppingLine(
    previous: String,
    current: String
): Boolean {
    val normalizedCurrent = current.lowercase()
    if (
        normalizedCurrent.startsWith("oppure") ||
        normalizedCurrent.startsWith("in alternativa") ||
        normalizedCurrent.startsWith("alternativa") ||
        normalizedCurrent.startsWith("nb") ||
        normalizedCurrent.startsWith("nutrienti") ||
        normalizedCurrent.startsWith("tot ")
    ) {
        return false
    }

    val firstChar = current.firstOrNull() ?: return false

    val currentLooksLikeContinuation =
        firstChar.isLowerCase() ||
                firstChar.isDigit() ||
                firstChar == '(' ||
                firstChar == '%' ||
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

private fun String.stripShoppingNutritionBlock(): String {
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

        val joined = keptLines
            .joinToString(separator = "\n")
            .trim()

        if (joined.isNotBlank()) {
            return joined
        }
    }

    return normalized
        .replace(INLINE_NUTRIENTS_FROM_LABEL_REGEX, "")
        .replace(INLINE_NUTRIENTS_FROM_TOTAL_REGEX, "")
        .trim()
}

private fun isNutritionLine(line: String): Boolean {
    val normalized = line
        .lowercase()
        .replace("’", "'")
        .replace(Regex("\\s+"), " ")
        .trim()

    return normalized.startsWith("nutrienti") ||
            normalized.startsWith("tot kcal") ||
            normalized.startsWith("tot. kcal") ||
            normalized.startsWith("tot g proteine") ||
            normalized.startsWith("tot. g proteine") ||
            normalized.startsWith("tot g carboidrati") ||
            normalized.startsWith("tot. g carboidrati") ||
            normalized.startsWith("tot g fibre") ||
            normalized.startsWith("tot. g fibre") ||
            normalized.startsWith("tot g grassi") ||
            normalized.startsWith("tot. g grassi") ||
            normalized.startsWith("tot g lipidi") ||
            normalized.startsWith("tot. g lipidi")
}

private fun String.normalizeShoppingText(): String {
    return replace(Regex("\\s+"), " ").trim()
}

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

private val SPACED_SLASH_ALTERNATIVE_REGEX = Regex("\\s+/\\s+")

private val INLINE_NUTRIENTS_FROM_LABEL_REGEX = Regex(
    pattern = "(?is)\\bNutrienti\\s*:.*$"
)

private val INLINE_NUTRIENTS_FROM_TOTAL_REGEX = Regex(
    pattern = "(?is)\\bTot\\.?\\s*(?:kcal|g\\s+proteine|g\\s+carboidrati|g\\s+fibre|g\\s+grassi|g\\s+lipidi)\\b.*$"
)

private val PROTECTED_PHRASES = listOf(
    "cotta e/o cruda",
    "cotte e/o crude",
    "cotto e/o crudo",
    "cotti e/o crudi",
    "cruda e/o cotta",
    "crude e/o cotte",
    "caffè latte"
)

private val BREAD_WITH_OR_REGEX = Regex(
    pattern = """((?:\d+(?:[.,]\d+)?\s*(?:kg|g|gr|grammi?|mg|l|ml|cl|pz)\s+di\s+)?(?:pane|panino)\s+)(scuro|integrale)\s+o\s+(?:(?:pane|panino)\s+)?(scuro|integrale)(.*)""",
    options = setOf(RegexOption.IGNORE_CASE)
)

private val BREAD_WITH_SLASH_REGEX = Regex(
    pattern = """((?:\d+(?:[.,]\d+)?\s*(?:kg|g|gr|grammi?|mg|l|ml|cl|pz)\s+di\s+)?(?:pane|panino)\s+)(scuro|integrale)\s*/\s*(?:(?:pane|panino)\s+)?(scuro|integrale)(.*)""",
    options = setOf(RegexOption.IGNORE_CASE)
)

private val IMPLICIT_BREAD_SHORTHAND_REGEX = Regex(
    pattern = """((?:\d+(?:[.,]\d+)?\s*(?:kg|g|gr|grammi?|mg|l|ml|cl|pz)\s+di\s+)?(?:pane|panino)\s+)scuro\s+integrale(.*)""",
    options = setOf(RegexOption.IGNORE_CASE)
)

private fun String.normalizeBreadQualifierShorthand(): String {
    val slashNormalized = BREAD_WITH_SLASH_REGEX.replace(this) { match ->
        val prefix = match.groupValues[1]
        val first = match.groupValues[2]
        val second = match.groupValues[3]
        val suffix = match.groupValues[4]
        "$prefix$first/$second$suffix"
    }

    val orNormalized = BREAD_WITH_OR_REGEX.replace(slashNormalized) { match ->
        val prefix = match.groupValues[1]
        val first = match.groupValues[2]
        val second = match.groupValues[3]
        val suffix = match.groupValues[4]
        "$prefix$first/$second$suffix"
    }

    return IMPLICIT_BREAD_SHORTHAND_REGEX.replace(orNormalized) { match ->
        val prefix = match.groupValues[1]
        val suffix = match.groupValues[2]
        prefix + "scuro/integrale" + suffix
    }
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun splitSlashWithSharedCarbGroup(text: String): List<String>? {
    val normalized = restoreProtectedPhrases(text).normalizeShoppingText()
    val parts = normalized
        .split(SPACED_SLASH_ALTERNATIVE_REGEX)
        .map(String::normalizeShoppingText)
        .filter(String::isNotBlank)

    if (parts.size < 2) return null

    val result = mutableListOf<String>()
    val current = mutableListOf<String>()

    parts.forEach { part ->
        if (current.isEmpty()) {
            current += part
        } else {
            val currentJoined = current.joinToString(" / ")
            val currentHasQty = ALL_QUANTITY_REGEX.containsMatchIn(currentJoined)

            val nextStartsNewAlt =
                currentHasQty && (
                        part.contains("pane", ignoreCase = true) ||
                                part.contains("panino", ignoreCase = true) ||
                                part.equals("scuro", ignoreCase = true) ||
                                part.equals("integrale", ignoreCase = true)
                        )

            if (nextStartsNewAlt) {
                result += currentJoined.normalizeShoppingText()
                current.clear()
                current += part
            } else {
                current += part
            }
        }
    }

    if (current.isNotEmpty()) {
        result += current.joinToString(" / ").normalizeShoppingText()
    }

    return result.takeIf { it.size > 1 }
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