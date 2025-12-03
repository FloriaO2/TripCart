package com.example.tripcart.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * ListProductEntity - 리스트에 포함된 상품 정보를 저장하는 테이블
 * - Firestore의 lists/{listId}/products 서브컬렉션과 동일한 구조
 * - Firestore의 lists/{listId}/products/{productId} 이런 식으로 구현
 * - RoomDB에서는 id, listId 조합을 복합 주요키로 사용!
 */
@Entity(
    tableName = "list_products",
    primaryKeys = ["id", "listId"],
    foreignKeys = [
        ForeignKey(
            entity = ListEntity::class,         // 참조하는 부모 테이블
            parentColumns = ["listId"],         // 부모 테이블의 컬럼
            childColumns = ["listId"],          // 이 테이블의 컬럼
            onDelete = ForeignKey.CASCADE       // 리스트 삭제 시 리스트 내 상품도 자동 삭제
        )
    ],
    // 조회 성능 향상 - listId 컬럼에 별도 인덱스 추가
    indices = [Index(value = ["listId"])]
)
data class ListProductEntity(
    val id: String,                    // 상품 고유 ID
    val listId: String,                // 리스트 ID
    val productId: String?,            // Firestore products 컬렉션의 상품 ID (랭킹 반영용, null이면 사용자 생성 상품)
    val productName: String,           // 상품 이름
    val category: String,              // 상품 카테고리
    val imageUrls: List<String>? = null,  // 상품 이미지 URL 리스트 (선택사항)
    val quantity: Int,                // 구매 수량
    val note: String? = null,         // 메모 (선택사항)
    val bought: String = "구매전"      // 구매 상태: "구매전", "구매중", "구매완료"
)

