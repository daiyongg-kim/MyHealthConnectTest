package com.assessemnt.myhealthapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.health.connect.client.PermissionController
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.assessemnt.myhealthapp.data.healthconnect.HealthConnectManager
import com.assessemnt.myhealthapp.domain.model.Exercise
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

class MainActivity : ComponentActivity() {

    private lateinit var healthConnectManager: HealthConnectManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        healthConnectManager = HealthConnectManager(this)

        setContent {
            MaterialTheme {
                HealthConnectTestScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun HealthConnectTestScreen() {
        var exercises by remember { mutableStateOf<List<Exercise>>(emptyList()) }
        var isLoading by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf("Ready to test Health Connect") }

        // Permission launcher
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = PermissionController.createRequestPermissionResultContract()
        ) { granted ->
            lifecycleScope.launch {
                if (granted.containsAll(HealthConnectManager.PERMISSIONS)) {
                    message = "Permissions granted! Reading exercises..."
                    readExercises { result ->
                        exercises = result
                        message = "Found ${result.size} exercises"
                        isLoading = false
                    }
                } else {
                    message = "Permissions denied"
                    isLoading = false
                    Toast.makeText(
                        this@MainActivity,
                        "Health Connect permissions required",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Health Connect Test") }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Button(
                    onClick = {
                        isLoading = true
                        lifecycleScope.launch {
                            checkPermissionsAndRead(
                                onNeedPermission = { permissionLauncher.launch(HealthConnectManager.PERMISSIONS) },
                                onSuccess = { result ->
                                    exercises = result
                                    message = "Found ${result.size} exercises"
                                    isLoading = false
                                }
                            )
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text(if (isLoading) "Loading..." else "Read Health Connect Data")
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isLoading) {
                    CircularProgressIndicator()
                }

                // Exercise list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(exercises) { exercise ->
                        ExerciseCard(exercise)
                    }
                }
            }
        }
    }

    @Composable
    fun ExerciseCard(exercise: Exercise) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = exercise.type,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Duration: ${exercise.durationMinutes} min",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Source: ${exercise.source.displayName()}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Time: ${exercise.startTime}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    private suspend fun checkPermissionsAndRead(
        onNeedPermission: () -> Unit,
        onSuccess: (List<Exercise>) -> Unit
    ) {
        // Check availability
        if (!healthConnectManager.isAvailable()) {
            return
        }

        // Check permissions
        if (healthConnectManager.hasAllPermissions()) {
            readExercises(onSuccess)
        } else {
            onNeedPermission()
        }
    }

    private suspend fun readExercises(onSuccess: (List<Exercise>) -> Unit) {
        // Read last 7 days
        val endTime = Instant.now()
        val startTime = endTime.minus(7, ChronoUnit.DAYS)

        val exercises = healthConnectManager.readExercises(startTime, endTime)
        onSuccess(exercises)
    }
}