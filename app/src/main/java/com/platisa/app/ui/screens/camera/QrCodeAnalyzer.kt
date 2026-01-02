package com.platisa.app.ui.screens.camera

import android.graphics.ImageFormat
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer

/**
 * Enhanced QR Code Analyzer with:
 * - ZXing (fast) as primary scanner
 * - ML Kit (robust) as fallback
 * - Detection cooldown to prevent duplicates
 * - Frame skipping for performance
 * - ROI cropping for faster processing
 */
class QrCodeAnalyzer(
    private val onQrCodeDetected: (String) -> Unit,
    private val onDebugInfo: (String) -> Unit
) : ImageAnalysis.Analyzer {

    // ZXing reader for fast barcode detection (QR, PDF417, DATA_MATRIX)
    private val zxingReader = MultiFormatReader().apply {
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(
                com.google.zxing.BarcodeFormat.QR_CODE,
                com.google.zxing.BarcodeFormat.PDF_417,
                com.google.zxing.BarcodeFormat.DATA_MATRIX,
                com.google.zxing.BarcodeFormat.AZTEC
            ),
            DecodeHintType.TRY_HARDER to true
        )
        setHints(hints)
    }

    // ML Kit scanner as fallback (neural network-based, handles damaged barcodes better)
    // Supports QR, PDF417, DATA_MATRIX for Serbian fiscal receipts
    private val mlKitScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_PDF417,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_AZTEC
            )
            .build()
    )

    // Cooldown to prevent duplicate detections
    private var lastDetectionTime = 0L
    private val DETECTION_COOLDOWN_MS = 1500L

    // Frame skipping for performance (process every Nth frame)
    private var frameCount = 0
    private val PROCESS_EVERY_N_FRAMES = 2  // Process every 2nd frame (15 FPS effective)

    // Track if we're currently processing ML Kit (async)
    @Volatile
    private var isMlKitProcessing = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        
        // Cooldown check - skip if we just detected something
        if (currentTime - lastDetectionTime < DETECTION_COOLDOWN_MS) {
            image.close()
            return
        }

        // Frame skip for performance
        frameCount++
        if (frameCount % PROCESS_EVERY_N_FRAMES != 0) {
            image.close()
            return
        }

        val width = image.width
        val height = image.height
        val rotation = image.imageInfo.rotationDegrees
        val format = image.format

        try {
            // Validate format
            if (format != ImageFormat.YUV_420_888) {
                onDebugInfo("Error: Format not YUV_420_888 (is $format)")
                image.close()
                return
            }

            val buffer = image.planes[0].buffer
            val data = toByteArray(buffer)

            // ========== PHASE 1: ZXing with ROI Cropping ==========
            // Crop to center 70% for faster processing
            val (croppedData, cropWidth, cropHeight) = cropToCenter(data, width, height)
            
            val source = PlanarYUVLuminanceSource(
                croppedData,
                cropWidth,
                cropHeight,
                0,
                0,
                cropWidth,
                cropHeight,
                false
            )

            // Try HybridBinarizer first (better for typical QR codes)
            try {
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                val result = zxingReader.decodeWithState(binaryBitmap)
                
                // Success! 
                lastDetectionTime = currentTime
                onDebugInfo("✓ ZXing (Hybrid): ${result.text.take(20)}...")
                onQrCodeDetected(result.text)
                image.close()
                return
            } catch (e: Exception) {
                // Continue to fallback
            } finally {
                zxingReader.reset()
            }

            // Try GlobalHistogramBinarizer (better for low contrast)
            try {
                val globalSource = PlanarYUVLuminanceSource(
                    croppedData, cropWidth, cropHeight, 0, 0, cropWidth, cropHeight, false
                )
                val globalBitmap = BinaryBitmap(com.google.zxing.common.GlobalHistogramBinarizer(globalSource))
                val result = zxingReader.decodeWithState(globalBitmap)
                
                lastDetectionTime = currentTime
                onDebugInfo("✓ ZXing (Global): ${result.text.take(20)}...")
                onQrCodeDetected(result.text)
                image.close()
                return
            } catch (e: Exception) {
                // Continue to ML Kit fallback
            } finally {
                zxingReader.reset()
            }

            // ========== PHASE 2: ML Kit Fallback ==========
            // Only try ML Kit if not already processing (async)
            if (!isMlKitProcessing) {
                val mediaImage = image.image
                if (mediaImage != null) {
                    isMlKitProcessing = true
                    val inputImage = InputImage.fromMediaImage(mediaImage, rotation)
                    
                    mlKitScanner.process(inputImage)
                        .addOnSuccessListener { barcodes ->
                            // Accept QR, PDF417, DATA_MATRIX, AZTEC
                            val barcode = barcodes.firstOrNull { 
                                it.format == Barcode.FORMAT_QR_CODE ||
                                it.format == Barcode.FORMAT_PDF417 ||
                                it.format == Barcode.FORMAT_DATA_MATRIX ||
                                it.format == Barcode.FORMAT_AZTEC
                            }
                            if (barcode != null && barcode.rawValue != null) {
                                lastDetectionTime = System.currentTimeMillis()
                                val formatName = when(barcode.format) {
                                    Barcode.FORMAT_QR_CODE -> "QR"
                                    Barcode.FORMAT_PDF417 -> "PDF417"
                                    Barcode.FORMAT_DATA_MATRIX -> "DataMatrix"
                                    else -> "Barcode"
                                }
                                onDebugInfo("✓ ML Kit ($formatName): ${barcode.rawValue!!.take(20)}...")
                                onQrCodeDetected(barcode.rawValue!!)
                            }
                            isMlKitProcessing = false
                        }
                        .addOnFailureListener {
                            isMlKitProcessing = false
                        }
                        .addOnCompleteListener {
                            // Close image after ML Kit completes
                            try { image.close() } catch (e: Exception) { }
                        }
                    
                    // Don't close image here - ML Kit will use it async
                    onDebugInfo("Scanning... (${width}x${height}, ML Kit active)")
                    return
                }
            }

            // No QR found with either method
            onDebugInfo("Scanning... (${width}x${height}, Rot:$rotation)")
            
        } catch (e: Exception) {
            onDebugInfo("Error: ${e.message}")
        } finally {
            // Only close if ML Kit didn't take over
            if (!isMlKitProcessing) {
                image.close()
            }
        }
    }

    /**
     * Convert ByteBuffer to ByteArray
     */
    private fun toByteArray(buffer: ByteBuffer): ByteArray {
        buffer.rewind()
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        return data
    }

    /**
     * Crop to center 70% of the image for ROI-based scanning.
     * This significantly improves performance by reducing pixels to scan.
     */
    private fun cropToCenter(data: ByteArray, width: Int, height: Int): Triple<ByteArray, Int, Int> {
        val cropFactor = 0.7f  // Use center 70%
        val cropWidth = (width * cropFactor).toInt()
        val cropHeight = (height * cropFactor).toInt()
        val startX = (width - cropWidth) / 2
        val startY = (height - cropHeight) / 2

        // For YUV, we only need the Y plane (luminance) for QR detection
        // Y plane is width * height bytes, laid out row by row
        val croppedData = ByteArray(cropWidth * cropHeight)
        
        for (y in 0 until cropHeight) {
            val srcOffset = (startY + y) * width + startX
            val dstOffset = y * cropWidth
            System.arraycopy(data, srcOffset, croppedData, dstOffset, cropWidth)
        }

        return Triple(croppedData, cropWidth, cropHeight)
    }
}

