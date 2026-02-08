package com.platisa.app.core.common

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import kotlinx.coroutines.tasks.await

object PdfUtils {

    private var isInitialized = false
    private val barcodeScanner by lazy { BarcodeScanning.getClient() }

    fun init(context: Context) {
        if (!isInitialized) {
            PDFBoxResourceLoader.init(context)
            isInitialized = true
        }
    }

    fun extractText(file: File): String {
        var document: PDDocument? = null
        return try {
            document = PDDocument.load(file)
            val stripper = PDFTextStripper()
            stripper.sortByPosition = true
            stripper.suppressDuplicateOverlappingText = false
            stripper.getText(document)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        } finally {
            try { document?.close() } catch (e: Exception) {}
        }
    }

    fun renderToBitmap(file: File, pageIndex: Int = 0): Bitmap? {
        // High-Reliability Strategy:
        // 1. Try Native Android Renderer (Fast, Low Memory)
        // 2. If it fails (or returns null), Fallback to PDFBox (Robust, High Memory)
        
        var bitmap = try {
            android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY).use { fileDescriptor ->
                renderToBitmap(fileDescriptor, pageIndex)
            }
        } catch (e: Exception) {
            android.util.Log.w("PdfUtils", "Native PDF Render failed: ${e.message}, trying PDFBox fallback...")
            null
        }

        if (bitmap == null) {
            android.util.Log.d("PdfUtils", "Fallback to PDFBox for: ${file.name}")
            bitmap = renderToBitmapPdfBox(file, pageIndex)
        }
        
        return bitmap
    }
    
    private fun renderToBitmapPdfBox(file: File, pageIndex: Int): Bitmap? {
        var document: PDDocument? = null
        return try {
            document = PDDocument.load(file)
            if (pageIndex >= document.numberOfPages) {
                return null
            }
            
            val renderer = PDFRenderer(document)
            // Scale: 2.0f ~ 144 DPI, 3.0f ~ 216 DPI. Using 2.0f to be safe on memory while good for OCR.
            val bitmap = renderer.renderImage(pageIndex, 2.0f, com.tom_roush.pdfbox.rendering.ImageType.RGB)
            
            // Ensure ARGB_8888 for ML Kit consistency
            if (bitmap.config != Bitmap.Config.ARGB_8888) {
                val argb = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                bitmap.recycle()
                argb
            } else {
                bitmap
            }
        } catch (e: OutOfMemoryError) {
             android.util.Log.e("PdfUtils", "OOM in PDFBox render", e)
             System.gc()
             null
        } catch (e: Exception) {
            android.util.Log.e("PdfUtils", "PDFBox Render error", e)
            null
        } finally {
            try { document?.close() } catch (e: Exception) {}
        }
    }

    fun renderToBitmap(fileDescriptor: android.os.ParcelFileDescriptor, pageIndex: Int = 0): Bitmap? {
        var pdfRenderer: android.graphics.pdf.PdfRenderer? = null
        var page: android.graphics.pdf.PdfRenderer.Page? = null
        return try {
            pdfRenderer = android.graphics.pdf.PdfRenderer(fileDescriptor)
            
            if (pageIndex >= pdfRenderer.pageCount) {
                return null
            }
            
            page = pdfRenderer.openPage(pageIndex)
            val width = 1660
            val height = 2340
            
            // Optimization: Use ARGB_8888 for better ML Kit compatibility
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE) 
            
            page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } catch (e: OutOfMemoryError) {
            android.util.Log.e("PdfUtils", "OOM in renderToBitmap", e)
            System.gc()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try { page?.close() } catch (e: Exception) {}
            try { pdfRenderer?.close() } catch (e: Exception) {}
        }
    }
    
    suspend fun extractQrCode(file: File, pageIndex: Int = 0): String? {
        var fileDescriptor: android.os.ParcelFileDescriptor? = null
        var pdfRenderer: android.graphics.pdf.PdfRenderer? = null
        
        return try {
            android.util.Log.d("PdfUtils", "Extracting QR from PDF (Native): ${file.name}")
            
            fileDescriptor = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = android.graphics.pdf.PdfRenderer(fileDescriptor)
            val pageCount = pdfRenderer.pageCount
            
            val pagesToTry = if (pageCount > 1) listOf(0, 1) else listOf(0)
            
            var foundQr: String? = null
            
            for (pageIdx in pagesToTry) {
                var page: android.graphics.pdf.PdfRenderer.Page? = null
                var bitmap: Bitmap? = null
                try {
                    page = pdfRenderer.openPage(pageIdx)
                    
                    val width = 1660
                    val height = 2340
                    
                    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    
                    page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    page = null // mark as closed
                    
                    android.util.Log.d("PdfUtils", "Rendered native page $pageIdx ($width x $height)")
                    
                    val result = scanBitmapWithRetries(bitmap)
                    if (result != null) {
                        android.util.Log.d("PdfUtils", "✅ QR FOUND via Native Renderer!")
                        foundQr = result
                        break
                    }
                    
                    android.util.Log.d("PdfUtils", "Trying Manual Binarization...")
                    val binarized = binarizeBitmap(bitmap)
                    val binarizedResult = scanBitmapWithRetries(binarized)
                    binarized.recycle()
                    
                    if (binarizedResult != null) {
                        android.util.Log.d("PdfUtils", "✅ QR FOUND via Native Renderer + Binarization!")
                        foundQr = binarizedResult
                        break
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PdfUtils", "Error on page $pageIdx", e)
                } finally {
                    try { page?.close() } catch (e: Exception) {}
                    try { bitmap?.recycle() } catch (e: Exception) {}
                }
            }
            foundQr
        } catch (e: OutOfMemoryError) {
            android.util.Log.e("PdfUtils", "OOM extracting QR code. Attempting to clear memory.", e)
            System.gc()
            null
        } catch (e: Exception) {
            android.util.Log.e("PdfUtils", "Error extracting QR code", e)
            null
        } finally {
            try { pdfRenderer?.close() } catch (e: Exception) {}
            try { fileDescriptor?.close() } catch (e: Exception) {}
        }
    }
    
    private fun binarizeBitmap(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
            // Optimization: Use ARGB_8888 for better ML Kit compatibility
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Simple thresholding
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            // Luminance formula
            val luma = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            
            // Threshold at 128
            pixels[i] = if (luma < 128) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
        
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private suspend fun scanBitmapWithRetries(bitmap: Bitmap): String? {
        // 1. Try Normal
        var result = scanWithMlKit(bitmap) ?: scanBitmap(bitmap)
        if (result != null) return result
        
        // 2. Try Inverted (Negative)
        // Only try inversion if normal failed
        // (Skipping rotation for now to save memory/time unless absolutely needed)
        return null
    }
    
    private suspend fun scanWithMlKit(bitmap: Bitmap): String? {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val barcodes = barcodeScanner.process(image).await()
            barcodes.firstOrNull()?.rawValue
        } catch (e: Exception) {
            null
        }
    }
    
    private fun scanBitmap(bitmap: Bitmap): String? {
        try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            
            val source = com.google.zxing.RGBLuminanceSource(width, height, pixels)
            
            // Try HybridBinarizer
            var binaryBitmap = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source))
            var reader = com.google.zxing.MultiFormatReader()
            var hints = mapOf(
                com.google.zxing.DecodeHintType.POSSIBLE_FORMATS to listOf(
                    com.google.zxing.BarcodeFormat.QR_CODE,
                    com.google.zxing.BarcodeFormat.DATA_MATRIX
                ),
                com.google.zxing.DecodeHintType.TRY_HARDER to true
            )
            reader.setHints(hints)
            
            try {
                return reader.decode(binaryBitmap).text
            } catch (e: Exception) {
                // Try GlobalHistogramBinarizer
                try {
                    binaryBitmap = com.google.zxing.BinaryBitmap(com.google.zxing.common.GlobalHistogramBinarizer(source))
                    return reader.decode(binaryBitmap).text
                } catch (e2: Exception) {
                    return null
                }
            }
        } catch (e: OutOfMemoryError) {
             android.util.Log.e("PdfUtils", "OOM in scanBitmap (ZXing)", e)
             return null
        } catch (e: Exception) {
            return null
        }
    }
}

