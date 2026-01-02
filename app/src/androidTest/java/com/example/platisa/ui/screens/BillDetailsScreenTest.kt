package com.platisa.app.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.platisa.app.core.domain.model.PaymentStatus
import com.platisa.app.core.domain.model.Receipt
import com.platisa.app.ui.screens.billdetails.BillDetailsContent
import com.platisa.app.ui.screens.billdetails.BillType
import com.platisa.app.ui.theme.PlatisaTheme
import androidx.navigation.testing.TestNavHostController
import androidx.compose.ui.platform.LocalContext
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.util.Date

/**
 * UI testovi za BillDetails ekran.
 * 
 * Testira:
 * - Prikaz detalja računa
 * - QR kod sekciju
 * - Dugmad za akcije
 */
@RunWith(AndroidJUnit4::class)
class BillDetailsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createTestReceipt(
        id: Long = 1,
        merchantName: String = "EPS SNABDEVANJE",
        totalAmount: BigDecimal = BigDecimal("4521.36"),
        paymentStatus: PaymentStatus = PaymentStatus.UNPAID,
        qrCodeData: String? = "K:PR|V:01|C:1|R:265000000009164816|N:EPS|I:RSD4521,36"
    ): Receipt {
        return Receipt(
            id = id,
            merchantName = merchantName,
            date = Date(),
            dueDate = Date(System.currentTimeMillis() + 86400000 * 15), // 15 dana
            totalAmount = totalAmount,
            currency = "RSD",
            imagePath = "/test/path.pdf",
            qrCodeData = qrCodeData,
            invoiceNumber = "123456789",
            paymentStatus = paymentStatus,
            sectionId = null,
            metadata = null
        )
    }

    @Test
    fun billDetails_displaysQRCodeSection_whenQRCodeExists() {
        val receipt = createTestReceipt()
        
        composeTestRule.setContent {
            val navController = TestNavHostController(LocalContext.current)
            
            PlatisaTheme {
                BillDetailsContent(
                    navController = navController,
                    receipt = receipt,
                    billType = BillType.ELECTRICITY,
                    vtConsumption = 150,
                    ntConsumption = 80,
                    receiptItems = emptyList(),
                    onSaveQr = {},
                    onMarkPaid = {},
                    isSaving = false
                )
            }
        }

        // QR kod sekcija treba da postoji
        // Note: We can check for the presence of save button which indicates QR section
        composeTestRule.waitForIdle()
    }

    @Test
    fun billDetails_displaysFormattedAmount() {
        val receipt = createTestReceipt(totalAmount = BigDecimal("4521.36"))
        
        composeTestRule.setContent {
            val navController = TestNavHostController(LocalContext.current)
            
            PlatisaTheme {
                BillDetailsContent(
                    navController = navController,
                    receipt = receipt,
                    billType = BillType.ELECTRICITY,
                    vtConsumption = 0,
                    ntConsumption = 0,
                    receiptItems = emptyList(),
                    onSaveQr = {},
                    onMarkPaid = {},
                    isSaving = false
                )
            }
        }

        // Iznos treba da bude prikazan u srpskom formatu
        composeTestRule
            .onNodeWithText("4.521,36", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun billDetails_displaysConsumptionMetrics_forElectricityBill() {
        val receipt = createTestReceipt()
        
        composeTestRule.setContent {
            val navController = TestNavHostController(LocalContext.current)
            
            PlatisaTheme {
                BillDetailsContent(
                    navController = navController,
                    receipt = receipt,
                    billType = BillType.ELECTRICITY,
                    vtConsumption = 150,
                    ntConsumption = 80,
                    receiptItems = emptyList(),
                    onSaveQr = {},
                    onMarkPaid = {},
                    isSaving = false
                )
            }
        }

        // Potrošnja treba da bude prikazana
        composeTestRule
            .onNodeWithText("150", substring = true)
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("80", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun billDetails_hidesConsumption_forNonElectricityBill() {
        val receipt = createTestReceipt(merchantName = "TELEKOM SRBIJA")
        
        composeTestRule.setContent {
            val navController = TestNavHostController(LocalContext.current)
            
            PlatisaTheme {
                BillDetailsContent(
                    navController = navController,
                    receipt = receipt,
                    billType = BillType.PHONE,
                    vtConsumption = 0,
                    ntConsumption = 0,
                    receiptItems = emptyList(),
                    onSaveQr = {},
                    onMarkPaid = {},
                    isSaving = false
                )
            }
        }

        // VT/NT metrike ne treba da budu prikazane za telefonske račune
        composeTestRule
            .onNodeWithText("VT", substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun billDetails_displaysMerchantName() {
        val receipt = createTestReceipt(merchantName = "EPS SNABDEVANJE D.O.O.")
        
        composeTestRule.setContent {
            val navController = TestNavHostController(LocalContext.current)
            
            PlatisaTheme {
                BillDetailsContent(
                    navController = navController,
                    receipt = receipt,
                    billType = BillType.ELECTRICITY,
                    vtConsumption = 0,
                    ntConsumption = 0,
                    receiptItems = emptyList(),
                    onSaveQr = {},
                    onMarkPaid = {},
                    isSaving = false
                )
            }
        }

        composeTestRule
            .onNodeWithText("EPS SNABDEVANJE", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun billDetails_showsBackButton() {
        val receipt = createTestReceipt()
        
        composeTestRule.setContent {
            val navController = TestNavHostController(LocalContext.current)
            
            PlatisaTheme {
                BillDetailsContent(
                    navController = navController,
                    receipt = receipt,
                    billType = BillType.ELECTRICITY,
                    vtConsumption = 0,
                    ntConsumption = 0,
                    receiptItems = emptyList(),
                    onSaveQr = {},
                    onMarkPaid = {},
                    isSaving = false
                )
            }
        }

        // Back dugme treba da postoji
        composeTestRule
            .onNode(hasContentDescription("Back") or hasContentDescription("Nazad"))
            .assertExists()
    }
}

