package com.assessemnt.myhealthapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.assessemnt.myhealthapp.presentation.conflictlist.ConflictListScreen
import com.assessemnt.myhealthapp.presentation.exerciselist.*
import com.assessemnt.myhealthapp.presentation.manualinput.ManualInputScreen

sealed class Screen(val route: String) {
    data object ExerciseList : Screen("exercise_list")
    data object ManualInput : Screen("manual_input")
    data object ConflictList : Screen("conflict_list")
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
            val viewModel: ExerciseListViewModel = viewModel()

            // Refresh when returning from manual input
            androidx.compose.runtime.LaunchedEffect(Unit) {
                viewModel.refreshExercises()
            }

            ExerciseListScreen(
                onNavigateToManualInput = {
                    navController.navigate(Screen.ManualInput.route)
                },
                viewModel = viewModel
            )
        }

        // Manual Input Screen
        composable(Screen.ManualInput.route) {
            ManualInputScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Conflict List Screen
        composable(Screen.ConflictList.route) {
            ConflictListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}