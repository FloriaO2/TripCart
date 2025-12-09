package com.example.tripcart.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// 국가 랭킹 데이터
data class CountryRanking(
    val country: String,
    val totalCount: Long
)

// 상품 랭킹 데이터
data class ProductRanking(
    val productId: String,
    val productName: String,
    val category: String,
    val imageUrls: List<String>,
    val count: Long
)

// 랭킹 UI 상태
data class RankingUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val topCountries: List<CountryRanking> = emptyList(), // TOP3 국가
    val countryProducts: Map<String, List<ProductRanking>> = emptyMap(), // 국가별 상품 랭킹
    val selectedCountry: String? = null,
    val selectedPlaceId: String? = null,
    val selectedPlaceName: String? = null,
    val placeProducts: List<ProductRanking> = emptyList(), // 상점별 상품 랭킹
    val allCountries: List<String> = emptyList(), // 모든 국가 목록
    val searchQuery: String = "",
    val filteredCountries: List<String> = emptyList() // 검색된 국가 목록
)

class RankingViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(RankingUiState())
    val uiState: StateFlow<RankingUiState> = _uiState.asStateFlow()
    
    private val db = FirebaseFirestore.getInstance()
    // TOP3 국가 종류에 대한 실시간 리스너
    private var countriesListener: ListenerRegistration? = null
    // 각 국가별 상품 랭킹에 대한 실시간 리스너
    private val productListeners = mutableMapOf<String, ListenerRegistration>()
    private var isInitialLoad = true
    
    init {
        setupRealtimeListeners()
        loadAllCountries()
    }
    
    override fun onCleared() {
        super.onCleared()
        countriesListener?.remove()
        productListeners.values.forEach { it.remove() }
        productListeners.clear()
    }
    
    // 실시간 리스너 설정
    private fun setupRealtimeListeners() {
        // ranking_countries 컬렉션 실시간 리스너
        countriesListener = db.collection("ranking_countries")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    viewModelScope.launch {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "랭킹 데이터를 불러오는데 실패했습니다: ${error.message}"
                        )
                    }
                    return@addSnapshotListener
                }
                
                snapshot?.let {
                    viewModelScope.launch {
                        // 초기 로드가 아닌 경우 isLoading을 변경하지 않음
                        // - isLoading이 true면 데이터가 바뀌었을 때
                        //   랭킹 페이지 자체가 새로고침돼버려서 스크롤 등의 정보가 리셋됨
                        //   -> isLoading이 true 되지 않은 상태로 데이터를 실시간 업데이트 할 수 있도록!
                        val shouldShowLoading = isInitialLoad
                        if (shouldShowLoading) {
                            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                        } else {
                            _uiState.value = _uiState.value.copy(errorMessage = null)
                        }
                        
                        try {
                            val countries = it.documents.mapNotNull { doc ->
                                val country = doc.id
                                val totalCount = doc.getLong("totalCount") ?: 0L // 기본값 0
                                CountryRanking(country, totalCount)
                            }
                            
                            // totalCount 내림차순 정렬 후 TOP3 추출
                            val top3 = countries.sortedByDescending { it.totalCount }.take(3)
                            
                            // 기존 상품 리스너 제거
                            // - 국가 순위가 달라졌을 때 순위에서 밀린 국가에 대한 리스너는 해제함으로써 불필요한 네트워크 요청을 막음
                            productListeners.values.forEach { it.remove() }
                            productListeners.clear()
                            
                            // 각 국가별 상품 랭킹 실시간 리스너 설정
                            top3.forEach { countryRanking ->
                                setupCountryProductsListener(countryRanking.country)
                            }
                            
                            // 각 국가별 상품 랭킹 병렬 로드
                            // - 병렬 로드가 아니면 한 번에 한 국가의 상품씩만 가져올 수 있다 ..
                            val deferredProducts = top3.map { countryRanking ->
                                async {
                                    countryRanking.country to loadCountryProducts(countryRanking.country)
                                }
                            }
                            val countryProductsMap = deferredProducts.awaitAll().toMap()
                            
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                topCountries = top3,
                                countryProducts = countryProductsMap
                            )
                            
                            isInitialLoad = false
                        } catch (e: Exception) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "랭킹 데이터를 불러오는데 실패했습니다: ${e.message}"
                            )
                            isInitialLoad = false
                        }
                    }
                }
            }
    }
    
    // 국가별 상품 랭킹 실시간 리스너
    private fun setupCountryProductsListener(country: String) {
        val listener = db.collection("ranking_countries")
            .document(country)
            .collection("products")
            .orderBy("count", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(5) // 5위까지만 보여주기
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                
                snapshot?.let {
                    viewModelScope.launch {
                        try {
                            val products = mutableListOf<ProductRanking>()
                            
                            it.documents.forEach { doc ->
                                val productId = doc.id
                                val count = doc.getLong("count") ?: 0L // 기본값 0
                                
                                // products 컬렉션에서 상품 정보 가져오기
                                val productDoc = db.collection("products")
                                    .document(productId)
                                    .get()
                                    .await()
                                
                                if (productDoc.exists()) {
                                    val productName = productDoc.getString("productName") ?: ""
                                    val category = productDoc.getString("category") ?: ""
                                    val imageUrls = (productDoc.get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                                    
                                    products.add(ProductRanking(productId, productName, category, imageUrls, count))
                                }
                            }
                            
                            // 현재 상태 업데이트
                            val currentProducts = _uiState.value.countryProducts.toMutableMap()
                            currentProducts[country] = products
                            
                            _uiState.value = _uiState.value.copy(
                                countryProducts = currentProducts
                            )
                        } catch (e: Exception) {
                            // 에러 무시
                        }
                    }
                }
            }
        
        productListeners[country] = listener
    }
    
    // TOP3 국가 로드 (초기 로드용, 초기 로드 이후엔 실시간 리스너 사용 예정)
    private fun loadTopCountries() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                // ranking_countries 컬렉션에서 모든 국가 문서 가져오기
                val snapshot = db.collection("ranking_countries")
                    .get()
                    .await()
                
                val countries = snapshot.documents.mapNotNull { doc ->
                    val country = doc.id
                    val totalCount = doc.getLong("totalCount") ?: 0L // 기본값 0
                    CountryRanking(country, totalCount)
                }
                
                // totalCount 내림차순 정렬 후 TOP3 추출
                val top3 = countries.sortedByDescending { it.totalCount }.take(3)
                
                // 각 국가별 상품 랭킹 병렬 로드
                // - 병렬 로드가 아니면 한 번에 한 국가의 상품씩만 가져올 수 있다 ..
                val deferredProducts = top3.map { countryRanking ->
                    async {
                        countryRanking.country to loadCountryProducts(countryRanking.country)
                    }
                }
                val countryProductsMap = deferredProducts.awaitAll().toMap()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    topCountries = top3,
                    countryProducts = countryProductsMap
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "랭킹 데이터를 불러오는데 실패했습니다: ${e.message}"
                )
            }
        }
    }
    
    // 국가별 상품 랭킹 로드 (TOP5)
    private suspend fun loadCountryProducts(country: String): List<ProductRanking> {
        return try {
            val snapshot = db.collection("ranking_countries")
                .document(country)
                .collection("products")
                .orderBy("count", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .await()
            
            val products = mutableListOf<ProductRanking>()
            
            snapshot.documents.forEach { doc ->
                val productId = doc.id
                val count = doc.getLong("count") ?: 0L // 기본값 0
                
                // products 컬렉션에서 상품 정보 가져오기
                val productDoc = db.collection("products")
                    .document(productId)
                    .get()
                    .await()
                
                if (productDoc.exists()) {
                    val productName = productDoc.getString("productName") ?: ""
                    val category = productDoc.getString("category") ?: ""
                    val imageUrls = (productDoc.get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    
                    products.add(ProductRanking(productId, productName, category, imageUrls, count))
                }
            }
            
            products
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // 선택된 국가의 모든 상품 랭킹 로드
    fun loadCountryProductRanking(country: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, selectedCountry = country)
            
            try {
                val snapshot = db.collection("ranking_countries")
                    .document(country)
                    .collection("products")
                    .orderBy("count", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                val products = mutableListOf<ProductRanking>()
                
                snapshot.documents.forEach { doc ->
                    val productId = doc.id
                    val count = doc.getLong("count") ?: 0L // 기본값 0
                    
                    // products 컬렉션에서 상품 정보 가져오기
                    val productDoc = db.collection("products")
                        .document(productId)
                        .get()
                        .await()
                    
                    if (productDoc.exists()) {
                        val productName = productDoc.getString("productName") ?: ""
                        val category = productDoc.getString("category") ?: ""
                        val imageUrls = (productDoc.get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                        
                        products.add(ProductRanking(productId, productName, category, imageUrls, count))
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    countryProducts = mapOf(country to products)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "상품 랭킹을 불러오는데 실패했습니다: ${e.message}"
                )
            }
        }
    }
    
    // 선택된 상점의 상품 랭킹 로드
    fun loadPlaceProductRanking(placeId: String, placeName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                selectedPlaceId = placeId,
                selectedPlaceName = placeName,
                selectedCountry = null
            )
            
            try {
                val snapshot = db.collection("ranking_places")
                    .document(placeId)
                    .collection("products")
                    .orderBy("count", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                val products = mutableListOf<ProductRanking>()
                
                snapshot.documents.forEach { doc ->
                    val productId = doc.id
                    val count = doc.getLong("count") ?: 0L // 기본값 0
                    
                    // products 컬렉션에서 상품 정보 가져오기
                    val productDoc = db.collection("products")
                        .document(productId)
                        .get()
                        .await()
                    
                    if (productDoc.exists()) {
                        val productName = productDoc.getString("productName") ?: ""
                        val category = productDoc.getString("category") ?: ""
                        val imageUrls = (productDoc.get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                        
                        products.add(ProductRanking(productId, productName, category, imageUrls, count))
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    placeProducts = products
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "상점 상품 랭킹을 불러오는데 실패했습니다: ${e.message}"
                )
            }
        }
    }
    
    // 모든 국가 목록 로드 (검색용)
    fun loadAllCountries() {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("ranking_countries")
                    .get()
                    .await()
                
                val countries = snapshot.documents.map { it.id }.sorted()
                
                _uiState.value = _uiState.value.copy(allCountries = countries)
            } catch (e: Exception) {
                // 에러 무시
            }
        }
    }
    
    // 국가 검색
    fun searchCountries(query: String) {
        val searchQuery = query.trim()
        _uiState.value = _uiState.value.copy(searchQuery = searchQuery)
        
        if (searchQuery.isEmpty()) {
            _uiState.value = _uiState.value.copy(filteredCountries = emptyList())
            return
        }
        
        val allCountries = _uiState.value.allCountries
        val filtered = allCountries.filter { country ->
            country.contains(searchQuery, ignoreCase = true) // 대소문자 구문 없이 검색
                                                             // 지금은 한국어로 검색해서 필요 없긴 한데
                                                             // 추후 영어 확장을 위해 남겨둠
        }
        
        _uiState.value = _uiState.value.copy(filteredCountries = filtered)
    }
    
    // 선택 초기화
    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedCountry = null,
            selectedPlaceId = null,
            selectedPlaceName = null,
            placeProducts = emptyList(),
            searchQuery = "",
            filteredCountries = emptyList()
        )
    }
    
    // 에러 메시지 초기화
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

