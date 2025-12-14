package com.example.tripcart.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.tripcart.R
import com.example.tripcart.ui.theme.PrimaryAccent
import com.example.tripcart.ui.viewmodel.ChatMessage
import com.example.tripcart.ui.viewmodel.ListViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.*
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDialog(
    listId: String,
    onDismiss: () -> Unit,
    viewModel: ListViewModel
) {
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""
    
    // 채팅 메시지 목록
    val messages by viewModel.getChatMessagesFlow(listId).collectAsState(initial = emptyList())
    val listState = rememberLazyListState()
    
    // 입력 상태
    var messageText by remember { mutableStateOf("") }
    
    // 카메라 상태
    var showCameraScreen by remember { mutableStateOf(false) }
    
    // 확대된 이미지 상태
    var expandedImageUrl by remember { mutableStateOf<String?>(null) }
    
    // 참여자 정보 (닉네임 가져오기용) - 실시간 업데이트
    val participantInfoResult by viewModel.getListParticipantsFlow(listId).collectAsState(initial = Result.failure(Exception("로딩 중...")))
    val participantsMap = remember(participantInfoResult) {
        participantInfoResult.getOrNull()?.let { participantInfo ->
            participantInfo.participants.associate { it.userId to it.name }
        } ?: emptyMap()
    }
    
    // 메시지가 업데이트되면 스크롤을 맨 아래로!
    // 추후 스크롤 내리는 버튼을 추가하는 식으로 보완 예정
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    // 화면 밀도를 반영해서 px를 dp로 변환
    val density = LocalDensity.current

    // imeInsets -> imeBottom -> isKeyboardVisible 연쇄적으로 사용
    // 키보드 객체 정보 가져오기
    val imeInsets = WindowInsets.ime 
    // 화면 하단에서 키보드 상단까지의 거리 측정 (양수인지 아닌지에 따라 키보드 열려있는지 닫혀있는지 확인 가능)
    val imeBottom = with(density) { imeInsets.getBottom(this) } 
    // 키보드가 열려있는지 닫혀있는지 체크
    val isKeyboardVisible = imeBottom > 0
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // 플랫폼 기본 너비 대신 커스텀 너비 사용
            decorFitsSystemWindows = false // 키보드 관련 UI를 커스텀 조작하기 위해,
                                           // 시스템이 자체적으로 조작하지 않도록 막기
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f) // 화면 너비의 85% 차지
                    // 키보드 열려있으면 화면 높이의 100%, 닫혀있으면 90%만큼만 차지
                    .fillMaxHeight(if (isKeyboardVisible) 1f else 0.9f)
                    // 키보드 열려있으면 화면 상단에 정렬, 닫혀있으면 중앙에 정렬
                    .align(if (isKeyboardVisible) Alignment.TopCenter else Alignment.Center),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 상단 바
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = PrimaryAccent.copy(alpha = 0.7f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "채팅",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Text(
                                text = "닫기",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                
                // 채팅 메시지 목록
                LazyColumn(
                    state = listState, // 스크롤 상태 관리에 필요
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(messages) { index, message ->
                        val prevMessage = if (index > 0) messages[index - 1] else null
                        
                        // 첫 채팅이거나, 이전 메시지의 날짜와 현재 메시지의 날짜가 다르면 날짜 표시
                        val showDateDivider = prevMessage == null || !isSameDay(
                            prevMessage.createdAt,
                            message.createdAt
                        )
                        
                        if (showDateDivider) {
                            DateDivider(date = message.createdAt)
                        }
                        
                        ChatBubble(
                            message = message,
                            isMyMessage = message.senderId == currentUserId,
                            senderName = participantsMap[message.senderId] ?: "익명",
                            // 남이 보낸 채팅만 발신자 닉네임 보여주기
                            showSenderName = message.senderId != currentUserId,
                            onImageClick = { imageUrl ->
                                expandedImageUrl = imageUrl
                            }
                        )
                    }
                }
                
                // 하단 입력창
                ChatInputBar(
                    messageText = messageText,
                    onMessageTextChange = { messageText = it },
                    onSendClick = {
                        if (messageText.isNotBlank()) {
                            scope.launch {
                                viewModel.sendChatMessage(listId, messageText.trim())
                                messageText = "" // 입력 필드 초기화
                            }
                        }
                    },
                    onCameraClick = {
                        showCameraScreen = true
                    }
                )
            }
        }
        }
    }
    
    // 카메라 전체 화면
    if (showCameraScreen) {
        CameraScreen(
            listId = listId,
            onDismiss = { showCameraScreen = false },
            onImageSent = { showCameraScreen = false },
            viewModel = viewModel
        )
    }
    
    // 확대된 이미지 보기
    expandedImageUrl?.let { imageUrl ->
        ExpandedImageView(
            imageUrl = imageUrl,
            onDismiss = { expandedImageUrl = null }
        )
    }
}

@Composable
fun CameraScreen(
    listId: String,
    onDismiss: () -> Unit,
    onImageSent: () -> Unit,
    viewModel: ListViewModel
) {
    val context = LocalContext.current // 앱 정보 접근, 시스템 서비스 접근, 리소스 접근 등에 필요
                                       // Android에서 제공하며, 이것저것 하기 위한 신분증으로 생각하면 됨
    val scope = rememberCoroutineScope()
    val storage = FirebaseStorage.getInstance()
    val lifecycleOwner = LocalLifecycleOwner.current // 카메라 관련 상태 관리에 필요
    
    var cameraMode by remember { mutableStateOf<CameraMode>(CameraMode.CAPTURE) } // 촬영 vs 미리보기
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) } // 촬영한 이미지의 임시 URI
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) } // 이미지 촬영 기능을 제공하는 도구
    // photoFile, photoUri는 로컬에서 다루는 임시 데이터 (Storage 업로드 후 삭제)
    var photoFile: File? by remember { mutableStateOf(null) } // Storage에 올리기 전 임시 이미지 파일
    var photoUri: Uri? by remember { mutableStateOf(null) } // Storage에 올리기 전 임시 이미지 URI
    
    // 카메라 권한 확인
    val hasCameraPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission() // 카메라 권한 다이얼로그 표시 및 결과 받기
    ) { isGranted ->
        if (!isGranted) {
            onDismiss() // 권한이 없으면 카메라 화면 닫기
        }
    }
    
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        
        // 임시 파일 생성
        val file = File(context.getExternalFilesDir(null), "temp_photo_${System.currentTimeMillis()}.jpg")
        photoFile = file
        photoUri = FileProvider.getUriForFile( // 외부 Storage에서 접근 가능하도록 이미지 URI 생성
            context, // 현재 앱의 신분증 st
            "${context.packageName}.fileprovider", // TripCart에서의 FileProvider입니다!를 명명
            file // 임시 파일 객체
        )
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // 플랫폼 기본 너비 대신 커스텀 너비 사용
            decorFitsSystemWindows = false // 키보드 관련 UI를 커스텀 조작하기 위해,
                                           // 시스템이 자체적으로 조작하지 않도록 막기
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            when (cameraMode) {
                CameraMode.CAPTURE -> {
                    // 카메라 촬영 화면
                    if (!hasCameraPermission) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("카메라 권한이 필요합니다.", color = Color.White)
                        }
                    } else {
                        // 카메라를 켜기 위해 ProcessCameraProvider 가져오기
                        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
                        
                        AndroidView(
                            factory = { ctx -> // 카메라 미리보기 뷰 생성 (ctx = context)
                                val previewView = PreviewView(ctx) // 실시간 카메라 화면
                                val executor = ContextCompat.getMainExecutor(ctx) // 메인 스레드에서 실행되도록 설정
                                
                                cameraProviderFuture.addListener({ // 카메라 Provider 초기화가 완료되기까지 대기
                                    val cameraProvider = cameraProviderFuture.get() // 카메라 Provider 가져오기
                                    
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider) // 미리보기 표시
                                    }
                                    
                                    imageCapture = ImageCapture.Builder() // 이미지 촬영 기능을 제공하는 도구
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY) // 촬영 지연 최소화 모드
                                        .build()
                                    
                                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA // 후면 카메라 선택
                                    
                                    try {
                                        cameraProvider.unbindAll() // 기존 카메라 바인딩 해제
                                        cameraProvider.bindToLifecycle( // 카메라 Lifecycle에 바인딩
                                                                        // 생명 주기 사용 - 화면 사라지면 카메라 자동 해제
                                            lifecycleOwner, // 이 생명주기를 사용하겠다!
                                            cameraSelector, // 카메라 선택
                                            preview, // 미리보기
                                            imageCapture // 이미지 촬영 기능을 제공하는 도구
                                        )
                                    } catch (e: Exception) {
                                        // 카메라 바인딩 실패
                                    }
                                }, executor)
                                
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // 하단 촬영 버튼
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(
                                onClick = {
                                    val capture = imageCapture ?: return@IconButton
                                    val file = photoFile ?: return@IconButton
                                    val uri = photoUri ?: return@IconButton
                                    
                                    // file에 사진 저장!
                                    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                                    
                                    capture.takePicture( // 실제 사진 촬영
                                        outputFileOptions,
                                        Executors.newSingleThreadExecutor(), // 촬영 결과 처리를 위한 스레드 풀
                                        object : ImageCapture.OnImageSavedCallback { // 촬영 결과 처리
                                            // 촬영 결과 성공 시
                                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                                capturedImageUri = uri
                                                cameraMode = CameraMode.PREVIEW
                                            }
                                            
                                            // 촬영 결과 실패 시
                                            override fun onError(exception: ImageCaptureException) {
                                                // 에러 발생
                                            }
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(Color.White, RoundedCornerShape(36.dp))
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.camera),
                                    contentDescription = "촬영",
                                    modifier = Modifier.size(40.dp),
                                    tint = PrimaryAccent
                                )
                            }
                        }
                        
                        // 상단 취소 버튼
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "취소",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                
                CameraMode.PREVIEW -> {
                    // 촬영한 이미지 프리뷰 화면
                    capturedImageUri?.let { uri ->
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // 상단 버튼 바
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.Black.copy(alpha = 0.7f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = {
                                        capturedImageUri = null // 촬영한 이미지 URI 초기화
                                        cameraMode = CameraMode.CAPTURE // 촬영 모드로 변경
                                    }) {
                                        Text("취소", color = Color.White, fontSize = 16.sp)
                                    }
                                    
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    // 현재 사용자 확인
                                                    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                                    
                                                    if (currentUser == null) {
                                                        return@launch
                                                    }
                                                    
                                                    // Firebase Storage에 업로드
                                                    // - 일단 JAVA UUID 사용, 추후 Firebase 자동 ID로 변경할 수도 있음
                                                    val messageId = UUID.randomUUID().toString()
                                                    val storagePath = "chats/$listId/$messageId.jpg"
                                                    
                                                    // Firestore에서 리스트 정보 확인
                                                    try {
                                                        val listDoc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                                            .collection("lists")
                                                            .document(listId)
                                                            .get()
                                                            .await()
                                                        
                                                        if (!listDoc.exists()) {
                                                            return@launch
                                                        }
                                                        
                                                        val listData = listDoc.data
                                                        val ownerId = listData?.get("ownerId") as? String
                                                        val sharedWith = listData?.get("sharedWith") as? List<*>
                                                        
                                                        // Storage 보안 규칙에서 사용하는 allMembers 필드 확인
                                                        val allMembers = listData?.get("allMembers")
                                                        val allMembersList = allMembers as? List<*>
                                                        
                                                        if (allMembersList != null) {
                                                            val currentUserId = currentUser.uid
                                                            val isInAllMembers = currentUserId in allMembersList
                                                            
                                                            if (!isInAllMembers) {
                                                                return@launch
                                                            }
                                                        } else {
                                                            return@launch
                                                        }
                                                    } catch (e: Exception) {
                                                        return@launch
                                                    }
                                                    
                                                    val imageRef = storage.reference.child(storagePath) // Storage 하위 경로 참조 생성
                                                    val uploadTask = imageRef.putFile(uri).await() // 이미지 업로드
                                                    val imageUrl = uploadTask.storage.downloadUrl.await().toString() // 이미지 URL 가져오기
                                                    
                                                    viewModel.sendChatMessage(listId, "", imageUrl) // 채팅 메시지 전송
                                                                                                    // 중간 ""는 텍스트 내용이 없음을 의미
                                                    
                                                    // 로컬 임시 파일 삭제
                                                    photoFile?.let { file ->
                                                        try {
                                                            if (file.exists()) {
                                                                file.delete()
                                                            }
                                                        } catch (e: Exception) {
                                                            // 임시 파일 삭제 실패
                                                        }
                                                    }
                                                    
                                                    onImageSent()
                                                } catch (e: Exception) {
                                                    // 이미지 업로드 실패
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PrimaryAccent
                                        )
                                    ) {
                                        Text("전송", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            
                            // 이미지 프리뷰
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Image( // 화면에 이미지 표시
                                    painter = rememberAsyncImagePainter(
                                        ImageRequest.Builder(context)
                                            .data(uri)
                                            .build()
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    // 전체 보기 - 이미지 비율 유지하며 부모 영역에 맞춤
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class CameraMode {
    CAPTURE,
    PREVIEW
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    isMyMessage: Boolean,
    senderName: String,
    showSenderName: Boolean,
    onImageClick: (String) -> Unit = {}
) {
    val isImage = message.message.startsWith("https://firebasestorage") || 
                  (message.message.startsWith("http") && message.message.endsWith(".jpg")) || 
                  (message.message.startsWith("http") && message.message.endsWith(".png"))
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMyMessage) Alignment.End else Alignment.Start
    ) {
        if (showSenderName) {
            Text(
                text = senderName,
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (!isMyMessage) {
                // 좌측 말풍선
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFE0E0E0))
                        .padding(6.dp)
                ) {
                    if (isImage) {
                        Image(
                            imageUrl = message.message,
                            modifier = Modifier
                                .width(200.dp)
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onImageClick(message.message) }
                        )
                    } else {
                        Text(
                            text = message.message,
                            fontSize = 14.sp,
                            color = Color.Black,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }
                Text(
                    text = formatTime(message.createdAt),
                    fontSize = 10.sp,
                    color = Color.Gray,
                    modifier = Modifier
                        .padding(start = 4.dp, bottom = 4.dp)
                        .widthIn(min = 40.dp) // 시간이 차지할 최소 너비 보장
                )
            } else {
                // 우측 말풍선
                Text(
                    text = formatTime(message.createdAt),
                    fontSize = 10.sp,
                    color = Color.Gray,
                    modifier = Modifier
                        .padding(end = 4.dp, bottom = 4.dp)
                        .widthIn(min = 40.dp) // 시간이 차지할 최소 너비 보장
                )
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PrimaryAccent)
                        .padding(6.dp)
                ) {
                    if (isImage) {
                        Image(
                            imageUrl = message.message,
                            modifier = Modifier
                                .width(200.dp)
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onImageClick(message.message) }
                        )
                    } else {
                        Text(
                            text = message.message,
                            fontSize = 14.sp,
                            color = Color.White,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Image(
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .build()
    )
    
    androidx.compose.foundation.Image( // 화면에 이미지 표시
        painter = painter,
        contentDescription = null,
        modifier = modifier,
        // 잘라 보기 - 이미지 비율을 유지하되 부모 영역에 맞춰 잘라내거나 확대
        contentScale = androidx.compose.ui.layout.ContentScale.Crop
    )
}

@Composable
fun ExpandedImageView(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // 플랫폼 기본 너비 대신 커스텀 너비 사용
            decorFitsSystemWindows = false // 키보드 관련 UI를 커스텀 조작하기 위해,
                                           // 시스템이 자체적으로 조작하지 않도록 막기
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() }, // 화면 클릭 시 닫기
            contentAlignment = Alignment.Center
        ) {
            // 닫기 버튼
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "닫기",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // 확대된 이미지
            val painter = rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .build()
            )
            
            androidx.compose.foundation.Image( // 화면에 이미지 표시
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                // 잘라 보기 - 이미지 비율을 유지하되 부모 영역에 맞춰 잘라내거나 확대
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        }
    }
}

@Composable
fun DateDivider(date: Date) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 5.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatDate(date),
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier
                .background(
                    Color(0xFFE0E0E0),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ChatInputBar(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // 카메라 버튼
            IconButton(
                onClick = onCameraClick,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Bottom)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.camera),
                    contentDescription = "카메라",
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // 입력창
            var textFieldValue by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(messageText)) }
            
            // 채팅 전송 후 messageText가 빈 문자열로 초기화되면 textFieldValue도 초기화해서 입력창 비워주기
            LaunchedEffect(messageText) { // messageText가 변경될 때마다 실행
                if (messageText.isEmpty() && textFieldValue.text.isNotEmpty()) {
                    textFieldValue = androidx.compose.ui.text.input.TextFieldValue("")
                }
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight() // 내용물 높이에 맞추기 - 텍스트 늘어나면 Box 높이도 함께 증가
                    .heightIn(min = 40.dp, max = 120.dp) // 최소 40dp, 최대 120dp 높이로 제한
                    .border(
                        width = 1.dp,
                        color = Color.Gray,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // OutlinedTextField 사용하면 기본 내장 padding때문에 height가 너무 커져서
                // BasicTextField을 사용해 UI 커스텀
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = {
                        textFieldValue = it
                        onMessageTextChange(it.text)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    ),
                    maxLines = 7,
                    minLines = 1,
                    // 커스텀 placeholder
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (textFieldValue.text.isEmpty()) {
                                Text(
                                    "메시지를 입력하세요",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField() // 실제 입력 필드 (항시 렌더링)
                        }
                    }
                )
            }
            
            // 전송 버튼
            IconButton(
                onClick = onSendClick,
                enabled = messageText.isNotBlank(), // 입력 내용 없으면 비활성화
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Bottom)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "전송",
                    tint = if (messageText.isNotBlank()) PrimaryAccent else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun formatTime(date: Date): String {
    val calendar = Calendar.getInstance()
    calendar.time = date // 변환에 사용할 날짜 객체 ex) 채팅에서 createAt에 저장된 date
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    
    val amPm = if (hour < 12) "오전" else "오후"
    val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    
    return "$amPm $displayHour:${String.format("%02d", minute)}"
}

fun formatDate(date: Date): String {
    val calendar = Calendar.getInstance()
    calendar.time = date // 변환에 사용할 날짜 객체 ex) 채팅에서 createAt에 저장된 date
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1 // 월은 0부터 시작하므로 +1
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    
    return "${year}년 ${month}월 ${day}일"
}

fun isSameDay(date1: Date, date2: Date): Boolean {
    val cal1 = Calendar.getInstance()
    cal1.time = date1
    val cal2 = Calendar.getInstance()
    cal2.time = date2
    
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

