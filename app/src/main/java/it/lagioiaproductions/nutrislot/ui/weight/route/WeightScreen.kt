package it.lagioiaproductions.nutrislot.ui.weight.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.ui.shared.WeightEntryUi
import it.lagioiaproductions.nutrislot.ui.shared.WeightSummaryUi
import it.lagioiaproductions.nutrislot.ui.weight.components.WeightSummaryCard
import it.lagioiaproductions.nutrislot.ui.weight.components.WeightTrendCard
import it.lagioiaproductions.nutrislot.ui.weight.support.buildRecentDateOptions
import it.lagioiaproductions.nutrislot.ui.weight.components.WeightEntryFormCard
import it.lagioiaproductions.nutrislot.ui.weight.components.weightHistorySection
import it.lagioiaproductions.nutrislot.ui.weight.support.parseWeightInput
import it.lagioiaproductions.nutrislot.ui.weight.support.todayDateKey
import it.lagioiaproductions.nutrislot.ui.weight.support.validateWeightInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(
    onBackClick: () -> Unit,
    entries: List<WeightEntryUi>,
    summary: WeightSummaryUi,
    onAddWeightEntry: (Float, String, String) -> Unit,
    onDeleteWeightEntry: (Long) -> Unit
) {
    var weightInput by rememberSaveable { mutableStateOf("") }
    var noteInput by rememberSaveable { mutableStateOf("") }
    var selectedDateKey by rememberSaveable { mutableStateOf(todayDateKey()) }
    var inputError by rememberSaveable { mutableStateOf<String?>(null) }

    val recentDates = remember { buildRecentDateOptions() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Peso") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                WeightSummaryCard(
                    summary = summary,
                    entries = entries,
                    totalEntries = entries.size
                )
            }

            item {
                WeightTrendCard(entries = entries)
            }

            item {
                WeightEntryFormCard(
                    recentDates = recentDates,
                    selectedDateKey = selectedDateKey,
                    weightInput = weightInput,
                    noteInput = noteInput,
                    inputError = inputError,
                    onDateSelected = { selectedDateKey = it },
                    onWeightInputChange = {
                        weightInput = it
                        inputError = null
                    },
                    onNoteInputChange = { noteInput = it },
                    onSaveClick = {
                        val validationError = validateWeightInput(weightInput)
                        if (validationError != null) {
                            inputError = validationError
                        } else {
                            onAddWeightEntry(
                                parseWeightInput(weightInput),
                                selectedDateKey,
                                noteInput
                            )
                            weightInput = ""
                            noteInput = ""
                            inputError = null
                        }
                    }
                )
            }

            weightHistorySection(
                entries = entries,
                onDeleteWeightEntry = onDeleteWeightEntry
            )
        }
    }
}
