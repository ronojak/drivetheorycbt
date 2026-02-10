package com.drivetheory.cbt.domain.usecase

import com.drivetheory.cbt.core.common.Result
import com.drivetheory.cbt.domain.repository.AuthRepository

class ResetPasswordUseCase(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String): Result<Unit> = repo.resetPassword(email)
}

