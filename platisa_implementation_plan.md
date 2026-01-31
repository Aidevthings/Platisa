# 📋 Platisa Ultimate Implementation Plan

## 🇷🇸 CORE PRINCIPLE: Serbian Language First

**Platisa is built for Serbian users.** All features MUST support:
1. ✅ **Serbian Latin** (Latinica: Račun, Faktura, Datum)
2. ✅ **Serbian Cyrillic** (Ћирилица: Рачун, Фактура, Датум)
3. ⚪ English (Nice to have, but NOT priority)

**📖 See: [SERBIAN_LANGUAGE_GUIDE.md](./SERBIAN_LANGUAGE_GUIDE.md) for detailed implementation rules**

**Key Rules:**
- Every OCR/parsing pattern MUST include both Serbian scripts
- Test with real bills in both Latin and Cyrillic
- Never assume one script over the other
- Serbian date format: dd.MM.yyyy
- Serbian number format: 12.345,67 (dot for thousands, comma for decimals)

---

## 🎯 Goal
Build a **production-grade, premium** receipt management system.
**Philosophy**: "Deep Engineering" - Robustness, Security, Performance, and User Experience are paramount. No shortcuts.

---

## 📊 Current State (Updated: 2025-12-23)

### ✅ Completed & Verified
- **Core Architecture**: Hilt dependency injection, MVVM with Clean Architecture (Data/Domain/Presentation layers)
- **Database**: Room database with Receipt, EpsData entities, TypeConverters for Date/BigDecimal
  - **Version 8** (Latest): Added `dueDate` field for payment deadline tracking
  - Migration path: v7 → v8 with proper field addition
- **Gmail Integration**: OAuth2 authentication, WorkManager background sync, PDF attachment processing
- **Receipt Parsing**: 
  - Multi-stage OCR pipeline (ML Kit + PDF text extraction with intelligent fallback)
  - QR code scanning for IPS and EPS receipts
  - Robust regex parsing for amounts and consumption data
  - **Payment deadline (Rok plaćanja) extraction** - Full Serbian Latin + Cyrillic support
  - Deduplication logic using Payment ID system
  - STORNO bill detection and automatic hiding
- **EPS Analytics**: Full consumption tracking (VT/NT), monthly aggregation, charting with Canvas
- **UI/UX**: **Cyberpunk/Neon aesthetic** fully implemented
  - Dark mode with Deep Void Blue backgrounds
  - Electric Cyan/Neon Purple color scheme
  - Custom `neonGlow()` and `glassBackground()` modifiers
  - Updated screens: `HomeScreen`, `EpsAnalyticsScreen`, `BillDetailsScreen`
  - Updated components: `PlatisaCard`, `PlatisaButton`
  - **Enhanced typography**: Larger fonts (15sp for primary labels)
  - **Payment deadline display**: Cyan colored, bold text on bill cards and detail pages
  - Help icon: Neon green (26dp) with simplified design
- **Data Export**: CSV and PDF export functionality
- **Search & Filters**: Full implementation with date/amount/section filtering

### 🚀 Recent Achievements (2025-11-23)

#### 🎨 Cyberpunk UI Redesign
- **Technique**: Implemented a custom "Neon" design system using Jetpack Compose modifiers.
- **Details**:
    - Replaced standard Material Design components with custom-built **NeonCards** featuring glowing borders and glassmorphism backgrounds.
    - Implemented a **Dashboard Layout** for the Home Screen, moving away from a simple list view.
    - Created a **"Total Spending" Neon Bar** with gradient borders and glow effects for high visual impact.
    - Applied **Color-Coded Status Indicators** (Orange/Yellow/Green) to receipt cards to visually represent payment status (Unpaid/Processing/Paid).
    - **Optimization**: Used `DrawModifier` for efficient rendering of glow effects without heavy bitmap shadows.

#### 🔄 Smart Sync Optimization
- **Technique**: Hybrid scheduling using Android `WorkManager`.
- **Details**:
    - **Immediate Sync**: Configured a `OneTimeWorkRequest` to trigger instantly upon application startup, ensuring fresh data is always available.
    - **Daily Background Sync**: Implemented a `PeriodicWorkRequest` scheduled specifically for **14:00 (Serbian Time)**. This timing was chosen to align with typical bill delivery windows while minimizing battery impact.
    - **Efficiency**: The system intelligently calculates the initial delay to hit the 14:00 window precisely, regardless of when the app was last opened.

#### 📄 Advanced PDF & QR Handling
- **Technique**: Native PDF Rendering + Image Binarization.
- **Details**:
    - Solved the issue of blank PDF previews by switching from third-party libraries to Android's native `PdfRenderer`.
    - Implemented a custom **Binarization Filter** (Thresholding) to convert grayscale PDF renders into pure Black & White bitmaps. This significantly improved QR code detection rates for EPS bills.
    - **Hybrid Scanning**: The system now attempts to scan the raw image first, and falls back to the binarized high-contrast version if the initial scan fails.

### 🎯 Latest Refinements (2025-12-23)

#### 📊 Advanced Statistics & Visual Enhancements
- **Spending Trends Graph Overhaul**:
  - **Dynamic Y-Axis**: Implemented a smart 5-line scaling system (2k, 5k, 10k steps) ensuring optimal data visualization regardless of spending range.
  - **'k' Formatting**: Clean integer display (e.g., "10k" vs "10000") for better readability.
  - **Visual Polish**: Added "Glass/3D/Polished Metal" effects to graph bars using complex gradients, edge lighting, and metallic shine overlays.
- **Tariff Usage Visualization**:
  - Added specific circular progress cards for **Niža Tarifa** (Low Tariff) and **Viša Tarifa** (High Tariff).
  - Integrated real consumption data from `EpsData` entity, correctly filtered by receipt dates.
  - Consistent Neon aesthetics (Neon Cyan / Neon Magenta).

#### 🔄 Sync Robustness & User Experience
- **Duplicate Handling Strategy**:
  - **Problem**: Duplicate bills (same invoice number) were triggering "Error" notifications during sync.
  - **Solution**: Refined `SyncReceiptsUseCase` to identify and silently skip duplicates without flagging them as errors.
  - **Result**: "Greška: Duplikat" messages removed; Sync status now correctly reflects "Success" even if bills were skipped.
- **Graph Visibility Fix**:
  - Fixed an issue where tariff graphs were empty due to missing dates in `EpsData`.
  - Implemented a repository-level join to fetch dates from the parent `Receipt` entity.

### 🎯 Latest Refinements (2025-12-20)

#### 📸 Camera Zoom Fix for QR Scanning
- **Problem**: Camera couldn't scan QR codes from receipts - image too zoomed out
- **Root Cause**: Default zoom level (0x) made QR codes too small in frame for ML Kit detection
- **Solution**: Set fixed auto-zoom to 0.5f (~3x) for optimal QR scanning
  - Removed manual zoom controls (slider, ZoomIn/ZoomOut buttons)
  - Camera now starts pre-zoomed for reliable QR detection
  - No user interaction needed - just point and scan
- **File Modified**: `CameraScreen.kt`
  - `setLinearZoom(0.5f)` on camera bind
  - Removed ~50 lines of zoom UI code

#### 🧾 Fiscal Receipt Camera Flow System
- **Problem**: Fiscal receipts scanned by camera weren't being saved or categorized
- **Solution**: Complete fiscal receipt flow implementation
  - **CameraViewModel.kt**: Added `saveFiscalReceipt(fiscalUrl: String)` function
    - Scrapes fiscal data from government website using `FiscalScraper`
    - Creates Receipt with `PaymentStatus.PAID` (store receipts are always paid)
    - Saves receipt + items to database
    - Sets `originalSource = "CAMERA_FISCAL"` for filtering
  - **CameraScreen.kt**: Updated fiscal QR flow
    - Calls `viewModel.saveFiscalReceipt()` before navigation
    - Shows loading message: "Fiskalni račun prepoznat! Učitavam..."
    - Shows success/error snackbar
    - Navigates to Poređenje screen after save
  - **ComparisonViewModel.kt**: Added `fiscalReceipts` StateFlow
    - Filters receipts by `originalSource == "CAMERA_FISCAL"`
    - Exposes as `StateFlow<List<Receipt>>` for UI
  - **ComparisonScreen.kt**: New "Moji Računi" section
    - Shows saved fiscal receipts at top of screen
    - `FiscalReceiptCard` composable with merchant name, date, amount
    - Clickable cards navigate to `BillDetailsScreen`
    - Below receipts: existing product search functionality
    - Empty state when no receipts saved

#### 🔍 QR Code Extractor Multi-Strategy (Gallery Images)
- **Problem**: ML Kit failed on large images (>1024px bug)
- **Solution**: Multi-strategy extraction pipeline
  - Step 0: Try multiple resize dimensions (256, 400, 512, 800px)
  - Step 1: ZXing on original image
  - Step 3: ML Kit with all rotations (0°, 90°, 180°, 270°)
  - Step 3b: Contrast enhancement + ML Kit
  - Step 4: Multiple binarization thresholds (100, 128, 150, 180, 200)
  - Step 5: Inverted binarization for reversed QR codes
- **Files Modified**: `QrCodeExtractor.kt`
  - Added `applyBinarizationWithThreshold(bitmap, threshold)`
  - Added `applyInvertedBinarization(bitmap, threshold)`
  - Comprehensive logging at each step

### 🎯 Latest Refinements (2025-12-18)

#### 📅 Payment Deadline (Rok plaćanja) Feature - COMPLETE
- **Problem**: Users couldn't see when bills were due, making it hard to prioritize payments
- **Solution**: Full payment deadline extraction, storage, and display system
  - **Parser Implementation** (`EpsParser.kt`):
    - Created `extractDueDate()` method with comprehensive Serbian pattern support
    - **Serbian Cyrillic patterns**: Рок за плаћање, Рок.*?пла[ћч]ањањ[еa]
    - **Serbian Latin patterns**: Rok za plaćanje, Rok za placanje, Rok.*?pla[cć]anje
    - **Alternative format**: Datum plaćanja (Some utilities use this)
    - Date format: dd.MM.yyyy (Serbian standard)
    - Uses SimpleDateFormat with error handling
  - **Database Migration** (v7 → v8):
    - Added `dueDate INTEGER` column to receipts table
    - Migration script: `ALTER TABLE receipts ADD COLUMN dueDate INTEGER`
    - Added to `DatabaseModule.kt` migrations list
    - Nullable field to support bills without deadlines
  - **Domain Models** (`EpsData.kt`, `Receipt.kt`, `ReceiptEntity.kt`):
    - Added `val dueDate: Date? = null` to all three models
    - Properly mapped in `Mappers.kt` (both toDomain and toEntity)
    - Added to `EpsData` mapping (even though not in entity, for consistency)
  - **Use Case Integration** (`SyncReceiptsUseCase.kt`):
    - Receipt creation now includes `dueDate = epsData.dueDate`
    - Automatic extraction during Gmail sync
    - Works seamlessly with existing bill processing flow
  - **UI Display - HomeScreen**:
    ```kotlin
    Column {
        Text(merchantName)   // EPS Distribucija
        Text(billDate)       // 16 Dec 2024
        Text("Rok: $dueDate") // Rok: 25 Dec 2024 (cyan, bold)
    }
    ```
    - Only shows for UNPAID and PROCESSING bills (hides for PAID)
    - Cyan color (#00EAFF) to match neon theme
    - Bold font weight for visibility
    - 13sp font size for balanced hierarchy
  - **UI Display - BillDetailsScreen**:
    - Two separate fields:
      - **DATUM RAČUNA**: Bill issue date (cyan icon)
      - **ROK PLAĆANJA**: Payment deadline (magenta icon)
    - Uses `Icons.Default.Event` for deadline (different from `CalendarMonth`)
    - Format: "dd. MMMM yyyy" (e.g., "25. decembar 2024")
    - Serbian locale (`sr-RS`) for month names
    - Only displays when dueDate exists
- **Layout Optimization**:
  - Bill cards remain **same height** despite new info
  - kWh consumption moved from left to right (under amount)
  - Due date placed under bill date (left side)
  - Balanced 2-column layout preserved
- **Typography Enhancements**:
  - "Ukupno za plaćanje": 12sp → 15sp, Bold
  - "Slikaj Kamerom": 12sp → 15sp, Bold
  - Bill date: 14sp → 13sp (slight reduction for balance)
  - Help icon: 22dp → 26dp, color: White → Neon Green
  - Help icon simplified: Removed inner glass shine overlay
- **Result**:
  - ✅ Users can now see payment deadlines at a glance
  - ✅ Works with both Serbian scripts (Cyrillic + Latin)
  - ✅ Foundation ready for notification system (7/3/1 days before due)
  - ✅ Clean, informative UI without visual clutter
  - ✅ Automatic extraction during Gmail sync

### 🎯 Latest Refinements (2025-12-17)

#### 🔒 CRITICAL FIX: Serbian Cyrillic Invoice Number Support
- **Problem**: Duplicate bill detection was failing, causing:
  1. Missing bills - All bills hidden because marked as "duplicates" when they weren't
  2. False duplicates - Different bills marked as same because parser extracted wrong 8-digit codes
  3. Cyrillic bills ignored - Parser only looked for Latin "Račun broj", missed Cyrillic "Рачун број"
- **Root Cause Analysis**:
  - ❌ Missing Cyrillic patterns - No support for Рачун број, Број рачуна, Позив на број
  - ❌ 8-digit minimum too short - Extracted company registration codes instead of invoice numbers
  - ❌ Aggressive blocking - No way to override when duplicate detected
- **Solution Implemented** (`ReceiptParser.kt`):
  - ✅ **Full Cyrillic Support**: Added ALL Serbian Cyrillic patterns
    - Рачун број / Број рачуна (Invoice number)
    - Фактура / Број фактуре (Invoice/bill)
    - Позив на број (Reference number - common on utilities)
    - Број документа (Document number)
    - Рач. бр / Бр. рачуна / Факт. бр (Abbreviated forms)
  - ✅ **Stricter Length**: Changed minimum from 8 → 10 digits to avoid false positives
  - ✅ **Better Logging**: Shows extracted number + which pattern matched
  - ⚠️ **Temporary Disable**: Duplicate prevention disabled during testing phase
- **Testing Phase** (2025-12-17):
  - Duplicate check temporarily disabled to verify invoice extraction works
  - Added comprehensive logging to track: extraction → save → display flow
  - Will re-enable with smarter logic after confirming all bills save/display correctly
- **Expected Re-enablement Logic**:
  ```kotlin
  // Future smart duplicate handling
  if (invoiceNumber matches && length >= 10) {
    // Show warning dialog
    // "This invoice already exists. Save anyway?"
    // [Cancel] [Save Anyway]
  }
  ```
- **Result**: 
  - ✅ Serbian Cyrillic bills now detected correctly
  - ✅ False positive duplicates eliminated
  - ✅ All bills save and display properly
  - 🔧 Smarter duplicate prevention coming after testing
- **See**: `SERBIAN_LANGUAGE_GUIDE.md` - Now the reference for all text scanning features

#### 💳 Bill Details UI & QR Code Enhancements
- **QR Code Display**: Implemented actual QR code generation and display from bill payment data
  - Removed decorative animations (rotating borders, pulsing glows, scanning lines)
  - Using `QrCodeGenerator` to create scannable QR codes from IPS/EPS payment strings
  - Clean white background for optimal scanning
  - Fallback text when QR data unavailable
- **UI Cleanup**: Removed 10+ decorative elements from Bill Details screen
  - Eliminated large radial gradient background (384dp)
  - Removed 7 small decorative circles behind section titles
  - Removed blur glow effect around back button
  - ~145 lines of code removed for cleaner, faster rendering
- **QR Code Section Optimization**:
  - Moved QR code closer to top (reduced spacing from 4dp to 0dp)
  - Enlarged "Save QR Code" button (64dp → 72dp height, wider with reduced padding)
  - Increased button text size (18sp → 20sp) with letter spacing
- **Smart Navigation After Save**: 
  - After saving QR code to gallery, app automatically returns to HomeScreen
  - HomeScreen auto-scrolls to show the bill whose QR was saved
  - Uses `savedStateHandle` to pass receipt ID between screens
  - Smooth animated scroll to bill position in list

#### 🎮 UI Navigation & Interaction Improvements  
- **Help Icon Added**: New green help icon in header next to notifications and theme toggle
  - Placeholder for future help/guide implementation
  - Matches glassmorphic style of other header icons
  - Uses `Icons.Default.Help` with neon green accent color
- **Fixed Bill Card Click Issues**: 
  - **Root Cause**: Snap-to-top scroll behavior was consuming touch events on bottom bills
  - **Solution**: Completely disabled snap-to-top feature to eliminate interference
  - Single tap now works reliably on all bills (top, middle, bottom)
  - Normal scrolling still works perfectly
- **"Confirm Payment" Button**: Added to PROCESSING status bills
  - Green button with check icon appears on purple/magenta bills  
  - Button text: "POTVRDI PLAĆANJE" (16sp font, bold)
  - Clicking marks bill as PAID and records payment date
- **Payment Date Tracking**: 
  - Added `paymentDate` field to Receipt domain model
  - PAID bills now show "Plaćeno: [actual payment date]" instead of bill date
  - Uses `updatedAt` database field to avoid schema migration
  - Payment date set when user confirms payment

#### 🎨 Visual Polish & Typography
- **Header Text Consistency**: "Ukupno za plaćanje" now uses white color matching "Slikaj Kamerom"
- **Color-Coded Bill Status**:
  - **UNPAID**: Cyan background - shows original bill date
  - **PROCESSING**: Purple/Magenta background - shows bill date + "POTVRDI PLAĆANJE" button  
  - **PAID**: Green background - shows "Plaćeno: [payment date]"
- **Bill Details Font Increases** (across the board for better readability):
  - Field labels: 12sp → 14sp
  - Amount (large): 24sp → 28sp
  - Regular field values: 18sp → 20sp
  - Multiline text: 14sp → 16sp, line height: 20sp → 22sp
  - Total consumption: 18sp → 22sp
  - "Raspodela potrošnje": 14sp → 16sp
  - Bar value text: 12sp → 15sp
  - Bar labels: 12sp → 14sp
  - Percentage text: 10sp → 12sp (now bold and colored)
- **Icon Size Increases**:
  - Large icons: 32dp → 36dp
  - Regular icons: 24dp → 28dp
  - All icons more visible and prominent
- **Consumption Bars - Glossy 3D Glass Effects**:
  - **Top Glass Shine**: White gradient covering top 35% (light reflection)
  - **Left Edge Gloss**: Vertical highlight on left side (15% width) for depth
  - **Bottom Inner Glow**: Dark gradient at bottom intensifying the base color
  - **Visible Border**: 2dp gradient border (White → color) for clear definition
  - **Enhanced Shadow**: 15dp elevation with 60% opacity for pronounced depth
  - **Rounded Corners**: Increased from 8dp → 12dp for smoother appearance
  - **Taller Bars**: Height increased from 128dp → 160dp for better visibility
  - **Percentage Styling**: Now bold and uses the bar's accent color (not gray)
  - **Wider Spacing**: Bar gap increased from 8dp → 16dp
  - Premium glossy glass look with proper 3D depth

#### 🔒 Critical Bug Fix - Duplicate Bill Prevention
- **Problem Identified**: Duplicate bills when electricity company sends multiple emails with same bill
  - Example: October 5th electricity bill appeared twice from two separate emails
  - Root cause: No unique identifier extraction for deduplication
- **Solution Implemented - Universal Invoice Number System**:
  - **Invoice Number Extraction** (`ReceiptParser.kt`):
    - Extracts unique bill numbers from ALL bill types (not just electricity)
    - **Serbian patterns**: Račun broj, Poziv na broj, Broj fakture, Broj dokumenta
    - **English patterns**: Invoice number, Bill number, Reference number, Document number
    - **Abbreviated forms**: Rač. br, Br. računa, Fakt. br
    - **Generic patterns**: ID numbers, long numbers after keywords, standalone 12+ digit numbers
    - Minimum 8 digits (down from 10) to catch more bill formats
  - **Database Schema** (`ReceiptEntity.kt`):
    - Added `invoiceNumber` field with UNIQUE INDEX constraint
    - Database automatically rejects duplicate invoice numbers
    - Version bumped to 5 (will trigger reset due to `.fallbackToDestructiveMigration()`)
  - **Deduplication Logic** (`SyncReceiptsUseCase.kt`):
    - Checks `getReceiptByInvoiceNumber()` before inserting
    - If duplicate found → Skip with log message
    - Also catches `SQLiteConstraintException` as backup safety
  - **Repository Layer**:
    - Added `getReceiptByInvoiceNumber()` method to DAO, Repository interface, and implementation
    - Proper query: `SELECT * FROM receipts WHERE invoiceNumber = :invoiceNumber LIMIT 1`
  - **UI Display** (`BillDetailsScreen.kt`):
    - Shows invoice number as "BROJ RAČUNA" field
    - Positioned between Amount and Due Date
    - Only displays when invoice number was successfully extracted
    - Uses Tag icon in neon cyan color
- **Works For All Bill Types**:
  - ⚡ Electricity (EPS) - "Račun broj: 123456789012"
  - 💧 Water - "Poziv na broj: 987654321"
  - 📱 Phone (Telekom/Telenor/Yettel) - "Broj fakture: 123456789"
  - 🌐 Internet (SBB/Supernova) - "Invoice number: 456789123"
  - 🏢 Other utilities - "Broj dokumenta: 789123456"
- **Result**: No more duplicate bills, regardless of how many times company sends the same bill via email!

### 🎯 Latest Refinements (2025-12-12)

#### 💎 Home Screen Visual & UX Polish
- **Currency Formatting**: Removed "RSD" suffix from all bill cards for cleaner, more minimalist presentation
  - Modified `Formatters.kt` to use `formatCurrency()` instead of `formatCurrencyWithSuffix()`
  - Applied consistently across `HomeScreen` and bill card components
- **Enhanced Category Icons**: Implemented pronounced 3D glass effects on bill card category icons
  - **Multi-layered shadows**: Combined elevation shadows (20dp spot color + 12dp ambient) for depth
  - **Thick visible borders**: 3dp gradient borders (White → main color) for maximum visibility
  - **Glass shine overlay**: Top-to-bottom white gradient for realistic glass reflection
  - **Inner glow**: Radial gradient accent at bottom-right corner for ambient lighting
- **Smart Scroll Behavior**: Implemented snap-to-item scrolling for recent bills list
  - **50% visibility threshold**: Bills snap fully into view if >50% visible, otherwise scroll out completely
  - **Top-edge snapping**: Prevents partial bill display at the top of the list
  - **Smooth animations**: Uses `animateScrollToItem()` for polished user experience

### 🚧 In Progress
- None currently

### 🔴 Blockers
- Unit tests failing due to Gradle configuration issue (non-critical, doesn't affect app functionality)

# Platisa Implementation Plan

## Goal Description
Overhaul the Platisa application with a "Cyberpunk/Infographic" aesthetic based on the "HUD" reference image. The goal is to transform the app from a simple list-based UI to a futuristic data dashboard.

## User Review Required
> [!IMPORTANT]
> This is a complete visual rewrite. Previous "Material Design" concepts will be replaced by custom "Neon" components.

## Proposed Changes

### Design System (New)
#### [MODIFY] [Color.kt](file:///a:/Software Dev/Platisa/app/src/main/java/com/example/platisa/ui/theme/Color.kt)
- Implement the "Void" and "Neon" palettes.
- Define Brush gradients for UI elements.

#### [NEW] [NeonCard.kt](file:///a:/Software Dev/Platisa/app/src/main/java/com/example/platisa/ui/components/NeonCard.kt)
- Base container with dark glass background and glowing border.

#### [NEW] [InfographicComponents.kt](file:///a:/Software Dev/Platisa/app/src/main/java/com/example/platisa/ui/components/InfographicComponents.kt)
- `GradientBar`: For bar charts.
- `TimelineNode`: For receipt history.
- `StatBadge`: For key metrics (Total, Month, etc.).

### Screens
#### [MODIFY] [HomeScreen.kt](file:///a:/Software Dev/Platisa/app/src/main/java/com/example/platisa/ui/screens/home/HomeScreen.kt)
- Convert to a Dashboard layout.
- Top section: "Total Balance" & "Monthly Spending" as infographic widgets.
- Middle section: "Recent Activity" as a vertical timeline.
- Bottom section: Quick Actions (Scan, Add) as floating neon buttons.

#### [MODIFY] [EpsAnalyticsScreen.kt](file:///a:/Software Dev/Platisa/app/src/main/java/com/example/platisa/ui/screens/analytics/EpsAnalyticsScreen.kt)
- Refine existing gauges to match the new "Donut" style.
- Add "Consumption History" bar chart.

## Verification Plan
### Manual Verification
- Visual check against the reference image.
- Verify "Glow" effects do not impact performance (target 60fps).

### Phase 2: Security & Privacy 🔒
- **Biometric Auth**: `BiometricPrompt` integration to lock the app (optional setting).
- **Secure Storage**: `EncryptedSharedPreferences` for storing OAuth tokens and sensitive flags.
- **Data Privacy**: Ensure no data leaves the device except for Cloud Vision/Gmail API calls (explicit user consent).

### Phase 3: Data Layer & Persistence 💾
- **Room Database**:
    - `Receipt` Entity (Complex): Includes `metadata` (JSON), `syncStatus`, `originalSource`.
    - `Section` Entity: Fully customizable.
    - `Tag` Entity: Many-to-many relationship for flexible categorization.
- **TypeConverters**: For `Date`, `BigDecimal` (money), and `List<String>`.
- **Repository Pattern**: Single source of truth, mediating between Database and Network.

### Phase 4: Design System & UI Components 🎨
- **Theming**: **Neon/Cyberpunk Aesthetic** (Dark Mode default).
    - **Colors**: Deep Black/Navy backgrounds, Neon Cyan/Pink/Purple accents.
    - **Style**: Glassmorphism, Glowing borders, Gradient text.
- **Component Library**:
    - `PlatisaCard`: Translucent dark cards with neon borders.
    - `PlatisaButton`: Gradient backgrounds with glow effects.
    - `PlatisaInput`: Minimalist with neon focus states.
- **Dark Mode**: The app is Dark Mode *only* (or primary).
- **Animations**: Shared element transitions, glowing pulse effects.

### Phase 5: Navigation & User Experience 🧭
- **Navigation**: Jetpack Navigation Compose.
- **Structure**:
    - **Home**: Tabbed view (Sections).
    - **Analytics**: Global stats.
    - **Settings**: App configuration.
- **Onboarding**: Intro screens explaining OCR and Gmail features.
- **Snackbar/Toast Manager**: Centralized UI feedback system.

### Phase 6: Advanced Camera & Image Engine 📸
- **CameraX**:
    - Tap-to-focus.
    - Flash control.
    - **Document Scanner Overlay**: Visual guide for alignment.
- **Image Pipeline**:
    - **Cropping**: Ability to crop image after capture.
    - **Compression**: Optimize storage (WebP format).
    - **Caching**: Coil image loader configuration.

### Phase 7: Intelligence Layer (The "Brain") 🧠
- **Hybrid OCR Strategy**:
    1.  **Fast**: ML Kit (On-device) for instant feedback.
    2.  **Deep**: Cloud Vision (Cloud) for difficult/handwritten bills.
- **Regex Engine**: Configurable regex patterns for different vendors (EPS, Telekom, Maxi, Lidl).
- **Auto-Tagging**: Keyword matching to automatically assign tags/sections.

### Phase 8: EPS Specialist Module ⚡
- **Deep Parsing**: Extract specific fields:
    - `obracunski_period`
    - `potrosnja_vt` / `potrosnja_nt`
    - `ed_broj`
- **Analytics**:
    - **Consumption Chart**: Custom Canvas drawing (Bezier curves) for smooth graphs.
    - **Cost Prediction**: Estimate next month's bill based on average.
- **Alerts**: "High consumption" warning if 20% > average.

### Phase 9: Gmail Automation & Sync 📧
- **OAuth2 Manager**: Robust token refresh logic.
- **Background Sync**: `WorkManager` job to check emails every 6/12/24 hours.
- **PDF Engine**:
    - High-res rendering for OCR.
    - Text extraction (if PDF is text-based) for 100% accuracy.
- **Smart Filter**: Only look for emails with keywords "Račun", "Faktura", "Izvod".

### Phase 10: Data Management & Export 📤
- **Search**: Full-text search (FTS4) in database.
- **Filters**: Date range, Amount range, Section, Tags.
- **Export**:
    - **CSV**: For Excel/Sheets.
    - **PDF Report**: Generate a monthly spending report.
    - **ZIP**: Export all receipt images.

### Phase 11: Settings & Personalization ⚙️
- **Preferences**:
    - Default Currency (RSD/EUR).
    - Notification settings.
    - Theme selector (System/Light/Dark).
- **Backup/Restore**:
    - Local Backup (Zip file).
    - Google Drive Backup (using Drive API).

### Phase 12: Quality Assurance & Polish ✨
- **Unit Tests**: Testing ViewModels and Parsers.
- **UI Tests**: Automated navigation tests.
- **Performance Profiling**: Memory leak checks (LeakCanary).
- **Accessibility**: TalkBack support for all elements.

## ✅ Verification Plan
- **Unit**: Run `./gradlew test`
- **Lint**: Run `./gradlew lint`
- **Manual**: Full regression test of all 12 phases.

### 🛍️ Product Search & Comparison (New)
- **Goal**: Enable searching for products across all fiscal receipts and comparing prices.
- **Problem**: Current search is broken and doesn't display result details needed for comparison.
- **Changes**:
  - **Domain**: New ProductSearchResult model (includes Merchant Name & Address).
  - **Repository**: Update searchItems to return ProductSearchResult via ItemWithContext mapping.
  - **UI**: Display Store Name + Address and Unit Price in a clean table when searching.

