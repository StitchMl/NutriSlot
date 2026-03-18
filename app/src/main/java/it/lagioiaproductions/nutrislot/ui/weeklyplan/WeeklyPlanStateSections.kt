package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.domain.model.WeekDay

@Composable
internal fun LoadingContent(
    innerPadding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 20.dp, vertical = 1.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator()

        Text(
            text = "Sto caricando il piano settimanale...",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
internal fun ErrorContent(
    innerPadding: PaddingValues,
    message: String,
    onImportClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Non riesco a caricare la dieta",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Button(
            onClick = onRefreshClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Riprova")
        }

        FilledTonalButton(
            onClick = onImportClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Importa un piano")
        }
    }
}

@Composable
internal fun EmptyContent(
    innerPadding: PaddingValues,
    onImportClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 20.dp, vertical = 1.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Nessuna dieta salvata",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Importa un PDF simile ai tuoi piani alimentari, controlla l’anteprima e poi torna qui per vedere la settimana in una vista più chiara e dinamica.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Button(
            onClick = onImportClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Importa piano da file")
        }
    }
}

@Composable
internal fun EmptySelectedDayStateCard(
    selectedDay: WeekDay,
    hiddenSelectedDaySlotsCount: Int,
    isShowingConsumed: Boolean
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Nessun slot da mostrare per ${selectedDay.displayName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = when {
                    !isShowingConsumed && hiddenSelectedDaySlotsCount > 0 ->
                        "Gli slot di questa giornata risultano già completati. Puoi mostrarli di nuovo attivando il filtro sui completati."
                    else ->
                        "Per questa giornata non ci sono slot visibili nel filtro attuale."
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}