package com.idroambiente.giornalieradicantiere.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
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
import com.idroambiente.giornalieradicantiere.ui.impostazioni.ImportaDatiScreen
import com.idroambiente.giornalieradicantiere.ui.impostazioni.ListeGestioneScreen
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

    fun safePopBackStack() {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        val isRoot = currentRoute == Routes.COMMESSE_LIST || currentRoute == Routes.SETUP
        // Non chiamare popBackStack alla root - evita race dove BackHandler di schermata precedente
        // è ancora attivo durante transizione; popBackStack() alla root può lasciare currentBackStackEntry null
        if (isRoot) return
        navController.popBackStack()
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300))
        }
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
                },
                onNavigateToWizard = { cantiereId, giornalieraId ->
                    navController.navigate(Routes.giornalieraWizard(cantiereId, giornalieraId))
                }
            )
        }

        composable(
            route = Routes.GIORNALIERE_LIST,
            arguments = listOf(navArgument("commessaId") { type = NavType.LongType })
        ) {
            GiornaliereScreen(
                onNavigateBack = { safePopBackStack() },
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
                onNavigateBack = { safePopBackStack() },
                onOpenGiornaliera = { cantiereId, giornalieraId ->
                    navController.navigate(Routes.giornalieraWizard(cantiereId, giornalieraId))
                }
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
                onNavigateBack = { safePopBackStack() }
            )
        }

        composable(Routes.IMPOSTAZIONI) {
            ImpostazioniScreen(
                onNavigateBack = { safePopBackStack() },
                onNavigateToListeGestione = {
                    navController.navigate(Routes.LISTE_GESTIONE)
                },
                onNavigateToImporta = { anagrafiche ->
                    navController.navigate(Routes.importaDati(anagrafiche))
                }
            )
        }

        composable(
            route = Routes.IMPORTA_DATI,
            arguments = listOf(navArgument("anagrafiche") { type = NavType.BoolType })
        ) { entry ->
            ImportaDatiScreen(
                anagrafiche = entry.arguments?.getBoolean("anagrafiche") ?: true,
                onNavigateBack = { safePopBackStack() }
            )
        }

        composable(Routes.LISTE_GESTIONE) {
            ListeGestioneScreen(
                onNavigateBack = { safePopBackStack() }
            )
        }

        composable(Routes.PERSONALE_LIST) {
            PersonaleListScreen(
                onNavigateBack = { safePopBackStack() }
            )
        }

        composable(Routes.MEZZO_LIST) {
            MezzoListScreen(
                onNavigateBack = { safePopBackStack() }
            )
        }

        composable(Routes.ATTREZZATURA_LIST) {
            AttrezzaturaListScreen(
                onNavigateBack = { safePopBackStack() }
            )
        }
    }
}
