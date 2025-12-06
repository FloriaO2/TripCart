package com.example.tripcart.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.tripcart.data.local.converters.ListConverter


// PlaceEntity - 장소 정보를 저장하는 테이블 (백그라운드용)

@Entity(tableName = "places")
@TypeConverters(ListConverter::class)  // ListConverter를 사용하여 List<String> 타입 변환
data class PlaceEntity(
    @PrimaryKey
    val placeId: String,              // 장소 고유 ID (Firestore places 컬렉션의 placeId)
    val lat: Double,                  // 위도
    val lng: Double,                  // 경도
    val name: String,                 // 장소 이름
    val listId: List<String> = emptyList()  // 이 장소를 참조하는 리스트 ID 배열
)

