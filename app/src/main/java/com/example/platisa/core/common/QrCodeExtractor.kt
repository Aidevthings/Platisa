package com.example.platisa.core.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.tasks.await
import java.io.File
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.DecodeHintType

object QrCodeExtractor {
    
    // ML Kit scanner - NO FORMAT RESTRICTION
    // This will scan ALL barcode types to identify what we're dealing with
    private val scanner = BarcodeScanning.getClient()  // Default scans ALL formats
    
    // For debugging: Also create a restricted scanner to compare
    private val restrictedScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_PDF417,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_AZTEC
            )
            .build()
    )
    
    // Accept ALL 2D barcode formats
    private val ACCEPTED_FORMATS = setOf(
        Barcode.FORMAT_QR_CODE,
        Barcode.FORMAT_PDF417,
        Barcode.FORMAT_DATA_MATRIX,
        Barcode.FORMAT_AZTEC,
        Barcode.FORMAT_CODE_128,
        Barcode.FORMAT_CODE_39,
        Barcode.FORMAT_CODE_93,
        Barcode.FORMAT_EAN_13,
        Barcode.FORMAT_EAN_8,
        Barcode.FORMAT_ITF,
        Barcode.FORMAT_UPC_A,
        Barcode.FORMAT_UPC_E,
        Barcode.FORMAT_CODABAR
    )
    
    /**
     * Extract QR code data from an image file or URI.
     * Returns the first QR code found, or null if none found.
     */
    suspend fun extractQrCode(imagePathOrUri: String, context: Context? = null): String? {
        android.util.Log.d("QrCodeExtractor", "========== EXTRACTION START ==========")
        android.util.Log.d("QrCodeExtractor", "Input: $imagePathOrUri")
        
        return try {
            // ========== STEP 0: Try ML Kit on RESIZED image (workaround for large bitmap bug) ==========
            // December 2024 bug: ML Kit fails on large bitmaps. Resize to max 800px.
            if (imagePathOrUri.startsWith("content://") && context != null) {
                android.util.Log.d("QrCodeExtractor", "Step 0: ML Kit on RESIZED image (large bitmap workaround)...")
                try {
                    val uri = Uri.parse(imagePathOrUri)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val originalBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    
                    if (originalBitmap != null) {
                        android.util.Log.d("QrCodeExtractor", "   Original size: ${originalBitmap.width}x${originalBitmap.height}")
                        
                        // Try multiple resize sizes (bug workaround)
                        for (maxDim in listOf(256, 400, 512, 800)) {
                            val scale = minOf(maxDim.toFloat() / originalBitmap.width, maxDim.toFloat() / originalBitmap.height, 1f)
                            if (scale >= 1f) continue  // Skip if image is already smaller
                            
                            val newWidth = (originalBitmap.width * scale).toInt()
                            val newHeight = (originalBitmap.height * scale).toInt()
                            android.util.Log.d("QrCodeExtractor", "   Trying resize: ${newWidth}x${newHeight}")
                            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
                        
                            val inputImage = InputImage.fromBitmap(resizedBitmap, 0)
                            val barcodes = scanner.process(inputImage).await()
                            android.util.Log.d("QrCodeExtractor", "   ML Kit (${newWidth}x${newHeight}) found ${barcodes.size} barcode(s)")
                            
                            for (bc in barcodes) {
                                val formatName = when(bc.format) {
                                    Barcode.FORMAT_QR_CODE -> "QR_CODE"
                                    Barcode.FORMAT_PDF417 -> "PDF417"
                                    Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
                                    else -> "OTHER(${bc.format})"
                                }
                                android.util.Log.d("QrCodeExtractor", "   → Barcode: format=$formatName, value=${bc.rawValue?.take(50)}...")
                            }
                            
                            val result = barcodes.firstOrNull()?.rawValue
                            if (result != null) {
                                android.util.Log.d("QrCodeExtractor", "✅ ML Kit RESIZED (${newWidth}x${newHeight}) SUCCESS!")
                                return result
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("QrCodeExtractor", "   ML Kit resized failed: ${e.message}")
                }
            }
            
            // ========== STEP 1: Load Bitmap manually ==========
            val bitmap = when {
                // File path
                imagePathOrUri.startsWith("/") -> {
                    android.util.Log.d("QrCodeExtractor", "Loading from file path...")
                    BitmapFactory.decodeFile(imagePathOrUri)
                }
                // Content URI (from camera)
                imagePathOrUri.startsWith("content://") && context != null -> {
                    android.util.Log.d("QrCodeExtractor", "Loading from content URI...")
                    loadBitmapFromUri(context, Uri.parse(imagePathOrUri))
                }
                else -> {
                    android.util.Log.e("QrCodeExtractor", "❌ Unknown URI format or missing context!")
                    null
                }
            }
            
            if (bitmap == null) {
                android.util.Log.e("QrCodeExtractor", "❌ BITMAP IS NULL - Failed to load image!")
                return null
            }
            
            android.util.Log.d("QrCodeExtractor", "✓ Bitmap loaded: ${bitmap.width}x${bitmap.height}")
            
            // 1. Try ZXing on ORIGINAL (Hybrid + Global)
            android.util.Log.d("QrCodeExtractor", "Step 1: ZXing on ORIGINAL...")
            var result = decodeWithZxing(bitmap)
            if (result != null) {
                android.util.Log.d("QrCodeExtractor", "✅ ZXing ORIGINAL SUCCESS: ${result.take(50)}...")
                return result
            }
            android.util.Log.d("QrCodeExtractor", "   ZXing on ORIGINAL: No match")
            
            // 2. Try ZXing on BOTTOM HALF (Hybrid + Global) - optimization for long receipts
            if (bitmap.height > bitmap.width * 1.2) {
                // Only if it's a tall image
                android.util.Log.d("QrCodeExtractor", "⚠️ Trying Bottom Half Crop...")
                var cropped = cropBottomHalf(bitmap)
                
                // 2a. Try ZXing on Crop
                result = decodeWithZxing(cropped)
                if (result != null) {
                    android.util.Log.d("QrCodeExtractor", "✅ ZXing succeeded on BOTTOM HALF!")
                    return result
                }
                
                // 2b. Try ML Kit on Crop (NEW)
                var inputImage = InputImage.fromBitmap(cropped, 0)
                var barcodes = scanner.process(inputImage).await()
                var text = barcodes.firstOrNull { it.format in ACCEPTED_FORMATS }?.rawValue
                if (text != null) {
                    android.util.Log.d("QrCodeExtractor", "✅ ML Kit succeeded on BOTTOM HALF!")
                    return text
                }

                // 2c. Try UPSCALED Crop (2x) - "Zoom & Enhance"
                val scaled = scaleBitmap(cropped, 2.0f)
                result = decodeWithZxing(scaled)
                if (result != null) return result
                
                inputImage = InputImage.fromBitmap(scaled, 0)
                barcodes = scanner.process(inputImage).await()
                text = barcodes.firstOrNull { it.format in ACCEPTED_FORMATS }?.rawValue
                if (text != null) return text
            }

            // 3. Fallback to ML Kit on ORIGINAL with MULTIPLE ROTATIONS
            android.util.Log.d("QrCodeExtractor", "Step 3: ML Kit on ORIGINAL (all rotations)...")
            
            // Try all rotation values (0, 90, 180, 270)
            for (rotation in listOf(0, 90, 180, 270)) {
                android.util.Log.d("QrCodeExtractor", "   Trying rotation: $rotation°...")
                val inputImage = InputImage.fromBitmap(bitmap, rotation)
                var barcodes = scanner.process(inputImage).await()
                
                // DEBUG: Log ALL barcodes found by ML Kit
                android.util.Log.d("QrCodeExtractor", "   ML Kit (rot=$rotation) found ${barcodes.size} barcode(s)")
                for (bc in barcodes) {
                    val formatName = when(bc.format) {
                        Barcode.FORMAT_QR_CODE -> "QR_CODE"
                        Barcode.FORMAT_PDF417 -> "PDF417"
                        Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
                        Barcode.FORMAT_AZTEC -> "AZTEC"
                        else -> "OTHER(${bc.format})"
                    }
                    android.util.Log.d("QrCodeExtractor", "   → Barcode: format=$formatName, value=${bc.rawValue?.take(50)}...")
                }
                
                var text = barcodes.firstOrNull { it.format in ACCEPTED_FORMATS }?.rawValue
                if (text != null) {
                    android.util.Log.d("QrCodeExtractor", "✅ ML Kit ORIGINAL (rot=$rotation) SUCCESS!")
                    return text
                }
            }
            android.util.Log.d("QrCodeExtractor", "   ML Kit on ORIGINAL: No match at any rotation")
            
            // 3b. Try ML Kit with CONTRAST ENHANCED image
            android.util.Log.d("QrCodeExtractor", "Step 3b: ML Kit with CONTRAST ENHANCEMENT...")
            val contrastBitmap = enhanceContrast(bitmap)
            for (rotation in listOf(0, 90)) {
                val inputImage = InputImage.fromBitmap(contrastBitmap, rotation)
                var barcodes = scanner.process(inputImage).await()
                android.util.Log.d("QrCodeExtractor", "   ML Kit (contrast, rot=$rotation) found ${barcodes.size} barcode(s)")
                
                var text = barcodes.firstOrNull { it.format in ACCEPTED_FORMATS }?.rawValue
                if (text != null) {
                    android.util.Log.d("QrCodeExtractor", "✅ ML Kit CONTRAST (rot=$rotation) SUCCESS!")
                    return text
                }
            }

            // 4. Try MULTIPLE BINARIZATION THRESHOLDS
            android.util.Log.d("QrCodeExtractor", "Step 4: Trying MULTIPLE BINARIZATION thresholds...")
            for (threshold in listOf(100, 128, 150, 180, 200)) {
                android.util.Log.d("QrCodeExtractor", "   Trying threshold: $threshold")
                val binarizedBitmap = applyBinarizationWithThreshold(bitmap, threshold)
                
                // 4a. Try ZXing on this threshold
                result = decodeWithZxing(binarizedBitmap)
                if (result != null) {
                    android.util.Log.d("QrCodeExtractor", "✅ ZXing BINARIZED (threshold=$threshold) SUCCESS!")
                    return result
                }
                
                // 4b. Try ML Kit on this threshold
                val inputImage = InputImage.fromBitmap(binarizedBitmap, 0)
                val barcodes = scanner.process(inputImage).await()
                android.util.Log.d("QrCodeExtractor", "   ML Kit (threshold=$threshold) found ${barcodes.size} barcode(s)")
                
                val text = barcodes.firstOrNull { it.format in ACCEPTED_FORMATS }?.rawValue
                if (text != null) {
                    android.util.Log.d("QrCodeExtractor", "✅ ML Kit BINARIZED (threshold=$threshold) SUCCESS!")
                    return text
                }
            }
            android.util.Log.d("QrCodeExtractor", "   All thresholds failed")
            
            // 5. Try INVERTED binarization (white QR on black background)
            android.util.Log.d("QrCodeExtractor", "Step 5: Trying INVERTED binarization...")
            for (threshold in listOf(100, 128, 150, 180)) {
                android.util.Log.d("QrCodeExtractor", "   Trying inverted threshold: $threshold")
                val invertedBitmap = applyInvertedBinarization(bitmap, threshold)
                
                // Try ZXing
                result = decodeWithZxing(invertedBitmap)
                if (result != null) {
                    android.util.Log.d("QrCodeExtractor", "✅ ZXing INVERTED (threshold=$threshold) SUCCESS!")
                    return result
                }
                
                // Try ML Kit
                val inputImage = InputImage.fromBitmap(invertedBitmap, 0)
                val barcodes = scanner.process(inputImage).await()
                android.util.Log.d("QrCodeExtractor", "   ML Kit (inverted, threshold=$threshold) found ${barcodes.size} barcode(s)")
                
                val text = barcodes.firstOrNull { it.format in ACCEPTED_FORMATS }?.rawValue
                if (text != null) {
                    android.util.Log.d("QrCodeExtractor", "✅ ML Kit INVERTED (threshold=$threshold) SUCCESS!")
                    return text
                }
            }
            
            android.util.Log.d("QrCodeExtractor", "❌ ALL METHODS FAILED - Barcode could not be decoded")
            return null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    // NEW: Crop bottom 50% of the image
    private fun cropBottomHalf(original: Bitmap): Bitmap {
        val width = original.width
        val height = original.height
        val startY = height / 2
        return Bitmap.createBitmap(original, 0, startY, width, height - startY)
    }

    private fun scaleBitmap(original: Bitmap, scale: Float): Bitmap {
        val width = (original.width * scale).toInt()
        val height = (original.height * scale).toInt()
        return Bitmap.createScaledBitmap(original, width, height, true)
    }
    
    /**
     * Enhance contrast of the image to make barcodes more visible.
     * Uses a simple contrast stretching algorithm.
     */
    private fun enhanceContrast(input: Bitmap): Bitmap {
        val width = input.width
        val height = input.height
        val output = input.copy(Bitmap.Config.ARGB_8888, true)
        
        val pixels = IntArray(width * height)
        output.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Increase contrast by 50%
        val factor = 1.5f
        val midPoint = 128
        
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = (pixel shr 24) and 0xFF
            var r = (pixel shr 16) and 0xFF
            var g = (pixel shr 8) and 0xFF
            var b = pixel and 0xFF
            
            // Apply contrast enhancement
            r = ((factor * (r - midPoint)) + midPoint).toInt().coerceIn(0, 255)
            g = ((factor * (g - midPoint)) + midPoint).toInt().coerceIn(0, 255)
            b = ((factor * (b - midPoint)) + midPoint).toInt().coerceIn(0, 255)
            
            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        
        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    private fun applyBinarization(input: Bitmap): Bitmap {
        return applyBinarizationWithThreshold(input, 180)
    }
    
    private fun applyBinarizationWithThreshold(input: Bitmap, threshold: Int): Bitmap {
        val width = input.width
        val height = input.height
        // Create mutable bitmap
        val output = input.copy(Bitmap.Config.ARGB_8888, true)
        
        val pixels = IntArray(width * height)
        output.getPixels(pixels, 0, width, 0, 0, width, height)
        
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            
            // Simple luminance
            val l = (r + g + b) / 3
            
            // If darker than threshold -> Black, else White
            pixels[i] = if (l < threshold) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
        
        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }
    
    /**
     * Inverted binarization - for QR codes with dark background and light foreground.
     * Lighter pixels become black, darker become white (opposite of normal).
     */
    private fun applyInvertedBinarization(input: Bitmap, threshold: Int): Bitmap {
        val width = input.width
        val height = input.height
        val output = input.copy(Bitmap.Config.ARGB_8888, true)
        
        val pixels = IntArray(width * height)
        output.getPixels(pixels, 0, width, 0, 0, width, height)
        
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            
            val l = (r + g + b) / 3
            
            // INVERTED: Lighter than threshold -> Black, else White
            pixels[i] = if (l >= threshold) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
        
        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }
    
    /**
     * Load bitmap directly using ContentResolver - no downscaling!
     * Coil may downscale images which breaks QR detection.
     */
    private suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            // Use ContentResolver directly for full resolution
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                android.util.Log.e("QrCodeExtractor", "❌ ContentResolver returned null InputStream!")
                return null
            }
            
            // Decode with no downsampling
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = false
            }
            
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            
            if (bitmap == null) {
                android.util.Log.e("QrCodeExtractor", "❌ BitmapFactory.decodeStream returned null!")
            } else {
                android.util.Log.d("QrCodeExtractor", "✓ ContentResolver loaded: ${bitmap.width}x${bitmap.height}, config=${bitmap.config}")
            }
            
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("QrCodeExtractor", "❌ Exception loading bitmap: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    private fun decodeWithZxing(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            
            // 1. Try HybridBinarizer (Default)
            var binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val reader = MultiFormatReader().apply {
                setHints(mapOf(
                    DecodeHintType.TRY_HARDER to true,
                    DecodeHintType.POSSIBLE_FORMATS to listOf(
                        com.google.zxing.BarcodeFormat.QR_CODE,
                        com.google.zxing.BarcodeFormat.PDF_417,
                        com.google.zxing.BarcodeFormat.DATA_MATRIX,
                        com.google.zxing.BarcodeFormat.AZTEC
                    )
                ))
            }
            
            try {
                return reader.decode(binaryBitmap).text
            } catch (e: Exception) {
                // Continue to fallback
            }
            
            // 2. Try GlobalHistogramBinarizer (Good for low contrast/washed out)
            binaryBitmap = BinaryBitmap(com.google.zxing.common.GlobalHistogramBinarizer(source))
            try {
                return reader.decode(binaryBitmap).text
            } catch (e: Exception) {
                return null
            }
        } catch (e: Exception) {
            null
        }
    }
}
