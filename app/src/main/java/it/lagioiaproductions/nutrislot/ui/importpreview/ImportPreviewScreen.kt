package it.lagioiaproductions.nutrislot.ui.importpreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.ui.importfile.ImportFileUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPreviewScreen(
    uiState: ImportFileUiState,
    onMealTextChange: (cellId: String, newValue: String) -> Unit,
    onClearCellClick: (cellId: String) -> Unit,
    onBackClick: () -> Unit,
    onConfirmReviewClick: () -> Unit,
    onTogglePreviewDay: (WeekDay?) -> Unit,
    onToggleShowOnlyFilledSlots: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Anteprima import")
                }
            )
        }
    ) { innerPadding ->
        if (!uiState.hasEditableDraft && uiState.importedDraft == null) {
            EmptyPreviewContent(
                innerPadding = innerPadding,
                onBackClick = onBackClick
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 12.dp,
                end = 16.dp,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                PreviewHeroCard(uiState = uiState)
            }

            item {
                PreviewFiltersCard(
                    uiState = uiState,
                    onTogglePreviewDay = onTogglePreviewDay,
                    onToggleShowOnlyFilledSlots = onToggleShowOnlyFilledSlots
                )
            }

            if (uiState.warnings.isNotEmpty()) {
                item {
                    ImportWarningsCard(
                        warnings = uiState.warnings.map { it.message }
                    )
                }
            }

            if (uiState.additionalOptions.isNotEmpty()) {
                item {
                    AdditionalOptionsSummaryCard(options = uiState.additionalOptions)
                }

                items(
                    items = uiState.additionalOptions.groupBy { it.mealSlotType }.toList(),
                    key = { it.first.name }
                ) { (slotType, options) ->
                    AdditionalOptionsCard(
                        slotType = slotType,
                        options = options
                    )
                }
            }

            if (uiState.mealRules.isNotEmpty()) {
                item {
                    MealRulesCard(rules = uiState.mealRules)
                }
            }

            if (uiState.filteredEditableCells.isEmpty()) {
                item {
                    EmptyFilteredStateCard()
                }
            } else {
                val grouped = uiState.filteredEditableCells.groupBy { it.dayOfWeek }

                grouped.forEach { (day, cells) ->
                    item(key = "header_${day.name}") {
                        DayHeaderCard(dayDisplayName = day.displayName)
                    }

                    items(
                        items = cells,
                        key = { it.id }
                    ) { cell ->
                        EditableMealCellCard(
                            cell = cell,
                            onMealTextChange = onMealTextChange,
                            onClearCellClick = { onClearCellClick(cell.id) }
                        )
                    }
                }
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onConfirmReviewClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading
                    ) {
                        Text(
                            if (uiState.isLoading) {
                                "Salvataggio in corso..."
                            } else {
                                "Conferma revisione"
                            }
                        )
                    }

                    FilledTonalButton(
                        onClick = onBackClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading
                    ) {
                        Text("Torna all’import")
                    }
                }
            }
        }
    }
}