package com.drivetheory.cbt.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drivetheory.cbt.core.common.Result
import com.drivetheory.cbt.domain.entities.UserProfile
import com.drivetheory.cbt.domain.repository.AuthRepository
import com.drivetheory.cbt.domain.repository.SubscriptionRepository
import com.drivetheory.cbt.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val subscriptionStatus: String? = null,
    val toast: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val users: UserRepository,
    private val subs: SubscriptionRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state

    fun load() {
        val uid = auth.currentUser()?.uid ?: return
        viewModelScope.launch {
            val prof = users.getProfile(uid)
            val sub = subs.getCached()
            val name = when (prof) { is Result.Success -> prof.data.name else -> null }
            val phone = when (prof) { is Result.Success -> prof.data.phone else -> null }
            val email = when (prof) { is Result.Success -> prof.data.email else -> auth.currentUser()?.email }
            _state.value = ProfileUiState(name = name, email = email, phone = phone, subscriptionStatus = sub?.status ?: "free")
        }
    }

    fun save(name: String?, phone: String?) {
        val user = auth.currentUser() ?: return
        viewModelScope.launch {
            val res = users.upsertProfile(UserProfile(uid = user.uid, name = name, email = user.email, phone = phone))
            _state.value = _state.value.copy(toast = if (res is Result.Success) "Saved" else "Save failed")
        }
    }
}
