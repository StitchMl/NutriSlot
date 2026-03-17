package it.lagioiaproductions.nutrislot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import it.lagioiaproductions.nutrislot.navigation.AppNavGraph
import it.lagioiaproductions.nutrislot.ui.theme.NutriSlotTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NutriSlotTheme {
                AppNavGraph()
            }
        }
    }
}