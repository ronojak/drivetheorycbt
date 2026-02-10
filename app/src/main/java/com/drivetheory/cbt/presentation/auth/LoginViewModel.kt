package com.drivetheory.cbt.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drivetheory.cbt.core.common.Result
import com.drivetheory.cbt.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
): ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = LoginUiState(loading = true)
            when (val res = authRepository.login(email, password)) {
                is Result.Success -> _state.value = LoginUiState(success = true)
                is Result.Error -> _state.value = LoginUiState(error = res.message)
                else -> Unit
            }
        }
    }
}

