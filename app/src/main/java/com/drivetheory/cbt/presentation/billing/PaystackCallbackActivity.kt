package com.drivetheory.cbt.presentation.billing

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PaystackCallbackActivity : AppCompatActivity() {
    private val vm: SubscriptionViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent?.data
        val ref = data?.getQueryParameter("reference")
        if (ref != null) {
            vm.verifyIfPending()
            Toast.makeText(this, "Verifying payment...", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
