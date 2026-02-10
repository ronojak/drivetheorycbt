package com.drivetheory.cbt.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import com.drivetheory.cbt.R
import com.drivetheory.cbt.presentation.MainActivity





@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private val vm: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        findViewById<Button>(R.id.btn_login).setOnClickListener {
            vm.login(email.text.toString().trim(), password.text.toString())
        }
        findViewById<Button>(R.id.btn_register).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        findViewById<Button>(R.id.btn_forgot).setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        lifecycle.addObserver(LoginObserver(vm) { success, error ->
            if (success) {
                Toast.makeText(this, "Login success", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        })
    }
}

