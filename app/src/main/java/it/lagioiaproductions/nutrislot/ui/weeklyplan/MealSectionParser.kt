@file:Suppress("CanBeVal")

package it.lagioiaproductions.nutrislot.ui.weeklyplan

import it.lagioiaproductions.nutrislot.ui.shared.normalizeMealUiLine

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
    val cleanedSource = text.stripMealNutritionBlock()

    val rawLines = cleanedSource
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .replace("•", "\n• ")
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }

    if (rawLines.isEmpty()) return emptyList()

    val sections = mutableListOf<ParsedMealStructuredSection>()
    val currentComponents = mutableListOf<MutableParsedMealComponent>()
    var forceNextAsAlternative = false

    fun flushSection() {
        val immutableComponents = currentComponents
            .map { component -> component.toImmutable() }
            .filter { component -> component.alternatives.isNotEmpty() }

        if (immutableComponents.isNotEmpty()) {
            sections += ParsedMealStructuredSection(
                components = immutableComponents
            )
        }
        currentComponents.clear()
        forceNextAsAlternative = false
    }

    rawLines.forEach { rawLine ->
        val startsWithBullet = BULLET_PREFIX_REGEX.containsMatchIn(rawLine)

        var normalizedLine = rawLine
            .trim()
            .removePrefix("•")
            .removePrefix("-")
            .removePrefix("–")
            .removePrefix("—")
            .trim()
            .normalizeMealUiLine()

        if (normalizedLine.isBlank()) return@forEach
        if (normalizedLine == "+") {
            forceNextAsAlternative = false
            return@forEach
        }
        if (isStandaloneMealHeading(normalizedLine)) return@forEach
        if (isNutritionLine(normalizedLine)) return@forEach

        if (isStandaloneAlternativeSeparatorLine(normalizedLine)) {
            forceNextAsAlternative = true
            return@forEach
        }

        if (startsWithBullet && currentComponents.isNotEmpty()) {
            flushSection()
        }

        val additiveSegments = splitTopLevelAdditives(normalizedLine)
        if (additiveSegments.isEmpty()) return@forEach

        additiveSegments.forEach { segment ->
            val parsed = parseComponentSegment(segment) ?: return@forEach

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
    }

    flushSection()
    return sections
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

internal fun mealSemanticLabel(semanticKey: String): String {
    return when (semanticKey) {
        "panino" -> "Panino"
        "piadina" -> "Piadina"
        "frisella" -> "Frisella"
        "insalata" -> "Insalata"
        "cereale_primo" -> "Primo"
        "pancake" -> "Pancake"
        "latticino" -> "Latticino"
        "carne" -> "Carne"
        "pesce" -> "Pesce"
        "uova" -> "Uova"
        "pane" -> "Pane"
        "colazione_secca" -> "Colazione"
        "banana" -> "Banana"
        "mela" -> "Mela"
        "pera" -> "Pera"
        "cioccolato" -> "Cioccolato"
        "frutta" -> "Frutta"
        "frutta_secca" -> "Frutta secca"
        "avocado" -> "Avocado"
        "formaggio" -> "Formaggio"
        "pomodoro" -> "Pomodoro"
        "carota" -> "Carota"
        "verdura" -> "Verdura"
        "olio" -> "Olio"
        "dolce_spalmabile" -> "Dolce"
        "caffe" -> "Caffè"
        else -> "Pasto"
    }
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

private fun renderComponentLine(component: ParsedMealComponent): String {
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

private fun parseComponentSegment(
    rawSegment: String
): ParsedComponentParseResult? {
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

    val matchable = normalized
        .lowercase()
        .replace("’", "'")
        .normalizeMealUiLine()

    when {
        EXAMPLE_PREFIX_REGEX.containsMatchIn(matchable) -> {
            val examplePayload = EXAMPLE_PREFIX_REGEX.replace(matchable, "").normalizeMealUiLine()
            component.exampleNotes += examplePayload.ifBlank { normalized }
        }

        (matchable.startsWith("n.") || matchable.startsWith("n ") || matchable.startsWith("max ")) &&
                matchable.contains("a settimana") -> {
            component.weeklyQuantityNotes += normalized
        }

        (matchable.startsWith("n.") || matchable.startsWith("n ") || matchable.startsWith("max ")) &&
                matchable.contains("nel pasto") -> {
            component.mealQuantityNotes += normalized
        }

        else -> {
            component.genericNotes += normalized
        }
    }
}

private fun extractParentheticalNotes(
    text: String
): ParentheticalExtraction {
    val notes = mutableListOf<String>()
    val baseBuilder = StringBuilder()
    var depth = 0
    val noteBuilder = StringBuilder()

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

private fun splitTopLevelAdditives(text: String): List<String> {
    return splitTopLevel(
        text
            .normalizeAlternativeMarkers()
            .normalizeBreadQualifierShorthand()
    ) { source, index ->
        additiveSeparatorLengthAt(source, index)
    }
}

private fun splitTopLevelAlternatives(text: String): List<String> {
    val normalized = text
        .normalizeAlternativeMarkers()
        .normalizeBreadQualifierShorthand()

    val splitByWords = splitTopLevel(normalized) { source, index ->
        alternativeWordSeparatorLengthAt(source, index)
    }

    return splitByWords
        .flatMap { part ->
            splitBreadAwareAlternatives(part)
        }
        .map(::normalizeInlineAlternativeText)
        .map(::restoreCollapsedWhitespace)
        .filter { it.isNotBlank() }
}

private fun splitBreadAwareAlternatives(text: String): List<String> {
    val normalized = text.normalizeBreadQualifierShorthand()

    splitSlashAlternativesKeepingCarbGroup(normalized)?.let { grouped ->
        return grouped
            .map(String::normalizeMealUiLine)
            .filter(String::isNotBlank)
    }

    return splitHeuristicSlashAlternatives(normalized)
        .map(String::normalizeMealUiLine)
        .filter(String::isNotBlank)
}

private const val BREAD_QUALIFIER_SLASH_PLACEHOLDER = "__BREAD_QUALIFIER_SLASH__"

private val BREAD_WITH_OR_REGEX = Regex(
    pattern = """((?:\d+(?:[.,]\d+)?\s*(?:kg|g|gr|grammi?|mg|ml|cl|l)\s+di\s+)?(?:pane|panino)\s+)(scuro|integrale)\s+o\s+(?:(?:pane|panino)\s+)?(scuro|integrale)(.*)""",
    options = setOf(RegexOption.IGNORE_CASE)
)

private val BREAD_WITH_SLASH_REGEX = Regex(
    pattern = """((?:\d+(?:[.,]\d+)?\s*(?:kg|g|gr|grammi?|mg|ml|cl|l)\s+di\s+)?(?:pane|panino)\s+)(scuro|integrale)\s*/\s*(?:(?:pane|panino)\s+)?(scuro|integrale)(.*)""",
    options = setOf(RegexOption.IGNORE_CASE)
)

private val IMPLICIT_BREAD_SHORTHAND_REGEX = Regex(
    pattern = """((?:\d+(?:[.,]\d+)?\s*(?:kg|g|gr|grammi?|mg|ml|cl|l)\s+di\s+)?(?:pane|panino)\s+)scuro\s+integrale(.*)""",
    options = setOf(RegexOption.IGNORE_CASE)
)

private val CANONICAL_BREAD_QUALIFIER_REGEX = Regex(
    pattern = """((?:\d+(?:[.,]\d+)?\s*(?:kg|g|gr|grammi?|mg|ml|cl|l)\s+di\s+)?(?:pane|panino)\s+)(scuro|integrale)/(scuro|integrale)(.*)""",
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

private fun protectBreadQualifierSlash(text: String): String {
    return CANONICAL_BREAD_QUALIFIER_REGEX.replace(text) { match ->
        val prefix = match.groupValues[1]
        val first = match.groupValues[2]
        val second = match.groupValues[3]
        val suffix = match.groupValues[4]
        "$prefix$first$BREAD_QUALIFIER_SLASH_PLACEHOLDER$second$suffix"
    }
}

private fun restoreProtectedBreadQualifierSlash(text: String): String {
    return text.replace(BREAD_QUALIFIER_SLASH_PLACEHOLDER, "/")
}

private fun splitSlashAlternativesKeepingCarbGroup(text: String): List<String>? {
    val normalized = protectBreadQualifierSlash(
        text
            .normalizeBreadQualifierShorthand()
            .normalizeMealUiLine()
    )

    if (!normalized.contains("/")) return null

    val parts = normalized
        .split("/")
        .map(::restoreProtectedBreadQualifierSlash)
        .map(String::trim)
        .filter(String::isNotBlank)

    if (parts.size < 2) return null

    val result = mutableListOf<String>()
    val current = mutableListOf<String>()

    parts.forEach { part ->
        if (current.isEmpty()) {
            current += part
        } else {
            val currentJoined = current.joinToString(" / ").normalizeMealUiLine()
            val currentHasQuantity = containsAlternativeQuantity(currentJoined)

            val nextStartsBreadAlternative =
                currentHasQuantity &&
                        (
                                part.contains("pane", ignoreCase = true) ||
                                        part.contains("panino", ignoreCase = true) ||
                                        part.equals("scuro", ignoreCase = true) ||
                                        part.equals("integrale", ignoreCase = true)
                                )

            if (nextStartsBreadAlternative) {
                result += currentJoined
                current.clear()
                current += part
            } else {
                current += part
            }
        }
    }

    if (current.isNotEmpty()) {
        result += current.joinToString(" / ").normalizeMealUiLine()
    }

    return result.takeIf { it.size > 1 }
}

private fun splitTopLevel(
    text: String,
    separatorLengthAt: (String, Int) -> Int
): List<String> {
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var depth = 0
    var index = 0

    fun flushCurrent() {
        val value = current.toString().normalizeMealUiLine()
        if (value.isNotBlank()) {
            result += value
        }
        current.clear()
    }

    while (index < text.length) {
        val char = text[index]

        when {
            char == '(' -> {
                depth += 1
                current.append(char)
                index += 1
            }

            char == ')' -> {
                if (depth > 0) depth -= 1
                current.append(char)
                index += 1
            }

            depth == 0 -> {
                val separatorLength = separatorLengthAt(text, index)
                if (separatorLength > 0) {
                    flushCurrent()
                    index += separatorLength
                } else {
                    current.append(char)
                    index += 1
                }
            }

            else -> {
                current.append(char)
                index += 1
            }
        }
    }

    flushCurrent()
    return result
}

private fun additiveSeparatorLengthAt(text: String, index: Int): Int {
    return when {
        text[index] == '+' -> 1
        text.regionMatches(index, " e ", 0, 3, ignoreCase = true) -> 3
        else -> 0
    }
}

private fun alternativeWordSeparatorLengthAt(text: String, index: Int): Int {
    return when {
        text.regionMatches(index, " e/o ", 0, 5, ignoreCase = true) -> 5
        text.regionMatches(index, " oppure ", 0, 8, ignoreCase = true) -> 8
        text.regionMatches(index, " in alternativa ", 0, 16, ignoreCase = true) -> 16
        text.regionMatches(index, " alternativa ", 0, 13, ignoreCase = true) -> 13
        text.regionMatches(index, " o ", 0, 3, ignoreCase = true) -> 3
        else -> 0
    }
}

private fun splitHeuristicSlashAlternatives(text: String): List<String> {
    val source = protectBreadQualifierSlash(text.normalizeBreadQualifierShorthand())
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var depth = 0
    var index = 0

    fun flushCurrent() {
        val value = restoreProtectedBreadQualifierSlash(current.toString()).normalizeMealUiLine()
        if (value.isNotBlank()) {
            result += value
        }
        current.clear()
    }

    while (index < source.length) {
        val char = source[index]

        when {
            char == '(' -> {
                depth += 1
                current.append(char)
                index += 1
            }

            char == ')' -> {
                if (depth > 0) depth -= 1
                current.append(char)
                index += 1
            }

            depth == 0 && char == '/' -> {
                val left = restoreProtectedBreadQualifierSlash(current.toString()).normalizeMealUiLine()
                val right = restoreProtectedBreadQualifierSlash(source.substring(index + 1).trimStart())

                if (shouldSplitSlashAlternative(left, right)) {
                    flushCurrent()
                    index += 1
                    while (index < source.length && source[index].isWhitespace()) {
                        index += 1
                    }
                } else {
                    current.append(char)
                    index += 1
                }
            }

            else -> {
                current.append(char)
                index += 1
            }
        }
    }

    flushCurrent()
    return result.ifEmpty { listOf(restoreProtectedBreadQualifierSlash(source).normalizeMealUiLine()) }
}

private fun shouldSplitSlashAlternative(
    left: String,
    right: String
): Boolean {
    if (left.isBlank() || right.isBlank()) return false

    val leftNormalized = left.normalizeMealUiLine()
    val rightNormalized = right.normalizeMealUiLine()

    if (
        leftNormalized.contains("pane", ignoreCase = true) &&
        (
                rightNormalized.equals("scuro", ignoreCase = true) ||
                        rightNormalized.equals("integrale", ignoreCase = true)
                )
    ) {
        return false
    }

    val leftHasQuantity = containsAlternativeQuantity(leftNormalized)
    val rightStartsWithQuantity = startsWithAlternativeQuantity(rightNormalized)

    if (rightStartsWithQuantity) return true
    if (leftHasQuantity) return true

    return false
}

private fun containsAlternativeQuantity(text: String): Boolean {
    return ALTERNATIVE_QUANTITY_REGEX.containsMatchIn(text)
}

private fun startsWithAlternativeQuantity(text: String): Boolean {
    return ALTERNATIVE_LEADING_QUANTITY_REGEX.containsMatchIn(text)
}

private fun normalizeInlineAlternativeText(text: String): String {
    return text
        .replace(Regex("(?i)/\\s+di\\s+(?=farro\\b|orzo\\b|riso\\b|couscous\\b|pane\\b|patate\\b)"), "/ ")
        .replace(Regex("\\s*/\\s*"), "/")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun isStandaloneAlternativeSeparatorLine(line: String): Boolean {
    return line == "/" ||
            line.equals("oppure", ignoreCase = true) ||
            line.equals("alternativa", ignoreCase = true) ||
            line.equals("in alternativa", ignoreCase = true)
}

private fun startsWithAlternativePrefix(text: String): Boolean {
    return ALTERNATIVE_PREFIX_REGEX.containsMatchIn(text)
}

private fun removeAlternativePrefix(text: String): String {
    return ALTERNATIVE_PREFIX_REGEX.replace(text, "").normalizeMealUiLine()
}

private fun String.normalizeAlternativeMarkers(): String {
    return replace(
        Regex("\\b(oppure|in alternativa|alternativa)\\s*:\\s*", RegexOption.IGNORE_CASE),
        "$1 "
    )
}

private fun restoreCollapsedWhitespace(text: String): String {
    return text.replace(Regex("\\s+"), " ").trim()
}

private fun MutableParsedMealComponent.merge(other: MutableParsedMealComponent) {
    alternatives += other.alternatives
    exampleNotes += other.exampleNotes
    genericNotes += other.genericNotes
    weeklyQuantityNotes += other.weeklyQuantityNotes
    mealQuantityNotes += other.mealQuantityNotes

    deduplicateInPlace(alternatives)
    deduplicateInPlace(exampleNotes)
    deduplicateInPlace(genericNotes)
    deduplicateInPlace(weeklyQuantityNotes)
    deduplicateInPlace(mealQuantityNotes)
}

private fun MutableParsedMealComponent.toImmutable(): ParsedMealComponent {
    return ParsedMealComponent(
        alternatives = alternatives.distinct(),
        exampleNotes = exampleNotes.distinct(),
        genericNotes = genericNotes.distinct(),
        weeklyQuantityNotes = weeklyQuantityNotes.distinct(),
        mealQuantityNotes = mealQuantityNotes.distinct()
    )
}

private fun <T> deduplicateInPlace(list: MutableList<T>) {
    val distinctValues = list.distinct()
    list.clear()
    list.addAll(distinctValues)
}

private fun inferMealVisualInfo(text: String): MealVisualInfo {
    val normalized = text
        .lowercase()
        .replace("’", "'")
        .normalizeMealUiLine()

    return when {
        "panino" in normalized || "panini" in normalized ->
            MealVisualInfo(emoji = "🥪", semanticKey = "panino")
        "piadina" in normalized ->
            MealVisualInfo(emoji = "🌯", semanticKey = "piadina")
        "frisella" in normalized || "friselle" in normalized ->
            MealVisualInfo(emoji = "🥯", semanticKey = "frisella")
        "insalatona" in normalized || "insalata" in normalized ->
            MealVisualInfo(emoji = "🥗", semanticKey = "insalata")
        "pasta fredda" in normalized ||
                "pasta" in normalized ||
                "riso" in normalized ||
                "orzo" in normalized ||
                "farro" in normalized ||
                "couscous" in normalized ||
                "minestrone" in normalized ->
            MealVisualInfo(emoji = "🍝", semanticKey = "cereale_primo")
        "pancake" in normalized ->
            MealVisualInfo(emoji = "🥞", semanticKey = "pancake")
        "yogurt" in normalized || "latte" in normalized || "kefir" in normalized ->
            MealVisualInfo(emoji = "🥛", semanticKey = "latticino")
        "pollo" in normalized ||
                "tacchino" in normalized ||
                "hamburger" in normalized ||
                "carne" in normalized ||
                "bresaola" in normalized ||
                "prosciutto" in normalized ||
                "affettato" in normalized ->
            MealVisualInfo(emoji = "🍗", semanticKey = "carne")
        "pesce" in normalized ||
                "salmone" in normalized ||
                "tonno" in normalized ||
                "sgombro" in normalized ->
            MealVisualInfo(emoji = "🐟", semanticKey = "pesce")
        "uova" in normalized || "uovo" in normalized || "frittata" in normalized ->
            MealVisualInfo(emoji = "🥚", semanticKey = "uova")
        "pane" in normalized ->
            MealVisualInfo(emoji = "🍞", semanticKey = "pane")
        "cereali" in normalized ||
                "cornflakes" in normalized ||
                "muesli" in normalized ||
                "granola" in normalized ||
                "porridge" in normalized ||
                "fette biscottate" in normalized ||
                "biscotti" in normalized ->
            MealVisualInfo(emoji = "🥣", semanticKey = "colazione_secca")
        "banana" in normalized ->
            MealVisualInfo(emoji = "🍌", semanticKey = "banana")
        "mela" in normalized ->
            MealVisualInfo(emoji = "🍎", semanticKey = "mela")
        "pera" in normalized ->
            MealVisualInfo(emoji = "🍐", semanticKey = "pera")
        "cioccolato" in normalized ->
            MealVisualInfo(emoji = "🍫", semanticKey = "cioccolato")
        "frutta" in normalized ||
                "frutto" in normalized ||
                "fragole" in normalized ||
                "kiwi" in normalized ||
                "arancia" in normalized ->
            MealVisualInfo(emoji = "🍓", semanticKey = "frutta")
        "mandorle" in normalized ||
                "nocciole" in normalized ||
                "noci" in normalized ||
                "arachidi" in normalized ||
                "frutta secca" in normalized ->
            MealVisualInfo(emoji = "🥜", semanticKey = "frutta_secca")
        "avocado" in normalized ->
            MealVisualInfo(emoji = "🥑", semanticKey = "avocado")
        "parmigiano" in normalized ||
                "primo sale" in normalized ||
                "ricotta" in normalized ||
                "mozzarella" in normalized ||
                "philadelphia" in normalized ||
                "formaggio" in normalized ||
                "feta" in normalized ->
            MealVisualInfo(emoji = "🧀", semanticKey = "formaggio")
        "pomodoro" in normalized || "pomodorini" in normalized ->
            MealVisualInfo(emoji = "🍅", semanticKey = "pomodoro")
        "carota" in normalized || "carote" in normalized ->
            MealVisualInfo(emoji = "🥕", semanticKey = "carota")
        "verdura" in normalized ||
                "lattuga" in normalized ||
                "lattughino" in normalized ||
                "songino" in normalized ||
                "rughetta" in normalized ||
                "radicchio" in normalized ||
                "valeriana" in normalized ||
                "zucchine" in normalized ->
            MealVisualInfo(emoji = "🥬", semanticKey = "verdura")
        "olio" in normalized || "olive" in normalized ->
            MealVisualInfo(emoji = "🫒", semanticKey = "olio")
        "miele" in normalized || "marmellata" in normalized ->
            MealVisualInfo(emoji = "🍯", semanticKey = "dolce_spalmabile")
        "caffè" in normalized || "caffe" in normalized ->
            MealVisualInfo(emoji = "☕", semanticKey = "caffe")
        else ->
            MealVisualInfo(emoji = "🍽️", semanticKey = "meal_generic")
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

private data class ParentheticalExtraction(
    val baseText: String,
    val notes: List<String>
)

private data class ParsedComponentParseResult(
    val component: MutableParsedMealComponent,
    val attachAsAlternativeToPrevious: Boolean
)

private data class MutableParsedMealComponent(
    val alternatives: MutableList<String> = mutableListOf(),
    val exampleNotes: MutableList<String> = mutableListOf(),
    val genericNotes: MutableList<String> = mutableListOf(),
    val weeklyQuantityNotes: MutableList<String> = mutableListOf(),
    val mealQuantityNotes: MutableList<String> = mutableListOf()
)

private val BULLET_PREFIX_REGEX = Regex("""^[•\-–—]\s*""")

private val ALTERNATIVE_PREFIX_REGEX = Regex(
    pattern = "^(?:oppure|in alternativa|alternativa)\\s*:?\\s+",
    option = RegexOption.IGNORE_CASE
)

private val EXAMPLE_PREFIX_REGEX = Regex(
    pattern = "^(?:es\\.?|ad\\s+es\\.?)\\s*",
    option = RegexOption.IGNORE_CASE
)

private val ALTERNATIVE_QUANTITY_REGEX = Regex(
    pattern = """\b(?:n\.?\s*\d+|\d+(?:[.,]\d+)?\s*(?:kg|g|gr|grammi?|ml|l|uov[ao]|fette?|cucchiai?|cucchiaini?))\b""",
    options = setOf(RegexOption.IGNORE_CASE)
)

private val ALTERNATIVE_LEADING_QUANTITY_REGEX = Regex(
    pattern = """^(?:n\.?\s*\d+|\d+(?:[.,]\d+)?\s*(?:kg|g|gr|grammi?|ml|l|uov[ao]|fette?|cucchiai?|cucchiaini?))\b""",
    options = setOf(RegexOption.IGNORE_CASE)
)