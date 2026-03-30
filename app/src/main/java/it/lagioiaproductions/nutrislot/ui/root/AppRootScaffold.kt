package it.lagioiaproductions.nutrislot.ui.root

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination

enum class AppTopLevelDestination(
    val route: String,
    val label: String,
    val emoji: String
) {
    Planner(
        route = "weekly_plan",
        label = "Piano",
        emoji = "🍽"
    ),
    Grocery(
        route = "shopping_list",
        label = "Spesa",
        emoji = "🛒"
    ),
    Water(
        route = "water_tracker",
        label = "Acqua",
        emoji = "💧"
    )
}

enum class AppQuickToolDestination(
    val label: String,
    val emoji: String
) {
    Calories(
        label = "Conta calorie",
        emoji = "🔥"
    ),
    Scanner(
        label = "Scanner",
        emoji = "📷"
    ),
    Weight(
        label = "Peso",
        emoji = "⚖️"
    )
}

@Suppress("AssignedValueIsNeverRead")
@Composable
fun AppRootScaffold(
    currentDestination: NavDestination?,
    onDestinationSelected: (AppTopLevelDestination) -> Unit,
    onOpenCalorieClick: () -> Unit = {},
    onOpenScannerClick: () -> Unit = {},
    onOpenWeightClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    var isQuickMenuExpanded by rememberSaveable { mutableStateOf(false) }

    val showBottomBar = AppTopLevelDestination.entries.any { destination ->
        currentDestination?.route == destination.route
    }

    val isPlannerScreen =
        currentDestination?.route == AppTopLevelDestination.Planner.route

    BackHandler(enabled = isQuickMenuExpanded) {
        isQuickMenuExpanded = false
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    currentDestination = currentDestination,
                    onDestinationSelected = { destination ->
                        isQuickMenuExpanded = false
                        onDestinationSelected(destination)
                    }
                )
            }
        },
        floatingActionButton = {
            if (showBottomBar) {
                QuickToolsFabMenu(
                    expanded = isQuickMenuExpanded,
                    onExpandedChange = { isQuickMenuExpanded = it },
                    onQuickToolSelected = { destination ->
                        isQuickMenuExpanded = false
                        when (destination) {
                            AppQuickToolDestination.Calories -> onOpenCalorieClick()
                            AppQuickToolDestination.Scanner -> onOpenScannerClick()
                            AppQuickToolDestination.Weight -> onOpenWeightClick()
                        }
                    }
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = if (isPlannerScreen) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                }
            ) {
                content()
            }

            AnimatedVisibility(
                visible = isQuickMenuExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.34f))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            isQuickMenuExpanded = false
                        }
                )
            }
        }
    }
}

@Composable
private fun QuickToolsFabMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onQuickToolSelected: (AppQuickToolDestination) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 })
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppQuickToolDestination.entries.forEach { destination ->
                    QuickToolAction(
                        destination = destination,
                        onClick = { onQuickToolSelected(destination) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { onExpandedChange(!expanded) },
            containerColor = if (expanded) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            contentColor = if (expanded) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            }
        ) {
            Text(
                text = if (expanded) "✕" else "＋",
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
private fun QuickToolAction(
    destination: AppQuickToolDestination,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp
        ) {
            Text(
                text = destination.label,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge
            )
        }

        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Text(
                text = destination.emoji,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}