package com.drivetheory.cbt.presentation.auth

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.drivetheory.cbt.R
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        val email = findViewById<EditText>(R.id.email)
        findViewById<Button>(R.id.btn_reset).setOnClickListener {
            FirebaseAuth.getInstance().sendPasswordResetEmail(email.text.toString().trim())
                .addOnSuccessListener { Toast.makeText(this, "Reset email sent", Toast.LENGTH_SHORT).show() }
                .addOnFailureListener { Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show() }
        }
    }
}
