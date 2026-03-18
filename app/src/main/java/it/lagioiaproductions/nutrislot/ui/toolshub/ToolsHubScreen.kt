package it.lagioiaproductions.nutrislot.ui.toolshub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsHubScreen(
    onOpenScannerClick: () -> Unit,
    onOpenCalorieClick: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Strumenti") }
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Area strumenti",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Da qui entri nello scanner prodotti e nel diario calorie. In seguito questa sezione potrà ospitare anche ricerca alimento e cronologia.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ToolsBadge(text = "Scanner")
                        ToolsBadge(text = "Calorie")
                        ToolsBadge(
                            text = "Locale",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            ToolCard(
                title = "Scanner",
                subtitle = "Barcode e ricerca manuale",
                description = "Seleziona un prodotto e indirizzalo verso lista spesa o conta calorie.",
                primaryActionLabel = "Apri scanner",
                secondaryActionLabel = "Vai al conta calorie",
                onPrimaryClick = onOpenScannerClick,
                onSecondaryClick = onOpenCalorieClick
            )

            ToolCard(
                title = "Conta Calorie",
                subtitle = "Diario alimentare e macro",
                description = "Monitora calorie, proteine, carboidrati e fibre della giornata.",
                primaryActionLabel = "Apri conta calorie",
                secondaryActionLabel = "Vai allo scanner",
                onPrimaryClick = onOpenCalorieClick,
                onSecondaryClick = onOpenScannerClick
            )
        }
    }
}

@Composable
private fun ToolCard(
    title: String,
    subtitle: String,
    description: String,
    primaryActionLabel: String,
    secondaryActionLabel: String,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )

            Button(
                onClick = onPrimaryClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(primaryActionLabel)
            }

            FilledTonalButton(
                onClick = onSecondaryClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(secondaryActionLabel)
            }
        }
    }
}

@Composable
private fun ToolsBadge(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}