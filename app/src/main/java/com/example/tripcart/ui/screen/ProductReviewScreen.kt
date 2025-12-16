package com.example.tripcart.ui.screen

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    viewModel: ProductViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var expandedImageUrl by remember { mutableStateOf<String?>(null) }
    
    // 상품 정보 및 리뷰 로드
    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
        viewModel.loadReviews(productId)
    }
    
    val product = uiState.currentProduct
    val reviews = uiState.reviews
    
    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "상품 리뷰",
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
                        onImageClick = { imageUrl ->
                            expandedImageUrl = imageUrl
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
        
        // 이미지 확대 다이얼로그
        expandedImageUrl?.let { imageUrl ->
            Dialog(onDismissRequest = { expandedImageUrl = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.9f))
                        .clickable { expandedImageUrl = null },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "확대된 이미지",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable { expandedImageUrl = null },
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
fun ProductInfoSection(
    product: com.example.tripcart.ui.viewmodel.SearchedProduct,
    onImageClick: (String) -> Unit
) {
    val averageRating = if (product.reviewCount > 0) {
        product.totalScore / product.reviewCount
    } else {
        0.0
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 상품 이미지들
        if (product.imageUrls.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(product.imageUrls) { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "상품 사진",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onImageClick(imageUrl) },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        
        // 상품 이름
        Text(
            text = product.productName,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        // 카테고리
        Text(
            text = product.category,
            fontSize = 14.sp,
            color = Color.Gray
        )
        
        // 평균 별점 및 리뷰 수
        val starColor = if (averageRating > 0) PrimaryBackground else Color.Gray
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 별 5개 표시
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
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

            // 평균 별점 텍스트
            Text(
                text = String.format("%.1f", averageRating),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryAccent
            )

            // 리뷰 수
            Text(
                text = "(리뷰 ${product.reviewCount}개)",
                fontSize = 14.sp,
                color = Color.Gray
            )
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
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

