package com.kliuchko.archive17.presentation.navigation

sealed interface Archive17Destination {
    val route: String

    data object Home : Archive17Destination {
        override val route = "home"
    }

    data object Search : Archive17Destination {
        override val route = "search"
    }

    data object Library : Archive17Destination {
        override val route = "library"
    }

    data object Profile : Archive17Destination {
        override val route = "profile"
    }

    data object Details : Archive17Destination {
        const val WORK_ID_ARGUMENT = "workId"
        override val route = "details/{$WORK_ID_ARGUMENT}"

        fun createRoute(workId: String): String = "details/$workId"
    }

    data object LocalDetails : Archive17Destination {
        const val BOOK_ID_ARGUMENT = "bookId"
        override val route = "local-details/{$BOOK_ID_ARGUMENT}"

        fun createRoute(bookId: String): String = "local-details/$bookId"
    }

    data object FreeDetails : Archive17Destination {
        const val EDITION_ID_ARGUMENT = "editionId"
        override val route = "free-details/{$EDITION_ID_ARGUMENT}"

        fun createRoute(editionId: String): String = "free-details/$editionId"
    }
}
