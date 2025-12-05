package com.example.tripcart.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripcart.data.local.TripCartDatabase
import com.example.tripcart.data.local.dao.*
import com.example.tripcart.data.local.entity.*
import com.example.tripcart.ui.viewmodel.PlaceDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class ListItemUiState(
    val listId: String,
    val name: String,
    val status: String,
    val country: String?,
    val places: List<Place>,  // Place 객체 리스트 (name, placeId 연결)
    val productCount: Int,
    val isSelected: Boolean = false,
    val isFromFirestore: Boolean = false // Firestore에서 온 리스트인지 구분
)

data class ListUiState(
    val lists: List<ListItemUiState> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ListViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ListUiState())
    val uiState: StateFlow<ListUiState> = _uiState.asStateFlow()
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // Room Database
    private val roomDb = androidx.room.Room.databaseBuilder(
        application,
        TripCartDatabase::class.java,
        "tripcart_database"
    )
        .fallbackToDestructiveMigration() // DB 버전 변경 시 데이터베이스 재생성 (테스트용!)
        .build()
    
    private val listDao = roomDb.listDao()
    private val listProductDao = roomDb.listProductDao()
    
    init {
        loadLists()
    }
    
    // 로컬 DB와 Firestore에서 리스트 로드
    fun loadLists() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Room DB와 Firestore에서 리스트 병합하여 가져오기
            combine(
                listDao.getAllLists(), // Room DB 리스트
                getFirestoreListsFlow() // Firestore 리스트
            ) { roomLists, firestoreLists ->
                // 리스트 ID를 키로 하는 맵 생성 (중복 제거)
                val mergedLists = mutableMapOf<String, Pair<ListEntity, Boolean>>()
                
                // Room DB 리스트 추가 (isFromFirestore = false)
                roomLists.forEach { list ->
                    mergedLists[list.listId] = Pair(list, false)
                }
                
                // Firestore 리스트 추가
                // Firestore에는 채팅, 권한 등 추가 필드가 있어서 Room DB에 저장하지 않음
                firestoreLists.forEach { list ->
                    if (!mergedLists.containsKey(list.listId)) {
                        mergedLists[list.listId] = Pair(list, true) // isFromFirestore = true
                        // Firestore 구조가 달라서 Room DB에는 저장하지 않음
                    }
                }
                
                // 정렬: 1) Room DB 리스트 먼저, 2) Firestore 리스트 나중에
                // 각 그룹 내에서는 이름 순으로 정렬
                mergedLists.values.toList().sortedWith(
                    compareBy<Pair<ListEntity, Boolean>> { it.second } // false(Room DB) 먼저, true(Firestore) 나중
                        .thenBy { it.first.name } // 이름 순으로 정렬
                )
            }.collect { mergedLists ->
                // 각 리스트에 대한 정보를 변환
                val listItems = mergedLists.map { (list, isFromFirestore) ->
                    async {
                        var productCount = list.productCount
                        var places = list.places
                        
                        // Firestore 리스트인 경우 productCount 필드를 사용
                        if (isFromFirestore) {
                            try {
                                val listDoc = db.collection("lists").document(list.listId).get().await()
                                if (listDoc.exists()) {
                                    // productCount 필드 사용
                                    val docProductCount = (listDoc.getLong("productCount") ?: listDoc.get("productCount") as? Number)?.toInt() ?: 0
                                    
                                    // productCount가 없거나 0인 경우 진행
                                    // productCount가 없는 기존 firestore 리스트들 일괄 업데이트 목적!
                                    if (docProductCount == 0) {
                                        try {
                                            val productsSnapshot = db.collection("lists")
                                                .document(list.listId)
                                                .collection("list_products")
                                                .get()
                                                .await()
                                            val actualCount = productsSnapshot.size()
                                            
                                            // 실제 상품이 있으면 productCount 필드 업데이트
                                            if (actualCount > 0) {
                                                db.collection("lists")
                                                    .document(list.listId)
                                                    .update("productCount", actualCount)
                                                    .await()
                                                productCount = actualCount
                                            } else {
                                                productCount = 0
                                            }
                                        } catch (e: Exception) {
                                            // 하위 컬렉션 조회 실패 시 문서의 productCount 사용
                                            productCount = docProductCount
                                        }
                                    } else {
                                        // productCount 필드가 있으면 그대로 사용
                                        productCount = docProductCount
                                    }
                                    
                                    // places 배열도 Firestore 문서에서 다시 가져오기
                                    val placesList = (listDoc.get("places") as? List<*>) ?: emptyList<Any?>()
                                    places = placesList.mapNotNull { placeMap ->
                                        when (placeMap) {
                                            is Map<*, *> -> {
                                                val placeName = placeMap["name"] as? String
                                                val placeId = placeMap["placeId"] as? String
                                                if (placeName != null && placeId != null) {
                                                    Place(name = placeName, placeId = placeId)
                                                } else null
                                            }
                                            else -> null
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                // 오류 발생
                            }
                        }
                        
                        ListItemUiState(
                            listId = list.listId,
                            name = list.name,
                            status = list.status,
                            country = list.country,
                            places = places,  // Place 객체 리스트 (name, placeId 연결)
                            productCount = productCount,
                            isFromFirestore = isFromFirestore
                        )
                    }
                }
                
                // 모든 작업 완료 대기
                val completedListItems = listItems.awaitAll()
                
                _uiState.value = _uiState.value.copy(
                    lists = completedListItems,
                    isLoading = false
                )
            }
        }
    }
    
    // 실시간 리스너를 이용해 Firestore에서 리스트를 실시간 Flow로 가져오기
    // ownerId 또는 sharedWith에 포함된 리스트를 모두 가져옴
    private fun getFirestoreListsFlow(): Flow<List<ListEntity>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val mergedLists = mutableMapOf<String, ListEntity>()
        
        // ownerId로 필터링한 리스트
        val ownerListener = db.collection("lists")
            .whereEqualTo("ownerId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("ListViewModel", "ownerId 리스트 가져오기 실패: ${error.message}", error)
                    return@addSnapshotListener
                }
                
                snapshot?.documents?.forEach { doc ->
                    val listEntity = parseListDocument(doc)
                    mergedLists[listEntity.listId] = listEntity
                }
                
                // 병합된 리스트 전송
                trySend(mergedLists.values.toList())
            }
        
        // sharedWith 배열에 포함된 리스트
        val sharedListener = db.collection("lists")
            .whereArrayContains("sharedWith", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("ListViewModel", "sharedWith 리스트 가져오기 실패: ${error.message}", error)
                    return@addSnapshotListener
                }
                
                snapshot?.documents?.forEach { doc ->
                    val listEntity = parseListDocument(doc)
                    mergedLists[listEntity.listId] = listEntity
                }
                
                // 병합된 리스트 전송
                trySend(mergedLists.values.toList())
            }
        
        // Flow가 취소될 때 리스너 해제
        awaitClose {
            ownerListener.remove()
            sharedListener.remove()
        }
    }
    
    // Firestore 문서를 ListEntity로 변환하는 헬퍼 함수
    private fun parseListDocument(doc: DocumentSnapshot): ListEntity {
        val listId = doc.id
        val name = doc.getString("name") ?: ""
        val status = doc.getString("status") ?: "준비중"
        val country = doc.getString("country")
        val productCount = (doc.getLong("productCount") ?: doc.get("productCount") as? Number)?.toInt() ?: 0
        // Firestore의 places 배열: [{"name": "...", "placeId": "..."}]
        val placesList = (doc.get("places") as? List<*>) ?: emptyList<Any?>()
        val places = placesList.mapNotNull { placeMap ->
            when (placeMap) {
                is Map<*, *> -> {
                    val placeName = placeMap["name"] as? String
                    val placeId = placeMap["placeId"] as? String
                    if (placeName != null && placeId != null) {
                        Place(name = placeName, placeId = placeId)
                    } else null
                }
                else -> null
            }
        }
        
        return ListEntity(
            listId = listId,
            name = name,
            status = status,
            country = country,
            places = places,  // List<Place> 객체 리스트
            productCount = productCount,
            firestoreSynced = true
        )
    }
    
    // Firestore에서 리스트 동기화
    private suspend fun syncListsFromFirestore() {
        val userId = auth.currentUser?.uid ?: return
        
        try {
            // ownerId로 필터링한 리스트
            val ownerListsSnapshot = db.collection("lists")
                .whereEqualTo("ownerId", userId)
                .get()
                .await()
            
            // sharedWith 배열에 포함된 리스트
            val sharedListsSnapshot = db.collection("lists")
                .whereArrayContains("sharedWith", userId)
                .get()
                .await()
            
            // 두 쿼리 결과를 병합하고 중복 제거
            val mergedLists = mutableMapOf<String, DocumentSnapshot>()
            
            ownerListsSnapshot.documents.forEach { doc ->
                mergedLists[doc.id] = doc
            }
            
            sharedListsSnapshot.documents.forEach { doc ->
                mergedLists[doc.id] = doc
            }
            
            mergedLists.values.forEach { doc ->
                val listEntity = parseListDocument(doc)
                
                // 로컬 DB에 리스트 저장
                listDao.insertList(listEntity)
            }
        } catch (e: Exception) {
            // Firestore 동기화 실패
            android.util.Log.e("ListViewModel", "Error syncing lists from Firestore", e)
        }
    }
    
    // 리스트 선택 상태 토글
    fun toggleListSelection(listId: String) {
        _uiState.value = _uiState.value.copy(
            lists = _uiState.value.lists.map { list ->
                if (list.listId == listId) {
                    list.copy(isSelected = !list.isSelected)
                } else {
                    list
                }
            }
        )
    }
    
    // 리스트 선택 (체크박스 ON)
    fun selectList(listId: String) {
        _uiState.value = _uiState.value.copy(
            lists = _uiState.value.lists.map { list ->
                if (list.listId == listId) {
                    list.copy(isSelected = true)
                } else {
                    list
                }
            }
        )
    }
    
    // 선택된 리스트들에 장소 추가
    suspend fun addPlaceToSelectedLists(placeDetails: PlaceDetails): Result<Unit> {
        return try {
            val selectedLists = _uiState.value.lists.filter { it.isSelected }
            
            
            // 각 선택된 리스트에 장소 추가
            selectedLists.forEach { listItem ->
                if (listItem.isFromFirestore) {
                    // Firestore 리스트인 경우 Firestore에서 직접 업데이트
                    try {
                        val listDoc = db.collection("lists").document(listItem.listId).get().await()
                        val currentPlaces = (listDoc.get("places") as? List<*>) ?: emptyList<Any?>()
                        val currentCountry = listDoc.getString("country")
                        
                        // 국가 검증
                        if (currentCountry != null && currentCountry != placeDetails.country) {
                            throw Exception("${listItem.name} 리스트에는 ${currentCountry}의 장소만 추가할 수 있습니다.")
                        }
                        
                        // 중복 체크 (Firestore의 places는 [{"name": "...", "placeId": "..."}] 형식)
                        val currentPlaceIds = currentPlaces.mapNotNull { 
                            when (it) {
                                is Map<*, *> -> it["placeId"] as? String
                                is String -> it  // 기존 형식 호환
                                else -> null
                            }
                        }
                        
                        if (placeDetails.placeId !in currentPlaceIds) {
                            val newPlace = hashMapOf("name" to placeDetails.name, "placeId" to placeDetails.placeId)
                            val updatedPlaces = currentPlaces.toMutableList().apply {
                                add(newPlace)
                            }
                            
                            // 국가 업데이트 (첫 장소인 경우)
                            val updateData = hashMapOf<String, Any>(
                                "places" to updatedPlaces
                            )
                            if (currentCountry == null && placeDetails.country != null) {
                                updateData["country"] = placeDetails.country
                            }
                            
                            db.collection("lists")
                                .document(listItem.listId)
                                .update(updateData)
                                .await()
                        }
                    } catch (e: Exception) {
                        throw Exception("${listItem.name} 리스트에 장소 추가 실패: ${e.message}")
                    }
                } else {
                    // Room DB 리스트인 경우 Room DB와 Firestore 모두 업데이트
                    val list = listDao.getListById(listItem.listId)
                    if (list != null) {
                        // 국가 검증: 리스트에 이미 장소가 있으면 같은 국가만 추가 가능
                        if (list.places.isNotEmpty()) {
                            // 기존 장소의 국가 확인 (Firestore에서)
                            val existingPlaceDoc = db.collection("places")
                                .document(list.places.first().placeId)
                                .get()
                                .await()
                            val existingCountry = existingPlaceDoc.getString("country")
                            
                            if (existingCountry != placeDetails.country) {
                                throw Exception("${list.name} 리스트에는 ${existingCountry}의 장소만 추가할 수 있습니다.")
                            }
                        }
                        
                        // 중복 체크
                        val placeExists = list.places.any { it.placeId == placeDetails.placeId }
                        if (!placeExists) {
                            // 로컬 DB에 Place 객체 추가
                            val updatedPlaces = list.places + Place(name = placeDetails.name, placeId = placeDetails.placeId)
                            // 국가 업데이트 (첫 장소인 경우)
                            if (list.country == null && placeDetails.country != null) {
                                listDao.updateList(list.copy(country = placeDetails.country, places = updatedPlaces))
                            } else {
                                listDao.updateList(list.copy(places = updatedPlaces))
                            }
                        }
                    }
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 새 리스트 생성
    suspend fun createNewList(placeDetails: PlaceDetails, listName: String = "새 리스트"): Result<String> {
        return try {
            val listId = UUID.randomUUID().toString()
            
            // 로컬 DB에 리스트 생성
            val listEntity = ListEntity(
                listId = listId,
                name = listName,
                status = "준비중",
                country = placeDetails.country,
                places = listOf(Place(name = placeDetails.name, placeId = placeDetails.placeId)),  // Place 객체로 저장
                productCount = 0,  // 새 리스트는 상품 개수 0
                firestoreSynced = false
            )
            listDao.insertList(listEntity)
            
            // 리스트 목록 새로고침
            loadLists()
            
            // 새로 생성한 리스트를 자동으로 선택 상태로 만들기
            // loadLists()가 비동기로 실행되므로 약간의 지연 후 선택 상태 설정
            viewModelScope.launch {
                kotlinx.coroutines.delay(500) // 리스트 로드 완료 대기 (Flow collect 완료 대기)
                selectList(listId)
            }
            
            Result.success(listId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 리스트 삭제
    fun deleteList(listId: String, isFromFirestore: Boolean) {
        viewModelScope.launch {
            try {
                // Firestore에서 삭제 시도 (없으면 패스)
                try {
                    db.collection("lists")
                        .document(listId)
                        .delete()
                        .await()
                } catch (e: Exception) {
                    // Firestore에 없으면 패스
                }
                
                // Room DB에서 삭제 시도 (없으면 패스)
                try {
                    listDao.deleteListById(listId)
                } catch (e: Exception) {
                    // Room DB에 없으면 패스
                }
                
                // 리스트 목록 다시 로드
                loadLists()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "리스트 삭제에 실패했습니다: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    // 리스트 상세보기: Firestore에서 기본 정보를 가져오고, 현재 운영 상태만 API로 실시간 조회
    suspend fun getListPlacesWithBusinessStatus(
        listId: String,
        placeViewModel: PlaceViewModel
    ): List<PlaceDetails> {
        return try {
            // 1. 리스트 정보 가져오기
            val listItem = _uiState.value.lists.find { it.listId == listId }
                ?: return emptyList()
            
            // 2. 각 장소의 기본 정보를 Firestore에서 가져오기
            val placeDetailsList = listItem.places.mapNotNull { place ->
                try {
                    val placeDoc = db.collection("places")
                        .document(place.placeId)
                        .get()
                        .await()
                    
                    if (placeDoc.exists()) {
                        val data = placeDoc.data
                        PlaceDetails(
                            placeId = data?.get("placeId") as? String ?: place.placeId,
                            name = data?.get("name") as? String ?: place.name,
                            latitude = (data?.get("latitude") as? Double) ?: 0.0,
                            longitude = (data?.get("longitude") as? Double) ?: 0.0,
                            address = data?.get("address") as? String,
                            country = data?.get("country") as? String,
                            phoneNumber = data?.get("phoneNumber") as? String,
                            websiteUri = data?.get("websiteUri") as? String,
                            openingHours = (data?.get("openingHours") as? List<*>)?.mapNotNull { it as? String },
                            photoUrl = data?.get("photoUrl") as? String,
                            businessStatus = null, // API에서 가져올 예정
                            isOpenNow = null, // API에서 가져올 예정
                            currentOpeningHours = null // API에서 가져올 예정
                        )
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
            
            // 3. 각 장소의 현재 운영 상태를 API로 조회 (병렬 처리)
            val businessStatusMap = placeViewModel.fetchPlaceBusinessStatusBatch(
                placeDetailsList.map { it.placeId }
            )
            
            // 4. 기본 정보와 운영 상태를 합치기
            placeDetailsList.map { placeDetails ->
                val businessStatus = businessStatusMap[placeDetails.placeId]
                placeDetails.copy(
                    businessStatus = businessStatus?.businessStatus,
                    isOpenNow = businessStatus?.isOpenNow,
                    currentOpeningHours = businessStatus?.currentOpeningHours
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // 상품이 리스트에 이미 존재하는지 확인 (체크박스 자동 선택에 사용)
    suspend fun productExistsInListSuspend(productId: String, listId: String): Boolean {
        return try {
            listProductDao.productExistsInList(productId, listId)
        } catch (e: Exception) {
            false
        }
    }
    
    // 선택된 리스트들에 상품 추가
    suspend fun addProductToSelectedLists(productDetails: com.example.tripcart.ui.screen.ProductDetails): Result<Unit> {
        return try {
            val selectedLists = _uiState.value.lists.filter { it.isSelected }
            
            if (selectedLists.isEmpty()) {
                return Result.failure(Exception("리스트를 선택해주세요"))
            }
            
            // 각 선택된 리스트에 상품 추가
            selectedLists.forEach { listItem ->
                if (listItem.isFromFirestore) {
                    // Firestore 리스트인 경우: list_products 하위 컬렉션에 추가
                    try {
                        // 중복 체크: list_products 컬렉션에서 이미 존재하는지 확인
                        val existingProduct = db.collection("lists")
                            .document(listItem.listId)
                            .collection("list_products")
                            .document(productDetails.id)
                            .get()
                            .await()
                        
                        if (!existingProduct.exists()) {
                            // 상품 데이터 생성
                            val productData = hashMapOf<String, Any>(
                                "id" to productDetails.id,
                                "productName" to productDetails.productName,
                                "category" to productDetails.category,
                                "quantity" to productDetails.quantity,
                                "bought" to "구매전"
                            )
                            
                            if (productDetails.productId != null) {
                                productData["productId"] = productDetails.productId
                            }
                            if (productDetails.imageUrls != null && productDetails.imageUrls.isNotEmpty()) {
                                productData["imageUrls"] = productDetails.imageUrls
                            }
                            if (productDetails.note != null && productDetails.note.isNotBlank()) {
                                productData["note"] = productDetails.note
                            }
                            
                            // list_products 하위 컬렉션에 상품 추가
                            db.collection("lists")
                                .document(listItem.listId)
                                .collection("list_products")
                                .document(productDetails.id)
                                .set(productData)
                                .await()
                            
                            // 리스트의 productCount 증가
                            val listDoc = db.collection("lists").document(listItem.listId).get().await()
                            if (listDoc.exists()) {
                                val currentProductCount = (listDoc.getLong("productCount") ?: 0).toInt()
                                db.collection("lists")
                                    .document(listItem.listId)
                                    .update("productCount", currentProductCount + 1)
                                    .await()
                            }
                        }
                    } catch (e: Exception) {
                        throw Exception("${listItem.name} 리스트에 상품 추가 실패: ${e.message}")
                    }
                } else {
                    // Room DB 리스트인 경우: list_products 테이블에 추가
                    // 상품이 이미 리스트에 있는지 확인 (중복 체크)
                    val alreadyExists = listProductDao.productExistsInList(productDetails.id, listItem.listId)
                    
                    if (!alreadyExists) {
                        // Room DB에 상품 추가 (IGNORE 전략으로 중복 시 자동 무시)
                        val productEntity = ListProductEntity(
                            id = productDetails.id,
                            listId = listItem.listId,
                            productId = productDetails.productId,  // null이면 사용자 생성 상품, 있으면 Firestore 공개 상품
                            productName = productDetails.productName,
                            category = productDetails.category,
                            imageUrls = productDetails.imageUrls,
                            quantity = productDetails.quantity,
                            note = productDetails.note,
                            bought = "구매전"
                        )
                        
                        listProductDao.insertProduct(productEntity)
                        
                        // 리스트의 productCount 증가
                        val list = listDao.getListById(listItem.listId)
                        if (list != null) {
                            val updatedList = list.copy(productCount = list.productCount + 1)
                            listDao.updateList(updatedList)
                        }
                    }
                }
            }
            
            // 리스트 목록 새로고침
            loadLists()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 리스트에서 상품 삭제
    suspend fun deleteProductFromList(productId: String, listId: String): Result<Unit> {
        return try {
            // 상품이 리스트에 존재하는지 확인
            val exists = listProductDao.productExistsInList(productId, listId)
            if (!exists) {
                return Result.failure(Exception("상품이 리스트에 존재하지 않습니다."))
            }
            
            // 상품 삭제
            listProductDao.deleteProductById(productId, listId)
            
            // 리스트의 productCount 감소
            val list = listDao.getListById(listId)
            if (list != null) {
                val updatedList = list.copy(productCount = (list.productCount - 1).coerceAtLeast(0))
                listDao.updateList(updatedList)
            }
            
            // 리스트 목록 새로고침
            loadLists()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 새 리스트 생성 (상품용)
    suspend fun createNewListForProduct(
        productDetails: com.example.tripcart.ui.screen.ProductDetails,
        listName: String = "새 리스트"
    ): Result<String> {
        return try {
            val listId = UUID.randomUUID().toString()
            
            // 로컬 DB에 리스트 생성
            val listEntity = ListEntity(
                listId = listId,
                name = listName,
                status = "준비중",
                country = null, // 상품은 국가 제약 없음
                places = emptyList(),
                productCount = 1, // 상품 1개 추가 예정
                firestoreSynced = false
            )
            listDao.insertList(listEntity)
            
            // 상품을 리스트에 추가
            val productEntity = ListProductEntity(
                id = productDetails.id,
                listId = listId,
                productId = productDetails.productId,  // null이면 사용자 생성 상품, 있으면 Firestore 공개 상품
                productName = productDetails.productName,
                category = productDetails.category,
                imageUrls = productDetails.imageUrls,
                quantity = productDetails.quantity,
                note = productDetails.note,
                bought = "구매전"
            )
            listProductDao.insertProduct(productEntity)
            
            // 리스트 목록 새로고침 (비동기로 처리)
            viewModelScope.launch {
                loadLists()
                
                // 새로 생성한 리스트를 자동으로 선택 상태로 만들기
                kotlinx.coroutines.delay(300)
                selectList(listId)
            }
            
            Result.success(listId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 리스트 상세 정보 가져오기 (리스트 정보 + 상품 목록)
    suspend fun getListDetail(listId: String): ListEntity? {
        return try {
            // 먼저 Room DB에서 확인
            val roomList = listDao.getListById(listId)
            if (roomList != null) {
                return roomList
            }
            
            // Room DB에 없으면 Firestore에서 가져오기
            val firestoreDoc = db.collection("lists").document(listId).get().await()
            if (firestoreDoc.exists()) {
                parseListDocument(firestoreDoc)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    // 특정 리스트의 상품 목록 가져오기 (Flow) - Room DB
    fun getProductsByListId(listId: String): Flow<List<ListProductEntity>> {
        return listProductDao.getProductsByListId(listId)
    }
    
    // 특정 리스트의 상품 목록 가져오기 (Flow) - Firestore
    fun getFirestoreProductsByListId(listId: String): Flow<List<ListProductEntity>> {
        return callbackFlow {
            val firestoreListener = db.collection("lists")
                .document(listId)
                .collection("list_products")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("ListViewModel", "Error listening to Firestore products", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val products = snapshot?.documents?.mapNotNull { doc ->
                        val data = doc.data
                        ListProductEntity(
                            id = data?.get("id") as? String ?: doc.id,
                            listId = listId,
                            productId = data?.get("productId") as? String,
                            productName = data?.get("productName") as? String ?: "",
                            category = data?.get("category") as? String ?: "",
                            imageUrls = (data?.get("imageUrls") as? List<*>)?.mapNotNull { it as? String },
                            quantity = (data?.get("quantity") as? Number)?.toInt() ?: 0,
                            note = data?.get("note") as? String,
                            bought = data?.get("bought") as? String ?: "구매전"
                        )
                    } ?: emptyList()
                    
                    trySend(products)
                }
            
            awaitClose {
                firestoreListener.remove()
            }
        }
    }
    
    // 상품 상태(bought) 업데이트
    suspend fun updateProductBoughtStatus(productId: String, listId: String, bought: String): Result<Unit> {
        return try {
            // 먼저 Room DB에서 확인
            val roomProduct = listProductDao.getProductById(productId, listId)
            if (roomProduct != null) {
                val updatedProduct = roomProduct.copy(bought = bought)
                listProductDao.updateProduct(updatedProduct)
                Result.success(Unit)
            } else {
                // Room DB에 없으면 Firestore에서 업데이트
                val firestoreDoc = db.collection("lists")
                    .document(listId)
                    .collection("list_products")
                    .document(productId)
                    .get()
                    .await()
                
                if (firestoreDoc.exists()) {
                    db.collection("lists")
                        .document(listId)
                        .collection("list_products")
                        .document(productId)
                        .update("bought", bought)
                        .await()
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("상품을 찾을 수 없습니다."))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 리스트 상태(status) 업데이트
    suspend fun updateListStatus(listId: String, status: String): Result<Unit> {
        return try {
            // 먼저 Room DB에서 확인
            val list = listDao.getListById(listId)
            if (list != null) {
                // Room DB 리스트인 경우
                val updatedList = list.copy(status = status)
                listDao.updateList(updatedList)
                
                // 리스트 목록 새로고침
                loadLists()
                
                Result.success(Unit)
            } else {
                // Room DB에 없으면 Firestore 리스트!
                try {
                    val listDoc = db.collection("lists").document(listId).get().await()
                    if (listDoc.exists()) {
                        db.collection("lists")
                            .document(listId)
                            .update("status", status)
                            .await()
                        
                        // 리스트 목록 새로고침
                        loadLists()
                        
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("리스트를 찾을 수 없습니다."))
                    }
                } catch (e: Exception) {
                    Result.failure(Exception("리스트 상태 업데이트 실패: ${e.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 초대코드 발급 (리스트를 Firestore로 이동)
    suspend fun generateInviteCode(listId: String, right: String): Result<String> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                return Result.failure(Exception("로그인이 필요합니다."))
            }
            
            // Room DB에서 리스트 가져오기
            val list = listDao.getListById(listId)
            if (list == null) {
                return Result.failure(Exception("리스트를 찾을 수 없습니다."))
            }
            
            // 이미 Firestore에 있는지 확인
            try {
                val existingDoc = db.collection("lists").document(listId).get().await()
                if (existingDoc.exists() && existingDoc.getString("inviteCode") != null) {
                    // 이미 초대코드가 발급된 경우 기존 초대코드 반환
                    return Result.success(existingDoc.getString("inviteCode")!!)
                }
            } catch (e: Exception) {
                // 오류 발생
            }
            
            // 초대코드 생성 (6자리 영숫자)
            val inviteCode = generateRandomInviteCode()
            
            // Room DB에서 상품 목록 가져오기
            val products = listProductDao.getProductsByListId(listId).first()
            
            // Firestore에 리스트 저장
            val placesList = list.places.map { place ->
                hashMapOf("name" to place.name, "placeId" to place.placeId)
            }
            
            val listData = hashMapOf<String, Any>(
                "name" to list.name,
                "status" to list.status,
                "places" to placesList,
                "ownerId" to userId,
                "sharedWith" to emptyList<String>(),
                "inviteCode" to inviteCode,
                "right" to right
            )
            
            if (list.country != null) {
                listData["country"] = list.country
            }
            
            // Firestore에 리스트 문서 생성
            try {
                db.collection("lists").document(listId).set(listData).await()
            } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    return Result.failure(Exception("Firestore 권한이 없습니다. Firebase Console에서 보안 규칙을 설정해주세요."))
                }
                throw e
            }
            
            // 상품들을 Firestore 하위 컬렉션으로 이동
            products.forEach { product ->
                val productData = hashMapOf<String, Any>(
                    "id" to product.id,
                    "productName" to product.productName,
                    "category" to product.category,
                    "quantity" to product.quantity,
                    "bought" to product.bought
                )
                
                if (product.productId != null) {
                    productData["productId"] = product.productId
                }
                if (product.imageUrls != null && product.imageUrls.isNotEmpty()) {
                    productData["imageUrls"] = product.imageUrls
                }
                if (product.note != null) {
                    productData["note"] = product.note
                }
                
                db.collection("lists")
                    .document(listId)
                    .collection("list_products") // products와 구분하기 위해 list_products 사용
                    .document(product.id)
                    .set(productData)
                    .await()
            }
            
            // Room DB에서 리스트 삭제
            listDao.deleteListById(listId)
            
            // 리스트 목록 새로고침
            loadLists()
            
            Result.success(inviteCode)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 초대코드로 리스트 참여
    suspend fun joinListByInviteCode(inviteCode: String): Result<String> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                return Result.failure(Exception("로그인이 필요합니다."))
            }
            
            // 초대코드로 리스트 찾기
            val querySnapshot = db.collection("lists")
                .whereEqualTo("inviteCode", inviteCode)
                .get()
                .await()
            
            if (querySnapshot.isEmpty) {
                return Result.failure(Exception("유효하지 않은 초대코드입니다."))
            }
            
            val listDoc = querySnapshot.documents.first()
            val listId = listDoc.id
            val ownerId = listDoc.getString("ownerId")
            
            // owner가 본인 리스트에 참여하려고 하는 경우
            if (ownerId == userId) {
                return Result.failure(Exception("자신이 만든 리스트에는 참여할 수 없습니다."))
            }
            
            // 이미 참여 중인지 확인
            val sharedWith = listDoc.get("sharedWith") as? List<*> ?: emptyList<Any?>()
            val isAlreadyJoined = sharedWith.contains(userId)
            
            if (!isAlreadyJoined) {
                // sharedWith에 사용자 추가
                val updatedSharedWith = sharedWith.toMutableList().apply {
                    add(userId)
                }
                
                db.collection("lists")
                    .document(listId)
                    .update("sharedWith", updatedSharedWith)
                    .await()
                
                // 리스트 목록 새로고침
                loadLists()
            }
            
            // 이미 참여 중이면 실패로 처리
            if (isAlreadyJoined) {
                return Result.failure(Exception("이미 참여 중인 리스트입니다."))
            }
            
            Result.success(listId)
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return Result.failure(Exception("초대코드로 리스트에 참여할 권한이 없습니다. Firebase 보안 규칙을 확인해주세요."))
            }
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 초대코드 가져오기 (이미 발급된 경우)
    suspend fun getInviteCode(listId: String): String? {
        return try {
            val listDoc = db.collection("lists").document(listId).get().await()
            if (listDoc.exists()) {
                listDoc.getString("inviteCode")
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("ListViewModel", "Error getting invite code", e)
            null
        }
    }
    
    // 초대코드가 발급되었는지 확인
    suspend fun hasInviteCode(listId: String): Boolean {
        return try {
            // 먼저 Room DB 확인 (Room DB에 있으면 초대코드 없음)
            val roomList = listDao.getListById(listId)
            if (roomList != null) {
                return false // Room DB 리스트는 초대코드 없음
            }
            
            // Room DB에 없으면 Firestore 확인
            val listDoc = db.collection("lists").document(listId).get().await()
            if (listDoc.exists()) {
                listDoc.getString("inviteCode") != null
            } else {
                false
            }
        } catch (e: Exception) {
            // Firestore 조회 실패 시 (권한 오류 등)
            false
        }
    }
    
    // 랜덤 초대코드 생성 (6자리 영숫자)
    private fun generateRandomInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { chars.random() }
            .joinToString("")
    }
}

