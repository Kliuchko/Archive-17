package com.kliuchko.archive17.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kliuchko.archive17.presentation.components.ArchiveNavIcon
import com.kliuchko.archive17.presentation.components.ArchiveNavigationIcon
import com.kliuchko.archive17.presentation.details.BookDetailsScreen
import com.kliuchko.archive17.presentation.home.HomeScreen
import com.kliuchko.archive17.presentation.library.LibraryScreen
import com.kliuchko.archive17.presentation.profile.ProfileScreen
import com.kliuchko.archive17.presentation.reader.EpubReaderActivity
import com.kliuchko.archive17.presentation.search.SearchScreen

private data class TopLevelDestination(
    val destination: Archive17Destination,
    val label: String,
    val icon: ArchiveNavIcon,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(Archive17Destination.Home, "Главная", ArchiveNavIcon.HOME),
    TopLevelDestination(Archive17Destination.Search, "Каталог", ArchiveNavIcon.SEARCH),
    TopLevelDestination(Archive17Destination.Library, "Полка", ArchiveNavIcon.SHELF),
    TopLevelDestination(Archive17Destination.Profile, "Профиль", ArchiveNavIcon.PROFILE),
)

@Composable
fun Archive17App(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = topLevelDestinations.any { it.destination.route == currentRoute }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                ArchiveBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Archive17Destination.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(Archive17Destination.Home.route) {
                HomeScreen(
                    onBookClick = { workId ->
                        navController.navigate(Archive17Destination.Details.createRoute(workId))
                    },
                    onCatalogClick = {
                        navController.navigate(Archive17Destination.Search.route) {
                            launchSingleTop = true
                        }
                    },
                    onLocalBookClick = { bookId ->
                        context.startActivity(EpubReaderActivity.createIntent(context, bookId))
                    },
                )
            }

            composable(Archive17Destination.Search.route) {
                SearchScreen(
                    onBookClick = { workId ->
                        navController.navigate(Archive17Destination.Details.createRoute(workId))
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
            ) { detailsEntry ->
                val workId = detailsEntry.arguments
                    ?.getString(Archive17Destination.Details.WORK_ID_ARGUMENT)
                    .orEmpty()

                BookDetailsScreen(
                    workId = workId,
                    onBackClick = navController::popBackStack,
                )
            }

            composable(Archive17Destination.Library.route) {
                LibraryScreen(
                    onBookClick = { workId ->
                        navController.navigate(Archive17Destination.Details.createRoute(workId))
                    },
                    onCatalogClick = {
                        navController.navigate(Archive17Destination.Search.route) {
                            launchSingleTop = true
                        }
                    },
                    onLocalBookClick = { bookId ->
                        context.startActivity(EpubReaderActivity.createIntent(context, bookId))
                    },
                )
            }

            composable(Archive17Destination.Profile.route) {
                ProfileScreen()
            }
        }
    }
}

@Composable
private fun ArchiveBottomNavigation(
    currentRoute: String?,
    onNavigate: (Archive17Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = androidx.compose.ui.unit.Dp.Hairline,
    ) {
        topLevelDestinations.forEach { item ->
            val selected = currentRoute == item.destination.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.destination) },
                icon = {
                    ArchiveNavigationIcon(icon = item.icon)
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
            )
        }
    }
}
