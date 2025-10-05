package com.assessemnt.myhealthapp.presentation.exerciselist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.assessemnt.myhealthapp.data.healthconnect.HealthConnectManager
import com.assessemnt.myhealthapp.domain.model.Exercise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

class ExerciseListViewModel(application: Application) : AndroidViewModel(application) {

    private val healthConnectManager = HealthConnectManager(application)

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _needsPermission = MutableStateFlow(false)
    val needsPermission: StateFlow<Boolean> = _needsPermission.asStateFlow()

    init {
        loadExercises()
    }

    private fun loadExercises() {
        viewModelScope.launch {
            // Check if Health Connect is available
            if (!healthConnectManager.isAvailable()) {
                return@launch
            }

            // Check permissions
            if (!healthConnectManager.hasAllPermissions()) {
                return@launch
            }

            _isLoading.value = true
            try {
                // Read last 7 days of exercises
                val endTime = Instant.now()
                val startTime = endTime.minus(7, ChronoUnit.DAYS)

                val exerciseList = healthConnectManager.readExercises(startTime, endTime)
                _exercises.value = exerciseList
            } finally {
                _isLoading.value = false
            }
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
                val exerciseList = healthConnectManager.readExercises(startTime, endTime)
                _exercises.value = exerciseList
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onPermissionHandled() {
        _needsPermission.value = false
    }

    fun deleteExercise(exerciseId: String) {
        _exercises.value = _exercises.value.filter { it.id != exerciseId }
    }
}