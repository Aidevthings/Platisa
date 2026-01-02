# DynamicSizeText - Quick Reference Card

## Import
```kotlin
import com.example.platisa.ui.components.DynamicSizeText
```

## Basic Usage
```kotlin
DynamicSizeText(
    text = "Your text here",
    minFontSize = 20.sp,
    maxFontSize = 60.sp
)
```

## All Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `text` | String | Required | Text to display |
| `modifier` | Modifier | Modifier | Layout modifier |
| `color` | Color | Color.White | Text color |
| `fontWeight` | FontWeight | FontWeight.Bold | Font weight |
| `fontFamily` | FontFamily | FontFamily.Monospace | Font family |
| `minFontSize` | TextUnit | 20.sp | Minimum font size |
| `maxFontSize` | TextUnit | 60.sp | Maximum font size |
| `maxLines` | Int | 1 | Maximum lines |

## Common Configurations

### Dashboard Display (Large)
```kotlin
DynamicSizeText(
    text = amount,
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp),
    minFontSize = 24.sp,
    maxFontSize = 60.sp
)
```

### Card Display (Medium)
```kotlin
DynamicSizeText(
    text = amount,
    modifier = Modifier
        .fillMaxWidth(0.8f)
        .padding(horizontal = 16.dp),
    minFontSize = 20.sp,
    maxFontSize = 42.sp
)
```

### List Item (Compact)
```kotlin
DynamicSizeText(
    text = amount,
    modifier = Modifier.widthIn(max = 150.dp),
    minFontSize = 14.sp,
    maxFontSize = 24.sp
)
```

## Size Recommendations

| Use Case | Min | Max | Container Width |
|----------|-----|-----|-----------------|
| Hero/Dashboard | 24sp | 60sp | 300-400dp |
| Card Header | 20sp | 48sp | 200-300dp |
| List Item | 14sp | 24sp | 120-150dp |
| Badge/Chip | 12sp | 18sp | 60-100dp |

## Quick Tips

### ✅ DO
- Use for numbers/amounts with variable length
- Use monospace fonts for numbers
- Set maxLines = 1 for best results
- Add padding for breathing room
- Test with min and max values

### ❌ DON'T
- Use for static labels
- Use in performance-critical loops
- Forget to constrain width
- Use with multi-line text
- Set min = max (just use Text)

## Troubleshooting

### Text Overflows
→ Lower `minFontSize`  
→ Increase container width  
→ Check padding values

### Text Too Small
→ Increase `maxFontSize`  
→ Reduce text length  
→ Widen container

### Performance Issues
→ Use in headers, not lists  
→ Check `remember` is working  
→ Consider static sizing for lists

## Example Outputs

```
Amount: 500
Size: 60sp (max)
████████ 500 ████████

Amount: 25,000
Size: 48sp (scaled)
████ 25.000 ████

Amount: 1,500,000
Size: 28sp (scaled)
█ 1.500.000 █
```

## Common Patterns

### With Currency Symbol
```kotlin
Row {
    Text("$", fontSize = 20.sp)
    DynamicSizeText(text = amount)
}
```

### In Centered Box
```kotlin
Box(
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = Alignment.Center
) {
    DynamicSizeText(text = amount)
}
```

### With Label Below
```kotlin
Column(horizontalAlignment = Alignment.CenterHorizontally) {
    DynamicSizeText(text = amount)
    Text("Total", fontSize = 14.sp)
}
```

---
**Version:** 1.0 | **Date:** Dec 2025 | **Component:** DynamicSizeText.kt
