package com.example.tripcart.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.tripcart.R
import com.example.tripcart.ui.theme.PrimaryAccent
import com.example.tripcart.ui.theme.PrimaryBackground
import com.example.tripcart.ui.viewmodel.PlaceViewModel
import com.example.tripcart.ui.viewmodel.RankingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingDetailScreen(
    selectedCountry: String? = null,
    onBack: () -> Unit = {},
    rankingViewModel: RankingViewModel = viewModel(),
    placeViewModel: PlaceViewModel = viewModel()
) {
    val rankingUiState by rankingViewModel.uiState.collectAsState()
    val placeUiState by placeViewModel.uiState.collectAsState()
    
    var showCountryDialog by remember { mutableStateOf(false) }
    var showPlaceDialog by remember { mutableStateOf(false) }
    
    // 선택한 국가가 변경될 때마다 국가별 상품 랭킹 데이터 요청
    LaunchedEffect(selectedCountry) {
        selectedCountry?.let {
            rankingViewModel.loadCountryProductRanking(it)
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
            
            // 선택된 국가/상점 이름 표시
            rankingUiState.selectedCountry?.let { country ->
                Text(
                    text = country,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 16.dp)
                )
            } ?: rankingUiState.selectedPlaceName?.let { placeName ->
                Text(
                    text = placeName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 16.dp)
                )
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
                
                if (products.isEmpty()) {
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
                                rank = ranks[index]
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
    rank: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 순위 뱃지
            Box(
                modifier = Modifier
                    .size(40.dp)
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
                    fontSize = 18.sp,
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
            
            // 상품 정보
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.productName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = product.category,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
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
                            Text(
                                text = country,
                                modifier = Modifier.padding(16.dp),
                                fontSize = 16.sp
                            )
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

