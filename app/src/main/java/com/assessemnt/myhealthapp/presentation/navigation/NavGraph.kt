package com.assessemnt.myhealthapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.assessemnt.myhealthapp.presentation.exerciselist.ExerciseListScreen

sealed class Screen(val route: String) {
    data object ExerciseList : Screen("exercise_list")
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.ExerciseList.route
    ) {
        // Exercise List Screen
        composable(Screen.ExerciseList.route) {
            ExerciseListScreen()
        }
    }
}