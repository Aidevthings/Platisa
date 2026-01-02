# Dynamic Font Sizing - Visual Flow Diagram

## Algorithm Flow

```
┌─────────────────────────────────────────────────────────────┐
│  START: User's total amount needs to be displayed          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  BoxWithConstraints measures available width                │
│  Example: 300dp = 900px (at 3x density)                    │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  Format amount: 125000 → "125.000"                          │
│  Character count: 7 characters                              │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  BINARY SEARCH ALGORITHM                                     │
│  ─────────────────────────────────────                      │
│  Initial range: minSize=24sp to maxSize=60sp                │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
                    ┌────────┐
                    │ Try 42sp│ (middle of 24-60)
                    └────┬───┘
                         │
          ┌──────────────┴──────────────┐
          │                             │
          ▼                             ▼
    ┌─────────┐                   ┌─────────┐
    │ TOO BIG │                   │ FITS!   │
    └────┬────┘                   └────┬────┘
         │                             │
         ▼                             ▼
    Try 33sp                       Try 51sp
    (middle of 24-42)             (middle of 42-60)
         │                             │
         └──────────┬──────────────────┘
                    │
                    ▼
            Continue until...
                    │
                    ▼
┌─────────────────────────────────────────────────────────────┐
│  OPTIMAL SIZE FOUND: 48sp                                   │
│  - Largest size that fits within available width            │
│  - Text does not overflow                                   │
│  - Found in ~5 iterations                                   │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  RESULT CACHED (remember)                                    │
│  - Only recalculate if text or width changes                │
│  - Subsequent renders use cached value                      │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  RENDER TEXT at 48sp                                        │
│  "125.000" perfectly fits and is clearly visible           │
└─────────────────────────────────────────────────────────────┘
```

## Visual Examples by Amount Size

### Scenario 1: Small Amount (500)
```
Available Width: ████████████████████ (300dp)
Text: "500"
Character Count: 3

┌────────────────────┐
│                    │
│       500          │ ← 60sp (MAXIMUM)
│                    │
└────────────────────┘
   ▲▲▲▲▲▲▲▲▲▲▲▲▲
   Lots of space
```

### Scenario 2: Medium Amount (25,000)
```
Available Width: ████████████████████ (300dp)
Text: "25.000"
Character Count: 6

┌────────────────────┐
│                    │
│     25.000         │ ← 52sp (SCALED)
│                    │
└────────────────────┘
   ▲▲▲▲▲▲▲▲▲
   Good spacing
```

### Scenario 3: Large Amount (1,500,000)
```
Available Width: ████████████████████ (300dp)
Text: "1.500.000"
Character Count: 9

┌────────────────────┐
│                    │
│  1.500.000         │ ← 28sp (NEAR MIN)
│                    │
└────────────────────┘
   ▲▲
   Tight fit
```

### Scenario 4: Very Large Amount (50,000,000)
```
Available Width: ████████████████████ (300dp)
Text: "50.000.000"
Character Count: 10

┌────────────────────┐
│                    │
│ 50.000.000         │ ← 24sp (MINIMUM)
│                    │
└────────────────────┘
   ▲
   Just fits
```

## Bill Card Comparison

### Before (Static Sizing):
```
┌──────────────────────────────────┐
│  EPS Distribucija    125.000 RSD │ ← 24sp ALWAYS
│  Elektroprivreda  1.500.000 RSD  │ ← 24sp ALWAYS (too small!)
│  Telekom               500 RSD   │ ← 24sp ALWAYS (too small!)
└──────────────────────────────────┘
```

### After (Dynamic Sizing):
```
┌──────────────────────────────────┐
│  EPS Distribucija    125.000 RSD │ ← 24sp (fits perfectly)
│  Elektroprivreda  1.500.000 RSD  │ ← 18sp (scaled down)
│  Telekom              500 RSD    │ ← 24sp (max for small)
└──────────────────────────────────┘
```

## Performance Comparison

### Old Method (Length-Based):
```
IF length <= 8:  use 48sp
IF length <= 10: use 40sp
IF length <= 12: use 34sp
ELSE:            use 28sp

Issues:
- Doesn't account for actual character widths
- Different fonts = different results
- No optimization for available space
```

### New Method (Binary Search):
```
STEP 1: Measure available width
STEP 2: Binary search for optimal size
STEP 3: Cache result
STEP 4: Render

Benefits:
+ Accounts for actual character widths
+ Works with any font family
+ Optimal use of available space
+ Smart caching for performance
```

## Edge Case Handling

### Case 1: Extremely Wide Container
```
Container: ████████████████████████████████████ (600dp)
Text: "500"

Result: Still capped at MAX (60sp)
Reason: Prevents text from looking absurd
```

### Case 2: Extremely Narrow Container
```
Container: ████ (50dp)
Text: "1.500.000"

Result: Still uses MIN (24sp) even if it overflows slightly
Reason: Maintains minimum readability
Note: This scenario shouldn't happen with proper layout
```

### Case 3: Font Family Change
```
Monospace: "1234567890" = 250px
Proportional: "1234567890" = 215px

Result: Dynamic sizing adapts automatically
Reason: Uses actual text measurement, not estimation
```

## Integration Points

### Where Dynamic Sizing is Used:
```
HomeScreen.kt
├── SummaryGrid
│   └── DynamicSizeText (Total Sum)
│       • Min: 24sp, Max: 60sp
│       • Full width available
│       • Monospace font
│
└── ModernBillCard
    ├── PROCESSING state
    │   └── DynamicSizeText (Amount)
    │       • Min: 14sp, Max: 24sp
    │       • Max width: 150dp
    │       • Monospace font
    │
    └── UNPAID/PAID state
        └── DynamicSizeText (Amount)
            • Min: 14sp, Max: 24sp
            • Max width: 150dp
            • Monospace font
```

## Component Architecture

```
┌─────────────────────────────────────────────────┐
│  DynamicSizeText Component                      │
│  ─────────────────────────                      │
│                                                  │
│  ┌────────────────────────────────────────┐    │
│  │  BoxWithConstraints                    │    │
│  │  • Provides maxWidth                   │    │
│  │  • Triggers recomposition on resize    │    │
│  └────────────┬───────────────────────────┘    │
│               │                                  │
│               ▼                                  │
│  ┌────────────────────────────────────────┐    │
│  │  LocalDensity + TextMeasurer           │    │
│  │  • Convert dp to px                    │    │
│  │  • Measure text at different sizes     │    │
│  └────────────┬───────────────────────────┘    │
│               │                                  │
│               ▼                                  │
│  ┌────────────────────────────────────────┐    │
│  │  remember (Caching)                    │    │
│  │  • Keys: text, availableWidth          │    │
│  │  • Avoids recalculation                │    │
│  └────────────┬───────────────────────────┘    │
│               │                                  │
│               ▼                                  │
│  ┌────────────────────────────────────────┐    │
│  │  Binary Search Algorithm               │    │
│  │  • Find optimal font size              │    │
│  │  • O(log n) complexity                 │    │
│  └────────────┬───────────────────────────┘    │
│               │                                  │
│               ▼                                  │
│  ┌────────────────────────────────────────┐    │
│  │  Text Component                        │    │
│  │  • Renders with optimal fontSize       │    │
│  └────────────────────────────────────────┘    │
│                                                  │
└─────────────────────────────────────────────────┘
```

---

**Created:** December 15, 2025  
**For Project:** Platisa (Android App)  
**Component:** DynamicSizeText
