package com.example.tripcart.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripcart.util.EnvUtils
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AddressComponent
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

// 모든 가능한 국가 이름(한국어/영어)을 한국어 이름으로 직접 매핑
private val COUNTRY_TO_KOREAN = mapOf(
    // 아시아
    "대한민국" to "대한민국",
    "한국" to "대한민국",
    "South Korea" to "대한민국",
    "Korea" to "대한민국",
    "일본" to "일본",
    "Japan" to "일본",
    "중국" to "중국",
    "China" to "중국",
    "대만" to "대만",
    "Taiwan" to "대만",
    "홍콩" to "홍콩",
    "Hong Kong" to "홍콩",
    "태국" to "태국",
    "Thailand" to "태국",
    "베트남" to "베트남",
    "Vietnam" to "베트남",
    "싱가포르" to "싱가포르",
    "Singapore" to "싱가포르",
    "말레이시아" to "말레이시아",
    "Malaysia" to "말레이시아",
    "인도네시아" to "인도네시아",
    "Indonesia" to "인도네시아",
    "필리핀" to "필리핀",
    "Philippines" to "필리핀",
    "인도" to "인도",
    "India" to "인도",
    "몽골" to "몽골",
    "Mongolia" to "몽골",
    "카자흐스탄" to "카자흐스탄",
    "Kazakhstan" to "카자흐스탄",
    "우즈베키스탄" to "우즈베키스탄",
    "Uzbekistan" to "우즈베키스탄",
    
    // 유럽
    "프랑스" to "프랑스",
    "France" to "프랑스",
    "영국" to "영국",
    "United Kingdom" to "영국",
    "UK" to "영국",
    "이탈리아" to "이탈리아",
    "Italy" to "이탈리아",
    "스페인" to "스페인",
    "Spain" to "스페인",
    "독일" to "독일",
    "Germany" to "독일",
    "네덜란드" to "네덜란드",
    "Netherlands" to "네덜란드",
    "벨기에" to "벨기에",
    "Belgium" to "벨기에",
    "스위스" to "스위스",
    "Switzerland" to "스위스",
    "오스트리아" to "오스트리아",
    "Austria" to "오스트리아",
    "체코" to "체코",
    "체코공화국" to "체코",
    "Czech Republic" to "체코",
    "폴란드" to "폴란드",
    "Poland" to "폴란드",
    "그리스" to "그리스",
    "Greece" to "그리스",
    "포르투갈" to "포르투갈",
    "Portugal" to "포르투갈",
    "터키" to "터키",
    "Turkey" to "터키",
    "러시아" to "러시아",
    "Russia" to "러시아",
    "노르웨이" to "노르웨이",
    "Norway" to "노르웨이",
    "스웨덴" to "스웨덴",
    "Sweden" to "스웨덴",
    "덴마크" to "덴마크",
    "Denmark" to "덴마크",
    "핀란드" to "핀란드",
    "Finland" to "핀란드",
    "아이슬란드" to "아이슬란드",
    "Iceland" to "아이슬란드",
    "아일랜드" to "아일랜드",
    "Ireland" to "아일랜드",
    
    // 아메리카
    "미국" to "미국",
    "United States" to "미국",
    "USA" to "미국",
    "US" to "미국",
    "America" to "미국",
    "캐나다" to "캐나다",
    "Canada" to "캐나다",
    "멕시코" to "멕시코",
    "Mexico" to "멕시코",
    "브라질" to "브라질",
    "Brazil" to "브라질",
    "아르헨티나" to "아르헨티나",
    "Argentina" to "아르헨티나",
    "칠레" to "칠레",
    "Chile" to "칠레",
    "페루" to "페루",
    "Peru" to "페루",
    "콜롬비아" to "콜롬비아",
    "Colombia" to "콜롬비아",
    
    // 오세아니아
    "호주" to "호주",
    "오스트레일리아" to "호주",
    "Australia" to "호주",
    "뉴질랜드" to "뉴질랜드",
    "New Zealand" to "뉴질랜드",
    
    // 아프리카
    "이집트" to "이집트",
    "Egypt" to "이집트",
    "남아프리카공화국" to "남아프리카공화국",
    "South Africa" to "남아프리카공화국",
    "모로코" to "모로코",
    "Morocco" to "모로코",
    
    // 중동
    "아랍에미리트" to "아랍에미리트",
    "United Arab Emirates" to "아랍에미리트",
    "UAE" to "아랍에미리트",
    "사우디아라비아" to "사우디아라비아",
    "Saudi Arabia" to "사우디아라비아",
    "이스라엘" to "이스라엘",
    "Israel" to "이스라엘",
    "요르단" to "요르단",
    "Jordan" to "요르단"
)

// 주소에서 국가를 추출하고 주소에서 제거
private fun extractCountryFromAddress(address: String?): Pair<String?, String?> {
    if (address == null) return Pair(null, null)
    
    // 국가 목록에서 주소에 포함된 국가 찾기 (긴 이름부터 매칭)
    val sortedCountries = COUNTRY_TO_KOREAN.keys.sortedByDescending { it.length }
    
    for (countryName in sortedCountries) {
        if (address.contains(countryName, ignoreCase = true)) {
            // 주소에서 국가 이름 제거
            val addressWithoutCountry = address
                .replace(countryName, "", ignoreCase = true)
                .trim()
                .replace(Regex("^[,，\\s]+"), "") // 앞의 쉼표나 공백 제거
                .replace(Regex("[,，\\s]+$"), "") // 뒤의 쉼표나 공백 제거
                .trim()
            
            // 한국어 이름으로 직접 변환
            val koreanName = COUNTRY_TO_KOREAN[countryName] ?: countryName
            
            return Pair(koreanName, addressWithoutCountry)
        }
    }
    
    // 국가를 찾지 못한 경우 원본 주소 반환
    return Pair(null, address)
}

data class PlaceDetails(
    val placeId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val country: String?,
    val phoneNumber: String?,
    val websiteUri: String?,
    val openingHours: List<String>?,
    val photoBitmap: Bitmap?
)

// PlaceDetails를 포함해 UI와 관련된 모든 요소들을 다룸
data class PlaceUiState(
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val predictions: List<AutocompletePrediction> = emptyList(),
    val searchError: String? = null,
    val selectedPlace: PlaceDetails? = null,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

class PlaceViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(PlaceUiState())
    val uiState: StateFlow<PlaceUiState> = _uiState.asStateFlow()
    
    private val placesClient: PlacesClient
    private val db = FirebaseFirestore.getInstance()
    
    init {
        val apiKey = EnvUtils.getMapsApiKey(application)
        if (!Places.isInitialized()) {
            Places.initialize(application, apiKey)
        }
        placesClient = Places.createClient(application)
    }
    
    // 장소 검색 (Autocomplete 사용)
    fun searchPlaces(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                searchQuery = query,
                searchError = null
            )
            
            // 최소 2글자 이상 입력해야 검색 가능
            if (query.length < 2) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false, // 실제 검색 안 함!
                    predictions = emptyList()
                )
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(
                isSearching = true,
                searchError = null
            )
            
            try {
                val token = AutocompleteSessionToken.newInstance()
                val request = FindAutocompletePredictionsRequest.builder()
                    .setQuery(query)
                    .setSessionToken(token)
                    .build()
                
                val response = placesClient.findAutocompletePredictions(request).await()
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    predictions = response.autocompletePredictions
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    searchError = "검색 중 오류가 발생했습니다: ${e.message}",
                    predictions = emptyList()
                )
            }
        }
    }
    
    /*
     * 선택한 장소의 상세 정보 가져오기
     * @param placeId 장소 ID
     * @param autocompleteAddress Autocomplete에서 받은 주소 (앵간해선 한국어 지원)
     * @param autocompleteName Autocomplete에서 받은 장소 이름 (한국어일 가능성이 높음)
     */
    fun fetchPlaceDetails(placeId: String, autocompleteAddress: String? = null, autocompleteName: String? = null) {
        if (placeId.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "장소 ID가 비어있습니다."
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                selectedPlace = null
            )
            
            try {
                android.util.Log.d("PlaceViewModel", "Fetching place details for: $placeId")
                val placeFields: List<Place.Field> = listOf(
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.LAT_LNG,
                    Place.Field.ADDRESS,
                    Place.Field.ADDRESS_COMPONENTS,
                    Place.Field.PHONE_NUMBER,
                    Place.Field.WEBSITE_URI,
                    Place.Field.OPENING_HOURS,
                    Place.Field.PHOTO_METADATAS
                )
                
                val placeRequest = FetchPlaceRequest.newInstance(
                    placeId,
                    placeFields
                )
                
                val placeResponse = placesClient.fetchPlace(placeRequest).await()
                val place = placeResponse.place
                
                // Autocomplete 주소 = 장소 검색 결과 목록에서 보여주는 주소 (앵간해선 한국어)
                // Places API 주소 = Places API에서 불러오는 상세 데이터 속 주소 (영어인 경우가 많음)
                // -> Autocomplete 주소를 우선 사용
                //    Places API 주소는 Autocomplete 주소가 없을 때만 사용
                val finalAddress = if (!autocompleteAddress.isNullOrEmpty()) {
                    autocompleteAddress
                } else {
                    place.address ?: ""
                }
                
                // 장소 이름: Autocomplete 이름이 있으면 사용 (한국어일 가능성이 높음), 없으면 Places API 이름 사용
                val finalName = autocompleteName ?: (place.name ?: "")
                
                //android.util.Log.d("PlaceViewModel", "Autocomplete address: $autocompleteAddress")
                //android.util.Log.d("PlaceViewModel", "Places API address: ${place.address}")
                //android.util.Log.d("PlaceViewModel", "Final address: $finalAddress")
                
                // 주소에서 국가 추출 (사전에 정의해둔 국가 목록 사용)
                val (countryFromAddress, addressWithoutCountry) = extractCountryFromAddress(finalAddress)
                
                // 영업시간 추출
                val openingHours = place.openingHours?.weekdayText
                
                // 대표 이미지 추출 후 비트맵 방식으로 재렌더링 (이미지가 있을 경우)
                val photoMetadata = place.photoMetadatas?.firstOrNull()
                val photoBitmap = try {
                    photoMetadata?.let { metadata ->
                        val photoRequest = FetchPhotoRequest.builder(metadata)
                            .setMaxWidth(800)
                            .setMaxHeight(800)
                            .build()
                        val photoResponse = placesClient.fetchPhoto(photoRequest).await()
                        photoResponse.bitmap
                    }
                } catch (e: Exception) {
                    // android.util.Log.e("PlaceViewModel", "Error fetching photo", e)
                    null
                }
                
                val placeDetails = PlaceDetails(
                    placeId = place.id ?: "",
                    name = finalName,
                    latitude = place.latLng?.latitude ?: 0.0,
                    longitude = place.latLng?.longitude ?: 0.0,
                    address = addressWithoutCountry,
                    country = countryFromAddress,
                    phoneNumber = place.phoneNumber,
                    websiteUri = place.websiteUri?.toString(),
                    openingHours = openingHours,
                    photoBitmap = photoBitmap
                )
                
                //android.util.Log.d("PlaceViewModel", "Place details fetched successfully: ${placeDetails.name}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedPlace = placeDetails
                )
            } catch (e: Exception) {
                //android.util.Log.e("PlaceViewModel", "Error fetching place details", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "장소 정보를 가져오는데 실패했습니다: ${e.message}"
                )
            }
        }
    }
    
    // 장소 중복 체크 후 리스트 선택 화면으로 이동
    // Firestore에 이미 존재하면 그 데이터를 사용하고, 없으면 저장 후 사용
    fun checkPlaceAndNavigate(placeDetails: PlaceDetails, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                errorMessage = null,
                saveSuccess = false
            )
            
            try {
                // placeId로 Firestore에서 조회
                val placeDoc = try {
                    db.collection("places").document(placeDetails.placeId).get().await()
                } catch (e: Exception) {
                    android.util.Log.e("PlaceViewModel", "Error checking duplicate place", e)
                    null
                }
                
                val finalPlaceDetails: PlaceDetails = if (placeDoc?.exists() == true) {
                    // Firestore에 이미 존재하는 경우, Firestore의 데이터를 사용
                    val data = placeDoc.data
                    PlaceDetails(
                        placeId = data?.get("placeId") as? String ?: placeDetails.placeId,
                        name = data?.get("name") as? String ?: placeDetails.name,
                        latitude = (data?.get("latitude") as? Double) ?: placeDetails.latitude,
                        longitude = (data?.get("longitude") as? Double) ?: placeDetails.longitude,
                        address = data?.get("address") as? String ?: placeDetails.address,
                        country = data?.get("country") as? String ?: placeDetails.country,
                        phoneNumber = data?.get("phoneNumber") as? String ?: placeDetails.phoneNumber,
                        websiteUri = data?.get("websiteUri") as? String ?: placeDetails.websiteUri,
                        openingHours = (data?.get("openingHours") as? List<*>)?.mapNotNull { it as? String } 
                            ?: placeDetails.openingHours,
                        photoBitmap = null // Firestore에는 사진이 저장되지 않으므로 null
                    )
                } else {
                    // Firestore에 존재하지 않는 경우, Google Places API 데이터를 Firestore에 저장
                    val placeData = hashMapOf(
                        "placeId" to placeDetails.placeId,
                        "name" to placeDetails.name,
                        "latitude" to placeDetails.latitude,
                        "longitude" to placeDetails.longitude,
                        "address" to (placeDetails.address ?: ""),
                        "country" to (placeDetails.country ?: ""),
                        "phoneNumber" to (placeDetails.phoneNumber ?: ""),
                        "websiteUri" to (placeDetails.websiteUri ?: ""),
                        "openingHours" to (placeDetails.openingHours ?: emptyList<String>())
                    )
                    
                    db.collection("places")
                        .document(placeDetails.placeId)
                        .set(placeData)
                        .await()
                    
                    // 저장한 데이터를 그대로 사용
                    placeDetails
                }
                
                // 선택한 장소를 ViewModel에 저장 (Firestore의 데이터 또는 새로 저장한 데이터)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    selectedPlace = finalPlaceDetails
                )
                onResult(true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "장소 확인에 실패했습니다: ${e.message}"
                )
                onResult(false)
            }
        }
    }
    
    // Firebase Firestore에 장소 저장 (placeId로 중복 체크)
    fun savePlaceToFirestore(placeDetails: PlaceDetails) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                errorMessage = null,
                saveSuccess = false
            )
            
            try {
                // placeId로 중복 체크
                val placeDoc = try {
                    db.collection("places").document(placeDetails.placeId).get().await()
                } catch (e: Exception) {
                    android.util.Log.e("PlaceViewModel", "Error checking duplicate place", e)
                    null
                }
                
                if (placeDoc?.exists() == true) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = "이미 등록된 장소입니다."
                    )
                    return@launch
                }
                
                // Firestore에 저장
                val placeData = hashMapOf(
                    "placeId" to placeDetails.placeId,
                    "name" to placeDetails.name,
                    "latitude" to placeDetails.latitude,
                    "longitude" to placeDetails.longitude,
                    "address" to (placeDetails.address ?: ""),
                    "country" to (placeDetails.country ?: ""),
                    "phoneNumber" to (placeDetails.phoneNumber ?: ""),
                    "websiteUri" to (placeDetails.websiteUri ?: ""),
                    "openingHours" to (placeDetails.openingHours ?: emptyList<String>())
                )
                
                db.collection("places")
                    .document(placeDetails.placeId)
                    .set(placeData)
                    .await()
                
                //android.util.Log.d("PlaceViewModel", "Place saved successfully: ${placeDetails.placeId}")
                
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "장소 저장에 실패했습니다: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }
    
    fun clearSelectedPlace() {
        _uiState.value = _uiState.value.copy(selectedPlace = null)
    }
}

