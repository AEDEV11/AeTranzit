package com.aedev.aetranzit.domain.model

enum class UserRole(val displayName: String) {
    ADMIN("Admin"),
    CONDUCTOR("Conductor");

    companion object {
        fun fromRaw(raw: String): UserRole =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: CONDUCTOR
    }
}
