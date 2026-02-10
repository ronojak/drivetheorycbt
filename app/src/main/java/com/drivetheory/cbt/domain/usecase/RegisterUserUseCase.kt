package com.drivetheory.cbt.domain.usecase

import com.drivetheory.cbt.core.common.Result
import com.drivetheory.cbt.domain.entities.UserProfile
import com.drivetheory.cbt.domain.repository.AuthRepository

class RegisterUserUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String, password: String, name: String?, phone: String?): Result<UserProfile> =
        repo.register(email, password, name, phone)
}

