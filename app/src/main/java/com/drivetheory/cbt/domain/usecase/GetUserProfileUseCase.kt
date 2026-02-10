package com.drivetheory.cbt.domain.usecase

import com.drivetheory.cbt.core.common.Result
import com.drivetheory.cbt.domain.entities.UserProfile
import com.drivetheory.cbt.domain.repository.UserRepository

class GetUserProfileUseCase(private val repo: UserRepository) {
    suspend operator fun invoke(uid: String): Result<UserProfile> = repo.getProfile(uid)
}

