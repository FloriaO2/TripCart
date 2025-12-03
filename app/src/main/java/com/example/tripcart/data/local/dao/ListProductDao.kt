package com.example.tripcart.data.local.dao

import androidx.room.*
import com.example.tripcart.data.local.entity.ListProductEntity
import kotlinx.coroutines.flow.Flow

/*
 * ListProductDao - ListProductEntity에 대한 데이터 접근 인터페이스
 * 
 * 역할:
 * ListProductEntity(상품 테이블)에 대한 CRUD 작업을 정의
 * Room이 이 인터페이스를 구현체로 자동 생성
 * 
 * Flow를 사용해, Flow 반환하는 메서드는 데이터 변경 시 자동으로 UI에 알림
 * -> 실시간 데이터 업데이트 가능!
 */
@Dao
interface ListProductDao {
    // 특정 리스트의 모든 상품 조회 (Flow 이용해서 실시간 업데이트)
    @Query("SELECT * FROM list_products WHERE listId = :listId ORDER BY id")
    fun getProductsByListId(listId: String): Flow<List<ListProductEntity>>
    
    // 특정 ID와 listId 조합으로 상품 조회
    @Query("SELECT * FROM list_products WHERE id = :id AND listId = :listId")
    suspend fun getProductById(id: String, listId: String): ListProductEntity?
    
    // 상품 저장
    // 복합 주요키 {id, listId} 기준으로 이미 있는 데이터면 무시, 없는 데이터면 추가 (중복 방지!)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProduct(product: ListProductEntity)
    
    // 상품 업데이트
    @Update
    suspend fun updateProduct(product: ListProductEntity)
    
    // 상품 삭제 (id와 listId 조합으로)
    @Query("DELETE FROM list_products WHERE id = :id AND listId = :listId")
    suspend fun deleteProductById(id: String, listId: String)
    
    // 특정 리스트의 모든 상품 삭제
    @Query("DELETE FROM list_products WHERE listId = :listId")
    suspend fun deleteProductsByListId(listId: String)
    
    // 특정 상품이 리스트에 이미 존재하는지 확인
    @Query("SELECT EXISTS(SELECT 1 FROM list_products WHERE id = :id AND listId = :listId)")
    suspend fun productExistsInList(id: String, listId: String): Boolean
}

