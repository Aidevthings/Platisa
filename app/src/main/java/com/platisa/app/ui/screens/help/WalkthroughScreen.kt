package com.platisa.app.ui.screens.help

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme // IMPORT ADDED
import androidx.navigation.NavController
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.platisa.app.R
import com.platisa.app.ui.components.PlatisaButton
import androidx.compose.ui.graphics.Brush

@OptIn(ExperimentalPagerApi::class)
@Composable
fun WalkthroughScreen(
    navController: NavController
) {
    val isDark = isSystemInDarkTheme()
    
    val pages = listOf(
        WalkthroughPage(
            title = "Dobrodošli u Platišu",
            description = "Vaš pametni asistent za upravljanje računima. Pratite troškove, plaćajte na vreme i uštedite novac.",
            imageResLight = R.drawable.walkthrough_home,
            imageResDark = R.drawable.walkthrough_home_dark
        ),
        WalkthroughPage(
            title = "Sve na Jednom Mestu",
            description = "Glavna tabla vam daje trenutni uvid u mesečnu potrošnju, sa jasnim grafičkim prikazom budžeta.",
            imageResLight = R.drawable.walkthrough_home, // Reuse home for consistency or potential specific intro
            imageResDark = R.drawable.walkthrough_home_dark
        ),
        WalkthroughPage(
            title = "Pametno Dodavanje",
            description = "Slikajte QR kod za trenutno prepoznavanje ili učitajte PDF račune direktno iz Gmail-a.",
            imageResLight = R.drawable.walkthrough_add_bill,
            imageResDark = R.drawable.walkthrough_add_bill_dark
        ),
        WalkthroughPage(
            title = "Detaljan Pregled",
            description = "Svaka stavka na računu je analizirana. Vidite tačno šta plaćate - od VT/NT struje do taksi.",
            imageResLight = R.drawable.walkthrough_details,
            imageResDark = R.drawable.walkthrough_details_dark
        ),
        WalkthroughPage(
            title = "Moćna Analitika",
            description = "Razumite svoje navike potrošnje kroz detaljne grafikone i trendove po mesecima.",
            imageResLight = R.drawable.walkthrough_analytics,
            imageResDark = R.drawable.walkthrough_analytics_dark
        ),
        WalkthroughPage(
            title = "Puna Kontrola",
            description = "Podesite budžet, promenite temu ili aktivirajte Premium opcije u podešavanjima.",
            imageResLight = R.drawable.walkthrough_settings,
            imageResDark = R.drawable.walkthrough_settings_dark
        )
    )

    val pagerState = rememberPagerState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Close Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            TextButton(onClick = { navController.popBackStack() }) {
                Text("Zatvori", color = MaterialTheme.colorScheme.primary)
            }
        }

        // Pager
        HorizontalPager(
            count = pages.size,
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            WalkthroughPageContent(pages[page], isDark)
        }

        // Indicators and Navigation
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Indicators
            Row(
                Modifier
                    .height(50.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pages.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(if (pagerState.currentPage == iteration) 12.dp else 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Button (Next or Finish)
            PlatisaButton(
                text = if (pagerState.currentPage == pages.lastIndex) "Završi" else "Dalje",
                onClick = {
                    /* Handle navigation or scroll */
                    if (pagerState.currentPage == pages.lastIndex) {
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}



@Composable
fun WalkthroughPageContent(page: WalkthroughPage, isDark: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Background Image (Full height of the pager item)
        Image(
            painter = painterResource(id = if (isDark) page.imageResDark else page.imageResLight),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Gradient Scrim & Text Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 0f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp), // Padding for text
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = page.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
                
                // Add a small spacer at the bottom to lift text slightly above the very edge if needed
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

data class WalkthroughPage(
    val title: String,
    val description: String,
    val imageResLight: Int,
    val imageResDark: Int
)

