package com.example.noticanswer

sealed interface AppScreen {
    data object Home : AppScreen
    data object Genres : AppScreen
    data object Settings : AppScreen

    data class SubGenres(
        val genreId: Long,
        val genreName: String
    ) : AppScreen

    data class Questions(
        val subGenreId: Long,
        val subGenreName: String
    ) : AppScreen

    data class CreateQuestion(
        val subGenreId: Long,
        val subGenreName: String
    ) : AppScreen

    data class EditQuestion(
        val questionId: Int
    ) : AppScreen
}