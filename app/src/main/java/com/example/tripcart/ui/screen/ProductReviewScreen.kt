package com.example.tripcart.ui.screen

import android.net.Uri
import android.widget.Space
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.tripcart.R
import com.example.tripcart.ui.theme.PrimaryAccent
import com.example.tripcart.ui.theme.PrimaryBackground
import com.example.tripcart.ui.theme.ReviewBackground
import com.example.tripcart.ui.theme.SecondaryBackground
import com.example.tripcart.ui.viewmodel.ProductViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductReviewScreen(
    productId: String,
    onBack: () -> Unit = {},
    onNavigateToWriteReview: () -> Unit = {},
    onNavigateToAddProduct: () -> Unit = {},
    viewModel: ProductViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var expandedImageIndex by remember { mutableStateOf<Pair<Int, List<String>>?>(null) }
    // 상품 정보 및 리뷰 로드
    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
        viewModel.loadReviews(productId)
    }

    // favorite 목록 로드
    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            viewModel.loadFavorites()
        }
    }

    val product = uiState.currentProduct
    val reviews = uiState.reviews
    val isFavorite = uiState.favoriteProductIds.contains(productId)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.White,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "상품 리뷰",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
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
                    actions = {
                        // 찜 버튼
                        IconButton(
                            modifier = Modifier.size(28.dp),
                            onClick = { viewModel.toggleFavorite(productId) }
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isFavorite) "찜 해제" else "찜하기",
                                tint = if (isFavorite) Color(0xFFFF1744) else Color.Gray,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                        // 리스트 추가 버튼
                        IconButton(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(28.dp),
                            onClick = onNavigateToAddProduct
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "리스트에 추가",
                                tint = PrimaryAccent,
                                modifier = Modifier.size(28.dp)
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
            if (product == null) {
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
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    // 상품 정보 섹션
                    item {
                        ProductInfoSection(
                            product = product,
                            // imageIndex: 클릭한 이미지의 인덱스
                            //             - 확대했을 때 바로 보여줄 사진의 index를 전달하는 역할
                            // imageUrls: 전체 이미지 리스트
                            onImageClick = { imageIndex ->
                                expandedImageIndex = Pair(imageIndex, product.imageUrls)
                            }
                        )
                    }

                    // 구분선
                    item {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = Color.Gray.copy(alpha = 0.3f)
                        )
                    }

                    // 리뷰 작성 버튼
                    item {
                        Button(
                            onClick = onNavigateToWriteReview,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryAccent
                            )
                        ) {
                            Text(
                                text = "리뷰 작성하기",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // 리뷰 목록
                    if (uiState.isLoadingReviews) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    } else if (reviews.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "아직 리뷰가 없습니다.",
                                    fontSize = 16.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        items(reviews) { review ->
                            ReviewItem(review = review)
                        }
                    }
                }
            }
        }
        
        // 이미지 확대 오버레이 (Scaffold 위에 표시)
        expandedImageIndex?.let { (initialIndex, images) ->
            if (images.isNotEmpty()) {
                val pagerState = rememberPagerState(
                    initialPage = initialIndex,
                    pageCount = { images.size }
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f))
                        .clickable { expandedImageIndex = null },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.statusBars),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 상단 바 (뒤로가기, 인덱스)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 좌측 뒤로가기 버튼
                            IconButton(
                                onClick = { expandedImageIndex = null },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.arrow_back),
                                    contentDescription = "닫기",
                                    modifier = Modifier.size(24.dp),
                                    colorFilter = ColorFilter.tint(Color.White)
                                )
                            }

                            // 중앙 인덱스 표시
                            Text(
                                text = "${pagerState.currentPage + 1} / ${images.size}",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // 우측 빈 공간 (레이아웃 균형을 위해)
                            Spacer(modifier = Modifier.size(40.dp))
                        }

                        // 이미지 슬라이더
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(bottom = 56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { page ->
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = images[page],
                                        contentDescription = "확대된 이미지",
                                        modifier = Modifier
                                            .fillMaxWidth(0.95f)
                                            .fillMaxHeight(0.9f),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductInfoSection(
        product: com.example.tripcart.ui.viewmodel.SearchedProduct,
        onImageClick: (Int) -> Unit
    ) {
        val averageRating = if (product.reviewCount > 0) {
            product.totalScore / product.reviewCount
        } else {
            0.0
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 좌측: 상품 이미지
            if (product.imageUrls.isNotEmpty()) {
                if (product.imageUrls.size == 1) {
                    // 이미지가 하나일 때
                    AsyncImage(
                        model = product.imageUrls[0],
                        contentDescription = "상품 사진",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onImageClick(0) },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // 이미지가 여러 개일 때는 슬라이드 가능
                    val pagerState = rememberPagerState(pageCount = { product.imageUrls.size })
                    Box(modifier = Modifier.size(120.dp)) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            AsyncImage(
                                model = product.imageUrls[page],
                                contentDescription = "상품 사진",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onImageClick(page) },
                                contentScale = ContentScale.Crop
                            )
                        }

                        // 페이지 인디케이터 (하단에 이미지 index 보여주는 점)
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(product.imageUrls.size) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(
                                            width = if (pagerState.currentPage == index) 8.dp else 6.dp,
                                            height = 6.dp
                                        )
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            if (pagerState.currentPage == index)
                                                Color.White
                                            else
                                                Color.White.copy(alpha = 0.5f)
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // 우측: 상품 정보
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // 상품 이름
                Text(
                    text = product.productName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp
                )

                // 카테고리
                Text(
                    text = product.category,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 평균 별점 및 리뷰 수
                val starColor = if (averageRating > 0) PrimaryBackground else Color.Gray
                Column {
                    // 별 5개 표시
                    Row {
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
                                modifier = Modifier.size(20.dp),
                                tint = tint
                            )
                        }
                    }

                    // 평균 별점 텍스트 및 리뷰 수
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 평균 별점
                        Row(
                            // 별 5개 표시한 거랑 시작점 맞추기 위해 좌측 padding 추가
                            modifier = Modifier.padding(start = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "평균 별점",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                lineHeight = 16.sp
                            )
                            Text(
                                text = String.format("%.1f", averageRating),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryAccent
                            )
                        }

                        // 세로 구분선
                        Text(
                            text = "|",
                            fontSize = 14.sp,
                            color = Color.Gray.copy(alpha = 0.5f)
                        )

                        // 리뷰 수
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "리뷰 개수",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                lineHeight = 16.sp
                            )
                            Text(
                                text = "${product.reviewCount}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryAccent
                            )
                        }
                    }
                }
            }
        }
    }


@Composable
fun ReviewItem(
        review: com.example.tripcart.ui.viewmodel.Review
    ) {
        var isExpanded by remember { mutableStateOf(false) }

        // 현재 사용자 ID 가져오기
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val isMyReview = currentUserId != null && currentUserId == review.userId

        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        val dateString = dateFormat.format(review.createdAt.toDate())

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isMyReview) ReviewBackground else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 별점 및 날짜
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 별점 표시
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        repeat(5) { index ->
                            // 각 별이 끝나는 지점이 1.0, 2.0, 3.0, 4.0, 5.0
                            val starValue = index + 1
                            val reviewStarColor = PrimaryBackground
                            // 개별 리뷰는 소수점이 나올 수 없으므로 반 별 고려 안 해도 됨
                            val (icon, tint) = when {
                                starValue <= review.rating -> Icons.Default.Star to reviewStarColor
                                else -> Icons.Default.Star to Color.Gray.copy(alpha = 0.3f)
                            }

                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = tint
                            )
                        }
                    }

                    // 날짜
                    Text(
                        text = dateString,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // 리뷰 이미지
                if (review.imageUrls.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(review.imageUrls) { imageUrl ->
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "리뷰 이미지",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                // 리뷰 내용
                if (!review.content.isNullOrBlank()) {
                    Text(
                        text = review.content,
                        fontSize = 14.sp,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
