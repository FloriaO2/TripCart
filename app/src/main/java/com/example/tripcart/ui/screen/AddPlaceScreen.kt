package com.example.tripcart.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tripcart.R
import com.example.tripcart.ui.theme.PrimaryAccent
import com.example.tripcart.ui.theme.PrimaryBackground
import com.example.tripcart.ui.theme.SecondaryBackground
import com.example.tripcart.ui.theme.TertiaryBackground
import com.example.tripcart.ui.viewmodel.PlaceViewModel
import com.example.tripcart.ui.viewmodel.PlaceDetails
import kotlinx.coroutines.delay

// 구글 맵 Places API를 사용하여 상점 검색 기능 구현

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlaceScreen(
    onBack: () -> Unit,
    onPlaceSelected: (PlaceDetails) -> Unit,
    viewModel: PlaceViewModel = viewModel()
) {
    val uiState = viewModel.uiState.collectAsState().value
    
    // 화면이 처음 표시될 때 선택된 장소 초기화
    LaunchedEffect(Unit) {
        viewModel.clearSelectedPlace()
    }
    
    var searchQuery by remember { mutableStateOf("") }
    
    // 검색어 변경 시 ViewModel에 검색 요청
    LaunchedEffect(searchQuery) {
        viewModel.searchPlaces(searchQuery)
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "상점 추가하기",
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
                    } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SecondaryBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 검색창
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("상점을 검색하세요") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "검색")
                    },
                    singleLine = true
                )
                
                // 검색 결과 리스트
                // 로딩 상태 표시
                if (uiState.isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                // Error에 값이 있음 = 에러 발생
                } else if (uiState.searchError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.searchError ?: "오류가 발생했습니다",
                            color = Color.Red
                        )
                    }
                // 입력창에 아무것도 입력하지 않았거나 한 글자만 입력한 상태
                // Places API는 최소 2글자 이상을 입력해야 검색 결과 받아옴
                } else if (searchQuery.length < 2) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "검색어를 입력해주세요.",
                            color = Color.Gray
                        )
                    }
                // 입력창에 2글자 이상을 입력했으나 검색 결과가 없는 상태
                } else if (uiState.predictions.isEmpty() && searchQuery.length >= 2) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "검색 결과가 없습니다.",
                            color = Color.Gray
                        )
                    }
                // 검색 결과 존재
                } else if (uiState.predictions.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(
                                bottom = if (uiState.selectedPlace != null) 400.dp else 0.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(uiState.predictions) { prediction ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clickable {
                                        // 장소 선택 시 상세 정보 가져오기
                                        val placeId = prediction.placeId
                                        if (placeId.isNotEmpty()) {
                                            // Autocomplete에서 받은 주소 정보 전달 (상점 이름 제외, 주소만)
                                            val address = prediction.getSecondaryText(null)?.toString() ?: ""
                                            // Autocomplete에서 받은 장소 이름 전달 (한국어일 가능성이 높음)
                                            val name = prediction.getPrimaryText(null)?.toString() ?: ""
                                            viewModel.fetchPlaceDetails(placeId, address, name)
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = TertiaryBackground
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = prediction.getPrimaryText(null).toString(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (prediction.getSecondaryText(null) != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.map),
                                                contentDescription = "지도",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = prediction.getSecondaryText(null).toString(),
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
            
            // 로딩 상태 표시 (오버레이)
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "장소 정보를 불러오는 중...",
                            color = Color.White
                        )
                    }
                }
            }
            
            // 선택한 장소 상세 정보 표시 및 저장 (하단에 고정)
            uiState.selectedPlace?.let { placeDetails ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(
                            width = 2.dp,
                            color = Color(0x4F000000),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .align(Alignment.BottomCenter),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        placeDetails.country?.let { country ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PrimaryBackground,
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 20.dp)
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.country),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                            .padding(end = 6.dp),
                                        colorFilter = ColorFilter.tint(Color(0xFF333333))
                                    )
                                    Text(
                                        text = country,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF333333)
                                    )
                                }
                            }
                        }

                        // 상점 이미지 표시
                        placeDetails.photoBitmap?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = placeDetails.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .padding(bottom = 12.dp)
                            )
                        } ?: run {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFE0E0E0))
                                    .padding(bottom = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "이미지를 불러올 수 없습니다",
                                    color = Color.DarkGray
                                )
                            }
                        }
                        
                        Text(
                            text = placeDetails.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        

                        
                        placeDetails.address?.let {
                            DetailRow(
                                icon = Icons.Default.LocationOn,
                                value = it
                            )
                        }
                        if (placeDetails.phoneNumber != null && placeDetails.phoneNumber.isNotEmpty()) {
                            DetailRow(
                                icon = Icons.Default.Phone,
                                value = placeDetails.phoneNumber
                            )
                        }
                        if (placeDetails.websiteUri != null && placeDetails.websiteUri.isNotEmpty()) {
                            DetailRowWithDrawable(
                                iconRes = R.drawable.link,
                                value = placeDetails.websiteUri
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 선택/취소 버튼 가로 정렬
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 취소 버튼
                            Button(
                                onClick = {
                                    viewModel.clearSelectedPlace()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xA6D32F2F) // 붉은 계열
                                )
                            ) {
                                Text(
                                    "취소", 
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            // 선택 버튼
                            Button(
                                onClick = {
                                    // 바로 리스트 선택 화면으로 이동 (이미지 로딩 완료 여부와 관계없이)
                                    onPlaceSelected(placeDetails)
                                    // 백그라운드에서 중복 체크 및 저장 처리
                                    viewModel.checkPlaceAndNavigate(placeDetails) { shouldNavigate ->
                                        // 이미 화면 이동했으므로 여기서는 아무것도 하지 않음
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isSaving,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xA6023694)
                                )
                            ) {
                                if (uiState.isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White
                                    )
                                } else {
                                    Text(
                                        "선택",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // 에러 메시지 표시
            uiState.errorMessage?.let { error ->
                LaunchedEffect(error) {
                    snackbarHostState.showSnackbar(
                        message = error,
                        duration = SnackbarDuration.Short
                    )
                    viewModel.clearError()
                }
            }
            
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryAccent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color(0xFF333333),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DetailRowWithDrawable(
    iconRes: Int,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(PrimaryAccent),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color(0xFF333333),
            fontWeight = FontWeight.Medium
        )
    }
}

