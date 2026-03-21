@file:Suppress("SameParameterValue")

package it.lagioiaproductions.nutrislot.ui.importpreview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.domain.model.CellRecognitionState
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.ui.importfile.EditableImportedMealCellUi
import it.lagioiaproductions.nutrislot.ui.importfile.ImportFileUiState
import it.lagioiaproductions.nutrislot.ui.weeklyplan.parseMealSectionVisuals

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPreviewScreen(
    uiState: ImportFileUiState,
    onMealTextChange: (String, String) -> Unit,
    onClearCellClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onConfirmReviewClick: () -> Unit,
    onTogglePreviewDay: (WeekDay) -> Unit,
    onToggleShowOnlyFilledSlots: () -> Unit
) {
    val cells = uiState.editableCells
        .sortedWith(compareBy({ it.dayOfWeek.sortOrder }, { it.mealSlotType.sortOrder }))

    val filledCount = cells.count { it.mealText.isNotBlank() }
    val suspectedCount = cells.count { it.originalRecognitionState == CellRecognitionState.SUSPECTED }
    val editedCount = cells.count { it.wasManuallyEdited }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Anteprima import")
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = onBackClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Indietro")
                    }

                    Button(
                        onClick = onConfirmReviewClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Conferma")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Riepilogo revisione",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Slot compilati: $filledCount • Celle dubbie: $suspectedCount • Celle modificate: $editedCount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (suspectedCount > 0) {
                        RecognitionBanner(
                            text = "Le celle evidenziate come dubbie vanno controllate con più attenzione prima del salvataggio.",
                            recognitionState = CellRecognitionState.SUSPECTED
                        )
                    }

                    if (uiState.warnings.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.warnings.forEach { warning ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = warning.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            WeekDay.orderedValues().forEach { day ->
                val dayCells = cells.filter { it.dayOfWeek == day }
                if (dayCells.isEmpty()) return@forEach

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = day.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        dayCells.forEachIndexed { index, cell ->
                            PreviewMealCellCard(
                                cell = cell,
                                onMealTextChange = onMealTextChange,
                                onClearCellClick = onClearCellClick
                            )

                            if (index != dayCells.lastIndex) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }

            TextButton(
                onClick = onToggleShowOnlyFilledSlots,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Aggiorna filtro slot compilati")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeekDay.orderedValues().take(3).forEach { day ->
                    OutlinedButton(
                        onClick = { onTogglePreviewDay(day) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(day.displayName.take(3))
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewMealCellCard(
    cell: EditableImportedMealCellUi,
    onMealTextChange: (String, String) -> Unit,
    onClearCellClick: (String) -> Unit
) {
    val parsedSections = parseMealSectionVisuals(cell.mealText)
    val recognitionState = cell.originalRecognitionState

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = cell.mealSlotType.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RecognitionBadge(recognitionState = recognitionState)

                    if (cell.wasManuallyEdited) {
                        SmallTag(
                            text = "Modificato",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            TextButton(
                onClick = { onClearCellClick(cell.id) }
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Svuota")
            }
        }

        if (parsedSections.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                parsedSections.forEach { section ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${section.visualInfo.emoji} ${section.lines.firstOrNull().orEmpty()}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )

                            section.lines.drop(1).forEach { line ->
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = cell.mealText,
            onValueChange = { onMealTextChange(cell.id, it) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            label = {
                Text("Testo pasto")
            },
            supportingText = {
                Text(
                    text = when (recognitionState) {
                        CellRecognitionState.SUSPECTED ->
                            "Questa cella sembra estratta in modo dubbio. Controlla bene testo e alternative."
                        CellRecognitionState.EMPTY ->
                            "Slot vuoto."
                        CellRecognitionState.MISSING_DAY ->
                            "Giorno non riconosciuto in origine."
                        CellRecognitionState.MISSING_MEAL_SLOT ->
                            "Slot pasto non riconosciuto in origine."
                        CellRecognitionState.RECOGNIZED ->
                            "Parsing riconosciuto."
                    }
                )
            }
        )
    }
}

@Composable
private fun RecognitionBanner(
    text: String,
    recognitionState: CellRecognitionState
) {
    val colors = recognitionColors(recognitionState)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.container
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = colors.content,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = colors.content
            )
        }
    }
}

@Composable
private fun RecognitionBadge(
    recognitionState: CellRecognitionState
) {
    val colors = recognitionColors(recognitionState)

    SmallTag(
        text = recognitionState.toUiLabel(),
        containerColor = colors.container,
        contentColor = colors.content
    )
}

@Composable
private fun SmallTag(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(contentColor.copy(alpha = 0.85f))
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor
        )
    }
}

@Composable
private fun recognitionColors(
    recognitionState: CellRecognitionState
): RecognitionColors {
    return when (recognitionState) {
        CellRecognitionState.SUSPECTED -> RecognitionColors(
            container = MaterialTheme.colorScheme.tertiaryContainer,
            content = MaterialTheme.colorScheme.onTertiaryContainer
        )
        CellRecognitionState.RECOGNIZED -> RecognitionColors(
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer
        )
        CellRecognitionState.EMPTY -> RecognitionColors(
            container = MaterialTheme.colorScheme.surfaceVariant,
            content = MaterialTheme.colorScheme.onSurfaceVariant
        )
        CellRecognitionState.MISSING_DAY,
        CellRecognitionState.MISSING_MEAL_SLOT -> RecognitionColors(
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

private fun CellRecognitionState.toUiLabel(): String {
    return when (this) {
        CellRecognitionState.RECOGNIZED -> "Riconosciuto"
        CellRecognitionState.SUSPECTED -> "Dubbio"
        CellRecognitionState.MISSING_DAY -> "Giorno mancante"
        CellRecognitionState.MISSING_MEAL_SLOT -> "Slot mancante"
        CellRecognitionState.EMPTY -> "Vuoto"
    }
}

private data class RecognitionColors(
    val container: androidx.compose.ui.graphics.Color,
    val content: androidx.compose.ui.graphics.Color
)