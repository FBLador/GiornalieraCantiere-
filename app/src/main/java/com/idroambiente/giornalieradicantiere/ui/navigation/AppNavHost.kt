package com.idroambiente.giornalieradicantiere.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.idroambiente.giornalieradicantiere.ui.commesse.CommesseScreen
import com.idroambiente.giornalieradicantiere.ui.giornaliere.GiornaliereScreen
import com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard.GiornalieraWizardScreen
import com.idroambiente.giornalieradicantiere.ui.impostazioni.AttrezzaturaListScreen
import com.idroambiente.giornalieradicantiere.ui.impostazioni.ImpostazioniScreen
import com.idroambiente.giornalieradicantiere.ui.impostazioni.MezzoListScreen
import com.idroambiente.giornalieradicantiere.ui.impostazioni.PersonaleListScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // TODO: Caricare ultima commessa aperta da DataStore e navigare direttamente
    // TODO: Aggiungere schermata autenticazione come startDestination

    NavHost(
        navController = navController,
        startDestination = Routes.COMMESSE_LIST,
        modifier = modifier
    ) {
        composable(Routes.COMMESSE_LIST) {
            CommesseScreen(
                onNavigateToCommessa = { commessaId ->
                    navController.navigate(Routes.giornaliereList(commessaId))
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
