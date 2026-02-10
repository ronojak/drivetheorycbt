package com.drivetheory.cbt.engine

import com.drivetheory.cbt.domain.model.Question
import org.junit.Assert.assertEquals
import org.junit.Test

class ExamEngineTest {
    @Test
    fun score_counts_correct_answers() {
        val questions = listOf(
            Question("1", "Q1", listOf("A","B"), correctIndex = 1),
            Question("2", "Q2", listOf("A","B"), correctIndex = 0),
        )
        val engine = ExamEngine(questions, 2, seed = 123)

        // Answer each shown question correctly in engine order
        repeat(engine.total()) {
            val q = engine.current()
            engine.selectAnswer(q.correctIndex)
            engine.next()
        }

        val (score, total) = engine.score()
        assertEquals(2, score)
        assertEquals(2, total)
    }
}
