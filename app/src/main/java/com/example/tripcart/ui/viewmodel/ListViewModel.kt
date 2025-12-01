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
    ).build()
    
    private val listDao = roomDb.listDao()
    
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
                
                // Firestore 리스트 추가 (Room DB에 없는 경우만, Room DB에 저장하지 않음)
                // Firestore에는 채팅, 권한 등 추가 필드가 있어서 Room DB에 저장하지 않음
                firestoreLists.forEach { list ->
                    if (!mergedLists.containsKey(list.listId)) {
                        mergedLists[list.listId] = Pair(list, true) // isFromFirestore = true
                        // Room DB에는 저장하지 않음 (Firestore 구조가 다르므로)
                    }
                }
                
                mergedLists.values.toList()
            }.collect { mergedLists ->
                // 각 리스트에 대한 정보를 변환 (추후 Firestore 연동 확장 시 필요)
                val listItems = mergedLists.map { (list, isFromFirestore) ->
                    async {
                        // 추후 Firestore 추가 조회 시 여기서 처리
                        val productCount = list.productCount
                        
                        ListItemUiState(
                            listId = list.listId,
                            name = list.name,
                            status = list.status,
                            country = list.country,
                            places = list.places,  // Place 객체 리스트 (name, placeId 연결)
                            productCount = productCount,
                            isFromFirestore = isFromFirestore
                        )
                    }
                }
                
                // 모든 작업 완료 대기 (추후 비동기 확장 대비)
                val completedListItems = listItems.awaitAll()
                
                _uiState.value = _uiState.value.copy(
                    lists = completedListItems,
                    isLoading = false
                )
            }
        }
    }
    
    // 실시간 리스너를 이용해 Firestore에서 리스트를 실시간 Flow로 가져오기
    private fun getFirestoreListsFlow(): Flow<List<ListEntity>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listenerRegistration = db.collection("lists")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("ListViewModel", "Error listening to Firestore lists", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val firestoreLists = snapshot.documents.map { doc ->
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
                        
                        ListEntity(
                            listId = listId,
                            name = name,
                            status = status,
                            country = country,
                            places = places,  // List<Place> 객체 리스트
                            productCount = productCount,
                            firestoreSynced = true
                        )
                    }
                    trySend(firestoreLists)
                }
            }
        
        // Flow가 취소될 때 리스너 해제
        awaitClose {
            listenerRegistration.remove()
        }
    }
    
    // Firestore에서 리스트 동기화
    private suspend fun syncListsFromFirestore() {
        val userId = auth.currentUser?.uid ?: return
        
        try {
            val listsSnapshot = db.collection("lists")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            listsSnapshot.documents.forEach { doc ->
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
                
                // 로컬 DB에 리스트 저장 (places 필드에 Place 객체 리스트 저장)
                // 상품은 Firestore 서브컬렉션에서만 관리, Room DB 저장 제거
                val listEntity = ListEntity(
                    listId = listId,
                    name = name,
                    status = status,
                    country = country,
                    places = places,  // List<Place> 객체 리스트
                    productCount = productCount,  // Firestore에서 가져온 상품 개수
                    firestoreSynced = true
                )
                listDao.insertList(listEntity)
                
                // 상품은 Firestore의 lists/{listId}/products 서브컬렉션에서만 관리
            }
        } catch (e: Exception) {
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
            
            // Firestore에 장소 저장 (중복 체크)
            val placeDoc = try {
                db.collection("places").document(placeDetails.placeId).get().await()
            } catch (e: Exception) {
                null
            }
            
            if (placeDoc?.exists() != true) {
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
            }
            
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
                            listDao.insertList(list.copy(places = updatedPlaces))
                            
                            // Firestore에도 추가 (Room DB 리스트도 Firestore에 저장되어 있을 수 있음)
                            try {
                                val listDoc = db.collection("lists").document(list.listId).get().await()
                                if (listDoc.exists()) {
                                    val currentPlaces = (listDoc.get("places") as? List<*>) ?: emptyList<Any?>()
                                    val currentPlaceIds = currentPlaces.mapNotNull {
                                        when (it) {
                                            is Map<*, *> -> it["placeId"] as? String
                                            is String -> it  // 기존 형식 호환
                                            else -> null
                                        }
                                    }
                                    
                                    if (placeDetails.placeId !in currentPlaceIds) {
                                        val newPlace = hashMapOf("name" to placeDetails.name, "placeId" to placeDetails.placeId)
                                        val updatedPlacesList = currentPlaces + newPlace
                                        
                                        val updateData = hashMapOf<String, Any>(
                                            "places" to updatedPlacesList
                                        )
                                        if (list.country == null && placeDetails.country != null) {
                                            updateData["country"] = placeDetails.country
                                            // 로컬 DB도 업데이트
                                            listDao.insertList(list.copy(country = placeDetails.country, places = updatedPlaces))
                                        } else {
                                            listDao.insertList(list.copy(places = updatedPlaces))
                                        }
                                        
                                        db.collection("lists")
                                            .document(list.listId)
                                            .update(updateData)
                                            .await()
                                    }
                                }
                            } catch (e: Exception) {
                                // Firestore 업데이트 실패해도 Room DB에는 저장됨
                                android.util.Log.w("ListViewModel", "Failed to update Firestore for Room DB list", e)
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
            val userId = auth.currentUser?.uid
                ?: return Result.failure(Exception("로그인이 필요합니다."))
            
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
            
            // Firestore에 리스트 생성 (실패해도 Room DB에는 저장되었으므로 계속 진행)
            try {
                val listData = hashMapOf(
                    "name" to listName,
                    "status" to "준비중",
                    "country" to (placeDetails.country ?: ""),
                    "places" to listOf(hashMapOf("name" to placeDetails.name, "placeId" to placeDetails.placeId)),
                    "productCount" to 0,  // 새 리스트는 상품 개수 0
                    "userId" to userId
                )
                
                db.collection("lists")
                    .document(listId)
                    .set(listData)
                    .await()
                
                // Firestore에 장소 저장 (중복 체크)
                val placeDoc = try {
                    db.collection("places").document(placeDetails.placeId).get().await()
                } catch (e: Exception) {
                    null
                }
                
                if (placeDoc?.exists() != true) {
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
                }
                
                // 로컬 DB 동기화 상태 업데이트
                listDao.insertList(listEntity.copy(firestoreSynced = true))
            } catch (e: Exception) {
                // Firestore 저장 실패해도 Room DB에는 저장되었으므로 계속 진행
                // 로컬 DB 동기화 상태는 false로 유지
            }
            
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
                if (isFromFirestore) {
                    // Firestore 리스트 삭제
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        db.collection("users")
                            .document(userId)
                            .collection("lists")
                            .document(listId)
                            .delete()
                            .await()
                    }
                } else {
                    // Room DB 리스트 삭제
                    // 리스트 삭제 (listId로 직접 삭제, places는 ListEntity에 포함되어 있어서 자동 삭제됨)
                    listDao.deleteListById(listId)
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
}

