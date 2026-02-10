package com.drivetheory.cbt.presentation.mpesa

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.drivetheory.cbt.BuildConfig

class ReceiptDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val receipt = intent.getStringExtra(EXTRA_RECEIPT)
        val checkout = intent.getStringExtra(EXTRA_CHECKOUT)
        val merchant = intent.getStringExtra(EXTRA_MERCHANT)
        val paymentId = intent.getStringExtra(EXTRA_PAYMENT_ID)
        val plan = intent.getStringExtra(EXTRA_PLAN)
        val amount = intent.getIntExtra(EXTRA_AMOUNT, 0)
        val updatedAt = intent.getLongExtra(EXTRA_UPDATED_AT, 0L)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ReceiptDetailsScreen(
                        receipt = receipt,
                        checkout = checkout,
                        merchant = merchant,
                        plan = plan,
                        amount = amount,
                        onShareText = {
                            val share = Intent(Intent.ACTION_SEND)
                            share.type = "text/plain"
                            share.putExtra(Intent.EXTRA_TEXT, buildShareText(receipt, checkout, merchant, plan, amount))
                            startActivity(Intent.createChooser(share, "Share Receipt"))
                        },
                        onShareCsv = {
                            val csv = buildCsv(paymentId, receipt, checkout, merchant, plan, amount, updatedAt)
                            val share = Intent(Intent.ACTION_SEND)
                            share.type = "text/csv"
                            share.putExtra(Intent.EXTRA_TEXT, csv)
                            startActivity(Intent.createChooser(share, "Share Receipt CSV"))
                        },
                        onViewLogs = {
                            val url = buildLogsUrl(paymentId, checkout)
                            if (url != null) startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    )
                }
            }
        }
    }

    private fun buildShareText(receipt: String?, checkout: String?, merchant: String?, plan: String?, amount: Int): String {
        val parts = mutableListOf<String>()
        parts.add("NTSA Premium Payment")
        if (!receipt.isNullOrBlank()) parts.add("Receipt: $receipt")
        if (!checkout.isNullOrBlank()) parts.add("Checkout ID: $checkout")
        if (!merchant.isNullOrBlank()) parts.add("Merchant ID: $merchant")
        if (!plan.isNullOrBlank()) parts.add("Plan: $plan")
        if (amount > 0) parts.add("Amount: KES $amount")
        return parts.joinToString("\n")
    }

    private fun buildCsv(paymentId: String?, receipt: String?, checkout: String?, merchant: String?, plan: String?, amount: Int, updatedAt: Long): String {
        val headers = listOf("paymentId","receipt","checkoutRequestId","merchantRequestId","planId","amount","updatedAt")
        val row = listOf(paymentId, receipt, checkout, merchant, plan, if (amount>0) amount.toString() else "", if (updatedAt>0) updatedAt.toString() else "")
        return headers.joinToString(",") + "\n" + row.joinToString(",") { escapeCsv(it) }
    }

    private fun escapeCsv(value: String?): String {
        if (value.isNullOrEmpty()) return ""
        val needs = value.contains('"') || value.contains(',') || value.contains('\n')
        return if (needs) '"' + value.replace("\"", "\"\"") + '"' else value
    }

    private fun buildLogsUrl(paymentId: String?, checkout: String?): String? {
        val project = BuildConfig.FIREBASE_PROJECT_ID
        if (project.isBlank()) return null
        val parts = mutableListOf<String>()
        if (!paymentId.isNullOrBlank()) parts.add("jsonPayload.paymentId=\"$paymentId\"")
        if (!checkout.isNullOrBlank()) parts.add("jsonPayload.checkoutRequestId=\"$checkout\"")
        if (parts.isEmpty()) return null
        val query = java.net.URLEncoder.encode(parts.joinToString(" AND "), "UTF-8")
        return "https://console.cloud.google.com/logs/query;query=$query;project=$project"
    }

    companion object {
        const val EXTRA_RECEIPT = "extra_receipt"
        const val EXTRA_CHECKOUT = "extra_checkout"
        const val EXTRA_MERCHANT = "extra_merchant"
        const val EXTRA_PAYMENT_ID = "extra_payment_id"
        const val EXTRA_PLAN = "extra_plan"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_UPDATED_AT = "extra_updated_at"
    }
}

@Composable
fun ReceiptDetailsScreen(
    receipt: String?,
    checkout: String?,
    merchant: String?,
    plan: String?,
    amount: Int,
    onShareText: () -> Unit,
    onShareCsv: () -> Unit,
    onViewLogs: () -> Unit
) {
    Column(Modifier.padding(16.dp)) {
        Text(text = "Payment Successful", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        if (!receipt.isNullOrBlank()) Text(text = "Receipt: $receipt")
        if (!checkout.isNullOrBlank()) Text(text = "Checkout ID: $checkout")
        if (!merchant.isNullOrBlank()) Text(text = "Merchant ID: $merchant")
        if (!plan.isNullOrBlank()) Text(text = "Plan: $plan")
        if (amount > 0) Text(text = "Amount: KES $amount")
        Spacer(Modifier.height(16.dp))
        Row {
            Button(onClick = onShareText) { Text("Share Receipt") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onShareCsv) { Text("Share CSV") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onViewLogs) { Text("View Logs") }
        }
    }
}
