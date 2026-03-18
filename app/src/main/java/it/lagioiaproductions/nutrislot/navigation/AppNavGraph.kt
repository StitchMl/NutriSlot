package it.lagioiaproductions.nutrislot.navigation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import it.lagioiaproductions.nutrislot.ui.calories.CalorieTrackerScreen
import it.lagioiaproductions.nutrislot.ui.importfile.ImportFileUiState
import it.lagioiaproductions.nutrislot.ui.importfile.ImportFileViewModel
import it.lagioiaproductions.nutrislot.ui.importpreview.ImportPreviewScreen
import it.lagioiaproductions.nutrislot.ui.root.AppRootScaffold
import it.lagioiaproductions.nutrislot.ui.root.AppTopLevelDestination
import it.lagioiaproductions.nutrislot.ui.scanner.ScannerScreen
import it.lagioiaproductions.nutrislot.ui.shared.AppBridgeViewModel
import it.lagioiaproductions.nutrislot.ui.shoppinglist.ShoppingListScreen
import it.lagioiaproductions.nutrislot.ui.toolshub.ToolsHubScreen
import it.lagioiaproductions.nutrislot.ui.water.WaterTrackerScreen
import it.lagioiaproductions.nutrislot.ui.weeklyplan.WeeklyPlanScreen
import it.lagioiaproductions.nutrislot.ui.weeklyplan.WeeklyPlanViewModel

private object Routes {
    const val IMPORT_FILE = "import_file"
    const val IMPORT_PREVIEW = "import_preview"
    const val SCANNER = "scanner"
    const val CALORIE_TRACKER = "calorie_tracker"
}

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val importFileViewModel: ImportFileViewModel = viewModel()
    val weeklyPlanViewModel: WeeklyPlanViewModel = viewModel()
    val bridgeViewModel: AppBridgeViewModel = viewModel()

    val importUiState by importFileViewModel.uiState.collectAsState()
    val weeklyPlanUiState by weeklyPlanViewModel.uiState.collectAsState()
    val bridgeUiState by bridgeViewModel.uiState.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(Unit) {
        weeklyPlanViewModel.loadLatestPlan()
    }

    AppRootScaffold(
        currentDestination = currentDestination,
        onDestinationSelected = { destination ->
            navController.navigate(destination.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = AppTopLevelDestination.Planner.route
        ) {
            composable(AppTopLevelDestination.Planner.route) {
                WeeklyPlanScreen(
                    uiState = weeklyPlanUiState,
                    onImportClick = { navController.navigate(Routes.IMPORT_FILE) },
                    onRefreshClick = weeklyPlanViewModel::loadLatestPlan,
                    onOpenSlotAction = weeklyPlanViewModel::openSlotAction,
                    onDismissSlotAction = weeklyPlanViewModel::dismissSlotAction,
                    onConsumeAsPlanned = weeklyPlanViewModel::consumeAsPlanned,
                    onConsumeReplacement = weeklyPlanViewModel::consumeReplacement,
                    onSelectCalendarDay = weeklyPlanViewModel::selectCalendarDay,
                    onToggleConsumedSlotsVisibility = weeklyPlanViewModel::toggleConsumedSlotsVisibility,
                    onAddMealToShopping = bridgeViewModel::addShoppingItemsFromTexts,
                    onAddDayToShopping = bridgeViewModel::addShoppingItemsFromTexts,
                    onAddWeekToShopping = bridgeViewModel::addShoppingItemsFromTexts,
                    shoppingFeedback = bridgeUiState.shoppingFeedback,
                    onConsumeShoppingFeedback = bridgeViewModel::clearShoppingFeedback
                )
            }

            composable(AppTopLevelDestination.Grocery.route) {
                ShoppingListScreen(
                    onOpenScannerClick = {
                        navController.navigate(Routes.SCANNER)
                    },
                    shoppingItems = bridgeUiState.shoppingItems,
                    latestScannedProduct = bridgeUiState.latestScannedProduct,
                    onAddManualItem = bridgeViewModel::addManualShoppingItem,
                    onTogglePurchased = bridgeViewModel::toggleShoppingItemPurchased,
                    onRemoveItem = bridgeViewModel::removeShoppingItem
                )
            }

            composable(AppTopLevelDestination.Water.route) {
                WaterTrackerScreen()
            }

            composable(AppTopLevelDestination.Tools.route) {
                ToolsHubScreen(
                    onOpenScannerClick = {
                        navController.navigate(Routes.SCANNER)
                    },
                    onOpenCalorieClick = {
                        navController.navigate(Routes.CALORIE_TRACKER)
                    }
                )
            }

            composable(Routes.SCANNER) {
                ScannerScreen(
                    onBackClick = { navController.popBackStack() },
                    onAddToShoppingList = { product ->
                        bridgeViewModel.sendProductToShopping(product)
                        navController.navigate(AppTopLevelDestination.Grocery.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onSendToCalorieTracker = { product ->
                        bridgeViewModel.sendProductToCalories(product)
                        navController.navigate(Routes.CALORIE_TRACKER) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Routes.CALORIE_TRACKER) {
                CalorieTrackerScreen(
                    onBackClick = { navController.popBackStack() },
                    onOpenScannerClick = {
                        navController.navigate(Routes.SCANNER) {
                            launchSingleTop = true
                        }
                    },
                    importedProduct = bridgeUiState.pendingCalorieProduct,
                    latestScannedProduct = bridgeUiState.latestScannedProduct,
                    onConsumeImportedProduct = bridgeViewModel::consumePendingCalorieProduct
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
                                route = AppTopLevelDestination.Planner.route,
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
                    Text("Importa piano")
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                        text = "Carica un PDF del piano settimanale",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Il parser è pensato per PDF testuali e strutturati simili a quelli di riferimento. Se il file è molto diverso, servirà revisione manuale.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

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
                        Text("Torna al planner")
                    }
                }
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
                            text = "Import e parsing in corso...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
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
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = fileName,
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
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (uiState.hasEditableDraft) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Anteprima pronta",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = buildDraftSummary(uiState),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider()

                        TextButtonSection(
                            onBackClick = onBackClick
                        )
                    }
                }
            }
        }
    }
}

private fun buildDraftSummary(uiState: ImportFileUiState): String {
    val slotCount = uiState.editableCells.count { it.mealText.isNotBlank() }
    val warningCount = uiState.warnings.size

    return buildString {
        append("Pasti estratti: ")
        append(slotCount)

        if (warningCount > 0) {
            append(" • Warning: ")
            append(warningCount)
        }
    }
}

@Composable
private fun TextButtonSection(
    onBackClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onBackClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Torna indietro")
    }
}