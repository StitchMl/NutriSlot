package it.lagioiaproductions.nutrislot.navigation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import it.lagioiaproductions.nutrislot.domain.model.ImportStatus
import it.lagioiaproductions.nutrislot.ui.importfile.ImportFileUiState
import it.lagioiaproductions.nutrislot.ui.importfile.ImportFileViewModel
import it.lagioiaproductions.nutrislot.ui.importpreview.ImportPreviewScreen
import it.lagioiaproductions.nutrislot.ui.weeklyplan.WeeklyPlanScreen
import it.lagioiaproductions.nutrislot.ui.weeklyplan.WeeklyPlanViewModel

private object Routes {
    const val WEEKLY_PLAN = "weekly_plan"
    const val IMPORT_FILE = "import_file"
    const val IMPORT_PREVIEW = "import_preview"
}

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val importFileViewModel: ImportFileViewModel = viewModel()
    val weeklyPlanViewModel: WeeklyPlanViewModel = viewModel()

    val importUiState by importFileViewModel.uiState.collectAsState()
    val weeklyPlanUiState by weeklyPlanViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        weeklyPlanViewModel.loadLatestPlan()
    }

    NavHost(
        navController = navController,
        startDestination = Routes.WEEKLY_PLAN
    ) {
        composable(Routes.WEEKLY_PLAN) {
            WeeklyPlanScreen(
                uiState = weeklyPlanUiState,
                onImportClick = { navController.navigate(Routes.IMPORT_FILE) },
                onRefreshClick = weeklyPlanViewModel::loadLatestPlan,
                onOpenSlotAction = weeklyPlanViewModel::openSlotAction,
                onDismissSlotAction = weeklyPlanViewModel::dismissSlotAction,
                onConsumeAsPlanned = weeklyPlanViewModel::consumeAsPlanned,
                onConsumeReplacement = weeklyPlanViewModel::consumeReplacement,
                onSelectCalendarDay = weeklyPlanViewModel::selectCalendarDay,
                onToggleConsumedSlotsVisibility = weeklyPlanViewModel::toggleConsumedSlotsVisibility
            )
        }

        composable(Routes.IMPORT_FILE) {
            ImportFileScreen(
                uiState = importUiState,
                onBackClick = { navController.popBackStack() },
                onFileSelected = importFileViewModel::importFromUri,
                onGoToPreviewClick = { navController.navigate(Routes.IMPORT_PREVIEW) }
            )
        }

        composable(Routes.IMPORT_PREVIEW) {
            ImportPreviewScreen(
                uiState = importUiState,
                onMealTextChange = importFileViewModel::updateMealText,
                onClearCellClick = importFileViewModel::clearMealText,
                onBackClick = { navController.popBackStack() },
                onConfirmReviewClick = {
                    importFileViewModel.confirmReviewAndSave {
                        weeklyPlanViewModel.loadLatestPlan()
                        navController.popBackStack(
                            route = Routes.WEEKLY_PLAN,
                            inclusive = false
                        )
                    }
                },
                onTogglePreviewDay = importFileViewModel::togglePreviewDay,
                onToggleShowOnlyFilledSlots = importFileViewModel::toggleShowOnlyFilledSlots
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportFileScreen(
    uiState: ImportFileUiState,
    onBackClick: () -> Unit,
    onFileSelected: (Uri) -> Unit,
    onGoToPreviewClick: () -> Unit
) {
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onFileSelected(uri)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Importa piano da file")
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
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
                        text = "Import intelligente del piano",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Seleziona un PDF simile ai tuoi piani alimentari di riferimento. L’app estrae il testo in locale, costruisce una preview e ti lascia correggere tutto prima della conferma.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Button(
                onClick = { pdfLauncher.launch(arrayOf("application/pdf")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Seleziona file PDF")
            }

            FilledTonalButton(
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Torna alla dieta")
            }

            if (uiState.isLoading) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Parsing in corso...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            uiState.infoMessage?.let { info ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = info,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            uiState.selectedFileName?.let { fileName ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "File selezionato",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            uiState.errorMessage?.let { errorMessage ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Errore import",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (uiState.hasEditableDraft) {
                ImportDraftSummaryCard(uiState = uiState)

                Button(
                    onClick = onGoToPreviewClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apri anteprima e correggi")
                }
            }

            HorizontalDivider()
        }
    }
}

@Composable
private fun ImportDraftSummaryCard(
    uiState: ImportFileUiState
) {
    val statusLabel = when (uiState.importStatus) {
        ImportStatus.SUCCESS -> "Successo"
        ImportStatus.PARTIAL -> "Parziale"
        ImportStatus.UNSUPPORTED -> "Non supportato"
        ImportStatus.FAILED -> "Fallito"
        null -> "N/D"
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Riepilogo parsing",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            uiState.selectedFileName?.let { fileName ->
                Text(
                    text = "File: $fileName",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = "Stato: $statusLabel",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Slot con contenuto: ${uiState.populatedEditableCellsCount} / ${uiState.editableCells.size}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Celle modificate manualmente: ${uiState.editedCellsCount}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Warning: ${uiState.warnings.size}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}