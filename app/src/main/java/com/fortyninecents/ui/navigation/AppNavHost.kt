package com.fortyninecents.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fortyninecents.ui.home.HomeScreen
import com.fortyninecents.ui.review.ReviewScreen
import com.fortyninecents.ui.transactions.TransactionsScreen

object Routes {
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val REVIEW = "review"
}

@Composable
fun AppNavHost(startDestination: String = Routes.HOME) {
    val navController = rememberNavController()
    // evita "voltar" duas vezes (toque duplo) e deixar a pilha vazia
    val back: () -> Unit = {
        if (navController.previousBackStackEntry != null) navController.popBackStack()
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenTransactions = { navController.navigate(Routes.TRANSACTIONS) { launchSingleTop = true } },
                onOpenReview = { navController.navigate(Routes.REVIEW) { launchSingleTop = true } },
            )
        }
        composable(Routes.TRANSACTIONS) {
            TransactionsScreen(onBack = back)
        }
        composable(Routes.REVIEW) {
            ReviewScreen(onBack = back)
        }
    }
}
