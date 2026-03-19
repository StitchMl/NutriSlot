@file:Suppress("SameParameterValue")

package it.lagioiaproductions.nutrislot.ui.calories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.ui.shared.LinkedScannedProductUi
import kotlin.math.roundToInt

private data class JournalMealEntryUi(
    val id: Int,
    val section: JournalSection,
    val title: String,
    val subtitle: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fibre: Int,
    val timeLabel: String
)

private enum class JournalSection(
    val label: String,
    val emoji: String,
    val defaultTime: String
) {
    BREAKFAST("Colazione", "☀️", "08:00"),
    LUNCH("Pranzo", "🥗", "13:00"),
    DINNER("Cena", "🍽️", "20:00"),
    SNACK("Snack", "🍎", "16:30")
}

private enum class JournalFilter(
    val label: String,
    val section: JournalSection?
) {
    ALL("Tutti", null),
    BREAKFAST("Colazione", JournalSection.BREAKFAST),
    LUNCH("Pranzo", JournalSection.LUNCH),
    DINNER("Cena", JournalSection.DINNER),
    SNACK("Snack", JournalSection.SNACK)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalorieTrackerScreen(
    onBackClick: () -> Unit,
    onOpenScannerClick: () -> Unit,
    importedProduct: LinkedScannedProductUi?,
    latestScannedProduct: LinkedScannedProductUi?,
    onConsumeImportedProduct: () -> Unit
) {
    val caloriesTarget = 2092
    val proteinsTarget = 120
    val carbsTarget = 200
    val fibreTarget = 25

    var selectedDayOffset by remember { mutableIntStateOf(0) }
    var selectedFilter by remember { mutableStateOf(JournalFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var nextId by remember { mutableIntStateOf(1000) }

    val journalEntries = remember {
        mutableStateListOf<JournalMealEntryUi>().apply {
            addAll(seedJournalEntries())
        }
    }

    fun addEntry(
        title: String,
        subtitle: String,
        section: JournalSection,
        calories: Int,
        protein: Int,
        carbs: Int,
        fibre: Int,
        timeLabel: String = section.defaultTime
    ) {
        journalEntries.add(
            0,
            JournalMealEntryUi(
                id = nextId++,
                section = section,
                title = title,
                subtitle = subtitle,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fibre = fibre,
                timeLabel = timeLabel
            )
        )
    }

    fun resetDay() {
        journalEntries.clear()
    }

    LaunchedEffect(importedProduct?.barcode, importedProduct?.name) {
        importedProduct?.let { product ->
            val inferredSection = inferSectionFromProduct(product)
            addEntry(
                title = product.name,
                subtitle = product.subtitle,
                section = inferredSection,
                calories = product.calories,
                protein = product.protein,
                carbs = product.carbs,
                fibre = product.fibre,
                timeLabel = inferredSection.defaultTime
            )
            onConsumeImportedProduct()
        }
    }

    val calories = journalEntries.sumOf { it.calories }
    val proteins = journalEntries.sumOf { it.protein }
    val carbs = journalEntries.sumOf { it.carbs }
    val fibre = journalEntries.sumOf { it.fibre }

    val progress = (calories.toFloat() / caloriesTarget.toFloat()).coerceIn(0f, 1f)
    val progressPercent = (progress * 100f).roundToInt()
    val remainingCalories = (caloriesTarget - calories).coerceAtLeast(0)

    val filteredEntries = journalEntries.filter { entry ->
        val matchesFilter = selectedFilter.section == null || entry.section == selectedFilter.section
        val matchesSearch = searchQuery.isBlank() ||
                entry.title.contains(searchQuery, ignoreCase = true) ||
                entry.subtitle.contains(searchQuery, ignoreCase = true)
        matchesFilter && matchesSearch
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TopJournalBar(
                onBackClick = onBackClick,
                onOpenScannerClick = onOpenScannerClick
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Journal",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Diario pasti e riepilogo nutrizionale",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DaySelectorCard(
                dayTitle = dayTitleForOffset(selectedDayOffset),
                daySubtitle = daySubtitleForOffset(selectedDayOffset),
                onPrevious = { selectedDayOffset-- },
                onNext = { selectedDayOffset++ }
            )

            SummaryCard(
                calories = calories,
                target = caloriesTarget,
                progress = progress,
                progressPercent = progressPercent,
                protein = proteins,
                proteinTarget = proteinsTarget,
                carbs = carbs,
                carbsTarget = carbsTarget,
                fibre = fibre,
                fibreTarget = fibreTarget
            )

            latestScannedProduct?.let { product ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "Ultimo prodotto scannerizzato: ${product.name}",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            SearchAndFilterBlock(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            if (journalEntries.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Rimanenti",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "$remainingCalories kcal",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Text(
                            text = if (calories >= caloriesTarget) "Goal raggiunto" else "Obiettivo in corso",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            JournalSection.entries.forEach { section ->
                val entriesForSection = filteredEntries.filter { it.section == section }
                if (entriesForSection.isNotEmpty()) {
                    JournalSectionBlock(
                        section = section,
                        entries = entriesForSection,
                        onDeleteEntry = { entryId ->
                            journalEntries.removeAll { it.id == entryId }
                        }
                    )
                }
            }

            if (filteredEntries.isEmpty()) {
                EmptyJournalState(
                    searchQuery = searchQuery,
                    onOpenScannerClick = onOpenScannerClick,
                    onResetDay = ::resetDay
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TopJournalBar(
    onBackClick: () -> Unit,
    onOpenScannerClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundIconAction(
            onClick = onBackClick,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Indietro"
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoundIconAction(
                        onClick = onOpenScannerClick,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Aggiungi alimento"
                        )
                    }

                    Text(
                        text = "Log food",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun RoundIconAction(
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.size(46.dp),
        shape = CircleShape,
        color = containerColor,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            IconButton(onClick = onClick) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides contentColor
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun DaySelectorCard(
    dayTitle: String,
    daySubtitle: String,
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
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Giorno precedente",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dayTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = daySubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Giorno successivo",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    calories: Int,
    target: Int,
    progress: Float,
    progressPercent: Int,
    protein: Int,
    proteinTarget: Int,
    carbs: Int,
    carbsTarget: Int,
    fibre: Int,
    fibreTarget: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
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
                        text = "di $target kcal",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ProgressRing(
                    progress = progress,
                    label = "$progressPercent%"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroSummaryItem(
                    label = "Proteine",
                    value = protein,
                    target = proteinTarget,
                    unit = "g",
                    accent = Color(0xFF2E90FA)
                )
                MacroSummaryItem(
                    label = "Carbo",
                    value = carbs,
                    target = carbsTarget,
                    unit = "g",
                    accent = Color(0xFF22C55E)
                )
                MacroSummaryItem(
                    label = "Fibre",
                    value = fibre,
                    target = fibreTarget,
                    unit = "g",
                    accent = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ProgressRing(
    progress: Float,
    label: String
) {
    Box(
        modifier = Modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 10.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            trackColor = Color.Transparent
        )
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 10.dp,
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Transparent
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun MacroSummaryItem(
    label: String,
    value: Int,
    target: Int,
    unit: String,
    accent: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$value",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = accent
        )
        Text(
            text = "di $target $unit",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchAndFilterBlock(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: JournalFilter,
    onFilterSelected: (JournalFilter) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            placeholder = {
                Text("Cerca pasti...")
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            )
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            JournalFilter.entries.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = {
                        Text(filter.label)
                    }
                )
            }
        }
    }
}

@Composable
private fun JournalSectionBlock(
    section: JournalSection,
    entries: List<JournalMealEntryUi>,
    onDeleteEntry: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ) {
            Text(
                text = section.label,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            entries.forEach { entry ->
                JournalMealCard(
                    entry = entry,
                    onDelete = { onDeleteEntry(entry.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JournalMealCard(
    entry: JournalMealEntryUi,
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
                    .size(54.dp)
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

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NutrientChip(
                        text = "${entry.calories} kcal",
                        background = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        content = MaterialTheme.colorScheme.primary
                    )
                    NutrientChip(
                        text = "${entry.protein}g proteine",
                        background = Color(0xFF2E90FA).copy(alpha = 0.12f),
                        content = Color(0xFF2E90FA)
                    )
                    NutrientChip(
                        text = "${entry.carbs}g carbo",
                        background = Color(0xFF22C55E).copy(alpha = 0.12f),
                        content = Color(0xFF22C55E)
                    )
                    if (entry.fibre > 0) {
                        NutrientChip(
                            text = "${entry.fibre}g fibre",
                            background = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                            content = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Text(
                    text = entry.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = entry.timeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifica",
                            tint = MaterialTheme.colorScheme.primary
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
    }
}

@Composable
private fun NutrientChip(
    text: String,
    background: Color,
    content: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = background
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = content
        )
    }
}

@Composable
private fun EmptyJournalState(
    searchQuery: String,
    onOpenScannerClick: () -> Unit,
    onResetDay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (searchQuery.isBlank()) {
                    "Nessun pasto registrato"
                } else {
                    "Nessun risultato trovato"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (searchQuery.isBlank()) {
                    "Aggiungi il primo alimento dallo scanner per iniziare il diario."
                } else {
                    "Prova a cambiare ricerca o filtro."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = onOpenScannerClick,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "Apri scanner",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Surface(
                    onClick = onResetDay,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "Reset",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

private fun seedJournalEntries(): List<JournalMealEntryUi> {
    return listOf(
        JournalMealEntryUi(
            id = 1,
            section = JournalSection.BREAKFAST,
            title = "Avocado toast",
            subtitle = "Pane integrale, avocado",
            calories = 280,
            protein = 8,
            carbs = 32,
            fibre = 6,
            timeLabel = "08:00"
        ),
        JournalMealEntryUi(
            id = 2,
            section = JournalSection.LUNCH,
            title = "Turkey sandwich",
            subtitle = "Tacchino, pane integrale",
            calories = 380,
            protein = 25,
            carbs = 45,
            fibre = 5,
            timeLabel = "12:00"
        ),
        JournalMealEntryUi(
            id = 3,
            section = JournalSection.DINNER,
            title = "Pasta con verdure",
            subtitle = "Pasta, zucchine, olio EVO",
            calories = 520,
            protein = 18,
            carbs = 64,
            fibre = 7,
            timeLabel = "20:15"
        )
    )
}

private fun inferSectionFromProduct(product: LinkedScannedProductUi): JournalSection {
    val source = "${product.name} ${product.subtitle}".lowercase()

    return when {
        listOf("colazione", "breakfast").any { source.contains(it) } -> JournalSection.BREAKFAST
        listOf("pranzo", "lunch").any { source.contains(it) } -> JournalSection.LUNCH
        listOf("cena", "dinner").any { source.contains(it) } -> JournalSection.DINNER
        else -> JournalSection.SNACK
    }
}

private fun dayTitleForOffset(offset: Int): String {
    return when (offset) {
        0 -> "Oggi"
        -1 -> "Ieri"
        1 -> "Domani"
        else -> if (offset > 0) "Tra $offset giorni" else "${-offset} giorni fa"
    }
}

private fun daySubtitleForOffset(offset: Int): String {
    return when (offset) {
        0 -> "Diario giornaliero"
        -1 -> "Giorno precedente"
        1 -> "Giorno successivo"
        else -> "Selezione rapida"
    }
}