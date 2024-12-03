package com.example.farm2fork_sellers

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.github.kittinunf.fuel.Fuel
import android.util.Log
import android.widget.LinearLayout
import com.example.farm2fork_sellers.utils.Data.Product
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.google.gson.Gson

class UserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)
        getUserData()
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
        products.forEach { product ->
            val productView = TextView(this).apply {
                text = "Name: ${product.name}\nStatus: ${product.status}\nWeight: ${product.weight} kg\nUser ID: ${product.user_id}"
                textSize = 16f
                setPadding(0, 16, 0, 16)
            }
            productsContainer.addView(productView)
        }
    }
}