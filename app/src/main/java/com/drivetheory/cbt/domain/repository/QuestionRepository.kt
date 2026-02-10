package com.drivetheory.cbt.domain.repository

import com.drivetheory.cbt.domain.model.Question

interface QuestionRepository {
    suspend fun loadQuestions(): List<Question>
    suspend fun loadQuestions(vehicle: String): List<Question>
}
