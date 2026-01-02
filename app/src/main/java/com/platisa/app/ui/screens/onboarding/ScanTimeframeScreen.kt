package com.platisa.app.ui.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.platisa.app.R
import com.platisa.app.core.worker.GmailSyncWorker
import com.platisa.app.ui.navigation.Screen
import com.platisa.app.ui.components.DynamicSizeText
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.foundation.layout.BoxWithConstraints

// --- GLOBAL COLORS ---
private val ColorBackground = Color(0xFF1E1E2C)
private val ColorPanelBg = Color(0xFF2C2C34)
private val ColorCyan = Color(0xFF00EAF0)    // 1 Month
private val ColorOrange = Color(0xFFFF8C00)  // 3 Months
private val ColorPink = Color(0xFFFF00D9)    // 6 Months
private val ColorSilver = Color(0xFFE0E0E0)  // 0 Days (Fresh Start)
private val ColorTextGray = Color(0xFFAAAAAA) // Secondary text

// --- TYPOGRAPHY ---
// Orbitron for the "Techno" look on numbers
val OrbitronFont = FontFamily(
    androidx.compose.ui.text.font.Font(R.font.orbitron)
)
// Standard Sans-Serif for readability on descriptions
val ReadableFont = FontFamily.Default

// 3D Text Shadows
private val ShadowNumber3D = Shadow(
    color = Color.White.copy(alpha = 0.7f),
    offset = Offset(2f, 2f),
    blurRadius = 8f
)

private val ShadowLabel = Shadow(
    color = Color.White.copy(alpha = 0.5f),
    offset = Offset(1f, 1f),
    blurRadius = 4f
)

private val ShadowTitle = Shadow(
    color = Color.White.copy(alpha = 0.4f),
    offset = Offset(1f, 1f),
    blurRadius = 3f
)

@Composable
fun ScanTimeframeScreen(
    navController: NavController,
    viewModel: ScanTimeframeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val workManager = WorkManager.getInstance(context)
    val isLoading by viewModel.isLoading.collectAsState()

    // Auth Check
    LaunchedEffect(Unit) {
        val account = com.platisa.app.core.common.GoogleAuthManager.getSignedInAccount(context)
        if (account == null) {
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.ScanTimeframe.route) { inclusive = true }
            }
        }
    }

    fun startSync(days: Int) {
        viewModel.checkTrialAndProceed(
            onProceed = {
                val inputData = workDataOf(
                    GmailSyncWorker.KEY_LOOKBACK_DAYS to days,
                    GmailSyncWorker.KEY_FORCE_FULL_SYNC to false
                )
                val syncRequest = OneTimeWorkRequestBuilder<GmailSyncWorker>()
                    .setInputData(inputData)
                    // Tag is still useful for other queries but UniqueWork is primary
                    .addTag("GmailSyncOneTime") 
                    .build()
                
                // Use UniqueWork to ensure SyncWaitViewModel can observe it correctly via getWorkInfosForUniqueWorkFlow
                workManager.enqueueUniqueWork(
                    "GmailSyncOneTime",
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    syncRequest
                )
                navController.navigate(Screen.SyncWait.route)
            },
            onExpired = {
                navController.navigate(Screen.SubscriptionPaywall.route) {
                    popUpTo(Screen.ScanTimeframe.route) { inclusive = true }
                }
            },
            onError = { errorMsg ->
                android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_LONG).show()
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.pozadina),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 2. Title "Period Pretrage"
            DynamicSizeText(
                text = "Period Pretrage",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .wrapContentWidth(Alignment.CenterHorizontally),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = ReadableFont,
                maxFontSize = 36.sp
            )

            // 3. Selection Blocks
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Block 1: Svež Početak (Cyan) - REPLACES 1 Month
                PeriodSelectionBlock(
                    periodNumber = "0",
                    periodText = "Dana",
                    descriptionText = "Svež Početak",
                    subtitleText = "Samo budući računi",
                    baseColor = ColorCyan,
                    onClick = { startSync(0) }
                )

                // Block 2: 1 Mesec (Orange) - MOVED from Block 1
                PeriodSelectionBlock(
                    periodNumber = "1",
                    periodText = "Mesec",
                    descriptionText = "Poslednji mesec",
                    subtitleText = "Skeniranje poslednjih 30 dana",
                    baseColor = ColorOrange,
                    onClick = { startSync(30) }
                )

                // Block 3: 3 Meseca (Pink) - MOVED from Block 2
                PeriodSelectionBlock(
                    periodNumber = "3",
                    periodText = "Meseca",
                    descriptionText = "Kvartalno",
                    subtitleText = "Skeniranje poslednjih 90 dana",
                    baseColor = ColorPink,
                    onClick = { startSync(90) }
                )
            }
        }

        if (isLoading) {
            LoadingOverlay()
        }
    }
}

@Composable
fun PeriodSelectionBlock(
    periodNumber: String,
    periodText: String,
    descriptionText: String,
    subtitleText: String,
    baseColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable(onClick = onClick)
            .drawBehind {
                val shadowColor = baseColor.copy(alpha = 0.4f)
                val transparent = baseColor.copy(alpha = 0f)
                
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(shadowColor, transparent),
                        center = center,
                        radius = size.width * 0.8f
                    ),
                    size = size,
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
            }
            .clip(RoundedCornerShape(16.dp))
            .background(baseColor.copy(alpha = 0.25f))
            .drawWithContent {
                drawContent()

                // 2. 3D Gradient Border
                val strokeWidth = 3.dp.toPx()
                val halfStroke = strokeWidth / 2f
                
                // Main Neon Border
                drawRoundRect(
                    color = baseColor.copy(alpha = 0.6f),
                    style = Stroke(width = strokeWidth),
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
                
                // 3D Bevel Overlay (White Top-Left -> Black Bottom-Right)
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.9f), // Highlight
                            baseColor.copy(alpha = 0.1f),   // Mid-tone transparent
                            Color.Black.copy(alpha = 0.8f)  // Shadow
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    ),
                    style = Stroke(width = strokeWidth),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    blendMode = BlendMode.Overlay
                )
                
                // 4. DECORATIVE: Tech Corner Brackets
                val bracketLen = 12.dp.toPx()
                val bracketStroke = 1.dp.toPx()
                val bracketColor = Color.White.copy(alpha = 0.5f)
                val padding = 8.dp.toPx() // Distance from edge
                
                // Top-Left
                drawLine(bracketColor, Offset(padding, padding), Offset(padding + bracketLen, padding), bracketStroke)
                drawLine(bracketColor, Offset(padding, padding), Offset(padding, padding + bracketLen), bracketStroke)
                
                // Top-Right
                drawLine(bracketColor, Offset(size.width - padding, padding), Offset(size.width - padding - bracketLen, padding), bracketStroke)
                drawLine(bracketColor, Offset(size.width - padding, padding), Offset(size.width - padding, padding + bracketLen), bracketStroke)

                // Bottom-Left
                drawLine(bracketColor, Offset(padding, size.height - padding), Offset(padding + bracketLen, size.height - padding), bracketStroke)
                drawLine(bracketColor, Offset(padding, size.height - padding), Offset(padding, size.height - padding - bracketLen), bracketStroke)

                // Bottom-Right
                drawLine(bracketColor, Offset(size.width - padding, size.height - padding), Offset(size.width - padding - bracketLen, size.height - padding), bracketStroke)
                drawLine(bracketColor, Offset(size.width - padding, size.height - padding), Offset(size.width - padding, size.height - padding - bracketLen), bracketStroke)
                
                // 5. DECORATIVE: Subtle Tech Line at bottom center
                drawLine(
                    color = baseColor.copy(alpha = 0.3f),
                    start = Offset(size.width * 0.3f, size.height - 4.dp.toPx()),
                    end = Offset(size.width * 0.7f, size.height - 4.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // --- LEFT SECTION: Number + Month ---
            Column(
                modifier = Modifier
                    .width(110.dp) // Slight increase to accommodate larger text
                    .fillMaxHeight()
                    .padding(start = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Big Number - 3D Effect
                Text3D(
                    text = periodNumber,
                    style = TextStyle(
                        fontFamily = OrbitronFont,
                        fontSize = 58.sp, // Enlarged from 48.sp
                        fontWeight = FontWeight.Bold
                    )
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Month Label - 3D Effect (Small)
                Text3D(
                    text = periodText,
                    style = TextStyle(
                        fontFamily = OrbitronFont,
                        fontSize = 20.sp, // Enlarged from 16.sp
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 1.sp
                    ),
                    offsetScale = 0.6f 
                )
            }

            // --- DIVIDER ---
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .padding(vertical = 12.dp)
                    .background(Color.White.copy(alpha = 0.2f))
            )

            // --- RIGHT SECTION: Description ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally // Always Center
            ) {
                // Main Description Title - 3D Effect
                Text3D(
                    text = descriptionText,
                    style = TextStyle(
                        fontFamily = ReadableFont, 
                        fontSize = 28.sp, // Enlarged from 22.sp
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    ),
                    offsetScale = 0.8f
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Subtitle - 3D Effect
                Text3D(
                    text = subtitleText,
                    style = TextStyle(
                        fontFamily = ReadableFont, 
                        fontSize = 14.sp, // Reduced to 14.sp for explanation
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.8f), 
                        textAlign = TextAlign.Center
                    ),
                    offsetScale = 0.4f // Reduced 3D offset for smaller text
                )
            }
        }
    }
}

@Composable
fun Text3D(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    offsetScale: Float = 1.0f
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        
        // Use the font size from style as maxFontSize
        val maxFontSize = style.fontSize
        val minFontSize = 10.sp 

        val availableWidthPx = with(density) { maxWidth.toPx() }

        val optimalFontSize = remember(text, availableWidthPx, maxFontSize) {
            if (maxFontSize == TextUnit.Unspecified) return@remember 20.sp // Fallback
            
            var currentSize = maxFontSize.value
            val minSize = minFontSize.value
            
            // Quick check if max size fits
            val maxSizeLayout = textMeasurer.measure(
                text = text,
                style = style, // Already has maxFontSize
                constraints = Constraints(maxWidth = Int.MAX_VALUE)
            )
            
            if (maxSizeLayout.size.width <= availableWidthPx) {
                return@remember maxFontSize
            }
            
            // Binary search
            var low = minSize
            var high = maxFontSize.value
            var bestSize = minSize
            
            while (low <= high) {
                val mid = (low + high) / 2f
                
                val layout = textMeasurer.measure(
                    text = text,
                    style = style.copy(fontSize = mid.sp),
                    constraints = Constraints(maxWidth = Int.MAX_VALUE)
                )
                
                if (layout.size.width <= availableWidthPx) {
                    bestSize = mid
                    low = mid + 1f
                } else {
                    high = mid - 1f
                }
            }
            
            bestSize.sp
        }

        val dynamicStyle = style.copy(fontSize = optimalFontSize)

        // Layer 3 (Deepest Shadow)
        Text(
            text = text,
            style = dynamicStyle.copy(color = Color(0xFF1F2937)), // Dark Gray
            modifier = Modifier.offset((3.dp * offsetScale), (3.dp * offsetScale))
        )
        // Layer 2 (Mid Shadow)
        Text(
            text = text,
            style = dynamicStyle.copy(color = Color(0xFF3E4C59)), // Slate Gray
            modifier = Modifier.offset((2.dp * offsetScale), (2.dp * offsetScale))
        )
        // Layer 1 (Light Shadow)
        Text(
            text = text,
            style = dynamicStyle.copy(color = Color(0xFF5F6C7B)), // Light Slate
            modifier = Modifier.offset((1.dp * offsetScale), (1.dp * offsetScale))
        )
        // Main Text (White with glow)
        Text(
            text = text,
            style = dynamicStyle.copy(
                color = Color.White,
                shadow = Shadow(
                    color = Color.White.copy(alpha = 0.5f),
                    offset = Offset(0f, 0f),
                    blurRadius = 8f * offsetScale
                )
            )
        )
    }
}

@Composable
fun LoadingOverlay() {
    androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ColorCyan)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = ColorCyan)
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Provera pretplate...",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

