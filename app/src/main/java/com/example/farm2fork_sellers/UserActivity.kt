package com.example.farm2fork_sellers

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.github.kittinunf.fuel.Fuel
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import com.example.farm2fork_sellers.utils.Data.Product
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class UserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)
        createNotificationChannel()
        requestNotificationPermission()
        getUserData()
        val rabbitMQConsumer = RabbitMQConsumer(this)
        rabbitMQConsumer.startConsuming()
        val refreshButton: Button = findViewById(R.id.refreshButton)
        refreshButton.setOnClickListener {
            val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
            val userId = sharedPreferences.getInt("USER_ID", -1)
            if (userId != -1) {
                fetchUserProducts(userId)
            }
        }
    }

    private fun getUserData() {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userId = sharedPreferences.getInt("USER_ID", -1)
        val userName = sharedPreferences.getString("USER_NAME", null)
        val userEmail = sharedPreferences.getString("USER_EMAIL", null)

        if (userId != -1 && userName != null && userEmail != null) {
            // Use the user data
            val emailTextView: TextView = findViewById(R.id.emailTextView)
            emailTextView.text = userEmail
            fetchUserProducts(userId)
        } else {
            // Handle the case where user data is not available
        }
    }

    private fun fetchUserProducts(userId: Int) {
        val url = "http://10.0.2.2:8000/api/products/user/$userId"
        Log.d("UserActivity", "Fetching products from $url")
        Fuel.get(url)
            .responseString { _, _, result ->
                when (result) {
                    is Result.Failure -> {
                        val error = result.getException()
                        Log.d("UserActivity", "Failed to fetch products: $error")
                    }
                    is Result.Success -> {
                        val responseString = result.get()
                        try {
                            val products = Gson().fromJson(responseString, Array<Product>::class.java).toList()
                            runOnUiThread {
                                displayProducts(products)
                            }
                        } catch (e: Exception) {
                            Log.d("UserActivity", "Error parsing JSON response", e)
                        }
                    }
                }
            }
    }

    private fun displayProducts(products: List<Product>) {
        val productsContainer: LinearLayout = findViewById(R.id.productsContainer)
        productsContainer.removeAllViews() // Clear existing views
        products.forEach { product ->
            val productView = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 16, 0, 16)
            }

            val productTextView = TextView(this).apply {
                text = "Name: ${product.name}\nStatus: ${product.status}\nWeight: ${product.weight} kg\nUser ID: ${product.user_id}"
                textSize = 16f
            }

            val toggleButton = Button(this).apply {
                text = "Toggle Status"
                setOnClickListener {
                    toggleProductStatus(product)
                }
            }

            productView.addView(productTextView)
            productView.addView(toggleButton)
            productsContainer.addView(productView)
        }
    }

    private fun toggleProductStatus(product: Product) {
        val newStatus = if (product.status == "inProgress") "Done" else "inProgress"
        val url = "http://10.0.2.2:8000/api/products/${product.id}/status"

        val jsonBody = """
        {
            "status": "$newStatus"
        }
    """.trimIndent()

        Fuel.put(url)
            .body(jsonBody)
            .header("Content-Type" to "application/json")
            .responseString { _, _, result ->
                when (result) {
                    is Result.Failure -> {
                        val error = result.getException()
                        Log.d("UserActivity", "Failed to update product status: $error")
                    }
                    is Result.Success -> {
                        Log.d("UserActivity", "Product status updated successfully")
                        runOnUiThread {
                            // Update the UI to reflect the new status
                            fetchUserProducts(product.user_id)
                        }
                    }
                }
            }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notification Channel"
            val descriptionText = "Channel for app notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("CHANNEL_ID", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }
    }
}