package com.drivetheory.cbt.presentation.account

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.drivetheory.cbt.R
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import android.content.Intent
import com.drivetheory.cbt.presentation.billing.SubscriptionActivity
import dagger.hilt.android.AndroidEntryPoint
import com.drivetheory.cbt.presentation.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {
    private val vm: ProfileViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        // Ensure a logged-in user exists; otherwise redirect to login
        val current = try { FirebaseAuth.getInstance().currentUser } catch (_: Exception) { null }
        if (current == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        val name = findViewById<EditText>(R.id.name)
        val emailView = findViewById<TextView>(R.id.email)
        val phone = findViewById<EditText>(R.id.phone)
        val sub = findViewById<TextView>(R.id.subscription)
        findViewById<Button>(R.id.btn_save).setOnClickListener {
            vm.save(name.text.toString().trim(), phone.text.toString().trim())
        }
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                vm.state.collect { st ->
                        name.setText(st.name ?: "")
                        emailView.text = st.email ?: current.email ?: ""
                        phone.setText(st.phone ?: "")
                        sub.text = "Subscription: ${st.subscriptionStatus ?: "Free"}"
                        st.toast?.let { Toast.makeText(this@ProfileActivity, it, Toast.LENGTH_SHORT).show() }
                    }
            }
        }
        vm.load()

        findViewById<Button>(R.id.btn_go_premium).setOnClickListener {
            startActivity(Intent(this, SubscriptionActivity::class.java))
        }
    }
}
