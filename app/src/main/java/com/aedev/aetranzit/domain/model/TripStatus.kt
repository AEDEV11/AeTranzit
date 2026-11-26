package com.aedev.aetranzit.domain.model

enum class TripStatus(val displayName: String) {
    ACTIVE("Active"),
    COMPLETED("Completed");

    companion object {
        fun fromRaw(raw: String): TripStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: ACTIVE
    }
}
