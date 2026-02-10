package com.drivetheory.cbt.domain.usecase

import com.drivetheory.cbt.core.common.Result
import com.drivetheory.cbt.domain.entities.Subscription
import com.drivetheory.cbt.domain.repository.SubscriptionRepository

class GetSubscriptionStatusUseCase(private val repo: SubscriptionRepository) {
    suspend operator fun invoke(uid: String, refresh: Boolean = true): Result<Subscription> =
        if (refresh) repo.refresh(uid) else repo.getCached()?.let { com.drivetheory.cbt.core.common.Result.Success(it) } ?: com.drivetheory.cbt.core.common.Result.Error("No cache")
}

