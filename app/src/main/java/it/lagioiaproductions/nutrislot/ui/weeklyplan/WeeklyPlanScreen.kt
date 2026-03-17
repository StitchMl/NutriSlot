package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.SlotDisplayState
import it.lagioiaproductions.nutrislot.domain.model.WeekDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyPlanScreen(
    uiState: WeeklyPlanUiState,
    onImportClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onOpenSlotAction: (slotId: String) -> Unit,
    onDismissSlotAction: () -> Unit,
    onConsumeAsPlanned: () -> Unit,
    onConsumeReplacement: (sourceSlotId: String) -> Unit,
    onSelectCalendarDay: (WeekDay) -> Unit,
    onToggleConsumedSlotsVisibility: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("La tua dieta")
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                LoadingContent(innerPadding = innerPadding)
            }

            uiState.errorMessage != null -> {
                ErrorContent(
                    innerPadding = innerPadding,
                    message = uiState.errorMessage,
                    onImportClick = onImportClick,
                    onRefreshClick = onRefreshClick
                )
            }

            uiState.isEmpty -> {
                EmptyContent(
                    innerPadding = innerPadding,
                    onImportClick = onImportClick
                )
            }

            else -> {
                LoadedWeeklyPlanContent(
                    innerPadding = innerPadding,
                    uiState = uiState,
                    onImportClick = onImportClick,
                    onRefreshClick = onRefreshClick,
                    onOpenSlotAction = onOpenSlotAction,
                    onSelectCalendarDay = onSelectCalendarDay,
                    onToggleConsumedSlotsVisibility = onToggleConsumedSlotsVisibility
                )
            }
        }

        uiState.slotActionDialog?.let { dialogUi ->
            SlotActionDialog(
                dialogUi = dialogUi,
                isApplying = uiState.isApplyingSlotAction,
                onDismiss = onDismissSlotAction,
                onConsumeAsPlanned = onConsumeAsPlanned,
                onConsumeReplacement = onConsumeReplacement
            )
        }
    }
}

@Composable
private fun LoadingContent(
    innerPadding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator()

        Text(
            text = "Sto caricando il piano settimanale...",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ErrorContent(
    innerPadding: PaddingValues,
    message: String,
    onImportClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Non riesco a caricare la dieta",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Button(
            onClick = onRefreshClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Riprova")
        }

        FilledTonalButton(
            onClick = onImportClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Importa un piano")
        }
    }
}

@Composable
private fun EmptyContent(
    innerPadding: PaddingValues,
    onImportClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Nessuna dieta salvata",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Importa un PDF simile ai tuoi piani alimentari, controlla l’anteprima e poi torna qui per vedere la settimana in una vista più chiara e dinamica.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Button(
            onClick = onImportClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Importa piano da file")
        }
    }
}

@Composable
private fun LoadedWeeklyPlanContent(
    innerPadding: PaddingValues,
    uiState: WeeklyPlanUiState,
    onImportClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onOpenSlotAction: (slotId: String) -> Unit,
    onSelectCalendarDay: (WeekDay) -> Unit,
    onToggleConsumedSlotsVisibility: () -> Unit
) {
    val selectedDaySlots = uiState.slots
        .filter { it.dayOfWeek == uiState.selectedCalendarDay }

    val visibleSelectedDaySlots = if (uiState.showConsumedSlotsInCalendar) {
        selectedDaySlots
    } else {
        selectedDaySlots.filterNot { slotUi ->
            slotUi.isActuallyCompletedThisWeek
        }
    }

    val hiddenSelectedDaySlotsCount = selectedDaySlots.size - visibleSelectedDaySlots.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .safeDrawingPadding(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp,
            end = 16.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DietHeroCard(uiState = uiState)
        }

        uiState.actionMessage?.let { message ->
            item {
                FeedbackCard(
                    title = "Aggiornamento completato",
                    message = message,
                    isError = false
                )
            }
        }

        uiState.actionErrorMessage?.let { message ->
            item {
                FeedbackCard(
                    title = "Operazione non riuscita",
                    message = message,
                    isError = true
                )
            }
        }

        item {
            DayStripCard(
                uiState = uiState,
                onSelectCalendarDay = onSelectCalendarDay
            )
        }

        item {
            CalendarControlsCard(
                uiState = uiState,
                hiddenSelectedDaySlotsCount = hiddenSelectedDaySlotsCount,
                onToggleConsumedSlotsVisibility = onToggleConsumedSlotsVisibility,
                onImportClick = onImportClick,
                onRefreshClick = onRefreshClick
            )
        }

        item {
            SelectedDayHeroCard(
                selectedDay = uiState.selectedCalendarDay,
                visibleSlotsCount = visibleSelectedDaySlots.size,
                totalSlotsCount = selectedDaySlots.size
            )
        }

        if (visibleSelectedDaySlots.isEmpty()) {
            item {
                EmptySelectedDayStateCard(
                    selectedDay = uiState.selectedCalendarDay,
                    hiddenSelectedDaySlotsCount = hiddenSelectedDaySlotsCount,
                    isShowingConsumed = uiState.showConsumedSlotsInCalendar
                )
            }
        } else {
            items(
                items = visibleSelectedDaySlots,
                key = { it.slotId }
            ) { slotUi ->
                WeeklySlotCard(
                    slotUi = slotUi,
                    onManageClick = { onOpenSlotAction(slotUi.slotId) }
                )
            }
        }
    }
}

@Composable
private fun DietHeroCard(
    uiState: WeeklyPlanUiState
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = uiState.planTitle ?: "Piano importato",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            uiState.sourceFileName?.let { fileName ->
                Text(
                    text = "Origine file: $fileName",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            StatusBadge(
                text = "Slot con contenuto: ${uiState.populatedSlotsCount} / ${uiState.slots.size}"
            )
        }
    }
}

@Composable
private fun FeedbackCard(
    title: String,
    message: String,
    isError: Boolean
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
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
private fun DayStripCard(
    uiState: WeeklyPlanUiState,
    onSelectCalendarDay: (WeekDay) -> Unit
) {
    val orderedDays = uiState.orderedCalendarDays

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Settimana",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
private fun CalendarControlsCard(
    uiState: WeeklyPlanUiState,
    hiddenSelectedDaySlotsCount: Int,
    onToggleConsumedSlotsVisibility: () -> Unit,
    onImportClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
private fun SelectedDayHeroCard(
    selectedDay: WeekDay,
    visibleSlotsCount: Int,
    totalSlotsCount: Int
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
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
                text = "Slot visibili: $visibleSlotsCount / $totalSlotsCount",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun EmptySelectedDayStateCard(
    selectedDay: WeekDay,
    hiddenSelectedDaySlotsCount: Int,
    isShowingConsumed: Boolean
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Nessun slot da mostrare per ${selectedDay.displayName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = when {
                    !isShowingConsumed && hiddenSelectedDaySlotsCount > 0 ->
                        "Gli slot di questa giornata risultano già completati. Puoi mostrarli di nuovo attivando il filtro sui completati."
                    else ->
                        "Per questa giornata non ci sono slot visibili nel filtro attuale."
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun WeeklySlotCard(
    slotUi: WeeklySlotUi,
    onManageClick: () -> Unit
) {
    val mealSections = parseMealSections(slotUi.displayedMealText)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MealTypeBadge(slotUi.mealSlotType)

                StatusBadge(
                    text = slotStatusLabel(slotUi.displayState),
                    containerColor = slotStatusContainerColor(slotUi.displayState),
                    contentColor = slotStatusContentColor(slotUi.displayState)
                )
            }

            if (
                slotUi.reassignedFromDayLabel != null &&
                slotUi.reassignedFromMealSlotLabel != null &&
                !slotUi.isActuallyCompletedThisWeek
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "Questo slot ora contiene il pasto originario di ${slotUi.reassignedFromDayLabel} • ${slotUi.reassignedFromMealSlotLabel}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            if (mealSections.isEmpty()) {
                Text(
                    text = "Nessun pasto pianificato per questo slot.",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                MealTextBlock(
                    sections = mealSections
                )
            }

            FilledTonalButton(
                onClick = onManageClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Gestisci slot")
            }
        }
    }
}

@Composable
private fun MealTextBlock(
    sections: List<List<String>>
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            sections.forEachIndexed { index, section ->
                if (index > 0) {
                    SectionSeparatorBadge()
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    section.forEach { line ->
                        Text(
                            text = "• $line",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionSeparatorBadge() {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = "In aggiunta",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun MealTypeBadge(
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
        shape = RoundedCornerShape(999.dp),
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
private fun StatusBadge(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
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

@Composable
private fun SlotActionDialog(
    dialogUi: SlotActionDialogUi,
    isApplying: Boolean,
    onDismiss: () -> Unit,
    onConsumeAsPlanned: () -> Unit,
    onConsumeReplacement: (sourceSlotId: String) -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isApplying) {
                onDismiss()
            }
        },
        title = {
            Text("${dialogUi.targetDayLabel} • ${dialogUi.targetMealSlotLabel}")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Stato attuale: ${slotStatusLabel(dialogUi.targetDisplayState)}",
                    style = MaterialTheme.typography.bodyMedium
                )

                val targetSections = parseMealSections(dialogUi.currentDisplayedMealText)

                if (targetSections.isEmpty()) {
                    Text(
                        text = "Questo slot non ha un pasto disponibile in questo momento.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "Pasto attualmente assegnato",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )

                    MealTextBlock(
                        sections = targetSections
                    )
                }

                if (
                    dialogUi.reassignedFromDayLabel != null &&
                    dialogUi.reassignedFromMealSlotLabel != null &&
                    !dialogUi.isTargetActuallyCompletedThisWeek
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "Questo slot contiene il pasto originario di ${dialogUi.reassignedFromDayLabel} • ${dialogUi.reassignedFromMealSlotLabel}.",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                if (dialogUi.canConsumeAsPlanned) {
                    Button(
                        onClick = onConsumeAsPlanned,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isApplying
                    ) {
                        Text(
                            if (isApplying) {
                                "Aggiornamento in corso..."
                            } else {
                                "Segna come completato"
                            }
                        )
                    }
                }

                if (!dialogUi.isTargetActuallyCompletedThisWeek) {
                    Text(
                        text = "Usa un pasto compatibile da un altro slot",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )

                    if (dialogUi.replacementOptions.isEmpty()) {
                        Text(
                            text = "Non ci sono altri pasti compatibili e disponibili da assegnare a questo slot.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        dialogUi.replacementOptions.forEach { option ->
                            ReplacementOptionButton(
                                option = option,
                                targetDayLabel = dialogUi.targetDayLabel,
                                targetMealSlotLabel = dialogUi.targetMealSlotLabel,
                                enabled = !isApplying,
                                onClick = { onConsumeReplacement(option.sourceSlotId) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isApplying
            ) {
                Text("Chiudi")
            }
        }
    )
}

@Composable
private fun ReplacementOptionButton(
    option: ReplacementMealOptionUi,
    targetDayLabel: String,
    targetMealSlotLabel: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val sections = parseMealSections(option.mealText)
    val flatLines = sections.flatten()
    val previewLines = flatLines.take(3)
    val hiddenLinesCount = (flatLines.size - previewLines.size).coerceAtLeast(0)
    val extraSectionsCount = (sections.size - 1).coerceAtLeast(0)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = enabled,
                    onClick = onClick
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(
                    text = "Da ${option.sourceDayLabel}",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )

                StatusBadge(
                    text = option.sourceMealSlotLabel,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Applica a $targetDayLabel • $targetMealSlotLabel",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    previewLines.forEach { line ->
                        Text(
                            text = "• $line",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (extraSectionsCount > 0) {
                        Text(
                            text = "Include anche $extraSectionsCount blocchi aggiuntivi",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (hiddenLinesCount > 0) {
                        Text(
                            text = "+ $hiddenLinesCount dettagli",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Text(
                text = if (enabled) "Tocca per usare questo pasto" else "Operazione in corso...",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

private fun slotStatusLabel(
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
private fun slotStatusContainerColor(
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
private fun slotStatusContentColor(
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

private fun parseMealSections(text: String): List<List<String>> {
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