package com.drivetheory.cbt.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.drivetheory.cbt.core.ServiceLocator
import com.drivetheory.cbt.domain.model.ExamAttempt
import com.drivetheory.cbt.domain.model.Question
import com.drivetheory.cbt.engine.ExamEngine
import com.drivetheory.cbt.engine.TimerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.drivetheory.cbt.core.gating.AccessGate

data class ExamUiState(
    val index: Int = 0,
    val total: Int = 0,
    val questionText: String = "",
    val options: List<String> = emptyList(),
    val selectedIndex: Int? = null,
    val remaining: Int = 0,
    val finished: Boolean = false,
    val score: Int = 0,
    val redirectToBilling: Boolean = false,
)

class ExamViewModel(app: Application, private val desiredCount: Int? = null, private val useAll: Boolean = false, private val vehicle: String = "car") : AndroidViewModel(app) {
    private val questionsRepo = ServiceLocator.questions(app)
    private val attemptsRepo = ServiceLocator.attempts(app)

    private var engine: ExamEngine? = null
    private var timer: TimerController? = null

    private val _uiState = MutableStateFlow(ExamUiState())
    val uiState: StateFlow<ExamUiState> = _uiState

    init {
        viewModelScope.launch {
            val questions = questionsRepo.loadQuestions(vehicle)
            if (questions.isEmpty()) {
                _uiState.value = ExamUiState(total = 0, questionText = "No questions available", options = emptyList())
                return@launch
            }
            val requested = if (useAll) questions.size else (desiredCount ?: 50)
            val now = System.currentTimeMillis()
            val prefs = com.drivetheory.cbt.core.security.EncryptedPrefs.get(getApplication())
            val status = prefs.getString("sub_status", "free") ?: "free"
            val expiry = prefs.getLong("sub_expiry", 0L)
            val isPremium = (status == "active") && (expiry == 0L || expiry > now)
            val gate = AccessGate(getApplication())
            val decision = gate.canStartSession(isPremium, requested, vehicle)
            // If explicitly not allowed, show reason if available
            if (!decision.allowed && decision.allowedCount <= 0) {
                val msg = decision.reason ?: "Daily limit reached. Upgrade to continue."
                _uiState.value = ExamUiState(total = 0, questionText = msg, options = emptyList(), redirectToBilling = true)
                return@launch
            }
            val finalCount = decision.allowedCount.coerceAtMost(questions.size)
            if (finalCount <= 0) {
                _uiState.value = ExamUiState(total = 0, questionText = "No questions available", options = emptyList())
                return@launch
            }
            // Free mode: do not sample/shuffle; serve next slice based on prior daily usage
            val shuffle = isPremium
            val baseQuestions = if (!isPremium) {
                val consumedToday = gate.consumedToday(vehicle)
                questions.drop(consumedToday)
            } else questions
            val actualCount = finalCount.coerceAtMost(baseQuestions.size)
            if (actualCount <= 0) {
                _uiState.value = ExamUiState(total = 0, questionText = "No questions available", options = emptyList())
                return@launch
            }
            engine = ExamEngine(baseQuestions, actualCount, seed = null, shuffle = shuffle)
            // Count usage against the daily category allowance for free users
            if (!isPremium) gate.recordUsage(vehicle, actualCount)
            val timeLimit = computeTimeLimitSeconds(actualCount)
            timer = TimerController(viewModelScope, totalSeconds = timeLimit).also { t ->
                t.start(onFinish = { finishAndScore() })
                viewModelScope.launch { t.remaining.collect { r -> _uiState.value = _uiState.value.copy(remaining = r) } }
            }
            refresh()
        }
    }

    private fun computeTimeLimitSeconds(totalQuestions: Int): Int {
        val perQuestion = 60 // seconds per question
        val minSeconds = 10 * 60 // 10 minutes minimum
        val maxSeconds = 2 * 60 * 60 // 2 hours maximum
        return (totalQuestions * perQuestion).coerceIn(minSeconds, maxSeconds)
    }

    private fun refresh() {
        val e = engine ?: return
        val q = e.current()
        _uiState.value = _uiState.value.copy(
            index = e.currentIndex(),
            total = e.total(),
            questionText = q.text,
            options = q.options,
            selectedIndex = e.selectedIndexFor(q)
        )
    }

    fun select(idx: Int) {
        engine?.selectAnswer(idx)
        refresh()
    }

    fun next() { if (engine?.next() == true) refresh() }
    fun prev() { if (engine?.prev() == true) refresh() }

    fun finishAndScore(): Pair<Int, Int> {
        val e = engine ?: return 0 to 0
        val (score, total) = e.score()
        _uiState.value = _uiState.value.copy(finished = true, score = score)
        // Build review data and publish to store
        ReviewStore.items = buildReview(e)
        viewModelScope.launch {
            attemptsRepo.recordAttempt(
                ExamAttempt(score = score, totalQuestions = total, timestamp = System.currentTimeMillis())
            )
        }
        timer?.cancel()
        return score to total
    }

    private fun buildReview(e: ExamEngine): List<AnswerReview> {
        val list = mutableListOf<AnswerReview>()
        e.questions.forEach { q ->
            val sel = e.selectedIndexFor(q)
            val isCorrect = sel != null && sel == q.correctIndex
            list.add(AnswerReview(question = q, selectedIndex = sel, correctIndex = q.correctIndex, isCorrect = isCorrect))
        }
        return list
    }

    companion object {
        fun factory(desiredCount: Int?, useAll: Boolean, vehicle: String): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = ServiceLocatorRef.app ?: throw IllegalStateException("App not set")
                @Suppress("UNCHECKED_CAST")
                return ExamViewModel(app, desiredCount, useAll, vehicle) as T
            }
        }
    }
}

// Simple holder to supply Application to ViewModel factory without DI framework.
object ServiceLocatorRef { var app: Application? = null }

data class AnswerReview(
    val question: Question,
    val selectedIndex: Int?,
    val correctIndex: Int,
    val isCorrect: Boolean
)

object ReviewStore {
    @Volatile var items: List<AnswerReview> = emptyList()
}
