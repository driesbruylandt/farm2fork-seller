package com.example.farm2fork_sellers

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.util.Log
import org.json.JSONObject
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.cUrlString
import com.github.kittinunf.result.Result
import androidx.appcompat.app.AppCompatActivity

class MainActivity : ComponentActivity() {

    private lateinit var previewView : PreviewView
    private lateinit var resultTextView : TextView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    private var isRequestInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
            setContentView(R.layout.activity_main)
            Log.d("MainActivity", "MainActivity started successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onCreate", e)
        }

        previewView = findViewById(R.id.previewView)
        resultTextView = findViewById(R.id.resultTextView)
        cameraExecutor = Executors.newSingleThreadExecutor()
        barcodeScanner = BarcodeScanning.getClient()


        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                resultTextView.text = "Camera permission denied"
            }
        }
        requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        val screenSize = android.util.Size(1280, 720)
        val resolutionSelector = ResolutionSelector.Builder().setResolutionStrategy(
            ResolutionStrategy(screenSize, ResolutionStrategy.FALLBACK_RULE_NONE)
        ).build()

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().setResolutionSelector(resolutionSelector).build().also{
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also{
                    it.setAnalyzer(cameraExecutor, {imageProxy ->
                        processImageProxy(imageProxy)
                })}
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val image = imageProxy.image
        if (image != null) {
            val image = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        resultTextView.text = barcode.rawValue
                        sendBarcodeToServer(barcode.rawValue)
                    }
                }
                .addOnFailureListener {
                    resultTextView.text = "Failed to scan qrcode"
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }

        }
    }

    private fun saveUserData(userId: Int, userName: String, userEmail: String) {
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("USER_ID", userId)
        editor.putString("USER_NAME", userName)
        editor.putString("USER_EMAIL", userEmail)
        editor.apply()
    }

    private fun sendBarcodeToServer(barcode: String?) {
        if (isRequestInProgress) {
            return
        }
        isRequestInProgress = true
        val url = "http://10.0.2.2:8000/api/verify-qr-token"
        val requestBody = mapOf("token" to barcode)
        Log.d("MainActivity", "Sending request to $url with barcode: $barcode and request body: $requestBody")

        Fuel.post(url)
            .header("Content-Type" to "application/json")
            .body(JSONObject(requestBody).toString())
            .responseString { request, response, result ->
                Log.d("MainActivity", "Request: ${request.cUrlString()}")
                Log.d("MainActivity", "Response: ${response.statusCode} ${response.responseMessage}")
                Log.d("MainActivity", "Response body: ${response.body().asString("application/json")}") // Log the response body
                isRequestInProgress = false
                when (result) {
                    is Result.Failure -> {
                        Log.d("MainActivity", "Request failed: ${result.error}")
                        Log.d("MainActivity", "Failed to send barcode to server", result.getException())
                        resultTextView.text = "Token is invalid"
                    }
                    is Result.Success -> {
                        Log.d("MainActivity", "Request succeeded: ${result.value}")
                        val responseString = result.get()
                        try {
                            Log.d("MainActivity", "Start Pairing; ${responseString}")
                            val jsonResponse = JSONObject(responseString)
                            Log.d("MainActivity", "Received response: $jsonResponse")

                            val status = jsonResponse.getString("status")
                            if (status == "success") {
                                val user = jsonResponse.getJSONObject("user")
                                val userId = user.getInt("id")
                                val userName = user.getString("name")
                                val userEmail = user.getString("email")
                                saveUserData(userId, userName, userEmail)
                                val intent = Intent(this, UserActivity::class.java)
                                startActivity(intent)
                            } else {
                                val message = jsonResponse.getString("message")
                                resultTextView.text = "Error: $message"
                            }
                        } catch (e: Exception) {
                            Log.d("MainActivity", "Error parsing JSON response", e)
                            resultTextView.text = "Error parsing response"
                        }
                    }
                }
            }
    }
}