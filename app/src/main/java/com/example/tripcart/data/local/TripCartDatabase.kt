package com.example.tripcart.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.tripcart.data.local.converters.ListConverter
import com.example.tripcart.data.local.dao.*
import com.example.tripcart.data.local.entity.*

/*
 * TripCartDatabase - RoomDB 메인 클래스
 * 
 * 1. Entity(테이블) 등록 및 관리
 * 2. DAO(데이터 접근 인터페이스) 제공
 * 3. TypeConverter를 적용하여 복잡한 타입 변환 지원
 */
@Database(
    entities = [
        ListEntity::class,           // 리스트 정보 테이블 (places 필드에 Place 객체 리스트 포함)
        ListProductEntity::class     // 상품 정보 테이블
    ],
    version = 5,                     // 데이터베이스 스키마 버전 
                                     // v1: 초기 버전
                                     // v2: BoughtStatusEntity 제거
                                     // v3: ListEntity에 createdAt 필드 추가 (생성순 정렬용)
                                     // v4: ListPlaceEntity 제거 (ListEntity.places로 통합)
                                     // v5: ListEntity에 productCount 필드 추가 (네트워크 효율 개선)
    exportSchema = false             // 스키마 내보내기 비활성화
)
@TypeConverters(ListConverter::class)  // ListConverter는 List<String>, List<Place> 타입 변환에 필요
abstract class TripCartDatabase : RoomDatabase() {
    abstract fun listDao(): ListDao              // 리스트 관련 데이터 접근
    abstract fun listProductDao(): ListProductDao // 상품 데이터 접근
}

