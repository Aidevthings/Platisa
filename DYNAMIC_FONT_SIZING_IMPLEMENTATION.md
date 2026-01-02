# Dynamic Font Sizing Implementation

## Overview
This document describes the implementation of dynamic font sizing for the Platisa app's home page redesign, specifically for the **Total Sum** display and **Bill Card amounts**.

## Problem Statement
The app needs to display monetary amounts that can vary widely in length (from hundreds to millions). The challenge is to:
- Keep the entire number visible at all times (no truncation)
- Make small numbers prominently visible (large font)
- Make large numbers fit within the available space (smaller font)
- Maintain a visually pleasing design

## Solution: DynamicSizeText Component

### Component Location
**File:** `app/src/main/java/com/example/platisa/ui/components/DynamicSizeText.kt`

### How It Works

The `DynamicSizeText` component uses a **binary search algorithm** to find the optimal font size:

1. **Measure Available Space**: Uses `BoxWithConstraints` to get the maximum width available
2. **Binary Search**: Efficiently finds the largest font size that fits within the space
3. **Text Measurement**: Uses `rememberTextMeasurer` to calculate actual text width at different font sizes
4. **Caching**: Remembers the optimal font size to avoid recalculation on every recomposition

### Algorithm Details

```kotlin
// Binary search for optimal font size
var low = minSize
var high = maxFontSize.value
var bestSize = minSize

while (low <= high) {
    val mid = (low + high) / 2f
    
    // Measure text width at current font size
    val layout = textMeasurer.measure(text, fontSize = mid.sp)
    
    if (layout.size.width <= availableWidthPx) {
        bestSize = mid  // This size fits, try larger
        low = mid + 1f
    } else {
        high = mid - 1f  // This size doesn't fit, try smaller
    }
}
```

### Performance Optimization
- **Binary Search**: O(log n) complexity instead of linear iteration
- **Remember**: Caches result so it only recalculates when text or width changes
- **Efficient**: Typically finds optimal size in 4-6 iterations instead of dozens

## Implementation in HomeScreen

### 1. Total Sum Display (SummaryGrid)

**Location:** `HomeScreen.kt` lines 391-401

**Configuration:**
```kotlin
DynamicSizeText(
    text = formatCurrency(totalAmount),
    minFontSize = 24.sp,  // Minimum readable size
    maxFontSize = 60.sp,  // Maximum size for visual impact
    fontFamily = FontFamily.Monospace  // Consistent character widths
)
```

**Range Support:**
- **Small amounts** (100-1,000): Display at ~60sp for maximum visibility
- **Medium amounts** (1,000-100,000): Scale down proportionally
- **Large amounts** (100,000-1,000,000): Display at minimum ~24sp to fit

### 2. Bill Card Amounts (ModernBillCard)

**Location:** `HomeScreen.kt` lines 585-596 and 661-672

**Configuration:**
```kotlin
DynamicSizeText(
    text = formatCurrency(receipt.totalAmount),
    modifier = Modifier.widthIn(max = 150.dp),  // Constrain max width
    minFontSize = 14.sp,  // Smaller min for tight spaces
    maxFontSize = 24.sp,  // Moderate max for card layout
    fontFamily = FontFamily.Monospace
)
```

**Why Different Settings?**
- Cards have less space than the main display
- Need to balance with merchant name and other info
- Still maintain readability while being more compact

## Font Size Recommendations

### Total Sum Display
- **Minimum**: 24sp
  - Still clearly readable even for very large numbers
  - Maintains professional appearance
- **Maximum**: 60sp
  - Creates visual impact for dashboard
  - Draws attention to important metric
  - Matches modern UI design trends

### Bill Card Amounts
- **Minimum**: 14sp
  - Readable in list context
  - Allows very large amounts to fit
- **Maximum**: 24sp
  - Doesn't overpower other card content
  - Maintains visual hierarchy

## Visual Examples

### Total Sum Scenarios:

**Small Amount (500 RSD)**
```
┌─────────────────────────┐
│                         │
│        500 RSD          │  ← Font: 60sp (maximum)
│                         │
│  Ukupno za plaćanje     │
└─────────────────────────┘
```

**Medium Amount (125,000 RSD)**
```
┌─────────────────────────┐
│                         │
│     125.000 RSD         │  ← Font: ~42sp (scaled)
│                         │
│  Ukupno za plaćanje     │
└─────────────────────────┘
```

**Large Amount (1,500,000 RSD)**
```
┌─────────────────────────┐
│                         │
│   1.500.000 RSD         │  ← Font: ~28sp (near minimum)
│                         │
│  Ukupno za plaćanje     │
└─────────────────────────┘
```

## Technical Considerations

### Performance
- **Efficient**: Binary search finds optimal size in ~5 iterations
- **Cached**: Result is remembered and only recalculates when needed
- **Smooth**: No visible lag or stuttering during rendering

### Accessibility
- **Minimum Size**: 24sp ensures readability for visually impaired users
- **High Contrast**: White text on dark background meets WCAG standards
- **Monospace Font**: Consistent character widths improve readability

### Edge Cases Handled
1. **Extremely small numbers** (< 100): Displayed at maximum size
2. **Extremely large numbers** (> 1,000,000): Will reduce to minimum but stay readable
3. **Empty/null amounts**: Displays "0 RSD" at maximum size
4. **Dynamic locale formatting**: Works with any number format (periods, commas, etc.)

## Alternative Implementation

The file also includes `DynamicSizeTextSimple` which uses iterative approach instead of binary search:

```kotlin
DynamicSizeTextSimple(
    text = text,
    minFontSize = 20.sp,
    maxFontSize = 60.sp,
    stepSize = 2f  // Reduce by 2sp each iteration
)
```

**When to use:**
- Debugging issues with binary search
- Need predictable step-down behavior
- Working with very narrow ranges

## Testing Recommendations

### Manual Testing Scenarios
1. **Test with amount: 100** → Should display at 60sp
2. **Test with amount: 1,000** → Should display at ~55sp
3. **Test with amount: 10,000** → Should display at ~48sp
4. **Test with amount: 100,000** → Should display at ~38sp
5. **Test with amount: 1,000,000** → Should display at ~28sp

### Visual Regression Testing
- Verify no text truncation occurs
- Check that text remains centered
- Ensure smooth font size transitions
- Confirm minimum size never goes below 24sp

## Future Enhancements

### Potential Improvements
1. **Animation**: Animate font size changes when amount updates
2. **Padding Adjustment**: Also adjust padding based on font size
3. **Multiple Lines**: Support for multi-line amounts if extremely large
4. **Custom Formatters**: Different formatting for different currencies

### Performance Optimizations
- Consider caching common sizes (100, 1000, 10000, etc.)
- Pre-calculate size lookup table for common amounts
- Use coroutines for very complex calculations

## Maintenance Notes

### When to Update
- If UI layout changes (width of containers)
- If font family changes (different character widths)
- If number format changes (more/fewer digits)
- If design system updates (new min/max sizes)

### How to Adjust
1. **Change min/max font sizes** in component parameters
2. **Adjust padding** in BoxWithConstraints calculation
3. **Update font family** if switching to proportional font
4. **Modify step size** in binary search for finer control

## Related Files
- `HomeScreen.kt` - Main implementation
- `DynamicSizeText.kt` - Reusable component
- `Formatters.kt` - Number formatting utilities
- `Color.kt` - Theme colors used in display

## References
- [Jetpack Compose Text Measurement](https://developer.android.com/reference/kotlin/androidx/compose/ui/text/package-summary)
- [BoxWithConstraints Documentation](https://developer.android.com/jetpack/compose/layouts/constraints-modifiers)
- [Material Design Typography](https://material.io/design/typography/the-type-system.html)

---

**Last Updated:** December 15, 2025  
**Author:** Claude (Anthropic)  
**Version:** 1.0
