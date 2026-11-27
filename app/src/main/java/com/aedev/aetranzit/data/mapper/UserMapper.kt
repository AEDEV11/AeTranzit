package com.aedev.aetranzit.data.mapper

import com.aedev.aetranzit.data.local.entity.UserEntity
import com.aedev.aetranzit.domain.model.User
import com.aedev.aetranzit.domain.model.UserRole

fun UserEntity.toDomain(): User = User(
    id = id,
    name = name,
    email = email,
    role = UserRole.fromRaw(role),
    companyId = companyId,
    isActive = isActive,
    mustChangePassword = mustChangePassword,
    isSynced = isSynced,
    updatedAt = updatedAt
)
