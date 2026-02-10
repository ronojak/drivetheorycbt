package com.drivetheory.cbt.data.repository

import com.drivetheory.cbt.data.local.db.AppDatabase
import com.drivetheory.cbt.data.local.entities.ExamAttemptEntity
import com.drivetheory.cbt.domain.model.ExamAttempt
import com.drivetheory.cbt.domain.repository.AttemptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AttemptRepositoryImpl(
    db: AppDatabase
) : AttemptRepository {
    private val dao = db.examAttemptDao()

    override suspend fun recordAttempt(attempt: ExamAttempt) {
        dao.insert(
            ExamAttemptEntity(
                score = attempt.score,
                totalQuestions = attempt.totalQuestions,
                timestamp = attempt.timestamp,
            )
        )
    }

    override fun observeAttempts(): Flow<List<ExamAttempt>> =
        dao.observeAll().map { list ->
            list.map { e ->
                ExamAttempt(
                    id = e.id,
                    score = e.score,
                    totalQuestions = e.totalQuestions,
                    timestamp = e.timestamp,
                )
            }
        }
}

