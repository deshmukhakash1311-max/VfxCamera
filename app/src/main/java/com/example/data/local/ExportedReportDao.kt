package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ExportedReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExportedReportDao {
    @Query("SELECT * FROM exported_reports ORDER BY createdAt DESC")
    fun getAllReports(): Flow<List<ExportedReportEntity>>

    @Query("SELECT * FROM exported_reports WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getReportsForProject(projectId: String): Flow<List<ExportedReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ExportedReportEntity)

    @Delete
    suspend fun deleteReport(report: ExportedReportEntity)

    @Query("SELECT * FROM exported_reports WHERE id = :id")
    suspend fun getReportById(id: String): ExportedReportEntity?
}
