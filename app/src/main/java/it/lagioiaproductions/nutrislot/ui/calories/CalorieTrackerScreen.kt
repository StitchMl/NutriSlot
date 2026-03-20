@file:Suppress("AssignedValueIsNeverRead")

package it.lagioiaproductions.nutrislot.ui.calories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.ui.shared.CalorieDayLogUi
import it.lagioiaproductions.nutrislot.ui.shared.CalorieJournalEntryUi
import it.lagioiaproductions.nutrislot.ui.shared.CalorieJournalSection
import it.lagioiaproductions.nutrislot.ui.shared.LinkedScannedProductUi
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalorieTrackerScreen(
    onBackClick: () -> Unit,
    onOpenScannerClick: () -> Unit,
    calorieJournalByDate: Map<String, CalorieDayLogUi>,
    importedProduct: LinkedScannedProductUi?,
    latestScannedProduct: LinkedScannedProductUi?,
    onConsumeImportedProductForDay: (String) -> Unit,
    onUpdateGoalForDay: (String, Int?) -> Unit,
    onDeleteEntry: (String, Long) -> Unit,
    onResetDay: (String) -> Unit
) {
    var selectedDayOffset by remember { mutableIntStateOf(0) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var goalInput by remember { mutableStateOf("") }

    val currentDayKey = remember(selectedDayOffset) {
        dayKeyForOffset(selectedDayOffset)
    }

    val dayLog = calorieJournalByDate[currentDayKey] ?: CalorieDayLogUi()
    val entries = dayLog.entries.sortedByDescending { it.id }

    val calories = entries.sumOf { it.calories }
    val protein = entries.sumOf { it.protein }
    val carbs = entries.sumOf { it.carbs }
    val fibre = entries.sumOf { it.fibre }

    val goalKcal = dayLog.goalKcal
    val progress = if (goalKcal != null && goalKcal > 0) {
        (calories.toFloat() / goalKcal.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val progressPercent = (progress * 100f).roundToInt()

    LaunchedEffect(importedProduct?.barcode, importedProduct?.name, currentDayKey) {
        if (importedProduct != null) {
            onConsumeImportedProductForDay(currentDayKey)
        }
    }

    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = {
                Text("Imposta goal kcal")
            },
            text = {
                OutlinedTextField(
                    value = goalInput,
                    onValueChange = { value ->
                        goalInput = value.filter { it.isDigit() }
                    },
                    singleLine = true,
                    label = { Text("Kcal target") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateGoalForDay(
                            currentDayKey,
                            goalInput.toIntOrNull()
                        )
                        showGoalDialog = false
                    }
                ) {
                    Text("Salva")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showGoalDialog = false }
                ) {
                    Text("Annulla")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Journal") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenScannerClick) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Aggiungi da scanner"
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DateCard(
                title = longDateForOffset(selectedDayOffset),
                subtitle = shortWeekdayForOffset(selectedDayOffset),
                onPrevious = { selectedDayOffset-- },
                onNext = { selectedDayOffset++ }
            )

            SummaryCard(
                calories = calories,
                goalKcal = goalKcal,
                progress = progress,
                progressPercent = progressPercent,
                protein = protein,
                carbs = carbs,
                fibre = fibre,
                onEditGoal = {
                    goalInput = goalKcal?.toString().orEmpty()
                    showGoalDialog = true
                }
            )

            latestScannedProduct?.let { product ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                ) {
                    Text(
                        text = "Ultimo scanner: ${product.name}",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenScannerClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log food")
                }

                FilledTonalButton(
                    onClick = { onResetDay(currentDayKey) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset giorno")
                }
            }

            if (entries.isEmpty()) {
                EmptyJournalCard()
            } else {
                CalorieJournalSection.entries.forEach { section ->
                    val sectionEntries = entries.filter { it.section == section }
                    if (sectionEntries.isNotEmpty()) {
                        SectionBlock(
                            section = section,
                            entries = sectionEntries,
                            dayKey = currentDayKey,
                            onDeleteEntry = onDeleteEntry
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DateCard(
    title: String,
    subtitle: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Precedente",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Successivo",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    calories: Int,
    goalKcal: Int?,
    progress: Float,
    progressPercent: Int,
    protein: Int,
    carbs: Int,
    fibre: Int,
    onEditGoal: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Calories",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = calories.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (goalKcal != null) {
                            "di $goalKcal kcal"
                        } else {
                            "Goal non impostato"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TextButton(
                        onClick = onEditGoal,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (goalKcal == null) "Imposta goal" else "Modifica goal"
                        )
                    }
                }

                Box(
                    modifier = Modifier.size(96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    strokeWidth = 10.dp,
                    trackColor = ProgressIndicatorDefaults.circularIndeterminateTrackColor,
                    strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
                    )
                    CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 10.dp,
                    trackColor = ProgressIndicatorDefaults.circularIndeterminateTrackColor,
                    strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
                    )
                    Text(
                        text = if (goalKcal == null) "--" else "$progressPercent%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroValue("Proteine", protein, "g", Color(0xFF2E90FA))
                MacroValue("Carbo", carbs, "g", Color(0xFF22C55E))
                MacroValue("Fibre", fibre, "g", MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun MacroValue(
    label: String,
    value: Int,
    unit: String,
    accent: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = accent
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionBlock(
    section: CalorieJournalSection,
    entries: List<CalorieJournalEntryUi>,
    dayKey: String,
    onDeleteEntry: (String, Long) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ) {
            Text(
                text = section.label,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        entries.forEach { entry ->
            JournalMealCard(
                entry = entry,
                onDelete = { onDeleteEntry(dayKey, entry.id) }
            )
        }
    }
}

@Composable
private fun JournalMealCard(
    entry: CalorieJournalEntryUi,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.section.emoji,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (entry.subtitle.isNotBlank()) {
                    Text(
                        text = entry.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NutrientChip("${entry.calories} kcal")
                    NutrientChip("${entry.protein}g pro")
                    NutrientChip("${entry.carbs}g carb")
                    if (entry.fibre > 0) {
                        NutrientChip("${entry.fibre}g fib")
                    }
                }

                ElevatedAssistChip(
                    onClick = {},
                    label = {
                        Text(
                            "${entry.sourceLabel} • ${entry.timeLabel}"
                        )
                    }
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Elimina",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun NutrientChip(
    text: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EmptyJournalCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Nessun pasto registrato",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Questa giornata è vuota. Aggiungi un alimento dallo scanner oppure registra pasti dal planner.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun calendarForOffset(offset: Int): Calendar {
    return Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, offset)
    }
}

private fun dayKeyForOffset(offset: Int): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        .format(calendarForOffset(offset).time)
}

private fun longDateForOffset(offset: Int): String {
    return SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        .format(calendarForOffset(offset).time)
}

private fun shortWeekdayForOffset(offset: Int): String {
    return SimpleDateFormat("EEE", Locale.getDefault())
        .format(calendarForOffset(offset).time)
}