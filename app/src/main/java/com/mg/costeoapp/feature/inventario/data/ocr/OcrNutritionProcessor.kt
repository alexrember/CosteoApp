package com.mg.costeoapp.feature.inventario.data.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class OcrNutritionProcessor(
    private val context: Context,
    private val parser: NutritionLabelParser = NutritionLabelParser()
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun processImage(imageUri: Uri): NutricionOcrResult? {
        return try {
            val image = InputImage.fromFilePath(context, imageUri)
            val textResult = recognizeText(image)
            if (textResult != null && textResult.text.isNotBlank()) {
                parser.parse(textResult)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun recognizeText(
        image: InputImage
    ): com.google.mlkit.vision.text.Text? = suspendCancellableCoroutine { cont ->
        recognizer.process(image)
            .addOnSuccessListener { text -> cont.resume(text) }
            .addOnFailureListener { cont.resume(null) }
    }

    fun close() {
        recognizer.close()
    }
}
