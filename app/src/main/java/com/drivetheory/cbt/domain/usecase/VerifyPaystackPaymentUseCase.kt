package com.drivetheory.cbt.domain.usecase

import com.drivetheory.cbt.core.common.Result
import com.drivetheory.cbt.domain.repository.BillingRepository

class VerifyPaystackPaymentUseCase(private val repo: BillingRepository) {
    suspend operator fun invoke(reference: String): Result<Boolean> = repo.verifyPayment(reference)
}

