package com.example.platisa.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.platisa.core.common.BaseScreen
import java.io.File

import com.example.platisa.ui.theme.LocalPlatisaColors

// Colors matching HomeScreen
// Refactored to use Theme


@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val userName by viewModel.userName.collectAsState()
    val avatarPath by viewModel.avatarPath.collectAsState()
    val celebrationImagePath by viewModel.celebrationImagePath.collectAsState()
    val splashStyle by viewModel.splashScreenStyle.collectAsState()
    val customColors = LocalPlatisaColors.current
    val context = LocalContext.current
    
    var showNameDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(userName) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setAvatarFromUri(it) }
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            // Save bitmap and set as avatar
            // Implementation needed for saving bitmap to file
        }
    }

    BaseScreen(viewModel = viewModel) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header with back button (Fixed at top)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.popBackStack() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Nazad",
                        tint = customColors.neonCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Profil",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                // Avatar Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Current Avatar Display
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .shadow(20.dp, CircleShape, spotColor = customColors.neonCyan.copy(alpha = 0.6f))
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(customColors.neonCyan, customColors.neonPurple)
                                )
                            )
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { showNameDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarPath != null) {
                            // Show custom avatar
                            Image(
                                painter = rememberAsyncImagePainter(
                                    if (avatarPath!!.startsWith("custom:")) {
                                        File(avatarPath!!.removePrefix("custom:"))
                                    } else {
                                        // Predefined avatar using resource identifier
                                        val resName = avatarPath!!.removePrefix("predefined:")
                                        context.resources.getIdentifier(resName, "drawable", context.packageName)
                                    }
                                ),
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Avatar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // User Name
                    Text(
                        text = userName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    TextButton(onClick = { showNameDialog = true }) {
                        Text(
                            text = "Promeni ime",
                            color = customColors.neonCyan,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Avatar Upload Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = customColors.neonCyan
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.neonCyan)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Galerija")
                    }
                    
                    OutlinedButton(
                        onClick = { cameraLauncher.launch(null) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = customColors.neonCyan
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.neonCyan)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Kamera")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Predefined Avatars Section
                SectionTitle("Izaberi Avatar")
                
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val avatarChunks = viewModel.predefinedAvatars.chunked(4)
                    avatarChunks.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { avatarName ->
                                Box(modifier = Modifier.weight(1f)) {
                                    AvatarItem(
                                        avatarName = avatarName,
                                        isSelected = avatarPath == "predefined:$avatarName",
                                        onClick = { viewModel.setAvatarFromPredefined(avatarName) }
                                    )
                                }
                            }
                            repeat(4 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Celebration Images Section
                SectionTitle("Poruke Obeležavanja")
                Text(
                    text = "Prikazuje se kada su svi računi plaćeni",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val celebrationChunks = viewModel.celebrationImages.chunked(3)
                    celebrationChunks.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { imageName ->
                                Box(modifier = Modifier.weight(1f)) {
                                    CelebrationImageItem(
                                        imageName = imageName,
                                        isSelected = celebrationImagePath == "predefined:$imageName",
                                        onClick = { 
                                            viewModel.setCelebrationImage(imageName)
                                            android.widget.Toast.makeText(context, "Uspešno promenjeno!", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Splash Screen Section
                SectionTitle("Odabir Početnog Ekrana")
                Text(
                    text = "Izaberi pozadinsku sliku za učitavanje aplikacije",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val splashChunks = viewModel.splashOptions.chunked(2)
                    splashChunks.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { option ->
                                Box(modifier = Modifier.weight(1f)) {
                                    SplashGridItem(
                                        option = option,
                                        isSelected = splashStyle == option.id,
                                        onClick = { viewModel.setSplashScreenStyle(option.id) }
                                    )
                                }
                            }
                            repeat(2 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    }

    // Name Edit Dialog
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Promeni Ime", color = customColors.neonCyan) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Tvoje ime") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = customColors.neonCyan,
                        focusedLabelColor = customColors.neonCyan,
                        cursorColor = customColors.neonCyan,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface 
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setUserName(tempName)
                        showNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = customColors.neonCyan
                    )
                ) {
                    Text("Sačuvaj", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    tempName = userName
                    showNameDialog = false 
                }) {
                    Text("Otkaži", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun AvatarItem(
    avatarName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val customColors = LocalPlatisaColors.current
    Box(
        modifier = Modifier
            .size(70.dp)
            .clip(CircleShape)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) customColors.neonCyan else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val resId = context.resources.getIdentifier(avatarName, "drawable", context.packageName)
        if (resId != 0) {
             Image(
                painter = rememberAsyncImagePainter(model = resId),
                contentDescription = avatarName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
             Icon(
                imageVector = Icons.Default.Person,
                contentDescription = avatarName,
                tint = if (isSelected) customColors.neonCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
fun CelebrationImageItem(
    imageName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val customColors = LocalPlatisaColors.current
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) customColors.statusPaid else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val resId = context.resources.getIdentifier(imageName, "drawable", context.packageName)
        if (resId != 0) {
             Image(
                painter = rememberAsyncImagePainter(model = resId),
                contentDescription = imageName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
             Icon(
                imageVector = Icons.Default.Celebration,
                contentDescription = imageName,
                tint = if (isSelected) customColors.statusPaid else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}
@Composable
fun SplashGridItem(
    option: SplashOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val customColors = LocalPlatisaColors.current
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f/16f)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) customColors.neonCyan else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(id = option.drawableRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(customColors.neonCyan.copy(alpha = 0.2f))
            )
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = customColors.neonCyan,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
                    .background(Color.White, CircleShape)
            )
        }
    }
}
