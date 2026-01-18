package com.idroambiente.giornalieradicantiere.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.idroambiente.giornalieradicantiere.ui.commesse.CommesseScreen

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
                    // TODO: Navigare a dettaglio commessa o lista cantieri
                    // navController.navigate("${Routes.CANTIERI_LIST}/$commessaId")
                }
            )
        }

        // TODO: Aggiungere altre schermate
        // composable("${Routes.CANTIERI_LIST}/{commessaId}") { backStackEntry ->
        //     val commessaId = backStackEntry.arguments?.getString("commessaId")?.toLongOrNull()
        //     CantieriScreen(commessaId = commessaId)
        // }
    }
}
