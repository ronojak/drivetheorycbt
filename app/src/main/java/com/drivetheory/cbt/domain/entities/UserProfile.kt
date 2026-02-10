package com.drivetheory.cbt.domain.entities

data class UserProfile(
    val uid: String,
    val name: String? = null,
    val email: String,
    val phone: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long? = null,
    val subscriptionStatus: String = "free",
    val planType: String? = null,
    val expiryDate: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

