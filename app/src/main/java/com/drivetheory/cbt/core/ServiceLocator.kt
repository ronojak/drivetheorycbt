package com.drivetheory.cbt.core

import android.content.Context
import com.drivetheory.cbt.data.local.db.AppDatabase
import com.drivetheory.cbt.data.repository.AttemptRepositoryImpl
import com.drivetheory.cbt.data.repository.QuestionRepositoryImpl
import com.drivetheory.cbt.data.seed.QuestionSeedLoader
import com.drivetheory.cbt.domain.repository.AttemptRepository
import com.drivetheory.cbt.domain.repository.QuestionRepository

object ServiceLocator {
    @Volatile private var questionRepo: QuestionRepository? = null
    @Volatile private var attemptRepo: AttemptRepository? = null

    fun questions(context: Context): QuestionRepository =
        questionRepo ?: synchronized(this) {
            questionRepo ?: QuestionRepositoryImpl(QuestionSeedLoader(context)).also { questionRepo = it }
        }

    fun attempts(context: Context): AttemptRepository =
        attemptRepo ?: synchronized(this) {
            attemptRepo ?: AttemptRepositoryImpl(AppDatabase.get(context)).also { attemptRepo = it }
        }
}

