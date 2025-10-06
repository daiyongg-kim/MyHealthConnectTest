package com.assessemnt.myhealthapp.domain.model

import java.util.UUID

/**
 * Represents a conflict between two overlapping exercises
 */
data class ExerciseConflict(
    val id: String = UUID.randomUUID().toString(),
    val exercise1: Exercise,
    val exercise2: Exercise,
    val detectedAt: kotlinx.datetime.LocalDateTime = java.time.LocalDateTime.now().let {
        kotlinx.datetime.LocalDateTime(it.year, it.monthValue, it.dayOfMonth, it.hour, it.minute)
    },
    val isResolved: Boolean = false
)