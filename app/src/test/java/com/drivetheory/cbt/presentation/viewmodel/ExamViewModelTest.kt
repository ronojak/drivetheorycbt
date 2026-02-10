package com.drivetheory.cbt.presentation.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExamViewModelTest {

    @Test
    fun initializes_and_scores_without_crash() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = ExamViewModel(app)

        // Allow async init to load questions (or decide none exist)
        delay(400)

        val total = vm.uiState.value.total

        // If there are no questions in the test environment, the ViewModel should not crash.
        if (total <= 0) {
            assertTrue(vm.uiState.value.questionText.isNotBlank())
            return@runBlocking
        }

        // Otherwise: Select first option, navigate, finish
        vm.select(0)
        vm.next()
        val (score, scoredTotal) = vm.finishAndScore()

        assertTrue(scoredTotal >= 1)
        assertTrue(score >= 0)
    }
}
