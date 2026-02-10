package com.drivetheory.cbt.domain.repository

import com.drivetheory.cbt.domain.model.ExamAttempt
import kotlinx.coroutines.flow.Flow

interface AttemptRepository {
    suspend fun recordAttempt(attempt: ExamAttempt)
    fun observeAttempts(): Flow<List<ExamAttempt>>
}

