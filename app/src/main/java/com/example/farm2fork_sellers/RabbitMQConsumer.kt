package com.example.farm2fork_sellers

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DeliverCallback
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.json.JSONObject

class RabbitMQConsumer(private val context: Context) {
    private val factory = ConnectionFactory().apply {
        host = "10.0.2.2"
        port = 5672
        username = "guest"
        password = "guest"
    }

    fun startConsuming() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = factory.newConnection()
                val channel = connection.createChannel()
                val queueName = "notification_queue"

                channel.queueDeclare(queueName, false, false, false, null)
                val deliverCallback = DeliverCallback { _, delivery ->
                    val message = String(delivery.body, Charsets.UTF_8)
                    Log.d("RabbitMQConsumer", "Received message: $message")
                    CoroutineScope(Dispatchers.Main).launch {
                        showNotification(message)
                    }
                }
                channel.basicConsume(queueName, true, deliverCallback) { _ -> }
            } catch (e: Exception) {
                Log.e("RabbitMQConsumer", "Error in startConsuming", e)
            }
        }
    }

    private fun showNotification(message: String) {
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            val jsonObject = JSONObject(message)
            val body = jsonObject.getString("body")

            val builder = NotificationCompat.Builder(context, "CHANNEL_ID")
                .setSmallIcon(R.drawable.notification_icon) // Ensure this is a valid drawable resource ID
                .setContentTitle("New Product Created")
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            with(NotificationManagerCompat.from(context)) {
                notify(1, builder.build())
            }
        } else {
            Log.w("RabbitMQConsumer", "Notifications are not enabled")
        }
    }
}