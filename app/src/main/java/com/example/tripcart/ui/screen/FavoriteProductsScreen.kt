package com.example.tripcart.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tripcart.R
import com.example.tripcart.ui.viewmodel.ProductViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteProductsScreen(
    onBack: () -> Unit = {},
    onNavigateToReview: (String) -> Unit = {},
    onNavigateToAddProduct: (String) -> Unit = {},
    viewModel: ProductViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // favorite 목록 로드 및 전체 상품 로드
    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            viewModel.loadFavorites()
            viewModel.loadAllProducts(showLoading = true)
        }
    }
    
    // 찜한 상품 목록
    // remember를 사용해, favoriteProductIds가 변경되면 자동으로 변경사항 업데이트
    val favoriteProducts = remember(uiState.allProducts, uiState.favoriteProductIds) {
        uiState.allProducts.filter { product ->
            uiState.favoriteProductIds.contains(product.productId)
        }
    }
    
    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "찜한 상품",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Image(
                            painter = painterResource(id = R.drawable.arrow_back),
                            contentDescription = "뒤로가기",
                            modifier = Modifier.size(24.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.Black)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (favoriteProducts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "찜한 상품이 없습니다.",
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                        }
                    }
                } else {
                    items(
                        items = favoriteProducts,
                        key = { it.productId }
                    ) { product ->
                        ProductItem(
                            product = product,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            onReviewClick = {
                                onNavigateToReview(product.productId)
                            },
                            onAddClick = {
                                onNavigateToAddProduct(product.productId)
                            },
                            isFavorite = true, // 찜한 상품 화면이므로 항상 true
                            onFavoriteClick = { 
                                viewModel.removeFavorite(product.productId)
                            }
                        )
                    }
                }
            }
        }
    }
}

