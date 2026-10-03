package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.CaptureEntity
import com.example.data.model.ExportedReportEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ShootingDayEntity

@Database(
    entities = [
        ProjectEntity::class,
        ShootingDayEntity::class,
        CaptureEntity::class,
        ExportedReportEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VfxDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun shootingDayDao(): ShootingDayDao
    abstract fun captureDao(): CaptureDao
    abstract fun exportedReportDao(): ExportedReportDao

    companion object {
        @Volatile
        private var INSTANCE: VfxDatabase? = null

        fun getDatabase(context: Context): VfxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VfxDatabase::class.java,
                    "vfx_capture_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
