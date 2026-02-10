package com.drivetheory.cbt.presentation.billing

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.drivetheory.cbt.R
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import android.content.Intent
import com.drivetheory.cbt.BuildConfig
import com.drivetheory.cbt.presentation.mpesa.MpesaPaymentActivity
import dagger.hilt.android.AndroidEntryPoint
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat

@AndroidEntryPoint
class SubscriptionActivity : AppCompatActivity() {
    private val vm: SubscriptionViewModel by viewModels()
    private lateinit var btnMonthly: Button
    private lateinit var btnAnnual: Button
    private var selectedPlan: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)
        btnMonthly = findViewById(R.id.btn_monthly)
        btnAnnual = findViewById(R.id.btn_annual)
        btnMonthly.setOnClickListener {
            selectPlan("monthly")
            startPayment("monthly")
        }
        btnAnnual.setOnClickListener {
            selectPlan("annual")
            startPayment("annual")
        }
        val mpesa = findViewById<Button>(R.id.btn_mpesa)
        if (BuildConfig.MPESA_ENABLED) {
            mpesa.setOnClickListener {
                startActivity(Intent(this, MpesaPaymentActivity::class.java))
            }
        } else {
            mpesa.setOnClickListener {
                Toast.makeText(this, "M-PESA temporarily unavailable", Toast.LENGTH_SHORT).show()
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                vm.state.collect { st ->
                    if (st.checkoutUrl != null) {
                        CustomTabsIntent.Builder().build().launchUrl(this@SubscriptionActivity, Uri.parse(st.checkoutUrl))
                    }
                    if (st.verified == true) {
                        Toast.makeText(this@SubscriptionActivity, "Subscription active", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    if (st.error != null) {
                        Toast.makeText(this@SubscriptionActivity, st.error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun startPayment(plan: String) {
        vm.initialize(plan)
    }

    private fun selectPlan(plan: String) {
        selectedPlan = plan
        val check = " ✓"
        btnMonthly.text = if (plan == "monthly") getString(R.string.choose_monthly_label, check) else getString(R.string.choose_monthly_label, "")
        btnAnnual.text = if (plan == "annual") getString(R.string.choose_annual_label, check) else getString(R.string.choose_annual_label, "")

        val selectedTint = ContextCompat.getColor(this, R.color.plan_selected)
        if (plan == "monthly") {
            btnMonthly.backgroundTintList = ColorStateList.valueOf(selectedTint)
            btnAnnual.backgroundTintList = null
        } else {
            btnAnnual.backgroundTintList = ColorStateList.valueOf(selectedTint)
            btnMonthly.backgroundTintList = null
        }
    }
}
