package com.kliuchko.archive17.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kliuchko.archive17.presentation.details.BookDetailsScreen
import com.kliuchko.archive17.presentation.library.LibraryScreen
import com.kliuchko.archive17.presentation.search.SearchScreen

@Composable
fun Archive17NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Archive17Destination.Search.route,
        modifier = modifier,
    ) {
        composable(Archive17Destination.Search.route) {
            SearchScreen(
                onBookClick = { workId ->
                    navController.navigate(Archive17Destination.Details.createRoute(workId))
                },
                onLibraryClick = {
                    navController.navigate(Archive17Destination.Library.route) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = Archive17Destination.Details.route,
            arguments = listOf(
                navArgument(Archive17Destination.Details.WORK_ID_ARGUMENT) {
                    type = NavType.StringType
                },
            ),
        ) { backStackEntry ->
            val workId = backStackEntry.arguments
                ?.getString(Archive17Destination.Details.WORK_ID_ARGUMENT)
                .orEmpty()

            BookDetailsScreen(
                workId = workId,
                onBackClick = navController::popBackStack,
                onLibraryClick = {
                    navController.navigate(Archive17Destination.Library.route) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Archive17Destination.Library.route) {
            LibraryScreen(
                onBookClick = { workId ->
                    navController.navigate(Archive17Destination.Details.createRoute(workId))
                },
                onSearchClick = {
                    navController.navigate(Archive17Destination.Search.route) {
                        popUpTo(Archive17Destination.Search.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}
