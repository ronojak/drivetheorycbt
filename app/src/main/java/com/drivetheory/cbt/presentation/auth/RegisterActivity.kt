package com.drivetheory.cbt.presentation.auth

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import com.drivetheory.cbt.R
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch



@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {
    private val vm: RegisterViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        val name = findViewById<EditText>(R.id.name)
        val phone = findViewById<EditText>(R.id.phone)
        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        findViewById<Button>(R.id.btn_register).setOnClickListener {
            vm.register(name.text.toString().trim(), phone.text.toString().trim(), email.text.toString().trim(), password.text.toString())
        }
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                vm.state.collect { st ->
                        if (st.success) {
                            Toast.makeText(this@RegisterActivity, "Check email for verification", Toast.LENGTH_LONG).show()
                            finish()
                        } else if (st.error != null) {
                            Toast.makeText(this@RegisterActivity, st.error, Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }
    }
}
