package com.example.tripcart.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.tripcart.R
import com.example.tripcart.ui.components.AppBottomBar
import com.example.tripcart.ui.components.AppTopBar
import com.example.tripcart.ui.theme.PrimaryAccent
import com.example.tripcart.ui.theme.PrimaryBackground
import com.example.tripcart.ui.viewmodel.RankingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    onNavigateToRoute: (String) -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToDetail: () -> Unit = {},
    onNavigateToCountryDetail: (String) -> Unit = {}, // TOP3 국가 - 전체 상품 보기 텍스트 버튼을 통해
                                                      // 이동하는 페이지
    onNavigateToAllProducts: () -> Unit = {}, // 전체 상품 모아보기 버튼을 통해 이동하는 페이지
    viewModel: RankingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        containerColor = Color.White,
        topBar = {
            AppTopBar(
                title = "랭킹",
                onNotificationClick = {
                    // TODO: 알림 기능 구현
                },
                onLogoClick = onNavigateToHome
            )
        },
        bottomBar = {
            AppBottomBar(
                currentRoute = "ranking",
                onItemClick = onNavigateToRoute
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // 상품 랭킹 보러가기 버튼 + 상품별 리뷰 모아보기 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 상품 랭킹 보러가기 버튼 (파란 배경 + 흰색 글씨)
                val rankingInteractionSource = remember { MutableInteractionSource() }
                val isRankingPressed = rankingInteractionSource.collectIsPressedAsState().value
                
                Button(
                    onClick = onNavigateToDetail,
                    modifier = Modifier
                        .weight(1f)
                        .height(90.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRankingPressed) Color(0xFF1565C0) else Color(0xFF1976D2)
                    ),
                    interactionSource = rankingInteractionSource
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "국가별 랭킹",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 17.sp
                        )
                        Text(
                            text = "보러가기",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White,
                            lineHeight = 12.sp
                        )
                    }
                }
                
                // 전체 상품 모아보기 버튼 (녹색 배경 + 흰색 글씨)
                val reviewInteractionSource = remember { MutableInteractionSource() }
                val isReviewPressed = reviewInteractionSource.collectIsPressedAsState().value
                
                Button(
                    onClick = onNavigateToAllProducts,
                    modifier = Modifier
                        .weight(1f)
                        .height(90.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isReviewPressed) Color(0xFF1B5E20) else Color(0xFF2E7D32)
                    ),
                    interactionSource = reviewInteractionSource
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "전체 상품",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 17.sp
                        )
                        Text(
                            text = "모아보기",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
            
            // TOP3 제목
            Text(
                text = "지금 가장 많이 찾는 여행지 TOP3!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 16.dp)
            )
            
            // 로딩 상태
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // 국가별 박스들
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(
                        items = uiState.topCountries,
                        key = { _, countryRanking -> countryRanking.country } // country를 고유키로 사용
                    ) { index, countryRanking ->
                        CountryRankingBox(
                            country = countryRanking.country,
                            rank = index + 1,
                            products = uiState.countryProducts[countryRanking.country] ?: emptyList(),
                            onViewAllClick = {
                                onNavigateToCountryDetail(countryRanking.country)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CountryRankingBox(
    country: String,
    rank: Int,
    products: List<com.example.tripcart.ui.viewmodel.ProductRanking>,
    onViewAllClick: () -> Unit
) {
    val medalEmoji = when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> ""
    }
    
    val boxColor = when (rank) {
        1 -> Color(0x80FFD700) // 반투명 금색 배경
        2 -> Color(0x80C0C0C0) // 반투명 은색 배경
        3 -> Color(0x80CD7F32) // 반투명 동색 배경
        else -> Color(0xFFF5F5F5)
    }
    
    val borderColor = when (rank) {
        1 -> Color(0xFFFFD700) // 진한 금색 테두리
        2 -> Color(0xFFC0C0C0) // 진한 은색 테두리
        3 -> Color(0xFFCD7F32) // 진한 동색 테두리
        else -> Color(0xFFE0E0E0)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = boxColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // 국가 이름 + 전체 보기 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // 상단 부분에 반투명 배경색 덧대서 상품 나열된 부분이랑 구분!
                    .background(
                        color = when (rank) {
                            1 -> Color(0x60FFD700) // 반투명 금색 배경
                            2 -> Color(0x60C0C0C0) // 반투명 은색 배경
                            3 -> Color(0x60CD7F32) // 반투명 동색 배경
                            else -> Color(0x40E0E0E0)
                        },
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    )
                    .padding(start = 16.dp, end = 16.dp, top = 5.dp, bottom = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = medalEmoji,
                        fontSize = 20.sp
                    )
                    Text(
                        text = country,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                TextButton(onClick = onViewAllClick) {
                    Text("전체 상품 보기", fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 상품 LazyRow
            if (products.isNotEmpty()) {
                // 같은 count 값을 가진 상품들은 같은 순위로 계산
                val ranks = remember(products) {
                    if (products.isEmpty()) return@remember emptyList<Int>()
                    val rankList = mutableListOf<Int>()
                    rankList.add(1) // 첫번째는 항상 1위
                    
                    for (i in 1 until products.size) {
                        if (products[i].count == products[i-1].count) {
                            rankList.add(rankList[i-1]) // 같은 count면 같은 순위
                        } else {
                            rankList.add(i + 1) // 다른 count면 다음 순위
                        }
                    }
                    rankList
                }
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp
                    )
                ) {
                    itemsIndexed(
                        items = products,
                        key = { _, product -> product.productId }
                    ) { index, product ->
                        ProductRankingItem(
                            product = product,
                            rank = ranks[index]
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "상품이 없습니다.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ProductRankingItem(
    product: com.example.tripcart.ui.viewmodel.ProductRanking,
    rank: Int
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .height(180.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 순위 뱃지
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        when (rank) {
                            1 -> Color(0xFFFFD700) // 금색
                            2 -> Color(0xFFC0C0C0) // 은색
                            3 -> Color(0xFFCD7F32) // 동색
                            else -> Color.Black
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.toString(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 상품 이미지
            if (product.imageUrls.isNotEmpty()) {
                AsyncImage(
                    model = product.imageUrls[0], // 이미지가 여러개 저장돼있어도
                                                  // 상품별로 첫번째 사진만 보여줌
                    contentDescription = product.productName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(4.dp))
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
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // 상품 이름
            Text(
                text = product.productName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
                lineHeight = 16.sp
            )

            // 카테고리
            Text(
                text = product.category,
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                lineHeight = 10.sp
            )
        }
    }
}

