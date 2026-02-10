package com.drivetheory.cbt.domain.model

data class ExamAttempt(
    val id: Long = 0,
    val score: Int,
    val totalQuestions: Int,
    val timestamp: Long,
)

