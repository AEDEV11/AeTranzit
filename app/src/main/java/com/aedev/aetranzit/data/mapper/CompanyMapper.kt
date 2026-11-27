package com.aedev.aetranzit.data.mapper

import com.aedev.aetranzit.data.local.entity.CompanyEntity
import com.aedev.aetranzit.domain.model.Company

fun CompanyEntity.toDomain(): Company = Company(
    id = id,
    name = name,
    joinCode = joinCode,
    createdAt = createdAt,
    isSynced = isSynced,
    updatedAt = updatedAt
)
