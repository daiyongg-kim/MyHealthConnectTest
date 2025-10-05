package com.assessemnt.myhealthapp.domain.model

enum class DataSource {
    MANUAL,                    // Manual input
    HEALTH_CONNECT_SAMSUNG,    // Samsung Health
    HEALTH_CONNECT_GARMIN,     // Garmin
    HEALTH_CONNECT_OTHER;      // Other Health Connect sources

    fun displayName(): String = when (this) {
        MANUAL -> "Manual"
        HEALTH_CONNECT_SAMSUNG -> "Samsung Health"
        HEALTH_CONNECT_GARMIN -> "Garmin"
        HEALTH_CONNECT_OTHER -> "Health Connect"
    }
}