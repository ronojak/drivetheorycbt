package com.drivetheory.cbt.data.repository

import com.drivetheory.cbt.data.seed.QuestionSeedLoader
import com.drivetheory.cbt.domain.model.Question
import com.drivetheory.cbt.domain.repository.QuestionRepository

class QuestionRepositoryImpl(
    private val loader: QuestionSeedLoader
) : QuestionRepository {
    override suspend fun loadQuestions(): List<Question> = loader.loadAllFromAssets()
    override suspend fun loadQuestions(vehicle: String): List<Question> = loader.loadForVehicle(vehicle)
}
