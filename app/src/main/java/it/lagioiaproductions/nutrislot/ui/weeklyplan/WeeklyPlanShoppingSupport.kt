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
    return normalizeShoppingItems(
        mealText
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace("•", "\n")
            .lines()
    )
}

private fun normalizeShoppingItems(items: List<String>): List<String> {
    return items
        .map { line ->
            line
                .replace(Regex("^[-+•\\s]+"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
                .removeSuffix(".")
        }
        .filter { it.isNotBlank() }
        .distinct()
}