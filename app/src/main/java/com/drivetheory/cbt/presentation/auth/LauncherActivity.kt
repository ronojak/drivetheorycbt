package com.drivetheory.cbt.presentation.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.drivetheory.cbt.presentation.MainActivity
import com.drivetheory.cbt.R

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                startActivity(Intent(this, LoginActivity::class.java))
            } else {
                startActivity(Intent(this, MainActivity::class.java))
            }
        } catch (_: Exception) {
            // If Firebase is not configured yet, fall back to main so the app still opens
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}
