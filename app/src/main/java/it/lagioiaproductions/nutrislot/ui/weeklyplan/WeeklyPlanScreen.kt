package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.ui.shared.ShoppingFeedbackUi

@Composable
fun WeeklyPlanScreen(
    uiState: WeeklyPlanUiState,
    onImportClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onOpenSlotAction: (slotId: String) -> Unit,
    onOpenSlotEdit: (slotId: String) -> Unit,
    onToggleSlotCompleted: (slotId: String) -> Unit,
    onDismissSlotAction: () -> Unit,
    onConsumeAsPlanned: () -> Unit,
    onConsumeReplacement: (sourceSlotId: String) -> Unit,
    onSelectCalendarDay: (WeekDay) -> Unit,
    onToggleConsumedSlotsVisibility: () -> Unit,
    onAddMealToShopping: (List<String>) -> Unit,
    onAddDayToShopping: (List<String>) -> Unit,
    onAddWeekToShopping: (List<String>) -> Unit,
    onOpenWeeklyQuantityChecklist: () -> Unit,
    shoppingFeedback: ShoppingFeedbackUi?,
    onSelectExtraCatalogOption: (String) -> Unit,
    onUndoCompletedMeal: () -> Unit,
    onConsumeShoppingFeedback: () -> Unit,
    onSaveSlotEdit: (mealText: String, nutritionText: String) -> Unit,
    onDismissSlotEdit: () -> Unit,
    onResetSlotEdit: () -> Unit
) {
    val hasLoadedPlan = uiState.planId != null || uiState.slots.isNotEmpty()
    val plannerFeedbackState = rememberPlannerShoppingFeedbackState(
        shoppingFeedback = shoppingFeedback,
        onConsumeShoppingFeedback = onConsumeShoppingFeedback
    )

    val addMealWithFeedback: (List<String>) -> Unit = { items ->
        plannerFeedbackState.dispatch(
            rawItems = items,
            submit = onAddMealToShopping,
            singleLabel = "Pasto",
            pluralLabel = "articoli del pasto"
        )
    }
    val addDayWithFeedback: (List<String>) -> Unit = { items ->
        plannerFeedbackState.dispatch(
            rawItems = items,
            submit = onAddDayToShopping,
            singleLabel = "Giorno",
            pluralLabel = "articoli del giorno"
        )
    }
    val addWeekWithFeedback: (List<String>) -> Unit = { items ->
        plannerFeedbackState.dispatch(
            rawItems = items,
            submit = onAddWeekToShopping,
            singleLabel = "Settimana",
            pluralLabel = "articoli della settimana"
        )
    }

    Scaffold { innerPadding ->
        when {
            uiState.isLoading -> LoadingContent(innerPadding = innerPadding)
            uiState.errorMessage != null -> ErrorContent(
                innerPadding = innerPadding,
                message = uiState.errorMessage,
                onImportClick = onImportClick,
                onRefreshClick = onRefreshClick
            )
            !hasLoadedPlan -> ImportOnlyContent(
                innerPadding = innerPadding,
                onImportClick = onImportClick
            )
            else -> WeeklyCalendarGridContent(
                innerPadding = innerPadding,
                uiState = uiState,
                onImportClick = onImportClick,
                onRefreshClick = onRefreshClick,
                onOpenSlotAction = onOpenSlotAction,
                onOpenSlotEdit = onOpenSlotEdit,
                onToggleSlotCompleted = onToggleSlotCompleted,
                onSelectCalendarDay = onSelectCalendarDay,
                onToggleConsumedSlotsVisibility = onToggleConsumedSlotsVisibility,
                onAddMealToShopping = addMealWithFeedback,
                onAddDayToShopping = addDayWithFeedback,
                onAddWeekToShopping = addWeekWithFeedback,
                onOpenWeeklyQuantityChecklist = onOpenWeeklyQuantityChecklist,
                plannerFeedbackMessage = plannerFeedbackState.message
            )
        }

        uiState.slotActionDialog?.let { dialogUi ->
            SlotActionDialog(
                dialogUi = dialogUi,
                isApplying = uiState.isApplyingSlotAction,
                onDismiss = onDismissSlotAction,
                onConsumeAsPlanned = onConsumeAsPlanned,
                onConsumeReplacement = onConsumeReplacement,
                onSelectExtraCatalogOption = onSelectExtraCatalogOption,
                onUndoCompletedMeal = onUndoCompletedMeal
            )
        }

        uiState.editSlotDialog?.let { dialogUi ->
            EditSlotDialog(
                dialogUi = dialogUi,
                onDismiss = onDismissSlotEdit,
                onSave = onSaveSlotEdit,
                onReset = onResetSlotEdit
            )
        }
    }
}