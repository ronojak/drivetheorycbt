package com.drivetheory.cbt.data.subscription

import android.content.Context
import com.drivetheory.cbt.core.common.Result
import com.drivetheory.cbt.core.security.EncryptedPrefs
import com.drivetheory.cbt.domain.entities.Subscription
import com.drivetheory.cbt.domain.repository.SubscriptionRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SubscriptionRepositoryImpl @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val db: FirebaseFirestore
) : SubscriptionRepository {
    private val prefs by lazy { EncryptedPrefs.get(context) }

    override suspend fun refresh(uid: String): Result<Subscription> = try {
        val snap = db.collection("subscriptions").document(uid).get().await()
        if (!snap.exists()) {
            val sub = Subscription(status = "free", planType = null, startDate = null, endDate = null, expiryDate = null)
            cache(sub)
            Result.Success(sub)
        } else {
            val status = snap.getString("status") ?: "free"
            val plan = snap.getString("planType")
            val expiry = snap.getLong("expiryDate")
            val start = snap.getLong("startDate")
            val end = snap.getLong("endDate")
            val sub = Subscription(status = status, planType = plan, startDate = start, endDate = end, expiryDate = expiry)
            cache(sub)
            Result.Success(sub)
        }
    } catch (e: Exception) {
        Result.Error("Refresh failed", e)
    }

    override suspend fun getCached(): Subscription? {
        val status = prefs.getString(KEY_STATUS, "free") ?: "free"
        val plan = prefs.getString(KEY_PLAN)
        val expiry = prefs.getLong(KEY_EXPIRY, 0L).takeIf { it > 0 }
        return Subscription(status = status, planType = plan, startDate = null, endDate = null, expiryDate = expiry)
    }

    override suspend fun cache(subscription: Subscription) {
        prefs.putString(KEY_STATUS, subscription.status)
        prefs.putString(KEY_PLAN, subscription.planType)
        prefs.putLong(KEY_EXPIRY, subscription.expiryDate ?: 0L)
        prefs.putLong(KEY_LAST_VERIFIED, System.currentTimeMillis())
    }

    companion object {
        private const val KEY_STATUS = "sub_status"
        private const val KEY_PLAN = "sub_plan"
        private const val KEY_EXPIRY = "sub_expiry"
        private const val KEY_LAST_VERIFIED = "sub_last_verified"
    }
}
