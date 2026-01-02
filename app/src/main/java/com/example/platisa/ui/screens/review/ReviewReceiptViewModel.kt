package com.example.platisa.ui.screens.review

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.platisa.core.common.BaseViewModel
import com.example.platisa.core.common.OcrManager
import com.example.platisa.core.domain.parser.AutoTagger
import com.example.platisa.core.domain.parser.EpsParser
import com.example.platisa.core.domain.parser.ParsedReceipt
import com.example.platisa.core.domain.parser.ReceiptParser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.platisa.core.domain.model.Receipt
import com.example.platisa.core.domain.model.EpsData
import com.example.platisa.core.domain.repository.ReceiptRepository
import java.math.BigDecimal
import java.util.Date

@HiltViewModel
class ReviewReceiptViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val repository: ReceiptRepository
) : BaseViewModel() {

    private val imageUriString: String = checkNotNull(savedStateHandle["imageUri"])
    val imageUri: Uri = if (imageUriString.startsWith("/")) {
        Uri.fromFile(java.io.File(imageUriString))
    } else {
        Uri.parse(imageUriString)
    }

    private val qrDataArg: String? = savedStateHandle["qrData"]

    private var currentActiveUri: Uri = imageUri

    private val _parsedReceipt = MutableStateFlow<ParsedReceipt?>(null)
    val parsedReceipt = _parsedReceipt.asStateFlow()

    private val _suggestedSection = MutableStateFlow<String?>(null)
    val suggestedSection = _suggestedSection.asStateFlow()

    private val _rawText = MutableStateFlow<String>("")
    val rawText = _rawText.asStateFlow()

    private var existingReceiptId: Long = 0

    private val _displayBitmap = MutableStateFlow<android.graphics.Bitmap?>(null)
    val displayBitmap = _displayBitmap.asStateFlow()

    private val _isExistingReceipt = MutableStateFlow(false)
    val isExistingReceipt = _isExistingReceipt.asStateFlow()

    private val _isPdfSource = MutableStateFlow(false)
    val isPdfSource = _isPdfSource.asStateFlow()

    private val _epsData = MutableStateFlow<EpsData?>(null)
    val epsData = _epsData.asStateFlow()

    private val _isDuplicate = MutableStateFlow(false)
    val isDuplicate = _isDuplicate.asStateFlow()

    private val _duplicateReceiptId = MutableStateFlow<Long?>(null)
    val duplicateReceiptId = _duplicateReceiptId.asStateFlow()

    private var lastSavedQrUri: String? = null

    init {
        processImage(imageUri)
    }

    fun reprocessImage(newUri: Uri) {
        processImage(newUri)
    }

    fun processManualUrl(url: String) {
        val trimmedUrl = url.trim()
        if (com.example.platisa.core.common.FiscalScraper.isFiscalUrl(trimmedUrl)) {
            android.util.Log.d("ReviewVM", "Processing Manual URL: $trimmedUrl")
            launchCatching {
                 val scrapeResult = com.example.platisa.core.common.FiscalScraper.scrapeFiscalData(trimmedUrl)
                 when (scrapeResult) {
                    is com.example.platisa.core.common.FiscalScraper.ScrapeResult.Success -> {
                        _parsedReceipt.value = scrapeResult.receipt
                        android.util.Log.d("ReviewVM", "✅ Manual scrape success!")
                        _rawText.value = "--- HTML DEBUG [${System.currentTimeMillis()}] ---\n${scrapeResult.debugHtml}\n--- HTML DEBUG END ---"
                        _suggestedSection.value = "Shopping"
                    }
                    is com.example.platisa.core.common.FiscalScraper.ScrapeResult.Error -> {
                        android.util.Log.e("ReviewVM", "❌ Manual scrape failed: ${scrapeResult.message}")
                        _rawText.value = "❌ SCRAPE ERROR [${System.currentTimeMillis()}]:\n${scrapeResult.message}"
                    }
                 }
            }
        } else {
            _rawText.value = "❌ INVALID URL: Not a valid fiscal receipt URL (suf.purs.gov.rs)"
        }
    }

    private fun processImage(targetUri: Uri) {
        currentActiveUri = targetUri
        launchCatching {
            val targetUriString = targetUri.toString()
            android.util.Log.d("ReviewVM", "Processing image: $targetUriString")
            
            // 0. Detect if this is a PDF source
            val uri = targetUri
            val mimeType = context.contentResolver.getType(uri)
            android.util.Log.d("ReviewVM", "MimeType: $mimeType")
            
            val isPdf = mimeType == "application/pdf" || imageUriString.endsWith(".pdf", ignoreCase = true)
            _isPdfSource.value = isPdf
            
            android.util.Log.d("ReviewVM", if (isPdf) "Detected PDF source" else "Detected image source")

            // First check if this receipt already exists in the database
            val existingReceipt = repository.getReceiptByPath(imageUriString)
            
            // FORCE DEBUG: Ignore existing receipt to always trigger scraper
            if (false && existingReceipt != null) {
                // Load from DB - this is an existing receipt (Gmail or previously saved)
                _isExistingReceipt.value = true
                existingReceiptId = existingReceipt.id
                _parsedReceipt.value = ParsedReceipt(
                    merchantName = existingReceipt.merchantName,
                    date = existingReceipt.date,
                    totalAmount = existingReceipt.totalAmount,
                    qrCodeData = existingReceipt.qrCodeData
                )
                
                // Load EPS data if available
                _epsData.value = repository.getEpsDataForReceipt(existingReceipt.id)
            } else {
                // 1. Extract QR code logic
                // Check if we have passed QR data from camera (Scan & Go)
                val passedQrData = if (targetUri == imageUri) qrDataArg else null
                
                val qrCodeData = passedQrData ?: com.example.platisa.core.common.QrCodeExtractor.extractQrCode(targetUriString, context)
                
                // DEBUG: Log result
                val qrStatusIdx = if (qrCodeData != null) "SUCCESS (${qrCodeData.take(15)}...)" else "FAILED"
                _rawText.value = "🔍 QR SCAN STATUS: $qrStatusIdx\n" +
                        "🔑 GEMINI KEY CHECK: ${if (com.example.platisa.BuildConfig.GEMINI_API_KEY.isNotBlank()) "✅ FOUND" else "❌ MISSING"}\n"

                var fiscalParsed: ParsedReceipt? = null
                
                // 2. If it is a fiscal URL, try scraping it
                if (qrCodeData != null && com.example.platisa.core.common.FiscalScraper.isFiscalUrl(qrCodeData)) {
                    android.util.Log.d("ReviewVM", "Detected Fiscal QR URL. Attempting to scrape...")
                    val scrapeResult = com.example.platisa.core.common.FiscalScraper.scrapeFiscalData(qrCodeData)
                    
                    when (scrapeResult) {
                        is com.example.platisa.core.common.FiscalScraper.ScrapeResult.Success -> {
                            fiscalParsed = scrapeResult.receipt
                            android.util.Log.d("ReviewVM", "✅ Fiscal data scraped successfully!")
                            _rawText.value = "--- HTML DEBUG [${System.currentTimeMillis()}] ---\n${scrapeResult.debugHtml}\n--- HTML DEBUG END ---"
                        }
                        is com.example.platisa.core.common.FiscalScraper.ScrapeResult.Error -> {
                            android.util.Log.e("ReviewVM", "❌ Fiscal scrape failed: ${scrapeResult.message}")
                            _rawText.value = "❌ SCRAPE ERROR [${System.currentTimeMillis()}]:\n${scrapeResult.message}\n\nFalling back to OCR..."
                            // fiscalParsed remains null, falls back to OCR
                        }
                    }
                }

                if (fiscalParsed != null) {
                    // Use fiscal data directly - 100% accurate
                    _parsedReceipt.value = fiscalParsed
                    _suggestedSection.value = "Shopping" // Usually store bills
                } else {
                    // 3. Gemini Disabled by User Request
                    var geminiParsed: ParsedReceipt? = null

                    // 4. Fallback/Parallel: Run standard OCR (needed for raw text logs and EpsParser metrics)
                    _isExistingReceipt.value = false
                    val text = OcrManager.processImage(context, targetUri)
                    
                    // Append Legacy OCR result
                    _rawText.value += "\n\n--- LEGACY OCR RESULT ---\n" + text
                    
                    val legacyParsed = ReceiptParser.parse(text)
                    
                    // Check for duplicate based on invoice number (prefer Gemini's if available)
                    val invoiceNum = geminiParsed?.invoiceNumber ?: legacyParsed.invoiceNumber
                    if (invoiceNum != null) {
                        val existingByInvoice = repository.getReceiptByInvoiceNumber(invoiceNum)
                        if (existingByInvoice != null) {
                            _isDuplicate.value = true
                            _duplicateReceiptId.value = existingByInvoice.id
                            android.util.Log.d("ReviewVM", "Duplicate found: Invoice #$invoiceNum already exists (ID: ${existingByInvoice.id})")
                        }
                    }
                    
                    if (geminiParsed != null) {
                        android.util.Log.d("ReviewVM", "✅ Gemini parsing success! Using AI result.")
                        _rawText.value += "\n\n✅ GEMINI PARSING SUCCESS!\nMerchant: ${geminiParsed.merchantName}\nName: ${geminiParsed.recipientName}"
                        _parsedReceipt.value = geminiParsed.copy(qrCodeData = qrCodeData)
                        // If Gemini found it, highly likely it's a bill
                        _suggestedSection.value = "Bills" 
                    } else {
                         android.util.Log.d("ReviewVM", "⚠️ Gemini failed or skipped. Using legacy Regex parser.")
                        _parsedReceipt.value = legacyParsed.copy(qrCodeData = qrCodeData)
                        _suggestedSection.value = AutoTagger.suggestSection(text)
                    }
                    
                    // Check for EPS data (Consumption graphs) - relies on raw text for now
                    var eps = EpsParser.parse(text)
                    
                    // Use Gemini's recipient info if available (Gemini reads Cyrillic properly)
                    if (geminiParsed != null && eps != null) {
                        val geminiName = geminiParsed.recipientName
                        val geminiAddress = geminiParsed.recipientAddress
                        if (!geminiName.isNullOrBlank() || !geminiAddress.isNullOrBlank()) {
                            android.util.Log.d("ReviewVM", "Using Gemini recipient: $geminiName, $geminiAddress")
                            eps = eps.copy(
                                recipientName = geminiName ?: eps.recipientName,
                                recipientAddress = geminiAddress ?: eps.recipientAddress
                            )
                        }
                    }
                    
                    _epsData.value = eps
                    
                    if (eps != null && _suggestedSection.value == null) {
                        _suggestedSection.value = "Bills"
                    }
                    
                    // CRITICAL FIX: Merge EPS data back into main receipt
                    // If EpsParser found a Due Date or Invoice Number that generic parser missed, prefer it.
                    if (eps != null) {
                        val current = _parsedReceipt.value
                        if (current != null) {
                            _parsedReceipt.value = current.copy(
                                dueDate = current.dueDate ?: eps.dueDate,
                                invoiceNumber = current.invoiceNumber ?: eps.invoiceNumber
                            )
                        }
                    }
                }
            }
        }
    }


    
    fun confirmReceipt(merchant: String, total: String, dateStr: String, invoiceNumber: String? = null) {
        launchCatching {
            android.util.Log.d("ReviewVM", "=== CONFIRM RECEIPT ===")
            android.util.Log.d("ReviewVM", "Merchant: $merchant")
            android.util.Log.d("ReviewVM", "Total: $total")
            android.util.Log.d("ReviewVM", "Date: $dateStr")
            android.util.Log.d("ReviewVM", "Invoice Number: $invoiceNumber")
            
            // Check for duplicate one more time before saving
            if (invoiceNumber != null) {
                val existingByInvoice = repository.getReceiptByInvoiceNumber(invoiceNumber)
                if (existingByInvoice != null) {
                    android.util.Log.w("ReviewVM", "DUPLICATE FOUND: Invoice $invoiceNumber already exists (ID: ${existingByInvoice.id})")
                    android.widget.Toast.makeText(
                        context,
                        "Račun broj $invoiceNumber već postoji! (ID: ${existingByInvoice.id})",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    return@launchCatching
                } else {
                    android.util.Log.d("ReviewVM", "No duplicate found - OK to save")
                }
            }
            val amount = try {
                // Parse Serbian number format (1.234,56)
                val format = java.text.NumberFormat.getInstance(java.util.Locale("sr", "RS"))
                val number = format.parse(total)
                BigDecimal(number.toString())
            } catch (e: Exception) {
                // Fallback to standard parsing if it fails (e.g. user entered 1234.56)
                try { BigDecimal(total) } catch (e2: Exception) { BigDecimal.ZERO }
            }
            
            val date = try {
                java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).parse(dateStr) ?: Date()
            } catch (e: Exception) {
                Date()
            }
            
            if (existingReceiptId != 0L) {
                // Update existing receipt
                val existingReceipt = repository.getReceiptById(existingReceiptId)
                existingReceipt?.let {
                    val updatedReceipt = it.copy(
                        merchantName = merchant,
                        totalAmount = amount,
                        date = date,
                        dueDate = _parsedReceipt.value?.dueDate ?: it.dueDate,
                        qrCodeData = _parsedReceipt.value?.qrCodeData
                    )
                    repository.updateReceipt(updatedReceipt)
                }
            } else {
                // Insert new receipt
                val receipt = Receipt(
                    id = 0,
                    merchantName = merchant,
                    totalAmount = amount,
                    date = date,
                    dueDate = _parsedReceipt.value?.dueDate, // Save parsed Due Date
                    imagePath = imageUriString,
                    qrCodeData = _parsedReceipt.value?.qrCodeData,
                    invoiceNumber = invoiceNumber,
                    savedQrUri = lastSavedQrUri,
                    paymentStatus = if (lastSavedQrUri != null) com.example.platisa.core.domain.model.PaymentStatus.PROCESSING else com.example.platisa.core.domain.model.PaymentStatus.UNPAID
                )
                val receiptId = repository.insertReceipt(receipt, _epsData.value?.billingPeriod)
                android.util.Log.d("ReviewVM", "✅ Receipt saved successfully! ID: $receiptId, Invoice: $invoiceNumber")

                // Save EPS data for new receipts - use cached data which has Gemini's recipient info
                val epsData = _epsData.value
                if (epsData != null) {
                    repository.insertEpsData(epsData, receiptId)
                    android.util.Log.d("ReviewVM", "✅ EPS data saved with recipient: ${epsData.recipientName}")
                }
                
                // Save Receipt Items (Price Comparison)
                val items = _parsedReceipt.value?.items
                if (!items.isNullOrEmpty()) {
                    repository.insertReceiptItems(items, receiptId)
                    android.util.Log.d("ReviewVM", "✅ Saved ${items.size} receipt items for ID: $receiptId")
                }
            }
        }
    }
    
    fun saveQrCodeToGallery(merchant: String, total: String, dateStr: String) {
        launchCatching {
            val qrData = _parsedReceipt.value?.qrCodeData ?: return@launchCatching
            
            val qrUri = com.example.platisa.core.common.QrSaveManager.saveEnhancedQrToGallery(
                context = context,
                qrData = qrData,
                merchantName = merchant,
                amount = total,
                date = dateStr
            )
            
            qrUri?.let { uri ->
                lastSavedQrUri = uri.toString()
                
                // Update payment status to PROCESSING and store URI if this is an existing receipt
                if (existingReceiptId != 0L) {
                    val receipt = repository.getReceiptById(existingReceiptId)
                    receipt?.let { existingReceipt ->
                        repository.updateReceipt(
                            existingReceipt.copy(
                                paymentStatus = com.example.platisa.core.domain.model.PaymentStatus.PROCESSING,
                                savedQrUri = uri.toString()
                            )
                        )
                    }
                }
                
                // Show toast notification
                android.widget.Toast.makeText(context, "QR kod sačuvan u galeriju!", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}
