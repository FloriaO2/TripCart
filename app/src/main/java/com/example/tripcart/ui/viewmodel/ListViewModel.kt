package com.example.tripcart.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripcart.data.local.TripCartDatabase
import com.example.tripcart.data.local.dao.*
import com.example.tripcart.data.local.entity.*
import com.example.tripcart.ui.viewmodel.PlaceDetails
import com.example.tripcart.util.GeofenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
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
    val isFromFirestore: Boolean = false, // Firestore에서 온 리스트인지 구분
    val userRole: String? = null // 현재 사용자의 권한 ("owner", "edit", "read", null=개인 리스트)
)

data class ListUiState(
    val lists: List<ListItemUiState> = emptyList(),
    val isLoading: Boolean = true, // 초기 로딩 상태를 true로 설정하여 앱 시작 시 로딩 표시
    val errorMessage: String? = null,
    val successMessage: String? = null // 성공 메시지 (상품/장소 추가 완료 등)
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
    private val placeDao = roomDb.placeDao()
    
    init {
        loadLists()
        
        // 앱 최초 실행 시에만 모든 리스트의 장소들을 places 테이블로 동기화
        viewModelScope.launch {
            val prefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            val isFirstSyncDone = prefs.getBoolean("is_first_sync_done", false)
            
            if (!isFirstSyncDone) {
                syncPlacesFromLists()
                // 동기화 완료 후 플래그 설정
                prefs.edit().putBoolean("is_first_sync_done", true).apply()
            }
        }
    }
    
    // 로컬 DB와 Firestore에서 리스트 로드
    fun loadLists() {
        viewModelScope.launch {
            // 현재 선택된 리스트 ID들을 저장 (선택 상태 보존)
            val selectedListIds = _uiState.value.lists
                .filter { it.isSelected }
                .map { it.listId }
                .toSet()
            
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
                mergedLists.values.toList().sortedBy { it.second } // false(Room DB) 먼저, true(Firestore) 나중
            }.collect { mergedLists ->
                // 각 리스트에 대한 정보를 변환
                val listItems = mergedLists.map { (list, isFromFirestore) ->
                    async {
                        var productCount = list.productCount
                        var places = list.places
                        var userRole: String? = null // 권한 정보 (Firestore 리스트인 경우 아래에서 불러옴)
                        
                        // Firestore 리스트인 경우 productCount 필드를 사용하고 권한 정보도 가져오기
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
                                    
                                    // 권한 정보 가져오기
                                    val userId = auth.currentUser?.uid
                                    if (userId != null) {
                                        val ownerId = listDoc.getString("ownerId")
                                        val right = listDoc.getString("right") ?: "read"
                                        
                                        userRole = when {
                                            ownerId == userId -> "owner"
                                            else -> {
                                                val sharedWith = (listDoc.get("sharedWith") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                                                if (sharedWith.contains(userId)) {
                                                    right
                                                } else {
                                                    null // 권한 없음
                                                }
                                            }
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
                            isFromFirestore = isFromFirestore,
                            userRole = userRole,
                            isSelected = selectedListIds.contains(list.listId) // 선택 상태 복원
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
                
                // documentChanges - Firestore에서 제공하는 WebSocket 기반 서버 푸시 방식
                // 리스트 추가, 수정, 삭제 등을 모두 실시간 반영할 수 있도록 지원!
                snapshot?.documentChanges?.forEach { change ->
                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            val listEntity = parseListDocument(change.document)
                            // 같은 listId면 덮어쓰기
                            mergedLists[listEntity.listId] = listEntity
                        }
                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                            // 삭제된 문서는 mergedLists에서 제거 (Firestore에선 이미 제거된 상태)
                            mergedLists.remove(change.document.id)
                        }
                    }
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
                
                // documentChanges - Firestore에서 제공하는 WebSocket 기반 서버 푸시 방식
                // 리스트 추가, 수정, 삭제 등을 모두 실시간 반영할 수 있도록 지원!
                snapshot?.documentChanges?.forEach { change ->
                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            val listEntity = parseListDocument(change.document)
                            mergedLists[listEntity.listId] = listEntity
                        }
                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                            // 삭제된 문서는 mergedLists에서 제거 (Firestore에선 이미 제거된 상태)
                            mergedLists.remove(change.document.id)
                        }
                    }
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
    
    // 모든 리스트의 장소들을 RoomDB의 places 테이블로 동기화
    suspend fun syncPlacesFromLists() {
        try {
            // Room DB의 모든 리스트 가져오기
            val roomLists = listDao.getAllLists().first()
            
            // Firestore의 모든 리스트 가져오기
            val userId = auth.currentUser?.uid ?: return
            val ownerListsSnapshot = db.collection("lists")
                .whereEqualTo("ownerId", userId)
                .get()
                .await()
            
            val sharedListsSnapshot = db.collection("lists")
                .whereArrayContains("sharedWith", userId)
                .get()
                .await()
            
            // Room DB 내 리스트의 장소들을 places 테이블에 저장/업데이트
            roomLists.forEach { list ->
                list.places.forEach { place ->
                    // Firestore의 places 컬렉션에서 장소 정보 가져오기
                    try {
                        val placeDoc = db.collection("places")
                            .document(place.placeId)
                            .get()
                            .await()
                        
                        if (placeDoc.exists()) {
                            val placeData = placeDoc.data ?: return@forEach
                            val lat = (placeData["latitude"] as? Double) ?: (placeData["lat"] as? Double) ?: 0.0
                            val lng = (placeData["longitude"] as? Double) ?: (placeData["lng"] as? Double) ?: 0.0
                            val placeName = placeData["name"] as? String ?: place.name
                            
                            // places 테이블에 저장/업데이트
                            val existingPlace = placeDao.getPlaceById(place.placeId)
                            if (existingPlace != null) {
                                // 기존 장소가 있지만 listId은 없다면 list 배열에 listId 추가
                                val updatedListIds = if (list.listId !in existingPlace.listId) {
                                    existingPlace.listId + list.listId
                                } else {
                                    // 기존 장소에 이미 listId도 있다면 변경 없이 그대로 유지
                                    existingPlace.listId
                                }
                                placeDao.updatePlace(existingPlace.copy(listId = updatedListIds))
                            } else {
                                // 기존 장소가 없으면 장소부터 새로 생성
                                val newPlace = PlaceEntity(
                                    placeId = place.placeId,
                                    lat = lat,
                                    lng = lng,
                                    name = placeName,
                                    listId = listOf(list.listId)
                                )
                                placeDao.insertPlace(newPlace)
                            }
                        }
                    } catch (e: Exception) {
                        // 장소 정보 가져오기 실패
                    }
                }
            }
            
            // Firestore 내 리스트의 장소들을 places 테이블에 저장/업데이트
            val allFirestoreDocs = mutableListOf<DocumentSnapshot>() // 빈 리스트 생성
            ownerListsSnapshot.documents.forEach { allFirestoreDocs.add(it) }
            sharedListsSnapshot.documents.forEach { doc ->
                if (!allFirestoreDocs.any { it.id == doc.id }) { // owner 리스트와 중복되지 않도록 체크
                    allFirestoreDocs.add(doc)
                }
            }
            
            allFirestoreDocs.forEach { doc ->
                val placesList = (doc.get("places") as? List<*>) ?: emptyList<Any?>()
                placesList.forEach { placeMap ->
                    when (placeMap) {
                        is Map<*, *> -> {
                            val placeId = placeMap["placeId"] as? String
                            val placeName = placeMap["name"] as? String
                            
                            if (placeId != null && placeName != null) {
                                try {
                                    // Firestore의 places 컬렉션에서 장소 정보 가져오기
                                    val placeDoc = db.collection("places")
                                        .document(placeId)
                                        .get()
                                        .await()
                                    
                                    if (placeDoc.exists()) {
                                        val placeData = placeDoc.data ?: return@forEach
                                        val lat = (placeData["latitude"] as? Double) ?: (placeData["lat"] as? Double) ?: 0.0
                                        val lng = (placeData["longitude"] as? Double) ?: (placeData["lng"] as? Double) ?: 0.0
                                        val placeNameFromFirestore = placeData["name"] as? String ?: placeName
                                        
                                        // places 테이블에 저장/업데이트
                                        val existingPlace = placeDao.getPlaceById(placeId)
                                        if (existingPlace != null) {
                                            // 기존 장소가 있지만 listId은 없다면 list 배열에 listId 추가
                                            val updatedListIds = if (doc.id !in existingPlace.listId) {
                                                existingPlace.listId + doc.id
                                            } else {
                                                // 기존 장소에 이미 listId도 있다면 변경 없이 그대로 유지
                                                existingPlace.listId
                                            }
                                            placeDao.updatePlace(existingPlace.copy(listId = updatedListIds))
                                        } else {
                                            // 기존 장소가 없으면 장소부터 새로 생성
                                            val newPlace = PlaceEntity(
                                                placeId = placeId,
                                                lat = lat,
                                                lng = lng,
                                                name = placeNameFromFirestore,
                                                listId = listOf(doc.id)
                                            )
                                            placeDao.insertPlace(newPlace)
                                        }
                                    }
                                } catch (e: Exception) {
                                    // 장소 정보 가져오기 실패
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // 동기화 실패 시 무시
        }
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
    
    // 모든 리스트 선택 해제
    // - 상품 추가와 상점 추가를 왔다갔다할 때 리스트 선택 상태가 공유되는 것을 방지
    fun clearAllSelections() {
        _uiState.value = _uiState.value.copy(
            lists = _uiState.value.lists.map { list ->
                list.copy(isSelected = false)
            }
        )
    }
    
    // 선택된 리스트들에 장소 추가
    suspend fun addPlaceToSelectedLists(placeDetails: PlaceDetails): Result<Unit> {
        return try {
            val selectedLists = _uiState.value.lists.filter { it.isSelected }
            
            // Firestore의 places 컬렉션에서 장소 정보 가져오기 (lat, lng, name)
            val placeDoc = db.collection("places")
                .document(placeDetails.placeId)
                .get()
                .await()
            
            val lat: Double
            val lng: Double
            val placeName: String
            
            if (!placeDoc.exists()) {
                // 장소가 Firestore에 없으면 placeDetails에서 직접 사용하고 저장
                lat = placeDetails.latitude
                lng = placeDetails.longitude
                placeName = placeDetails.name
                
                // Firestore에 장소 정보 저장
                val placeData = hashMapOf<String, Any>(
                    "placeId" to placeDetails.placeId,
                    "latitude" to lat,
                    "longitude" to lng,
                    "name" to placeName,
                    "address" to (placeDetails.address ?: ""),
                    "country" to (placeDetails.country ?: ""),
                    "photoUrl" to (placeDetails.photoUrl ?: ""),
                    "phoneNumber" to (placeDetails.phoneNumber ?: ""),
                    "websiteUri" to (placeDetails.websiteUri ?: ""),
                    "openingHours" to (placeDetails.openingHours ?: emptyList())
                )
                db.collection("places")
                    .document(placeDetails.placeId)
                    .set(placeData)
                    .await()
            } else {
                val placeData = placeDoc.data ?: return Result.failure(Exception("장소 정보가 없습니다."))
                lat = (placeData["latitude"] as? Double) ?: (placeData["lat"] as? Double) ?: placeDetails.latitude
                lng = (placeData["longitude"] as? Double) ?: (placeData["lng"] as? Double) ?: placeDetails.longitude
                placeName = placeData["name"] as? String ?: placeDetails.name
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
                        
                        // 중복이면 예외 던지기
                        if (placeDetails.placeId in currentPlaceIds) {
                            throw Exception("이미 추가된 장소입니다.")
                        }
                        
                        // 중복이 아니므로 추가 진행
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
                        
                        // RoomDB의 places 테이블에 저장/업데이트
                        val existingPlace = placeDao.getPlaceById(placeDetails.placeId)
                        if (existingPlace != null) {
                            // 기존 장소가 있으면 listId 배열에 추가
                            val updatedListIds = if (listItem.listId !in existingPlace.listId) {
                                existingPlace.listId + listItem.listId
                            } else {
                                existingPlace.listId
                            }
                            placeDao.updatePlace(existingPlace.copy(listId = updatedListIds))
                        } else {
                            // 기존 장소가 없으면 새로 생성
                            val newPlaceEntity = PlaceEntity(
                                placeId = placeDetails.placeId,
                                lat = lat,
                                lng = lng,
                                name = placeName,
                                listId = listOf(listItem.listId)
                            )
                            placeDao.insertPlace(newPlaceEntity)
                            // Geofence 등록
                            GeofenceManager.addGeofence(getApplication(), newPlaceEntity)
                        }
                        
                        // Firestore 리스트에 장소 추가 시 랭킹 데이터 업데이트
                        // 리스트에 이미 상품이 있는 경우, 해당 상품들의 랭킹 데이터 업데이트
                        val finalCountry = currentCountry ?: placeDetails.country
                        if (finalCountry != null) {
                            // 리스트의 상품 목록 가져오기
                            val productsSnapshot = db.collection("lists")
                                .document(listItem.listId)
                                .collection("list_products")
                                .get()
                                .await()
                            
                            productsSnapshot.documents.forEach { productDoc ->
                                val productId = productDoc.getString("productId")
                                if (productId != null && productId.isNotEmpty()) {
                                    // 국가별 카운트는 리스트당 한 번만 추가 (첫 장소인 경우만)
                                    val shouldUpdateCountry = currentCountry == null && placeDetails.country != null
                                    if (shouldUpdateCountry) {
                                        // 국가 totalCount, 장소 totalCount, 장소별 카운트 등 모두 업데이트
                                        updateProductStats(finalCountry, listOf(placeDetails.placeId), productId, increment = true)
                                    } else {
                                        // 장소별 카운트만 업데이트
                                        updateProductStatsForPlace(placeDetails.placeId, productId, increment = true)
                                    }
                                }
                            }
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
                            
                            // RoomDB의 places 테이블에 저장/업데이트
                            val existingPlace = placeDao.getPlaceById(placeDetails.placeId)
                            if (existingPlace != null) {
                                // 기존 장소가 있으면 listId 배열에 추가
                                val updatedListIds = if (listItem.listId !in existingPlace.listId) {
                                    existingPlace.listId + listItem.listId
                                } else {
                                    existingPlace.listId
                                }
                                placeDao.updatePlace(existingPlace.copy(listId = updatedListIds))
                            } else {
                                // 기존 장소가 없으면 새로 생성
                                val newPlace = PlaceEntity(
                                    placeId = placeDetails.placeId,
                                    lat = lat,
                                    lng = lng,
                                    name = placeName,
                                    listId = listOf(listItem.listId)
                                )
                                placeDao.insertPlace(newPlace)
                                // Geofence 등록
                                GeofenceManager.addGeofence(getApplication(), newPlace)
                            }
                            
                            // Room DB 리스트에 장소 추가 시 랭킹 데이터 업데이트
                            // 리스트에 이미 상품이 있는 경우, 해당 상품들의 랭킹 데이터 업데이트
                            val finalCountry = list.country ?: placeDetails.country
                            if (finalCountry != null) {
                                // 리스트의 상품 목록 가져오기
                                val products = listProductDao.getProductsByListId(listItem.listId).first()
                                
                                products.forEach { product ->
                                    val productId = product.productId
                                    if (productId != null && productId.isNotEmpty()) {
                                        // 국가별 카운트는 리스트당 한 번만 추가 (첫 장소인 경우만)
                                        val shouldUpdateCountry = list.country == null && placeDetails.country != null
                                        if (shouldUpdateCountry) {
                                            // 국가 totalCount, 장소 totalCount, 장소별 카운트 등 모두 업데이트
                                            updateProductStats(finalCountry, listOf(placeDetails.placeId), productId, increment = true)
                                        } else {
                                            // 장소별 카운트만 업데이트
                                            updateProductStatsForPlace(placeDetails.placeId, productId, increment = true)
                                        }
                                    }
                                }
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
    suspend fun createNewList(listName: String = "새 리스트"): Result<String> {
        return try {
            val listId = UUID.randomUUID().toString()
            
            // 로컬 DB에 빈 리스트 생성
            val listEntity = ListEntity(
                listId = listId,
                name = listName,
                status = "준비중",
                country = null,
                places = emptyList(),
                productCount = 0,
                firestoreSynced = false
            )
            listDao.insertList(listEntity)
            
            // 리스트 목록 새로고침 (선택 상태는 자동으로 보존)
            loadLists()
            
            Result.success(listId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 하위 컬렉션의 모든 문서를 삭제
    private suspend fun deleteSubcollection(listId: String, subcollectionName: String) {
        val batchSize = 500 // Firestore 배치 작업 최대 크기
        var hasMore = true
        
        while (hasMore) {
            // 배치 크기만큼 하위 컬렉션의 문서들을 가져옴
            val snapshot = db.collection("lists")
                .document(listId)
                .collection(subcollectionName)
                .limit(batchSize.toLong())
                .get()
                .await()
            
            if (snapshot.isEmpty) {
                hasMore = false
                break
            }
            
            // 배치 작업을 통해 일괄적으로 문서 삭제
            val batch = db.batch()
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            // commit - 배치 작업 실행 시작
            // await - 배치 작업 완료될 때까지 대기
            batch.commit().await()
            
            // 데이터 최대한 많이 가져왔더니 배치 크기 꽉 채워서 가져옴
            // = 남은 데이터가 있을 수 있으니 hasMore을 true로 설정한 후 다시 삭제 로직 돌리기
            hasMore = snapshot.size() == batchSize
        }
    }
    
    // 리스트 삭제
    fun deleteList(listId: String, isFromFirestore: Boolean) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "로그인이 필요합니다."
                    )
                    return@launch
                }
                
                // 삭제할 리스트의 장소 목록 가져오기 (places 테이블 정리용)
                val listItem = _uiState.value.lists.find { it.listId == listId }
                val placeIds = listItem?.places?.map { it.placeId } ?: emptyList()
                val country = listItem?.country
                
                if (isFromFirestore) {
                    // Firestore 리스트인 경우, Firestore lists 컬렉션에서 삭제
                    val listDoc = db.collection("lists").document(listId).get().await()
                    if (!listDoc.exists()) {
                        // 리스트 삭제 실패 에러
                        return@launch
                    }
                    
                    val ownerId = listDoc.getString("ownerId")
                    val listCountry = listDoc.getString("country")
                    
                    // 랭킹 데이터 업데이트 (리스트 삭제 전에 수행)
                    if (listCountry != null) {
                        val productsSnapshot = db.collection("lists")
                            .document(listId)
                            .collection("list_products")
                            .get()
                            .await()
                        
                        productsSnapshot.documents.forEach { productDoc ->
                            val productId = productDoc.getString("productId")
                            // Firestore 리스트에서 불러온 상품일 경우
                            if (productId != null && productId.isNotEmpty()) {
                                // 국가별 카운트 감소
                                updateProductStats(listCountry, placeIds, productId, increment = false)
                            }
                        }
                    }
                    
                    if (ownerId == userId) {
                        // 공유 리스트에서 owner가 삭제하는 경우, 하위 컬렉션 포함 리스트 전체 삭제
                        try {
                            // 하위 컬렉션 삭제
                            deleteSubcollection(listId, "list_products")
                            
                            // 리스트 삭제
                            db.collection("lists")
                                .document(listId)
                                .delete()
                                .await()
                        } catch (e: Exception) {
                            // 리스트 삭제 실패 에러
                            return@launch
                        }
                    } else {
                        // 공유 리스트에서 owner 아닌 사람이 나가는 경우, arrayRemove 이용
                        try {
                            val updates = mutableMapOf<String, Any>()
                            
                            // sharedWith 배열에서 자신 제거
                            updates["sharedWith"] = FieldValue.arrayRemove(userId)
                            
                            // allMembers 배열에서 자신 제거
                            updates["allMembers"] = FieldValue.arrayRemove(userId)
                            
                            // participants 맵에서 자신 제거
                            updates["participants.$userId"] = FieldValue.delete()
                            
                            db.collection("lists")
                                .document(listId)
                                .update(updates)
                                .await()
                        } catch (e: Exception) {
                            // 리스트 나가기 실패 에러
                            return@launch
                        }
                    }
                } else {
                    // 개인 리스트인 경우, RoomDB에서 삭제
                    try {
                        // 랭킹 데이터 업데이트 (리스트 삭제 전에 수행)
                        if (country != null) {
                            val products = listProductDao.getProductsByListId(listId).first()
                            
                            products.forEach { product ->
                                val productId = product.productId
                                // Firestore 리스트에서 불러온 상품일 경우
                                if (productId != null && productId.isNotEmpty()) {
                                    // 국가별 카운트 감소
                                    updateProductStats(country, placeIds, productId, increment = false)
                                }
                            }
                        }
                        
                        listDao.deleteListById(listId)
                    } catch (e: Exception) {
                        // 리스트 삭제 실패 에러
                        return@launch
                    }
                }
                
                // places 테이블에서 해당 리스트 ID 제거 및 정리
                placeIds.forEach { placeId ->
                    val place = placeDao.getPlaceById(placeId)
                    if (place != null) {
                        val updatedListIds = place.listId.filter { it != listId }
                        if (updatedListIds.isEmpty()) {
                            // listId 배열이 비어있으면 해당 장소 데이터 삭제
                            // Geofence 제거
                            GeofenceManager.removeGeofence(getApplication(), place.placeId, place.name)
                            placeDao.deletePlaceById(placeId)
                        } else {
                            // listId 배열에서 해당 리스트 ID 제거
                            placeDao.updatePlace(place.copy(listId = updatedListIds))
                        }
                    }
                }
                
                // Firestore 리스트는 리스너가 자동으로 반영하고,
                // Room DB 리스트는 Flow가 자동으로 반영하므로
                // 별도의 loadLists() 호출 불필요
            } catch (e: Exception) {
                // 리스트 삭제 실패 에러
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    // 성공 메시지 설정
    fun setSuccessMessage(message: String) {
        _uiState.value = _uiState.value.copy(successMessage = message)
    }
    
    // 성공 메시지 초기화
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
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
    
    // 랭킹 데이터 업데이트 (Aggregation Collection)
    private suspend fun updateProductStats(
        country: String?,
        placeIds: List<String>,
        productId: String,
        increment: Boolean = true
    ) {
        if (country == null || productId.isEmpty()) return
        
        val change = if (increment) 1 else -1
        
        try {
            // 트랜잭션을 사용하여 읽기-수정-쓰기를 원자적으로 처리
            db.runTransaction { transaction ->
                // 국가별 전체 카운트 업데이트
                val countryTotalRef = db.collection("product_stats")
                    .document("countries")
                    .collection(country)
                    .document("totalCount")
                
                // 국가별 상품 카운트 업데이트
                val countryProductRef = db.collection("product_stats")
                    .document("countries")
                    .collection(country)
                    .document("products")
                    .collection(productId)
                    .document("count")
                
                // 먼저 현재 값들을 읽어옴 (트랜잭션 내에서)
                val countryTotalDoc = transaction.get(countryTotalRef)
                val countryProductDoc = transaction.get(countryProductRef)
                
                val currentTotalCount = (countryTotalDoc.getLong("count") ?: 0).toLong()
                val currentProductCount = (countryProductDoc.getLong("count") ?: 0).toLong()
                
                val newTotalCount = (currentTotalCount + change).coerceAtLeast(0)
                val newProductCount = (currentProductCount + change).coerceAtLeast(0)
                
                // 업데이트 실행
                if (newTotalCount > 0) {
                    transaction.set(countryTotalRef, hashMapOf("count" to newTotalCount), com.google.firebase.firestore.SetOptions.merge())
                } else if (countryTotalDoc.exists()) {
                    transaction.delete(countryTotalRef)
                }
                
                if (newProductCount > 0) {
                    transaction.set(countryProductRef, hashMapOf("count" to newProductCount), com.google.firebase.firestore.SetOptions.merge())
                } else if (countryProductDoc.exists()) {
                    transaction.delete(countryProductRef)
                }
                
                // 장소별 상품 카운트 업데이트
                placeIds.forEach { placeId ->
                    val placeProductRef = db.collection("product_stats")
                        .document("places")
                        .collection(placeId)
                        .document("products")
                        .collection(productId)
                        .document("count")
                    
                    val placeProductDoc = transaction.get(placeProductRef)
                    val currentPlaceCount = (placeProductDoc.getLong("count") ?: 0).toLong()
                    val newPlaceCount = (currentPlaceCount + change).coerceAtLeast(0)
                    
                    if (newPlaceCount > 0) {
                        transaction.set(placeProductRef, hashMapOf("count" to newPlaceCount), com.google.firebase.firestore.SetOptions.merge())
                    } else if (placeProductDoc.exists()) {
                        transaction.delete(placeProductRef)
                    }
                }
                
                null
            }.await()
        } catch (e: Exception) {
            // 랭킹 데이터 업데이트 실패
            android.util.Log.e("ListViewModel", "Error updating product stats", e)
        }
    }
    
    // 장소별 상품 카운트만 업데이트 (국가 카운트는 제외)
    private suspend fun updateProductStatsForPlace(
        placeId: String,
        productId: String,
        increment: Boolean = true
    ) {
        if (productId.isEmpty()) return
        
        val change = if (increment) 1 else -1
        
        try {
            // 트랜잭션을 사용하여 읽기-수정-쓰기를 원자적으로 처리
            db.runTransaction { transaction ->
                val placeProductRef = db.collection("product_stats")
                    .document("places")
                    .collection(placeId)
                    .document("products")
                    .collection(productId)
                    .document("count")
                
                val placeProductDoc = transaction.get(placeProductRef)
                val currentPlaceCount = (placeProductDoc.getLong("count") ?: 0).toLong()
                val newPlaceCount = (currentPlaceCount + change).coerceAtLeast(0)
                
                if (newPlaceCount > 0) {
                    transaction.set(placeProductRef, hashMapOf("count" to newPlaceCount), com.google.firebase.firestore.SetOptions.merge())
                } else if (placeProductDoc.exists()) {
                    transaction.delete(placeProductRef)
                }
                
                null
            }.await()
        } catch (e: Exception) {
            // 오류 발생
            android.util.Log.e("ListViewModel", "Error updating product stats for place", e)
        }
    }
    
    // 리스트에서 장소 삭제
    suspend fun removePlaceFromList(
        listId: String,
        placeId: String,
        isFirestoreList: Boolean
    ): Result<Unit> {
        return try {
            if (isFirestoreList) {
                // Firestore 리스트인 경우
                val listDoc = db.collection("lists").document(listId).get().await()
                if (!listDoc.exists()) {
                    return Result.failure(Exception("리스트를 찾을 수 없습니다."))
                }
                
                val currentPlaces = (listDoc.get("places") as? List<*>) ?: emptyList<Any?>()
                val currentCountry = listDoc.getString("country")
                
                // 삭제할 장소 찾기
                val placeToRemove = currentPlaces.find { placeMap ->
                    when (placeMap) {
                        is Map<*, *> -> (placeMap["placeId"] as? String) == placeId
                        else -> false
                    }
                }
                
                if (placeToRemove == null) {
                    return Result.failure(Exception("장소를 찾을 수 없습니다."))
                }
                
                // 삭제할 placeId를 제외한 장소들만 필터링함으로써 선택한 장소 제거
                val updatedPlaces = currentPlaces.filter { placeMap ->
                    when (placeMap) {
                        is Map<*, *> -> (placeMap["placeId"] as? String) != placeId
                        else -> false
                    }
                }
                
                // 리스트 업데이트
                if (updatedPlaces.isEmpty() && currentCountry != null) {
                    // places가 비어있으면 places와 country 모두 업데이트
                    db.collection("lists")
                        .document(listId)
                        .update(
                            "places", updatedPlaces,
                            "country", FieldValue.delete()
                        )
                        .await()
                } else {
                    // places만 업데이트
                    db.collection("lists")
                        .document(listId)
                        .update("places", updatedPlaces)
                        .await()
                }
                
                // 통계 업데이트
                val remainingPlaceIds = updatedPlaces.mapNotNull { placeMap ->
                    when (placeMap) {
                        is Map<*, *> -> placeMap["placeId"] as? String
                        else -> null
                    }
                }
                
                // 삭제할 장소가 리스트 내 유일한 장소였는지 확인
                val isLastPlace = currentPlaces.size == 1
                
                // 리스트 내 상품 목록 가져오기
                val productsSnapshot = db.collection("lists")
                    .document(listId)
                    .collection("list_products")
                    .get()
                    .await()
                
                productsSnapshot.documents.forEach { productDoc ->
                    val productId = productDoc.getString("productId")
                    if (productId != null && productId.isNotEmpty() && currentCountry != null) {
                        if (isLastPlace) {
                            // 유일한 장소였을 경우: 국가 통계도 감소
                            updateProductStats(currentCountry, listOf(placeId), productId, increment = false)
                        } else {
                            // 다른 장소들이 남아있을 경우: 해당 장소 통계만 감소
                            updateProductStatsForPlace(placeId, productId, increment = false)
                        }
                    }
                }
            } else {
                // Room DB 리스트인 경우
                val list = listDao.getListById(listId)
                if (list == null) {
                    return Result.failure(Exception("리스트를 찾을 수 없습니다."))
                }
                
                // 삭제할 장소가 있는지 확인
                val placeToRemove = list.places.find { it.placeId == placeId }
                if (placeToRemove == null) {
                    return Result.failure(Exception("장소를 찾을 수 없습니다."))
                }
                
                // 삭제할 placeId를 제외한 장소들만 필터링함으로써 선택한 장소 제거
                val updatedPlaces = list.places.filter { it.placeId != placeId }
                
                // 리스트 업데이트 (places가 비어있으면 country도 null로 설정)
                val updatedCountry = if (updatedPlaces.isEmpty()) null else list.country
                listDao.updateList(list.copy(places = updatedPlaces, country = updatedCountry))
                
                // 통계 업데이트
                val isLastPlace = list.places.size == 1
                
                // 리스트 내 상품 목록 가져오기
                val products = listProductDao.getProductsByListId(listId).first()
                
                products.forEach { product ->
                    val productId = product.productId
                    if (productId != null && productId.isNotEmpty() && list.country != null) {
                        if (isLastPlace) {
                            // 유일한 장소였을 경우: 국가 통계도 감소
                            updateProductStats(list.country!!, listOf(placeId), productId, increment = false)
                        } else {
                            // 다른 장소들이 남아있을 경우: 해당 장소 통계만 감소
                            updateProductStatsForPlace(placeId, productId, increment = false)
                        }
                    }
                }
                
                // places 테이블에서 해당 리스트 ID 제거
                val place = placeDao.getPlaceById(placeId)
                if (place != null) {
                    val updatedListIds = place.listId.filter { it != listId }
                    if (updatedListIds.isEmpty()) {
                        // listId 배열이 비어있으면 해당 장소 데이터 삭제
                        GeofenceManager.removeGeofence(getApplication(), place.placeId, place.name)
                        placeDao.deletePlaceById(placeId)
                    } else {
                        // listId 배열에서 해당 리스트 ID 제거
                        placeDao.updatePlace(place.copy(listId = updatedListIds))
                    }
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 선택된 리스트들에 상품 추가
    suspend fun addProductToSelectedLists(
        productDetails: com.example.tripcart.ui.screen.ProductDetails,
        imageUrls: List<String>? = null // 업로드된 이미지 URL (null이면 productDetails.imageUrls 사용)
    ): Result<Unit> {
        return try {
            val selectedLists = _uiState.value.lists.filter { it.isSelected }
            
            if (selectedLists.isEmpty()) {
                return Result.failure(Exception("리스트를 선택해주세요"))
            }
            
            // 사용할 이미지 URL 결정 (파라미터로 전달된 것이 우선, 없으면 productDetails의 imageUrls)
            val finalImageUrls = imageUrls ?: productDetails.imageUrls
            
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
                            if (finalImageUrls != null && finalImageUrls.isNotEmpty()) {
                                productData["imageUrls"] = finalImageUrls
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
                                
                                // productId가 있는 경우만 랭킹 데이터 업데이트
                                productDetails.productId?.let { productId ->
                                    val country = listDoc.getString("country")
                                    val places = (listDoc.get("places") as? List<*>)?.mapNotNull { 
                                        when (it) {
                                            is Map<*, *> -> it["placeId"] as? String
                                            else -> null
                                        }
                                    } ?: emptyList()
                                    
                                    if (country != null && places.isNotEmpty()) {
                                        updateProductStats(country, places, productId, increment = true)
                                    }
                                }
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
                            imageUrls = finalImageUrls,
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
                            
                            // productId가 있는 경우만 랭킹 데이터 업데이트
                            productDetails.productId?.let { productId ->
                                val country = list.country
                                val placeIds = list.places.map { it.placeId }
                                
                                if (country != null && placeIds.isNotEmpty()) {
                                    updateProductStats(country, placeIds, productId, increment = true)
                                }
                            }
                        }
                    } else {
                        // 새 리스트 생성 당시에는 존재하지 않았던 이미지나 productId를 뒤늦게 업데이트
                        val existingProduct = listProductDao.getProductById(productDetails.id, listItem.listId)
                        if (existingProduct != null) {
                            val updatedProduct = existingProduct.copy(
                                imageUrls = finalImageUrls ?: existingProduct.imageUrls,
                                productId = productDetails.productId ?: existingProduct.productId
                            )
                            listProductDao.updateProduct(updatedProduct)
                            
                            // productId가 업데이트된 경우 랭킹 데이터도 업데이트
                            if (productDetails.productId != null && existingProduct.productId == null) {
                                val list = listDao.getListById(listItem.listId)
                                if (list != null) {
                                    val country = list.country
                                    val placeIds = list.places.map { it.placeId }
                                    
                                    if (country != null && placeIds.isNotEmpty()) {
                                        updateProductStats(country, placeIds, productDetails.productId, increment = true)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Firestore 리스트는 리스너가 자동으로 반영하고,
            // Room DB 리스트는 직접 업데이트했으므로
            // 별도의 loadLists() 호출 불필요
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 리스트에서 상품 삭제
    suspend fun deleteProductFromList(productId: String, listId: String): Result<Unit> {
        return try {
            // 먼저 Room DB에서 확인
            val exists = listProductDao.productExistsInList(productId, listId)
            val firestoreProductId: String?
            
            if (exists) {
                // Room DB 상품인 경우
                val roomProduct = listProductDao.getProductById(productId, listId)
                firestoreProductId = roomProduct?.productId
                
                // 상품 삭제
                listProductDao.deleteProductById(productId, listId)
                
                // 리스트의 productCount 감소
                val list = listDao.getListById(listId)
                if (list != null) {
                    val updatedList = list.copy(productCount = (list.productCount - 1).coerceAtLeast(0))
                    listDao.updateList(updatedList)
                    
                    // productId가 있으면 랭킹 데이터 업데이트
                    firestoreProductId?.let { pid ->
                        val country = list.country
                        val placeIds = list.places.map { it.placeId }
                        if (country != null && placeIds.isNotEmpty()) {
                            updateProductStats(country, placeIds, pid, increment = false)
                        }
                    }
                }
            } else {
                // Room DB에 없으면 Firestore에서 확인
                val firestoreDoc = db.collection("lists")
                    .document(listId)
                    .collection("list_products")
                    .document(productId)
                    .get()
                    .await()
                
                if (!firestoreDoc.exists()) {
                    return Result.failure(Exception("상품이 리스트에 존재하지 않습니다."))
                }
                
                // Firestore에서 productId 가져오기
                firestoreProductId = firestoreDoc.getString("productId")
                
                // 상품 삭제
                db.collection("lists")
                    .document(listId)
                    .collection("list_products")
                    .document(productId)
                    .delete()
                    .await()
                
                // 리스트의 productCount 감소
                val listDoc = db.collection("lists").document(listId).get().await()
                if (listDoc.exists()) {
                    val currentProductCount = (listDoc.getLong("productCount") ?: 0).toInt()
                    db.collection("lists")
                        .document(listId)
                        .update("productCount", (currentProductCount - 1).coerceAtLeast(0))
                        .await()
                    
                    // productId가 있으면 랭킹 데이터 업데이트
                    firestoreProductId?.let { pid ->
                        val country = listDoc.getString("country")
                        val places = (listDoc.get("places") as? List<*>)?.mapNotNull { 
                            when (it) {
                                is Map<*, *> -> it["placeId"] as? String
                                else -> null
                            }
                        } ?: emptyList()
                        
                        if (country != null && places.isNotEmpty()) {
                            updateProductStats(country, places, pid, increment = false)
                        }
                    }
                }
            }
            
            // Firestore 리스트는 리스너가 자동으로 반영하고,
            // Room DB 리스트는 직접 업데이트했으므로
            // 별도의 loadLists() 호출 불필요
            
            Result.success(Unit)
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
    
    // Firestore 리스트 상세 정보를 실시간 Flow로 가져오기
    fun getFirestoreListDetailFlow(listId: String): Flow<ListEntity?> = callbackFlow {
        val listener = db.collection("lists")
            .document(listId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("ListViewModel", "Error listening to Firestore list detail", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    try {
                        val listEntity = parseListDocument(snapshot)
                        trySend(listEntity)
                    } catch (e: Exception) {
                        android.util.Log.e("ListViewModel", "Error parsing list document", e)
                        trySend(null)
                    }
                } else {
                    trySend(null)
                }
            }
        
        awaitClose {
            listener.remove()
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
    
    // 상품 정보 업데이트 (이름, 카테고리, 수량, 메모, 이미지)
    suspend fun updateProductInfo(
        productId: String,
        listId: String,
        productName: String,
        category: String,
        quantity: Int,
        note: String?,
        imageUrls: List<String>?
    ): Result<Unit> {
        return try {
            // 먼저 Room DB에서 확인
            val roomProduct = listProductDao.getProductById(productId, listId)
            if (roomProduct != null) {
                val updatedProduct = roomProduct.copy(
                    productName = productName,
                    category = category,
                    quantity = quantity,
                    note = note,
                    imageUrls = imageUrls
                )
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
                    val updateData = hashMapOf<String, Any>(
                        "productName" to productName,
                        "category" to category,
                        "quantity" to quantity
                    )
                    if (note != null && note.isNotBlank()) {
                        updateData["note"] = note
                    } else {
                        updateData["note"] = ""
                    }
                    if (imageUrls != null && imageUrls.isNotEmpty()) {
                        updateData["imageUrls"] = imageUrls
                    } else {
                        updateData["imageUrls"] = emptyList<String>()
                    }
                    
                    db.collection("lists")
                        .document(listId)
                        .collection("list_products")
                        .document(productId)
                        .update(updateData)
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
                
                // Room DB 리스트는 직접 업데이트했으므로 UI 상태도 직접 업데이트
                _uiState.value = _uiState.value.copy(
                    lists = _uiState.value.lists.map { listItem ->
                        if (listItem.listId == listId) {
                            listItem.copy(status = status)
                        } else {
                            listItem
                        }
                    }
                )
                
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
                        
                        // Firestore 리스트는 리스너가 자동으로 반영하므로 별도 처리 불필요
                        
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
    
    // 리스트 이름 업데이트
    suspend fun updateListName(
        listId: String,
        newName: String,
        isFirestoreList: Boolean
    ): Result<Unit> {
        return try {
            if (isFirestoreList) {
                // Firestore 리스트인 경우
                try {
                    val listDoc = db.collection("lists").document(listId).get().await()
                    if (listDoc.exists()) {
                        db.collection("lists")
                            .document(listId)
                            .update("name", newName)
                            .await()
                        
                        // Firestore 리스트는 리스너가 자동으로 반영하므로 별도 처리 불필요
                        
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("리스트를 찾을 수 없습니다."))
                    }
                } catch (e: Exception) {
                    Result.failure(Exception("리스트 이름 업데이트 실패: ${e.message}"))
                }
            } else {
                // Room DB 리스트인 경우
                val list = listDao.getListById(listId)
                if (list != null) {
                    val updatedList = list.copy(name = newName)
                    listDao.updateList(updatedList)
                    
                    // Room DB 리스트는 직접 업데이트했으므로 UI 상태도 직접 업데이트
                    _uiState.value = _uiState.value.copy(
                        lists = _uiState.value.lists.map { listItem ->
                            if (listItem.listId == listId) {
                                listItem.copy(name = newName)
                            } else {
                                listItem
                            }
                        }
                    )
                    
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("리스트를 찾을 수 없습니다."))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 초대코드 발급 (리스트를 Firestore로 이동)
    suspend fun generateInviteCode(listId: String, right: String, ownerNickname: String): Result<String> {
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
            
            // participants 맵 생성: {userId: {nickname: "닉네임"}}
            // owner는 role이 "owner"로 고정!
            val participants = hashMapOf<String, Any>(
                userId to hashMapOf(
                    "nickname" to ownerNickname
                )
            )
            
            val listData = hashMapOf<String, Any>(
                "name" to list.name,
                "status" to list.status,
                "places" to placesList,
                "ownerId" to userId,
                "sharedWith" to emptyList<String>(),
                "allMembers" to listOf(userId), // ownerId를 포함한 모든 멤버
                "inviteCode" to inviteCode,
                "right" to right,
                "participants" to participants
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
    suspend fun joinListByInviteCode(inviteCode: String, nickname: String): Result<String> {
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
                
                // allMembers 업데이트 (ownerId + sharedWith)
                val existingAllMembers = listDoc.get("allMembers") as? List<*> ?: emptyList<Any?>()
                val updatedAllMembers = existingAllMembers.toMutableList().apply {
                    if (!contains(userId)) {
                        add(userId)
                    }
                }
                
                // participants 맵에 사용자 추가 (닉네임 연동 목적)

                // 기존 participants 필드 불러오기
                val existingParticipants = listDoc.get("participants") as? Map<*, *>

                // 기존 participants 필드가 있다면 빈 mutableMap 생성 후 기존 데이터 복사
                val participants = if (existingParticipants != null) {
                    val mutableMap = mutableMapOf<String, Any>()
                    existingParticipants.forEach { (key, value) ->
                        if (key is String && value is Map<*, *>) {
                            mutableMap[key] = value
                        }
                    }
                    mutableMap
                // 기존 participants 필드가 없다면 빈 mutableMap 생성
                } else {
                    mutableMapOf<String, Any>()
                }
                
                // 새 사용자 추가
                participants[userId] = hashMapOf(
                    "nickname" to nickname
                )
                
                db.collection("lists")
                    .document(listId)
                    .update(
                        mapOf(
                            "sharedWith" to updatedSharedWith,
                            "allMembers" to updatedAllMembers,
                            "participants" to participants
                        )
                    )
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
    
    // 현재 사용자가 리스트의 owner인지 확인
    suspend fun isListOwner(listId: String): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            val listDoc = db.collection("lists").document(listId).get().await()
            if (!listDoc.exists()) {
                return false
            }
            val ownerId = listDoc.getString("ownerId")
            ownerId == userId
        } catch (e: Exception) {
            false
        }
    }
    
    // 리스트 참여자 정보를 실시간 Flow로 가져오기
    fun getListParticipantsFlow(listId: String): Flow<Result<ListParticipantInfo>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(Result.failure(Exception("로그인이 필요합니다.")))
            close()
            return@callbackFlow
        }
        
        val listener = db.collection("lists")
            .document(listId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(Exception("참여자 정보를 가져오는데 실패했습니다: ${error.message}")))
                    return@addSnapshotListener
                }
                
                if (snapshot == null || !snapshot.exists()) {
                    trySend(Result.failure(Exception("리스트를 찾을 수 없습니다.")))
                    return@addSnapshotListener
                }
                
                try {
                    val ownerId = snapshot.getString("ownerId") ?: ""
                    val sharedWith = (snapshot.get("sharedWith") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    val right = snapshot.getString("right") ?: "read"
                    val participantsMap = (snapshot.get("participants") as? Map<*, *>) ?: emptyMap<Any?, Any>()
                    
                    // 현재 사용자의 권한 확인
                    val currentUserRole = when {
                        ownerId == userId -> "owner"
                        sharedWith.contains(userId) -> right
                        else -> null
                    }
                    
                    // 참여자 목록 생성 (owner + sharedWith)
                    val participantIds = mutableListOf<String>()
                    if (ownerId.isNotEmpty()) {
                        participantIds.add(ownerId)
                    }
                    participantIds.addAll(sharedWith)
                    
                    // 중복 제거
                    val uniqueParticipantIds = participantIds.distinct()
                    
                    // 각 참여자 정보 가져오기
                    val participants = uniqueParticipantIds.mapNotNull { participantId ->
                        try {
                            // participants 맵에서 닉네임 가져오기
                            val participantInfo = participantsMap[participantId] as? Map<*, *>
                            val nickname = participantInfo?.get("nickname") as? String
                            
                            // role은 리스트 레벨의 right 필드 사용 (owner는 예외)
                            val role = when {
                                participantId == ownerId -> "owner"
                                sharedWith.contains(participantId) -> right
                                else -> "read"
                            }
                            
                            Participant(
                                userId = participantId,
                                name = nickname?.takeIf { it.isNotEmpty() } ?: "익명의 사용자",
                                role = role
                            )
                        } catch (e: Exception) {
                            // 사용자 정보를 가져올 수 없으면 기본 정보 사용
                            Participant(
                                userId = participantId,
                                name = "익명의 사용자",
                                role = if (participantId == ownerId) "owner" else right
                            )
                        }
                    }
                    
                    // owner 정보 가져오기
                    val ownerInfo = participantsMap[ownerId] as? Map<*, *>
                    // 서버에서 불러오는 값
                    val ownerNickname = ownerInfo?.get("nickname") as? String
                    // 실제로 사용할 값 (ownerNickname이 비어있으면 "익명의 사용자" 사용)
                    val ownerName = ownerNickname?.takeIf { it.isNotEmpty() } ?: "익명의 사용자"
                    
                    val participantInfo = ListParticipantInfo(
                        isShared = true,
                        ownerId = ownerId,
                        ownerName = ownerName,
                        currentUserRole = currentUserRole,
                        currentUserId = userId,
                        participants = participants
                    )
                    
                    trySend(Result.success(participantInfo))
                } catch (e: Exception) {
                    trySend(Result.failure(e))
                }
            }
        
        // 코루틴 시스템이 Flow 취소를 감지하면 자동으로 awaitClose 실행 - 리스너 해제
        awaitClose {
            listener.remove()
        }
    }
    
    // 리스트 참여자 정보 가져오기
    suspend fun getListParticipants(listId: String): Result<ListParticipantInfo> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                return Result.failure(Exception("로그인이 필요합니다."))
            }
            
            val listDoc = db.collection("lists").document(listId).get().await()
            if (!listDoc.exists()) {
                return Result.failure(Exception("리스트를 찾을 수 없습니다."))
            }
            
            val ownerId = listDoc.getString("ownerId") ?: ""
            val sharedWith = (listDoc.get("sharedWith") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val right = listDoc.getString("right") ?: "read"
            val participantsMap = (listDoc.get("participants") as? Map<*, *>) ?: emptyMap<Any?, Any>()
            
            // 현재 사용자의 권한 확인
            val currentUserRole = when {
                ownerId == userId -> "owner"
                sharedWith.contains(userId) -> right
                else -> null
            }
            
            // 참여자 목록 생성 (owner + sharedWith)
            val participantIds = mutableListOf<String>()
            if (ownerId.isNotEmpty()) {
                participantIds.add(ownerId)
            }
            participantIds.addAll(sharedWith)
            
            // 중복 제거
            val uniqueParticipantIds = participantIds.distinct()
            
            // 각 참여자 정보 가져오기
            val participants = uniqueParticipantIds.mapNotNull { participantId ->
                try {
                    // participants 맵에서 닉네임 가져오기
                    val participantInfo = participantsMap[participantId] as? Map<*, *>
                    val nickname = participantInfo?.get("nickname") as? String
                    
                    // role은 리스트 레벨의 right 필드 사용 (owner는 예외)
                    val role = when {
                        participantId == ownerId -> "owner"
                        sharedWith.contains(participantId) -> right
                        else -> "read"
                    }
                    
                    Participant(
                        userId = participantId,
                        name = nickname?.takeIf { it.isNotEmpty() } ?: "익명의 사용자",
                        role = role
                    )
                } catch (e: Exception) {
                    // 사용자 정보를 가져올 수 없으면 기본 정보 사용
                    Participant(
                        userId = participantId,
                        name = "익명의 사용자",
                        role = if (participantId == ownerId) "owner" else right
                    )
                }
            }
            
            // owner 정보 가져오기
            val ownerInfo = participantsMap[ownerId] as? Map<*, *>
            val ownerNickname = ownerInfo?.get("nickname") as? String
            val ownerName = ownerNickname?.takeIf { it.isNotEmpty() } ?: "익명의 사용자"
            
            Result.success(ListParticipantInfo(
                isShared = true,
                ownerId = ownerId,
                ownerName = ownerName,
                currentUserRole = currentUserRole,
                currentUserId = userId,
                participants = participants
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 채팅 메시지 전송
    suspend fun sendChatMessage(listId: String, message: String, imageUrl: String? = null): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                return Result.failure(Exception("로그인이 필요합니다."))
            }
            
            if (message.isBlank() && imageUrl == null) {
                return Result.failure(Exception("메시지 또는 이미지를 입력해주세요."))
            }
            
            val chatData = hashMapOf<String, Any>(
                "senderId" to userId,
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            
            // Not Null (이미지와 텍스트 중 존재하는 값을 실제 데이터로 가짐)
            val messageContent = imageUrl ?: message
            chatData["message"] = messageContent
            
            // 채팅 메시지 저장
            db.collection("lists")
                .document(listId)
                .collection("list_chats")
                .add(chatData)
                .await()
            
            // 알림 생성 - 비동기 처리
            viewModelScope.launch {
                try {
                    createChatNotification(listId, userId, messageContent)
                } catch (e: Exception) {
                    // 에러 발생
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 채팅 알림 생성
    private suspend fun createChatNotification(listId: String, senderId: String, message: String) {
        try {
            // 리스트 정보 가져오기
            val listDoc = db.collection("lists").document(listId).get().await()
            if (!listDoc.exists()) return
            
            val listName = listDoc.getString("name") ?: "리스트"
            val allMembers = (listDoc.get("allMembers") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            
            // 발신자 닉네임 가져오기
            val participantsMap = (listDoc.get("participants") as? Map<*, *>) ?: emptyMap<Any?, Any>()
            val senderNickname = (participantsMap[senderId] as? Map<*, *>)?.get("nickname") as? String ?: "익명"
            
            // 이미지인 경우와 텍스트인 경우를 분리해서 메시지 내용 처리
            val displayMessage = if (message.startsWith("https://firebasestorage") || 
                (message.startsWith("http") && (message.endsWith(".jpg") || message.endsWith(".png")))) {
                "(이미지)"
            } else {
                message
            }
            
            // 발신자를 제외한 모든 멤버에게 알림 생성
            val batch = db.batch()
            val timestamp = com.google.firebase.Timestamp.now()
            
            allMembers.forEach { memberId ->
                if (memberId != senderId) {
                    val notificationRef = db.collection("notifications")
                        .document(memberId)
                        .collection("user_notifications")
                        .document()
                    
                    val notificationData = hashMapOf<String, Any>(
                        "listId" to listId,
                        "listName" to listName,
                        "senderId" to senderId,
                        "senderNickname" to senderNickname,
                        "message" to displayMessage,
                        "createdAt" to timestamp,
                        "isRead" to false,
                        "type" to "chat"
                    )
                    
                    batch.set(notificationRef, notificationData)
                }
            }
            
            batch.commit().await()
            
            // notification 문서 데이터가 추가되면 자동으로 FCM 푸시 시작
            // Cloud Functions의 sendChatNotification 트리거를 이용해 푸시 알림 생성 자동 처리!
            
        } catch (e: Exception) {
            // 에러 발생
        }
    }
    
    // 채팅 메시지 실시간 Flow로 가져오기
    fun getChatMessagesFlow(listId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = db.collection("lists")
            .document(listId)
            .collection("list_chats")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList()) // 에러 발생 시 빈 리스트 전송
                    return@addSnapshotListener
                }
                
                // mapNotNull - Null인 값은 결과에서 제외
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null // 데이터가 없으면 Null 반환
                        ChatMessage(
                            messageId = doc.id,
                            senderId = data["senderId"] as? String ?: "",
                            message = data["message"] as? String ?: "",
                            createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate() ?: java.util.Date()
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(messages)
            }
        
        // 채팅창이 꺼지면 자동으로 채팅방 리스너를 제거함으로써 불필요한 네트워크 요청 방지
        awaitClose {
            listener.remove()
        }
    }
}

// 채팅 메시지 데이터 클래스
data class ChatMessage(
    val messageId: String,
    val senderId: String,
    val message: String,
    val createdAt: java.util.Date
)

// 참여자 정보 데이터 클래스
data class ListParticipantInfo(
    val isShared: Boolean,
    val ownerId: String?,
    val ownerName: String?,
    val currentUserRole: String?, // "owner", "read", "edit"
    val currentUserId: String?,
    val participants: List<Participant>
)

data class Participant(
    val userId: String,
    val name: String,
    val role: String // "owner", "read", "edit"
)

