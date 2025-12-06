package com.example.tripcart.data.local.dao

import androidx.room.*
import com.example.tripcart.data.local.entity.PlaceEntity
import kotlinx.coroutines.flow.Flow

/*
 * PlaceDao - PlaceEntity에 대한 데이터 접근 인터페이스
 * 
 * 역할:
 * PlaceEntity(장소 테이블)에 대한 CRUD 작업을 정의
 * Room이 이 인터페이스를 구현체로 자동 생성
 * 
 * Flow를 사용해, Flow 반환하는 메서드는 데이터 변경 시 자동으로 UI에 알림
 * -> 실시간 데이터 업데이트 가능!
 */
@Dao
interface PlaceDao {
    // 모든 장소 조회 (Flow 이용해서 실시간 업데이트)
    @Query("SELECT * FROM places")
    fun getAllPlaces(): Flow<List<PlaceEntity>>
    
    // 특정 ID의 장소 조회
    @Query("SELECT * FROM places WHERE placeId = :placeId")
    suspend fun getPlaceById(placeId: String): PlaceEntity?
    
    // 장소 저장
    // placeId 기준으로 이미 있는 데이터면 무시, 없는 데이터면 추가 (중복 방지!)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlace(place: PlaceEntity)
    
    // 장소 업데이트
    @Update
    suspend fun updatePlace(place: PlaceEntity)
    
    // 장소 삭제 (ID 이용)
    @Query("DELETE FROM places WHERE placeId = :placeId")
    suspend fun deletePlaceById(placeId: String)
    
    // 특정 리스트 ID를 참조하는 모든 장소 조회 (백그라운드 위치 추적용)
    @Query("SELECT * FROM places WHERE listId LIKE '%' || :listId || '%'")
    suspend fun getPlacesByListId(listId: String): List<PlaceEntity>
}

