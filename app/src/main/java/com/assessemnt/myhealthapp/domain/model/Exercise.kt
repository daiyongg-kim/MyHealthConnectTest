package com.assessemnt.myhealthapp.domain.model

import kotlinx.datetime.LocalDateTime

data class Exercise(
    val id: String,                    // Unique identifier
    val type: String,                  // "Running", "Yoga", "Walking", etc.
    val startTime: LocalDateTime,      // When exercise started
    val durationMinutes: Int,          // How long it lasted
    val source: DataSource,            // Where data came from
    val distance: Double? = null,      // Distance in km (optional)
    val calories: Int? = null,         // Calories burned (optional)
    val notes: String? = null          // User notes (optional)
)