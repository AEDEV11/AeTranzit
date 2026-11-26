package com.aedev.aetranzit.domain.model

enum class PassengerType(
    val displayName: String,
    val chipLabel: String,
    val isDiscounted: Boolean
) {
    REGULAR(displayName = "Regular", chipLabel = "Regular", isDiscounted = false),
    STUDENT(displayName = "Student", chipLabel = "Student -20%", isDiscounted = true),
    SENIOR(displayName = "Senior Citizen", chipLabel = "Senior -20%", isDiscounted = true),
    PWD(displayName = "PWD", chipLabel = "PWD -20%", isDiscounted = true);

    companion object {
        fun fromRaw(raw: String): PassengerType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: REGULAR
    }
}
