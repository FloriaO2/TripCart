package com.example.tripcart.data.local.entity

/*
 * Place - 리스트에 포함된 장소 정보를 저장하는 데이터 클래스
 * 
 * Firestore 구조:
 * "places": [
 *   {"name": "placename1", "placeId": "placeId1"},
 *   {"name": "placename2", "placeId": "placeId2"}
 * ]
 * 
 * Room DB에서의 사용:
 * val place = Place(name = "스타벅스 강남점", placeId = "place123")
 * val places = listOf(place1, place2)
 */
data class Place(
    val name: String,        // 장소 이름 (UI 표시용)
    val placeId: String      // Firestore places 컬렉션의 장소 ID
)

