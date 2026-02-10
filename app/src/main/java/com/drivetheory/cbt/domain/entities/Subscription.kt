package com.drivetheory.cbt.domain.entities

data class Subscription(
    val status: String, // free, active, past_due, expired
    val planType: String?, // monthly, annual
    val startDate: Long?,
    val endDate: Long?,
    val expiryDate: Long?
)

