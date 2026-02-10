package com.drivetheory.cbt.presentation.mpesa

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.drivetheory.cbt.R

@AndroidEntryPoint
class MpesaPaymentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MpesaPaymentScreen(onDone = { finish() })
                }
            }
        }
    }
}

@Composable
fun MpesaPaymentScreen(onDone: () -> Unit, vm: MpesaPaymentViewModel = hiltViewModel()) {
    val st by vm.state.collectAsState(initial = MpesaUiState())
    var phone by remember { mutableStateOf(st.phoneNumber) }
    var plan by remember { mutableStateOf(st.selectedPlanId) }
    var manual by remember { mutableStateOf(st.manualReceipt) }

    Column(Modifier.padding(16.dp)) {
        Text(text = "Go Premium (M-PESA)", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it; vm.setPhone(it) },
            label = { Text("Phone Number (07xxxxxxxx)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Row {
            PlanChip("monthly", plan == "monthly") { plan = "monthly"; vm.setPlan("monthly") }
            Spacer(Modifier.width(8.dp))
            PlanChip("annual", plan == "annual") { plan = "annual"; vm.setPlan("annual") }
            Spacer(Modifier.width(8.dp))
            PlanChip("lifetime", plan == "lifetime") { plan = "lifetime"; vm.setPlan("lifetime") }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { vm.startPayment() },
            enabled = !st.inProgress,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.mpesa_green), contentColor = Color.White)
        ) {
            Text("Pay with M-PESA")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { vm.toggleManual() }) { Text("Already paid? Enter receipt") }
        if (st.showManual) {
            OutlinedTextField(
                value = manual,
                onValueChange = { manual = it; vm.setManualReceipt(it) },
                label = { Text("M-PESA Receipt (e.g., NLJ7RT61SV)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.submitReceipt() }, enabled = !st.inProgress) { Text("Submit Receipt") }
        }
        Spacer(Modifier.height(12.dp))
        if (st.inProgress) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text(text = st.message ?: "Processing…")
        }
        if (st.status != null) {
            Text(text = "Status: ${st.status}")
            if (st.status == "PAID") {
                val ctx = LocalContext.current
                Toast.makeText(ctx, "Premium activated", Toast.LENGTH_SHORT).show()
                LaunchedEffect(Unit) {
                    val intent = android.content.Intent(ctx, ReceiptDetailsActivity::class.java)
                    intent.putExtra(ReceiptDetailsActivity.EXTRA_RECEIPT, st.receipt)
                    intent.putExtra(ReceiptDetailsActivity.EXTRA_CHECKOUT, st.checkoutRequestId)
                    intent.putExtra(ReceiptDetailsActivity.EXTRA_MERCHANT, st.merchantRequestId)
                    intent.putExtra(ReceiptDetailsActivity.EXTRA_PAYMENT_ID, st.paymentId)
                    intent.putExtra(ReceiptDetailsActivity.EXTRA_PLAN, st.serverPlanId ?: st.selectedPlanId)
                    intent.putExtra(ReceiptDetailsActivity.EXTRA_AMOUNT, st.amount ?: 0)
                    intent.putExtra(ReceiptDetailsActivity.EXTRA_UPDATED_AT, st.updatedAt ?: 0L)
                    ctx.startActivity(intent)
                    onDone()
                }
            }
        }
        if (st.error != null) {
            Text(text = st.error ?: "", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun PlanChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text.replaceFirstChar { it.uppercase() }) }
    )
}
