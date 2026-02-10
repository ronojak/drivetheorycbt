package com.drivetheory.cbt.data.auth

import com.drivetheory.cbt.core.common.Result
import com.drivetheory.cbt.domain.entities.UserProfile
import com.drivetheory.cbt.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : AuthRepository {
    override suspend fun register(email: String, password: String, name: String?, phone: String?): Result<UserProfile> = try {
        val res = auth.createUserWithEmailAndPassword(email, password).await()
        val user = res.user ?: throw IllegalStateException("No user")
        user.sendEmailVerification().await()
        val profile = UserProfile(uid = user.uid, name = name, email = email, phone = phone)
        db.collection("users").document(user.uid).set(profile).await()
        Result.Success(profile)
    } catch (e: Exception) {
        Result.Error("Register failed", e)
    }

    override suspend fun login(email: String, password: String): Result<UserProfile> = try {
        val res = auth.signInWithEmailAndPassword(email, password).await()
        val user = res.user ?: throw IllegalStateException("No user")
        val snap = db.collection("users").document(user.uid).get().await()
        val profile = if (snap.exists()) snap.toObject(UserProfile::class.java)!!.copy(uid = user.uid) else UserProfile(uid = user.uid, email = user.email ?: email)
        Result.Success(profile)
    } catch (e: Exception) {
        Result.Error("Login failed", e)
    }

    override suspend fun logout(): Result<Unit> = try { auth.signOut(); Result.Success(Unit) } catch (e: Exception) { Result.Error("Logout failed", e) }

    override suspend fun resetPassword(email: String): Result<Unit> = try { auth.sendPasswordResetEmail(email).await(); Result.Success(Unit) } catch (e: Exception) { Result.Error("Reset failed", e) }

    override fun currentUser(): UserProfile? = auth.currentUser?.let { UserProfile(uid = it.uid, email = it.email ?: "") }
}

