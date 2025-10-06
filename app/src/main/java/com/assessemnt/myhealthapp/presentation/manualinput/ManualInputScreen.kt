package com.assessemnt.myhealthapp.presentation.manualinput

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualInputScreen(
    onNavigateBack: () -> Unit,
    viewModel: ManualInputViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Navigate back when saved
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Exercise") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Exercise Type Dropdown
            ExerciseTypeDropdown(
                selectedType = state.exerciseType,
                onTypeSelected = { viewModel.updateExerciseType(it) },
                isError = state.exerciseTypeError != null,
                errorMessage = state.exerciseTypeError
            )

            // Start Time Picker
            StartTimePicker(
                startTime = state.startTime,
                onTimeSelected = { viewModel.updateStartTime(it) },
                isError = state.startTimeError != null,
                errorMessage = state.startTimeError
            )

            // Duration Dropdown
            DurationDropdown(
                selectedDuration = state.durationMinutes,
                onDurationSelected = { viewModel.updateDuration(it) },
                isError = state.durationError != null,
                errorMessage = state.durationError
            )

            // Distance (optional)
            OutlinedTextField(
                value = state.distance,
                onValueChange = { viewModel.updateDistance(it) },
                label = { Text("Distance (km)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = state.distanceError != null,
                supportingText = {
                    state.distanceError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Calories (optional)
            OutlinedTextField(
                value = state.calories,
                onValueChange = { viewModel.updateCalories(it) },
                label = { Text("Calories") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = state.caloriesError != null,
                supportingText = {
                    state.caloriesError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Notes (optional)
            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.updateNotes(it) },
                label = { Text("Notes") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // Save Button
            Button(
                onClick = { viewModel.saveExercise() },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save Exercise")
                }
            }

            // Error message
            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    // Conflict Dialog
    state.conflictingExercise?.let { conflictingExercise ->
        if (state.showConflictDialog) {
            ConflictDialog(
                existingExercise = conflictingExercise,
                onKeepNew = { viewModel.resolveConflictKeepNew() },
                onKeepExisting = { viewModel.resolveConflictKeepExisting() },
                onDismiss = { viewModel.dismissConflictDialog() }
            )
        }
    }
}

@Composable
fun ConflictDialog(
    existingExercise: com.assessemnt.myhealthapp.domain.model.Exercise,
    onKeepNew: () -> Unit,
    onKeepExisting: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Time Conflict Detected") },
        text = {
            Column {
                Text("The time you selected overlaps with an existing exercise:")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${existingExercise.type} - ${existingExercise.durationMinutes} min",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Text(
                    text = "Time: ${existingExercise.startTime}",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("What would you like to do?")
            }
        },
        confirmButton = {
            TextButton(onClick = onKeepNew) {
                Text("Keep New")
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepExisting) {
                Text("Keep Existing")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseTypeDropdown(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?
) {
    var expanded by remember { mutableStateOf(false) }

    val exerciseTypes = listOf(
        "Running",
        "Walking",
        "Swimming",
        "Yoga",
        "Hiking",
        "Other"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedType,
            onValueChange = {},
            readOnly = true,
            label = { Text("Exercise Type *") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            isError = isError,
            supportingText = {
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            exerciseTypes.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type) },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurationDropdown(
    selectedDuration: String,
    onDurationSelected: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?
) {
    var expanded by remember { mutableStateOf(false) }

    val durationOptions = listOf(
        "15" to "15 minutes",
        "30" to "30 minutes",
        "45" to "45 minutes",
        "60" to "1 hour",
        "120" to "2 hours",
        "180" to "3 hours"
    )

    val displayText = durationOptions.find { it.first == selectedDuration }?.second ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Duration *") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            isError = isError,
            supportingText = {
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            durationOptions.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onDurationSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartTimePicker(
    startTime: kotlinx.datetime.LocalDateTime?,
    onTimeSelected: (kotlinx.datetime.LocalDateTime) -> Unit,
    isError: Boolean,
    errorMessage: String?
) {
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableStateOf(startTime?.hour ?: 12) }
    var selectedMinute by remember { mutableStateOf(startTime?.minute ?: 0) }

    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val displayText = startTime?.let {
        val javaTime = LocalDateTime.of(it.year, it.monthNumber, it.dayOfMonth, it.hour, it.minute)
        javaTime.format(formatter)
    } ?: ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showTimePicker = true }
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Start Time *") },
            isError = isError,
            supportingText = {
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            },
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val now = LocalDateTime.now()
                    val updatedTime = kotlinx.datetime.LocalDateTime(
                        now.year, now.monthValue, now.dayOfMonth,
                        selectedHour, selectedMinute
                    )
                    onTimeSelected(updatedTime)
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Select Time") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    // Hour Picker
                    var hourExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = hourExpanded,
                        onExpandedChange = { hourExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = String.format("%02d", selectedHour),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Hour") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = hourExpanded)
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = hourExpanded,
                            onDismissRequest = { hourExpanded = false }
                        ) {
                            (0..23).forEach { hour ->
                                DropdownMenuItem(
                                    text = { Text(String.format("%02d", hour)) },
                                    onClick = {
                                        selectedHour = hour
                                        hourExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Text(" : ", modifier = Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.headlineMedium)

                    // Minute Picker
                    var minuteExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = minuteExpanded,
                        onExpandedChange = { minuteExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = String.format("%02d", selectedMinute),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Minute") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = minuteExpanded)
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = minuteExpanded,
                            onDismissRequest = { minuteExpanded = false }
                        ) {
                            (0..59).forEach { minute ->
                                DropdownMenuItem(
                                    text = { Text(String.format("%02d", minute)) },
                                    onClick = {
                                        selectedMinute = minute
                                        minuteExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}