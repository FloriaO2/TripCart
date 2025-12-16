package com.example.tripcart.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.tripcart.R
import com.example.tripcart.ui.theme.PrimaryAccent
import com.example.tripcart.ui.theme.PrimaryBackground
import com.example.tripcart.ui.theme.SecondaryBackground
import com.example.tripcart.ui.theme.TagBackground
import com.example.tripcart.ui.theme.TertiaryBackground
import com.example.tripcart.ui.viewmodel.ProductViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AllProductsScreen(
    onBack: () -> Unit = {},
    onNavigateToReview: (String) -> Unit = {},
    onNavigateToAddProduct: (String) -> Unit = {}, // productId 전달한 상태로 상품 추가 페이지로 이동
    viewModel: ProductViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // favorite 목록 로드
    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            viewModel.loadFavorites()
        }
    }
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategories by remember { mutableStateOf<List<String>>(emptyList()) }
    var searchKeyword by remember { mutableStateOf<String?>(null) }
    
    // 화면 진입 시 모든 상품 로드
    LaunchedEffect(Unit) {
        // 처음 진입 시에만 로딩
        viewModel.loadAllProducts(showLoading = uiState.allProducts.isEmpty())
    }
    
    // 필터링된 상품 목록
    val filteredProducts = remember(
        searchKeyword,
        selectedCategories,
        uiState.allProducts
    ) {
        viewModel.getFilteredProducts(searchKeyword, selectedCategories)
    }
    
    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "전체 상품",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Image(
                            painter = painterResource(id = R.drawable.arrow_back),
                            contentDescription = "뒤로가기",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // 카테고리 영역
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(15.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "카테고리",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        // 카테고리 텍스트 버튼들을 한 줄에 넘치지 않도록 분배
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            PRODUCT_CATEGORIES.forEach { category ->
                                val isSelected = selectedCategories.contains(category)
                                // TextButton을 사용하면 기본 할당되는 버튼 height때문에 UI가 이상해져서
                                // Text에 .clickable을 연동해 사용
                                Text(
                                    text = category,
                                    fontSize = 14.sp,
                                    color = if (isSelected) PrimaryAccent else Color.Gray,
                                    modifier = Modifier
                                        .clickable {
                                            selectedCategories = if (isSelected) {
                                                selectedCategories - category
                                            } else {
                                                selectedCategories + category
                                            }
                                        }
                                        .padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
                
                // 검색어 입력창 + 구분선 + 태그/뱃지 영역을 sticky header로 고정
                // 스크롤 내려도 화면 밖으로 사라지지 않고 상단에 고정되도록!
                stickyHeader {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                    ) {
                        Column {
                            // 검색어 입력창
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 16.dp),
                                placeholder = { Text("상품 이름 검색") },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = "검색")
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "지우기")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )

                            // 검색 버튼
                            Button(
                                onClick = {
                                    if (searchQuery.isNotBlank()) {
                                        searchKeyword = searchQuery
                                    } else {
                                        searchKeyword = null
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp) // 상단 입력창과 유사한 높이로 지정
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 8.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryAccent.copy(alpha = 0.9f)
                                )
                            ) {
                                Text("검색", fontSize = 16.sp, color = Color.White)
                            }
                            
                            // 구분선
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 16.dp),
                                thickness = 1.dp,
                                color = Color.Gray.copy(alpha = 0.3f)
                            )
                            
                            // 태그/뱃지 영역
                            if (searchKeyword != null || selectedCategories.isNotEmpty()) {
                                // 태그/뱃지 영역을 한 줄에 넘치지 않도록 분배
                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .padding(bottom = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // let은 한 번만 실행 - 검색어 태그는 최대 1개만 생성 가능
                                    searchKeyword?.let { keyword ->
                                        FilterTag(
                                            text = keyword,
                                            backgroundColor = PrimaryBackground,
                                            onRemove = {
                                                searchKeyword = null
                                                searchQuery = ""
                                            }
                                        )
                                    }
                                    
                                    // forEach는 다중 실행 - 여러 개의 카테고리 태그 생성 가능
                                    selectedCategories.forEach { category ->
                                        FilterTag(
                                            text = category,
                                            backgroundColor = TagBackground,
                                            onRemove = {
                                                selectedCategories = selectedCategories - category
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                    
                    // 상품 목록
                    if (filteredProducts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(400.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val hasFilter = searchKeyword != null || selectedCategories.isNotEmpty()
                                Text(
                                    text = if (hasFilter) {
                                        "해당하는 상품이 없습니다."
                                    } else {
                                        "공개된 상품이 없습니다."
                                    },
                                    color = Color.Gray,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    } else {
                        items(
                            items = filteredProducts,
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
                                isFavorite = uiState.favoriteProductIds.contains(product.productId),
                                onFavoriteClick = { viewModel.toggleFavorite(product.productId) }
                            )
                        }
                    }
                }
            }
        }
    }


@Composable
fun FilterTag(
    text: String,
    backgroundColor: Color,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(16.dp)),
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
                color = Color.Black
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "제거",
                    modifier = Modifier.size(16.dp),
                    tint = Color.Black
                )
            }
        }
    }
}

@Composable
fun ProductItem(
    product: com.example.tripcart.ui.viewmodel.SearchedProduct,
    modifier: Modifier = Modifier,
    onReviewClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {}
) {
    
    // 평균 별점 계산
    val averageRating = if (product.reviewCount > 0) {
        product.totalScore / product.reviewCount
    } else {
        0.0
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onReviewClick), // 전체 카드 클릭시 리뷰 페이지로 이동
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 좌측 - 상품 이미지
            if (product.imageUrls.isNotEmpty()) {
                AsyncImage(
                    model = product.imageUrls[0],
                    contentDescription = product.productName,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "이미지 없음",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
            
            // 우측 - 상품 정보
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp) // 이미지 높이와 동일하게 설정
            ) {
                // 상단 - 평균 별점 시각화 + 평균 별점 텍스트 및 리뷰 개수 + 전체 리뷰 보러가기 텍스트 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 평균 별점 시각화 + 평균 별점 텍스트 및 리뷰 개수
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 평균 별점 시각화
                        val starColor = if (averageRating > 0) PrimaryBackground else Color.Gray
                        Row{
                            repeat(5) { index ->
                                // 각 별이 끝나는 지점이 1.0, 2.0, 3.0, 4.0, 5.0
                                val starValue = index + 1.0
                                // 평균 별점이 0보다 크고,
                                // 각 별이 시작하는 지점이 평균 별점보다 작으며,
                                // 각 별이 끝나는 지점이 평균 별점보다 크면 반 별 처리
                                val isHalfStar = averageRating > 0 && 
                                               starValue - 1.0 < averageRating && 
                                               averageRating < starValue
                                
                                val (icon, tint) = when {
                                    starValue <= averageRating -> Icons.Default.Star to starColor // 꽉 찬 별
                                    isHalfStar -> Icons.Default.StarHalf to starColor // 반 별
                                    else -> Icons.Default.Star to Color.Gray.copy(alpha = 0.3f) // 빈 별
                                }
                                
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = tint
                                )
                            }
                        }
                        
                        // 평균 별점 텍스트 및 리뷰 개수
                        Text(
                            text = if (product.reviewCount > 0) {
                                String.format("%.1f (%d)", averageRating, product.reviewCount)
                            } else {
                                "0.0 (0)"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (averageRating > 0) PrimaryBackground else Color.Gray
                        )
                    }
                    
                    // 전체 리뷰 보러가기 텍스트
                    Text(
                        text = "all reviews",
                        fontSize = 12.sp,
                        color = PrimaryAccent
                    )
                }
                
                // 하단 - 좌측은 상품명 및 카테고리 + 우측은 아이콘 버튼들
                // 상단 별점 관련 Row를 제외한 나머지 공간에서 세로 기준 중앙정렬
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 좌측 - 상품명 및 카테고리
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = product.productName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2
                        )
                        
                        Text(
                            text = product.category,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    
                    // 우측 - 하트 아이콘 버튼 + 리스트 추가 아이콘 버튼
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 하트 아이콘 버튼
                        IconButton(
                            onClick = onFavoriteClick,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isFavorite) "찜 해제" else "찜하기",
                                tint = if (isFavorite) Color(0xFFFF1744) else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        // 리스트 추가 아이콘 버튼
                        IconButton(
                            onClick = onAddClick,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "리스트에 추가",
                                tint = PrimaryAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

