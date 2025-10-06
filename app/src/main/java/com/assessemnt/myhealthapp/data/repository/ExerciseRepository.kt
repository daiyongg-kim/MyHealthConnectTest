package com.assessemnt.myhealthapp.data.repository

import com.assessemnt.myhealthapp.data.local.ExerciseDao
import com.assessemnt.myhealthapp.data.local.ExerciseEntity
import com.assessemnt.myhealthapp.domain.model.Exercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExerciseRepository(private val dao: ExerciseDao) {

    fun getAllExercises(): Flow<List<Exercise>> {
        return dao.getAllExercises().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getExerciseById(id: String): Exercise? {
        return dao.getExerciseById(id)?.toDomain()
    }

    suspend fun insertExercise(exercise: Exercise) {
        dao.insertExercise(ExerciseEntity.fromDomain(exercise))
    }

    suspend fun insertExercises(exercises: List<Exercise>) {
        dao.insertExercises(exercises.map { ExerciseEntity.fromDomain(it) })
    }

    suspend fun deleteExercise(exercise: Exercise) {
        dao.deleteExerciseById(exercise.id)
    }

    suspend fun deleteExerciseById(id: String) {
        dao.deleteExerciseById(id)
    }

    suspend fun deleteExercisesByIds(ids: List<String>) {
        dao.deleteExercisesByIds(ids)
    }

    suspend fun deleteAllExercises() {
        dao.deleteAllExercises()
    }
}