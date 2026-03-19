package it.lagioiaproductions.nutrislot.ui.importpreview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.domain.model.CellRecognitionState
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.ui.importfile.EditableImportedMealCellUi

@Composable
internal fun EditableMealCellCard(
    modifier: Modifier = Modifier,
    cell: EditableImportedMealCellUi?,
    slotType: MealSlotType,
    onClick: () -> Unit
) {
    val style = remember(cell?.mealSlotType ?: slotType, cell?.originalRecognitionState, cell?.wasManuallyEdited) {
        previewVisualStyleForSlot(
            slotType = cell?.mealSlotType ?: slotType,
            recognitionState = cell?.originalRecognitionState ?: CellRecognitionState.EMPTY,
            wasManuallyEdited = cell?.wasManuallyEdited ?: false
        )
    }

    ElevatedCard(
        modifier = modifier
            .heightIn(min = 156.dp)
            .clickable(enabled = cell != null, onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = style.container
        )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(8.dp)
                    .background(style.accent)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = style.badgeContainer
                ) {
                    Text(
                        text = recognitionLabel(
                            state = cell?.originalRecognitionState ?: CellRecognitionState.EMPTY,
                            wasManuallyEdited = cell?.wasManuallyEdited ?: false
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = style.badgeContent
                    )
                }

                if (cell == null || cell.mealText.isBlank()) {
                    Text(
                        text = "Tocca per aggiungere o correggere",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = cell.mealText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF1F1A17),
                        maxLines = 7,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = if (cell == null) "Vuoto" else "Tocca per modificare",
                    style = MaterialTheme.typography.labelMedium,
                    color = style.accent,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun recognitionLabel(
    state: CellRecognitionState,
    wasManuallyEdited: Boolean
): String {
    if (wasManuallyEdited) return "Modificato"

    return when (state) {
        CellRecognitionState.RECOGNIZED -> "OK"
        CellRecognitionState.SUSPECTED -> "Dubbio"
        CellRecognitionState.MISSING_DAY -> "Giorno?"
        CellRecognitionState.MISSING_MEAL_SLOT -> "Slot?"
        CellRecognitionState.EMPTY -> "Vuoto"
    }
}

private data class PreviewCellVisualStyle(
    val container: Color,
    val accent: Color,
    val badgeContainer: Color,
    val badgeContent: Color
)

private fun previewVisualStyleForSlot(
    slotType: MealSlotType,
    recognitionState: CellRecognitionState,
    wasManuallyEdited: Boolean
): PreviewCellVisualStyle {
    val base = when (slotType) {
        MealSlotType.BREAKFAST -> basePreviewStyle(
            container = Color(0xFFFFF4EC),
            accent = Color(0xFFFFA36C)
        )
        MealSlotType.MORNING_SNACK -> basePreviewStyle(
            container = Color(0xFFF1FAF1),
            accent = Color(0xFF73C27C)
        )
        MealSlotType.LUNCH -> basePreviewStyle(
            container = Color(0xFFEDF5FF),
            accent = Color(0xFF5AA9FF)
        )
        MealSlotType.AFTERNOON_SNACK -> basePreviewStyle(
            container = Color(0xFFFFF4E3),
            accent = Color(0xFFFFC15A)
        )
        MealSlotType.DINNER -> basePreviewStyle(
            container = Color(0xFFF4F0FF),
            accent = Color(0xFF9A89FF)
        )
    }

    return when {
        wasManuallyEdited -> base.copy(
            badgeContainer = Color(0xFFDFF0E0),
            badgeContent = Color(0xFF1F5A28)
        )
        recognitionState == CellRecognitionState.RECOGNIZED -> base.copy(
            badgeContainer = Color(0xFFDDEAFE),
            badgeContent = Color(0xFF1E4E8C)
        )
        recognitionState == CellRecognitionState.EMPTY -> base.copy(
            badgeContainer = Color(0xFFE7E7E7),
            badgeContent = Color(0xFF4A4A4A)
        )
        else -> base.copy(
            badgeContainer = Color(0xFFFFE1DE),
            badgeContent = Color(0xFF8C2F27)
        )
    }
}

private fun basePreviewStyle(
    container: Color,
    accent: Color
): PreviewCellVisualStyle {
    return PreviewCellVisualStyle(
        container = container,
        accent = accent,
        badgeContainer = Color(0xFFE7E7E7),
        badgeContent = Color(0xFF4A4A4A)
    )
}