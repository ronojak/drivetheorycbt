package com.drivetheory.cbt.domain.repository

import com.drivetheory.cbt.core.common.Result
import com.drivetheory.cbt.domain.entities.UserProfile

interface AuthRepository {
    suspend fun register(email: String, password: String, name: String?, phone: String?): Result<UserProfile>
    suspend fun login(email: String, password: String): Result<UserProfile>
    suspend fun logout(): Result<Unit>
    suspend fun resetPassword(email: String): Result<Unit>
    fun currentUser(): UserProfile?
}

