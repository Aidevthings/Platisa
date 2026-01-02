# 🇷🇸 PLATISA - Serbian Bill Management App

## 🎯 What is Platisa?

Platisa is a **premium Serbian bill management app** that scans, organizes, and tracks household bills (electricity, water, phone, internet, etc.) for users in Serbia.

---

## 🔴 **RULE #1: Serbian Language FIRST**

**THIS IS THE MOST IMPORTANT RULE OF THE ENTIRE PROJECT:**

Platisa is built for **Serbian users**. Every feature MUST work with:

### ✅ Serbian Latin (Latinica)
```
Račun broj
Faktura
Datum
Iznos
Ukupno
Za uplatu
```

### ✅ Serbian Cyrillic (Ћирилица)
```
Рачун број
Фактура
Датум
Износ
Укупно
За уплату
```

### ⚪ English (Optional)
```
Invoice number
Bill
Date
Amount
Total
```

**English is nice to have, but NOT the priority.**

---

## 📖 Core Documentation

| Document | Purpose | When to Read |
|----------|---------|--------------|
| **[SERBIAN_LANGUAGE_GUIDE.md](./SERBIAN_LANGUAGE_GUIDE.md)** | **📕 REQUIRED READING** - Complete guide for Serbian language support | Before writing ANY scanning/parsing code |
| [platisa_implementation_plan.md](./platisa_implementation_plan.md) | Full project roadmap and feature status | Understanding project structure |
| [FIX_SUMMARY.md](./FIX_SUMMARY.md) | Latest bug fixes and changes | After pulling new code |
| [DIAGNOSTIC_FIX_GUIDE.md](./DIAGNOSTIC_FIX_GUIDE.md) | Testing and debugging guide | When testing invoice scanning |

---

## ⚡ Quick Start for Developers

### Before You Write ANY Code That Scans Text:

1. ✅ **Read [SERBIAN_LANGUAGE_GUIDE.md](./SERBIAN_LANGUAGE_GUIDE.md)** (10 min read)
2. ✅ Check if term exists in BOTH Serbian scripts (Latin + Cyrillic)
3. ✅ Add patterns for BOTH scripts to your regex
4. ✅ Test with real bills in BOTH scripts
5. ✅ Verify in Logcat that BOTH scripts are detected

### Example: Adding a New Field Parser

❌ **WRONG - Latin Only:**
```kotlin
Pattern.compile("(?:Račun\\s+broj)[:\\s]+(\\d+)")
```

✅ **CORRECT - Both Scripts:**
```kotlin
Pattern.compile("(?:Račun\\s+broj|Рачун\\s+број)[:\\s]+(\\d+)", Pattern.CASE_INSENSITIVE)
```

---

## 🏗️ Project Architecture

```
Platisa/
├── app/src/main/java/com/example/platisa/
│   ├── core/
│   │   ├── common/          # OCR, Utils, Formatters
│   │   ├── data/            # Database, Repository
│   │   ├── domain/          # Models, Business Logic
│   │   │   └── parser/      # 🔴 CRITICAL: Text parsing (Serbian support here!)
│   │   └── worker/          # Background jobs
│   ├── di/                  # Dependency Injection
│   └── ui/                  # Screens and Components
│
├── SERBIAN_LANGUAGE_GUIDE.md    # 📕 THE MOST IMPORTANT FILE
├── PLATISA.md                    # 📘 This file (overview)
├── platisa_implementation_plan.md # 📗 Project roadmap
└── FIX_SUMMARY.md               # 📙 Latest changes
```

---

## 🔍 Key Files That MUST Support Serbian

### 🔴 Critical - Text Parsing
These files MUST have Serbian Latin + Cyrillic patterns:

| File | What It Does | Serbian Support |
|------|--------------|-----------------|
| `core/domain/parser/ReceiptParser.kt` | Extracts invoice numbers, dates, amounts | ✅ BOTH SCRIPTS REQUIRED |
| `core/domain/parser/EpsParser.kt` | Extracts electricity consumption data | ✅ BOTH SCRIPTS REQUIRED |
| `core/domain/parser/IpsParser.kt` | Extracts payment QR code data | ✅ BOTH SCRIPTS REQUIRED |

### 🟡 Important - Display
These files must DISPLAY Serbian text correctly:

| File | What It Does | Serbian Support |
|------|--------------|-----------------|
| `ui/screens/home/HomeScreen.kt` | Main bill list | ✅ UTF-8 encoding, proper fonts |
| `ui/screens/review/ReviewReceiptScreen.kt` | Bill review after scan | ✅ UTF-8 encoding, proper fonts |
| `ui/screens/billdetails/BillDetailsScreen.kt` | Detailed bill view | ✅ UTF-8 encoding, proper fonts |

### 🟢 Nice to Have - Search/Filter
These files should ACCEPT Serbian input:

| File | What It Does | Serbian Support |
|------|--------------|-----------------|
| `ui/screens/search/SearchScreen.kt` | Search bills | ✅ Both scripts in search |
| Database queries | Filter by text | ✅ Case-insensitive Serbian |

---

## 🧪 Testing Serbian Support

### ✅ Minimum Testing Requirements

Before merging ANY code that scans text, you MUST test:

1. **Latin Script Bill** (e.g., EPS electricity)
   ```
   Račun broj: 1234567890
   Za uplatu: 5.432,00 RSD
   Datum: 15.12.2025
   ```

2. **Cyrillic Script Bill** (e.g., Water utility)
   ```
   Рачун број: 9876543210
   За уплату: 2.145,50 РСД
   Датум: 15.12.2025
   ```

3. **Check Logcat**
   ```
   ✅ Found invoice number: 1234567890 (Latin pattern matched)
   ✅ Found invoice number: 9876543210 (Cyrillic pattern matched)
   ```

---

## 🚨 Common Mistakes to AVOID

### ❌ Mistake #1: Forgetting Cyrillic
```kotlin
// WRONG - Only Latin
if (text.contains("Račun broj")) { ... }

// CORRECT - Both scripts
if (text.contains("Račun broj") || text.contains("Рачун број")) { ... }
```

### ❌ Mistake #2: Case-Sensitive Matching
```kotlin
// WRONG - Won't match "RAČUN BROJ"
text.contains("Račun broj")

// CORRECT - Case insensitive
text.contains("Račun broj", ignoreCase = true)
```

### ❌ Mistake #3: Hardcoded English Terms
```kotlin
// WRONG - English only
val label = "Invoice Number"

// CORRECT - Serbian (app is in Serbian)
val label = "Račun Broj"  // Or get from strings.xml
```

---

## 📊 Current Features

### ✅ Fully Implemented
- ✅ Camera scanning with OCR (Serbian Latin + Cyrillic)
- ✅ Gmail bill import (PDF + images)
- ✅ EPS electricity consumption tracking
- ✅ Invoice number extraction (Serbian Latin + Cyrillic)
- ✅ Duplicate bill prevention with Payment ID system
- ✅ STORNO bill detection and auto-hide
- ✅ Payment status tracking (Unpaid → Processing → Paid)
- ✅ QR code generation for IPS payments
- ✅ **Payment deadline (Rok plaćanja) parsing and display**
- ✅ Cyberpunk/Neon UI design with enhanced typography
- ✅ Gmail sync with automatic bill processing

### 🚧 In Progress
- Testing invoice number extraction with more bill types

### 📅 Planned
- Water bill specific parsing
- Telekom bill specific parsing
- Budget tracking and predictions
- Export to CSV/PDF

---

## 🔧 Serbian Number & Date Formats

### Serbian Number Format
```kotlin
// Serbian uses . for thousands, , for decimals
val format = NumberFormat.getInstance(Locale("sr", "RS"))
12345.67 → "12.345,67"  // Serbian format
```

### Serbian Date Format
```kotlin
// Serbian uses dd.MM.yyyy
val format = SimpleDateFormat("dd.MM.yyyy", Locale("sr", "RS"))
Date() → "17.12.2025"  // Serbian format
```

### Currency Display
```kotlin
// Serbian uses RSD or dinara
"5.432,00 RSD"    // With currency code
"5.432,00 dinara" // With word
```

---

## 🎨 UI/UX Principles

### Design Language
- **Theme**: Cyberpunk/Neon (Dark Mode only)
- **Colors**: Deep void blue background, Neon cyan/purple/green accents
- **Effects**: Glassmorphism, glowing borders, gradient text
- **Fonts**: Default system fonts (support Cyrillic automatically)

### Serbian UI Text
- App is in **Serbian language** (Latin script by default)
- Users in Vojvodina might prefer Cyrillic - consider settings option
- Field labels use Serbian terms: "Račun broj", "Datum", "Iznos"

---

## 🚀 Development Workflow

### 1. Feature Request
```
"Add water bill consumption tracking"
```

### 2. Ask Yourself
- **Does this scan text?** → Must support Serbian Latin + Cyrillic
- **What Serbian terms are involved?** → List them in BOTH scripts
- **Have I read SERBIAN_LANGUAGE_GUIDE.md?** → If not, READ IT NOW

### 3. Implementation
```kotlin
// Example: Water consumption parser
val patterns = listOf(
    // BOTH SCRIPTS - Always!
    Pattern.compile("(?:Potrošnja vode|Потрошња воде)[:\\s]+(\\d+)", Pattern.CASE_INSENSITIVE),
    Pattern.compile("(?:Kubni metri|Кубни метри)[:\\s]+(\\d+)", Pattern.CASE_INSENSITIVE),
)
```

### 4. Testing
- Test with Latin bill
- Test with Cyrillic bill
- Check Logcat for pattern matches
- Verify UI displays correctly

### 5. Documentation
- Update `SERBIAN_LANGUAGE_GUIDE.md` with new terms
- Update `platisa_implementation_plan.md` feature status
- Add test cases to comments

---

## 📞 Support & Resources

### Serbian Language Resources
- **Latin-Cyrillic Converter**: [Use online tools to convert terms](https://www.lexilogos.com/keyboard/serbian_conversion.htm)
- **Serbian Bills**: Test with real bills from EPS, Telekom, water utilities
- **Locale**: `sr-RS` (Serbia), `sr-Latn-RS` (Latin), `sr-Cyrl-RS` (Cyrillic)

### Technical Resources
- **ML Kit**: Supports Serbian (both scripts treated as Latin-compatible)
- **Room Database**: UTF-8 by default (handles both scripts)
- **Jetpack Compose**: Full Unicode support

---

## 🎯 Success Criteria

A feature is "DONE" when:

✅ **Functional:**
- [ ] Works with Serbian Latin bills
- [ ] Works with Serbian Cyrillic bills
- [ ] Works with mixed-script bills
- [ ] English support (optional bonus)

✅ **Tested:**
- [ ] Unit tests with Serbian Latin examples
- [ ] Unit tests with Serbian Cyrillic examples
- [ ] Manual test with 3+ real bills (both scripts)
- [ ] Logcat shows correct pattern matches

✅ **Documented:**
- [ ] Terms added to `SERBIAN_LANGUAGE_GUIDE.md`
- [ ] Feature status updated in `platisa_implementation_plan.md`
- [ ] Code comments explain Serbian patterns used

---

## 🏁 Getting Started Checklist

### For New Developers:

- [ ] Read this file (PLATISA.md) - **5 minutes**
- [ ] Read [SERBIAN_LANGUAGE_GUIDE.md](./SERBIAN_LANGUAGE_GUIDE.md) - **10 minutes**
- [ ] Scan the [platisa_implementation_plan.md](./platisa_implementation_plan.md) - **15 minutes**
- [ ] Install app and scan a test bill (Latin) - **5 minutes**
- [ ] Install app and scan a test bill (Cyrillic) - **5 minutes**
- [ ] Check Logcat to see pattern matching in action - **5 minutes**

**Total: 45 minutes to understand the core principle** 🎓

---

## 💡 Remember

**When in doubt, ask:**
1. "Does this work with Cyrillic?"
2. "Have I tested with both scripts?"
3. "Have I read SERBIAN_LANGUAGE_GUIDE.md?"

---

## 📝 Version History

| Date | Change | Impact |
|------|--------|--------|
| 2025-12-18 | **Payment Deadline Feature** | ✅ Added dueDate parsing (Cyrillic + Latin) |
| 2025-12-18 | Database migration v7 → v8 | ✅ Added dueDate field to receipts table |
| 2025-12-18 | UI enhancements | ✅ Larger fonts (15sp), Help icon redesign |
| 2025-12-18 | HomeScreen & BillDetails updates | ✅ Due date display on cards and details page |
| 2025-12-17 | Added full Cyrillic support to invoice parser | ✅ Fixed duplicate detection |
| 2025-12-17 | Created SERBIAN_LANGUAGE_GUIDE.md | 📕 Core reference document |
| 2025-12-17 | Established Serbian-first development principle | 🎯 Clear priority |

---

**🇷🇸 Platisa - Built for Serbia, Built for Serbians** 🇷🇸
