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
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.tripcart.ui.viewmodel.NotificationViewModel
import com.example.tripcart.util.SetStatusBarColor

// 국가 이름과 국기 이모티콘 매핑
private val countryFlagMap = mapOf(
"아프가니스탄" to "🇦🇫",
"올란드 제도" to "🇦🇽",
"알바니아" to "🇦🇱",
"알제리" to "🇩🇿",
"아메리칸 사모아" to "🇦🇸",
"안도라" to "🇦🇩",
"앙골라" to "🇦🇴",
"앵귈라" to "🇦🇮",
"남극" to "🇦🇶",
"앤티가 바부다" to "🇦🇬",
"아르헨티나" to "🇦🇷",
"아르메니아" to "🇦🇲",
"아루바" to "🇦🇼",
"호주" to "🇦🇺",
"오스트리아" to "🇦🇹",
"아제르바이잔" to "🇦🇿",

"바하마" to "🇧🇸",
"바레인" to "🇧🇭",
"방글라데시" to "🇧🇩",
"바베이도스" to "🇧🇧",
"벨라루스" to "🇧🇾",
"벨기에" to "🇧🇪",
"벨리즈" to "🇧🇿",
"베냉" to "🇧🇯",
"버뮤다" to "🇧🇲",
"부탄" to "🇧🇹",
"볼리비아" to "🇧🇴",
"카리브 네덜란드" to "🇧🇶",
"보스니아 헤르체고비나" to "🇧🇦",
"보츠와나" to "🇧🇼",
"부베 섬" to "🇧🇻",
"브라질" to "🇧🇷",
"브루나이" to "🇧🇳",
"불가리아" to "🇧🇬",
"부르키나파소" to "🇧🇫",
"부룬디" to "🇧🇮",

"캄보디아" to "🇰🇭",
"카메룬" to "🇨🇲",
"캐나다" to "🇨🇦",
"카보베르데" to "🇨🇻",
"케이맨 제도" to "🇰🇾",
"중앙아프리카공화국" to "🇨🇫",
"차드" to "🇹🇩",
"칠레" to "🇨🇱",
"중국" to "🇨🇳",
"크리스마스 섬" to "🇨🇽",
"코코스 제도" to "🇨🇨",
"콜롬비아" to "🇨🇴",
"코모로" to "🇰🇲",
"콩고공화국" to "🇨🇬",
"콩고민주공화국" to "🇨🇩",
"쿡 제도" to "🇨🇰",
"코스타리카" to "🇨🇷",
"코트디부아르" to "🇨🇮",
"크로아티아" to "🇭🇷",
"쿠바" to "🇨🇺",
"퀴라소" to "🇨🇼",
"키프로스" to "🇨🇾",
"체코" to "🇨🇿",

"덴마크" to "🇩🇰",
"지부티" to "🇩🇯",
"도미니카 연방" to "🇩🇲",
"도미니카 공화국" to "🇩🇴",

"에콰도르" to "🇪🇨",
"이집트" to "🇪🇬",
"엘살바도르" to "🇸🇻",
"적도기니" to "🇬🇶",
"에리트레아" to "🇪🇷",
"에스토니아" to "🇪🇪",
"에스와티니" to "🇸🇿",
"에티오피아" to "🇪🇹",

"포클랜드 제도" to "🇫🇰",
"페로 제도" to "🇫🇴",
"피지" to "🇫🇯",
"핀란드" to "🇫🇮",
"프랑스" to "🇫🇷",
"프랑스령 기아나" to "🇬🇫",
"프랑스령 폴리네시아" to "🇵🇫",
"프랑스 남부와 남극 지역" to "🇹🇫",

"가봉" to "🇬🇦",
"감비아" to "🇬🇲",
"조지아" to "🇬🇪",
"독일" to "🇩🇪",
"가나" to "🇬🇭",
"지브롤터" to "🇬🇮",
"그리스" to "🇬🇷",
"그린란드" to "🇬🇱",
"그레나다" to "🇬🇩",
"과들루프" to "🇬🇵",
"괌" to "🇬🇺",
"과테말라" to "🇬🇹",
"건지섬" to "🇬🇬",
"기니" to "🇬🇳",
"기니비사우" to "🇬🇼",
"가이아나" to "🇬🇾",

"아이티" to "🇭🇹",
"허드 맥도널드 제도" to "🇭🇲",
"바티칸 시국" to "🇻🇦",
"온두라스" to "🇭🇳",
"홍콩" to "🇭🇰",
"헝가리" to "🇭🇺",

"아이슬란드" to "🇮🇸",
"인도" to "🇮🇳",
"인도네시아" to "🇮🇩",
"이란" to "🇮🇷",
"이라크" to "🇮🇶",
"아일랜드" to "🇮🇪",
"맨섬" to "🇮🇲",
"이스라엘" to "🇮🇱",
"이탈리아" to "🇮🇹",

"일본" to "🇯🇵",
"대한민국" to "🇰🇷",
"북한" to "🇰🇵",
"대만" to "🇹🇼",

"영국" to "🇬🇧",
"미국" to "🇺🇸",
"베트남" to "🇻🇳",
"싱가포르" to "🇸🇬",
"태국" to "🇹🇭",
"필리핀" to "🇵🇭",
"말레이시아" to "🇲🇾",
"뉴질랜드" to "🇳🇿",
"남아프리카공화국" to "🇿🇦",
"짐바브웨" to "🇿🇼"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    onNavigateToRoute: (String) -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToDetail: () -> Unit = {},
    onNavigateToCountryDetail: (String) -> Unit = {}, // TOP3 국가 - 전체 상품 보기 텍스트 버튼을 통해
                                                      // 이동하는 페이지
    onNavigateToAllProducts: () -> Unit = {}, // 전체 상품 모아보기 버튼을 통해 이동하는 페이지
    onNavigateToNotification: () -> Unit = {},
    onNavigateToReview: (String) -> Unit = {}, // 상품 리뷰 페이지로 이동
    viewModel: RankingViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val notificationState by notificationViewModel.uiState.collectAsState()
    
    // 상태바 색상을 상단바와 동일하게 설정
    SetStatusBarColor(
        statusBarColor = Color.White,
        isLightStatusBars = true
    )
    
    Scaffold(
        containerColor = Color.White,
        topBar = {
            AppTopBar(
                title = "랭킹",
                onNotificationClick = onNavigateToNotification,
                onLogoClick = onNavigateToHome,
                unreadNotificationCount = notificationState.unreadCount
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
                // 국가별 인기 상품 제목
                Text(
                    text = "🏆 인기 국가별 추천 상품 🏆",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp, top = 0.dp, bottom = 16.dp)
                )

                // 국가별 박스들
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        items = uiState.topCountries,
                        key = { _, countryRanking -> countryRanking.country } // country를 고유키로 사용
                    ) { index, countryRanking ->
                        CountryRankingBox(
                            country = countryRanking.country,
                            rank = index + 1,
                            products = getTopProductsWithSameRank(
                                uiState.countryProducts[countryRanking.country] ?: emptyList(),
                                maxCount = 5 // 기준 상품의 위치: 이 위치의 상품 순위까지 모두 포함
                            ),
                            onViewAllClick = {
                                onNavigateToCountryDetail(countryRanking.country)
                            },
                            onNavigateToReview = onNavigateToReview
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
    onViewAllClick: () -> Unit,
    onNavigateToReview: (String) -> Unit = {}
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
                    // 국기 이모티콘 표시 (매핑에 없으면 표시하지 않음)
                    countryFlagMap[country]?.let { flag ->
                        Text(
                            text = flag,
                            fontSize = 20.sp
                        )
                    }
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
                            rank = ranks[index],
                            onClick = { onNavigateToReview(product.productId) }
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
    rank: Int,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .height(180.dp)
            .clickable(onClick = onClick),
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
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
                lineHeight = 14.sp
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

// 기준 상품의 순위까지 모두 포함하여 반환
// maxCount: 기준이 되는 상품의 위치 (maxCount번째 상품의 순위까지 모두 포함)
// products는 count 기준 내림차순 정렬
private fun getTopProductsWithSameRank(
    products: List<com.example.tripcart.ui.viewmodel.ProductRanking>,
    maxCount: Int = 5  // 기준 상품의 위치: 이 위치의 상품 순위까지 모두 포함
): List<com.example.tripcart.ui.viewmodel.ProductRanking> {
    if (products.isEmpty() || maxCount <= 0) return emptyList()
    
    // 기준 상품 위치(maxCount번째)까지 가져와서 그 상품의 count 값을 확인
    // 상품이 maxCount개보다 적으면 모든 상품을 가져옴
    val topN = products.take(maxCount)
    if (topN.isEmpty()) return emptyList()
    
    // 기준 상품(마지막 상품)의 count 값
    // .last: 상품이 maxCount개보다 적으면 마지막 상품이 기준이 됨
    val lastCount = topN.last().count
    
    // 기준 상품의 count 이상인 모든 상품을 포함 (같은 순위의 상품들을 모두 포함할 수 있도록)
    return products.takeWhile { it.count >= lastCount }
}
