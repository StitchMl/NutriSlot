package it.lagioiaproductions.nutrislot.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import it.lagioiaproductions.nutrislot.ui.calories.CalorieTrackerScreen
import it.lagioiaproductions.nutrislot.ui.importfile.ImportFileRoute
import it.lagioiaproductions.nutrislot.ui.importfile.ImportFileViewModel
import it.lagioiaproductions.nutrislot.ui.importpreview.ImportPreviewScreen
import it.lagioiaproductions.nutrislot.ui.root.AppRootScaffold
import it.lagioiaproductions.nutrislot.ui.root.AppTopLevelDestination
import it.lagioiaproductions.nutrislot.ui.scanner.ScannerScreen
import it.lagioiaproductions.nutrislot.ui.shared.AppBridgeViewModel
import it.lagioiaproductions.nutrislot.ui.shoppinglist.ShoppingListRoute
import it.lagioiaproductions.nutrislot.ui.water.WaterTrackerRoute
import it.lagioiaproductions.nutrislot.ui.weeklyplan.WeeklyPlanScreen
import it.lagioiaproductions.nutrislot.ui.weeklyplan.WeeklyPlanViewModel
import it.lagioiaproductions.nutrislot.ui.weeklyplan.WeeklyQuantityChecklistScreen
import it.lagioiaproductions.nutrislot.ui.weight.WeightScreen

private object Routes {
    const val IMPORT_FILE = "import_file"
    const val IMPORT_PREVIEW = "import_preview"
    const val SCANNER = "scanner"
    const val CALORIE_TRACKER = "calorie_tracker"
    const val WEIGHT_TRACKER = "weight_tracker"
    const val WEEKLY_QUANTITY_CHECKLIST = "weekly_quantity_checklist"
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
    val shoppingActions = rememberShoppingListQuickActions()

    WeeklyPlanNavigationEffects(
        isPlannerVisible = currentDestination?.route == AppTopLevelDestination.Planner.route,
        weeklyPlanUiState = weeklyPlanUiState,
        weeklyPlanViewModel = weeklyPlanViewModel,
        bridgeViewModel = bridgeViewModel
    )

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
        },
        onOpenCalorieClick = {
            navController.navigate(Routes.CALORIE_TRACKER) {
                launchSingleTop = true
            }
        },
        onOpenScannerClick = {
            navController.navigate(Routes.SCANNER) {
                launchSingleTop = true
            }
        },
        onOpenWeightClick = {
            navController.navigate(Routes.WEIGHT_TRACKER) {
                launchSingleTop = true
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
                    onOpenSlotEdit = weeklyPlanViewModel::openEditSlot,
                    onToggleSlotCompleted = weeklyPlanViewModel::toggleSlotCompletedFromCalendar,
                    onDismissSlotAction = weeklyPlanViewModel::dismissSlotAction,
                    onConsumeAsPlanned = weeklyPlanViewModel::consumeAsPlanned,
                    onConsumeReplacement = weeklyPlanViewModel::consumeReplacement,
                    onSelectCalendarDay = weeklyPlanViewModel::selectCalendarDay,
                    onToggleConsumedSlotsVisibility = weeklyPlanViewModel::toggleConsumedSlotsVisibility,
                    onAddMealToShopping = shoppingActions.addItems,
                    onAddDayToShopping = shoppingActions.addItems,
                    onAddWeekToShopping = shoppingActions.addItems,
                    onOpenWeeklyQuantityChecklist = {
                        navController.navigate(Routes.WEEKLY_QUANTITY_CHECKLIST) {
                            launchSingleTop = true
                        }
                    },
                    shoppingFeedback = bridgeUiState.shoppingFeedback,
                    onSelectExtraCatalogOption = weeklyPlanViewModel::selectExtraCatalogOption,
                    onUndoCompletedMeal = weeklyPlanViewModel::undoCompletedMeal,
                    onConsumeShoppingFeedback = bridgeViewModel::clearShoppingFeedback,
                    onSaveSlotEdit = weeklyPlanViewModel::saveEditSlot,
                    onDismissSlotEdit = weeklyPlanViewModel::dismissEditSlot,
                    onResetSlotEdit = weeklyPlanViewModel::resetEditSlot
                )
            }

            composable(Routes.WEEKLY_QUANTITY_CHECKLIST) {
                WeeklyQuantityChecklistScreen(
                    items = weeklyPlanUiState.weeklyQuantityChecklist,
                    onBackClick = { navController.popBackStack() },
                    onOpenWaterTracker = {
                        navController.navigate(AppTopLevelDestination.Water.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(AppTopLevelDestination.Grocery.route) {
                ShoppingListRoute(
                    onOpenScannerClick = {
                        navController.navigate(Routes.SCANNER)
                    },
                    latestScannedProduct = bridgeUiState.latestScannedProduct
                )
            }

            composable(AppTopLevelDestination.Water.route) {
                WaterTrackerRoute()
            }

            composable(Routes.SCANNER) {
                ScannerScreen(
                    onBackClick = { navController.popBackStack() },
                    onAddToShoppingList = { product ->
                        shoppingActions.addItem(product.name)
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
                    calorieJournalByDate = bridgeUiState.calorieJournalByDate,
                    importedProduct = bridgeUiState.pendingCalorieProduct,
                    latestScannedProduct = bridgeUiState.latestScannedProduct,
                    onConsumeImportedProductForDay = bridgeViewModel::consumePendingCalorieProductForDay,
                    onUpdateGoalForDay = bridgeViewModel::setCalorieGoalForDay,
                    onDeleteEntry = bridgeViewModel::removeCalorieEntry,
                    onResetDay = bridgeViewModel::resetCalorieDay
                )
            }

            composable(Routes.WEIGHT_TRACKER) {
                WeightScreen(
                    onBackClick = { navController.popBackStack() },
                    entries = bridgeUiState.weightEntries,
                    summary = bridgeUiState.weightSummary,
                    onAddWeightEntry = bridgeViewModel::addWeightEntry,
                    onDeleteWeightEntry = bridgeViewModel::removeWeightEntry
                )
            }

            composable(Routes.IMPORT_FILE) {
                ImportFileRoute(
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
                        importFileViewModel.confirmReviewAndSave(
                            onSaved = {
                                weeklyPlanViewModel.loadLatestPlan()
                                navController.popBackStack(
                                    route = AppTopLevelDestination.Planner.route,
                                    inclusive = false
                                )
                            }
                        )
                    },
                    onTogglePreviewDay = importFileViewModel::togglePreviewDay,
                    onToggleShowOnlyFilledSlots = importFileViewModel::toggleShowOnlyFilledSlots
                )
            }
        }
    }
}
