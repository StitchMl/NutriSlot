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
import androidx.compose.runtime.LaunchedEffect
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

private data class ShoppingItemUi(
    val id: Int,
    val name: String,
    val isPurchased: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    onOpenScannerClick: () -> Unit,
    importedProduct: LinkedScannedProductUi?,
    latestScannedProduct: LinkedScannedProductUi?,
    onConsumeImportedProduct: () -> Unit
) {
    val items = remember {
        mutableStateListOf(
            ShoppingItemUi(1, "Pane fresco"),
            ShoppingItemUi(2, "Latte intero"),
            ShoppingItemUi(3, "Pomodori"),
            ShoppingItemUi(4, "Frutta di stagione"),
            ShoppingItemUi(5, "Uova"),
            ShoppingItemUi(6, "Parmigiano"),
            ShoppingItemUi(7, "Pasta"),
            ShoppingItemUi(8, "Avocado")
        )
    }

    var nextId by remember { mutableIntStateOf(9) }
    var draftText by remember { mutableStateOf("") }

    val purchasedCount = items.count { it.isPurchased }

    fun addItemFromText(text: String) {
        val normalized = text.trim()
        if (normalized.isBlank()) return

        items.add(
            0,
            ShoppingItemUi(
                id = nextId,
                name = normalized
            )
        )
        nextId += 1
    }

    fun addItem() {
        addItemFromText(draftText)
        draftText = ""
    }

    fun togglePurchased(itemId: Int) {
        val index = items.indexOfFirst { it.id == itemId }
        if (index == -1) return

        val current = items[index]
        items[index] = current.copy(isPurchased = !current.isPurchased)
    }

    fun removeItem(itemId: Int) {
        val index = items.indexOfFirst { it.id == itemId }
        if (index != -1) {
            items.removeAt(index)
        }
    }

    LaunchedEffect(importedProduct?.barcode, importedProduct?.name) {
        importedProduct?.let { product ->
            addItemFromText(product.name)
            onConsumeImportedProduct()
        }
    }

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
                        text = "Puoi aggiungere articoli manualmente e usare lo scanner per cercare prodotti da portare qui.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShoppingBadge(text = "Totale: ${items.size}")
                        ShoppingBadge(text = "Acquistati: $purchasedCount")
                        ShoppingBadge(
                            text = "Scanner pronto",
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
                        onClick = ::addItem,
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

                    if (items.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "La lista è vuota. Aggiungi il primo articolo qui sopra.",
                                modifier = Modifier.padding(14.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items.forEachIndexed { index, item ->
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
                                        onCheckedChange = { togglePurchased(item.id) }
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
                                        onClick = { removeItem(item.id) }
                                    ) {
                                        Text("Rimuovi")
                                    }
                                }
                            }

                            if (index != items.lastIndex) {
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