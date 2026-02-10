package com.drivetheory.cbt.domain.entities

data class PaymentRecord(
    val reference: String,
    val amount: Long,
    val currency: String = "KES",
    val status: String,
    val createdAt: Long = System.currentTimeMillis()
)

