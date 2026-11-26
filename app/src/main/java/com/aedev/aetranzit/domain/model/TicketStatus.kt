package com.aedev.aetranzit.domain.model

enum class TicketStatus(val displayName: String) {
    ACTIVE("Active"),
    DROPPED_OFF("Dropped Off");

    companion object {
        fun fromRaw(raw: String): TicketStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: ACTIVE
    }
}
