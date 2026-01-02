package com.example.platisa.core.common

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object QrSaveManager {

    private const val TAG = "QrSaveManager"

    /**
     * Generates an enhanced QR image with header/footer and saves it to the gallery.
     */
    fun saveEnhancedQrToGallery(
        context: Context,
        qrData: String,
        merchantName: String,
        amount: String,
        date: String
    ): Uri? {
        try {
            // 1. Generate core QR bitmap
            val qrSize = 512
            val qrBitmap = QrCodeGenerator.generateQrCode(qrData, qrSize) ?: return null

            // 2. Create larger canvas for header/footer
            val padding = 40
            val headerHeight = 100
            val footerHeight = 60
            val totalWidth = qrSize + (padding * 2)
            val totalHeight = qrSize + headerHeight + footerHeight + (padding * 2)

            val finalBitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(finalBitmap)
            canvas.drawColor(Color.WHITE)

            val paint = Paint().apply {
                isAntiAlias = true
                color = Color.BLACK
                textAlign = Paint.Align.CENTER
            }

            // 3. Draw Header (Merchant + Amount)
            paint.apply {
                textSize = 34f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(merchantName.uppercase(), (totalWidth / 2).toFloat(), 50f, paint)
            
            paint.apply {
                textSize = 30f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            canvas.drawText("Iznos: $amount", (totalWidth / 2).toFloat(), 95f, paint)

            // 4. Draw QR Code
            canvas.drawBitmap(qrBitmap, padding.toFloat(), (headerHeight + padding).toFloat(), null)

            // 5. Draw Footer (Date)
            paint.apply {
                textSize = 24f
                color = Color.DKGRAY
            }
            val footerY = totalHeight - 40f
            canvas.drawText("Datum računa: $date", (totalWidth / 2).toFloat(), footerY, paint)

            // 6. Save to Gallery
            // Format: Platisa_Merchant_Date_Timestamp (to ensure uniqueness)
            val cleanMerchant = merchantName.replace(" ", "_").replace(".", "").replace(",", "")
            val cleanDate = date.replace(".", "_")
            val fileName = "Platisa_${cleanMerchant}_${cleanDate}_${System.currentTimeMillis()}"
            return saveBitmapToMediaStore(context, finalBitmap, fileName)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save enhanced QR", e)
            return null
        }
    }

    private fun saveBitmapToMediaStore(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val contentResolver = context.contentResolver
        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Platisa")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val imageUri = contentResolver.insert(imageCollection, contentValues) ?: return null

        try {
            val outputStream: OutputStream? = contentResolver.openOutputStream(imageUri)
            outputStream?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(imageUri, contentValues, null, null)
            }
            return imageUri
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap", e)
            contentResolver.delete(imageUri, null, null)
            return null
        }
    }

    /**
     * Deletes a QR image from the gallery.
     */
    fun deleteQrFromGallery(context: Context, uriString: String?) {
        if (uriString.isNullOrEmpty()) return
        
        try {
            val uri = Uri.parse(uriString)
            context.contentResolver.delete(uri, null, null)
            Log.d(TAG, "Successfully deleted QR from gallery: $uriString")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete QR from gallery: $uriString", e)
        }
    }
}
