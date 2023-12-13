package com.stuti.mlkitsample

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TextAnalyser(var context: Context, var coroutineScope: CoroutineScope) : ImageAnalysis.Analyzer {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        val inputImage = InputImage.fromMediaImage(
            image.image!!,
            image.imageInfo.rotationDegrees
        )
        coroutineScope.launch {
            val result = recognizer.process(inputImage).await()
            println("The text is - ${result.text}")
            image.close()
        }
    }
}