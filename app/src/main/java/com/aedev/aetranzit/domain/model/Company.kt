package com.aedev.aetranzit.domain.model

data class Company(
    val id: String,
    val name: String,
    val joinCode: String,
    val createdAt: Long,
    val isSynced: Boolean,
    val updatedAt: Long
) {
    companion object {
        const val DEFAULT_COMPANY_ID = "default-company"
        const val DEFAULT_COMPANY_NAME = "AeTranzit Fleet"
        const val DEFAULT_JOIN_CODE = "AET-0001"
        const val FALLBACK_BRAND_NAME = "AeTranzit"
    }
}
