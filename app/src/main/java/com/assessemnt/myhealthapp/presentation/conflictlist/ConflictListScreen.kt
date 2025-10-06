package com.assessemnt.myhealthapp.presentation.conflictlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.assessemnt.myhealthapp.domain.model.DataSource
import com.assessemnt.myhealthapp.domain.model.Exercise
import com.assessemnt.myhealthapp.domain.model.ExerciseConflict

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConflictListScreen(
    onNavigateBack: () -> Unit,
    viewModel: ConflictListViewModel = viewModel()
) {
    val conflicts by viewModel.conflicts.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resolve Conflicts (${conflicts.size})") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            conflicts.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "All conflicts resolved!",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You can go back to the exercise list",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) {
                            Text("Back to List")
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(conflicts) { conflict ->
                        ConflictCard(
                            conflict = conflict,
                            onExerciseSelected = { selectedExerciseId ->
                                viewModel.resolveConflict(conflict.id, selectedExerciseId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConflictCard(
    conflict: ExerciseConflict,
    onExerciseSelected: (String) -> Unit
) {
    var selectedExerciseId by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Conflict Detected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "These exercises overlap in time. Choose which one to keep:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Exercise 1
            ExerciseOption(
                exercise = conflict.exercise1,
                isSelected = selectedExerciseId == conflict.exercise1.id,
                onSelected = {
                    selectedExerciseId = conflict.exercise1.id
                    onExerciseSelected(conflict.exercise1.id)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Exercise 2
            ExerciseOption(
                exercise = conflict.exercise2,
                isSelected = selectedExerciseId == conflict.exercise2.id,
                onSelected = {
                    selectedExerciseId = conflict.exercise2.id
                    onExerciseSelected(conflict.exercise2.id)
                }
            )
        }
    }
}

@Composable
private fun ExerciseOption(
    exercise: Exercise,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelected
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.type,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatTime(exercise),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = formatSource(exercise.source),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (exercise.distance != null || exercise.calories != null) {
                    Text(
                        text = buildDetails(exercise),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatTime(exercise: Exercise): String {
    val start = "${exercise.startTime.hour.toString().padStart(2, '0')}:${exercise.startTime.minute.toString().padStart(2, '0')}"
    return "$start · ${exercise.durationMinutes} min"
}

private fun formatSource(source: DataSource): String {
    return when (source) {
        DataSource.MANUAL -> "Manual Entry"
        DataSource.HEALTH_CONNECT_SAMSUNG -> "Samsung Health"
        DataSource.HEALTH_CONNECT_GARMIN -> "Garmin"
        DataSource.HEALTH_CONNECT_OTHER -> "Health Connect"
    }
}

private fun buildDetails(exercise: Exercise): String {
    val parts = mutableListOf<String>()
    exercise.distance?.let { parts.add("${String.format("%.1f", it)} km") }
    exercise.calories?.let { parts.add("$it cal") }
    return parts.joinToString(" · ")
}