package com.idroambiente.giornalieradicantiere.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.idroambiente.giornalieradicantiere.data.preferences.PreferencesRepository
import com.idroambiente.giornalieradicantiere.ui.commesse.CommesseScreen
import com.idroambiente.giornalieradicantiere.ui.commesse.RiepilogoCommessaScreen
import com.idroambiente.giornalieradicantiere.ui.giornaliere.GiornaliereScreen
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.GiornalieraWizardScreen
import com.idroambiente.giornalieradicantiere.ui.impostazioni.AttrezzaturaListScreen
import com.idroambiente.giornalieradicantiere.ui.impostazioni.ImpostazioniScreen
import com.idroambiente.giornalieradicantiere.ui.impostazioni.MezzoListScreen
import com.idroambiente.giornalieradicantiere.ui.impostazioni.PersonaleListScreen
import com.idroambiente.giornalieradicantiere.ui.setup.SetupScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    preferencesRepository: PreferencesRepository,
    modifier: Modifier = Modifier
) {
    val setupCompleted by preferencesRepository.setupCompleted.collectAsState(initial = null)

    // Show nothing while loading the preference (null = not yet loaded)
    if (setupCompleted == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = if (setupCompleted == true) Routes.COMMESSE_LIST else Routes.SETUP

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.SETUP) {
            SetupScreen(
                onSetupComplete = {
                    navController.navigate(Routes.COMMESSE_LIST) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.COMMESSE_LIST) {
            CommesseScreen(
                onNavigateToCommessa = { commessaId ->
                    navController.navigate(Routes.giornaliereList(commessaId))
                },
                onNavigateToRiepilogoCommessa = { commessaId ->
                    navController.navigate(Routes.riepilogoCommessa(commessaId))
                },
                onNavigateToImpostazioni = {
                    navController.navigate(Routes.IMPOSTAZIONI)
                }
            )
        }

        composable(
            route = Routes.GIORNALIERE_LIST,
            arguments = listOf(navArgument("commessaId") { type = NavType.LongType })
        ) {
            GiornaliereScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGiornaliera = { cantiereId, giornalieraId ->
                    navController.navigate(Routes.giornalieraWizard(cantiereId, giornalieraId))
                },
                onNavigateToWizard = { cantiereId ->
                    navController.navigate(Routes.giornalieraWizard(cantiereId))
                },
                onNavigateToRiepilogoCommessa = { commessaId ->
                    navController.navigate(Routes.riepilogoCommessa(commessaId))
                }
            )
        }

        composable(
            route = Routes.RIEPILOGO_COMMESSA,
            arguments = listOf(navArgument("commessaId") { type = NavType.LongType })
        ) {
            RiepilogoCommessaScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.GIORNALIERA_WIZARD,
            arguments = listOf(
                navArgument("cantiereId") { type = NavType.LongType },
                navArgument("giornalieraId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) {
            GiornalieraWizardScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.IMPOSTAZIONI) {
            ImpostazioniScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPersonaleList = {
                    navController.navigate(Routes.PERSONALE_LIST)
                },
                onNavigateToMezzoList = {
                    navController.navigate(Routes.MEZZO_LIST)
                },
                onNavigateToAttrezzaturaList = {
                    navController.navigate(Routes.ATTREZZATURA_LIST)
                }
            )
        }

        composable(Routes.PERSONALE_LIST) {
            PersonaleListScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MEZZO_LIST) {
            MezzoListScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ATTREZZATURA_LIST) {
            AttrezzaturaListScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
