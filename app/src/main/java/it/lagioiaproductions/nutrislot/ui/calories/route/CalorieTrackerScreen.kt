package it.lagioiaproductions.nutrislot.ui.calories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.ui.shared.CalorieDayLogUi
import it.lagioiaproductions.nutrislot.ui.shared.LinkedScannedProductUi

@Suppress("AssignedValueIsNeverRead")
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

    val dayUi = buildCalorieTrackerDayUi(
        selectedDayOffset = selectedDayOffset,
        calorieJournalByDate = calorieJournalByDate
    )

    LaunchedEffect(importedProduct?.barcode, importedProduct?.name, dayUi.dayKey) {
        if (importedProduct != null) {
            onConsumeImportedProductForDay(dayUi.dayKey)
        }
    }

    if (showGoalDialog) {
        CalorieGoalDialog(
            goalInput = goalInput,
            onGoalInputChange = { value ->
                goalInput = value.filter { it.isDigit() }
            },
            onConfirm = {
                onUpdateGoalForDay(dayUi.dayKey, goalInput.toIntOrNull())
                showGoalDialog = false
            },
            onDismiss = { showGoalDialog = false }
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
            CalorieDateCard(
                title = dayUi.dateTitle,
                subtitle = dayUi.weekdayLabel,
                onPrevious = { selectedDayOffset-- },
                onNext = { selectedDayOffset++ }
            )

            CalorieSummaryCard(
                dayUi = dayUi,
                onEditGoal = {
                    goalInput = dayUi.goalKcal?.toString().orEmpty()
                    showGoalDialog = true
                }
            )

            latestScannedProduct?.let { product ->
                LatestScannedProductBanner(product = product)
            }

            CalorieTrackerActionRow(
                onOpenScannerClick = onOpenScannerClick,
                onResetDayClick = { onResetDay(dayUi.dayKey) }
            )

            CalorieJournalContent(
                sectionEntries = dayUi.sectionEntries,
                dayKey = dayUi.dayKey,
                onDeleteEntry = onDeleteEntry
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
