package com.example.platisa.ui.navigation

import android.net.Uri

sealed class Screen(val route: String, val title: String) {
    data object Home : Screen("home", "Režije")
    data object Market : Screen("market", "Market")
    data object Analytics : Screen("analytics", "Podaci")
    data object Settings : Screen("settings?openSection={openSection}", "Podešavanja") {
        fun createRoute(openSection: String? = null): String {
            return if (openSection != null) {
                "settings?openSection=$openSection"
            } else {
                "settings"
            }
        }
    }
    data object Profile : Screen("profile", "Profil")
    data object Help : Screen("help", "Pomoć")
    data object Camera : Screen("camera", "Kamera")
    data object ReviewReceipt : Screen("review_receipt/{imageUri}?qrData={qrData}", "Pregled Računa") {
        fun createRoute(imageUri: String, qrData: String? = null): String {
            val encodedUri = Uri.encode(imageUri)
            return if (qrData != null) {
                "review_receipt/$encodedUri?qrData=${Uri.encode(qrData)}"
            } else {
                "review_receipt/$encodedUri"
            }
        }
    }
    data object EpsAnalytics : Screen("eps_analytics", "EPS Statistika")
    data object ReceiptDetail : Screen("receipt_detail/{receiptId}", "Detalji Računa") {
        fun createRoute(receiptId: Long) = "receipt_detail/$receiptId"
    }
    data object BillDetails : Screen("bill_details/{billId}", "Detalji Računa") {
        fun createRoute(billId: String) = "bill_details/$billId"
    }
    data object Search : Screen("search", "Pretraga")
    data object Diagnostics : Screen("diagnostics", "Dijagnostika")
    data object Splash : Screen("splash", "Splash")
    data object Greetings : Screen("greetings", "Dobrodošli")
    data object FiscalReceiptDetails : Screen("fiscal_receipt_details/{receiptId}", "Fiskalni Račun") {
        fun createRoute(receiptId: Long) = "fiscal_receipt_details/$receiptId"
    }
    data object ScanTimeframe : Screen("scan_timeframe", "Period Skeniranja")
    data object SubscriptionPaywall : Screen("subscription_paywall", "Pretplata")
    data object Login : Screen("login?autoLogin={autoLogin}", "Prijavi se") {
        fun createRoute(autoLogin: Boolean = false): String {
            return "login?autoLogin=$autoLogin"
        }
    }
    data object SyncWait : Screen("sync_wait", "Sinhronizacija")
    data object Walkthrough : Screen("walkthrough", "Uputstvo")
}
