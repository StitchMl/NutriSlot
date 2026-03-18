package it.lagioiaproductions.nutrislot.ui.shoppinglist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.ui.shared.LinkedScannedProductUi
import it.lagioiaproductions.nutrislot.ui.shared.ShoppingListItemUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    onOpenScannerClick: () -> Unit,
    shoppingItems: List<ShoppingListItemUi>,
    latestScannedProduct: LinkedScannedProductUi?,
    onAddManualItem: (String) -> Unit,
    onTogglePurchased: (Long) -> Unit,
    onRemoveItem: (Long) -> Unit
) {
    var draftText by remember { mutableStateOf("") }

    val purchasedCount = shoppingItems.count { it.isPurchased }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Lista spesa") }
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
                        text = "Lista acquisti",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Gli articoli aggiunti dal planner e dallo scanner restano disponibili finché l'app rimane aperta.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShoppingBadge(text = "Totale: ${shoppingItems.size}")
                        ShoppingBadge(text = "Acquistati: $purchasedCount")
                        ShoppingBadge(
                            text = "Planner collegato",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                            text = product.subtitle,
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
                        text = "Aggiungi articolo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = draftText,
                        onValueChange = { draftText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nuovo articolo") },
                        placeholder = { Text("Es. Yogurt greco") },
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            onAddManualItem(draftText)
                            draftText = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = draftText.isNotBlank()
                    ) {
                        Text("Aggiungi alla lista")
                    }

                    FilledTonalButton(
                        onClick = onOpenScannerClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apri scanner prodotti")
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
                        text = "Articoli attuali",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (shoppingItems.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "La lista è vuota. Aggiungi il primo articolo dal planner, dallo scanner o manualmente.",
                                modifier = Modifier.padding(14.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        shoppingItems.forEachIndexed { index, item ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = item.isPurchased,
                                        onCheckedChange = { onTogglePurchased(item.id) }
                                    )

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (item.isPurchased) {
                                                FontWeight.Normal
                                            } else {
                                                FontWeight.Medium
                                            },
                                            color = if (item.isPurchased) {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )

                                        Text(
                                            text = if (item.isPurchased) {
                                                "Acquistato"
                                            } else {
                                                "Da acquistare"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    TextButton(
                                        onClick = { onRemoveItem(item.id) }
                                    ) {
                                        Text("Rimuovi")
                                    }
                                }
                            }

                            if (index != shoppingItems.lastIndex) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShoppingBadge(
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
            color = contentColor
        )
    }
}