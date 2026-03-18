package it.lagioiaproductions.nutrislot.ui.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    ),
    Tools(
        route = "tools_hub",
        label = "Strumenti",
        emoji = "✨"
    )
}

@Composable
fun AppRootScaffold(
    currentDestination: NavDestination?,
    onDestinationSelected: (AppTopLevelDestination) -> Unit,
    content: @Composable () -> Unit
) {
    val showBottomBar = AppTopLevelDestination.entries
        .any { destination ->
            currentDestination?.route == destination.route
        }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    currentDestination = currentDestination,
                    onDestinationSelected = onDestinationSelected
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            content()
        }
    }
}