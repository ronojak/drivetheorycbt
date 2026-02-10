package com.drivetheory.cbt.domain.repository

import com.drivetheory.cbt.core.common.Result
import com.drivetheory.cbt.domain.entities.UserProfile

interface UserRepository {
    suspend fun getProfile(uid: String): Result<UserProfile>
    suspend fun upsertProfile(profile: UserProfile): Result<Unit>
    suspend fun deleteProfile(uid: String): Result<Unit>
}

