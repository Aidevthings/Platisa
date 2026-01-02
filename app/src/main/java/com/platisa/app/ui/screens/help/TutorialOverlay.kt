package com.platisa.app.ui.screens.help

import androidx.compose.animation.core.*
import com.platisa.app.ui.screens.home.Glass3DIcon
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex

// Colors matching HomeScreen
private val HtmlBackgroundDark = Color(0xFF111217)
private val HtmlNeonCyan = Color(0xFF00EAFF)
private val HtmlNeonMagenta = Color(0xFFFF00D9)
private val HtmlNeonGreen = Color(0xFF39FF14)
private val HtmlNeonYellow = Color(0xFFF7FF00)
private val HtmlLightCyan = Color(0xFF00D4DD) // Match production
private val HtmlNeonWhite = Color(0xFFFFFFFF) // Bright white for nav bar frames
private val HtmlGray400 = Color(0xFF9CA3AF)
private val HtmlGray300 = Color(0xFFD1D5DB) // Added for production match

data class TutorialStep(
    val title: String,
    val description: String,
    val targetId: String,
    val icon: ImageVector,
    val iconColor: Color,
    val showArrow: Boolean = false,
    val arrowDirection: ArrowDirection = ArrowDirection.LEFT,
    val showMultipleArrows: Boolean = false,
    val showMockBillCard: Boolean = false,
    val mockBillStatus: PaymentStatusDemo? = null,
    val showIcon: Boolean = true
)

enum class PaymentStatusDemo {
    UNPAID,    // Cijan - neskenirani
    PROCESSING, // Magenta - spreman za plaćanje
    PAID       // Zeleno - plaćen
}

enum class ArrowDirection {
    LEFT, RIGHT, UP, DOWN
}

// Global map za čuvanje pozicija komponenata
object SpotlightRegistry {
    private val positions = mutableMapOf<String, Rect>()
    
    fun register(id: String, rect: Rect) {
        positions[id] = rect
    }
    
    fun getPosition(id: String): Rect? = positions[id]
    
    fun clear() {
        positions.clear()
    }
}

@Composable
fun MockBillCard(
    status: PaymentStatusDemo
) {
    val isDark = isSystemInDarkTheme()
    
    // Unified colors for both Light and Dark mode to match real app behavior
    val (bgColor, borderColor, iconBgColor) = when (status) {
        PaymentStatusDemo.UNPAID -> Triple(
            HtmlLightCyan.copy(alpha = if (isDark) 0.25f else 0.15f),
            HtmlLightCyan.copy(alpha = 0.8f),
            HtmlLightCyan.copy(alpha = 0.6f)
        )
        PaymentStatusDemo.PROCESSING -> Triple(
            if (isDark) HtmlNeonMagenta.copy(alpha = 0.25f) else CalmMagenta.copy(alpha = 0.15f),
            if (isDark) HtmlNeonMagenta.copy(alpha = 0.8f) else CalmMagenta.copy(alpha = 0.8f),
            if (isDark) HtmlNeonMagenta.copy(alpha = 0.6f) else CalmMagenta.copy(alpha = 0.6f)
        )
        PaymentStatusDemo.PAID -> Triple(
            HtmlNeonGreen.copy(alpha = if (isDark) 0.25f else 0.15f),
            HtmlNeonGreen.copy(alpha = 0.8f),
            HtmlNeonGreen.copy(alpha = 0.6f)
        )
    }

    val mainTextColor = if (isDark) Color.White else Color.Black
    val subTextColor = if (isDark) HtmlGray300 else Color.DarkGray
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Glass3DIcon(
                    icon = Icons.Default.Lightbulb,
                    backgroundColor = iconBgColor,
                    size = 56.dp
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    com.platisa.app.ui.components.DynamicSizeText(
                        text = "EPS Distribucija",
                        modifier = Modifier
                            .fillMaxWidth()
                            .tutorialTarget("bill_merchant_name"),
                        color = mainTextColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Default,
                        minFontSize = 14.sp,
                        maxFontSize = 18.sp,
                        maxLines = 1
                    )
                    // Datum računa
                    if (status == PaymentStatusDemo.PAID) {
                        Text(
                            text = "Plaćeno",
                            fontSize = 13.sp,
                            color = subTextColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "14 Nov 2025",
                            fontSize = 13.sp,
                            color = subTextColor,
                            modifier = Modifier.tutorialTarget("bill_date")
                        )
                    } else {
                        Text(
                            text = "14 Nov 2025",
                            fontSize = 13.sp,
                            color = subTextColor,
                            modifier = Modifier.tutorialTarget("bill_date")
                        )
                    }
                    // Rok plaćanja
                    if (status != PaymentStatusDemo.PAID) {
                        Text(
                            text = "15 Oct 2025",
                            fontSize = 13.sp,
                            color = HtmlNeonCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.tutorialTarget("bill_due_date")
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    com.platisa.app.ui.components.DynamicSizeText(
                        text = "3.299,00",
                        modifier = Modifier
                            .widthIn(max = 150.dp)
                            .tutorialTarget("bill_amount"),
                        color = mainTextColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        minFontSize = 14.sp,
                        maxFontSize = 24.sp,
                        maxLines = 1
                    )
                    // Potrošnja (kWh)
                    Text(
                        text = "142 kWh",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFFFFD700) else Color(0xFFF57F17), // Gold vs Darker Gold/Orange
                        modifier = Modifier.tutorialTarget("bill_consumption")
                    )
                }
            }
            
            // Confirm Payment button for PROCESSING status
            if (status == PaymentStatusDemo.PROCESSING) {
                if (isDark) {
                    Button(
                        onClick = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 0.dp) // Updated padding
                            .padding(bottom = 12.dp)
                            .height(56.dp), // Height 56dp
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HtmlNeonGreen.copy(alpha = 0.2f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HtmlNeonGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = HtmlNeonGreen,
                            modifier = Modifier.size(24.dp) // Icon 24dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "POTVRDI PLAĆANJE",
                            color = HtmlNeonGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp, // Font 18sp
                            letterSpacing = 1.sp
                        )
                    }
                } else {
                    // LIGHT MODE Button
                    Button(
                        onClick = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 0.dp)
                            .padding(bottom = 12.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f) // Light Green tint
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF388E3C), // Darker Green
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "POTVRDI PLAĆANJE",
                            color = Color(0xFF388E3C), // Darker Green
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MultipleArrowsForBillCard(
    modifier: Modifier = Modifier
) {
    // Definicija podataka za strelice (targetId, direction, label, color)
    val arrowData = listOf(
        ArrowData("bill_merchant_name", ArrowDirection.LEFT, "Ime dobavljača", Color(0xFFFF00D9)),
        ArrowData("bill_date", ArrowDirection.LEFT, "Datum", Color(0xFFFF00D9)),
        ArrowData("bill_due_date", ArrowDirection.LEFT, "Rok", Color(0xFFFF00D9)),
        ArrowData("bill_amount", ArrowDirection.RIGHT, "Iznos", Color(0xFFFFD700)),
        ArrowData("bill_consumption", ArrowDirection.RIGHT, "kWh", Color(0xFFFFD700))
    )
    
    // Animacija pulsiranja
    val infiniteTransition = rememberInfiniteTransition(label = "multipleArrows")
    val arrowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrowAlpha"
    )
    
    Canvas(modifier = modifier) {
        val arrowLength = 80f
        val arrowHeadSize = 20f
        
        arrowData.forEach { data ->
            val targetRect = SpotlightRegistry.getPosition(data.targetId) ?: return@forEach
            
            // Odredi početnu i krajnju tačku strelice
            val (startX, startY, endX, endY, labelX, labelY) = when(data.direction) {
                ArrowDirection.LEFT -> {
                    val sx = targetRect.left - 20f
                    val sy = targetRect.top + targetRect.height / 2
                    val ex = sx - arrowLength
                    val ey = sy
                    val lx = ex - 10f
                    val ly = ey
                    Tuple6(sx, sy, ex, ey, lx, ly)
                }
                ArrowDirection.RIGHT -> {
                    val sx = targetRect.right + 20f
                    val sy = targetRect.top + targetRect.height / 2
                    val ex = sx + arrowLength
                    val ey = sy
                    val lx = ex + 10f
                    val ly = ey
                    Tuple6(sx, sy, ex, ey, lx, ly)
                }
                else -> Tuple6(0f, 0f, 0f, 0f, 0f, 0f)
            }
            
            // Nacrtaj liniju
            drawLine(
                color = data.color.copy(alpha = arrowAlpha),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 3f
            )
            
            // Nacrtaj glavu strelice
            val arrowPath = Path().apply {
                when(data.direction) {
                    ArrowDirection.LEFT -> {
                        moveTo(endX, endY)
                        lineTo(endX + arrowHeadSize, endY - arrowHeadSize / 2)
                        lineTo(endX + arrowHeadSize, endY + arrowHeadSize / 2)
                    }
                    ArrowDirection.RIGHT -> {
                        moveTo(endX, endY)
                        lineTo(endX - arrowHeadSize, endY - arrowHeadSize / 2)
                        lineTo(endX - arrowHeadSize, endY + arrowHeadSize / 2)
                    }
                    else -> {}
                }
                close()
            }
            
            drawPath(
                path = arrowPath,
                color = data.color.copy(alpha = arrowAlpha),
                style = Fill
            )
            
            // Nacrtaj labelu
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(
                    (arrowAlpha * 255).toInt(),
                    (data.color.red * 255).toInt(),
                    (data.color.green * 255).toInt(),
                    (data.color.blue * 255).toInt()
                )
                textSize = 32f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                isAntiAlias = true
                textAlign = when(data.direction) {
                    ArrowDirection.LEFT -> android.graphics.Paint.Align.RIGHT
                    ArrowDirection.RIGHT -> android.graphics.Paint.Align.LEFT
                    else -> android.graphics.Paint.Align.CENTER
                }
            }
            drawContext.canvas.nativeCanvas.drawText(data.label, labelX, labelY, textPaint)
        }
    }
}

data class ArrowData(
    val targetId: String,
    val direction: ArrowDirection,
    val label: String,
    val color: Color
)

data class Tuple6(val a: Float, val b: Float, val c: Float, val d: Float, val e: Float, val f: Float)

@Composable
fun ArrowPointer(
    targetRect: Rect,
    direction: ArrowDirection,
    color: Color,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    // Animacija pulsiranja
    val infiniteTransition = rememberInfiniteTransition(label = "arrow")
    val arrowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrowAlpha"
    )
    
    Canvas(modifier = modifier) {
        val arrowLength = 80f
        val arrowHeadSize = 24f
        
        // Odredi početnu i krajnju tačku strelice
        val (startX, startY, endX, endY) = when(direction) {
            ArrowDirection.LEFT -> {
                // Strelica sa desne strane ka targetRect-u
                val startX = targetRect.left - 40f
                val startY = targetRect.top + targetRect.height / 2
                val endX = startX - arrowLength
                val endY = startY
                Tuple4(startX, startY, endX, endY)
            }
            ArrowDirection.RIGHT -> {
                // Strelica sa leve strane ka targetRect-u
                val startX = targetRect.right + 40f
                val startY = targetRect.top + targetRect.height / 2
                val endX = startX + arrowLength
                val endY = startY
                Tuple4(startX, startY, endX, endY)
            }
            ArrowDirection.UP -> {
                val startX = targetRect.left + targetRect.width / 2
                val startY = targetRect.top - 40f
                val endX = startX
                val endY = startY - arrowLength
                Tuple4(startX, startY, endX, endY)
            }
            ArrowDirection.DOWN -> {
                val startX = targetRect.left + targetRect.width / 2
                val startY = targetRect.bottom + 40f
                val endX = startX
                val endY = startY + arrowLength
                Tuple4(startX, startY, endX, endY)
            }
        }
        
        // Nacrtaj liniju
        drawLine(
            color = color.copy(alpha = arrowAlpha),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 4f
        )
        
        // Nacrtaj strelicu (trougao)
        val arrowPath = Path().apply {
            when(direction) {
                ArrowDirection.LEFT -> {
                    moveTo(endX, endY)
                    lineTo(endX + arrowHeadSize, endY - arrowHeadSize / 2)
                    lineTo(endX + arrowHeadSize, endY + arrowHeadSize / 2)
                }
                ArrowDirection.RIGHT -> {
                    moveTo(endX, endY)
                    lineTo(endX - arrowHeadSize, endY - arrowHeadSize / 2)
                    lineTo(endX - arrowHeadSize, endY + arrowHeadSize / 2)
                }
                ArrowDirection.UP -> {
                    moveTo(endX, endY)
                    lineTo(endX - arrowHeadSize / 2, endY + arrowHeadSize)
                    lineTo(endX + arrowHeadSize / 2, endY + arrowHeadSize)
                }
                ArrowDirection.DOWN -> {
                    moveTo(endX, endY)
                    lineTo(endX - arrowHeadSize / 2, endY - arrowHeadSize)
                    lineTo(endX + arrowHeadSize / 2, endY - arrowHeadSize)
                }
            }
            close()
        }
        
        drawPath(
            path = arrowPath,
            color = color.copy(alpha = arrowAlpha),
            style = Fill
        )
    }
}

data class Tuple4(val first: Float, val second: Float, val third: Float, val fourth: Float)

// Calm Colors for Light Theme
private val CalmBlue = Color(0xFF1976D2)   // Standard Blue
private val CalmMagenta = Color(0xFFC2185B) // Pink/Rose
private val CalmGreen = Color(0xFF388E3C)  // Carmal Green
private val DarkPurpleBackground = Color(0xFF2E003E) // Deep Purple for Theme Card
private val ElectricPurple = Color(0xFFD500F9)     // Bright Purple for Dark Mode

@Composable
fun TutorialOverlay(
    onDismiss: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    val isDark = isSystemInDarkTheme()
    
    val overlayColor = if (isDark) Color.Black.copy(alpha = 0.95f) else Color(0xFFEFEBE9).copy(alpha = 0.95f)
    
    val steps = remember(isDark) {
        val blue = if (isDark) HtmlNeonCyan else CalmBlue
        val magenta = if (isDark) HtmlNeonMagenta else CalmMagenta
        val green = if (isDark) HtmlNeonGreen else CalmGreen
        // Step 10: Purple for both Light and Dark themes (User request)
        // ElectricPurple for Dark mode (visibility), DarkPurpleBackground for Light mode
        val themeIconColor = if (isDark) ElectricPurple else DarkPurpleBackground
        
        listOf(
            TutorialStep(
                title = "Dobrodošli u Platiša!",
                description = "Hajde da prođemo kroz osnovne funkcije aplikacije. Kliknite 'Sledeće' da nastavite.",
                targetId = "avatar_greeting",
                icon = Icons.Default.Lightbulb,
                iconColor = blue
            ),
            TutorialStep(
                title = "Vaš Profil",
                description = "Kliknite na avatar ili 'Zdravo, Platiša!' da prilagodite profil, promenite ime, avatar i poruke obeležavanja.",
                targetId = "avatar_greeting",
                icon = Icons.Default.Person,
                iconColor = blue
            ),
            TutorialStep(
                title = "Skenirajte Račun",
                description = "Kliknite ovde da otvorite kameru i skenirate novi račun. Aplikacija će automatski izvući sve podatke.",
                targetId = "camera_button",
                icon = Icons.Default.CameraAlt,
                iconColor = blue
            ),
            TutorialStep(
                title = "Kartica Računa",
                description = "Ovo je vaša kartica računa. Hajde da vidimo šta sve prikazuje.",
                targetId = "bill_card",
                icon = Icons.Default.Description,
                iconColor = blue,
                showMockBillCard = true,
                mockBillStatus = PaymentStatusDemo.UNPAID
            ),
            TutorialStep(
                title = "Detalji Računa",
                description = "Ime dobavljača | Datum izdavanja | Rok plaćanja | Iznos za plaćanje | Potrošnja (za struju)",
                targetId = "bill_card_details",
                icon = Icons.Default.Info,
                iconColor = blue,
                showMultipleArrows = true,
                showMockBillCard = true,
                mockBillStatus = PaymentStatusDemo.UNPAID,
                showIcon = false
            ),
            TutorialStep(
                title = "Pregled Računa (Cijan)",
                description = "Cijan boja označava NESKENIRANI račun. Kliknite na račun da vidite detalje i sačuvate QR kod za plaćanje.",
                targetId = "bill_card",
                icon = Icons.Default.QrCode,
                iconColor = blue, // Was NeonCyan
                showMockBillCard = true,
                mockBillStatus = PaymentStatusDemo.UNPAID
            ),
            TutorialStep(
                title = "Spreman za Plaćanje (Magenta)",
                description = "Kada račun postane LJUBIČAST, to znači da ste sačuvali QR kod. Sada idite u vašu bankarsku aplikaciju, platite račun, pa se vratite ovde i kliknite zeleno dugme 'POTVRDI'.",
                targetId = "bill_card",
                icon = Icons.Default.Payment,
                iconColor = magenta,
                showMockBillCard = true,
                mockBillStatus = PaymentStatusDemo.PROCESSING,
                showIcon = false
            ),
            TutorialStep(
                title = "Plaćeno (Zeleno)",
                description = "Nakon što potvrdite plaćanje, račun postaje ZELEN - to znači da je USPJEŠNO PLAĆEN! Račun se automatski arhivira.",
                targetId = "bill_card",
                icon = Icons.Default.CheckCircle,
                iconColor = green,
                showMockBillCard = true,
                mockBillStatus = PaymentStatusDemo.PAID
            ),
            TutorialStep(
                title = "Notifikacije",
                description = "Podesite podsetnike za rokove plaćanja. Aplikacija će vas obavestiti kada se približi rok.",
                targetId = "notification_bell",
                icon = Icons.Default.Notifications,
                iconColor = blue
            ),
            TutorialStep(
                title = "Svetla/Tamna Tema",
                description = "Promenite izgled aplikacije između svetlog i tamnog režima prema vašim preferencijama.",
                targetId = "theme_toggle",
                icon = Icons.Default.LightMode,
                iconColor = themeIconColor
            ),
            TutorialStep(
                title = "Market - Uporedi Cene",
                description = "Ovde možete pretraživati proizvode iz vaših fiskalnih računa i uporediti cene u raznim prodavnicama. Pronađite gde je najjeftinije!",
                targetId = "pijaca_nav",
                icon = Icons.Default.CompareArrows,
                iconColor = green
            ),
            TutorialStep(
                title = "Podaci",
                description = "Pratite svoju potrošnju kroz detaljne analize po kategorijama, periodima i trendovima. Saznajte gde najviše trošite!",
                targetId = "statistics_nav",
                icon = Icons.Default.Analytics,
                iconColor = magenta
            ),
            TutorialStep(
                title = "Važan Savet",
                description = "Platiša radi isključivo putem Gmail-a. Savetujemo da sve vaše e-račune (EPS, Infostan, Telekom...) preusmerite na jedan Gmail nalog kako bi ih aplikacija sve pronašla na jednom mestu.",
                targetId = "gmail_tip", // Specific ID to center card w/o spotlight
                icon = Icons.Default.Email,
                iconColor = blue
            ),
            TutorialStep(
                title = "Pomoć Uvek Dostupna",
                description = "Kliknite na ovu ikonicu bilo kada da ponovo vidite ovaj tutorial.",
                targetId = "help_icon",
                icon = Icons.Default.Help,
                iconColor = green
            )
        )
    }
    
    val currentTargetRect = SpotlightRegistry.getPosition(steps[currentStep].targetId)
    
    com.platisa.app.ui.theme.PlatisaTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            val showMockBill = steps[currentStep].showMockBillCard
            
            // Prikaži spotlight SAMO ako NIJE prvi step (Introduction) I NIJE Mock Bill step
            if (currentStep > 0 && !showMockBill) {
                if (currentTargetRect != null) {
                    SpotlightCutout(
                        targetRect = currentTargetRect,
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(1000f)
                    )
                    
                    // Use icon color for the spotlight border
                    val borderColor = steps[currentStep].iconColor
                    
                    // Skip NeonBorder (glow) for Avatar (step 1) and Camera (step 2) cards
                    // AND disable completely in Light Theme per request
                    val skipGlow = !isDark || currentStep in listOf(1, 2)
                    if (!skipGlow) {
                        NeonBorder(
                            targetRect = currentTargetRect,
                            color = borderColor,
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(1001f)
                        )
                    }
                    
                    // Prikaži strelicu ako je potrebno
                    if (steps[currentStep].showArrow) {
                        ArrowPointer(
                            targetRect = currentTargetRect,
                            direction = steps[currentStep].arrowDirection,
                            color = steps[currentStep].iconColor,
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(1001f)
                        )
                    }
                    
                    // Prikaži SVE strelice za bill card detalje
                    if (steps[currentStep].showMultipleArrows) {
                        MultipleArrowsForBillCard(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(1001f)
                        )
                    }
                } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(overlayColor)
                        .clickable(enabled = false) { }
                        .zIndex(1000f)
                )
                }
            } else {
                // Prvi step ILI Mock Bill step - tamna pozadina bez spotlight-a
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(overlayColor)
                        .clickable(enabled = false) { }
                        .zIndex(1000f)
                )
            }
            
            // Check if current step targets nav bar icons
            val isNavBarStep = steps[currentStep].targetId in listOf("pijaca_nav", "statistics_nav")
            val bottomPadding = if (isNavBarStep) 100.dp else 16.dp
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottomPadding)
                    .zIndex(1002f),
                contentAlignment = if (isNavBarStep) Alignment.Center else Alignment.BottomCenter
            ) {
                // For nav bar steps, center the card
                if (isNavBarStep) {
                    TutorialCard(
                        step = steps[currentStep],
                        currentStepNumber = currentStep + 1,
                        totalSteps = steps.size,
                        onNext = {
                            if (currentStep < steps.size - 1) {
                                currentStep++
                            } else {
                                SpotlightRegistry.clear()
                                onDismiss()
                            }
                        },
                        onSkip = {
                            SpotlightRegistry.clear()
                            onDismiss()
                        },
                        isLastStep = currentStep == steps.size - 1
                    )
                } else {
                    val showMockBill = steps[currentStep].showMockBillCard && steps[currentStep].mockBillStatus != null
                    
                    // All non-nav-bar steps - center content
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // Mock Bill Card (if this step has one)
                            if (showMockBill) {
                                MockBillCard(status = steps[currentStep].mockBillStatus!!)
                            }
                            
                            // Tutorial Card
                            TutorialCard(
                                step = steps[currentStep],
                                currentStepNumber = currentStep + 1,
                                totalSteps = steps.size,
                                onNext = {
                                    if (currentStep < steps.size - 1) {
                                        currentStep++
                                    } else {
                                        SpotlightRegistry.clear()
                                        onDismiss()
                                    }
                                },
                                onSkip = {
                                    SpotlightRegistry.clear()
                                    onDismiss()
                                },
                                isLastStep = currentStep == steps.size - 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpotlightCutout(
    targetRect: Rect,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val isDark = isSystemInDarkTheme()
    var canvasOffset by remember { mutableStateOf(Offset.Zero) }
    
    val overlayColor = if (isDark) Color.Black.copy(alpha = 0.95f) else Color(0xFFEFEBE9).copy(alpha = 0.95f)
    
    Canvas(
        modifier = modifier.onGloballyPositioned { coordinates ->
            canvasOffset = coordinates.localToWindow(Offset.Zero)
        }
    ) {
        val padding = with(density) { 4.dp.toPx() } // Minimalan padding
        val cornerRad = with(density) { 8.dp.toPx() }
        
        // Adjust target rect by canvas offset
        val adjustedRect = Rect(
            left = targetRect.left - canvasOffset.x,
            top = targetRect.top - canvasOffset.y,
            right = targetRect.right - canvasOffset.x,
            bottom = targetRect.bottom - canvasOffset.y
        )
        
        val path = Path().apply {
            // Full screen rectangle
            addRect(Rect(Offset.Zero, size))
            
            // Cutout area
            addRoundRect(
                RoundRect(
                    left = adjustedRect.left - padding,
                    top = adjustedRect.top - padding,
                    right = adjustedRect.right + padding,
                    bottom = adjustedRect.bottom + padding,
                    cornerRadius = CornerRadius(cornerRad)
                )
            )
        }
        
        path.fillType = PathFillType.EvenOdd
        
        drawPath(
            path = path,
            color = overlayColor
        )
    }
}

@Composable
fun NeonBorder(
    targetRect: Rect,
    color: Color,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var canvasOffset by remember { mutableStateOf(Offset.Zero) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "neon")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    
    Canvas(
        modifier = modifier.onGloballyPositioned { coordinates ->
            canvasOffset = coordinates.localToWindow(Offset.Zero)
        }
    ) {
        val padding = with(density) { 4.dp.toPx() }
        val cornerRadius = with(density) { 8.dp.toPx() }
        
        // Adjust target rect by canvas offset
        val adjustedRect = Rect(
            left = targetRect.left - canvasOffset.x,
            top = targetRect.top - canvasOffset.y,
            right = targetRect.right - canvasOffset.x,
            bottom = targetRect.bottom - canvasOffset.y
        )
        
        val left = adjustedRect.left - padding
        val top = adjustedRect.top - padding
        val right = adjustedRect.right + padding
        val bottom = adjustedRect.bottom + padding
        
        // 0. Intense Inner Glow (The "Aura" behind/over the icon)
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = glowAlpha * 0.7f),
                    color.copy(alpha = glowAlpha * 0.3f),
                    Color.Transparent
                ),
                center = adjustedRect.center,
                radius = (adjustedRect.width + adjustedRect.height) / 3f
            ),
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            cornerRadius = CornerRadius(cornerRadius)
        )

        // 1. Outer glow border
        drawRoundRect(
            color = color.copy(alpha = glowAlpha * 0.4f),
            topLeft = Offset(left - 4, top - 4),
            size = Size(right - left + 8, bottom - top + 8),
            cornerRadius = CornerRadius(cornerRadius + 4),
            style = Stroke(width = 12f)
        )
        
        // 2. Main neon border
        drawRoundRect(
            color = Color.White.copy(alpha = glowAlpha),
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            cornerRadius = CornerRadius(cornerRadius),
            style = Stroke(width = 3f)
        )
        
        // 3. Inner border
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.6f * glowAlpha),
                    color.copy(alpha = 0.8f * glowAlpha)
                )
            ),
            topLeft = Offset(left + 2, top + 2),
            size = Size(right - left - 4, bottom - top - 4),
            cornerRadius = CornerRadius(cornerRadius - 2),
            style = Stroke(width = 2f)
        )
    }
}

@Composable
fun TutorialCard(
    step: TutorialStep,
    currentStepNumber: Int,
    totalSteps: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    isLastStep: Boolean
) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color.Black
    val buttonBorderColor = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)
    val buttonTextColor = if (isDark) Color.White else Color.Black
    
    // Providna kartica bez pozadine
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
            if (step.showIcon) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(85.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer glow layer
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        step.iconColor.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        step.iconColor.copy(alpha = 0.4f),
                                        step.iconColor.copy(alpha = 0.1f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                            .border(2.dp, step.iconColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Inner neon aura (The "glow" behind the icon)
                        Box(
                            modifier = Modifier
                                .size(45.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            step.iconColor.copy(alpha = 0.6f),
                                            step.iconColor.copy(alpha = 0.2f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                )
                        )
                        
                        Icon(
                            imageVector = step.icon,
                            contentDescription = null,
                            tint = step.iconColor,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Column(
                modifier = Modifier.heightIn(min = 100.dp), // Removed max height constraint
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = step.title,
                    fontSize = 24.sp, // Slightly smaller
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = step.description,
                    fontSize = 16.sp, // Slightly smaller for longer text
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    color = textColor.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ProgressDots(
                currentStep = currentStepNumber,
                totalSteps = totalSteps,
                activeColor = step.iconColor
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = buttonTextColor
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, buttonBorderColor)
                ) {
                    Text("Preskoči", fontSize = 18.sp)
                }
                
                Button(
                    onClick = onNext,
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = step.iconColor.copy(alpha = 0.3f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(2.dp, step.iconColor)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isLastStep) "Završi" else "Sledeće",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isLastStep) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
}

@Composable
fun ProgressDots(
    currentStep: Int,
    totalSteps: Int,
    activeColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(totalSteps) { index ->
            val isActive = index + 1 == currentStep
            
            val inactiveColor = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.5f)
            
            Box(
                modifier = Modifier
                    .size(if (isActive) 12.dp else 8.dp)
                    .background(
                        color = if (isActive) activeColor else inactiveColor,
                        shape = CircleShape
                    )
                    .border(
                        width = if (isActive) 2.dp else 0.dp,
                        color = activeColor.copy(alpha = 0.7f),
                        shape = CircleShape
                    )
            )
            
            if (index < totalSteps - 1) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

fun Modifier.tutorialTarget(id: String): Modifier = this.onGloballyPositioned { coordinates ->
    // Use boundsInWindow() which gives correct window-relative coordinates
    val bounds = coordinates.boundsInWindow()
    
    // Register the raw bounds without manual offset adjustment
    // The overlay is drawn in the same coordinate space, so no offset needed
    SpotlightRegistry.register(id, bounds)
}

@Composable
fun Glass3DIcon(
    icon: ImageVector,
    backgroundColor: Color,
    size: Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0.8f),
                        backgroundColor.copy(alpha = 0.4f)
                    )
                )
            )
            .border(
                1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.5f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle 3D shadow/glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Icon with shadow
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(32.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    spotColor = backgroundColor
                )
        )
    }
}

