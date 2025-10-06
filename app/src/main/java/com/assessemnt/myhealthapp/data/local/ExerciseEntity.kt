package com.assessemnt.myhealthapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.assessemnt.myhealthapp.domain.model.DataSource
import com.assessemnt.myhealthapp.domain.model.Exercise

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val startTime: String,          // Store as ISO string
    val durationMinutes: Int,
    val source: String,             // Store as string
    val distance: Double? = null,
    val calories: Int? = null,
    val notes: String? = null
) {
    fun toDomain(): Exercise {
        return Exercise(
            id = id,
            type = type,
            startTime = kotlinx.datetime.LocalDateTime.parse(startTime),
            durationMinutes = durationMinutes,
            source = DataSource.valueOf(source),
            distance = distance,
            calories = calories,
            notes = notes
        )
    }

    companion object {
        fun fromDomain(exercise: Exercise): ExerciseEntity {
            return ExerciseEntity(
                id = exercise.id,
                type = exercise.type,
                startTime = exercise.startTime.toString(),
                durationMinutes = exercise.durationMinutes,
                source = exercise.source.name,
                distance = exercise.distance,
                calories = exercise.calories,
                notes = exercise.notes
            )
        }
    }
}
