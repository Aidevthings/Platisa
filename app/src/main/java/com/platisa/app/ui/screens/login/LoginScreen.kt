package com.platisa.app.ui.screens.login

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.platisa.app.R
import com.platisa.app.core.common.BaseScreen
import com.platisa.app.core.common.GoogleAuthManager
import com.platisa.app.ui.navigation.Screen
import com.platisa.app.ui.screens.settings.SettingsViewModel
// Removed hardcoded colors - using LocalPlatisaColors instead
import kotlinx.coroutines.launch

import com.platisa.app.ui.theme.LocalPlatisaColors

@Composable
fun LoginScreen(
    navController: NavController,
    autoLogin: Boolean = false, // Auto-trigger sign-in when true (e.g., after account deletion)
    viewModel: SettingsViewModel = hiltViewModel() // Reusing SettingsViewModel for auth logic
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val customColors = LocalPlatisaColors.current
    var hasAutoLaunched by remember { mutableStateOf(false) } // Prevent infinite loops
    
    var isLoading by remember { mutableStateOf(false) }
    
    // Store launcher in a ref so LaunchedEffect can access it
    val launcherRef = remember { mutableStateOf<androidx.activity.result.ActivityResultLauncher<android.content.Intent>?>(null) }
    
    // Debug helper to show user their correct SHA1
    var debugSha1 by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            isLoading = true
            scope.launch {
                try {
                    val account = GoogleAuthManager.signIn(result.data ?: return@launch)
                    if (account != null) {
                        // 2. Exchange Google Token for Firebase Credential
                        val isFirebaseAuthSuccess = GoogleAuthManager.firebaseAuthWithGoogle(account)
                        
                        if (isFirebaseAuthSuccess) {
                            Toast.makeText(context, "Dobrodošli: ${account.email}", Toast.LENGTH_SHORT).show()
                            viewModel.setConnectedAccount(account.email)
                            // Note: We DO NOT trigger sync here anymore. The ScanTimeframeScreen will trigger it.

                            // Navigate to ScanTimeframe instead of SyncWait to let user choose period
                            navController.navigate(Screen.ScanTimeframe.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        } else {
                            Toast.makeText(context, "Greška pri povezivanju sa serverom (Firebase Auth)", Toast.LENGTH_LONG).show()
                            GoogleAuthManager.signOut(context) {} // Cleanup
                        }
                    } else {
                        Toast.makeText(context, "Greška pri prijavi", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: com.google.android.gms.common.api.ApiException) {
                    android.util.Log.e("LoginScreen", "Sign-in failed with code: ${e.statusCode}", e)
                    
                    // SHOW THE SHA-1 TO THE USER
                    if (e.statusCode == 7 || e.statusCode == 10) {
                         // Copy to clipboard for easy sharing
                         val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                         val clip = android.content.ClipData.newPlainText("SHA1", debugSha1)
                         clipboard.setPrimaryClip(clip)
                         
                         android.app.AlertDialog.Builder(context)
                            .setTitle("Greška u konfiguraciji (Status ${e.statusCode})")
                            .setMessage("Aplikacija ima pogrešan potpis.\n\nVaš SHA-1 je:\n$debugSha1\n\n(Kopirano u clipboard)\n\nOvo mora biti uneto u Firebase konzolu.")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                    
                    Toast.makeText(context, "Greška (${e.statusCode}): Proverite Google Play Services", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Greška: ${e.message}", Toast.LENGTH_SHORT).show()
                    android.util.Log.e("LoginScreen", "Sign-in error", e)
                } finally {
                    isLoading = false
                }
            }
        } else {
            val code = result.resultCode
            android.util.Log.e("LoginScreen", "Sign-in FAILED/CANCELLED. ResultCode: $code")
            
            // Attempt to get more details from the intent if possible, even on failure
            if (result.data != null) {
                try {
                    val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    task.addOnFailureListener { e ->
                        if (e is com.google.android.gms.common.api.ApiException) {
                            android.util.Log.e("LoginScreen", "Detailed ApiException: ${e.statusCode}", e)
                            Toast.makeText(context, "Greška pri prijavi (Status: ${e.statusCode})", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LoginScreen", "Error parsing failed intent", e)
                }
            }

            if (code == Activity.RESULT_CANCELED) {
                // It was cancelled by user OR by system due to config error
                Toast.makeText(context, "Prijava otkazana (proverite internet/podešavanja)", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Prijava nije uspela (Code: $code)", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // Store launcher in ref for LaunchedEffect access
    launcherRef.value = launcher
    
    // Auto-trigger Google Sign-In when redirected from account deletion
    LaunchedEffect(autoLogin) {
        if (autoLogin && !hasAutoLaunched) {
            hasAutoLaunched = true
            android.util.Log.d("LoginScreen", "Auto-launching Google Sign-In (from account deletion)")
            kotlinx.coroutines.delay(300) // Brief delay for UI to settle
            val signInIntent = com.platisa.app.core.common.GoogleAuthManager.getSignInClient(context).signInIntent
            launcherRef.value?.launch(signInIntent)
        }
    }


    if (isLoading) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, customColors.neonCyan)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = customColors.neonCyan,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Prijavljivanje...",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // Auto-launch removed to prevent annoying popups on every app start
    // user must manually click the button.

    BaseScreen(viewModel = viewModel) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            com.platisa.app.ui.components.AppBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.weight(1f))
                
                // Logo or App Name
                Text(
                    text = "Platiša",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = customColors.neonCyan,
                    letterSpacing = 4.sp
                )
                
                Text(
                    text = "Vaš pametni asistent za račune",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Login Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Povežite Gmail nalog",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            text = "Da bismo automatski pronašli vaše račune, potrebno je da se prijavite.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Napomena: Aplikacija podržava isključivo Gmail naloge.",
                            fontSize = 12.sp,
                            color = customColors.neonCyan,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Button(
                            onClick = {
                                if (com.platisa.app.core.common.NetworkUtils.isNetworkAvailable(context)) {
                                    val availability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
                                    val resultCode = availability.isGooglePlayServicesAvailable(context)
                                    
                                    if (resultCode == com.google.android.gms.common.ConnectionResult.SUCCESS) {
                                        // 1. Validate Web Client ID Resource existence
                                        val webClientIdId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                                        if (webClientIdId == 0) {
                                            Toast.makeText(context, "FATAL: google-services.json nije generisao ID!", Toast.LENGTH_LONG).show()
                                            return@Button
                                        }
                                        val webClientId = context.getString(webClientIdId)
                                        android.util.Log.d("LoginScreen", "DEBUG: WebClientID: $webClientId")
                                        
                                        // 2. Validate App Signature (SHA-1)
                                        try {
                                            val info = context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNATURES)
                                            for (signature in info.signatures) {
                                                val md = java.security.MessageDigest.getInstance("SHA-1")
                                                md.update(signature.toByteArray())
                                                val digest = md.digest()
                                                val hexString = StringBuilder()
                                                for (b in digest) {
                                                    hexString.append(String.format("%02X:", b))
                                                }
                                                if (hexString.isNotEmpty()) hexString.setLength(hexString.length - 1)
                                                // Update State for Error Dialog
                                                debugSha1 = hexString.toString()
                                                android.util.Log.d("LoginScreen", "DEBUG APP SHA-1: $debugSha1")
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("LoginScreen", "Cannot get signature", e)
                                        }

                                        // CHECK: If it doesn't match expected (optional check)
                                        // val expected = "F9:45:AE:C1..." 
                                        
                                        // 3. Direct / "Attached" Sign-In Intent Construction
                                        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                                            .requestIdToken(webClientId)
                                            .requestEmail()
                                            .requestScopes(com.google.android.gms.common.api.Scope(com.google.api.services.gmail.GmailScopes.GMAIL_READONLY))
                                            .build()
                                            
                                        val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                                        
                                        // Save SHA1 to Helper or ViewModel to show in error if needed, 
                                        // for now we trust Logcat, but user can't see Logcat easily.
                                        // Hack: Show it in a long Toast just in case
                                        // Toast.makeText(context, "SHA-1: $currentSha1", Toast.LENGTH_LONG).show()
                                        
                                        // 4. Launch
                                        launcher.launch(client.signInIntent)
                                        
                                    } else {
                                        if (availability.isUserResolvableError(resultCode)) {
                                            availability.getErrorDialog(context as Activity, resultCode, 9000)?.show()
                                        } else {
                                            Toast.makeText(context, "Google Play servisi nisu podržani (Code: $resultCode)", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Nema internet konekcije! Proverite vezu.", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = customColors.neonCyan
                            ),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text(
                                "Prijavi se sa Google-om",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

