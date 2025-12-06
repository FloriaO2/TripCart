package com.example.tripcart.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
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
        ListProductEntity::class,    // 상품 정보 테이블
        PlaceEntity::class           // 장소 정보 테이블 (백그라운드 위치 추적용)
    ],
    version = 9,                     // 데이터베이스 스키마 버전
                                     // v1: 초기 버전
                                     // v2: BoughtStatusEntity 제거
                                     //     - Firestore 권한 설정때문에 일관성을 위해 BoughtStatusEntity 분리 후 사용 중이었는데
                                     //       상품 자체를 단순 배열이 아닌 하위 컬렉션으로 분리하면 필드별로 권한 설정이 가능하며
                                     //       그렇게 되면 BoughtStatusEntity를 분리할 필요가 없어짐
                                     //       -> BoughtStatusEntity를 각 상품 내부에서 처리하도록 병합
                                     // v3: ListEntity에 createdAt 필드 추가 (생성순 정렬용)
                                     // v4: ListPlaceEntity 제거 (ListEntity.places로 통합)
                                     // v5: ListEntity에 productCount 필드 추가 (네트워크 효율 개선)
                                     // v6: ListProductEntity 추가 (상품 테이블 생성)
                                     // v7: ListDao에 updateList() 메서드 추가 (UPDATE만 수행, 상품 유지)
                                     //     - 저장 + 수정 간결하게 한 코드로 처리하려고 REPLACE 쓰는 중이었는데
                                     //       (REPLACE 사용하면 키 중복시 단순히 해당 키 내용을 새로운 데이터로 덮어쓰는 걸로 알고 있었음)
                                     //       알고보니 REPLACE는 DELETE + INSERT 라서
                                     //       리스트 수정할 때마다 CASCADE 되어있는 list_products가 함께 삭제
                                     //       -> 저장, 수정을 분리하고 수정은 UPDATE로 구현
                                     // v8: ListProductEntity.productId를 nullable로 변경 (사용자 생성 상품과 Firestore 공개 상품 구분)
                                     // v9: PlaceEntity 추가 (백그라운드 위치 추적 및 푸시 알림용)

    exportSchema = false             // 스키마 내보내기 비활성화
)
@TypeConverters(ListConverter::class)  // ListConverter는 List<String>, List<Place> 타입 변환에 필요
abstract class TripCartDatabase : RoomDatabase() {
    abstract fun listDao(): ListDao              // 리스트 관련 데이터 접근
    abstract fun listProductDao(): ListProductDao // 상품 데이터 접근
    abstract fun placeDao(): PlaceDao            // 장소 데이터 접근
    
    companion object {
        @Volatile // 한 스레드가 변경한 값이 다른 스레드에 즉시 반영 - 멀티스레드 환경에 유리
        private var INSTANCE: TripCartDatabase? = null
        
        fun getDatabase(context: Context): TripCartDatabase {
            // synchronized: 한 스레드가 사용 중이면 다른 스레드는 대기!
            // - 동시에 접근했을 때 INSTANCE가 중복 생성되지 않도록 보완 및 동기화
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TripCartDatabase::class.java,
                    "tripcart_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance // 반환값
            }
        }
    }
}

