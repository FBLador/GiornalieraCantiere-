package com.idroambiente.giornalieradicantiere

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
