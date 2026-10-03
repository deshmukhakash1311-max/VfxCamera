package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val client: String = "",
    val productionCompany: String = "",
    val productionShow: String = "",
    val projectIdCode: String = "",
    val description: String = "",
    val director: String = "",
    val vfxSupervisor: String = "",
    val vfxProducer: String = "",
    val cameraOperator: String = "",
    val date: String = "",
    val location: String = "",
    val coverImagePath: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    @Embedded val cameraDefaults: CameraSettings = CameraSettings(),
    @Embedded val vfxDefaults: VfxSettings = VfxSettings()
)
