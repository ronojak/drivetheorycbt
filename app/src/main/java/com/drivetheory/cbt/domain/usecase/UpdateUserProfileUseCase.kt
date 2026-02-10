package com.drivetheory.cbt.domain.usecase

import com.drivetheory.cbt.core.common.Result
import com.drivetheory.cbt.domain.entities.UserProfile
import com.drivetheory.cbt.domain.repository.UserRepository

class UpdateUserProfileUseCase(private val repo: UserRepository) {
    suspend operator fun invoke(profile: UserProfile): Result<Unit> = repo.upsertProfile(profile)
}

