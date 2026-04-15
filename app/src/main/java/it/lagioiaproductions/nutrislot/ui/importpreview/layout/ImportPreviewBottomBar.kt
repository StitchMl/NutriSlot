package it.lagioiaproductions.nutrislot.ui.importpreview.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.ui.importfile.state.ImportFileUiState

/** Bottom action bar for confirming or leaving the import preview flow. */
@Composable
internal fun ImportPreviewBottomBar(
    uiState: ImportFileUiState,
    onBackClick: () -> Unit,
    onConfirmReviewClick: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!uiState.hasAnyMealText) {
                Text(
                    text = "Inserisci almeno un pasto per poter salvare.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()
            }

            Button(
                onClick = onConfirmReviewClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && uiState.hasAnyMealText
            ) {
                Text(
                    if (uiState.isLoading) {
                        "Salvataggio..."
                    } else if (uiState.isManualDraft) {
                        "Salva piano"
                    } else {
                        "Conferma e salva"
                    }
                )
            }

            FilledTonalButton(
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text("Torna alla scelta")
            }
        }
    }
}
