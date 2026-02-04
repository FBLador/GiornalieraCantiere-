package com.idroambiente.giornalieradicantiere

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.idroambiente.giornalieradicantiere.data.preferences.PreferencesRepository
import com.idroambiente.giornalieradicantiere.ui.navigation.AppNavHost
import com.idroambiente.giornalieradicantiere.ui.theme.GiornalieraDiCantiereTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GiornalieraDiCantiereTheme {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    preferencesRepository = preferencesRepository
                )
            }
        }
    }
}
