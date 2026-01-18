package com.idroambiente.giornalieradicantiere

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.idroambiente.giornalieradicantiere.ui.navigation.AppNavHost
import com.idroambiente.giornalieradicantiere.ui.theme.GiornalieraDiCantiereTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // TODO: Caricare ultima commessa aperta da DataStore all'avvio
    // TODO: Aggiungere autenticazione prima della navigazione

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GiornalieraDiCantiereTheme {
                val navController = rememberNavController()
                AppNavHost(navController = navController)
            }
        }
    }
}