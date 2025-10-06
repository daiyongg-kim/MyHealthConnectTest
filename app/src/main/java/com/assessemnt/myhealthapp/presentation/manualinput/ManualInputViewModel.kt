package com.assessemnt.myhealthapp.presentation.manualinput

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.assessemnt.myhealthapp.domain.model.DataSource
import com.assessemnt.myhealthapp.domain.model.Exercise
import com.assessemnt.myhealthapp.presentation.exerciselist.ExerciseListViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ManualInputState(
    val exerciseType: String = "",
    val durationMinutes: String = "",
    val startTime: kotlinx.datetime.LocalDateTime? = null,
    val distance: String = "",
    val calories: String = "",
    val notes: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val exerciseTypeError: String? = null,
    val durationError: String? = null,
    val startTimeError: String? = null,
    val distanceError: String? = null,
    val caloriesError: String? = null,
    val showConflictDialog: Boolean = false,
    val conflictingExercise: Exercise? = null
)

class ManualInputViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ManualInputState(
        startTime = getCurrentDateTime()
    ))
    val state: StateFlow<ManualInputState> = _state.asStateFlow()

    private fun getCurrentDateTime(): kotlinx.datetime.LocalDateTime {
        val now = java.time.LocalDateTime.now()
        return kotlinx.datetime.LocalDateTime(
            now.year, now.monthValue, now.dayOfMonth,
            now.hour, now.minute
        )
    }

    fun updateExerciseType(type: String) {
        _state.update {
            it.copy(
                exerciseType = type,
                exerciseTypeError = if (type.isBlank()) "Exercise type is required" else null
            )
        }
    }

    fun updateDuration(duration: String) {
        _state.update {
            it.copy(
                durationMinutes = duration,
                durationError = if (duration.isBlank()) "Duration is required" else null
            )
        }
    }

    fun updateStartTime(startTime: kotlinx.datetime.LocalDateTime) {
        _state.update {
            it.copy(
                startTime = startTime,
                startTimeError = null
            )
        }
    }

    fun updateDistance(distance: String) {
        _state.update {
            val error = if (distance.isNotBlank() && distance.toDoubleOrNull() == null) {
                "Distance must be a valid number"
            } else null
            it.copy(distance = distance, distanceError = error)
        }
    }

    fun updateCalories(calories: String) {
        _state.update {
            val error = if (calories.isNotBlank() && calories.toIntOrNull() == null) {
                "Calories must be a valid number"
            } else null
            it.copy(calories = calories, caloriesError = error)
        }
    }

    fun updateNotes(notes: String) {
        _state.update { it.copy(notes = notes) }
    }

    fun dismissConflictDialog() {
        _state.update { it.copy(showConflictDialog = false, conflictingExercise = null) }
    }

    fun resolveConflictKeepNew() {
        // Keep the new exercise, discard the conflict
        _state.update { it.copy(showConflictDialog = false, conflictingExercise = null) }
        saveWithoutConflictCheck()
    }

    fun resolveConflictKeepExisting() {
        // Keep the existing exercise, discard the new one
        _state.update { it.copy(showConflictDialog = false, conflictingExercise = null, isSaving = false) }
    }

    fun saveExercise() {
        // Validate all fields
        val currentState = _state.value

        if (currentState.exerciseType.isBlank()) {
            _state.update { it.copy(exerciseTypeError = "Exercise type is required") }
            return
        }

        if (currentState.durationMinutes.isBlank()) {
            _state.update { it.copy(durationError = "Duration is required") }
            return
        }

        if (currentState.startTime == null) {
            _state.update { it.copy(startTimeError = "Start time is required") }
            return
        }

        if (currentState.exerciseTypeError != null ||
            currentState.durationError != null ||
            currentState.startTimeError != null ||
            currentState.distanceError != null ||
            currentState.caloriesError != null) {
            return
        }

        // Check for conflicts before saving
        val newExercise = Exercise(
            id = UUID.randomUUID().toString(),
            type = currentState.exerciseType,
            startTime = currentState.startTime,
            durationMinutes = currentState.durationMinutes.toInt(),
            source = DataSource.MANUAL,
            distance = currentState.distance.toDoubleOrNull(),
            calories = currentState.calories.toIntOrNull(),
            notes = currentState.notes.ifBlank { null }
        )

        // Check for time overlap with existing manual exercises
        val conflicting = ExerciseListViewModel.manualExercises.find { existing ->
            hasTimeOverlap(existing, newExercise)
        }

        if (conflicting != null) {
            _state.update {
                it.copy(
                    isSaving = false,
                    showConflictDialog = true,
                    conflictingExercise = conflicting
                )
            }
        } else {
            saveWithoutConflictCheck()
        }
    }

    private fun saveWithoutConflictCheck() {
        val currentState = _state.value

        _state.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            try {
                val exercise = Exercise(
                    id = UUID.randomUUID().toString(),
                    type = currentState.exerciseType,
                    startTime = currentState.startTime!!,
                    durationMinutes = currentState.durationMinutes.toInt(),
                    source = DataSource.MANUAL,
                    distance = currentState.distance.toDoubleOrNull(),
                    calories = currentState.calories.toIntOrNull(),
                    notes = currentState.notes.ifBlank { null }
                )

                // Save to shared storage
                ExerciseListViewModel.manualExercises.add(exercise)

                _state.update {
                    it.copy(isSaving = false, isSaved = true)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: "Failed to save exercise"
                    )
                }
            }
        }
    }

    private fun hasTimeOverlap(e1: Exercise, e2: Exercise): Boolean {
        // Calculate end times
        val e1Start = toMinutes(e1.startTime)
        val e1End = e1Start + e1.durationMinutes

        val e2Start = toMinutes(e2.startTime)
        val e2End = e2Start + e2.durationMinutes

        // Check for overlap: e1 starts before e2 ends AND e2 starts before e1 ends
        return e1Start < e2End && e2Start < e1End
    }

    private fun toMinutes(time: kotlinx.datetime.LocalDateTime): Long {
        return time.year * 525600L + time.monthNumber * 43800L +
                time.dayOfMonth * 1440L + time.hour * 60L + time.minute
    }
}