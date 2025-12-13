package com.example.tripcart.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tripcart.ui.screen.AddProductScreen
import com.example.tripcart.ui.screen.HomeScreen
import com.example.tripcart.ui.screen.ListScreen
import com.example.tripcart.ui.screen.ListDetailScreen
import com.example.tripcart.ui.screen.LoginScreen
import com.example.tripcart.ui.screen.MapScreen
import com.example.tripcart.ui.screen.MyPageScreen
import com.example.tripcart.ui.screen.AddPlaceScreen
import com.example.tripcart.ui.screen.AddPlaceToListScreen
import com.example.tripcart.ui.screen.AddProductToListScreen
import com.example.tripcart.ui.screen.RankingScreen
import com.example.tripcart.ui.screen.RankingDetailScreen
import com.example.tripcart.ui.viewmodel.ProductViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tripcart.ui.viewmodel.PlaceViewModel
import com.example.tripcart.ui.viewmodel.PlaceDetails
import com.example.tripcart.ui.viewmodel.ListViewModel
import com.example.tripcart.ui.viewmodel.RankingViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object List : Screen("list")
    object ListDetail : Screen("list_detail/{listId}") {
        // 지도에서 리스트 상세 페이지로 이동할 때 사용!
        fun createRoute(listId: String) = "list_detail/$listId"
    }
    object Map : Screen("map")
    object Ranking : Screen("ranking")
    object RankingDetail : Screen("ranking_detail") {
        fun createRoute(country: String? = null) = if (country != null) "ranking_detail/$country" else "ranking_detail"
    }
    object RankingDetailWithCountry : Screen("ranking_detail/{country}") {
        fun createRoute(country: String) = "ranking_detail/$country"
    }
    object MyPage : Screen("my_page")
    object AddPlace : Screen("add_place")
    object AddProduct : Screen("add_product")
    object AddPlaceToList : Screen("add_place_to_list")
    object AddProductToList : Screen("add_product_to_list")
}

@Composable
fun TripCartNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Login.route,
    listViewModel: ListViewModel? = null, // 외부에서 전달받거나 내부에서 생성
    onRequestNotificationPermission: () -> Unit = {}, // 알림 권한 요청 콜백
    onRequestBackgroundLocationPermission: () -> Unit = {}, // 백그라운드 위치 권한 요청 콜백
    shouldNavigateToMap: Boolean = false, // MapScreen으로 이동해야 하는지 여부
    onNavigateToMapComplete: () -> Unit = {} // MapScreen으로 이동 완료 후 호출되는 콜백
) {
    val auth = FirebaseAuth.getInstance()
    
    // ListViewModel을 NavGraph 레벨에서 생성하여 공유 (전달받지 않은 경우)
    val sharedListViewModel: ListViewModel = listViewModel ?: viewModel()
    
    // PlaceViewModel도 NavGraph 레벨에서 생성하여 공유
    val sharedPlaceViewModel: PlaceViewModel = viewModel()
    
    // ProductViewModel도 NavGraph 레벨에서 생성하여 공유
    val sharedProductViewModel: ProductViewModel = viewModel()
    
    // RankingViewModel도 NavGraph 레벨에서 생성하여 공유
    val sharedRankingViewModel: RankingViewModel = viewModel()
    
    // 로그인 상태 확인을 거쳐 시작 화면 결정 (재계산 방지)
    // NavHost의 startDestination은 컴포지션 동안 변경되면 안 되므로 remember로 고정
    val initialRoute = remember {
        if (auth.currentUser != null) {
            Screen.Home.route
        } else {
            Screen.Login.route
        }
    }
    
    // MapScreen으로 이동해야 하는 경우
    LaunchedEffect(shouldNavigateToMap) {
        if (shouldNavigateToMap && auth.currentUser != null) {
            // 로그인된 상태에서만 MapScreen으로 이동
            navController.navigate(Screen.Map.route) {
                // 백 스택을 모두 제거해서 MapScreen을 루트로 설정
                popUpTo(0) { inclusive = true }
            }
            // 이동 완료 후 콜백을 호출해 shouldNavigateToMapState 값을 false로 리셋
            onNavigateToMapComplete()
        }
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
                        Screen.Ranking.route -> {
                            navController.navigate(Screen.Ranking.route)
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
                        Screen.Ranking.route -> {
                            navController.navigate(Screen.Ranking.route) {
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
                    navController.navigate(Screen.AddProduct.route)
                },
                onNavigateToPlaceSearch = {
                    navController.navigate(Screen.AddPlace.route)
                },
                onNavigateToListDetail = { listId ->
                    navController.navigate(Screen.ListDetail.createRoute(listId))
                },
                viewModel = sharedListViewModel
            )
        }
        
        composable(
            route = Screen.ListDetail.route,
            arguments = listOf(
                navArgument("listId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getString("listId") ?: ""
            ListDetailScreen(
                listId = listId,
                onBack = {
                    navController.popBackStack()
                },
                onEditList = {
                    // TODO: 리스트 편집 화면으로 이동
                },
                onEditProduct = { productId, listId ->
                    // TODO: 상품 편집 화면으로 이동
                },
                onGroupAddClick = {
                    // TODO: 그룹 추가 기능 구현
                },
                viewModel = sharedListViewModel
            )
        }
        
        composable(Screen.AddPlace.route) {
            AddPlaceScreen(
                onBack = {
                    navController.popBackStack()
                },
                onPlaceSelected = { placeDetails: PlaceDetails ->
                    // PlaceViewModel에 선택한 장소가 이미 저장되어 있음 (checkPlaceAndNavigate에서 저장)
                    // AddPlaceToListScreen에서 같은 ViewModel 인스턴스를 사용하여 접근
                    navController.navigate(Screen.AddPlaceToList.route)
                },
                viewModel = sharedPlaceViewModel
            )
        }
        
        composable(Screen.AddPlaceToList.route) {
            // AddPlaceScreen과 같은 ViewModel 인스턴스 사용
            // selectedPlace가 설정될 때까지 대기
            val placeUiState by sharedPlaceViewModel.uiState.collectAsState()
            val placeDetails = placeUiState.selectedPlace
            
            if (placeDetails == null) {
                // 장소가 아직 선택되지 않았으면 로딩 표시
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                AddPlaceToListScreen(
                    placeDetails = placeDetails,
                    onBack = {
                        navController.popBackStack()
                    },
                    onComplete = {
                        // 리스트 선택 완료 후 ListScreen으로 이동
                        navController.navigate(Screen.List.route) {
                            // 뒤로가기 스택에서 AddPlaceToList와 AddPlace 화면 제거
                            popUpTo(Screen.List.route) {
                                inclusive = false
                            }
                        }
                    },
                    listViewModel = sharedListViewModel, // NavGraph 레벨에서 생성한 ViewModel 공유
                    placeViewModel = sharedPlaceViewModel
                )
            }
        }
        
        composable(Screen.AddProduct.route) {
            AddProductScreen(
                onBack = {
                    // 뒤로가기 시 draftProduct 초기화하여 AddProductScreen 내부 데이터 리셋
                    sharedProductViewModel.clearDraftProduct()
                    navController.popBackStack()
                },
                onProductSaved = { productDetails ->
                    // 상품 저장 후 AddProductToListScreen으로 이동
                    // ProductViewModel에 저장된 상품 정보를 사용
                    navController.navigate(Screen.AddProductToList.route)
                },
                viewModel = sharedProductViewModel
            )
        }
        
        composable(Screen.AddProductToList.route) {
            // ProductViewModel에서 저장된 상품 정보 가져오기
            val productUiState by sharedProductViewModel.uiState.collectAsState()
            val productDetails = productUiState.savedProduct
            
            // ListScreen에서 AddProductScreen 진입하면 잔여 데이터 없는 새 화면을 띄워줌
            var isInitialLoad by remember { mutableStateOf<Boolean>(true) }
            LaunchedEffect(Unit) {
                if (isInitialLoad && productDetails == null) {
                    navController.popBackStack()
                }
                isInitialLoad = false
            }
            
            if (productDetails == null && isInitialLoad) {
                // 상품이 아직 저장되지 않았으면 로딩 표시
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (productDetails != null) {
                AddProductToListScreen(
                    productDetails = productDetails,
                    onBack = {
                        // 뒤로가기 시 draftProduct 데이터를 이용해 AddProductScreen에서의 입력 데이터를 복원
                        // savedProduct는 Firebase Storage에 업로드된 이미지 url을 포함한 모든 정보를 가지고 있음
                        navController.popBackStack()
                    },
                    onComplete = {
                        // 리스트 선택 완료 후 ListScreen으로 이동
                        // ProductViewModel의 savedProduct와 draftProduct 초기화
                        sharedProductViewModel.clearSuccess()
                        sharedProductViewModel.clearDraftProduct()
                        navController.navigate(Screen.List.route) {
                            // 뒤로가기 스택에서 AddProductToList와 AddProduct 화면 제거
                            popUpTo(Screen.List.route) {
                                inclusive = false
                            }
                        }
                    },
                    listViewModel = sharedListViewModel,
                    productViewModel = sharedProductViewModel
                )
            }
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
                        Screen.Ranking.route -> {
                            navController.navigate(Screen.Ranking.route) {
                                popUpTo(Screen.Map.route) { inclusive = false }
                            }
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
                },
                onNavigateToListDetail = { listId ->
                    navController.navigate(Screen.ListDetail.createRoute(listId))
                },
                listViewModel = sharedListViewModel
            )
        }
        
        composable(Screen.Ranking.route) {
            RankingScreen(
                onNavigateToRoute = { route ->
                    when (route) {
                        Screen.Home.route, "active_list" -> {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Ranking.route) { inclusive = false }
                            }
                        }
                        Screen.List.route -> {
                            navController.navigate(Screen.List.route) {
                                popUpTo(Screen.Ranking.route) { inclusive = false }
                            }
                        }
                        Screen.Map.route -> {
                            navController.navigate(Screen.Map.route) {
                                popUpTo(Screen.Ranking.route) { inclusive = false }
                            }
                        }
                        Screen.Ranking.route -> {
                            // 이미 Ranking 화면이면 이동하지 않음
                        }
                        Screen.MyPage.route -> {
                            navController.navigate(Screen.MyPage.route) {
                                popUpTo(Screen.Ranking.route) { inclusive = false }
                            }
                        }
                        // TODO: 다른 라우트들도 추가
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Ranking.route) { inclusive = false }
                    }
                },
                onNavigateToDetail = {
                    navController.navigate(Screen.RankingDetail.route)
                },
                // TOP3 국가 - 전체 상품 보기 텍스트 버튼을 통해 이동하는 페이지
                onNavigateToCountryDetail = { country ->
                    navController.navigate(Screen.RankingDetailWithCountry.createRoute(country))
                },
                viewModel = sharedRankingViewModel
            )
        }
        
        composable(Screen.RankingDetail.route) {
            RankingDetailScreen(
                selectedCountry = null,
                onBack = {
                    navController.popBackStack()
                },
                rankingViewModel = sharedRankingViewModel,
                placeViewModel = sharedPlaceViewModel
            )
        }
        
        composable(
            route = Screen.RankingDetailWithCountry.route,
            arguments = listOf(
                navArgument("country") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val country = backStackEntry.arguments?.getString("country")
            RankingDetailScreen(
                selectedCountry = country,
                onBack = {
                    navController.popBackStack()
                },
                rankingViewModel = sharedRankingViewModel,
                placeViewModel = sharedPlaceViewModel
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
                onRequestNotificationPermission = onRequestNotificationPermission,
                onRequestBackgroundLocationPermission = onRequestBackgroundLocationPermission,
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
                        Screen.Ranking.route -> {
                            navController.navigate(Screen.Ranking.route) {
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

