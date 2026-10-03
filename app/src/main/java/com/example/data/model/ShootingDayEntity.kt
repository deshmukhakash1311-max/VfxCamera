package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "shooting_days",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"]), Index(value = ["projectId", "dayNumber"], unique = true)]
)
data class ShootingDayEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val dayNumber: Int,
    val date: String,
    val location: String = "",
    val dayNotes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // Day level camera overrides (null means inherit from Project)
    @Embedded(prefix = "day_cam_") val cameraOverrides: CameraOverrides = CameraOverrides(),

    // Day level VFX overrides (null means inherit from Project)
    @Embedded(prefix = "day_vfx_") val vfxOverrides: VfxOverrides = VfxOverrides()
)
