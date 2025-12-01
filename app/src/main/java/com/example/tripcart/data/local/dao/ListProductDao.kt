package com.example.tripcart.data.local.dao

import androidx.room.*
import com.example.tripcart.data.local.entity.ListProductEntity
import kotlinx.coroutines.flow.Flow

/**
 * ListProductDao - ListProductEntity에 대한 데이터 접근 인터페이스
 * 
 * 역할:
 * ListProductEntity(상품 정보 테이블)에 대한 CRUD 작업을 정의
 * Room이 이 인터페이스를 구현체로 자동 생성
 * 
 * Flow를 사용해, Flow 반환하는 메서드는 데이터 변경 시 자동으로 UI에 알림
 * -> 실시간 데이터 업데이트 가능!
 */
@Dao
interface ListProductDao {
    // 특정 리스트에 속한 모든 상품 조회 (Flow 이용해서 실시간 업데이트)
    @Query("SELECT * FROM list_products WHERE listId = :listId")
    fun getProductsByListId(listId: String): Flow<List<ListProductEntity>>
    
    // 상품 저장 (중복 시 덮어쓰기 -> 업데이트도 이 메서드로 처리)
    // 상품 업데이트 시:
    // 1. getProductsByListId()로 기존 데이터 불러오기
    // 2. .copy()로 원하는 필드만 수정
    // 3. insertProduct()로 저장 (REPLACE 전략으로 자동 덮어쓰기)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ListProductEntity)
    
    // 상품 삭제
    @Delete
    suspend fun deleteProduct(product: ListProductEntity)
    
    // 특정 리스트의 모든 상품 삭제 (리스트 삭제 시 사용)
    @Query("DELETE FROM list_products WHERE listId = :listId")
    suspend fun deleteProductsByListId(listId: String)
}

