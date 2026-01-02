package com.platisa.app.core.common

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.IOException

/**
 * OCR Manager using ML Kit text recognition.
 * The default Latin text recognizer also supports Cyrillic scripts (Russian, Bulgarian, Serbian, etc.)
 * according to ML Kit documentation. 
 * 
 * For best results with Serbian Cyrillic text, we rely on:
 * 1. The built-in multi-script support of ML Kit
 * 2. Post-processing normalization in EpsParser to fix common OCR mistakes
 */
object OcrManager {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun processImage(context: Context, imageUri: Uri): String {
        return try {
            // Check if it's a PDF
            val path = imageUri.path?.lowercase()
            if (path != null && path.endsWith(".pdf")) {
                val file = java.io.File(imageUri.path!!)
                if (file.exists()) {
                    // Strategy 1: Direct Text Extraction (Fastest & Most Accurate for Digital PDFs)
                    val extractedText = PdfUtils.extractText(file) // Uses PDFBox text stripper
                    if (extractedText.length > 50) {
                         android.util.Log.d("OcrManager", "✅ Text extracted directly from PDF (${extractedText.length} chars)")
                         return extractedText
                    }

                    // Strategy 2: Rasterize & OCR (Fallback for Scanned PDFs)
                    // Now uses hybrid Native -> PDFBox fallback in PdfUtils
                    android.util.Log.d("OcrManager", "Processing PDF via OCR (Direct text extraction was empty/insufficient)")
                    val bitmap = PdfUtils.renderToBitmap(file)
                    if (bitmap != null) {
                        val image = InputImage.fromBitmap(bitmap, 0)
                        val result = recognizer.process(image).await()
                        val text = result.text
                        bitmap.recycle()
                        
                        // Log OCR result length for debugging
                        android.util.Log.d("OcrManager", "OCR result length: ${text.length} characters")
                        return text
                    }
                }
            }
            
            // Standard image OCR
            val image = InputImage.fromFilePath(context, imageUri)
            val result = recognizer.process(image).await()
            val text = result.text
            
            // Log OCR result length for debugging
            android.util.Log.d("OcrManager", "OCR result length: ${text.length} characters")
            text
        } catch (e: IOException) {
            android.util.Log.e("OcrManager", "IO Error during OCR", e)
            ""
        } catch (e: Exception) {
            android.util.Log.e("OcrManager", "Error during OCR", e)
            ""
        }
    }
}

