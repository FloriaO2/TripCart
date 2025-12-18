package com.example.tripcart.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.tripcart.R
import com.example.tripcart.ui.theme.PrimaryAccent
import com.example.tripcart.ui.theme.PrimaryBackground
import com.example.tripcart.ui.viewmodel.PlaceViewModel
import com.example.tripcart.ui.viewmodel.ProductViewModel
import com.example.tripcart.ui.viewmodel.RankingViewModel
import com.google.firebase.auth.FirebaseAuth
import android.content.Context
import android.content.SharedPreferences
import com.example.tripcart.ui.theme.TagBackground

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

// 최근 검색어 데이터 클래스
data class RecentSearch(
    val type: String, // "country" or "place"
    val displayName: String, // 국가 이름 or 상점 이름
    val placeId: String? = null // 상점인 경우 placeId
)

// 최근 검색어 관리 함수들
// SharedPreferences - key-value 형태로 저장하는 로컬 경량 DB
//                     최근 검색어처럼 간단한 거는 Room DB 연결보다 이걸 쓰는게 나음!
private const val PREFS_NAME = "ranking_search_history" // SharedPreferences 구분용 별칭
private const val KEY_RECENT_SEARCHES = "recent_searches" // 최근 검색어 키
private const val MAX_RECENT_SEARCHES = 10 // 최근 검색어 최대 개수
private const val SEPARATOR = "|||" // 최근 검색어 구분자

private fun getRecentSearches(context: Context): List<RecentSearch> {
    // Context.MODE_PRIVATE - 앱 내부에서만 접근 가능
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val searchesString = prefs.getString(KEY_RECENT_SEARCHES, "") ?: ""
    return if (searchesString.isEmpty()) {
        emptyList()
    } else {
        searchesString.split(SEPARATOR).filter { it.isNotEmpty() }.mapNotNull { item ->
            val parts = item.split(":") // country:국가이름, place:1234:상점이름 이런 식으로 저장돼있음
            when (parts.size) {
                2 -> if (parts[0] == "country") RecentSearch("country", parts[1]) else null
                3 -> if (parts[0] == "place") RecentSearch("place", parts[2], parts[1]) else null
                else -> null
            }
        }
    }
}

private fun addRecentSearch(context: Context, search: RecentSearch) {
    // Context.MODE_PRIVATE - 앱 내부에서만 접근 가능
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    // 기존 검색어 목록 가져오기
    val currentSearches = getRecentSearches(context).toMutableList()
    
    // 이미 존재하면 제거 (중복 방지)
    currentSearches.removeAll { it.type == search.type && it.displayName == search.displayName && it.placeId == search.placeId }
    // 맨 앞에 추가
    currentSearches.add(0, search)
    // 최대 개수 제한
    if (currentSearches.size > MAX_RECENT_SEARCHES) {
        currentSearches.removeAt(currentSearches.size - 1)
    }
    
    // 직렬화 - 기존 형식에 맞춰 텍스트 변환
    val serialized = currentSearches.joinToString(SEPARATOR) { searchItem ->
        when (searchItem.type) {
            "country" -> "country:${searchItem.displayName}"
            "place" -> "place:${searchItem.placeId}:${searchItem.displayName}"
            else -> ""
        }
    }
    prefs.edit().putString(KEY_RECENT_SEARCHES, serialized).apply()
}

private fun removeRecentSearch(context: Context, search: RecentSearch) {
    // Context.MODE_PRIVATE - 앱 내부에서만 접근 가능
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    // 기존 검색어 목록 가져오기
    val currentSearches = getRecentSearches(context).toMutableList()
    currentSearches.removeAll { it.type == search.type && it.displayName == search.displayName && it.placeId == search.placeId }
    
    // 직렬화 - 기존 형식에 맞춰 텍스트 변환
    val serialized = currentSearches.joinToString(SEPARATOR) { searchItem ->
        when (searchItem.type) {
            "country" -> "country:${searchItem.displayName}"
            "place" -> "place:${searchItem.placeId}:${searchItem.displayName}"
            else -> ""
        }
    }
    prefs.edit().putString(KEY_RECENT_SEARCHES, serialized).apply()
}

private fun clearRecentSearches(context: Context) {
    // Context.MODE_PRIVATE - 앱 내부에서만 접근 가능
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    // 최근 검색어 목록 초기화
    prefs.edit().putString(KEY_RECENT_SEARCHES, "").apply()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RankingDetailScreen(
    selectedCountry: String? = null,
    onBack: () -> Unit = {},
    onNavigateToReview: (String) -> Unit = {}, // 상품 리뷰 페이지로 이동
    onNavigateToAddProduct: (String) -> Unit = {}, // productId 전달한 상태로 상품 추가 페이지로 이동
    rankingViewModel: RankingViewModel = viewModel(),
    placeViewModel: PlaceViewModel = viewModel(),
    productViewModel: ProductViewModel = viewModel()
) {
    val rankingUiState by rankingViewModel.uiState.collectAsState()
    val placeUiState by placeViewModel.uiState.collectAsState()
    val productUiState by productViewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var showCountryDialog by remember { mutableStateOf(false) }
    var showPlaceDialog by remember { mutableStateOf(false) }
    var recentSearches by remember { mutableStateOf(getRecentSearches(context)) }
    
    // favorite 목록 로드
    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            productViewModel.loadFavorites()
        }
    }
    
    // 선택한 국가가 변경될 때마다 국가별 상품 랭킹 데이터 요청
    // countryProducts에 전체 상품 데이터가 없을 때만 로드 
    LaunchedEffect(selectedCountry) {
        selectedCountry?.let { country ->
            // ViewModel의 selectedCountry 설정
            rankingViewModel.setSelectedCountry(country)
            
            val currentProducts = rankingUiState.countryProducts[country]
            // 데이터가 없을 때만 로드
            if (currentProducts == null || currentProducts.isEmpty()) {
                rankingViewModel.loadCountryProductRanking(country)
            }
        }
    }
    
    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "상품 랭킹",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // 국가 선택하기 버튼 + 상점 검색하기 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp), // 랭킹 페이지와 버튼 위치 통일시키기 위해 추가
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 국가 선택하기 버튼 (주황색 배경 + 흰색 글씨)
                val countryInteractionSource = remember { MutableInteractionSource() }
                val isCountryPressed = countryInteractionSource.collectIsPressedAsState().value
                
                Button(
                    onClick = { showCountryDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(90.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCountryPressed) Color(0xFFE65100) else Color(0xFFFF9800)
                    ),
                    interactionSource = countryInteractionSource
                ) {
                    Text(
                        text = "국가 선택하기",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                // 상점 검색하기 버튼 (노란색 배경 + 검정색 글씨)
                val placeInteractionSource = remember { MutableInteractionSource() }
                val isPlacePressed = placeInteractionSource.collectIsPressedAsState().value
                
                Button(
                    onClick = { showPlaceDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(90.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlacePressed) Color(0xFFFFEB3B) else Color(0xFFFFF176)
                    ),
                    interactionSource = placeInteractionSource
                ) {
                    Text(
                        text = "상점 검색하기",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF424242)
                    )
                }
            }
            
            // 최근 검색어 표시
            if (rankingUiState.selectedCountry == null && rankingUiState.selectedPlaceName == null && recentSearches.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 5.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "최근 검색어",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 16.sp
                        )
                        Text(
                            text = "전체 삭제",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            lineHeight = 12.sp,
                            modifier = Modifier.clickable {
                                clearRecentSearches(context)
                                recentSearches = emptyList()
                            }
                        )
                    }

                    // FlowRow - 하위 요소들 가로 너비에 맞게 자동 배치
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recentSearches.forEach { search ->
                            RecentSearchTag(
                                text = search.displayName,
                                onRemove = {
                                    removeRecentSearch(context, search)
                                    recentSearches = getRecentSearches(context)
                                },
                                onClick = {
                                    when (search.type) {
                                        "country" -> {
                                            rankingViewModel.setSelectedCountry(search.displayName)
                                            rankingViewModel.loadCountryProductRanking(search.displayName)
                                        }
                                        "place" -> {
                                            search.placeId?.let { placeId ->
                                                rankingViewModel.loadPlaceProductRanking(placeId, search.displayName)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            // 선택된 국가/상점 이름 표시
            rankingUiState.selectedCountry?.let { country ->
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 국가 아이콘
                    Image(
                        painter = painterResource(id = R.drawable.country),
                        contentDescription = "국가",
                        modifier = Modifier.size(25.dp)
                    )
                    // 현재 검색어
                    Text(
                        text = "현재 검색어 ",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    // 국기 이모티콘 표시 (매핑에 없으면 표시하지 않음)
                    countryFlagMap[country]?.let { flag ->
                        Text(
                            text = flag,
                            fontSize = 24.sp
                        )
                    }
                    Text(
                        text = country,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // X 아이콘 버튼
                    IconButton(
                        onClick = { rankingViewModel.clearSelection() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "선택 해제",
                            modifier = Modifier.size(20.dp),
                            tint = Color.Gray
                        )
                    }
                }
            } ?: rankingUiState.selectedPlaceName?.let { placeName ->
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 상점 아이콘
                    Image(
                        painter = painterResource(id = R.drawable.store),
                        contentDescription = "상점",
                        modifier = Modifier.size(25.dp)
                    )
                    // 현재 검색어
                    Text(
                        text = "현재 검색어 ",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = placeName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // X 아이콘 버튼
                    IconButton(
                        onClick = { rankingViewModel.clearSelection() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "선택 해제",
                            modifier = Modifier.size(20.dp),
                            tint = Color.Gray
                        )
                    }
                }
            }
            
            // 상품 랭킹 리스트
            if (rankingUiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // 국가 선택시 국가별 상품 랭킹, 아니라면 상점별 상품 랭킹을 사용
                val products = rankingUiState.selectedCountry?.let { country ->
                    rankingUiState.countryProducts[country] ?: emptyList()
                } ?: rankingUiState.placeProducts
                
                // 선택된 국가나 상점이 없을 때
                val isSearching = rankingUiState.selectedCountry != null || rankingUiState.selectedPlaceName != null
                
                if (!isSearching) { // 검색 하기 전 기본 문구
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "상단 버튼을 눌러\n원하는 국가 및 상점을 입력해주세요.",
                            color = Color.Gray,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (products.isEmpty()) { // 검색을 했는데 검색 결과가 없을 경우
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "랭킹 데이터가 없습니다.",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    // 같은 count 값을 가진 상품들은 같은 순위로 계산
                    val ranks = remember(products) {
                        if (products.isEmpty()) return@remember emptyList<Int>()
                        val rankList = mutableListOf<Int>()
                        rankList.add(1) // 첫번째는 항상 1위
                        
                        for (i in 1 until products.size) {
                            if (products[i].count == products[i-1].count) {
                                rankList.add(rankList[i-1]) // 같은 count면 이전 값과 같은 순위
                            } else {
                                rankList.add(i + 1) // 다른 count면 다음 순위
                            }
                        }
                        rankList
                    }
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        itemsIndexed(products) { index, product ->
                            ProductRankingDetailItem(
                                product = product,
                                rank = ranks[index],
                                onClick = { onNavigateToReview(product.productId) },
                                onAddClick = { onNavigateToAddProduct(product.productId) },
                                isFavorite = productUiState.favoriteProductIds.contains(product.productId),
                                onFavoriteClick = { productViewModel.toggleFavorite(product.productId) }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 국가 선택 다이얼로그
    if (showCountryDialog) {
        CountrySelectionDialog(
            onDismiss = { showCountryDialog = false },
            onCountrySelected = { country ->
                addRecentSearch(context, RecentSearch("country", country))
                recentSearches = getRecentSearches(context)
                rankingViewModel.loadCountryProductRanking(country)
                showCountryDialog = false
            },
            rankingViewModel = rankingViewModel
        )
    }
    
    // 상점 검색 다이얼로그
    if (showPlaceDialog) {
        PlaceSearchDialog(
            onDismiss = { showPlaceDialog = false },
            onPlaceSelected = { placeId, placeName ->
                addRecentSearch(context, RecentSearch("place", placeName, placeId))
                recentSearches = getRecentSearches(context)
                rankingViewModel.loadPlaceProductRanking(placeId, placeName)
                showPlaceDialog = false
            },
            placeViewModel = placeViewModel
        )
    }
}

@Composable
fun ProductRankingDetailItem(
    product: com.example.tripcart.ui.viewmodel.ProductRanking,
    rank: Int,
    onClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick), // 전체 카드 클릭시 리뷰 페이지로 이동
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 순위 뱃지
            Box(
                modifier = Modifier
                    .size(30.dp)
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // 상품 이미지
            if (product.imageUrls.isNotEmpty()) {
                AsyncImage(
                    model = product.imageUrls[0], // 이미지가 여러개 저장돼있어도
                                                  // 상품별로 첫번째 사진만 보여줌
                    contentDescription = product.productName,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
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
            
            // 상품 정보
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.productName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp
                )
                Text(
                    text = product.category,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    lineHeight = 14.sp
                )
            }
            
            // 하트 아이콘 버튼 + 리스트 추가 아이콘 버튼
            Row(
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

@Composable
fun CountrySelectionDialog(
    onDismiss: () -> Unit,
    onCountrySelected: (String) -> Unit,
    rankingViewModel: RankingViewModel
) {
    val uiState by rankingViewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    // 검색어 변경 시 검색 실행
    LaunchedEffect(searchQuery) {
        rankingViewModel.searchCountries(searchQuery)
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 검색창
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("국가 검색") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "검색")
                    },
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 국가 목록
                val countriesToShow = if (searchQuery.isNotEmpty()) { // 입력된 검색어가 있다면
                    uiState.filteredCountries
                } else {
                    uiState.allCountries
                }
                
                // 클릭을 통해 빠르게 국가 선택할 수 있도록 돕는 목록
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(countriesToShow) { _, country ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    onCountrySelected(country)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF5F5F5)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 국기 이모티콘 표시 (매핑에 없으면 표시하지 않음)
                                countryFlagMap[country]?.let { flag ->
                                    Text(
                                        text = flag,
                                        fontSize = 20.sp
                                    )
                                }
                            Text(
                                text = country,
                                fontSize = 16.sp
                            )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaceSearchDialog(
    onDismiss: () -> Unit,
    onPlaceSelected: (String, String) -> Unit,
    placeViewModel: PlaceViewModel
) {
    val uiState = placeViewModel.uiState.collectAsState().value
    // 키보드 컨트롤러 - 키보드 열고 닫을 때 사용하는 도구
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchQuery by remember { mutableStateOf("") }
    
    // 검색어 변경 시 검색 실행
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            placeViewModel.searchPlaces(searchQuery)
        }
    }
    
    // 화면이 처음 표시될 때 선택된 장소 초기화
    LaunchedEffect(Unit) {
        placeViewModel.clearSelectedPlace()
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 검색창
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("상점 검색") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "검색")
                    },
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 검색 결과
                if (uiState.isSearching) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (searchQuery.length < 2) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Text(
                            text = "검색어를 입력해주세요.",
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp)
                        )
                    }
                } else if (uiState.predictions.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "검색 결과가 없습니다.",
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(uiState.predictions) { prediction ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable {
                                        // 장소 클릭해서 결과 페이지로 넘어가면 자동으로 키보드 닫기
                                        keyboardController?.hide()
                                        val placeId = prediction.placeId
                                        if (placeId.isNotEmpty()) {
                                            val name = prediction.structuredFormatting?.mainText ?: ""
                                            val address = prediction.structuredFormatting?.secondaryText ?: ""
                                            placeViewModel.fetchPlaceDetails(placeId, address, name)
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF5F5F5)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = prediction.structuredFormatting?.mainText ?: prediction.description,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (prediction.structuredFormatting?.secondaryText != null) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = prediction.structuredFormatting.secondaryText,
                                            fontSize = 14.sp,
                                            color = Color.Gray
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
    
    // 장소 선택 시 콜백 호출
    LaunchedEffect(uiState.selectedPlace) {
        uiState.selectedPlace?.let { placeDetails ->
            onPlaceSelected(placeDetails.placeId, placeDetails.name)
            placeViewModel.clearSelectedPlace()
        }
    }
}

@Composable
fun RecentSearchTag(
    text: String,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = TagBackground,
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

