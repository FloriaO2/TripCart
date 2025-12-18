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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import android.net.Uri
import coil.compose.AsyncImage
import com.example.tripcart.R
import com.example.tripcart.ui.theme.PrimaryAccent
import com.example.tripcart.ui.theme.PrimaryBackground
import com.example.tripcart.ui.theme.SecondaryBackground
import com.example.tripcart.ui.theme.TertiaryBackground
import com.example.tripcart.ui.viewmodel.PlaceViewModel
import com.example.tripcart.ui.viewmodel.PlaceDetails
import com.example.tripcart.ui.viewmodel.ListViewModel
import com.example.tripcart.util.SetStatusBarColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlaceForListScreen(
    listId: String,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    placeViewModel: PlaceViewModel = viewModel(),
    listViewModel: ListViewModel = viewModel()
) {
    val uiState by placeViewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 화면이 처음 표시될 때 선택된 장소 초기화
    LaunchedEffect(Unit) {
        placeViewModel.clearSelectedPlace()
        // 해당 리스트를 선택 상태로 설정
        listViewModel.clearAllSelections()
        listViewModel.toggleListSelection(listId)
    }
    
    var searchQuery by remember { mutableStateOf("") }
    
    // 검색어 변경 시 ViewModel에 검색 요청
    LaunchedEffect(searchQuery) {
        placeViewModel.searchPlaces(searchQuery)
    }
    
    // 상태바 색상을 상단바와 동일하게 설정
    SetStatusBarColor(
        statusBarColor = SecondaryBackground,
        isLightStatusBars = true
    )
    
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
                } else if (uiState.searchError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.searchError ?: "오류가 발생했습니다.",
                            color = Color.Red
                        )
                    }
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
                                        keyboardController?.hide()
                                        val placeId = prediction.placeId
                                        if (placeId.isNotEmpty()) {
                                            val address = prediction.structuredFormatting?.secondaryText ?: ""
                                            val name = prediction.structuredFormatting?.mainText ?: ""
                                            placeViewModel.fetchPlaceDetails(placeId, address, name)
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
                                        text = prediction.structuredFormatting?.mainText ?: prediction.description,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (prediction.structuredFormatting?.secondaryText != null) {
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
                                                text = placeViewModel.removeCountryFromAutocompleteAddress(
                                                    prediction.structuredFormatting.secondaryText
                                                ),
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    // 반투명 배경 - 팝업 외부 클릭 시 팝업 닫기
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.2f))
                            .clickable {
                                placeViewModel.clearSelectedPlace()
                            }
                    )
                    // Card를 별도의 Box로 감싸서 클릭 이벤트 연결
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .clickable { }
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .border(
                                    width = 2.dp,
                                    color = Color(0x4F000000),
                                    shape = RoundedCornerShape(12.dp)
                                ),
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
                                placeDetails.photoUrl?.let { url ->
                                    AsyncImage(
                                        model = url,
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
                                            .padding(bottom = 12.dp)
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFE0E0E0)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "이미지를 불러올 수 없습니다.",
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
                                    val context = LocalContext.current
                                    DetailRow(
                                        icon = Icons.Default.Phone,
                                        value = placeDetails.phoneNumber,
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                                data = Uri.parse("tel:${placeDetails.phoneNumber}")
                                            }
                                            context.startActivity(intent)
                                        }
                                    )
                                }
                                if (placeDetails.websiteUri != null && placeDetails.websiteUri.isNotEmpty()) {
                                    val context = LocalContext.current
                                    DetailRowWithDrawable(
                                        iconRes = R.drawable.link,
                                        value = placeDetails.websiteUri,
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                data = Uri.parse(placeDetails.websiteUri)
                                            }
                                            context.startActivity(intent)
                                        }
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
                                            placeViewModel.clearSelectedPlace()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xA6D32F2F)
                                        )
                                    ) {
                                        Text(
                                            "취소",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // 확인 버튼 - 바로 리스트에 추가
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val result = listViewModel.addPlaceToSelectedLists(placeDetails)
                                                
                                                if (result.isSuccess) {
                                                    listViewModel.setSuccessMessage("장소가 추가되었습니다.")
                                                    onComplete()
                                                } else {
                                                    snackbarHostState.showSnackbar(
                                                        message = result.exceptionOrNull()?.message ?: "오류가 발생했습니다.",
                                                        duration = SnackbarDuration.Long
                                                    )
                                                }
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
                                                "확인",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
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
                        duration = SnackbarDuration.Long
                    )
                    placeViewModel.clearError()
                }
            }
            
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
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
    value: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
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

