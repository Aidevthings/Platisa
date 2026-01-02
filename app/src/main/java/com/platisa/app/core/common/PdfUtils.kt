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

    fun init(context: Context) {
        if (!isInitialized) {
            PDFBoxResourceLoader.init(context)
            isInitialized = true
        }
    }

    fun extractText(file: File): String {
        return try {
            val document = PDDocument.load(file)
            val stripper = PDFTextStripper()
            // CRITICAL FIX: Sort text by visual position (Top-Down, Left-Right)
            // This prevents "stream order" scrambling where text appears out of layout context.
            stripper.sortByPosition = true
            // Also helpful for messy PDFs
            stripper.suppressDuplicateOverlappingText = false
            
            val text = stripper.getText(document)
            document.close()
            text
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun renderToBitmap(file: File, pageIndex: Int = 0): Bitmap? {
        // High-Reliability Strategy:
        // 1. Try Native Android Renderer (Fast, Low Memory)
        // 2. If it fails (or returns null), Fallback to PDFBox (Robust, High Memory)
        
        var bitmap = try {
            val fileDescriptor = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            val result = renderToBitmap(fileDescriptor, pageIndex)
            fileDescriptor.close()
            result
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
        return try {
            val document = PDDocument.load(file)
            if (pageIndex >= document.numberOfPages) {
                document.close()
                return null
            }
            
            val renderer = PDFRenderer(document)
            // Scale: 2.0f ~ 144 DPI, 3.0f ~ 216 DPI. Using 2.0f to be safe on memory while good for OCR.
            val bitmap = renderer.renderImage(pageIndex, 2.0f, com.tom_roush.pdfbox.rendering.ImageType.RGB)
            document.close()
            
            // Ensure ARGB_8888 for ML Kit consistency
            if (bitmap.config != Bitmap.Config.ARGB_8888) {
                val argb = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                bitmap.recycle()
                return argb
            }
            bitmap
        } catch (e: OutOfMemoryError) {
             android.util.Log.e("PdfUtils", "OOM in PDFBox render", e)
             System.gc()
             null
        } catch (e: Exception) {
            android.util.Log.e("PdfUtils", "PDFBox Render error", e)
            null
        }
    }

    fun renderToBitmap(fileDescriptor: android.os.ParcelFileDescriptor, pageIndex: Int = 0): Bitmap? {
        return try {
            val pdfRenderer = android.graphics.pdf.PdfRenderer(fileDescriptor)
            
            if (pageIndex >= pdfRenderer.pageCount) {
                pdfRenderer.close()
                return null
            }
            
            val page = pdfRenderer.openPage(pageIndex)
            val width = 1660
            val height = 2340
            
            // Optimization: Use ARGB_8888 for better ML Kit compatibility
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE) 
            
            page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            
            page.close()
            pdfRenderer.close()
            
            bitmap
        } catch (e: OutOfMemoryError) {
            android.util.Log.e("PdfUtils", "OOM in renderToBitmap", e)
            System.gc()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun extractQrCode(file: File, pageIndex: Int = 0): String? {
        return try {
            android.util.Log.d("PdfUtils", "Extracting QR from PDF (Native): ${file.name}")
            
            // STRATEGY: Use Android Native PdfRenderer
            // Optimization: Reduce resolution to ~200 DPI (approx 1660x2340)
            // Memory: 1660 * 2340 * 2 bytes (RGB_565) = ~7.7 MB (was ~35 MB at 300 DPI)
            
            val fileDescriptor = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = android.graphics.pdf.PdfRenderer(fileDescriptor)
            val pageCount = pdfRenderer.pageCount
            
            val pagesToTry = if (pageCount > 1) listOf(0, 1) else listOf(0)
            
            for (pageIdx in pagesToTry) {
                try {
                    val page = pdfRenderer.openPage(pageIdx)
                    
                    // Render at medium-high resolution (~200 DPI)
                    // At 200 DPI -> 1660 x 2340 pixels
                    val width = 1660
                    val height = 2340
                    
                    // Optimization: Use ARGB_8888 for better ML Kit compatibility
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    canvas.drawColor(android.graphics.Color.WHITE) // Ensure white background
                    
                    // Render
                    page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close() // Close page immediately after render
                    
                    android.util.Log.d("PdfUtils", "Rendered native page $pageIdx ($width x $height)")
                    
                    // 1. Try scanning the full page (Normal)
                    var result = scanBitmapWithRetries(bitmap)
                    if (result != null) {
                        android.util.Log.d("PdfUtils", "✅ QR FOUND via Native Renderer!")
                        fileDescriptor.close()
                        bitmap.recycle()
                        return result
                    }
                    
                    // 2. Try scanning with MANUAL BINARIZATION (Thresholding)
                    // This removes anti-aliasing gray pixels
                    android.util.Log.d("PdfUtils", "Trying Manual Binarization...")
                    val binarized = binarizeBitmap(bitmap)
                    result = scanBitmapWithRetries(binarized)
                    binarized.recycle()
                    bitmap.recycle()
                    
                    if (result != null) {
                        android.util.Log.d("PdfUtils", "✅ QR FOUND via Native Renderer + Binarization!")
                        fileDescriptor.close()
                        return result
                    }
                    
                } catch (e: Exception) {
                    android.util.Log.e("PdfUtils", "Error on page $pageIdx", e)
                }
            }
            
            pdfRenderer.close()
            fileDescriptor.close()
            android.util.Log.w("PdfUtils", "No QR code found")
            null
        } catch (e: OutOfMemoryError) {
            android.util.Log.e("PdfUtils", "OOM extracting QR code. Attempting to clear memory.", e)
            System.gc()
            null
        } catch (e: Exception) {
            android.util.Log.e("PdfUtils", "Error extracting QR code", e)
            null
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
            val scanner = BarcodeScanning.getClient()
            val barcodes = scanner.process(image).await()
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

