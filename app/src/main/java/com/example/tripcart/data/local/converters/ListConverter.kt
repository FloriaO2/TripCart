package com.example.tripcart.data.local.converters

import androidx.room.TypeConverter
import com.example.tripcart.data.local.entity.Place

/*
 * ListConverter - Room TypeConverter
 * 
 * 역할:
 * Room은 기본적으로 List<String> 같은 복잡한 타입을 직접 저장할 수 없음
 * 따라서 Converter를 이용해 List<String> ↔ String, List<Place> ↔ String 간 변환을 자동으로 처리
 * 
 * 사용 위치:
 * - ListProductEntity의 imageUrls 필드에서 사용 (List<String>)
 * - ListEntity의 places 필드에서 사용 (List<Place>)
 * - TripCartDatabase에 @TypeConverters로 등록되어 자동 적용
 * 
 * 변환 예시:
 * - List<String>: ["url1", "url2"] → "url1,url2"
 * - List<Place>: [Place("장소1", "id1"), Place("장소2", "id2")] → "장소1|||id1;;;장소2|||id2"
 * 
 * 관계:
 * - TripCartDatabase에서 @TypeConverters로 등록
 * - ListProductEntity의 imageUrls 필드에 자동 적용
 * - ListEntity의 places 필드에 자동 적용
 */

class ListConverter {
    /*
     * List<String>을 String으로 변환 (DB에 저장할 때)
     * @param value 변환할 리스트
     * @return 콤마로 구분된 문자열
     */
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(separator = ",")
    }
    
    /*
     * String을 List<String>으로 변환 (DB에서 읽을 때)
     * @param value 콤마로 구분된 문자열
     * @return 문자열 리스트
     */
    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) {
            emptyList()
        } else {
            value.split(",")
        }
    }
    
    /*
     * List<Place>를 String으로 변환 (DB에 저장할 때)
     * 형식: "장소명1|||placeId1;;;장소명2|||placeId2"
     * - ||| : name과 placeId 구분자
     * - ;;; : 각 Place 항목 구분자
     * 
     * @param value 변환할 Place 리스트
     * @return 구분자로 연결된 문자열
     */
    @TypeConverter
    fun fromPlaceList(value: List<Place>): String {
        if (value.isEmpty()) return ""
        return value.joinToString(separator = ";;;") { place ->
            "${place.name}|||${place.placeId}"
        }
    }
    
    /*
     * String을 List<Place>로 변환 (DB에서 읽을 때)
     * 형식: "장소명1|||placeId1;;;장소명2|||placeId2"
     * 
     * @param value 구분자로 연결된 문자열
     * @return Place 리스트
     */
    @TypeConverter
    fun toPlaceList(value: String): List<Place> {
        if (value.isEmpty()) return emptyList()
        
        return try {
            value.split(";;;").mapNotNull { item ->
                val parts = item.split("|||", limit = 2)
                if (parts.size == 2) {
                    Place(name = parts[0], placeId = parts[1])
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

