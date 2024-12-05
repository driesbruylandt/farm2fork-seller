package com.example.farm2fork_sellers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.github.kittinunf.fuel.Fuel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.github.kittinunf.result.Result

class NetworkChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        val isConnected = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        if (isConnected) {
            Log.d("NetworkChangeReceiver", "Network is connected")
            processPendingToggles(context)
        } else {
            Log.d("NetworkChangeReceiver", "Network is disconnected")
        }
    }

    private fun getPendingToggles(context: Context): MutableSet<String> {
        val sharedPreferences = context.getSharedPreferences("pendingToggles", Context.MODE_PRIVATE)
        return sharedPreferences.getStringSet("toggles", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
    }

    private fun clearPendingToggles(context: Context) {
        val sharedPreferences = context.getSharedPreferences("pendingToggles", Context.MODE_PRIVATE)
        sharedPreferences.edit().remove("toggles").apply()
    }

    private fun processPendingToggles(context: Context) {
        val pendingToggles = getPendingToggles(context)
        if (pendingToggles.isNotEmpty()) {
            for (toggle in pendingToggles) {
                val (productId, status) = toggle.split(":")
                val url = "http://10.0.2.2:8000/api/products/$productId/status"
                val jsonBody = """
            {
                "status": "$status"
            }
            """.trimIndent()

                Fuel.put(url)
                    .body(jsonBody)
                    .header("Content-Type" to "application/json")
                    .responseString { _, _, result ->
                        result.fold(
                            success = { data ->
                                Log.d("NetworkChangeReceiver", "Product $productId updated successfully: $data")
                                // Remove successfully processed toggle
                                val updatedPendingToggles = getPendingToggles(context)
                                updatedPendingToggles.remove(toggle)
                                val sharedPreferences = context.getSharedPreferences("pendingToggles", Context.MODE_PRIVATE)
                                sharedPreferences.edit().putStringSet("toggles", updatedPendingToggles).apply()
                            },
                            failure = { error ->
                                Log.d("NetworkChangeReceiver", "Failed to update product $productId: ${error.message}")
                            }
                        )
                    }
            }
        }
    }

}