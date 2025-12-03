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
    val isPublic: Boolean = false
)

data class ProductUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false,
    val savedProduct: com.example.tripcart.ui.screen.ProductDetails? = null, // 저장된 상품 정보
    val draftProduct: DraftProduct = DraftProduct(), // 입력 중인 상품 데이터
    val hasNavigatedToAddToList: Boolean = false // AddProductToListScreen으로 이동하는지 여부
)

class ProductViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ProductUiState())
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()
    
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
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
                
                // 이미지 업로드 (로컬 URI -> Firebase Storage URL)
                // 공개 상품은 public 경로에, 비공개 상품은 user 경로에 저장
                val imageUrls = if (imageUris.isNotEmpty()) {
                    uploadImages(
                        imageUris,
                        pathType = if (isPublic) ImagePathType.PUBLIC else ImagePathType.USER
                    )
                } else {
                    emptyList()
                }
                
                // 상품 고유 ID 생성
                val productId = UUID.randomUUID().toString()
                
                // 공개 상품인 경우 products 컬렉션에 저장 (productName, category, imageUrls만)
                // productId는 Firestore 문서 ID로 자동 생성됨
                var firestoreProductId: String? = null
                if (isPublic) {
                    val productData = hashMapOf(
                        "productName" to productName,
                        "category" to category,
                        "imageUrls" to imageUrls
                    )
                    val documentRef = db.collection("products")
                        .add(productData)
                        .await()
                    firestoreProductId = documentRef.id // Firestore 문서 ID
                }
                
                // 저장된 상품 정보 저장
                val savedProduct = com.example.tripcart.ui.screen.ProductDetails(
                    id = productId,
                    productName = productName,
                    category = category,
                    imageUrls = imageUrls,
                    quantity = quantity,
                    note = productMemo.ifBlank { null },
                    productId = firestoreProductId
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
    
    // 초대코드가 발급된 리스트는 이미지들을 user 경로에서 list 경로로 복사
    suspend fun copyImagesToList(imageUrls: List<String>, listId: String): List<String> {
        return imageUrls.map { url ->
            copyImageToList(url, listId)
        }
    }
}

