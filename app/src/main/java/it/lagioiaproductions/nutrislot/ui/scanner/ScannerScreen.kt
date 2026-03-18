package it.lagioiaproductions.nutrislot.ui.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.ui.shared.LinkedScannedProductUi

private data class ScannerProductUi(
    val id: Int,
    val name: String,
    val subtitle: String,
    val barcode: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fibre: Int
)

private enum class ScannerMode(
    val label: String
) {
    BARCODE("Barcode"),
    MANUAL("Ricerca manuale")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onBackClick: () -> Unit,
    onAddToShoppingList: (LinkedScannedProductUi) -> Unit,
    onSendToCalorieTracker: (LinkedScannedProductUi) -> Unit
) {
    var selectedMode by remember { mutableStateOf(ScannerMode.BARCODE) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedProductId by remember { mutableStateOf<Int?>(null) }
    var scanCounter by remember { mutableIntStateOf(0) }

    val demoProducts = remember {
        mutableStateListOf(
            ScannerProductUi(
                id = 1,
                name = "Cereali integrali",
                subtitle = "Porzione 45 g • colazione",
                barcode = "8001234567890",
                calories = 200,
                protein = 8,
                carbs = 32,
                fibre = 5
            ),
            ScannerProductUi(
                id = 2,
                name = "Yogurt greco bianco",
                subtitle = "Vasetto 170 g • snack",
                barcode = "8005550001112",
                calories = 145,
                protein = 15,
                carbs = 6,
                fibre = 0
            ),
            ScannerProductUi(
                id = 3,
                name = "Pane proteico",
                subtitle = "2 fette • pranzo",
                barcode = "8019993334445",
                calories = 180,
                protein = 14,
                carbs = 18,
                fibre = 7
            )
        )
    }

    val filteredProducts = demoProducts.filter { product ->
        searchQuery.isBlank() ||
                product.name.contains(searchQuery, ignoreCase = true) ||
                product.subtitle.contains(searchQuery, ignoreCase = true) ||
                product.barcode.contains(searchQuery)
    }

    val selectedProduct = filteredProducts.firstOrNull { it.id == selectedProductId }
        ?: demoProducts.firstOrNull { it.id == selectedProductId }
        ?: filteredProducts.firstOrNull()

    fun ScannerProductUi.toLinkedProduct(): LinkedScannedProductUi {
        return LinkedScannedProductUi(
            name = name,
            subtitle = subtitle,
            barcode = barcode,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fibre = fibre
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Scanner") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Impostazioni scanner"
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Ricerca prodotto",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Qui porteremo barcode scan e ricerca manuale. Per ora la schermata simula l’ingresso del prodotto e il collegamento con spesa e calorie.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScannerBadge(text = "Sessione: ${scanCounter + 1}")
                        ScannerBadge(
                            text = if (selectedProduct != null) "Prodotto selezionato" else "Nessuna selezione",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
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
                        text = "Modalità",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScannerMode.entries.forEach { mode ->
                            FilterChip(
                                selected = selectedMode == mode,
                                onClick = { selectedMode = mode },
                                label = { Text(mode.label) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Cerca prodotto o barcode") },
                        placeholder = { Text("Es. Yogurt greco") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null
                            )
                        }
                    )
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(
                            modifier = Modifier.padding(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(110.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (selectedMode == ScannerMode.BARCODE) "SCAN" else "TYPE",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Text(
                                    text = if (selectedMode == ScannerMode.BARCODE) {
                                        "Inquadra o simula un barcode"
                                    } else {
                                        "Cerca manualmente un prodotto"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )

                                FilledTonalButton(
                                    onClick = {
                                        scanCounter += 1
                                        selectedProductId = filteredProducts.firstOrNull()?.id
                                            ?: demoProducts.firstOrNull()?.id
                                    }
                                ) {
                                    Text(
                                        if (selectedMode == ScannerMode.BARCODE) {
                                            "Simula scansione"
                                        } else {
                                            "Usa primo risultato"
                                        }
                                    )
                                }
                            }
                        }
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
                        text = "Risultati",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (filteredProducts.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Nessun prodotto trovato con i filtri attuali.",
                                modifier = Modifier.padding(14.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        filteredProducts.forEach { product ->
                            ScannerResultCard(
                                productName = product.name,
                                subtitle = product.subtitle,
                                caloriesLabel = "${product.calories} kcal",
                                barcode = product.barcode,
                                isSelected = selectedProductId == product.id,
                                onSelect = { selectedProductId = product.id }
                            )
                        }
                    }
                }
            }

            if (selectedProduct != null) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Prodotto attivo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = selectedProduct.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "${selectedProduct.subtitle} • ${selectedProduct.calories} kcal",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ScannerBadge(text = "Barcode: ${selectedProduct.barcode}")
                            ScannerBadge(
                                text = "${selectedProduct.protein}P • ${selectedProduct.carbs}C • ${selectedProduct.fibre}F",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Button(
                            onClick = { onAddToShoppingList(selectedProduct.toLinkedProduct()) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Aggiungi alla lista spesa")
                        }

                        FilledTonalButton(
                            onClick = { onSendToCalorieTracker(selectedProduct.toLinkedProduct()) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Invia al conta calorie")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannerResultCard(
    productName: String,
    subtitle: String,
    caloriesLabel: String,
    barcode: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = productName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScannerBadge(
                    text = caloriesLabel,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                ScannerBadge(text = barcode)
            }

            FilledTonalButton(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSelected) "Selezionato" else "Seleziona")
            }
        }
    }
}

@Composable
private fun ScannerBadge(
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