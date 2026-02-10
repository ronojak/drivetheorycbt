package com.drivetheory.cbt.domain.repository

import com.drivetheory.cbt.core.common.Result
import com.drivetheory.cbt.domain.entities.Subscription

interface SubscriptionRepository {
    suspend fun refresh(uid: String): Result<Subscription>
    suspend fun getCached(): Subscription?
    suspend fun cache(subscription: Subscription)
}

