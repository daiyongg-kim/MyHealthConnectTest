package com.assessemnt.myhealthapp.data.healthconnect

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.assessemnt.myhealthapp.domain.model.DataSource
import com.assessemnt.myhealthapp.domain.model.Exercise
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.Duration

@OptIn(kotlin.time.ExperimentalTime::class)
class HealthConnectManager(private val context: Context) {

    companion object {
        private const val TAG = "HealthConnectManager"

        // All the permissions we need for reading/writing exercise data
        val PERMISSIONS = setOf(
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getWritePermission(DistanceRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class)
        )
    }

    val healthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    // Check if Health Connect is installed and available on this device
    fun isAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    // Returns true if user has granted all permissions we need
    suspend fun hasAllPermissions(): Boolean {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        return granted.containsAll(PERMISSIONS)
    }

    // Get the permission controller for requesting permissions
    fun getPermissionController(): PermissionController {
        return healthConnectClient.permissionController
    }

    // Reads all exercises from apps like Samsung Health, Garmin, etc.
    suspend fun readExercises(
        startTime: java.time.Instant,
        endTime: java.time.Instant
    ): List<Exercise> {
        Log.d(TAG, "Reading exercises from $startTime to $endTime")

        return try {
            val request = ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )

            val response = healthConnectClient.readRecords(request)
            Log.d(TAG, "Found ${response.records.size} exercises")

            // Convert Health Connect records to our Exercise model
            response.records.map { record ->
                val source = mapDataSource(record.metadata.dataOrigin.packageName)
                val exerciseType = mapExerciseType(record.exerciseType)

                Log.d(TAG, "Exercise: $exerciseType from $source")

                // Convert java.time.Instant to kotlinx LocalDateTime via epoch millis
                val epochMillis = record.startTime.toEpochMilli()
                val startInstant = kotlinx.datetime.Instant.fromEpochMilliseconds(epochMillis)

                Exercise(
                    id = record.metadata.id,
                    type = exerciseType,
                    startTime = startInstant.toLocalDateTime(TimeZone.currentSystemDefault()),
                    durationMinutes = Duration.between(record.startTime, record.endTime).toMinutes().toInt(),
                    source = source,
                    distance = null,
                    calories = null,
                    notes = record.title
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading exercises: ${e.message}")
            emptyList()
        }
    }

    // Write function - will implement later with proper SDK usage
    suspend fun writeExercise(exercise: Exercise): Boolean {
        Log.d(TAG, "Write exercise not yet implemented for SDK 1.1.0-rc03")
        return false
        // TODO: Implement write using the new SDK's public APIs
    }

    // Figure out which app the data came from based on package name
    private fun mapDataSource(packageName: String): DataSource {
        return when {
            packageName.contains("samsung", ignoreCase = true) -> DataSource.HEALTH_CONNECT_SAMSUNG
            packageName.contains("garmin", ignoreCase = true) -> DataSource.HEALTH_CONNECT_GARMIN
            else -> DataSource.HEALTH_CONNECT_OTHER
        }
    }

    // Convert Health Connect's exercise type numbers to readable strings
    private fun mapExerciseType(healthConnectType: Int): String {
        return when (healthConnectType) {
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "Running"
            ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Walking"
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> "Swimming"
            ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "Yoga"
            ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "Hiking"
            else -> "Other"
        }
    }

    // Convert our exercise type strings back to Health Connect's numbers
    private fun mapToHealthConnectType(exerciseType: String): Int {
        return when (exerciseType.lowercase()) {
            "running" -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
            "walking" -> ExerciseSessionRecord.EXERCISE_TYPE_WALKING
            "swimming" -> ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL
            "yoga" -> ExerciseSessionRecord.EXERCISE_TYPE_YOGA
            "hiking" -> ExerciseSessionRecord.EXERCISE_TYPE_HIKING
            else -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
        }
    }
}