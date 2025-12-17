package com.example.tripcart.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.util.UUID

// 입력 중인 상품 데이터 (임시 저장용)
data class DraftProduct(
    val productImages: List<android.net.Uri> = emptyList(),
    val productName: String = "",
    val productMemo: String = "",
    val quantity: Int = 1,
    val selectedCategory: String? = null,
    val isPublic: Boolean = false,
    val isFromFirestore: Boolean = false,
    val firestoreProductId: String? = null,
    val firestoreImageUrls: List<String> = emptyList()
)

// Firestore에서 검색된 상품 정보
data class SearchedProduct(
    val productId: String,
    val productName: String,
    val category: String,
    val imageUrls: List<String>,
    val totalScore: Double = 0.0,
    val reviewCount: Int = 0
)

// 리뷰 정보
data class Review(
    val reviewId: String,
    val productId: String,
    val userId: String,
    val userName: String? = null,
    val rating: Int, // 1-5 별점
    val content: String? = null,
    val imageUrls: List<String> = emptyList(),
    val createdAt: com.google.firebase.Timestamp
)

data class ProductUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false,
    val savedProduct: com.example.tripcart.ui.screen.ProductDetails? = null, // 저장된 상품 정보
    val draftProduct: DraftProduct = DraftProduct(), // 입력 중인 상품 데이터
    val hasNavigatedToAddToList: Boolean = false, // AddProductToListScreen으로 이동하는지 여부
    val searchResults: List<SearchedProduct> = emptyList(), // 검색 결과
    val allProducts: List<SearchedProduct> = emptyList(), // 전체 상품 목록
    val currentProduct: SearchedProduct? = null, // 현재 선택된 상품 (리뷰 페이지용)
    val reviews: List<Review> = emptyList(), // 리뷰 목록
    val isLoadingReviews: Boolean = false, // 리뷰 로딩 중
    val favoriteProductIds: Set<String> = emptySet() // 찜한 상품 ID 목록
)

class ProductViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ProductUiState())
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()
    
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // favorite 리스너
    private var favoriteListener: com.google.firebase.firestore.ListenerRegistration? = null
    
    override fun onCleared() {
        super.onCleared()
        favoriteListener?.remove()
    }
    
    // 이미지 업로드 경로 타입
    enum class ImagePathType {
        PUBLIC,    // products/public/ - 모든 사용자 접근 가능
        USER,      // products/user/{userId}/ - 업로드한 사용자만 접근 가능
        LIST       // products/list/{listId}/ - 리스트 공유 사용자만 접근 가능
    }
    
    /*
     * Firebase Storage에 이미지 업로드
     * @param imageUri 업로드할 이미지 URI - Storage에 업로드하기 전,
     *                                    로컬에서 이미지 다루기 위해 사용!
     *                                    화면 닫히면 사라지는 임시 경로라 로컬 DB에는 영향 X
     * @param pathType 이미지 저장 경로 타입 (PUBLIC, USER, LIST)
     * @param listId 리스트 ID (pathType이 LIST일 경우 필요)
     * @return 업로드된 이미지의 다운로드 URL - Storage에 업로드된 이미지의 공개 URL
     */
    private suspend fun uploadImage(
        imageUri: Uri,
        pathType: ImagePathType = ImagePathType.USER,
        listId: String? = null
    ): String {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("사용자가 로그인되어 있지 않습니다.")
        val imageId = UUID.randomUUID().toString()
        
        val imageRef = when (pathType) {
            // 공개 상품은 products/public/ 경로에 저장
            ImagePathType.PUBLIC -> {
                storage.reference.child("products/public/$imageId.jpg")
            }
            // 비공개 상품은 products/user/{userId}/ 경로에 저장
            ImagePathType.USER -> {
                storage.reference.child("products/user/$userId/$imageId.jpg")
            }
            // 초대코드가 발급된 리스트 내의 상품은 products/list/{listId}/ 경로에 저장
            ImagePathType.LIST -> {
                val listIdValue = listId ?: throw IllegalArgumentException("LIST 타입일 경우 listId가 필요합니다.")
                storage.reference.child("products/list/$listIdValue/$imageId.jpg")
            }
        }
        
        val uploadTask = imageRef.putFile(imageUri).await()
        return uploadTask.storage.downloadUrl.await().toString()
    }
    
    // 여러 이미지를 업로드하고 URL 리스트 반환 (병렬 처리)
    private suspend fun uploadImages(
        imageUris: List<Uri>,
        pathType: ImagePathType = ImagePathType.USER,
        listId: String? = null
    ): List<String> = coroutineScope {
        imageUris.map { uri: Uri ->
            async<String> {
                uploadImage(uri, pathType, listId)
            }
        }.map { deferred ->
            deferred.await()
        }
    }
    
    /*
     * 이미지를 다른 경로로 복사
     * 초대코드가 발급된 리스트의 경우 (products/user/{userId}/ -> products/list/{listId}/)
     */
    suspend fun copyImageToList(sourceUrl: String, targetListId: String): String {
        val imageId = UUID.randomUUID().toString()
        
        // 원본 이미지 다운로드 (바이트 배열로)
        val sourceRef = storage.getReferenceFromUrl(sourceUrl)
        val bytes = sourceRef.getBytes(Long.MAX_VALUE).await()
        
        // 새 경로에 업로드
        val targetRef = storage.reference.child("products/list/$targetListId/$imageId.jpg")
        val uploadTask = targetRef.putBytes(bytes).await()
        
        return uploadTask.storage.downloadUrl.await().toString()
    }
    
    // 상품 저장
    fun saveProduct(
        productName: String,
        productMemo: String,
        quantity: Int,
        category: String,
        imageUris: List<Uri>,
        isPublic: Boolean
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                errorMessage = null,
                saveSuccess = false,
                hasNavigatedToAddToList = false
            )
            
            try {
                val userId = auth.currentUser?.uid
                    ?: throw IllegalStateException("사용자가 로그인되어 있지 않습니다.")
                
                // 이미지 업로드는 하지 않고 Uri만 전달 (AddProductToListScreen에서 리스트 선택 시 업로드)
                // 상품 고유 ID 생성 (로컬용)
                val productId = UUID.randomUUID().toString()
                
                // 공개 상품인 경우 products 컬렉션 업로드는
                // AddProductListScreen에서 리스트 추가 시점에 이미지 업로드 후 저장하도록
                var firestoreProductId: String? = null
                
                // 저장된 상품 정보 저장 (이미지 URL은 null, Uri만 전달)
                val savedProduct = com.example.tripcart.ui.screen.ProductDetails(
                    id = productId,
                    productName = productName,
                    category = category,
                    imageUrls = null, // 아직 업로드하지 않음
                    imageUris = imageUris, // Uri 전달
                    quantity = quantity,
                    note = productMemo.ifBlank { null },
                    productId = firestoreProductId,
                    isPublic = isPublic // 공개 여부 전달
                )
                
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveSuccess = true,
                    savedProduct = savedProduct
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "상품 저장에 실패했습니다: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(
            saveSuccess = false,
            savedProduct = null,
            hasNavigatedToAddToList = false
        )
    }
    
    fun setHasNavigatedToAddToList(hasNavigated: Boolean) {
        _uiState.value = _uiState.value.copy(hasNavigatedToAddToList = hasNavigated)
    }
    
    // 입력 중인 상품 데이터 저장
    fun saveDraftProduct(draftProduct: DraftProduct) {
        _uiState.value = _uiState.value.copy(draftProduct = draftProduct)
    }
    
    // 입력 중인 상품 데이터 초기화
    fun clearDraftProduct() {
        _uiState.value = _uiState.value.copy(draftProduct = DraftProduct())
    }
    
    // 리스트에 상품 이미지 업로드 (LIST 경로 사용)
    suspend fun uploadImagesToList(imageUris: List<Uri>, listId: String): List<String> {
        return uploadImages(imageUris, pathType = ImagePathType.LIST, listId = listId)
    }
    
    // 공개/비공개에 따라 이미지 업로드 (공개면 public, 비공개면 user 경로)
    suspend fun uploadImagesForProduct(imageUris: List<Uri>, isPublic: Boolean): List<String> {
        return uploadImages(
            imageUris,
            pathType = if (isPublic) ImagePathType.PUBLIC else ImagePathType.USER
        )
    }
    
    // 공개 상품을 Firestore products 컬렉션에 저장
    suspend fun savePublicProductToFirestore(
        productName: String,
        category: String,
        imageUrls: List<String>
    ): String? {
        return try {
            if (imageUrls.isEmpty()) {
                // 이미지가 없으면 저장하지 않음
                return null
            }
            
            val productData = hashMapOf(
                "productName" to productName,
                "category" to category,
                "imageUrls" to imageUrls
            )
            
            // Firestore에 저장 (자동 생성된 20자리 영숫자 ID 사용)
            val documentRef = db.collection("products")
                .add(productData)
                .await()
            
            documentRef.id // 자동 생성된 20자리 영숫자 ID 반환
        } catch (e: Exception) {
            android.util.Log.e("ProductViewModel", "Failed to save public product to Firestore", e)
            null
        }
    }
    
    // 초대코드가 발급된 리스트는 이미지들을 user 경로에서 list 경로로 복사
    suspend fun copyImagesToList(imageUrls: List<String>, listId: String): List<String> {
        return imageUrls.map { url ->
            copyImageToList(url, listId)
        }
    }
    
    // 캐시된 allProducts에서 상품 검색 (부분 문자열 검색)
    // 검색어가 상품명에 포함되어 있으면 모두 결과로 반환
    // - Firestore products 컬렉션에서 직접 검색하면 접두사 검색만 지원하기 때문에
    //   첫 글자로만 필터링이 가능함
    //   -> 모든 위치의 글자로도 검색이 가능하도록
    //      FavoriteProductsScreen과 AllProductsScreen에서 사용 중이던 allProducts을 사용
    //      -> 세 화면에 각각 진입할 때마다 새로고침해서 allProducts 목록을 업데이트 하도록!
    fun searchProducts(keyword: String) {
        viewModelScope.launch {
            if (keyword.isBlank()) {
                _uiState.value = _uiState.value.copy(searchResults = emptyList())
                return@launch
            }
            
            // allProducts가 비어있으면 먼저 로드
            if (_uiState.value.allProducts.isEmpty()) {
                loadAllProducts(showLoading = false)
                // loadAllProducts가 완료될 때까지 잠시 대기
                kotlinx.coroutines.delay(100)
            }
            
            val keywordLower = keyword.lowercase() // 전체 소문자 버전
            val allProducts = _uiState.value.allProducts
            
            // 상품명에 검색어가 포함되어 있는지 확인
            val results = allProducts
                .filter { 
                    it.productName.lowercase().contains(keywordLower)
                }
                .take(20) // 최대 20개 결과만 반환
            
            _uiState.value = _uiState.value.copy(
                searchResults = results
            )
        }
    }
    
    // Firestore에서 상품 불러오기
    fun loadProductFromFirestore(productId: String) {
        viewModelScope.launch {
            try {
                val doc = db.collection("products")
                    .document(productId)
                    .get()
                    .await()
                
                if (!doc.exists()) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "상품을 찾을 수 없습니다."
                    )
                    return@launch
                }
                
                val productName = doc.getString("productName") ?: ""
                val category = doc.getString("category") ?: ""
                val imageUrls = (doc.get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                val totalScore = (doc.get("totalScore") as? Number)?.toDouble() ?: 0.0
                val reviewCount = (doc.get("reviewCount") as? Number)?.toInt() ?: 0
                
                // DraftProduct 업데이트
                val currentDraft = _uiState.value.draftProduct
                val updatedDraft = currentDraft.copy(
                    productName = productName,
                    selectedCategory = category,
                    isFromFirestore = true, // Firestore에서 불러온 상품이다!
                    firestoreProductId = productId,
                    firestoreImageUrls = imageUrls,
                    isPublic = false // 불러온 상품은 공개 불가
                )
                
                _uiState.value = _uiState.value.copy(draftProduct = updatedDraft)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "상품 불러오기에 실패했습니다: ${e.message}"
                )
            }
        }
    }
    
    // 검색 결과 초기화
    fun clearSearchResults() {
        _uiState.value = _uiState.value.copy(searchResults = emptyList())
    }
    
    // Firestore에서 불러온 상품 저장 (이미지는 새로 추가한 이미지만 업로드)
    fun saveProductFromFirestore(
        productName: String,
        productMemo: String,
        quantity: Int,
        category: String,
        existingImageUrls: List<String>,
        newImageUris: List<Uri>,
        firestoreProductId: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                errorMessage = null,
                saveSuccess = false,
                hasNavigatedToAddToList = false
            )
            
            try {
                // 새로 추가한 이미지만 업로드
                val newImageUrls = if (newImageUris.isNotEmpty()) {
                    uploadImages(newImageUris, pathType = ImagePathType.USER)
                } else {
                    emptyList()
                }
                
                // 기존 이미지 URL + 새로 업로드한 이미지 URL 합치기
                val allImageUrls = existingImageUrls + newImageUrls
                
                // 상품 고유 ID 생성
                val productId = UUID.randomUUID().toString()
                
                // 저장된 상품 정보 저장
                val savedProduct = com.example.tripcart.ui.screen.ProductDetails(
                    id = productId,
                    productName = productName,
                    category = category,
                    imageUrls = allImageUrls,
                    quantity = quantity,
                    note = productMemo.ifBlank { null },
                    productId = firestoreProductId,
                    isPublic = false // Firestore에서 불러온 상품은 공개 불가
                )
                
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveSuccess = true,
                    savedProduct = savedProduct
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "상품 저장에 실패했습니다: ${e.message}"
                )
            }
        }
    }
    
    // Firestore products 컬렉션에서 모든 상품 불러오기
    fun loadAllProducts(showLoading: Boolean = true) {
        viewModelScope.launch {
            // 이미 데이터가 있으면 로딩을 표시하지 않음 (깜빡임 방지)
            if (showLoading || _uiState.value.allProducts.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }
            try {
                val snapshot = db.collection("products").get().await()
                val products = snapshot.documents.mapNotNull { doc ->
                    val productName = doc.getString("productName") ?: return@mapNotNull null
                    val category = doc.getString("category") ?: ""
                    val imageUrls = (doc.get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    val totalScore = (doc.get("totalScore") as? Number)?.toDouble() ?: 0.0
                    val reviewCount = (doc.get("reviewCount") as? Number)?.toInt() ?: 0
                    
                    SearchedProduct(
                        productId = doc.id,
                        productName = productName,
                        category = category,
                        imageUrls = imageUrls,
                        totalScore = totalScore,
                        reviewCount = reviewCount
                    )
                }.sortedByDescending { product ->
                    // 평균 별점 높은 순으로 정렬
                    if (product.reviewCount > 0) {
                        product.totalScore / product.reviewCount
                    } else {
                        0.0
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    allProducts = products,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "상품 불러오기에 실패했습니다: ${e.message}"
                )
            }
        }
    }
    
    // 필터링된 상품 목록 반환
    fun getFilteredProducts(
        searchKeyword: String?,
        selectedCategories: List<String>
    ): List<SearchedProduct> {
        val allProducts = _uiState.value.allProducts
        
        return allProducts.filter { product ->
            // 이름 검색 필터
            // searchKeyword?.let - searchKeyword가 null이 아닐 때 검색 수행
            val matchesSearch = searchKeyword?.let { keyword ->
                product.productName.contains(keyword, ignoreCase = true) // 대소문자 무시
            } ?: true // searchKeyword가 null이면 모든 상품 통과
            
            // 카테고리 필터
            val matchesCategory = if (selectedCategories.isEmpty()) {
                true // selectedCategories가 비어있으면 모든 상품 통과
            } else {
                selectedCategories.contains(product.category)
            }
            
            matchesSearch && matchesCategory
        }
    }
    
    // 상품 정보 로드 (리뷰 페이지용)
    fun loadProduct(productId: String) {
        viewModelScope.launch {
            try {
                val doc = db.collection("products")
                    .document(productId)
                    .get()
                    .await()
                
                if (!doc.exists()) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "상품을 찾을 수 없습니다."
                    )
                    return@launch
                }
                
                val productName = doc.getString("productName") ?: ""
                val category = doc.getString("category") ?: ""
                val imageUrls = (doc.get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                val totalScore = (doc.get("totalScore") as? Number)?.toDouble() ?: 0.0
                val reviewCount = (doc.get("reviewCount") as? Number)?.toInt() ?: 0
                
                val product = SearchedProduct(
                    productId = doc.id,
                    productName = productName,
                    category = category,
                    imageUrls = imageUrls,
                    totalScore = totalScore,
                    reviewCount = reviewCount
                )
                
                _uiState.value = _uiState.value.copy(currentProduct = product)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "상품 불러오기에 실패했습니다: ${e.message}"
                )
            }
        }
    }
    
    // 리뷰 목록 로드
    fun loadReviews(productId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingReviews = true)
            try {
                val snapshot = db.collection("products")
                    .document(productId)
                    .collection("reviews")
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                val reviews = snapshot.documents.mapNotNull { doc ->
                    val reviewId = doc.id
                    val userId = doc.getString("userId") ?: return@mapNotNull null
                    val userName = doc.getString("userName")
                    val rating = (doc.get("rating") as? Number)?.toInt() ?: return@mapNotNull null
                    val content = doc.getString("content")
                    val imageUrls = (doc.get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    val createdAt = doc.getTimestamp("createdAt") ?: return@mapNotNull null
                    
                    Review(
                        reviewId = reviewId,
                        productId = productId,
                        userId = userId,
                        userName = userName,
                        rating = rating,
                        content = content,
                        imageUrls = imageUrls,
                        createdAt = createdAt
                    )
                }
                
                _uiState.value = _uiState.value.copy(
                    reviews = reviews,
                    isLoadingReviews = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingReviews = false,
                    errorMessage = "리뷰 불러오기에 실패했습니다: ${e.message}"
                )
            }
        }
    }
    
    // 리뷰 작성
    fun writeReview(
        productId: String,
        rating: Int,
        content: String?,
        imageUris: List<Uri>
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            try {
                val userId = auth.currentUser?.uid
                    ?: throw IllegalStateException("사용자가 로그인되어 있지 않습니다.")
                
                // 이미지 업로드
                val imageUrls = if (imageUris.isNotEmpty()) {
                    imageUris.map { uri ->
                        val imageId = UUID.randomUUID().toString()
                        val imageRef = storage.reference.child("reviews/$imageId.jpg")
                        val uploadTask = imageRef.putFile(uri).await()
                        uploadTask.storage.downloadUrl.await().toString()
                    }
                } else {
                    emptyList()
                }
                
                // reviewId 생성 (productId + UUID)
                val reviewId = "${productId}_${UUID.randomUUID()}"
                
                // 리뷰 저장
                val reviewData = hashMapOf(
                    "reviewId" to reviewId,
                    "productId" to productId,
                    "userId" to userId,
                    "rating" to rating,
                    "content" to (content ?: ""),
                    "imageUrls" to imageUrls,
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                
                db.collection("products")
                    .document(productId)
                    .collection("reviews")
                    .document(reviewId)
                    .set(reviewData)
                    .await()
                
                // 상품의 totalScore와 reviewCount 업데이트
                val productRef = db.collection("products").document(productId) // 경로 (주소만 가져옴)
                val productDoc = productRef.get().await() // 상단에서 주어진 경로를 이용해 실제 데이터를 가져옴
                
                val currentTotalScore = (productDoc.get("totalScore") as? Number)?.toDouble() ?: 0.0
                val currentReviewCount = (productDoc.get("reviewCount") as? Number)?.toInt() ?: 0
                
                val newTotalScore = currentTotalScore + rating
                val newReviewCount = currentReviewCount + 1
                
                productRef.update(
                    "totalScore", newTotalScore,
                    "reviewCount", newReviewCount
                ).await()
                
                // 현재 상품 정보 업데이트
                val updatedProduct = _uiState.value.currentProduct?.copy(
                    totalScore = newTotalScore,
                    reviewCount = newReviewCount
                )
                
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveSuccess = true,
                    currentProduct = updatedProduct
                )
                
                // 리뷰 목록 새로고침
                loadReviews(productId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "리뷰 작성에 실패했습니다: ${e.message}"
                )
            }
        }
    }
    
    // 찜한 상품 목록 로드
    fun loadFavorites() {
        val userId = auth.currentUser?.uid ?: return
        
        favoriteListener?.remove() // 기존 리스너 제거 (리스너 중복 방지)
        
        favoriteListener = db.collection("users")
            .document(userId)
            .collection("favorite")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                
                snapshot?.let {
                    // 각 문서의 ID만 추출해 리스트로 변환
                    // 단, 중복 제거를 위해 toSet 이용해서 Set으로 변환
                    val favoriteIds = it.documents.map { doc -> doc.id }.toSet()
                    _uiState.value = _uiState.value.copy(favoriteProductIds = favoriteIds)
                }
            }
    }
    
    // 찜한 상품 추가
    fun addFavorite(productId: String) {
        val userId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            try {
                db.collection("users")
                    .document(userId)
                    .collection("favorite")
                    .document(productId)
                    .set(mapOf("productId" to productId))
                    .await()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "찜하기에 실패했습니다: ${e.message}"
                )
            }
        }
    }
    
    // 찜한 상품 제거
    fun removeFavorite(productId: String) {
        val userId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            try {
                db.collection("users")
                    .document(userId)
                    .collection("favorite")
                    .document(productId)
                    .delete()
                    .await()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "찜 해제에 실패했습니다: ${e.message}"
                )
            }
        }
    }
    
    // 찜한 상품인지 확인
    fun isFavorite(productId: String): Boolean {
        return _uiState.value.favoriteProductIds.contains(productId)
    }
    
    // 찜 아이콘 버튼 클릭시 찜 추가, 해제 변환
    fun toggleFavorite(productId: String) {
        if (isFavorite(productId)) {
            removeFavorite(productId)
        } else {
            addFavorite(productId)
        }
    }
    
    // 찜한 상품 목록 불러오기
    fun loadFavoriteProducts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val userId = auth.currentUser?.uid
                    ?: throw IllegalStateException("사용자가 로그인되어 있지 않습니다.")
                
                // users/{userId}/favorite 하위 컬렉션에서 상품 ID 목록 가져오기
                val favoriteSnapshot = db.collection("users")
                    .document(userId)
                    .collection("favorite")
                    .get()
                    .await()
                
                val favoriteProductIds = favoriteSnapshot.documents.map { it.id }
                
                if (favoriteProductIds.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        allProducts = emptyList(),
                        isLoading = false
                    )
                    return@launch
                }
                
                // 각 상품 ID로 products 컬렉션에서 상세 정보 불러오기
                val products = favoriteProductIds.mapNotNull { productId ->
                    try {
                        val doc = db.collection("products")
                            .document(productId)
                            .get()
                            .await()
                        
                        if (!doc.exists()) {
                            return@mapNotNull null
                        }
                        
                        val productName = doc.getString("productName") ?: return@mapNotNull null
                        val category = doc.getString("category") ?: ""
                        val imageUrls = (doc.get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                        val totalScore = (doc.get("totalScore") as? Number)?.toDouble() ?: 0.0
                        val reviewCount = (doc.get("reviewCount") as? Number)?.toInt() ?: 0
                        
                        SearchedProduct(
                            productId = doc.id,
                            productName = productName,
                            category = category,
                            imageUrls = imageUrls,
                            totalScore = totalScore,
                            reviewCount = reviewCount
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    allProducts = products,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "찜한 상품 불러오기에 실패했습니다: ${e.message}"
                )
            }
        }
    }
}


