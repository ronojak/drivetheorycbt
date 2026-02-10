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

data class RegisterUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val repo: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state

    fun register(name: String?, phone: String?, email: String, password: String) {
        viewModelScope.launch {
            _state.value = RegisterUiState(loading = true)
            val res = repo.register(email, password, name, phone)
            _state.value = when (res) {
                is Result.Success -> RegisterUiState(success = true)
                is Result.Error -> RegisterUiState(error = (res.cause?.message?.takeIf { it.isNotBlank() } ?: res.message))
                else -> RegisterUiState()
            }
        }
    }
}

