package com.idroambiente.giornalieradicantiere

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import android.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.idroambiente.giornalieradicantiere.data.preferences.TemaApp
import com.idroambiente.giornalieradicantiere.ui.theme.isDark
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.idroambiente.giornalieradicantiere.data.preferences.PreferencesRepository
import com.idroambiente.giornalieradicantiere.notifications.DailyReminderScheduler
import com.idroambiente.giornalieradicantiere.ui.navigation.AppNavHost
import com.idroambiente.giornalieradicantiere.ui.theme.GiornalieraDiCantiereTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var dailyReminderScheduler: DailyReminderScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            dailyReminderScheduler.syncScheduleFromPreferences()
        }

        // L'intestazione è sempre scura (blu o marrone scuro): icone della barra di stato chiare
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContent {
            val tema by preferencesRepository.tema.collectAsState(initial = TemaApp.SISTEMA)
            GiornalieraDiCantiereTheme(darkTheme = tema.isDark(isSystemInDarkTheme())) {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    preferencesRepository = preferencesRepository
                )
            }
        }
    }
}
