package com.stuti.mlkitsample

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.Image
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.stuti.mlkitsample.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var textAnalyser: TextAnalyser
    var last = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
        textAnalyser = TextAnalyser(this, CoroutineScope(Dispatchers.Default))
        requestPermission()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                textAnalyser.getResultFlow().onEach {
                    //println("Intermediate result: $it")
                }.collect { lastResult ->
                    last = lastResult
                    println("Last Result: $lastResult")
                }

                /*

                if (lastResult.isNotEmpty()) {
                    // Get the last element from the list
                    val lastElement = lastResult.last()
                    Toast.makeText(this@MainActivity, "text - $lastElement", Toast.LENGTH_LONG).show()
                    println("Last result received in FlowConsumer: $lastElement")
                } else {
                    // Handle the case where the flow is empty
                    println("Flow is empty")
                }

*/
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }

        }

        binding.btnCapture.setOnClickListener {
            Toast.makeText(this@MainActivity, "last text - $last", Toast.LENGTH_LONG).show()
        }

    }

    private fun requestPermission() {
        requestCameraPermissionIfMissing { granted ->
            if (granted)
                startCamera()
            else
                Toast.makeText(this, "Please allow permission", Toast.LENGTH_SHORT).show()

        }
    }


    private fun startCamera() {
        val processCameraProvider = ProcessCameraProvider.getInstance(this)
        processCameraProvider.addListener({
            try {
                val cameraProvider = processCameraProvider.get()
                val previewUseCase = buildPreviewUseCase()
                val imageAnalysisUseCase = buildImageAnalysisUseCase()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    previewUseCase, imageAnalysisUseCase
                )
            } catch (e: Exception) {
                Log.d("MainActivity", e.message.toString())
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun buildImageAnalysisUseCase(): ImageAnalysis {
        return ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build().also {
                it.setAnalyzer(cameraExecutor, textAnalyser)
            }
    }

    private fun buildPreviewUseCase(): Preview {
        return Preview.Builder().build()
            .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }
    }

    private fun requestCameraPermissionIfMissing(onResult: (Boolean) -> Unit) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
            onResult(true)
        else
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                onResult(it)
            }.launch(Manifest.permission.CAMERA)
    }
}