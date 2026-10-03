package com.example.data.model

enum class InheritanceSource {
    PROJECT_DEFAULT,
    DAY_OVERRIDE,
    SHOT_OVERRIDE,
    ORIGINAL_CAMERA
}

data class ResolvedField(
    val key: String,
    val label: String,
    val value: String,
    val source: InheritanceSource,
    val sourceDescription: String,
    val originalCameraValue: String? = null,
    val isOverridden: Boolean = (source == InheritanceSource.DAY_OVERRIDE || source == InheritanceSource.SHOT_OVERRIDE)
)
