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
                }
            )
        }

        composable(
            route = Routes.GIORNALIERE_LIST,
            arguments = listOf(navArgument("commessaId") { type = NavType.LongType })
        ) {
            GiornaliereScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGiornaliera = { giornalieraId ->
                    // TODO: Navigare a dettaglio giornaliera
                }
            )
        }
    }
}
