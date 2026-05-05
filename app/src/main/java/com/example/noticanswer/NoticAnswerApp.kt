package com.example.noticanswer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun NoticAnswerApp() {
    var backStack by remember {
        mutableStateOf<List<AppScreen>>(listOf(AppScreen.Home))
    }

    val currentScreen = backStack.last()

    fun navigate(screen: AppScreen) {
        backStack = backStack + screen
    }

    fun goBack() {
        if (backStack.size > 1) {
            backStack = backStack.dropLast(1)
        }
    }

    BackHandler(enabled = backStack.size > 1) {
        goBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        when (val screen = currentScreen) {
            AppScreen.Home -> HomeScreen(
                onOpenProblems = {
                    navigate(AppScreen.Genres)
                },
                onOpenSettings = {
                    navigate(AppScreen.Settings)
                }
            )

            AppScreen.Genres -> GenreListScreen(
                onBack = ::goBack,
                onOpenGenre = { genre ->
                    navigate(
                        AppScreen.SubGenres(
                            genreId = genre.id,
                            genreName = genre.name
                        )
                    )
                }
            )

            is AppScreen.SubGenres -> SubGenreListScreen(
                genreId = screen.genreId,
                genreName = screen.genreName,
                onBack = ::goBack,
                onOpenSubGenre = { subGenre ->
                    navigate(
                        AppScreen.Questions(
                            subGenreId = subGenre.id,
                            subGenreName = subGenre.name
                        )
                    )
                }
            )

            is AppScreen.Questions -> QuestionListScreen(
                subGenreId = screen.subGenreId,
                subGenreName = screen.subGenreName,
                onBack = ::goBack,
                onCreateQuestion = {
                    navigate(
                        AppScreen.CreateQuestion(
                            subGenreId = screen.subGenreId,
                            subGenreName = screen.subGenreName
                        )
                    )
                },
                onEditQuestion = { question ->
                    navigate(AppScreen.EditQuestion(question.id))
                }
            )

            is AppScreen.CreateQuestion -> QuestionCreateScreen(
                subGenreId = screen.subGenreId,
                subGenreName = screen.subGenreName,
                onBack = ::goBack,
                onSaved = ::goBack
            )

            is AppScreen.EditQuestion -> QuestionEditScreen(
                questionId = screen.questionId,
                onBack = ::goBack,
                onSaved = ::goBack
            )

            AppScreen.Settings -> SettingsScreen(
                onBack = ::goBack
            )
        }
    }
}