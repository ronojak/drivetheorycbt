package com.drivetheory.cbt.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.drivetheory.cbt.core.ServiceLocator
import com.drivetheory.cbt.domain.model.ExamAttempt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(app: Application) : AndroidViewModel(app) {
    private val attemptsRepo = ServiceLocator.attempts(app)

    val attempts: StateFlow<List<ExamAttempt>> = attemptsRepo.observeAttempts()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val app = ServiceLocatorRef.app ?: throw IllegalStateException("App not set")
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(app) as T
        }
    }
}

