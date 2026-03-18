package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.SlotDisplayState
import it.lagioiaproductions.nutrislot.domain.model.WeekDay

@Composable
internal fun DietHeroCard(
    uiState: WeeklyPlanUiState
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Piano alimentare",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = uiState.planTitle ?: "Settimana attiva",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            val subtitle = buildString {
                append("Gestisci gli slot della settimana e tieni traccia dei pasti già usati.")
                uiState.sourceFileName?.let { fileName ->
                    append(" • File: ")
                    append(fileName)
                }
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeeklyStatusBadge(
                    text = "Slot con contenuto: ${uiState.populatedSlotsCount}/${uiState.slots.size}"
                )

                WeeklyStatusBadge(
                    text = "Giorno attivo: ${uiState.selectedCalendarDay.displayName}",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )

                WeeklyStatusBadge(
                    text = if (uiState.showConsumedSlotsInCalendar) {
                        "Completati visibili"
                    } else {
                        "Completati nascosti"
                    },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
internal fun FeedbackCard(
    title: String,
    message: String,
    isError: Boolean
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
internal fun DayStripCard(
    uiState: WeeklyPlanUiState,
    onSelectCalendarDay: (WeekDay) -> Unit
) {
    val orderedDays = uiState.orderedCalendarDays

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Settimana",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Scegli il giorno su cui vuoi lavorare.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(orderedDays) { day ->
                    FilterChip(
                        selected = uiState.selectedCalendarDay == day,
                        onClick = { onSelectCalendarDay(day) },
                        label = {
                            Text(
                                if (day == uiState.currentWeekReferenceDay) {
                                    "${day.displayName} • Oggi"
                                } else {
                                    day.displayName
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun CalendarControlsCard(
    uiState: WeeklyPlanUiState,
    hiddenSelectedDaySlotsCount: Int,
    onToggleConsumedSlotsVisibility: () -> Unit,
    onImportClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Controlli rapidi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            FilterChip(
                selected = uiState.showConsumedSlotsInCalendar,
                onClick = onToggleConsumedSlotsVisibility,
                label = {
                    Text(
                        if (uiState.showConsumedSlotsInCalendar) {
                            "Mostrando anche i pasti completati"
                        } else {
                            "Nascondi i pasti completati"
                        }
                    )
                }
            )

            if (!uiState.showConsumedSlotsInCalendar && hiddenSelectedDaySlotsCount > 0) {
                Text(
                    text = "Per ${uiState.selectedCalendarDay.displayName.lowercase()} ho nascosto $hiddenSelectedDaySlotsCount slot già completati nella settimana corrente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onImportClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Importa nuovo piano")
                }

                FilledTonalButton(
                    onClick = onRefreshClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Aggiorna dieta")
                }
            }
        }
    }
}

@Composable
internal fun SelectedDayHeroCard(
    selectedDay: WeekDay,
    visibleSlotsCount: Int,
    totalSlotsCount: Int
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = selectedDay.displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Hai $visibleSlotsCount slot visibili su $totalSlotsCount per questo giorno.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun MealTypeBadge(
    mealSlotType: MealSlotType
) {
    val containerColor = when (mealSlotType) {
        MealSlotType.BREAKFAST -> MaterialTheme.colorScheme.tertiaryContainer
        MealSlotType.MORNING_SNACK -> MaterialTheme.colorScheme.secondaryContainer
        MealSlotType.LUNCH -> MaterialTheme.colorScheme.primaryContainer
        MealSlotType.AFTERNOON_SNACK -> MaterialTheme.colorScheme.secondaryContainer
        MealSlotType.DINNER -> MaterialTheme.colorScheme.errorContainer
    }

    val contentColor = when (mealSlotType) {
        MealSlotType.BREAKFAST -> MaterialTheme.colorScheme.onTertiaryContainer
        MealSlotType.MORNING_SNACK -> MaterialTheme.colorScheme.onSecondaryContainer
        MealSlotType.LUNCH -> MaterialTheme.colorScheme.onPrimaryContainer
        MealSlotType.AFTERNOON_SNACK -> MaterialTheme.colorScheme.onSecondaryContainer
        MealSlotType.DINNER -> MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor
    ) {
        Text(
            text = mealSlotType.displayName,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}

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