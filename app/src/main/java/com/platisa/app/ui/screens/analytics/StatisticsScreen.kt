package com.platisa.app.ui.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.platisa.app.core.common.BaseScreen
import com.platisa.app.core.common.Formatters
import com.platisa.app.core.domain.model.BillCategory
import com.platisa.app.ui.theme.*
import java.math.BigDecimal
import com.platisa.app.ui.components.DynamicSizeText
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.foundation.layout.BoxWithConstraints

@OptIn(ExperimentalMaterial3Api::class)
// Global Padding Setting for Statistics Cards
private val GlobalCardPadding = 16.dp
private val DarkThemeStatsBorder = Color.Gray

@Composable
fun StatisticsScreen(
    navController: NavController,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val totalSpending by viewModel.totalSpending.collectAsState()
    val spendingByCategory by viewModel.spendingByCategory.collectAsState()
    val spendingTrends by viewModel.spendingTrends.collectAsState()
    val selectedGraphPeriod by viewModel.selectedGraphPeriod.collectAsState()
    
    // Using grocery/pharmacy stats from ViewModel
    val groceryStats by viewModel.groceryStats.collectAsState()
    val pharmacyStats by viewModel.pharmacyStats.collectAsState()
    val paymentStats by viewModel.paymentStats.collectAsState()
    val categoryDetails by viewModel.categoryDetails.collectAsState()
    
    // We only care about Dark Mode for this specific "Cyberpunk" look as per prompt.
    // However, if user is in Light mode, we should probably still show the Dark Cyberpunk UI 
    // because the prompt says "it's only for the dark theme... It's that page in our app...".
    // But to be safe and responsive, if system is light, maybe we should keep light?
    // The prompt says: "So it's only for the dark theme... I'm giving you the dark theme...".
    // I will enforce the Dark Cyberpunk look if isSystemInDarkTheme() is true.
    
    val isSystemDark = isSystemInDarkTheme()
    
    // For this specific design request, we will use the new Cyberpunk colors
    // IF the system is dark. If light, we keep existing logic or adapted logic.
    // FOR NOW, to ensure the user sees the change, I will prioritize the new design
    // when in dark mode.
    
    if (isSystemDark) {
        CyberpunkStatisticsScreen(
            navController = navController,
            viewModel = viewModel,
            selectedPeriod = selectedPeriod,
            totalSpending = totalSpending,
            spendingByCategory = spendingByCategory,
            spendingTrends = spendingTrends,
            selectedGraphPeriod = selectedGraphPeriod,
            paymentStats = paymentStats,
            groceryStats = groceryStats,

            pharmacyStats = pharmacyStats,
            categoryDetails = categoryDetails
        )
    } else {
        // Fallback to old implementation or a "Light" version of it
        // Since I am replacing the file, I must provide the implementation.
        // I will provide the Cyberpunk version as the main one, but wrapped to handle light mode if needed.
        // Actually, let's make it the default for now as the user specifically requested "Design the UI".
        CyberpunkStatisticsScreen(
            navController = navController,
            viewModel = viewModel,
            selectedPeriod = selectedPeriod,
            totalSpending = totalSpending,
            spendingByCategory = spendingByCategory,
            spendingTrends = spendingTrends,
            selectedGraphPeriod = selectedGraphPeriod,
            paymentStats = paymentStats,
            groceryStats = groceryStats,

            pharmacyStats = pharmacyStats,
            categoryDetails = categoryDetails
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberpunkStatisticsScreen(
    navController: NavController,
    viewModel: AnalyticsViewModel,
    selectedPeriod: TimePeriod,
    totalSpending: BigDecimal,
    spendingByCategory: Map<BillCategory, BigDecimal>,
    spendingTrends: List<Pair<String, Float>>,
    selectedGraphPeriod: GraphPeriod,
    paymentStats: PaymentStats,
    groceryStats: GroceryStats,
    pharmacyStats: PharmacyStats,
    categoryDetails: List<CategoryDetailedStats>
) {
    val scrollState = rememberScrollState()
    // Use LocalPlatisaColors.isDark for consistent in-app theme state
    val isDark = LocalPlatisaColors.current.isDark


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) CyberBackgroundDark else LightStatsBackground)
    ) {
        // Ambient Background Effect
        if (isDark) {
            AuroraBackground()
        } else {
            LightThemeBackground()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 1. Custom Header (Replaces TopAppBar to remove forced height gap)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 0.dp) // Reset horizontal
                    .padding(top = 12.dp) // Added comfortable top spacing
            ) {
                // Back Button
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(40.dp) // Defines the header height (40dp minimum)
                        .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDark) Color.White else LightStatsTextMain
                    )
                }

                // Title
                Text(
                    "Statistika",
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = if (isDark) Color.White else LightStatsTextMain,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .then(if (isDark) Modifier.shadow(10.dp, RoundedCornerShape(8.dp), ambientColor = CyberPrimary, spotColor = CyberPrimary) else Modifier)
                )
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp), 
                verticalArrangement = Arrangement.spacedBy(16.dp) // Reduced spacing between elements
            ) {
                Spacer(modifier = Modifier.height(0.dp)) // Removed gap between header and first card

                // 2. Monthly Spending & Period Selector
                MonthlySpendingSection(selectedPeriod, totalSpending, viewModel)

                // 3. Bill Status Grid (Unpaid / Paid)
                BillStatusGrid(paymentStats)

                // 4. Spending Trends Chart
                SpendingTrendsChartV2(spendingTrends, selectedGraphPeriod, viewModel)

                // 5. Category Pie Chart
                CategoryPieChart(spendingByCategory, totalSpending)

                // 6. Category Details (Replaces "Po Prodavnicama")
                CategoryDetailsSection(categoryDetails)
                
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun LightThemeBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Cyan Blob (Top Left)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF25D1F4).copy(alpha = 0.08f), Color.Transparent),
                center = Offset(width * -0.2f, height * -0.1f),
                radius = 600.dp.toPx()
            ),
            radius = 600.dp.toPx(),
            center = Offset(width * -0.2f, height * -0.1f)
        )
        
        // Purple/Green Blob (Top Right)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFA7F3D0).copy(alpha = 0.1f), Color.Transparent), // Emerald-ish
                center = Offset(width * 1.1f, height * 0.1f),
                radius = 500.dp.toPx()
            ),
             radius = 500.dp.toPx(),
            center = Offset(width * 1.1f, height * 0.1f)
        )
        
        // Blue Blob (Bottom Left)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFDBEAFE).copy(alpha = 0.4f), Color.Transparent),
                center = Offset(width * 0.2f, height * 1.1f),
                radius = 600.dp.toPx()
            ),
            radius = 600.dp.toPx(),
            center = Offset(width * 0.2f, height * 1.1f)
        )
    }
}

// --- Components ---

@Composable
fun AuroraBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Top Left Cyan/Primary
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(CyberPrimary.copy(alpha = 0.15f), Color.Transparent),
                center = Offset(0f, 0f),
                radius = 500.dp.toPx()
            ),
            center = Offset(0f, 0f),
            radius = 500.dp.toPx()
        )

        // Top Right Purple
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFA855F7).copy(alpha = 0.15f), Color.Transparent), // Purple-500
                center = Offset(w, h * 0.2f),
                radius = 400.dp.toPx()
            ),
            center = Offset(w, h * 0.2f),
            radius = 400.dp.toPx()
        )

        // Bottom Left Blue
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF2563EB).copy(alpha = 0.1f), Color.Transparent), // Blue-600
                center = Offset(w * 0.2f, h),
                radius = 600.dp.toPx()
            ),
            center = Offset(w * 0.2f, h),
            radius = 600.dp.toPx()
        )
    }
}

@Composable
fun MonthlySpendingSection(
    selectedperiod: TimePeriod, 
    totalSpending: BigDecimal, 
    viewModel: AnalyticsViewModel
) {
    // Use LocalPlatisaColors.isDark for consistent in-app theme state
    val isDark = LocalPlatisaColors.current.isDark

    val mainTextColor = if (isDark) Color.White else LightStatsTextMain
    val labelTextColor = if (isDark) Color.Gray else LightStatsTextMuted
    val primaryColor = if (isDark) CyberPrimary else LightStatsPrimary

    val (titleText, titleSize) = when (selectedperiod) {
        TimePeriod.MONTHLY -> "UKUPNA MESEČNA POTROŠNJA" to 14.sp
        TimePeriod.SIX_MONTHS -> "UKUPNA ŠESTOMESEČNA POTROŠNJA" to 11.sp // Smaller font to prevent breaking
        TimePeriod.YEARLY -> "UKUPNA GODIŠNJA POTROŠNJA" to 14.sp
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Hero Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .glassPanel(cornerRadius = 24.dp)
                .padding(GlobalCardPadding)
        ) {
            // Decorative Glow REMOVED

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = titleText,
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold,
                    color = labelTextColor,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                DynamicSizeText(
                    text = "${Formatters.formatCurrency(totalSpending)}",
                    maxFontSize = 56.sp,
                    minFontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = mainTextColor,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    modifier = if (isDark) Modifier.shadow(12.dp, RoundedCornerShape(8.dp), ambientColor = CyberPrimary, spotColor = CyberPrimary) else Modifier
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Growth Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.6f))
                        .border(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.White, CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "↑ 10%", 
                        color = primaryColor, 
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "u odnosu na prošli mesec",
                        color = labelTextColor,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Segmented Control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(
                    if(isDark) 1.dp else 2.dp,
                    if(isDark) DarkThemeStatsBorder else androidx.compose.ui.graphics.Color(0xFF795548),
                    RoundedCornerShape(16.dp)
                )
                .background(Color.Transparent) // No background to avoid 'gray padding'
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val periods = listOf(TimePeriod.MONTHLY to "1 Mesec", TimePeriod.SIX_MONTHS to "6 Meseci", TimePeriod.YEARLY to "1 Godina")
            
            periods.forEach { (period, label) ->
                val isSelected = period == selectedperiod
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) primaryColor.copy(alpha = 0.2f) else Color.Transparent
                        )
                        .clickable { viewModel.setPeriod(period) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) primaryColor else labelTextColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 15.sp, // Increased font size
                        modifier = if (isDark && isSelected) Modifier.shadow(10.dp, RoundedCornerShape(8.dp), ambientColor = CyberPrimary, spotColor = CyberPrimary) else Modifier
                    )
                }
            }
        }
    }
}

@Composable
fun BillStatusGrid(paymentStats: PaymentStats) {
    // Use LocalPlatisaColors.isDark for consistent in-app theme state
    val isDark = LocalPlatisaColors.current.isDark

    val mainTextColor = if (isDark) Color.White else LightStatsTextMain
    val labelTextColor = if (isDark) Color.Gray else LightStatsTextMuted
    val primaryColor = if (isDark) CyberPrimary else LightStatsPrimary
    val secondaryColor = if (isDark) CyberSecondary else LightStatsSecondary

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // Unpaid
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .glassPanel(cornerRadius = 24.dp)
                .padding(GlobalCardPadding)
        ) {
            // Icon Background
            Icon(
                Icons.Default.PendingActions,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
                    .alpha(if (isDark) 0.5f else 0.2f)
            )
            
            Column {
                Text("NEPLAĆENI", color = labelTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    Formatters.formatCurrencySimple(paymentStats.unpaidTotal), 
                    color = mainTextColor, 
                    fontSize = 24.sp, 
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${paymentStats.unpaidCount} Računa", 
                    color = primaryColor, 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Progress Bar
                LinearProgressIndicator(
                    progress = 0.75f, // Mock progress or calculate ratio
                    color = primaryColor,
                    trackColor = labelTextColor.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                )
            }
        }

        // Paid
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .glassPanel(cornerRadius = 24.dp)
                .padding(GlobalCardPadding)
        ) {
             Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = secondaryColor,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
                    .alpha(if (isDark) 0.5f else 0.2f)
            )
            
            Column {
                Text("PLAĆENI", color = labelTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    Formatters.formatCurrencySimple(paymentStats.paidTotal), 
                    color = mainTextColor, 
                    fontSize = 24.sp, 
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${paymentStats.paidCount} Računa", 
                    color = secondaryColor, 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))
                 LinearProgressIndicator(
                    progress = 0.25f, // Mock
                    color = secondaryColor,
                    trackColor = labelTextColor.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                )
            }
        }
    }
}


@Composable
fun SpendingTrendsChartV2(
    trends: List<Pair<String, Float>>,
    selectedPeriod: GraphPeriod,
    viewModel: AnalyticsViewModel
) {
    var chartType by remember { mutableStateOf(ChartType.LINE) }
    // Use LocalPlatisaColors.isDark for consistent in-app theme state
    val isDark = LocalPlatisaColors.current.isDark

    val mainTextColor = if (isDark) Color.White else LightStatsTextMain
    val labelTextColor = if (isDark) Color.Gray else LightStatsTextMuted
    val primaryColor = if (isDark) CyberPrimary else LightStatsPrimary
    val secondaryColor = if (isDark) CyberSecondary else LightStatsSecondary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .glassPanel(cornerRadius = 24.dp)
            .padding(GlobalCardPadding)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Trendovi Potrošnje",
                    color = mainTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                
                // Chart Type Toggle
                // Chart Type Selector
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Line Chart Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (chartType == ChartType.LINE) primaryColor.copy(alpha = 0.2f) 
                                else if (isDark) Color.White.copy(alpha = 0.05f) 
                                else Color.Black.copy(alpha = 0.05f)
                            )
                            .clickable { chartType = ChartType.LINE }
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Line Chart",
                            tint = if (chartType == ChartType.LINE) primaryColor else labelTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Bar Chart Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (chartType == ChartType.BAR) primaryColor.copy(alpha = 0.2f) 
                                else if (isDark) Color.White.copy(alpha = 0.05f) 
                                else Color.Black.copy(alpha = 0.05f)
                            )
                            .clickable { chartType = ChartType.BAR }
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Bar Chart",
                            tint = if (chartType == ChartType.BAR) primaryColor else labelTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Canvas Chart
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                if (trends.isEmpty()) {
                     Text("Nema podataka", color = labelTextColor, modifier = Modifier.align(Alignment.Center))
                } else {
                    val points = trends.map { it.second }
                    val maxDataValue = points.maxOrNull() ?: 1f
                    // Round up max value for nicer scaling (e.g. 135 -> 140 or 150)
                    val maxValue = if (maxDataValue == 0f) 100f else maxDataValue * 1.1f
                    
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val chartHeight = height * 0.85f // Leave space for X axis labels if drawn in canvas, or just padding
                        val chartWidth = width * 0.85f // Leave space for Y axis labels
                        val yAxisLabelWidth = width * 0.15f
                        
                        // Divide into 4 horizontal lines (0%, 33%, 66%, 100%)
                        val steps = 3
                        val stepHeight = chartHeight / steps
                        
                        val textPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY // Keep gray for labels, or conform to theme if needed, but standard gray is usually fine
                            textSize = 30f
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }

                        // Draw Grid & Y-Labels
                        for (i in 0..steps) {
                            val y = chartHeight - (i * stepHeight)
                            val value = (maxValue / steps) * i
                            
                            // Grid Line
                            drawLine(
                                color = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f),
                                start = Offset(yAxisLabelWidth, y),
                                end = Offset(width, y),
                                strokeWidth = 2f,
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                            
                            // Y-Label
                            drawContext.canvas.nativeCanvas.drawText(
                                Formatters.formatCurrencySimple(value.toBigDecimal()),
                                yAxisLabelWidth - 10f, // Padding right of label
                                y + 10f, // Center vertically roughly
                                textPaint.apply { textSize = 34f } // Increased text size
                            )
                        }

                        val stepX = (width - yAxisLabelWidth) / (points.size.coerceAtLeast(1))
                        
                        if (chartType == ChartType.LINE) {
                            // LINE CHART LOGIC
                            val path = androidx.compose.ui.graphics.Path()
                            
                            points.forEachIndexed { index, value ->
                                val x = yAxisLabelWidth + (index * stepX) + (stepX / 2) // Center point in slot
                                val y = chartHeight - ((value / maxValue) * chartHeight)
                                
                                if (index == 0) path.moveTo(x, y) else {
                                    // Cubic Bezier for smooth lines? Or straight for accurate data?
                                    // Let's stick to straight for now like previous, or simple connection
                                    path.lineTo(x, y)
                                }
                                
                                // Draw Point
                                drawCircle(
                                    color = if (isDark) Color.White else primaryColor, // White dots in dark, primary in light
                                    radius = 6f,
                                    center = Offset(x, y)
                                )
                                drawCircle(
                                    color = primaryColor, // Glow/Border
                                    radius = 3f,
                                    center = Offset(x, y)
                                )
                            }
                            
                            // Gradient Fill
                            val fillPath = androidx.compose.ui.graphics.Path().apply {
                                addPath(path)
                                lineTo(yAxisLabelWidth + (points.size - 1) * stepX + (stepX / 2), chartHeight)
                                lineTo(yAxisLabelWidth + (stepX / 2), chartHeight)
                                close()
                            }
                            
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(primaryColor.copy(alpha = 0.2f), Color.Transparent),
                                    startY = 0f,
                                    endY = chartHeight
                                )
                            )
                            
                            // Stroke
                            drawPath(
                                path = path,
                                color = primaryColor,
                                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                            
                        } else {
                            // BAR CHART LOGIC
                            val barWidth = stepX * 0.6f // 60% of slot width
                            
                            points.forEachIndexed { index, value ->
                                val xCenter = yAxisLabelWidth + (index * stepX) + (stepX / 2)
                                val barHeight = (value / maxValue) * chartHeight
                                val barTop = chartHeight - barHeight
                                
                                val topLeft = Offset(xCenter - barWidth / 2, barTop)
                                val size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                                
                                // Draw Bar
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(primaryColor, secondaryColor)
                                    ),
                                    topLeft = topLeft,
                                    size = size,
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                                )
                                
                                // Draw Glow (Keep for dark mode mainly, maybe reduce for light)
                                if (isDark) {
                                    drawRoundRect(
                                        color = primaryColor.copy(alpha = 0.3f),
                                        topLeft = topLeft.copy(x = topLeft.x - 2f),
                                        size = size.copy(width = size.width + 4f),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
                                        style = Stroke(width = 2f) 
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // X-Axis Labels linked to actual data
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).padding(start = 48.dp), // Check padding to align with Y-axis offset
                horizontalArrangement = Arrangement.SpaceBetween // Or custom placement
            ) {
                 if (trends.isNotEmpty()) {
                     // Show max 6 labels to avoid crowding
                     val labelStep = (trends.size / 6).coerceAtLeast(1)
                     trends.forEachIndexed { index, pair ->
                         if (index % labelStep == 0) {
                             Text(
                                 text = pair.first.take(3), // First 3 chars
                                 color = labelTextColor, 
                                 fontSize = 13.sp, 
                                 fontWeight = FontWeight.Medium,
                                 textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                 modifier = Modifier.width(30.dp) // Fixed width for alignment
                             )
                         }
                     }
                 }
            }
        }
    }
}

enum class ChartType {
    LINE, BAR
}

@Composable
fun CategoryPieChart(spendingByCategory: Map<BillCategory, BigDecimal>, totalSpending: BigDecimal) {
    // Use LocalPlatisaColors.isDark for consistent in-app theme state
    val isDark = LocalPlatisaColors.current.isDark

    val mainTextColor = if (isDark) Color.White else LightStatsTextMain
    val labelTextColor = if (isDark) Color.Gray else LightStatsTextMuted
    


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .glassPanel(cornerRadius = 24.dp)
            .padding(GlobalCardPadding)
    ) {
        Column {
             Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Ovaj Mesec",
                    color = mainTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Box(
                    modifier = Modifier
                        .background(if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Kategorije", color = labelTextColor, fontSize = 12.sp)
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                
                // Donut Chart
                Box(contentAlignment = Alignment.Center) {
                    val density = LocalDensity.current
                    val sizePx = with(density) { 130.dp.toPx() }
                    val canvasSize = androidx.compose.ui.geometry.Size(sizePx, sizePx) // Renamed to avoid capture confusion
                            
                    // Remember heavy objects
                    // val nativePaint = remember { android.graphics.Paint() }
                    // val blurMask = remember { android.graphics.BlurMaskFilter(20f, android.graphics.BlurMaskFilter.Blur.NORMAL) }
                    
                    // Memoize the glass effect brush
                    val glassBrush = remember(canvasSize) {
                        Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent),
                            center = Offset(canvasSize.width / 2, canvasSize.height / 2),
                            radius = canvasSize.minDimension / 2
                        )
                    }

                    Canvas(modifier = Modifier.size(130.dp)) {
                        val total = totalSpending.toFloat()
                        var startAngle = -90f
                        val strokeWidth = 35f
                        
                        // Background Track
                        drawCircle(
                            color = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f),
                            style = Stroke(width = strokeWidth)
                        )
                        
                        if (total > 0f) {
                            spendingByCategory.entries.forEach { (category, amount) ->
                                val sweep = (amount.toFloat() / total) * 360f
                                val color = category.color
                                
                                // 1. Neon Glow (Dark Mode only) - Disabled temporarily due to build issues
                                /*
                                if (isDark) {
                                    drawIntoCanvas {
                                        nativePaint.color = color.toArgb()
                                        nativePaint.maskFilter = blurMask
                                        nativePaint.style = android.graphics.Paint.Style.STROKE
                                        nativePaint.strokeWidth = strokeWidth
                                        nativePaint.isAntiAlias = true
                                        
                                        val rectF = android.graphics.RectF(0f, 0f, size.width, size.height)
                                        it.nativeCanvas.drawArc(
                                            rectF,
                                            startAngle,
                                            sweep,
                                            false,
                                            nativePaint
                                        )
                                    }
                                }
                                */

                                // 2. Main Arc
                                // We use flat color here as creating a unique sweepGradient per segment is heavy and visually similar to just the color
                                // If gradient is strictly required, simple vertical gradient is cheaper, but for now reverting to color 
                                // with the glass overlay provides the 3D effect efficiently.
                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                                
                                // 3. Glass/Glossy Highlight (Top reflection)
                                drawArc(
                                    brush = glassBrush,
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth * 0.3f, cap = StrokeCap.Butt)
                                )
                                
                                startAngle += sweep
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Ukupno", color = labelTextColor, fontSize = 14.sp)
                        Text(
                            // Format 100000 -> 100k
                            if (totalSpending >= BigDecimal(1000)) "${(totalSpending.toFloat()/1000).toInt()}k" else "${totalSpending.toInt()}", 
                            color = mainTextColor, 
                            fontSize = 24.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Legend
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                     val categories = spendingByCategory.entries.sortedByDescending { it.value }.map { it.key }.take(4).ifEmpty { listOf(BillCategory.ELECTRICITY, BillCategory.OTHER, BillCategory.TELECOM, BillCategory.WATER) }
                     
                     categories.forEach { cat ->
                          val color = cat.color

                          Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                              Row(verticalAlignment = Alignment.CenterVertically) {
                                  Box(modifier = Modifier.size(8.dp).background(color, CircleShape).then(
                                      if(!isDark && cat == BillCategory.ELECTRICITY) Modifier.shadow(5.dp, CircleShape, spotColor = color) else Modifier
                                  ))
                                  Spacer(modifier = Modifier.width(8.dp))
                                  Text(
                                    text = cat.displayName,
                                    color = if (isDark) Color(0xFFCCCCCC) else LightStatsTextMuted, fontSize = 16.sp
                                  )
                              }
                              Text(
                                  "${ if (totalSpending > BigDecimal.ZERO) ((spendingByCategory[cat] ?: BigDecimal.ZERO).toFloat() / totalSpending.toFloat() * 100).toInt() else 0 }%", 
                                  color = mainTextColor, 
                                  fontWeight = FontWeight.Bold,
                                  fontSize = 16.sp
                              )
                          }
                     }
                }
            }
        }
    }
}

@Composable
fun CategoryDetailsSection(categoryDetails: List<CategoryDetailedStats>) {
    // Use LocalPlatisaColors.isDark for consistent in-app theme state
    val isDark = LocalPlatisaColors.current.isDark
    val mainTextColor = if (isDark) Color.White else LightStatsTextMain

    Column {
        Text(
            "Detalji po Kategorijama",
            color = mainTextColor,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (categoryDetails.isEmpty()) {
            Text("Nema podataka", color = if (isDark) Color.Gray else LightStatsTextMuted)
        } else {
            categoryDetails.forEach { stats ->
                CategoryDetailCard(stats, isDark, mainTextColor)
            }
        }
    }
}

@Composable
fun CategoryDetailCard(stats: CategoryDetailedStats, isDark: Boolean, mainTextColor: Color) {
    var isExpanded by remember { mutableStateOf(false) }
    val borderColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color.Black.copy(alpha = 0.3f) else Color.White)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { isExpanded = !isExpanded }
            .then(if (!isDark) Modifier.shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha=0.05f)) else Modifier)
    ) {
         // Inner Glow
        if (isDark) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                stats.category.color.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(stats.category.color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .border(1.dp, stats.category.color.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = stats.category.icon,
                        contentDescription = null,
                        tint = stats.category.color,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stats.category.displayName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = mainTextColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                     Text(
                        text = "${stats.count} Transakcija", // "Trips" -> Transakcija/Poseta
                        fontSize = 13.sp,
                        color = if (isDark) Color.Gray else LightStatsTextMuted
                    )
                }
                
                // Total & Arrow
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.formatCurrencySimple(stats.totalAmount),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = mainTextColor,
                         fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = if (isDark) Color.Gray else LightStatsTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Expanded Content
            androidx.compose.animation.AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider(color = borderColor, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (stats.merchantBreakdown.isEmpty()) {
                         Text("Nema detalja o prodavnicama", fontSize = 13.sp, color = if (isDark) Color.Gray else LightStatsTextMuted)
                    } else {
                        stats.merchantBreakdown.forEach { (merchant, amount) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = merchant.ifBlank { "Nepoznato" },
                                    fontSize = 14.sp,
                                    color = if (isDark) Color.White.copy(alpha = 0.8f) else LightStatsTextMain,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )
                                Text(
                                    text = Formatters.formatCurrencySimple(amount),
                                    fontSize = 14.sp,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroceryAnalyticsSection(
    stats: GroceryStats,
    selectedPeriod: GraphPeriod,
    onPeriodChange: (GraphPeriod) -> Unit
) {
    // Use LocalPlatisaColors.isDark for consistent in-app theme state
    val isDark = LocalPlatisaColors.current.isDark

    val primaryTextColor = if (isDark) Color.White else SolarTextPrimary
    val secondaryTextColor = if (isDark) Color.White.copy(alpha = 0.7f) else SolarTextSecondary
    val cardBackgroundColor = if (isDark) Color.Black.copy(alpha = 0.3f) else androidx.compose.ui.graphics.Color.White
    val cardBorderColor = if (isDark) Color.White.copy(alpha = 0.1f) else SolarSurfaceVariant

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50), // Grocery Green
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Supermarket",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor
                )
            }
            
            // Dropdown Selector
            PeriodDropdownSelector(
                selectedPeriod = selectedPeriod,
                onPeriodChange = onPeriodChange
            )
        }

        // Main Stats Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBackgroundColor)
                .border(
                    if(isDark) 1.dp else 2.dp, 
                    if(isDark) DarkThemeStatsBorder else androidx.compose.ui.graphics.Color(0xFF795548), 
                    RoundedCornerShape(16.dp)
                )
        ) {
             // Inner Glow
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF4CAF50).copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            Column(
                modifier = Modifier.padding(GlobalCardPadding),
                verticalArrangement = Arrangement.spacedBy(GlobalCardPadding)
            ) {
                // Total Amount Logic
                Column {
                    Text(
                        "Ukupno",
                        fontSize = 18.sp,
                        color = primaryTextColor // Dark Text
                    )
                    Text(
                        Formatters.formatCurrency(stats.total),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryTextColor,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
                
                // Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Visits Count
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(cardBorderColor, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                "Poseta",
                                fontSize = 14.sp,
                                color = primaryTextColor.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${stats.count}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                    
                    // Avg Basket
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(cardBorderColor, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                "Ukupno",
                                fontSize = 14.sp,
                                color = primaryTextColor.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                Formatters.formatCurrency(stats.total).replace("RSD", "").trim(), // Shorten
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color(0xFF006400) // Dark Green
                            )
                        }
                    }
                }
                
                // Top Merchants List
                if (stats.topMerchants.isNotEmpty()) {
                    Divider(color = Color.Black.copy(alpha = 0.1f))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Najviše trošeno u:",
                            fontSize = 12.sp,
                            color = primaryTextColor.copy(alpha = 0.8f),
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        stats.topMerchants.forEachIndexed { index, pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                ) {
                                    Text(
                                        "${index + 1}.",
                                        color = androidx.compose.ui.graphics.Color(0xFF006400),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Text(
                                        text = pair.first,
                                        color = primaryTextColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = Formatters.formatCurrency(pair.second),
                                    color = androidx.compose.ui.graphics.Color(0xFF006400), // Dark Green
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PharmacyAnalyticsSection(
    stats: PharmacyStats,
    selectedPeriod: GraphPeriod,
    onPeriodChange: (GraphPeriod) -> Unit
) {
    // Use LocalPlatisaColors.isDark for consistent in-app theme state
    val isDark = LocalPlatisaColors.current.isDark

    val primaryTextColor = if (isDark) Color.White else SolarTextPrimary
    val secondaryTextColor = if (isDark) Color.White.copy(alpha = 0.7f) else SolarTextSecondary
    val cardBackgroundColor = if (isDark) Color.Black.copy(alpha = 0.3f) else androidx.compose.ui.graphics.Color.White
    val cardBorderColor = if (isDark) Color.White.copy(alpha = 0.1f) else SolarSurfaceVariant
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.LocalHospital,
                    contentDescription = null,
                    tint = HtmlNeonPink, // Pharmacy Pink
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Apoteka",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor
                )
            }
            
            // Dropdown Selector
            PeriodDropdownSelector(
                selectedPeriod = selectedPeriod,
                onPeriodChange = onPeriodChange
            )
        }

        // Main Stats Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBackgroundColor)
                .border(
                    if(isDark) 1.dp else 2.dp, 
                    if(isDark) DarkThemeStatsBorder else androidx.compose.ui.graphics.Color(0xFF795548), 
                    RoundedCornerShape(16.dp)
                )
        ) {
             // Inner Glow
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                HtmlNeonPink.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            Column(
                modifier = Modifier.padding(GlobalCardPadding),
                verticalArrangement = Arrangement.spacedBy(GlobalCardPadding)
            ) {
                // Total Amount Logic
                Column {
                    Text(
                        "Ukupno",
                        fontSize = 16.sp,
                        color = primaryTextColor // Dark Text
                    )
                        com.platisa.app.ui.components.DynamicSizeText(
                            text = Formatters.formatCurrency(stats.total),
                            minFontSize = 18.sp,
                            maxFontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start
                        )
                }
                
                // Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Visits Count
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(cardBorderColor, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                "Poseta",
                                fontSize = 14.sp,
                                color = primaryTextColor.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${stats.count}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = HtmlNeonPink
                            )
                        }
                    }
                    
                    // Avg Basket
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(cardBorderColor, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                "Ukupno",
                                fontSize = 14.sp,
                                color = primaryTextColor.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            com.platisa.app.ui.components.DynamicSizeText(
                                text = Formatters.formatCurrency(stats.total).replace("RSD", "").trim(),
                                minFontSize = 12.sp,
                                maxFontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = HtmlNeonPink,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Start
                            )
                        }
                    }
                }
                
                // Top Merchants List
                if (stats.topMerchants.isNotEmpty()) {
                    Divider(color = Color.Black.copy(alpha = 0.1f))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Najviše trošeno u:",
                            fontSize = 12.sp,
                            color = primaryTextColor.copy(alpha = 0.8f),
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        stats.topMerchants.forEachIndexed { index, pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                ) {
                                    Text(
                                        "${index + 1}.",
                                        color = HtmlNeonPink,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Text(
                                        text = pair.first,
                                        color = primaryTextColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = Formatters.formatCurrency(pair.second),
                                    color = HtmlNeonPink, // Neon Pink
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RestaurantAnalyticsSection(
    stats: RestaurantStats,
    selectedPeriod: GraphPeriod,
    onPeriodChange: (GraphPeriod) -> Unit
) {
    val HtmlOrange = Color(0xFFFF9800)
    
    // Use LocalPlatisaColors.isDark for consistent in-app theme state
    val isDark = LocalPlatisaColors.current.isDark

    val primaryTextColor = if (isDark) Color.White else SolarTextPrimary
    val secondaryTextColor = if (isDark) Color.White.copy(alpha = 0.7f) else SolarTextSecondary
    val cardBackgroundColor = if (isDark) Color.Black.copy(alpha = 0.3f) else androidx.compose.ui.graphics.Color.White
    val cardBorderColor = if (isDark) Color.White.copy(alpha = 0.1f) else SolarSurfaceVariant
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Wrapper for Title + Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Restorani",
                style = MaterialTheme.typography.titleLarge,
                color = primaryTextColor,
                fontWeight = FontWeight.Bold
            )
            
            PeriodDropdownSelector(
                selectedPeriod = selectedPeriod,
                onPeriodChange = onPeriodChange
            )
        }
        
        // Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBackgroundColor)
                .border(
                    if(isDark) 1.dp else 2.dp, 
                    if(isDark) DarkThemeStatsBorder else androidx.compose.ui.graphics.Color(0xFF795548), 
                    RoundedCornerShape(16.dp)
                )
        ) {
             // Inner Glow
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                HtmlOrange.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            Column(
                modifier = Modifier.padding(GlobalCardPadding),
                verticalArrangement = Arrangement.spacedBy(GlobalCardPadding)
            ) {
                // Total Amount Logic
                Column {
                    Text(
                        "Ukupno na restorane i hranu",
                        fontSize = 12.sp,
                        color = primaryTextColor // Dark Text
                    )
                        com.platisa.app.ui.components.DynamicSizeText(
                            text = Formatters.formatCurrency(stats.total),
                            minFontSize = 16.sp,
                            maxFontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start
                        )
                }
                
                // Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Visits Count
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(cardBorderColor, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                "Poseta",
                                fontSize = 11.sp,
                                color = primaryTextColor.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${stats.count}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = HtmlOrange
                            )
                        }
                    }
                    
                    // Avg Basket
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(cardBorderColor, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                "Prosek",
                                fontSize = 11.sp,
                                color = primaryTextColor.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            com.platisa.app.ui.components.DynamicSizeText(
                                text = Formatters.formatCurrency(stats.average).replace("RSD", "").trim(),
                                minFontSize = 12.sp,
                                maxFontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = HtmlOrange,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Start
                            )
                        }
                    }
                }
                
                // Top Merchants List
                if (stats.topMerchants.isNotEmpty()) {
                    Divider(color = Color.Black.copy(alpha = 0.1f))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Najviše posećeno:",
                            fontSize = 12.sp,
                            color = primaryTextColor.copy(alpha = 0.8f),
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        stats.topMerchants.forEachIndexed { index, pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                ) {
                                    Text(
                                        "${index + 1}.",
                                        color = HtmlOrange,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Text(
                                        text = pair.first,
                                        color = primaryTextColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = Formatters.formatCurrency(pair.second),
                                    color = HtmlOrange, 
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// Extension for Glassmorphism
fun Modifier.glassPanel(
    cornerRadius: androidx.compose.ui.unit.Dp = 16.dp,
    containerColor: Color? = null,
    borderColor: Color? = null
): Modifier = composed {
    // Use LocalPlatisaColors.isDark for consistent in-app theme state
    val isDark = LocalPlatisaColors.current.isDark

    val bg = containerColor ?: if (isDark) CyberCardBg else androidx.compose.ui.graphics.Color.White
    val border = borderColor ?: if (isDark) DarkThemeStatsBorder else LightStatsBorder
    
    this
        .clip(RoundedCornerShape(cornerRadius))
        .background(bg)
        .border(
            if (isDark) 1.dp else 2.dp, 
            if (isDark) border else androidx.compose.ui.graphics.Color(0xFF795548), 
            RoundedCornerShape(cornerRadius)
        )
        .then(if (!isDark) Modifier else Modifier) // Removed shadow in Light Theme to ensure pure White background
}

@Composable
fun PeriodDropdownSelector(
    selectedPeriod: GraphPeriod,
    onPeriodChange: (GraphPeriod) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = when(selectedPeriod) {
                    GraphPeriod.MONTHLY -> "Mesečno"
                    GraphPeriod.SIX_MONTHS -> "6 Meseci"
                    GraphPeriod.THIS_YEAR -> "Ova Godina"
                },
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(CyberBackgroundDark)
        ) {
            DropdownMenuItem(
                text = { Text("Mesečno", color = Color.White) },
                onClick = { 
                    onPeriodChange(GraphPeriod.MONTHLY)
                    expanded = false 
                }
            )
            DropdownMenuItem(
                text = { Text("6 Meseci", color = Color.White) },
                onClick = { 
                    onPeriodChange(GraphPeriod.SIX_MONTHS)
                    expanded = false 
                }
            )
            DropdownMenuItem(
                text = { Text("Ova Godina", color = Color.White) },
                onClick = { 
                    onPeriodChange(GraphPeriod.THIS_YEAR)
                    expanded = false 
                }
            )
        }
    }
}


