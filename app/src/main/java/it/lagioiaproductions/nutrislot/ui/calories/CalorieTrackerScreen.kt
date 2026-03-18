package it.lagioiaproductions.nutrislot.ui.calories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.ui.shared.LinkedScannedProductUi
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalorieTrackerScreen(
    onBackClick: () -> Unit,
    onOpenScannerClick: () -> Unit,
    importedProduct: LinkedScannedProductUi?,
    latestScannedProduct: LinkedScannedProductUi?,
    onConsumeImportedProduct: () -> Unit
) {
    val caloriesTarget = 2000
    val proteinsTarget = 120
    val carbsTarget = 200
    val fibreTarget = 25

    var calories by remember { mutableIntStateOf(0) }
    var proteins by remember { mutableIntStateOf(0) }
    var carbs by remember { mutableIntStateOf(0) }
    var fibre by remember { mutableIntStateOf(0) }

    fun addFood(
        kcal: Int,
        protein: Int,
        carb: Int,
        fiber: Int
    ) {
        calories += kcal
        proteins += protein
        carbs += carb
        fibre += fiber
    }

    fun resetDay() {
        calories = 0
        proteins = 0
        carbs = 0
        fibre = 0
    }

    LaunchedEffect(importedProduct?.barcode, importedProduct?.name) {
        importedProduct?.let { product ->
            addFood(
                kcal = product.calories,
                protein = product.protein,
                carb = product.carbs,
                fiber = product.fibre
            )
            onConsumeImportedProduct()
        }
    }

    val progressPercent = ((calories.toFloat() / caloriesTarget.toFloat()) * 100f)
        .coerceIn(0f, 999f)
        .roundToInt()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Conta Calorie") },
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
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Diario calorie giornaliero",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Surface(
                        modifier = Modifier.size(220.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "$calories / $caloriesTarget",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )

                                Text(
                                    text = "Kcal",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CalorieBadge(
                            text = "Progress: $progressPercent%"
                        )
                        CalorieBadge(
                            text = "Residue: ${(caloriesTarget - calories).coerceAtLeast(0)} kcal",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            latestScannedProduct?.let { product ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Ultimo prodotto dallo scanner",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "${product.subtitle} • ${product.calories} kcal",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Log rapido",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = { addFood(180, 12, 18, 3) },
                            label = { Text("Snack demo") }
                        )

                        FilterChip(
                            selected = false,
                            onClick = { addFood(420, 30, 45, 6) },
                            label = { Text("Pranzo demo") }
                        )

                        FilterChip(
                            selected = false,
                            onClick = { addFood(520, 38, 42, 5) },
                            label = { Text("Cena demo") }
                        )
                    }

                    Button(
                        onClick = onOpenScannerClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+ Log food da scanner")
                    }

                    FilledTonalButton(
                        onClick = ::resetDay,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset giornata")
                    }
                }
            }

            MacroCard(
                title = "Proteine",
                current = proteins,
                target = proteinsTarget,
                unit = "g"
            )

            MacroCard(
                title = "Carboidrati",
                current = carbs,
                target = carbsTarget,
                unit = "g"
            )

            MacroCard(
                title = "Fibre",
                current = fibre,
                target = fibreTarget,
                unit = "g"
            )
        }
    }
}

@Composable
private fun MacroCard(
    title: String,
    current: Int,
    target: Int,
    unit: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "$current / $target $unit",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Residuo: ${(target - current).coerceAtLeast(0)} $unit",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CalorieBadge(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
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