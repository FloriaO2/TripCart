package com.example.tripcart.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.tripcart.data.local.converters.ListConverter

/*
 * ListProductEntity - 리스트에 포함된 상품 정보를 저장하는 테이블
 * - Firestore의 lists/{listId}/products 서브컬렉션과 동일한 구조
 * - Firestore의 lists/{listId}/products/{productId} 이런 식으로 구현
 */
@Entity(
    tableName = "list_products",
    foreignKeys = [
        ForeignKey(
            entity = ListEntity::class,        // 참조하는 부모 테이블
            parentColumns = ["listId"],         // 부모 테이블의 컬럼
            childColumns = ["listId"],          // 이 테이블의 컬럼
            onDelete = ForeignKey.CASCADE       // 리스트 삭제 시 리스트 내 상품도 자동 삭제
        )
    ],
    indices = [Index(value = ["listId"])]      // listId로 조회 성능 향상 (listId 컬럼에 인덱스 생성)
)
@TypeConverters(ListConverter::class)  // ListConverter 적용
data class ListProductEntity(
    @PrimaryKey
    val id: String,                    // 상품 항목 고유 ID
    val listId: String,                // 소속 리스트 ID (외래키)
    val productId: String?,            // Firestore products 컬렉션의 ID (null 가능, 랭킹 반영용)
    val productName: String,           // 상품 이름
    val category: String,              // 상품 카테고리
    val imageUrls: List<String>,       // 상품 이미지 URL 리스트 (ListConverter로 변환됨)
    val quantity: Int,                 // 수량
    val note: String?,                 // 메모
    val bought: String = "구매전"      // 구매 상태: "구매전", "구매중", "구매완료"
)

