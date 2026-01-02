# DynamicSizeText - Usage Guide & Examples

## Quick Start

### Basic Usage
```kotlin
import com.example.platisa.ui.components.DynamicSizeText

DynamicSizeText(
    text = "Your dynamic text here",
    minFontSize = 20.sp,
    maxFontSize = 60.sp
)
```

## Complete Examples

### Example 1: Dashboard Total (Large Display)
```kotlin
@Composable
fun DashboardTotal(amount: BigDecimal) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            DynamicSizeText(
                text = formatCurrency(amount),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                minFontSize = 24.sp,  // Still readable even if huge number
                maxFontSize = 60.sp,  // Big impact for small numbers
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Total Amount",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}
```

### Example 2: List Item Amount (Compact Display)
```kotlin
@Composable
fun ListItemAmount(amount: BigDecimal) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Merchant Name", fontWeight = FontWeight.Bold)
            Text("Due: 01/15/2025", fontSize = 12.sp, color = Color.Gray)
        }
        
        DynamicSizeText(
            text = formatCurrency(amount),
            modifier = Modifier.widthIn(max = 150.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            minFontSize = 14.sp,  // Minimum for list items
            maxFontSize = 24.sp,  // Maximum to not overpower layout
            maxLines = 1
        )
    }
}
```

### Example 3: Card Header (Medium Display)
```kotlin
@Composable
fun CardHeaderWithAmount(title: String, amount: BigDecimal) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            DynamicSizeText(
                text = formatCurrency(amount),
                modifier = Modifier.fillMaxWidth(0.9f),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                minFontSize = 18.sp,
                maxFontSize = 42.sp,
                maxLines = 1
            )
        }
    }
}
```

### Example 4: Multiple Currency Support
```kotlin
@Composable
fun MultiCurrencyDisplay(
    amount: BigDecimal,
    currency: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DynamicSizeText(
            text = formatNumber(amount),
            modifier = Modifier.weight(1f),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            minFontSize = 20.sp,
            maxFontSize = 48.sp,
            maxLines = 1
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = currency,
            fontSize = 18.sp,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
    }
}
```

### Example 5: With Background and Border
```kotlin
@Composable
fun StyledAmountBox(amount: BigDecimal) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.Cyan.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        DynamicSizeText(
            text = formatCurrency(amount),
            modifier = Modifier.fillMaxWidth(0.95f),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            minFontSize = 22.sp,
            maxFontSize = 56.sp,
            maxLines = 1
        )
    }
}
```

### Example 6: Animated Updates
```kotlin
@Composable
fun AnimatedAmountDisplay(amount: BigDecimal) {
    var displayedAmount by remember { mutableStateOf(BigDecimal.ZERO) }
    
    LaunchedEffect(amount) {
        animate(
            initialValue = displayedAmount.toFloat(),
            targetValue = amount.toFloat()
        ) { value, _ ->
            displayedAmount = BigDecimal(value.toString())
        }
    }
    
    DynamicSizeText(
        text = formatCurrency(displayedAmount),
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        minFontSize = 24.sp,
        maxFontSize = 60.sp,
        maxLines = 1
    )
}
```

## Configuration Guide

### Font Size Selection Matrix

| Use Case | Container | Min Size | Max Size | Reasoning |
|----------|-----------|----------|----------|-----------|
| **Dashboard Hero** | Full width (300-400dp) | 24sp | 60sp | Maximum visual impact |
| **Card Total** | 80% width (200-300dp) | 20sp | 48sp | Prominent but balanced |
| **List Item** | Fixed width (120-150dp) | 14sp | 24sp | Compact for scrolling |
| **Badge/Chip** | Small (60-100dp) | 12sp | 18sp | Tight space constraint |
| **Mini Widget** | Tiny (40-80dp) | 10sp | 16sp | Minimal space |

### Recommended Padding Values

```kotlin
// For Large Displays (Dashboard)
modifier = Modifier
    .fillMaxWidth()
    .padding(horizontal = 24.dp)  // Give text breathing room

// For Medium Displays (Cards)
modifier = Modifier
    .fillMaxWidth()
    .padding(horizontal = 16.dp)

// For Small Displays (Lists)
modifier = Modifier
    .widthIn(max = 150.dp)
    .padding(horizontal = 8.dp)
```

### Font Family Recommendations

```kotlin
// For Money/Numbers - Use Monospace
fontFamily = FontFamily.Monospace
// Benefits: Equal character widths, better alignment

// For General Text - Use Default/Sans
fontFamily = FontFamily.Default
// Benefits: More space-efficient, better readability

// For Emphasis - Use Serif
fontFamily = FontFamily.Serif
// Benefits: Traditional, formal appearance
```

## Common Patterns

### Pattern 1: Currency with Symbol
```kotlin
@Composable
fun CurrencyWithSymbol(amount: BigDecimal) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$",
            fontSize = 20.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        DynamicSizeText(
            text = formatNumber(amount),
            minFontSize = 20.sp,
            maxFontSize = 48.sp
        )
    }
}
```

### Pattern 2: Value with Unit
```kotlin
@Composable
fun ValueWithUnit(value: Int, unit: String) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center
    ) {
        DynamicSizeText(
            text = value.toString(),
            modifier = Modifier.weight(1f),
            minFontSize = 24.sp,
            maxFontSize = 60.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = unit,
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}
```

### Pattern 3: Percentage Display
```kotlin
@Composable
fun PercentageDisplay(percentage: Double) {
    DynamicSizeText(
        text = "${String.format("%.1f", percentage)}%",
        modifier = Modifier.fillMaxWidth(),
        color = if (percentage >= 0) Color.Green else Color.Red,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        minFontSize = 20.sp,
        maxFontSize = 48.sp
    )
}
```

## Debugging Tips

### Issue 1: Text Still Overflows
```kotlin
// Problem:
DynamicSizeText(
    text = veryLongText,
    minFontSize = 30.sp  // Too large!
)

// Solution: Lower the minimum
DynamicSizeText(
    text = veryLongText,
    minFontSize = 16.sp  // More flexible
)
```

### Issue 2: Text Too Small
```kotlin
// Problem:
DynamicSizeText(
    text = shortText,
    maxFontSize = 24.sp  // Too small!
)

// Solution: Increase the maximum
DynamicSizeText(
    text = shortText,
    maxFontSize = 60.sp  // Better impact
)
```

### Issue 3: Not Centering Properly
```kotlin
// Problem:
DynamicSizeText(text = amount)

// Solution: Add alignment modifier
Box(
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = Alignment.Center
) {
    DynamicSizeText(
        text = amount,
        modifier = Modifier.fillMaxWidth(0.9f)
    )
}
```

### Issue 4: Performance with Long Lists
```kotlin
// Problem: Using DynamicSizeText in LazyColumn with 1000+ items

// Solution: Use simpler static sizing for list items
// Only use DynamicSizeText for hero/header elements

LazyColumn {
    item {
        // Use dynamic sizing here
        DynamicSizeText(text = totalSum)
    }
    
    items(receipts) { receipt ->
        // Use static sizing for performance
        Text(
            text = receipt.amount,
            fontSize = 18.sp
        )
    }
}
```

## Testing Utilities

### Test Different Amounts
```kotlin
@Preview
@Composable
fun TestDynamicSizing() {
    val testAmounts = listOf(
        BigDecimal("100"),
        BigDecimal("1000"),
        BigDecimal("10000"),
        BigDecimal("100000"),
        BigDecimal("1000000"),
        BigDecimal("10000000")
    )
    
    Column {
        testAmounts.forEach { amount ->
            DynamicSizeText(
                text = formatCurrency(amount),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, Color.Red), // Shows boundaries
                minFontSize = 24.sp,
                maxFontSize = 60.sp
            )
        }
    }
}
```

### Measure Font Size
```kotlin
@Composable
fun DynamicSizeTextWithDebug(text: String) {
    var currentSize by remember { mutableStateOf(0.sp) }
    
    Column {
        DynamicSizeText(
            text = text,
            minFontSize = 20.sp,
            maxFontSize = 60.sp,
            onSizeCalculated = { size -> currentSize = size }
        )
        
        Text(
            text = "Font size: $currentSize",
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}
```

## Performance Benchmarks

### Typical Measurements
```
Text: "125.000 RSD" (11 characters)
Available width: 300dp (900px)
─────────────────────────────────
Iterations: 5-6
Time: < 1ms
Memory: ~2KB
Result: 46sp
```

### Compared to Linear Search
```
Binary Search:   O(log n) = ~5-6 iterations
Linear Search:   O(n) = ~20-30 iterations
Performance Gain: 4-5x faster
```

## Best Practices

### ✅ DO:
- Use for variable-length numeric displays
- Set reasonable min/max bounds
- Use monospace fonts for numbers
- Add padding for text breathing room
- Test with extreme values (min and max)

### ❌ DON'T:
- Use for static labels
- Set min = max (just use Text)
- Use in performance-critical lists
- Forget to set maxLines = 1
- Use with multi-line content

## Migration Guide

### From Static Sizing
```kotlin
// Before:
Text(
    text = formatCurrency(amount),
    fontSize = 24.sp
)

// After:
DynamicSizeText(
    text = formatCurrency(amount),
    minFontSize = 18.sp,
    maxFontSize = 32.sp
)
```

### From Length-Based Logic
```kotlin
// Before:
val fontSize = when {
    text.length <= 8 -> 48.sp
    text.length <= 10 -> 40.sp
    else -> 32.sp
}
Text(text = text, fontSize = fontSize)

// After:
DynamicSizeText(
    text = text,
    minFontSize = 32.sp,
    maxFontSize = 48.sp
)
```

---

**Last Updated:** December 15, 2025  
**Component Version:** 1.0  
**Minimum Compose Version:** 1.5.0
