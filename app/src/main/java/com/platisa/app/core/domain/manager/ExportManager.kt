package com.platisa.app.core.domain.manager

import android.content.Context
import com.platisa.app.core.domain.model.Receipt
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Locale

object ExportManager {

    fun exportToCsv(context: Context, receipts: List<Receipt>): File? {
        return try {
            val file = File(context.getExternalFilesDir(null), "receipts_export_${System.currentTimeMillis()}.csv")
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
            val file = File(context.getExternalFilesDir(null), "receipts_report_${System.currentTimeMillis()}.pdf")
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
                
                val line = "${dateFormat.format(receipt.date)} - ${receipt.merchantName}: ${receipt.totalAmount} ${receipt.currency}"
                contentStream.showText(line)
                contentStream.newLine()
                yOffset -= 14.5f
            }
            
            contentStream.endText()
            contentStream.close()
            
            document.save(file)
            document.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

