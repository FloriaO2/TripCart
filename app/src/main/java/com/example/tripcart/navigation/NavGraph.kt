package com.example.tripcart.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tripcart.ui.screen.HomeScreen
import com.example.tripcart.ui.screen.LoginScreen
import com.google.firebase.auth.FirebaseAuth

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
}

@Composable
fun TripCartNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Login.route
) {
    val auth = FirebaseAuth.getInstance()
    
    // 로그인 상태 확인을 거쳐 시작 화면 결정
    val initialRoute = if (auth.currentUser != null) {
        Screen.Home.route
    } else {
        Screen.Login.route
    }
    
    NavHost(
        navController = navController,
        startDestination = initialRoute,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onSignInSuccess = {
                    navController.navigate(Screen.Home.route) {
                        // 로그인 화면으로 돌아가지 않도록 백 스택 제거
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Home.route) {
            HomeScreen(
                onSignOut = {
                    auth.signOut()
                    navController.navigate(Screen.Login.route) {
                        // 홈 화면으로 돌아가지 않도록 백 스택 제거
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

