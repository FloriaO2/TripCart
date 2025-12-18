package com.example.tripcart.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Job
import java.util.Date

data class NotificationItem(
    val notificationId: String,
    val listId: String,
    val listName: String,
    val senderId: String,
    val senderNickname: String,
    val message: String,
    val createdAt: Date,
    val isRead: Boolean,
    val type: String = "chat" // "chat" 또는 다른 타입
)

data class NotificationUiState(
    val notifications: List<NotificationItem> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val unreadChatListIds: Set<String> = emptySet() // 읽지 않은 채팅 알림이 있는 listId 목록
)

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // 알림 로드 Job 추적 (중복 실행 방지)
    private var loadJob: Job? = null
    
    init {
        loadNotifications()
    }
    
    // 알림 목록 실시간 로드 (추후 서술할 getNotificationsFlow 함수로 수집한 데이터를 이용)
    fun loadNotifications() {
        // 이미 실행 중인 Job이 있으면 취소하고 새로 시작
        loadJob?.cancel()
        
        loadJob = viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // getNotificationsFlow 함수로 수집한 데이터를 이용!
                getNotificationsFlow(userId).collect { notifications ->
                    val unreadCount = notifications.count { !it.isRead }
                    // 읽지 않은 채팅 알림의 listId 목록 추출
                    val unreadChatListIds = notifications
                        .filter { !it.isRead && it.type == "chat" }
                        .map { it.listId }
                        .toSet()
                    _uiState.value = _uiState.value.copy(
                        notifications = notifications,
                        unreadCount = unreadCount,
                        unreadChatListIds = unreadChatListIds,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }
    
    // 알림 목록 실시간 Flow (데이터 수집 역할)
    private fun getNotificationsFlow(userId: String): Flow<List<NotificationItem>> = callbackFlow {
        val listener = db.collection("notifications")
            .document(userId)
            .collection("user_notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList()) // 에러 발생 시 빈 리스트 전송
                    return@addSnapshotListener
                }
                
                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                        NotificationItem(
                            notificationId = doc.id,
                            listId = data["listId"] as? String ?: "",
                            listName = data["listName"] as? String ?: "",
                            senderId = data["senderId"] as? String ?: "",
                            senderNickname = data["senderNickname"] as? String ?: "",
                            message = data["message"] as? String ?: "",
                            createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                            isRead = data["isRead"] as? Boolean ?: false,
                            type = data["type"] as? String ?: "chat"
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(notifications)
            }
        
        awaitClose { listener.remove() }
    }
    
    // 알림 읽음 처리
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            
            try {
                db.collection("notifications")
                    .document(userId)
                    .collection("user_notifications")
                    .document(notificationId)
                    .update("isRead", true)
                    .await()
            } catch (e: Exception) {
                // 에러 처리
            }
        }
    }
    
    // 모든 알림 읽음 처리
    fun markAllAsRead() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            
            try {
                val notifications = db.collection("notifications")
                    .document(userId)
                    .collection("user_notifications")
                    .whereEqualTo("isRead", false)
                    .get()
                    .await()
                
                val batch = db.batch()
                // notifications: isRead가 false였던 애들만 모아놓은 집합
                notifications.documents.forEach { doc ->
                    // doc.reference: 문서 위치 정보
                    batch.update(doc.reference, "isRead", true)
                }
                batch.commit().await()
            } catch (e: Exception) {
                // 에러 처리
            }
        }
    }
    
    // 특정 리스트의 채팅 알림 모두 읽음 처리
    fun markChatNotificationsAsRead(listId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            
            try {
                val notifications = db.collection("notifications")
                    .document(userId)
                    .collection("user_notifications")
                    .whereEqualTo("listId", listId)
                    .whereEqualTo("type", "chat")
                    .whereEqualTo("isRead", false)
                    .get()
                    .await()
                
                val batch = db.batch()
                notifications.documents.forEach { doc ->
                    batch.update(doc.reference, "isRead", true)
                }
                batch.commit().await()
            } catch (e: Exception) {
                // 에러 처리
            }
        }
    }
    
    // 알림 삭제
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            
            try {
                db.collection("notifications")
                    .document(userId)
                    .collection("user_notifications")
                    .document(notificationId)
                    .delete()
                    .await()
            } catch (e: Exception) {
                // 에러 처리
            }
        }
    }
    
    // 모든 알림 삭제
    fun deleteAllNotifications() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            
            try {
                val notifications = db.collection("notifications")
                    .document(userId)
                    .collection("user_notifications")
                    .get()
                    .await()
                
                val batch = db.batch()
                notifications.documents.forEach { doc ->
                    // doc.reference: 문서 위치 정보
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            } catch (e: Exception) {
                // 에러 처리
            }
        }
    }
}

