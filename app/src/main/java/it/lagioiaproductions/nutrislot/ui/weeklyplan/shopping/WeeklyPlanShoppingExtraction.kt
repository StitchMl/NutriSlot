package it.lagioiaproductions.nutrislot.ui.weeklyplan

internal fun extractShoppingItemsFromSlots(slots: List<WeeklySlotUi>): List<String> {
    return slots
        .flatMap { extractShoppingItemsFromMealText(it.displayedMealText) }
        .distinct()
}

internal fun extractShoppingItemsFromMealText(mealText: String): List<String> {
    val sections = parseMealStructuredSections(mealText)
    val parsedItems = sections
        .flatMap { section -> section.components }
        .mapNotNull(::buildShoppingItemFromComponent)

    if (parsedItems.isEmpty()) return emptyList()
    return normalizeShoppingItems(parsedItems)
}

private fun buildShoppingItemFromComponent(component: ParsedMealComponent): String? {
    val normalizedAlternatives = component.alternatives
        .map(::normalizeShoppingText)
        .filter { it.isNotBlank() }

    if (normalizedAlternatives.isEmpty()) return null

    val expandedAlternatives = collapseBreadVariantsForShopping(normalizedAlternatives)
    val base = expandedAlternatives.joinToString(separator = " / ")
    if (base.isBlank()) return null

    val inlineNotes = mutableListOf<String>()

    if (component.mealQuantityNotes.isNotEmpty()) {
        inlineNotes += component.mealQuantityNotes.distinct()
    }

    if (component.exampleNotes.isNotEmpty()) {
        inlineNotes += "es. ${component.exampleNotes.distinct().joinToString(separator = "; ")}"
    }

    val genericNotesToKeep = component.genericNotes
        .filter { note ->
            val normalized = note.lowercase()
            "%" in normalized || normalized.contains("integrale") || normalized.contains("light")
        }
        .distinct()

    inlineNotes += genericNotesToKeep

    return if (inlineNotes.isEmpty()) {
        base
    } else {
        "$base (${inlineNotes.joinToString(separator = "; ")})"
    }
}
