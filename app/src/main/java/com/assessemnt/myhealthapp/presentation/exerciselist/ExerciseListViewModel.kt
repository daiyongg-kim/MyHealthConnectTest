package com.assessemnt.myhealthapp.presentation.exerciselist

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.assessemnt.myhealthapp.data.healthconnect.HealthConnectManager
import com.assessemnt.myhealthapp.data.local.ExerciseDatabase
import com.assessemnt.myhealthapp.data.repository.ExerciseRepository
import com.assessemnt.myhealthapp.domain.model.DataSource
import com.assessemnt.myhealthapp.domain.model.Exercise
import com.assessemnt.myhealthapp.domain.model.ExerciseConflict
import com.assessemnt.myhealthapp.presentation.conflictlist.ConflictListViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.abs

class ExerciseListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExerciseRepository
    private val healthConnectManager = HealthConnectManager(application)

    init {
        val database = ExerciseDatabase.getDatabase(application)
        repository = ExerciseRepository(database.exerciseDao())
    }

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _needsPermission = MutableStateFlow(false)
    val needsPermission: StateFlow<Boolean> = _needsPermission.asStateFlow()

    private val _showConflictDialog = MutableStateFlow(false)
    val showConflictDialog: StateFlow<Boolean> = _showConflictDialog.asStateFlow()

    private val _conflictingExercises = MutableStateFlow<List<Exercise>>(emptyList())
    val conflictingExercises: StateFlow<List<Exercise>> = _conflictingExercises.asStateFlow()

    init {
        loadExercises()
    }

    private fun loadExercises() {
        viewModelScope.launch {
            repository.getAllExercises().collect { exercises ->
                _exercises.value = exercises
                Log.d("ExerciseList", "=== Loaded ${exercises.size} exercises from database ===")
            }
        }
    }

    fun refreshExercises() {
        viewModelScope.launch {
            val currentExercises = _exercises.value
            checkForConflicts(currentExercises)
        }
    }

    private fun checkForConflicts(exercises: List<Exercise>) {
        Log.d("ExerciseList", "=== Checking conflicts for ${exercises.size} exercises ===")

        val conflictGroups = mutableListOf<List<Exercise>>()
        val processedIds = mutableSetOf<String>()

        for (exercise in exercises) {
            if (processedIds.contains(exercise.id)) continue

            // Find all exercises that overlap with this one
            val group = mutableListOf(exercise)
            val toProcess = mutableListOf(exercise)
            val groupIds = mutableSetOf(exercise.id)

            while (toProcess.isNotEmpty()) {
                val current = toProcess.removeAt(0)

                // Find exercises that overlap with current exercise
                val overlapping = exercises.filter { other ->
                    other.id != current.id &&
                    !groupIds.contains(other.id) &&
                    hasTimeOverlap(current, other)
                }

                overlapping.forEach { overlap ->
                    group.add(overlap)
                    groupIds.add(overlap.id)
                    toProcess.add(overlap)
                }
            }

            if (group.size > 1) {
                Log.d("ExerciseList", "Found conflict group with ${group.size} exercises")
                conflictGroups.add(group)
                processedIds.addAll(groupIds)
            } else {
                processedIds.add(exercise.id)
            }
        }

        if (conflictGroups.isNotEmpty()) {
            // Show the first conflict group
            val firstGroup = conflictGroups.first()
            Log.d("ExerciseList", "=== Showing conflict dialog for first group with ${firstGroup.size} exercises ===")
            _conflictingExercises.value = firstGroup
            _showConflictDialog.value = true
        } else {
            Log.d("ExerciseList", "=== No conflicts detected ===")
        }
    }

    fun dismissConflictDialog() {
        _showConflictDialog.value = false
        _conflictingExercises.value = emptyList()
    }

    fun resolveConflicts(selectedExerciseId: String) {
        viewModelScope.launch {
            val conflictIds = _conflictingExercises.value.map { it.id }
            val idsToDelete = conflictIds.filter { it != selectedExerciseId }
            repository.deleteExercisesByIds(idsToDelete)

            _showConflictDialog.value = false
            _conflictingExercises.value = emptyList()

            // After resolving, check if there are more conflicts
            val currentExercises = _exercises.value
            checkForConflicts(currentExercises)
        }
    }

    fun onSyncClicked() {
        viewModelScope.launch {
            // Check if Health Connect is available
            if (!healthConnectManager.isAvailable()) {
                return@launch
            }

            // Check permissions
            if (!healthConnectManager.hasAllPermissions()) {
                _needsPermission.value = true
                return@launch
            }

            // If permissions granted, fetch data
            _isLoading.value = true
            try {
                val endTime = Instant.now()
                val startTime = endTime.minus(7, ChronoUnit.DAYS)
                val healthConnectExercises = healthConnectManager.readExercises(startTime, endTime)

                Log.d("ExerciseList", "=== Sync: Fetched ${healthConnectExercises.size} from Health Connect ===")

                // Get current exercises from database
                val currentExercises = _exercises.value

                // Combine all exercises
                val allExercises = currentExercises + healthConnectExercises
                Log.d("ExerciseList", "=== Before dedup: Current=${currentExercises.size}, New=${healthConnectExercises.size} ===")

                // Remove duplicates: prefer Health Connect over Manual
                val uniqueExercises = removeDuplicates(allExercises)
                Log.d("ExerciseList", "=== After dedup: ${uniqueExercises.size} exercises ===")

                // Save all unique exercises to database
                repository.insertExercises(uniqueExercises)

                // Check for conflicts in the combined list
                checkForConflicts(uniqueExercises)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun removeDuplicates(exercises: List<Exercise>): List<Exercise> {
        val result = mutableListOf<Exercise>()
        val processedIds = mutableSetOf<String>()

        for (exercise in exercises) {
            if (processedIds.contains(exercise.id)) continue

            // Find duplicates (same type, within 5 minutes)
            val duplicates = exercises.filter { other ->
                other.id != exercise.id &&
                other.type == exercise.type &&
                isSameTime(exercise, other, thresholdMinutes = 5)
            }

            if (duplicates.isEmpty()) {
                result.add(exercise)
                processedIds.add(exercise.id)
            } else {
                // Log duplicates found
                Log.d("ExerciseList", "=== Found ${duplicates.size} duplicates for: ${exercise.type} at ${exercise.startTime} ===")
                Log.d("ExerciseList", "  Original: ${exercise.source.displayName()} | ${exercise.durationMinutes}min | ID: ${exercise.id}")
                duplicates.forEach { dup ->
                    Log.d("ExerciseList", "  Duplicate: ${dup.source.displayName()} | ${dup.durationMinutes}min | ID: ${dup.id}")
                }

                // Prefer Health Connect over Manual
                val allSimilar = listOf(exercise) + duplicates
                val preferred = allSimilar.firstOrNull { it.source != DataSource.MANUAL }
                    ?: exercise

                Log.d("ExerciseList", "  → Keeping: ${preferred.source.displayName()} | ${preferred.durationMinutes}min | ID: ${preferred.id}")

                result.add(preferred)
                processedIds.addAll(allSimilar.map { it.id })
            }
        }

        return result.sortedByDescending { it.startTime.toString() }
    }

    private fun isSameTime(e1: Exercise, e2: Exercise, thresholdMinutes: Int): Boolean {
        val e1Time = e1.startTime.toString()
        val e2Time = e2.startTime.toString()

        // Check same date first (yyyy-MM-dd)
        val e1Date = e1Time.split("T").getOrNull(0)
        val e2Date = e2Time.split("T").getOrNull(0)

        if (e1Date != e2Date) {
            Log.d("ExerciseList", "Different dates: $e1Date vs $e2Date")
            return false
        }

        // Extract hour and minute from ISO format (e.g., "2025-10-05T14:30:00")
        val e1Parts = e1Time.split("T").getOrNull(1)?.split(":")
        val e2Parts = e2Time.split("T").getOrNull(1)?.split(":")

        if (e1Parts == null || e2Parts == null) return false

        val e1Hour = e1Parts.getOrNull(0)?.toIntOrNull() ?: return false
        val e1Minute = e1Parts.getOrNull(1)?.toIntOrNull() ?: return false
        val e2Hour = e2Parts.getOrNull(0)?.toIntOrNull() ?: return false
        val e2Minute = e2Parts.getOrNull(1)?.toIntOrNull() ?: return false

        val e1TotalMinutes = e1Hour * 60 + e1Minute
        val e2TotalMinutes = e2Hour * 60 + e2Minute
        val diff = abs(e1TotalMinutes - e2TotalMinutes)

        Log.d("ExerciseList", "Time comparison: $e1Hour:$e1Minute vs $e2Hour:$e2Minute, diff=$diff minutes")

        return diff <= thresholdMinutes
    }

    fun onPermissionHandled() {
        _needsPermission.value = false
    }

    fun deleteExercise(exerciseId: String) {
        viewModelScope.launch {
            repository.deleteExerciseById(exerciseId)
        }
    }

    private fun hasTimeOverlap(e1: Exercise, e2: Exercise): Boolean {
        val e1Start = toMinutes(e1.startTime)
        val e1End = e1Start + e1.durationMinutes
        val e2Start = toMinutes(e2.startTime)
        val e2End = e2Start + e2.durationMinutes
        return e1Start < e2End && e2Start < e1End
    }

    @Suppress("DEPRECATION")
    private fun toMinutes(time: kotlinx.datetime.LocalDateTime): Long {
        return time.year * 525600L + time.monthNumber * 43800L +
                time.dayOfMonth * 1440L + time.hour * 60L + time.minute
    }

}