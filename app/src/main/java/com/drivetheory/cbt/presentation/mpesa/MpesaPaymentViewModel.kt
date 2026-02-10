package com.drivetheory.cbt.presentation.mpesa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drivetheory.cbt.core.common.Result
import com.drivetheory.cbt.domain.repository.AuthRepository
import com.drivetheory.cbt.domain.repository.MpesaRepository
import com.drivetheory.cbt.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MpesaUiState(
    val phoneNumber: String = "",
    val selectedPlanId: String = "monthly",
    val manualReceipt: String = "",
    val showManual: Boolean = false,
    val paymentId: String? = null,
    val status: String? = null,
    val error: String? = null,
    val inProgress: Boolean = false,
    val message: String? = null,
    val receipt: String? = null,
    val checkoutRequestId: String? = null,
    val merchantRequestId: String? = null,
    val amount: Int? = null,
    val serverPlanId: String? = null,
    val updatedAt: Long? = null
)

@HiltViewModel
class MpesaPaymentViewModel @Inject constructor(
    private val mpesa: MpesaRepository,
    private val auth: AuthRepository,
    private val subs: SubscriptionRepository
) : ViewModel() {
    private val _state = MutableStateFlow(MpesaUiState())
    val state: StateFlow<MpesaUiState> = _state

    private var pollJob: Job? = null

    fun setPhone(input: String) {
        _state.value = _state.value.copy(phoneNumber = input)
    }

    fun setPlan(planId: String) {
        _state.value = _state.value.copy(selectedPlanId = planId)
    }

    fun toggleManual() {
        _state.value = _state.value.copy(showManual = !_state.value.showManual)
    }

    fun setManualReceipt(input: String) {
        _state.value = _state.value.copy(manualReceipt = input)
    }

    fun startPayment() {
        val uid = auth.currentUser()?.uid ?: return
        val phone = normalizeKenyanMsisdn(_state.value.phoneNumber) ?: run {
            _state.value = _state.value.copy(error = "Invalid phone number")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(inProgress = true, error = null, message = "Initiating STK Push…")
            when (val res = mpesa.initiate(uid, phone, _state.value.selectedPlanId)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(paymentId = res.data.serverPaymentId, status = res.data.status, message = "Check your phone and enter M-PESA PIN")
                    beginPolling()
                }
                is Result.Error -> _state.value = _state.value.copy(error = res.message, inProgress = false)
                else -> Unit
            }
        }
    }

    private fun beginPolling() {
        pollJob?.cancel()
        val id = _state.value.paymentId ?: return
        pollJob = viewModelScope.launch {
            val start = System.currentTimeMillis()
            while (System.currentTimeMillis() - start < 60_000) {
                when (val res = mpesa.status(id)) {
                    is Result.Success -> {
                        _state.value = _state.value.copy(
                            status = res.data.status,
                            receipt = res.data.mpesaReceipt,
                            checkoutRequestId = res.data.checkoutRequestId,
                            merchantRequestId = res.data.merchantRequestId,
                            amount = res.data.amount,
                            serverPlanId = res.data.planId,
                            updatedAt = res.data.updatedAt
                        )
                        if (res.data.status == "PAID") {
                            auth.currentUser()?.uid?.let { subs.refresh(it) }
                            _state.value = _state.value.copy(inProgress = false, message = "Payment successful")
                            break
                        }
                        if (res.data.status in listOf("FAILED", "CANCELLED", "TIMEOUT")) {
                            _state.value = _state.value.copy(inProgress = false)
                            break
                        }
                    }
                    is Result.Error -> _state.value = _state.value.copy(error = res.message)
                    else -> Unit
                }
                delay(2500)
            }
        }
    }

    companion object {
        fun normalizeKenyanMsisdn(input: String?): String? {
            if (input == null) return null
            val s = input.filter { it.isDigit() }
            if (s.startsWith("2547") && s.length == 12) return s
            if (s.startsWith("07") && s.length == 10) return "254" + s.substring(1)
            if (s.startsWith("7") && s.length == 9) return "254" + s
            if (s.startsWith("2541") || s.startsWith("01")) {
                if (s.startsWith("01") && s.length == 10) return "254" + s.substring(1)
                if (s.startsWith("2541") && s.length == 12) return s
            }
            return null
        }
    }

    fun submitReceipt() {
        val uid = auth.currentUser()?.uid ?: return
        val receipt = _state.value.manualReceipt.trim()
        if (receipt.isEmpty()) {
            _state.value = _state.value.copy(error = "Enter receipt code")
            return
        }
        val phone = normalizeKenyanMsisdn(_state.value.phoneNumber)
        viewModelScope.launch {
            _state.value = _state.value.copy(inProgress = true, error = null, message = "Submitting receipt…")
            when (val res = mpesa.submitManualReceipt(uid, receipt, _state.value.selectedPlanId, phone)) {
                is Result.Success -> {
                    if (res.data) {
                        auth.currentUser()?.uid?.let { subs.refresh(it) }
                        _state.value = _state.value.copy(inProgress = false, message = "Payment verified via receipt", status = "PAID")
                    } else {
                        _state.value = _state.value.copy(inProgress = false, message = "Receipt submitted for review", status = _state.value.status)
                    }
                }
                is Result.Error -> _state.value = _state.value.copy(inProgress = false, error = res.message)
                else -> Unit
            }
        }
    }
}
