package it.lagioiaproductions.nutrislot.ui.scanner

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ScannerHeroCard(
    previewBitmap: Bitmap?,
    isAnalyzing: Boolean,
    onTakePhotoClick: () -> Unit,
    onPickFromGalleryClick: () -> Unit,
    onAnalyzeClick: () -> Unit,
    onClearImageClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Scansione",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        if (previewBitmap == null) {
            ScannerEmptyHero(
                onTakePhotoClick = onTakePhotoClick,
                onPickFromGalleryClick = onPickFromGalleryClick
            )
        } else {
            ScannerPreviewHero(
                previewBitmap = previewBitmap,
                isAnalyzing = isAnalyzing,
                onTakePhotoClick = onTakePhotoClick,
                onPickFromGalleryClick = onPickFromGalleryClick,
                onAnalyzeClick = onAnalyzeClick,
                onClearImageClick = onClearImageClick
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScannerEmptyHero(
    onTakePhotoClick: () -> Unit,
    onPickFromGalleryClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(34.dp)
                )
            }

            Text(
                text = "Scansiona un prodotto alimentare",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Fotografa il fronte della confezione oppure carica un'immagine nitida. Per leggere bene i nutrienti, la tabella deve essere visibile e ravvicinata.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScannerHintChip("Nome")
                ScannerHintChip("Marca")
                ScannerHintChip("Kcal e macro")
                ScannerHintChip("Barcode")
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onTakePhotoClick,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null)
            Text(text = "Scatta", modifier = Modifier.padding(start = 8.dp))
        }

        FilledTonalButton(
            onClick = onPickFromGalleryClick,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
            Text(text = "Galleria", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun ScannerPreviewHero(
    previewBitmap: Bitmap,
    isAnalyzing: Boolean,
    onTakePhotoClick: () -> Unit,
    onPickFromGalleryClick: () -> Unit,
    onAnalyzeClick: () -> Unit,
    onClearImageClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                bitmap = previewBitmap.asImageBitmap(),
                contentDescription = "Anteprima prodotto",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 260.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )

            Text(
                text = "Foto pronta per l'analisi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "L'analisi funziona molto meglio se la foto e nitida e la tabella nutrizionale e leggibile.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onAnalyzeClick,
            enabled = !isAnalyzing,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Text(text = "Analizza", modifier = Modifier.padding(start = 8.dp))
        }

        FilledTonalButton(
            onClick = onTakePhotoClick,
            enabled = !isAnalyzing,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null)
            Text(text = "Nuova foto", modifier = Modifier.padding(start = 8.dp))
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilledTonalButton(
            onClick = onPickFromGalleryClick,
            enabled = !isAnalyzing,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
            Text(text = "Cambia", modifier = Modifier.padding(start = 8.dp))
        }

        FilledTonalButton(
            onClick = onClearImageClick,
            enabled = !isAnalyzing,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Text(text = "Rimuovi", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun ScannerHintChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
