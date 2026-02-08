package com.platisa.app.core.common

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.io.OutputStream

object QrSaveManager {

    private const val TAG = "QrSaveManager"

    /**
     * Generates a clean QR image (no header/footer text) and saves it to the gallery.
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

            // 2. Save clean QR bitmap to Gallery (no extra text)
            // Format: Platisa_Merchant_Date_Timestamp (to ensure uniqueness)
            val cleanMerchant = merchantName.replace(" ", "_").replace(".", "").replace(",", "")
            val cleanDate = date.replace(".", "_")
            val fileName = "Platisa_${cleanMerchant}_${cleanDate}_${System.currentTimeMillis()}"
            return saveBitmapToMediaStore(context, qrBitmap, fileName)

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

