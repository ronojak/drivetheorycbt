package com.drivetheory.cbt.presentation.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drivetheory.cbt.core.common.Result
import com.drivetheory.cbt.domain.repository.AuthRepository
import com.drivetheory.cbt.domain.repository.BillingRepository
import com.drivetheory.cbt.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionUiState(
    val loading: Boolean = false,
    val checkoutUrl: String? = null,
    val reference: String? = null,
    val verified: Boolean? = null,
    val error: String? = null
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val billing: BillingRepository,
    private val subs: SubscriptionRepository,
    private val auth: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SubscriptionUiState())
    val state: StateFlow<SubscriptionUiState> = _state

    fun initialize(plan: String) {
        val uid = auth.currentUser()?.uid ?: return
        viewModelScope.launch {
            _state.value = SubscriptionUiState(loading = true)
            when (val res = billing.initializePayment(uid, plan)) {
                is Result.Success -> _state.value = SubscriptionUiState(checkoutUrl = res.data.checkoutUrl, reference = res.data.reference)
                is Result.Error -> _state.value = SubscriptionUiState(error = res.message)
                else -> Unit
            }
        }
    }

    fun verifyIfPending() {
        val ref = _state.value.reference ?: return
        viewModelScope.launch {
            when (val res = billing.verifyPayment(ref)) {
                is Result.Success -> {
                    if (res.data) {
                        auth.currentUser()?.uid?.let { subs.refresh(it) }
                        _state.value = _state.value.copy(verified = true)
                    }
                }
                is Result.Error -> _state.value = _state.value.copy(error = res.message)
                else -> Unit
            }
        }
    }
}
