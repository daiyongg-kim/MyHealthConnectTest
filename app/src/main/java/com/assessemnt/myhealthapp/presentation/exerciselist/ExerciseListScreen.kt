package com.assessemnt.myhealthapp.presentation.exerciselist

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.assessemnt.myhealthapp.PermissionCheckActivity
import com.assessemnt.myhealthapp.domain.model.Exercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(
    onNavigateToManualInput: () -> Unit,
    onNavigateToConflictList: () -> Unit,
    viewModel: ExerciseListViewModel = viewModel()
) {
    val context = LocalContext.current
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val needsPermission by viewModel.needsPermission.collectAsStateWithLifecycle()
    val showConflictDialog by viewModel.showConflictDialog.collectAsStateWithLifecycle()
    val conflictingExercises by viewModel.conflictingExercises.collectAsStateWithLifecycle()

    // Launch PermissionCheckActivity when permission is needed
    LaunchedEffect(needsPermission) {
        if (needsPermission) {
            val intent = Intent(context, PermissionCheckActivity::class.java)
            context.startActivity(intent)
            viewModel.onPermissionHandled()
        }
    }

    // Show conflict dialog
    if (showConflictDialog) {
        ConflictDialog(
            conflictingExercises = conflictingExercises,
            onResolve = { selectedId ->
                viewModel.resolveConflicts(selectedId)
            },
            onDismiss = {
                viewModel.dismissConflictDialog()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Exercises") },
                actions = {
                    IconButton(
                        onClick = { viewModel.onSyncClicked() },
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync from Health Connect"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToManualInput
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Exercise"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (exercises.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No exercises found",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(exercises) { exercise ->
                        ExerciseCard(
                            exercise = exercise,
                            onDelete = { viewModel.deleteExercise(exercise.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExerciseCard(
    exercise: Exercise,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { },
                onLongClick = { showDeleteDialog = true }
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = exercise.type,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
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

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Exercise") },
            text = { Text("Are you sure you want to delete this exercise?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ConflictDialog(
    conflictingExercises: List<Exercise>,
    onResolve: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedId by remember { mutableStateOf(conflictingExercises.firstOrNull()?.id ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Time Conflict Detected") },
        text = {
            Column {
                Text(
                    text = "Multiple exercises overlap in time. Select which one to keep:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                conflictingExercises.forEach { exercise ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedId == exercise.id,
                            onClick = {
                                selectedId = exercise.id
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "${exercise.type} - ${exercise.durationMinutes} min",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Time: ${exercise.startTime}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onResolve(selectedId)
                }
            ) {
                Text("Keep Selected")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}