package com.example.tripcart.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.tripcart.MainActivity
import com.example.tripcart.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class TripCartMessagingService : FirebaseMessagingService() {
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // FCM 토큰을 Firestore에 저장
        saveTokenToFirestore(token)
    }
    
    private fun saveTokenToFirestore(token: String) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) return
        
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        // data payload에서 정보 가져오기
        // - 원래는 notification payload를 사용했었고
        //   notification payload는 알림 표시를 시스템이 자동 처리하는 로직인지라 시스템이 직접 관여했었는데,
        //   이렇게 되면 백그라운드일 때는
        //   알림 표시에 관여하기 위한 시스템이 꺼져있는 상태라 onMessageReceived가 호출되지 않음
        // - data payload는 시스템이 자동 처리하지 않는 로직이라
        //   백그라운드에서도 정상적으로 onMessageReceived가 호출돼서
        //   커스텀 알림을 언제나 표시할 수 있음
        val listId = remoteMessage.data["listId"] ?: ""
        val title = remoteMessage.data["title"] ?: "리스트"
        val senderNickname = remoteMessage.data["senderNickname"] ?: ""
        val message = remoteMessage.data["message"] ?: ""
        
        if (listId.isNotEmpty()) {
            sendNotification(title, senderNickname, message, listId)
        }
    }
    
    private fun sendNotification(title: String, senderNickname: String, message: String, listId: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            //   FLAG_ACTIVITY_NEW_TASK: 새 Task 생성 후 그 안에 새 Activity 생성
            //   FLAG_ACTIVITY_CLEAR_TASK: 현재 Task의 모든 Activity를 삭제하고 새 Activity 생성
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("listId", listId)
            putExtra("navigateTo", "list_detail")
            putExtra("openChat", true) // 채팅 팝업을 바로 열 수 있도록
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            //   FLAG_UPDATE_CURRENT : 기존 PendingIntent가 있으면 거기에 업데이트
            //   FLAG_IMMUTABLE : PendingIntent 내용 변경 불가
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // string.xml에 정의한 기본 알림 채널 ID
        val channelId = getString(R.string.default_notification_channel_id)
        
        // 접힌 상태용 텍스트: {닉네임}: {내용}
        val collapsedText = "$senderNickname: $message"
        
        // 확장 상태용 텍스트: 발신자 닉네임(굵게) + 줄바꿈 + 메시지 내용(작고 얇게)
        val expandedText = android.text.SpannableStringBuilder().apply {
            // 발신자 닉네임 (굵게)
            val nicknameStart = length // 현재 텍스트 길이 (시작 위치 기록)
            append(senderNickname)
            val nicknameEnd = length // 현재 텍스트 길이 (끝 위치 기록)
            val nicknameBoldSpan = android.text.style.StyleSpan(android.graphics.Typeface.BOLD)
            // android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            // - 시작 경계와 끝 경계를 모두 EXCLUSIVE (포함하지 않음) 상태로 인지해서
            //   시작 경계와 끝 경계 사이에 텍스트를 추가해도 Span이 확장되지 않음
            setSpan(nicknameBoldSpan, nicknameStart, nicknameEnd, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            
            // 줄바꿈
            append("\n")
            
            // 메시지 내용 (작고 얇게)
            val messageStart = length // 현재 텍스트 길이 (시작 위치 기록)
            append(message)
            val messageEnd = length // 현재 텍스트 길이 (끝 위치 기록)
            val messageSizeSpan = android.text.style.RelativeSizeSpan(0.85f) // 85% 크기
            val messageStyleSpan = android.text.style.StyleSpan(android.graphics.Typeface.NORMAL)
            // android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            // - 시작 경계와 끝 경계를 모두 EXCLUSIVE (포함하지 않음) 상태로 인지해서
            //   시작 경계와 끝 경계 사이에 텍스트를 추가해도 Span이 확장되지 않음
            setSpan(messageSizeSpan, messageStart, messageEnd, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(messageStyleSpan, messageStart, messageEnd, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.tripcart_border) // 추후, 위치 관련 푸시 알림에서 사용하는 SmallIcon도 이걸로 바꿔야 할 수도 있음
            .setContentTitle(title) // 리스트 이름
            .setContentText(collapsedText) // 접힌 상태: {닉네임}: {내용}
            .setStyle(NotificationCompat.BigTextStyle()
            .bigText(android.text.SpannableString(expandedText))) // 확장된 상태: 서식 적용된 두 줄 텍스트
            .setAutoCancel(true) // 알림 클릭 시 알림 자동 삭제
            .setContentIntent(pendingIntent) // 알림 클릭 시 액션 Intent
            // 알림 우선순위 HIGH
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // 알림 기본 동작 모두 활성화
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOnlyAlertOnce(false) // 각 알림마다 알림음 재생
            .setShowWhen(true) // 시간 표시
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Android O 이상에서는 채널 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "TripCart 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "채팅 및 기타 알림"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // 고유한 알림 ID 생성 (타임스탬프 + 해시코드)
        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt() + listId.hashCode()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}

