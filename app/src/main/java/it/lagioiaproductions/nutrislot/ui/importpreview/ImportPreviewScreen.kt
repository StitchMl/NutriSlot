@file:Suppress("AssignedValueIsNeverRead")

package it.lagioiaproductions.nutrislot.ui.importpreview

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    if (!uiState.hasEditableDraft && uiState.importedDraft == null) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Controlla import") }
                )
            }
        ) { innerPadding ->
            EmptyPreviewContent(
                innerPadding = innerPadding,
                onBackClick = onBackClick
            )
        }
        return
    }

    var editingCellId by rememberSaveable { mutableStateOf<String?>(null) }
    val gridState = rememberImportPreviewGridState(
        uiState = uiState,
        editingCellId = editingCellId
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Controlla import") }
            )
        },
        bottomBar = {
            ImportPreviewBottomBar(
                uiState = uiState,
                onBackClick = onBackClick,
                onConfirmReviewClick = onConfirmReviewClick
            )
        }
    ) { innerPadding ->
        ImportPreviewContent(
            modifier = Modifier.padding(innerPadding),
            uiState = uiState,
            gridState = gridState,
            onTogglePreviewDay = onTogglePreviewDay,
            onToggleShowOnlyFilledSlots = onToggleShowOnlyFilledSlots,
            onCellClick = { clicked -> editingCellId = clicked.id }
        )

        gridState.editingCell?.let { editingCell ->
            EditMealCellDialog(
                cell = editingCell,
                onDismiss = { editingCellId = null },
                onSave = { updatedText ->
                    onMealTextChange(editingCell.id, updatedText)
                    editingCellId = null
                },
                onClear = {
                    onClearCellClick(editingCell.id)
                    editingCellId = null
                }
            )
        }
    }
}