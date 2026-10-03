package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "exported_reports")
data class ExportedReportEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val projectName: String,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val pageCount: Int,
    val shotCount: Int,
    val layoutType: String,
    val createdAt: Long = System.currentTimeMillis()
)
