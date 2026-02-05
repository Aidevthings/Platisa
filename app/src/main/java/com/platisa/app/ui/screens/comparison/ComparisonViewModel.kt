package com.platisa.app.ui.screens.comparison

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.platisa.app.core.domain.model.ProductSearchResult
import com.platisa.app.core.domain.model.Receipt
import com.platisa.app.core.data.parser.IpsData
import com.platisa.app.core.domain.model.PaymentStatus
import com.platisa.app.core.domain.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ComparisonViewModel @Inject constructor(
    private val repository: ReceiptRepository,
    private val preferenceManager: com.platisa.app.core.data.preferences.PreferenceManager,
    private val secureStorage: com.platisa.app.core.domain.SecureStorage,
    private val vibrationHelper: com.platisa.app.core.common.VibrationHelper
) : ViewModel() {

    fun vibrate(type: com.platisa.app.core.common.VibrationHelper.HapticType) {
        vibrationHelper.vibrate(type)
    }

    private val _currency = MutableStateFlow(secureStorage.getCurrency())
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _conversionRate = MutableStateFlow(java.math.BigDecimal(preferenceManager.lastKnownEuroRate.toDouble()))
    val conversionRate: StateFlow<java.math.BigDecimal> = _conversionRate.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    
    val isDarkTheme = preferenceManager.themeFlow

    // Limit data to last 6 months (approx 180 days)
    private val sixMonthsInMillis = 180L * 24 * 60 * 60 * 1000
    
    val searchResults: StateFlow<List<ProductSearchResult>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                repository.searchItems(query).map { results ->
                    val cutoffDate = System.currentTimeMillis() - sixMonthsInMillis
                    
                    val filteredByDate = results.filter { it.date.time >= cutoffDate }
                    
                    if (preferenceManager.hasScannedRestaurantBill) {
                        filteredByDate
                    } else {
                        // Filter out RESTAURANT items if not enabled
                        filteredByDate.filter { 
                            com.platisa.app.core.domain.model.BillCategorizer.categorize(it.merchantName) != 
                            com.platisa.app.core.domain.model.BillCategory.RESTAURANT 
                        }
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // List of saved fiscal receipts (from camera scanning)
    val fiscalReceipts: StateFlow<List<Receipt>> = repository.getAllReceipts()
        .map { receipts ->
            val cutoffDate = System.currentTimeMillis() - sixMonthsInMillis
            receipts.filter { 
                it.originalSource == "CAMERA_FISCAL" && it.date.time >= cutoffDate 
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    /**
     * Save an IPS payment bill directly to database.
     * Called when scanner detects an IPS QR code (K:PR format).
     * 
     * @param ipsData Parsed IPS data from QR code
     * @return Receipt ID of the saved bill
     */
    suspend fun saveIpsBill(ipsData: IpsData): Long {
        // 1. Deduplication: Check if receipt with same Reference Number (Invoice Number) exists
        if (!ipsData.referenceNumber.isNullOrEmpty()) {
            val existing = repository.getReceiptByInvoiceNumber(ipsData.referenceNumber)
            if (existing != null) {
                android.util.Log.d("ComparisonViewModel", "Duplicate IPS bill found: ${ipsData.referenceNumber}")
                
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
                 android.util.Log.d("ComparisonViewModel", "Fuzzy duplicate found: ${fuzzyMatch.merchantName} - ${fuzzyMatch.totalAmount}")
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
            invoiceNumber = ipsData.referenceNumber,
            payerName = ipsData.payerName,
            payerAddress = ipsData.payerAddress
        )
        
        val receiptId = repository.insertReceipt(receipt)
        android.util.Log.d("ComparisonViewModel", "IPS bill saved: ID=$receiptId, Merchant=${receipt.merchantName}, Amount=${receipt.totalAmount}")
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
                            android.util.Log.d("ComparisonViewModel", "Duplicate receipt found: ${parsed.invoiceNumber}")
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
                        invoiceNumber = parsed.invoiceNumber,
                        payerName = parsed.payerName,
                        payerAddress = parsed.payerAddress
                    )
                    
                    val receiptId = repository.insertReceipt(receipt)
                    
                    // Save items if available
                    if (parsed.items.isNotEmpty()) {
                        repository.insertReceiptItems(parsed.items, receiptId)
                    }
                    
                    android.util.Log.d("ComparisonViewModel", "Fiscal receipt saved: ID=$receiptId, Merchant=${receipt.merchantName}, Amount=${receipt.totalAmount}, Items=${parsed.items.size}")
                    receiptId
                }
                is com.platisa.app.core.common.FiscalScraper.ScrapeResult.Error -> {
                    android.util.Log.e("ComparisonViewModel", "Failed to scrape fiscal data: ${result.message}")
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ComparisonViewModel", "Error saving fiscal receipt", e)
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
            android.util.Log.d("ComparisonViewModel", "Fiscal receipt fallback saved: ID=$receiptId, URL=$fiscalUrl")
            receiptId
        } catch (e: Exception) {
            android.util.Log.e("ComparisonViewModel", "Error saving fiscal receipt fallback", e)
            null
        }
    }
}

