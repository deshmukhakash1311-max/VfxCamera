package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CaptureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureDao {
    @Query("SELECT * FROM captures WHERE shootingDayId = :dayId AND isDeleted = 0 ORDER BY shotSequenceNumber ASC")
    fun getCapturesForDay(dayId: String): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE shootingDayId = :dayId AND isDeleted = 0 ORDER BY shotSequenceNumber ASC")
    suspend fun getCapturesListForDay(dayId: String): List<CaptureEntity>

    @Query("SELECT * FROM captures WHERE projectId = :projectId AND isDeleted = 0 ORDER BY createdAt ASC")
    fun getCapturesForProject(projectId: String): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE projectId = :projectId AND isDeleted = 0 ORDER BY createdAt ASC")
    suspend fun getCapturesListForProject(projectId: String): List<CaptureEntity>

    @Query("SELECT * FROM captures WHERE id = :id")
    suspend fun getCaptureById(id: String): CaptureEntity?

    @Query("SELECT * FROM captures WHERE id = :id")
    fun observeCaptureById(id: String): Flow<CaptureEntity?>

    @Query("SELECT MAX(shotSequenceNumber) FROM captures WHERE shootingDayId = :dayId")
    suspend fun getMaxSequenceNumberForDay(dayId: String): Int?

    @Query("SELECT COUNT(*) FROM captures WHERE projectId = :projectId AND isDeleted = 0")
    fun observeCaptureCountForProject(projectId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM captures WHERE projectId = :projectId AND isDeleted = 0")
    suspend fun getCaptureCountForProject(projectId: String): Int

    @Query("SELECT COUNT(*) FROM captures WHERE shootingDayId = :dayId AND isDeleted = 0")
    suspend fun getCaptureCountForDay(dayId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapture(capture: CaptureEntity)

    @Update
    suspend fun updateCapture(capture: CaptureEntity)

    @Delete
    suspend fun deleteCapture(capture: CaptureEntity)

    @Query("UPDATE captures SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteCapture(id: String)

    @Query("UPDATE captures SET isReference = :isRef WHERE id = :id")
    suspend fun setReferenceStatus(id: String, isRef: Boolean)
}
