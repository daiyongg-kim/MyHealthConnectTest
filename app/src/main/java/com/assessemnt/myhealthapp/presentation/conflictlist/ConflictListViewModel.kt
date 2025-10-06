package com.assessemnt.myhealthapp.presentation.conflictlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.assessemnt.myhealthapp.domain.model.ExerciseConflict
import com.assessemnt.myhealthapp.presentation.exerciselist.ExerciseListViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConflictListViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        val detectedConflicts = mutableListOf<ExerciseConflict>()
    }

    private val _conflicts = MutableStateFlow<List<ExerciseConflict>>(emptyList())
    val conflicts: StateFlow<List<ExerciseConflict>> = _conflicts.asStateFlow()

    init {
        loadConflicts()
    }

    private fun loadConflicts() {
        _conflicts.value = detectedConflicts.filter { !it.isResolved }
    }

    fun resolveConflict(conflictId: String, selectedExerciseId: String) {
        val conflict = detectedConflicts.find { it.id == conflictId } ?: return

        // Remove the non-selected exercise from manual exercises
        val exerciseToRemove = if (selectedExerciseId == conflict.exercise1.id) {
            conflict.exercise2
        } else {
            conflict.exercise1
        }

        ExerciseListViewModel.manualExercises.removeAll { it.id == exerciseToRemove.id }

        // Mark conflict as resolved and remove from list
        detectedConflicts.removeAll { it.id == conflictId }

        // Reload conflicts
        loadConflicts()
    }
}