package com.drivetheory.cbt.engine

import com.drivetheory.cbt.domain.model.Question
import kotlin.random.Random

class ExamEngine(
    questions: List<Question>,
    questionCount: Int,
    seed: Long? = null,
    shuffle: Boolean = true
) {
    private val rng = seed?.let { Random(it) } ?: Random
    val questions: List<Question>
    private var index: Int = 0
    private val selections: MutableMap<String, Int> = mutableMapOf()

    init {
        val limited = questionCount.coerceAtMost(questions.size)
        val subset = if (shuffle) questions.shuffled(rng).take(limited) else questions.take(limited)
        this.questions = subset
    }

    fun currentIndex(): Int = index
    fun total(): Int = questions.size
    fun current(): Question = questions[index]
    fun isAnswered(q: Question = current()): Boolean = selections.containsKey(q.id)
    fun selectedIndexFor(q: Question = current()): Int? = selections[q.id]

    fun selectAnswer(index: Int) {
        val q = current()
        selections[q.id] = index
    }

    fun next(): Boolean = if (index < total() - 1) { index++; true } else false
    fun prev(): Boolean = if (index > 0) { index--; true } else false

    fun score(): Pair<Int, Int> {
        var correct = 0
        questions.forEach { q ->
            val selected = selections[q.id]
            if (selected != null && selected == q.correctIndex) correct++
        }
        return correct to total()
    }
}
