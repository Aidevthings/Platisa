package com.example.platisa.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * A Text composable that dynamically adjusts its font size to fit within the available width.
 * 
 * This component ensures that the entire text is always visible by:
 * - Starting with the maximum font size
 * - Measuring the text width with current font size
 * - Reducing font size until text fits within available width
 * - Never going below the minimum font size
 * 
 * @param text The text to display
 * @param modifier Modifier for the text
 * @param color Text color
 * @param fontWeight Font weight
 * @param fontFamily Font family
 * @param minFontSize Minimum font size (default: 20sp) - prevents text from becoming unreadable
 * @param maxFontSize Maximum font size (default: 60sp) - prevents text from being too large
 * @param maxLines Maximum number of lines (default: 1)
 */
@Composable
fun DynamicSizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    fontWeight: FontWeight = FontWeight.Bold,
    fontFamily: FontFamily = FontFamily.Monospace,
    minFontSize: TextUnit = 20.sp,
    maxFontSize: TextUnit = 60.sp,
    maxLines: Int = 1,
    textAlign: androidx.compose.ui.text.style.TextAlign = androidx.compose.ui.text.style.TextAlign.Center
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        
        // Available width in pixels (subtract some padding for safety)
        val availableWidthPx = with(density) { maxWidth.toPx() - 32f } // 16dp padding on each side
        
        // Binary search for optimal font size
        val optimalFontSize = remember(text, availableWidthPx) {
            var currentSize = maxFontSize.value
            val minSize = minFontSize.value
            
            // Quick check if even max size fits
            val maxSizeLayout = textMeasurer.measure(
                text = text,
                style = TextStyle(
                    fontSize = maxFontSize,
                    fontWeight = fontWeight,
                    fontFamily = fontFamily
                ),
                constraints = Constraints(maxWidth = Int.MAX_VALUE)
            )
            
            if (maxSizeLayout.size.width <= availableWidthPx) {
                return@remember maxFontSize
            }
            
            // Binary search for optimal size
            var low = minSize
            var high = maxFontSize.value
            var bestSize = minSize
            
            while (low <= high) {
                val mid = (low + high) / 2f
                
                val layout = textMeasurer.measure(
                    text = text,
                    style = TextStyle(
                        fontSize = mid.sp,
                        fontWeight = fontWeight,
                        fontFamily = fontFamily
                    ),
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
        
        Text(
            text = text,
            fontSize = optimalFontSize,
            color = color,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            maxLines = maxLines,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Alternative simple implementation using iterative approach
 * Useful for debugging or when binary search causes issues
 */
@Composable
fun DynamicSizeTextSimple(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    fontWeight: FontWeight = FontWeight.Bold,
    fontFamily: FontFamily = FontFamily.Monospace,
    minFontSize: TextUnit = 20.sp,
    maxFontSize: TextUnit = 60.sp,
    maxLines: Int = 1,
    stepSize: Float = 2f
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        
        val availableWidthPx = with(density) { maxWidth.toPx() - 32f }
        
        val optimalFontSize = remember(text, availableWidthPx) {
            var currentSize = maxFontSize.value
            
            while (currentSize >= minFontSize.value) {
                val layout = textMeasurer.measure(
                    text = text,
                    style = TextStyle(
                        fontSize = currentSize.sp,
                        fontWeight = fontWeight,
                        fontFamily = fontFamily
                    ),
                    constraints = Constraints(maxWidth = Int.MAX_VALUE)
                )
                
                if (layout.size.width <= availableWidthPx) {
                    return@remember currentSize.sp
                }
                
                currentSize -= stepSize
            }
            
            minFontSize
        }
        
        Text(
            text = text,
            fontSize = optimalFontSize,
            color = color,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            maxLines = maxLines
        )
    }
}
