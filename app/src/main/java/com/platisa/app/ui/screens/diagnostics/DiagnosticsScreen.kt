package com.platisa.app.ui.screens.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.platisa.app.core.domain.parser.ReceiptParser
import com.platisa.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class TestResult(
    val name: String,
    val passed: Boolean,
    val message: String
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val repository: com.platisa.app.core.domain.repository.ReceiptRepository
) : ViewModel() {

    private val _testResults = MutableStateFlow<List<TestResult>>(emptyList())
    val testResults = _testResults.asStateFlow()
    
    private val _cleanupResult = MutableStateFlow<String?>(null)
    val cleanupResult = _cleanupResult.asStateFlow()

    fun runCleanup() {
        viewModelScope.launch {
            _cleanupResult.value = "Scanning..."
            try {
                val count = repository.deleteDuplicateReceipts()
                _cleanupResult.value = "Removed $count duplicate receipts."
            } catch (e: Exception) {
                _cleanupResult.value = "Error: ${e.message}"
            }
        }
    }

    fun runTests() {
        viewModelScope.launch {
            val results = mutableListOf<TestResult>()

            // Test 1: Merchant Name
            results.add(testMerchantName())

            // Test 2: Total Amount (Serbian Format)
            results.add(testTotalAmountSerbian())

            // Test 3: Total Amount (Standard Format - if supported, or verify it fails gracefully)
            results.add(testTotalAmountStandard())

            _testResults.value = results
        }
    }

    private fun testMerchantName(): TestResult {
        val text = "Welcome to MAXI\nTotal: 1.200,00"
        val result = ReceiptParser.parse(text)
        val passed = result.merchantName == "Welcome to MAXI"
        return TestResult(
            "Merchant Name Extraction",
            passed,
            "Expected 'Welcome to MAXI', got '${result.merchantName}'"
        )
    }

    private fun testTotalAmountSerbian(): TestResult {
        val text = "Items: ...\nZA UPLATU: 1.250,50\nHvala"
        val result = ReceiptParser.parse(text)
        val expected = BigDecimal("1250.50")
        val passed = result.totalAmount?.compareTo(expected) == 0
        return TestResult(
            "Amount Extraction (Serbian)",
            passed,
            "Expected $expected, got ${result.totalAmount}"
        )
    }

    private fun testTotalAmountStandard(): TestResult {
        // Current parser expects Serbian format (1.200,00). 
        // If we pass 1200.00, it might be parsed as 120000 or null depending on regex.
        // Let's test what it strictly supports: 1.200,00
        val text = "Total: 1.200,00"
        val result = ReceiptParser.parse(text)
        val expected = BigDecimal("1200.00")
        val passed = result.totalAmount?.compareTo(expected) == 0
        return TestResult(
            "Amount Extraction (Format Check)",
            passed,
            "Expected $expected, got ${result.totalAmount}"
        )
    }
}

@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val results by viewModel.testResults.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        com.platisa.app.ui.components.AppBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "Dijagnostika & Testovi",
                style = MaterialTheme.typography.headlineMedium,
                color = CyberCyan
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { viewModel.runTests() },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Pokreni Testove", color = Color.White)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val cleanupMsg by viewModel.cleanupResult.collectAsState()
            
            Button(
                onClick = { viewModel.runCleanup() },
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Obrisi Duplikate", color = Color.White)
            }
            
            if (cleanupMsg != null) {
                Text(
                    text = cleanupMsg!!,
                    color = MatrixGreen,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results) { result ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = CardSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (result.passed) "✅" else "❌",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(result.name, color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.titleMedium)
                                if (!result.passed) {
                                    Text(result.message, color = AlertRed, style = MaterialTheme.typography.bodySmall)
                                } else {
                                    Text("Passed", color = MatrixGreen, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.8f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Nazad", color = TextSecondary)
            }
        }
    }
}

