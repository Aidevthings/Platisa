package com.platisa.app.ui.screens.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.platisa.app.ui.theme.PlatisaTheme

@Composable
fun TrialPopup(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = PlatisaTheme.colors.surfaceContainer.copy(alpha = 0.95f)
            ),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, PlatisaTheme.colors.neonCyan),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon / Number
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .background(PlatisaTheme.colors.neonCyan.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape)
                        .border(2.dp, PlatisaTheme.colors.neonCyan, androidx.compose.foundation.shape.CircleShape)
                ) {
                    Text(
                        text = "3",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlatisaTheme.colors.neonCyan
                    )
                }
                
                Text(
                    text = "3 Meseca Besplatno!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlatisaTheme.colors.textPrimary,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Dobrodošli! Kao novi korisnik, dobijate pun pristup svim Premium funkcijama potpuno besplatno naredna 3 meseca.",
                    fontSize = 16.sp,
                    color = PlatisaTheme.colors.textPrimary.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PlatisaTheme.colors.neonCyan),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(
                        text = "Super, hvala!",
                        color = PlatisaTheme.colors.textOnPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

