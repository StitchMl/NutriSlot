package it.lagioiaproductions.nutrislot.ui.weeklyplan.calendar

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.domain.model.SlotDisplayState

@Composable
internal fun WeeklyStatusBadge(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}

internal fun slotStatusLabel(
    state: SlotDisplayState
): String {
    return when (state) {
        SlotDisplayState.Empty -> "Vuoto"
        SlotDisplayState.PlannedAvailable -> "Disponibile"
        SlotDisplayState.ConsumedAsPlanned -> "Completato"
        is SlotDisplayState.ConsumedWithReplacement -> "Completato con scambio"
        SlotDisplayState.OriginalMealAlreadyUsedElsewhere -> "Pasto spostato"
    }
}

@Composable
internal fun slotStatusContainerColor(
    state: SlotDisplayState
): Color {
    return when (state) {
        SlotDisplayState.Empty -> MaterialTheme.colorScheme.surfaceVariant
        SlotDisplayState.PlannedAvailable -> MaterialTheme.colorScheme.primaryContainer
        SlotDisplayState.ConsumedAsPlanned -> MaterialTheme.colorScheme.tertiaryContainer
        is SlotDisplayState.ConsumedWithReplacement -> MaterialTheme.colorScheme.secondaryContainer
        SlotDisplayState.OriginalMealAlreadyUsedElsewhere -> MaterialTheme.colorScheme.errorContainer
    }
}

@Composable
internal fun slotStatusContentColor(
    state: SlotDisplayState
): Color {
    return when (state) {
        SlotDisplayState.Empty -> MaterialTheme.colorScheme.onSurfaceVariant
        SlotDisplayState.PlannedAvailable -> MaterialTheme.colorScheme.onPrimaryContainer
        SlotDisplayState.ConsumedAsPlanned -> MaterialTheme.colorScheme.onTertiaryContainer
        is SlotDisplayState.ConsumedWithReplacement -> MaterialTheme.colorScheme.onSecondaryContainer
        SlotDisplayState.OriginalMealAlreadyUsedElsewhere -> MaterialTheme.colorScheme.onErrorContainer
    }
}