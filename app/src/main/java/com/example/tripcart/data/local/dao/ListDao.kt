package com.example.tripcart.data.local.dao

import androidx.room.*
import com.example.tripcart.data.local.entity.ListEntity
import kotlinx.coroutines.flow.Flow

/*
 * ListDao - ListEntity에 대한 데이터 접근 인터페이스
 * 
 * 역할:
 * ListEntity(리스트 테이블)에 대한 CRUD 작업을 정의
 * Room이 이 인터페이스를 구현체로 자동 생성
 * 
 * Flow를 사용해, Flow 반환하는 메서드는 데이터 변경 시 자동으로 UI에 알림
 * -> 실시간 데이터 업데이트 가능!
 */
@Dao
interface ListDao {
    // 리스트 생성순 조회 (Flow 이용해서 실시간 업데이트)
    // 최신 생성순(내림차순)으로 정렬: ORDER BY createdAt DESC
    @Query("SELECT * FROM lists ORDER BY createdAt DESC")
    fun getAllLists(): Flow<List<ListEntity>>
    
    // 특정 ID의 리스트 조회
    @Query("SELECT * FROM lists WHERE listId = :listId")
    suspend fun getListById(listId: String): ListEntity?
    
    // 리스트 저장 (같은 키일 경우 덮어쓰기! 덮어쓸 땐 DELETE + INSERT)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: ListEntity)
    
    // 리스트 업데이트 (UPDATE만 수행)
    @Update
    suspend fun updateList(list: ListEntity)
    
    // 리스트 삭제 (ID 이용)
    @Query("DELETE FROM lists WHERE listId = :listId")
    suspend fun deleteListById(listId: String)
    
    // 특정 국가의 리스트 조회 (Flow 이용해서 실시간 업데이트)
    @Query("SELECT * FROM lists WHERE country = :country OR country IS NULL")
    fun getListsByCountry(country: String?): Flow<List<ListEntity>>
}

