package com.drivetheory.cbt.data.user

import com.drivetheory.cbt.core.common.Result
import com.drivetheory.cbt.domain.entities.UserProfile
import com.drivetheory.cbt.domain.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : UserRepository {
    override suspend fun getProfile(uid: String): Result<UserProfile> = try {
        val snap = db.collection("users").document(uid).get().await()
        if (snap.exists()) Result.Success(snap.toObject(UserProfile::class.java)!!.copy(uid = uid)) else Result.Error("Not found")
    } catch (e: Exception) {
        Result.Error("Fetch failed", e)
    }

    override suspend fun upsertProfile(profile: UserProfile): Result<Unit> = try {
        db.collection("users").document(profile.uid).set(profile.copy(updatedAt = System.currentTimeMillis())).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error("Upsert failed", e)
    }

    override suspend fun deleteProfile(uid: String): Result<Unit> = try {
        db.collection("users").document(uid).delete().await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error("Delete failed", e)
    }
}

