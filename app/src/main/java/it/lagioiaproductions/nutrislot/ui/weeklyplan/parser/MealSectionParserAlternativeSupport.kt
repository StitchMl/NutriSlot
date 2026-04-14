package it.lagioiaproductions.nutrislot.ui.weeklyplan.parser

import it.lagioiaproductions.nutrislot.ui.shared.normalizeMealUiLine

private val ALTERNATIVE_QUANTITY_REGEX = Regex(
    pattern = """\b(?:n\.?\s*\d+|\d+(?:[.,]\d+)?\s*(?:kg|g|gr|grammi?|ml|l|uov[ao]|fette?|cucchiai?|cucchiaini?))\b""",
    options = setOf(RegexOption.IGNORE_CASE)
)

private val ALTERNATIVE_LEADING_QUANTITY_REGEX = Regex(
    pattern = """^(?:n\.?\s*\d+|\d+(?:[.,]\d+)?\s*(?:kg|g|gr|grammi?|ml|l|uov[ao]|fette?|cucchiai?|cucchiaini?))\b""",
    options = setOf(RegexOption.IGNORE_CASE)
)

internal fun splitTopLevelAdditives(text: String): List<String> {
    return splitTopLevel(
        text
            .normalizeAlternativeMarkers()
            .normalizeBreadQualifierShorthand()
    ) { source, index ->
        additiveSeparatorLengthAt(source, index)
    }
}

internal fun splitTopLevelAlternatives(text: String): List<String> {
    val normalized = text
        .normalizeAlternativeMarkers()
        .normalizeBreadQualifierShorthand()

    val splitByWords = splitTopLevel(normalized) { source, index ->
        alternativeWordSeparatorLengthAt(source, index)
    }

    return splitByWords
        .flatMap(::splitBreadAwareAlternatives)
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
