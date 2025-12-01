package com.example.tripcart.data.remote

import com.google.gson.annotations.SerializedName

// Autocomplete API 응답 모델 - 장소 검색 결과
data class AutocompleteResponse(
    @SerializedName("predictions")
    val predictions: List<Prediction>?,
    @SerializedName("status")
    val status: String
)

data class Prediction(
    @SerializedName("place_id")
    val placeId: String,
    @SerializedName("description")
    val description: String,
    // 장소 이름이랑 주소를 분리한 형태
    @SerializedName("structured_formatting")
    val structuredFormatting: StructuredFormatting?
)

data class StructuredFormatting(
    // 장소 이름
    @SerializedName("main_text")
    val mainText: String,
    // 장소 주소
    @SerializedName("secondary_text")
    val secondaryText: String?
)

// Place Details API 응답 모델 - 장소 상세 정보 열람 결과
data class PlaceDetailsResponse(
    @SerializedName("result")
    val result: PlaceResult?,
    @SerializedName("status")
    val status: String
)

data class PlaceResult(
    @SerializedName("place_id")
    val placeId: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("geometry")
    val geometry: Geometry?,
    @SerializedName("formatted_address")
    val formattedAddress: String?,
    @SerializedName("address_components")
    val addressComponents: List<AddressComponent>?,
    @SerializedName("formatted_phone_number")
    val formattedPhoneNumber: String?,
    @SerializedName("website")
    val website: String?,
    @SerializedName("opening_hours")
    val openingHours: OpeningHours?,
    @SerializedName("current_opening_hours")
    val currentOpeningHours: CurrentOpeningHours?,
    @SerializedName("business_status")
    val businessStatus: String?,
    @SerializedName("photos")
    val photos: List<Photo>?
)

data class Geometry(
    @SerializedName("location")
    val location: Location?
)

data class Location(
    @SerializedName("lat")
    val lat: Double,
    @SerializedName("lng")
    val lng: Double
)

data class AddressComponent(
    @SerializedName("long_name")
    val longName: String,
    @SerializedName("short_name")
    val shortName: String,
    @SerializedName("types")
    val types: List<String>
)

data class OpeningHours(
    @SerializedName("weekday_text")
    val weekdayText: List<String>?,
    @SerializedName("open_now")
    val openNow: Boolean?
)

data class CurrentOpeningHours(
    @SerializedName("weekday_text")
    val weekdayText: List<String>?,
    @SerializedName("open_now")
    val openNow: Boolean?
)

data class Photo(
    @SerializedName("photo_reference")
    val photoReference: String,
    @SerializedName("width")
    val width: Int?,
    @SerializedName("height")
    val height: Int?
)

