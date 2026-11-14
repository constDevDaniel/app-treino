package com.daniel.treino

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.daniel.treino.ui.TreinoAppShell
import com.daniel.treino.ui.theme.TreinoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TreinoTheme(temaEscuro = false) {
                TreinoAppShell()
            }
        }
    }
}
