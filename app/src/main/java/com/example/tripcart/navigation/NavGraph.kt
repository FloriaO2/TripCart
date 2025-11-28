package com.example.tripcart.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tripcart.ui.screen.HomeScreen
import com.example.tripcart.ui.screen.ListScreen
import com.example.tripcart.ui.screen.LoginScreen
import com.example.tripcart.ui.screen.MapScreen
import com.example.tripcart.ui.screen.MyPageScreen
import com.example.tripcart.ui.screen.PlaceSearchScreen
import com.google.firebase.auth.FirebaseAuth

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object List : Screen("list")
    object Map : Screen("map")
    object MyPage : Screen("my_page")
    object PlaceSearch : Screen("place_search")
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
                        // 로그인 중인 계정이 존재한다면 홈화면 이후에만 머물도록!
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToRoute = { route ->
                    when (route) {
                        Screen.Home.route, "active_list" -> {
                            // 이미 Home 화면이면 이동하지 않음
                            if (navController.currentDestination?.route != Screen.Home.route) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                }
                            }
                        }
                        Screen.List.route -> {
                            navController.navigate(Screen.List.route)
                        }
                        Screen.Map.route -> {
                            navController.navigate(Screen.Map.route)
                        }
                        Screen.MyPage.route -> {
                            navController.navigate(Screen.MyPage.route)
                        }
                        // TODO: 다른 라우트들도 추가
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.List.route) {
            ListScreen(
                onNavigateToRoute = { route ->
                    when (route) {
                        Screen.Home.route, "active_list" -> {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.List.route) { inclusive = false }
                            }
                        }
                        Screen.List.route -> {
                            // 이미 List 화면이면 이동하지 않음
                        }
                        Screen.Map.route -> {
                            navController.navigate(Screen.Map.route) {
                                popUpTo(Screen.List.route) { inclusive = false }
                            }
                        }
                        Screen.MyPage.route -> {
                            navController.navigate(Screen.MyPage.route) {
                                popUpTo(Screen.List.route) { inclusive = false }
                            }
                        }
                        // TODO: 다른 라우트들도 추가
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.List.route) { inclusive = false }
                    }
                },
                onAddClick = {
                    // TODO: 상품 추가하기 기능 구현
                },
                onNavigateToPlaceSearch = {
                    navController.navigate(Screen.PlaceSearch.route)
                }
            )
        }
        
        composable(Screen.PlaceSearch.route) {
            PlaceSearchScreen(
                onBack = {
                    navController.popBackStack()
                },
                onPlaceSelected = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Map.route) {
            MapScreen(
                onNavigateToRoute = { route ->
                    when (route) {
                        Screen.Home.route, "active_list" -> {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Map.route) { inclusive = false }
                            }
                        }
                        Screen.List.route -> {
                            navController.navigate(Screen.List.route) {
                                popUpTo(Screen.Map.route) { inclusive = false }
                            }
                        }
                        Screen.Map.route -> {
                            // 이미 Map 화면이면 이동하지 않음
                        }
                        Screen.MyPage.route -> {
                            navController.navigate(Screen.MyPage.route) {
                                popUpTo(Screen.Map.route) { inclusive = false }
                            }
                        }
                        // TODO: 다른 라우트들도 추가
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Map.route) { inclusive = false }
                    }
                }
            )
        }
        
        composable(Screen.MyPage.route) {
            MyPageScreen(
                onSignOut = {
                    auth.signOut()
                    navController.navigate(Screen.Login.route) {
                        // 마이페이지와 홈 화면으로 돌아가지 않도록 백 스택 제거
                        // 로그인 중인 계정이 존재하지 않는다면 로그인 화면에만 머물도록!
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToRoute = { route ->
                    when (route) {
                        Screen.Home.route, "active_list" -> {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.MyPage.route) { inclusive = true }
                            }
                        }
                        Screen.List.route -> {
                            navController.navigate(Screen.List.route) {
                                popUpTo(Screen.MyPage.route) { inclusive = false }
                            }
                        }
                        Screen.Map.route -> {
                            navController.navigate(Screen.Map.route) {
                                popUpTo(Screen.MyPage.route) { inclusive = false }
                            }
                        }
                        // TODO: 다른 라우트들도 추가
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.MyPage.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

