package com.example.farm2fork_sellers

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity

class UserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)

        val userEmail = intent.getStringExtra("USER_EMAIL")
        val emailTextView: TextView = findViewById(R.id.emailTextView)
        emailTextView.text = userEmail
    }
}