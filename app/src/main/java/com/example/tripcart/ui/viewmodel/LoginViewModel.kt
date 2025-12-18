package com.example.tripcart.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripcart.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    // 인증 객체 생성
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    // UI 상태 관리
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    // Google 로그인 클라이언트 관리
    private var googleSignInClient: GoogleSignInClient? = null
    
    init {
        initializeGoogleSignIn()
    }
    
    // 구글 로그인 키 이용해서 토큰 가져오기
    private fun initializeGoogleSignIn() {
        try {
            val webClientId = getApplication<Application>().getString(R.string.default_web_client_id)
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()
            
            googleSignInClient = GoogleSignIn.getClient(getApplication(), gso)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "OAuth 클라이언트 ID가 설정되지 않았습니다. Firebase Console에서 SHA 키를 등록해주세요."
            )
        }
    }
    
    // 구글 로그인에 사용되는 자체 화면 가져오기
    fun getSignInIntent(): Intent {
        return googleSignInClient?.signInIntent
            ?: throw IllegalStateException("Google Sign-In이 초기화되지 않았습니다. Firebase Console에서 SHA 키를 등록해주세요.")
    }
    
    // 구글 로그인 결과 처리
    fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        viewModelScope.launch {
            // 로딩 상태 설정
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // 구글 로그인 결과 가져오기
                val account = completedTask.getResult(ApiException::class.java)
                account?.idToken?.let { idToken ->
                    signInWithFirebase(idToken)
                } ?: run {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "로그인에 실패했습니다."
                    )
                }
            } catch (e: ApiException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "로그인에 실패했습니다: ${e.message}"
                )
            }
        }
    }
    
    // 구글 로그인 토큰을 이용해서 파이어베이스 인증
    private fun signInWithFirebase(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 로그인 성공 시 FCM 토큰 가져와서 저장
                    saveFCMToken()
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSignedIn = true,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Firebase 인증에 실패했습니다: ${task.exception?.message}"
                    )
                }
            }
    }
    
    // FCM 토큰 가져와서 Firestore에 저장
    private fun saveFCMToken() {
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                val userId = auth.currentUser?.uid
                if (userId != null && token != null) {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                        .await()
                }
            } catch (e: Exception) {
                // FCM 토큰 저장 실패 시 무시 (onNewToken에서 자동으로 저장됨)
            }
        }
    }
    
    // 에러 메시지 초기화
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

// UI에 사용되는 로그인 상태 관리
data class LoginUiState(
    val isLoading: Boolean = false,
    val isSignedIn: Boolean = false,
    val errorMessage: String? = null
)

