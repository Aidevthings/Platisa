package com.platisa.app.ui.screens.help

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// Colors matching HomeScreen
private val HtmlBackgroundDark = Color(0xFF111217)
private val HtmlNeonCyan = Color(0xFF00EAFF)
private val HtmlNeonMagenta = Color(0xFFFF00D9)
private val HtmlNeonGreen = Color(0xFF39FF14)
private val HtmlLightCyan = Color(0xFF00D4DD)
private val HtmlGray400 = Color(0xFF9CA3AF)
private val HtmlGray900 = Color(0xFF1F2937)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    navController: NavController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Pomoć",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Nazad",
                            tint = HtmlNeonCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HtmlBackgroundDark
                )
            )
        },
        containerColor = HtmlBackgroundDark
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            // Welcome Section
            item { WelcomeCard() }
            
            // Header Icons Explanation
            item { HeaderIconsCard() }
            
            // Step 1: Scanning
            item {
                HelpStepCard(
                    stepNumber = 1,
                    title = "Kako Skenirati Račun",
                    icon = Icons.Default.CameraAlt,
                    iconColor = HtmlNeonCyan,
                    steps = listOf(
                        "Kliknite na dugme 'Slikaj Kamerom' na početnoj strani",
                        "Usmjerite kameru na račun tako da je čitav račun vidljiv",
                        "Sačekajte da se račun automatski prepozna",
                        "Pregledajte izvučene podatke i potvrdite"
                    )
                )
            }
            
            // Step 2: Bill Status Colors
            item { BillStatusCard() }
            
            // Step 3: Opening Bill Details
            item {
                HelpStepCard(
                    stepNumber = 2,
                    title = "Kako Pregledati Račun",
                    icon = Icons.Default.Description,
                    iconColor = HtmlNeonCyan,
                    steps = listOf(
                        "Na početnoj strani kliknite na bilo koji račun iz liste",
                        "Otvorit će se stranica sa detaljima računa",
                        "Ovde možete videti sve informacije o računu"
                    )
                )
            }
            
            // Step 4: Payment Process
            item { PaymentProcessCard() }
            
            // Step 5: Confirming Payment
            item {
                HelpStepCard(
                    stepNumber = 4,
                    title = "Kako Potvrditi Plaćanje",
                    icon = Icons.Default.CheckCircle,
                    iconColor = HtmlNeonGreen,
                    steps = listOf(
                        "Nakon što platite račun u banci, vratite se u Platisa",
                        "Kliknite na račun koji ste platili",
                        "Kliknite na dugme 'POTVRDI PLAĆANJE'",
                        "Račun će promeniti boju u zelenu ✓"
                    )
                )
            }
            
            // FAQ Section
            item { FAQCard() }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun WelcomeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = HtmlGray900),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = HtmlNeonCyan,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Dobrodošli u Platisa!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Ovaj vodič će vam pomoći da naučite kako da koristite aplikaciju za upravljanje računima.",
                fontSize = 15.sp,
                color = HtmlGray400,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
fun HeaderIconsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = HtmlGray900),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = HtmlNeonCyan,
                    modifier = Modifier.size(28.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "Ikone na Vrhu Ekrana",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Avatar Icon
            IconExplanation(
                icon = Icons.Default.Person,
                iconColor = HtmlNeonCyan,
                title = "Avatar / Profil",
                description = "Kliknite na avatar ili 'Zdravo, [Ime]!' da biste otvorili stranicu za prilagođavanje profila. Možete promeniti ime, avatar sliku i poruke obeležavanja."
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Notification Bell
            IconExplanation(
                icon = Icons.Default.Notifications,
                iconColor = HtmlNeonCyan,
                title = "🔔 Zvonce (Notifikacije)",
                description = "Otvara podešavanja notifikacija. Možete uključiti/isključiti podsetnicina za rokove plaćanja i prilagoditi koliko dana unapred želite da budete podseteni."
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Theme Toggle
            IconExplanation(
                icon = Icons.Default.LightMode,
                iconColor = Color.White,
                title = "☀️ Sunce (Tema)",
                description = "Menja temu aplikacije između svetlog (Light) i tamnog (Dark) režima. Tamni režim štedi bateriju na OLED ekranima."
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Help Icon
            IconExplanation(
                icon = Icons.Default.Help,
                iconColor = HtmlNeonGreen,
                title = "❓ Upitnik (Pomoć)",
                description = "Otvara ovu stranicu sa uputstvom. Možete je otvoriti bilo kada ako vam zatreba pomoć ili želite da ponovite tutorial."
            )
        }
    }
}

@Composable
fun IconExplanation(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconColor.copy(alpha = 0.2f), CircleShape)
                .border(1.dp, iconColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = HtmlGray400,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun HelpStepCard(
    stepNumber: Int,
    title: String,
    icon: ImageVector,
    iconColor: Color,
    steps: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = HtmlGray900),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconColor.copy(alpha = 0.2f), CircleShape)
                        .border(1.dp, iconColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber.toString(),
                        color = iconColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            steps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "•",
                        color = iconColor,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = step,
                        fontSize = 15.sp,
                        color = HtmlGray400,
                        lineHeight = 22.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (index < steps.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun BillStatusCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = HtmlGray900),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = HtmlNeonCyan,
                    modifier = Modifier.size(28.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "Boje Računa (Statusi)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Računi menjaju boju u zavisnosti od statusa plaćanja:",
                fontSize = 15.sp,
                color = HtmlGray400,
                lineHeight = 22.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            StatusRow(
                color = HtmlLightCyan,
                label = "NEPLAĆEN",
                description = "Račun je skeniran ali još nije plaćen"
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            StatusRow(
                color = HtmlNeonMagenta,
                label = "U OBRADI",
                description = "QR kod je sačuvan, čeka se potvrda plaćanja"
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            StatusRow(
                color = HtmlNeonGreen,
                label = "PLAĆEN",
                description = "Račun je uspešno plaćen i potvrđen ✓"
            )
        }
    }
}

@Composable
fun StatusRow(
    color: Color,
    label: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = description,
                fontSize = 13.sp,
                color = HtmlGray400,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun PaymentProcessCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = HtmlGray900),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(HtmlNeonMagenta.copy(alpha = 0.2f), CircleShape)
                        .border(1.dp, HtmlNeonMagenta, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "3",
                        color = HtmlNeonMagenta,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = null,
                    tint = HtmlNeonMagenta,
                    modifier = Modifier.size(28.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "Kako Platiti Račun",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            StepWithIcon("1", "Kliknite na račun koji želite da platite", HtmlNeonMagenta)
            Spacer(modifier = Modifier.height(12.dp))
            
            StepWithIcon("2", "Kliknite na dugme 'Sačuvaj QR Kod'", HtmlNeonMagenta)
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HtmlNeonMagenta.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .border(1.dp, HtmlNeonMagenta.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "📸 QR kod se automatski čuva u galeriji telefona",
                        fontSize = 14.sp,
                        color = HtmlNeonMagenta,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Račun menja boju u MAGENTA (U OBRADI)",
                        fontSize = 13.sp,
                        color = HtmlGray400
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            StepWithIcon("3", "Otvorite mobilnu bankarsku aplikaciju", HtmlNeonMagenta)
            Spacer(modifier = Modifier.height(12.dp))
            
            StepWithIcon("4", "Odaberite opciju za plaćanje QR kodom preko IPS sistema", HtmlNeonMagenta)
            Spacer(modifier = Modifier.height(12.dp))
            
            StepWithIcon("5", "Skenirajte sačuvani QR kod iz galerije", HtmlNeonMagenta)
            Spacer(modifier = Modifier.height(12.dp))
            
            StepWithIcon("6", "Potvrdite plaćanje u banci", HtmlNeonMagenta)
        }
    }
}

@Composable
fun StepWithIcon(number: String, text: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(color.copy(alpha = 0.2f), CircleShape)
                .border(1.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = number, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = text,
            fontSize = 15.sp,
            color = HtmlGray400,
            lineHeight = 22.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun FAQCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = HtmlGray900),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.QuestionAnswer,
                    contentDescription = null,
                    tint = HtmlNeonGreen,
                    modifier = Modifier.size(28.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "Često Postavljana Pitanja",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            FAQItem(
                question = "Šta je STORNO račun?",
                answer = "STORNO račun označava da je prethodni račun otkazan. Automatski se sakriva jer ne zahteva plaćanje."
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            FAQItem(
                question = "Da li mogu da platim račun bez QR koda?",
                answer = "Da, možete uneti podatke ručno u bankarskoj aplikaciji, ali QR kod značajno ubrzava proces."
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            FAQItem(
                question = "Šta ako aplikacija ne prepozna račun?",
                answer = "Pokušajte da slikate račun u dobrom osvetljenju i da je čitav račun vidljiv u kadru."
            )
        }
    }
}

@Composable
fun FAQItem(question: String, answer: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "❓ $question",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = answer,
            fontSize = 14.sp,
            color = HtmlGray400,
            lineHeight = 20.sp
        )
    }
}

