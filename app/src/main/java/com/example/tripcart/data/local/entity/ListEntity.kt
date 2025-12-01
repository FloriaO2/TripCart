package com.example.tripcart.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.tripcart.data.local.converters.ListConverter

/*
 * ListEntity - 리스트 정보를 저장하는 테이블
 * 
 * 테이블 구조:
 * - tableName: "lists"
 * - PrimaryKey: listId
 */
@Entity(tableName = "lists")
@TypeConverters(ListConverter::class)  // ListConverter를 사용하여 List<String> 타입 변환
data class ListEntity(
    @PrimaryKey
    val listId: String,              // 리스트 고유 ID
    val name: String,                // 리스트 이름
    val status: String,               // 리스트 상태: "준비중", "진행중", "완료"
    val country: String?,             // 리스트의 국가 (null 가능)
    val places: List<Place> = emptyList(),  // 장소 리스트 (name, placeId)
    val productCount: Int = 0,  // 상품 개수 (네트워크 효율을 위해 문서에 함께 저장)
    val createdAt: Long = System.currentTimeMillis(),  // 생성 시간 (생성순 정렬용)
    val firestoreSynced: Boolean = false  // Firestore와 동기화 여부 (마이그레이션용)
)

