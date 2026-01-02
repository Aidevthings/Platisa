package com.platisa.app.core.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    fun compressImage(context: Context, imageFile: File, quality: Int = 80): File? {
        return try {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            if (bitmap == null) return null
            saveToWebp(context, bitmap, quality)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun cropAndCompressImage(
        context: Context,
        imageFile: File,
        cropLeftPercent: Float,
        cropTopPercent: Float,
        cropWidthPercent: Float,
        cropHeightPercent: Float,
        quality: Int = 80
    ): File? {
        return try {
            val originalBitmap = BitmapFactory.decodeFile(imageFile.absolutePath) ?: return null
            
            val width = originalBitmap.width
            val height = originalBitmap.height
            
            val left = (width * cropLeftPercent).toInt()
            val top = (height * cropTopPercent).toInt()
            val cropWidth = (width * cropWidthPercent).toInt()
            val cropHeight = (height * cropHeightPercent).toInt()
            
            // Ensure crop parameters are within bounds
            val safeWidth = if (left + cropWidth > width) width - left else cropWidth
            val safeHeight = if (top + cropHeight > height) height - top else cropHeight
            
            val croppedBitmap = Bitmap.createBitmap(originalBitmap, left, top, safeWidth, safeHeight)
            val result = saveToWebp(context, croppedBitmap, quality)
            
            // Cleanup bitmaps
            originalBitmap.recycle()
            if (croppedBitmap != originalBitmap) {
                croppedBitmap.recycle()
            }
            
            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveToWebp(context: Context, bitmap: Bitmap, quality: Int): File? {
        return try {
            val file = File(context.cacheDir, "processed_${System.currentTimeMillis()}.webp")
            val outputStream = FileOutputStream(file)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, outputStream)
            } else {
                bitmap.compress(Bitmap.CompressFormat.WEBP, quality, outputStream)
            }
            
            outputStream.flush()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

