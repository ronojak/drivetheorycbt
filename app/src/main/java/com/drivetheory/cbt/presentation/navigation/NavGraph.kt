package com.drivetheory.cbt.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.drivetheory.cbt.presentation.screens.ExamScreen
import com.drivetheory.cbt.presentation.screens.HistoryScreen
import com.drivetheory.cbt.presentation.screens.HomeScreen
import com.drivetheory.cbt.presentation.screens.ResultsScreen
import com.drivetheory.cbt.presentation.screens.ExamSetupScreen
import com.drivetheory.cbt.presentation.screens.ReviewScreen

@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME, modifier = modifier) {
        composable(Routes.HOME) {
            HomeScreen(
                onStartCar = { navController.navigate(Routes.examSetup("car")) },
                onStartMotorcycle = { navController.navigate(Routes.examSetup("motorcycle")) },
                onStartLorry = { navController.navigate(Routes.examSetup("lorry")) },
                onStartBusCoach = { navController.navigate(Routes.examSetup("buscoach")) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) }
            )
        }
        composable(
            route = Routes.EXAM_SETUP,
            arguments = listOf(navArgument("vehicle") { type = NavType.StringType; defaultValue = "car" })
        ) { backStackEntry ->
            val vehicle = backStackEntry.arguments?.getString("vehicle") ?: "car"
            ExamSetupScreen(
                vehicle = vehicle,
                onStart = { count, all ->
                    navController.navigate(Routes.exam(vehicle, count, all))
                },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.EXAM,
            arguments = listOf(
                navArgument("vehicle") { type = NavType.StringType; defaultValue = "car" },
                navArgument("count") { type = NavType.IntType; defaultValue = -1 },
                navArgument("all") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val vehicle = backStackEntry.arguments?.getString("vehicle") ?: "car"
            val count = backStackEntry.arguments?.getInt("count")?.takeIf { it >= 0 }
            val all = backStackEntry.arguments?.getBoolean("all") ?: false
            ExamScreen(
                onFinish = { score, total ->
                    navController.popBackStack()
                    navController.navigate(Routes.results(score, total))
                },
                desiredCount = count,
                useAll = all,
                vehicle = vehicle,
                onRedirectToBilling = { navController.popBackStack() }
            )
        }
        composable(
            Routes.RESULTS,
            arguments = listOf(
                navArgument("score") { type = NavType.IntType },
                navArgument("total") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val score = backStackEntry.arguments?.getInt("score") ?: 0
            val total = backStackEntry.arguments?.getInt("total") ?: 0
            ResultsScreen(
                score = score,
                total = total,
                onBackHome = { navController.popBackStack(Routes.HOME, inclusive = false) },
                onReview = { navController.navigate(Routes.REVIEW) }
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.REVIEW) {
            ReviewScreen(onBack = { navController.popBackStack() })
        }
    }
}
