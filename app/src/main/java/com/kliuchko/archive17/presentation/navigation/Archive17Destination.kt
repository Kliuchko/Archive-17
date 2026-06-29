package com.kliuchko.archive17.presentation.navigation

sealed interface Archive17Destination {
    val route: String

    data object Search : Archive17Destination {
        override val route = "search"
    }

    data object Library : Archive17Destination {
        override val route = "library"
    }

    data object Details : Archive17Destination {
        const val WORK_ID_ARGUMENT = "workId"
        override val route = "details/{$WORK_ID_ARGUMENT}"

        fun createRoute(workId: String): String = "details/$workId"
    }
}
