package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import it.lagioiaproductions.nutrislot.ui.shared.ShoppingFeedbackUi
import kotlinx.coroutines.delay

@Stable
class PlannerShoppingFeedbackState internal constructor(
    initialMessage: String? = null
) {
    private var nextFeedbackId by mutableLongStateOf(1L)
    private var feedbackToken by mutableStateOf(
        initialMessage?.let { PlannerFeedbackTokenUi(id = 0L, message = it) }
    )

    val message: String?
        get() = feedbackToken?.message

    val tokenId: Long?
        get() = feedbackToken?.id

    fun showExternalFeedback(message: String) {
        feedbackToken = PlannerFeedbackTokenUi(
            id = nextFeedbackId++,
            message = message
        )
    }

    fun clear(tokenId: Long) {
        if (feedbackToken?.id == tokenId) {
            feedbackToken = null
        }
    }

    fun dispatch(
        rawItems: List<String>,
        submit: (List<String>) -> Unit,
        singleLabel: String,
        pluralLabel: String
    ) {
        val cleanedItems = normalizeShoppingItems(rawItems)
        if (cleanedItems.isEmpty()) {
            showExternalFeedback("Nessun elemento valido da aggiungere alla lista della spesa.")
            return
        }

        submit(cleanedItems)
        showExternalFeedback(
            when (cleanedItems.size) {
                1 -> "$singleLabel aggiunto alla lista della spesa."
                else -> "${cleanedItems.size} $pluralLabel aggiunti alla lista della spesa."
            }
        )
    }
}

@Composable
fun rememberPlannerShoppingFeedbackState(
    shoppingFeedback: ShoppingFeedbackUi?,
    onConsumeShoppingFeedback: () -> Unit
): PlannerShoppingFeedbackState {
    val state = remember { PlannerShoppingFeedbackState() }

    LaunchedEffect(state.tokenId) {
        val activeId = state.tokenId ?: return@LaunchedEffect
        delay(2200)
        state.clear(activeId)
    }

    LaunchedEffect(shoppingFeedback?.id) {
        shoppingFeedback?.let { feedback ->
            state.showExternalFeedback(feedback.message)
            onConsumeShoppingFeedback()
        }
    }

    return state
}

internal data class PlannerFeedbackTokenUi(
    val id: Long,
    val message: String
)

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

private fun buildShoppingItemFromComponent(
    component: ParsedMealComponent
): String? {
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

private val BREAD_ONLY_REGEX = Regex(
    pattern = """^(?:\d+(?:[.,]\d+)?\s*(?:kg|g|gr|grammi?|ml|l)\s+di\s+)?pane\s+(scuro|integrale)\b.*$""",
    options = setOf(RegexOption.IGNORE_CASE)
)

private fun collapseBreadVariantsForShopping(alternatives: List<String>): List<String> {
    if (alternatives.size < 2) return alternatives

    val breadAlternatives = alternatives.filter { BREAD_ONLY_REGEX.matches(it) }
    val nonBreadAlternatives = alternatives.filterNot { BREAD_ONLY_REGEX.matches(it) }

    if (breadAlternatives.size < 2) return alternatives

    val qualifiers = breadAlternatives.mapNotNull { alt ->
        BREAD_ONLY_REGEX.matchEntire(alt)?.groupValues?.getOrNull(1)?.lowercase()
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

private fun normalizeShoppingItems(items: List<String>): List<String> {
    return items
        .mapNotNull { line ->
            val cleaned = line
                .stripMealNutritionBlock()
                .replace(Regex("^[-+•\\s]+"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
                .removeSuffix(".")
                .trim()

            cleaned.takeIf {
                it.isNotBlank() &&
                        !isNutritionLine(it) &&
                        !isStandaloneShoppingHeading(it)
            }
        }
        .distinct()
}

private fun normalizeShoppingText(text: String): String {
    return text
        .replace(Regex("\\s+"), " ")
        .trim()
}
private fun isStandaloneShoppingHeading(line: String): Boolean {
    return when (line.lowercase().trim()) {
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