package it.lagioiaproductions.nutrislot.ui.weeklyplan

internal fun parseMealSections(text: String): List<List<String>> {
    val rawLines = text
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .replace("•", "\n• ")
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }

    if (rawLines.isEmpty()) {
        return emptyList()
    }

    val sections = mutableListOf<MutableList<String>>()
    var currentSection = mutableListOf<String>()

    fun flushSection() {
        if (currentSection.isNotEmpty()) {
            sections += currentSection
            currentSection = mutableListOf()
        }
    }

    rawLines.forEach { rawLine ->
        val normalizedLine = rawLine
            .trim()
            .removePrefix("•")
            .removePrefix("-")
            .removePrefix("–")
            .removePrefix("—")
            .trim()

        if (normalizedLine.isBlank()) {
            return@forEach
        }

        if (normalizedLine == "+") {
            flushSection()
            return@forEach
        }

        val previous = currentSection.lastOrNull()
        if (previous != null && shouldAppendToPreviousLine(previous, normalizedLine)) {
            currentSection[currentSection.lastIndex] = "$previous $normalizedLine"
        } else {
            currentSection += normalizedLine
        }
    }

    flushSection()
    return sections
}

private fun shouldAppendToPreviousLine(
    previous: String,
    current: String
): Boolean {
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
                previous.endsWith(" ed") ||
                previous.endsWith(" oppure")

    return currentLooksLikeContinuation || previousLooksOpen
}