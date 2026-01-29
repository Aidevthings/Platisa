package com.platisa.app.core.domain.manager

import android.content.Context
import android.util.Log
import com.platisa.app.core.domain.model.Receipt
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Locale

private const val TAG = "ExportManager"

object ExportManager {

    fun exportToCsv(context: Context, receipts: List<Receipt>): File? {
        return try {
            val dateFormatName = SimpleDateFormat("d.M.yyyy.", Locale("sr", "RS")) // Serbian format
            val dateStr = dateFormatName.format(java.util.Date())
            val fileName = "Platiša - izveštaj plaćanja - $dateStr.csv"
            
            val file = File(context.getExternalFilesDir(null), fileName)
            val writer = FileWriter(file)
            
            writer.append("Date,Merchant,Amount,Currency,PaymentStatus,InvoiceNumber,ExternalId,Source\n")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            
            for (receipt in receipts) {
                writer.append("${dateFormat.format(receipt.date)},")
                writer.append("${receipt.merchantName.replace(",", " ").replace("\n", " ")},")
                writer.append("${receipt.totalAmount},")
                writer.append("${receipt.currency},")
                writer.append("${receipt.paymentStatus},")
                writer.append("${(receipt.invoiceNumber ?: "").replace("\n", "").replace(",", "")},")
                writer.append("${(receipt.externalId ?: "").replace("\n", "").replace(",", "")},")
                writer.append("${receipt.originalSource}\n")
            }
            
            writer.flush()
            writer.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportToPdf(context: Context, receipts: List<Receipt>): File? {
        return try {
            Log.d(TAG, "Starting PDF export with ${receipts.size} receipts")
            
            // CRITICAL: Initialize PDFBox before using it
            PDFBoxResourceLoader.init(context.applicationContext)
            Log.d(TAG, "PDFBox initialized")
            
            val dateFormatName = SimpleDateFormat("d.M.yyyy.", Locale("sr", "RS")) // Serbian format
            val dateStr = dateFormatName.format(java.util.Date())
            val fileName = "Platiša - izveštaj plaćanja - $dateStr.pdf"
            
            val file = File(context.getExternalFilesDir(null), fileName)
            val document = PDDocument()
            var page = PDPage()
            document.addPage(page)
            
            var contentStream = PDPageContentStream(document, page)
            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18f)
            contentStream.newLineAtOffset(50f, 750f)
            contentStream.showText("Platisa Spending Report")
            contentStream.endText()
            
            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA, 12f)
            contentStream.newLineAtOffset(50f, 700f)
            contentStream.setLeading(14.5f)
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            var yOffset = 700f
            
            for (receipt in receipts) {
                if (yOffset < 50f) {
                    contentStream.endText()
                    contentStream.close()
                    
                    page = PDPage()
                    document.addPage(page)
                    contentStream = PDPageContentStream(document, page)
                    contentStream.beginText()
                    contentStream.setFont(PDType1Font.HELVETICA, 12f)
                    contentStream.newLineAtOffset(50f, 750f)
                    contentStream.setLeading(14.5f)
                    yOffset = 750f
                }
                
                // Sanitize text - remove special characters that break PDF rendering
                val merchantName = receipt.merchantName
                    .replace("\n", " ")
                    .replace("\r", "")
                    .take(40) // Limit length to prevent overflow
                val line = "${dateFormat.format(receipt.date)} - $merchantName: ${receipt.totalAmount} ${receipt.currency}"
                
                // PDFBox doesn't support all Unicode chars with Type1 fonts, sanitize
                val safeLine = line.filter { it.code < 256 }
                contentStream.showText(safeLine)
                contentStream.newLine()
                yOffset -= 14.5f
            }
            
            contentStream.endText()
            contentStream.close()
            
            document.save(file)
            document.close()
            
            Log.d(TAG, "PDF export successful: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "PDF export failed", e)
            e.printStackTrace()
            null
        }
    }
}

