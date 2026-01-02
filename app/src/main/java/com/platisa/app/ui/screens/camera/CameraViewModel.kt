package com.platisa.app.ui.screens.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.platisa.app.core.data.parser.IpsData
import com.platisa.app.core.domain.model.PaymentStatus
import com.platisa.app.core.domain.model.Receipt
import com.platisa.app.core.domain.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val repository: ReceiptRepository
) : ViewModel() {

    /**
     * Save an IPS payment bill directly to database.
     * Called when camera detects an IPS QR code (K:PR format).
     * 
     * @param ipsData Parsed IPS data from QR code
     * @return Receipt ID of the saved bill
     */
    suspend fun saveIpsBill(ipsData: IpsData): Long {
        // 1. Deduplication: Check if receipt with same Reference Number (Invoice Number) exists
        if (!ipsData.referenceNumber.isNullOrEmpty()) {
            val existing = repository.getReceiptByInvoiceNumber(ipsData.referenceNumber)
            if (existing != null) {
                android.util.Log.d("CameraViewModel", "Duplicate IPS bill found: ${ipsData.referenceNumber}")
                
                if (existing.paymentStatus == PaymentStatus.PAID) {
                    throw IllegalStateException("Račun je već plaćen!")
                } else {
                    throw IllegalStateException("Račun je već skeniran!")
                }
            }
        }
        
        // 2. Fuzzy Deduplication: Check same Amount + Merchant + Recent Date (45 days)
        if (ipsData.amount != null) {
            val candidates = repository.getReceiptsByAmount(ipsData.amount)
            val recipientName = ipsData.recipientName ?: ""
            val fortyFiveDaysAgo = System.currentTimeMillis() - (45L * 24 * 60 * 60 * 1000)
            
            val fuzzyMatch = candidates.firstOrNull { candidate ->
                // Check recent date
                if (candidate.date.time < fortyFiveDaysAgo) return@firstOrNull false
                
                // Check merchant similarity (simple contains check)
                candidate.merchantName.contains(recipientName, ignoreCase = true) ||
                recipientName.contains(candidate.merchantName, ignoreCase = true)
            }
            
            if (fuzzyMatch != null) {
                 android.util.Log.d("CameraViewModel", "Fuzzy duplicate found: ${fuzzyMatch.merchantName} - ${fuzzyMatch.totalAmount}")
                 if (fuzzyMatch.paymentStatus == PaymentStatus.PAID) {
                    throw IllegalStateException("Račun sa istim iznosom je već plaćen!")
                } else {
                    throw IllegalStateException("Račun sa istim iznosom je već skeniran!")
                }
            }
        }

        val receipt = Receipt(
            merchantName = ipsData.recipientName ?: "Nepoznat primalac",
            totalAmount = ipsData.amount ?: BigDecimal.ZERO,
            date = Date(),
            imagePath = "",  // No image for auto-scanned IPS
            qrCodeData = buildIpsQrString(ipsData),
            paymentStatus = PaymentStatus.UNPAID,
            originalSource = "CAMERA_IPS",
            invoiceNumber = ipsData.referenceNumber
        )
        
        val receiptId = repository.insertReceipt(receipt)
        android.util.Log.d("CameraViewModel", "IPS bill saved: ID=$receiptId, Merchant=${receipt.merchantName}, Amount=${receipt.totalAmount}")
        return receiptId
    }
    
    /**
     * Reconstruct the IPS QR string for storage.
     * This allows regenerating the QR code later for payment.
     */
    private fun buildIpsQrString(ips: IpsData): String {
        val parts = mutableListOf<String>()
        parts.add("K:PR")
        parts.add("V:01")
        parts.add("C:1")
        ips.recipientAccount?.let { parts.add("R:$it") }
        ips.recipientName?.let { parts.add("N:$it") }
        ips.amount?.let { parts.add("I:RSD${it.toPlainString().replace(".", ",")}") }
        ips.referenceNumber?.let { parts.add("RO:$it") }
        ips.purposeCode?.let { parts.add("SF:$it") }
        ips.purposeDescription?.let { parts.add("S:$it") }
        return parts.joinToString("|")
    }
    
    /**
     * Save a fiscal receipt (from store QR code) to database.
     * Fiscal receipts are automatically marked as PAID since they are paid at the store.
     * 
     * @param fiscalUrl The URL from the fiscal QR code
     * @return Receipt ID of the saved receipt, or null if scraping failed
     */
    suspend fun saveFiscalReceipt(fiscalUrl: String): Long? {
        return try {
            val result = com.platisa.app.core.common.FiscalScraper.scrapeFiscalData(fiscalUrl)
            
            when (result) {
                is com.platisa.app.core.common.FiscalScraper.ScrapeResult.Success -> {
                    val parsed = result.receipt
                    
                    // Deduplication: Check if receipt with same Invoice Number already exists
                    if (!parsed.invoiceNumber.isNullOrEmpty()) {
                        val existing = repository.getReceiptByInvoiceNumber(parsed.invoiceNumber)
                        if (existing != null) {
                            android.util.Log.d("CameraViewModel", "Duplicate receipt found: ${parsed.invoiceNumber}")
                            // Return existing ID to open it instead of creating duplicate
                            return existing.id
                        }
                    }

                    val receipt = Receipt(
                        merchantName = parsed.merchantName ?: "Nepoznata prodavnica",
                        totalAmount = parsed.totalAmount ?: BigDecimal.ZERO,
                        date = parsed.date ?: Date(),
                        imagePath = "",
                        qrCodeData = fiscalUrl,
                        paymentStatus = PaymentStatus.PAID,  // Fiscal receipts are always paid
                        originalSource = "CAMERA_FISCAL",
                        invoiceNumber = parsed.invoiceNumber
                    )
                    
                    val receiptId = repository.insertReceipt(receipt)
                    
                    // Save items if available
                    if (parsed.items.isNotEmpty()) {
                        repository.insertReceiptItems(parsed.items, receiptId)
                    }
                    
                    android.util.Log.d("CameraViewModel", "Fiscal receipt saved: ID=$receiptId, Merchant=${receipt.merchantName}, Amount=${receipt.totalAmount}, Items=${parsed.items.size}")
                    receiptId
                }
                is com.platisa.app.core.common.FiscalScraper.ScrapeResult.Error -> {
                    android.util.Log.e("CameraViewModel", "Failed to scrape fiscal data: ${result.message}")
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CameraViewModel", "Error saving fiscal receipt", e)
            null
        }
    }
    
    /**
     * Fallback: Save fiscal receipt with just the URL when scraping fails.
     * User can manually click link to view receipt.
     */
    suspend fun saveFiscalReceiptFallback(fiscalUrl: String): Long? {
        return try {
            val receipt = Receipt(
                merchantName = "Fiskalni Račun",  // Generic name
                totalAmount = BigDecimal.ZERO,  // Unknown amount
                date = Date(),
                imagePath = "",
                qrCodeData = fiscalUrl,  // Store URL for later viewing
                paymentStatus = PaymentStatus.PAID,
                originalSource = "CAMERA_FISCAL",
                invoiceNumber = null
            )
            
            val receiptId = repository.insertReceipt(receipt)
            android.util.Log.d("CameraViewModel", "Fiscal receipt fallback saved: ID=$receiptId, URL=$fiscalUrl")
            receiptId
        } catch (e: Exception) {
            android.util.Log.e("CameraViewModel", "Error saving fiscal receipt fallback", e)
            null
        }
    }
}

