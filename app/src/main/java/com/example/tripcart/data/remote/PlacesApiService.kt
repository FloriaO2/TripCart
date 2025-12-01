package com.example.tripcart.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

// Google Places API REST 서비스
interface PlacesApiService {
    
    // 장소 자동완성 검색
    @GET("place/autocomplete/json")
    suspend fun autocomplete(
        @Query("input") input: String, // 검색어
        @Query("key") key: String, // Google Places API 키
        @Query("language") language: String = "ko" // 언어 디폴트값 한국어
    ): AutocompleteResponse
    
    // 장소 상세 정보 조회
    @GET("place/details/json")
    suspend fun placeDetails(
        @Query("place_id") placeId: String,
        @Query("key") key: String, // Google Places API 키
        @Query("language") language: String = "ko", // 언어 디폴트값 한국어
        // 요청할 데이터 목록
        @Query("fields") fields: String = "place_id,name,geometry,formatted_address,address_components,formatted_phone_number,website,opening_hours,current_opening_hours,business_status,photos"
    ): PlaceDetailsResponse
}

