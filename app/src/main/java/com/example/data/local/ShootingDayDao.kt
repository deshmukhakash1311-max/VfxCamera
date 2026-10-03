package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ShootingDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShootingDayDao {
    @Query("SELECT * FROM shooting_days WHERE projectId = :projectId ORDER BY dayNumber ASC")
    fun getShootingDaysForProject(projectId: String): Flow<List<ShootingDayEntity>>

    @Query("SELECT * FROM shooting_days WHERE projectId = :projectId ORDER BY dayNumber ASC")
    suspend fun getShootingDaysListForProject(projectId: String): List<ShootingDayEntity>

    @Query("SELECT * FROM shooting_days WHERE id = :id")
    suspend fun getShootingDayById(id: String): ShootingDayEntity?

    @Query("SELECT * FROM shooting_days WHERE id = :id")
    fun observeShootingDayById(id: String): Flow<ShootingDayEntity?>

    @Query("SELECT MAX(dayNumber) FROM shooting_days WHERE projectId = :projectId")
    suspend fun getMaxDayNumber(projectId: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShootingDay(shootingDay: ShootingDayEntity)

    @Update
    suspend fun updateShootingDay(shootingDay: ShootingDayEntity)

    @Delete
    suspend fun deleteShootingDay(shootingDay: ShootingDayEntity)

    @Query("SELECT COUNT(*) FROM shooting_days WHERE projectId = :projectId")
    suspend fun getDayCountForProject(projectId: String): Int
}
