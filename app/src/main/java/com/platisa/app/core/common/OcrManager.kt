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
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

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

                         // EPS PDFs often have page-2 as scanned image (no selectable text).
                         // If base-cost line is missing, OCR page 2 and append.
                         val isEps = listOf("EPS", "ELEKTROPRIVREDA", "ЕПС", "ЕЛЕКТРОПРИВРЕДА")
                             .any { extractedText.contains(it, ignoreCase = true) }
                         val hasBaseLine = Regex(
                             """(ZADU[ŽZ]ENJE\s+ZA\s+ELEKTRI[ČC]NU\s+ENERGIJU\s+U\s+OBRA[ČC]UNSKOM\s+PERIODU|ЗАДУЖЕЊЕ\s+ЗА\s+ЕЛЕКТРИЧНУ\s+ЕНЕРГИЈУ\s+У\s+ОБРАЧУНСКОМ\s+ПЕРИОДУ)""",
                             RegexOption.IGNORE_CASE
                         ).containsMatchIn(extractedText)

                         if (isEps && !hasBaseLine) {
                             val page2 = PdfUtils.renderToBitmap(file, 1)
                             if (page2 != null) {
                                 try {
                                     val image = InputImage.fromBitmap(page2, 0)
                                     val result = recognizer.process(image).await()
                                     val page2Text = result.text
                                     if (page2Text.isNotBlank()) {
                                         android.util.Log.d("OcrManager", "✅ EPS page-2 OCR length: ${page2Text.length} characters")
                                         return extractedText + "\n\n" + page2Text
                                     }
                                 } finally {
                                     page2.recycle()
                                 }
                             }
                         }

                         return extractedText
                    }

                    // Strategy 2: Rasterize & OCR (Fallback for Scanned PDFs)
                    // Try first two pages (page 2 often contains EPS base-cost table).
                    android.util.Log.d("OcrManager", "Processing PDF via OCR (Direct text extraction was empty/insufficient)")
                    val combined = StringBuilder()
                    val pagesToTry = listOf(0, 1)
                    for (pageIndex in pagesToTry) {
                        val bitmap = PdfUtils.renderToBitmap(file, pageIndex)
                        if (bitmap != null) {
                            try {
                                val image = InputImage.fromBitmap(bitmap, 0)
                                val result = recognizer.process(image).await()
                                val text = result.text
                                if (text.isNotBlank()) {
                                    if (combined.isNotEmpty()) combined.append("\n\n")
                                    combined.append(text)
                                }
                                android.util.Log.d("OcrManager", "OCR page $pageIndex length: ${text.length} characters")
                            } finally {
                                bitmap.recycle()
                            }
                        }
                    }
                    if (combined.isNotBlank()) return combined.toString()
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

