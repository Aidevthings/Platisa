# 🇷🇸 PLATISA - SERBIAN LANGUAGE DEVELOPMENT GUIDE

## 🎯 CORE PRINCIPLE

**Platisa is a SERBIAN app first and foremost.**

All scanning, parsing, and text recognition features **MUST** support:
1. **Serbian Latin** (Račun, Faktura, Datum)
2. **Serbian Cyrillic** (Рачун, Фактура, Датум)

**English support is SECONDARY** - it's nice to have but NOT the priority.

---

## 📋 Serbian Language Requirements

### ✅ MANDATORY Support
Every OCR/parsing function MUST handle:

#### Latin Script (Latinica)
```
Račun broj, Broj računa
Faktura, Broj fakture
Poziv na broj
Za uplatu, Ukupno
Datum, Iznos
```

#### Cyrillic Script (Ћирилица)
```
Рачун број, Број рачуна
Фактура, Број фактуре
Позив на број
За уплату, Укупно
Датум, Износ
```

### ✅ BOTH Scripts Required
**Never** assume bills are only in Latin or only in Cyrillic. Serbian bills use:
- **Latin** - Common on modern digital bills (EPS, Telekom)
- **Cyrillic** - Common on official/government bills (Utilities, taxes)
- **Mixed** - Some bills mix both scripts in different sections

---

## 🔍 Pattern Recognition Rules

### Rule 1: Always Provide BOTH Scripts
When creating regex patterns for Serbian text:

❌ **WRONG:**
```kotlin
Pattern.compile("(?:Račun\\s+broj)[:\\s]+(\\d+)")
```

✅ **CORRECT:**
```kotlin
Pattern.compile("(?:Račun\\s+broj|Рачун\\s+број)[:\\s]+(\\d+)")
```

### Rule 2: Test With Real Serbian Bills
Before marking a feature as "done", test with:
- ⚡ EPS bill (electricity) - Usually **Latin**
- 💧 Water utility bill - Often **Cyrillic**
- 📱 Telekom bill - Usually **Latin**
- 🏛️ Government tax bill - Usually **Cyrillic**

### Rule 3: Serbian Characters Matter
Don't forget special characters:

**Latin:**
```
č, ć, š, ž, đ
Č, Ć, Š, Ž, Đ
```

**Cyrillic:**
```
а, б, в, г, д, е, ж, з, и, ј, к, л, м, н, о, п, р, с, т, у, ф, х, ц, ч, џ, ш
А, Б, В, Г, Д, Е, Ж, З, И, Ј, К, Л, М, Н, О, П, Р, С, Т, У, Ф, Х, Ц, Ч, Џ, Ш
```

---

## 📝 Common Serbian Terms by Category

### Invoice/Bill Terms
| English | Latin | Cyrillic |
|---------|-------|----------|
| Invoice number | Račun broj, Broj računa | Рачун број, Број рачуна |
| Invoice | Faktura, Račun | Фактура, Рачун |
| Document number | Broj dokumenta | Број документа |
| Reference number | Poziv na broj | Позив на број |
| Date | Datum | Датум |
| Amount | Iznos | Износ |
| Total | Ukupno | Укупно |
| For payment | Za uplatu | За уплату |
| Due date | Rok plaćanja | Рок плаћања |

### Utility Terms
| English | Latin | Cyrillic |
|---------|-------|----------|
| Electricity | Struja, Elektro | Струја, Електро |
| Water | Voda | Вода |
| Gas | Gas | Гас |
| Heating | Grejanje | Грејање |
| Consumption | Potrošnja | Потрошња |
| Higher tariff | Viša tarifa | Виша тарифа |
| Lower tariff | Niža tarifa | Нижа тарифа |

### Telekom Terms
| English | Latin | Cyrillic |
|---------|-------|----------|
| Phone | Telefon | Телефон |
| Internet | Internet | Интернет |
| Account number | Broj naloga | Број налога |
| Minutes | Minuti | Минути |
| Data | Podaci | Подаци |

---

## 🛠️ Implementation Checklist

When building any new feature that scans or parses text:

### ✅ Before Writing Code
- [ ] List all Serbian terms (Latin + Cyrillic) you need to detect
- [ ] Create test data with BOTH scripts
- [ ] Check if existing utility functions handle both scripts

### ✅ While Writing Code
- [ ] Every regex pattern includes BOTH scripts
- [ ] String matching is case-insensitive
- [ ] Special characters (č, ć, š, ž, đ) handled correctly
- [ ] No hardcoded Latin-only strings

### ✅ After Writing Code
- [ ] Test with real Serbian bills (Latin)
- [ ] Test with real Serbian bills (Cyrillic)
- [ ] Test with mixed-script bills
- [ ] Check Logcat output shows correct matches
- [ ] User can see extracted data in UI

---

## 🧪 Testing Data

### Test Strings - Latin
```kotlin
val latinTests = listOf(
    "Račun broj: 1234567890",
    "Broj računa: 9876543210",
    "Poziv na broj: 1122334455",
    "Za uplatu: 5.432,00 dinara",
    "Ukupno: 12.345,67 RSD",
    "Datum: 15.12.2025"
)
```

### Test Strings - Cyrillic
```kotlin
val cyrillicTests = listOf(
    "Рачун број: 1234567890",
    "Број рачуна: 9876543210",
    "Позив на број: 1122334455",
    "За уплату: 5.432,00 динара",
    "Укупно: 12.345,67 РСД",
    "Датум: 15.12.2025"
)
```

### Test Real Bills
Keep test bills in `/docs/test-bills/`:
```
/docs/test-bills/
  ├─ eps-latin.jpg          # EPS electricity (Latin)
  ├─ eps-cyrillic.jpg       # EPS electricity (Cyrillic)
  ├─ water-cyrillic.jpg     # Water utility (Cyrillic)
  ├─ telekom-latin.jpg      # Telekom (Latin)
  └─ mixed-script.jpg       # Mixed Latin/Cyrillic
```

---

## 📚 Key Files to Maintain Serbian Support

### Core Parsing Files
```
/core/domain/parser/
  ├─ ReceiptParser.kt       # MUST support both scripts
  ├─ EpsParser.kt           # MUST support both scripts
  └─ IpsParser.kt           # MUST support both scripts
```

### Database/Entity Files
```
/core/data/database/entity/
  └─ ReceiptEntity.kt       # String fields accept both scripts
```

### UI Display Files
```
/ui/screens/
  ├─ review/ReviewReceiptScreen.kt    # Display both scripts correctly
  ├─ billdetails/BillDetailsScreen.kt # Display both scripts correctly
  └─ home/HomeScreen.kt               # Display both scripts correctly
```

---

## 🚨 Common Mistakes to Avoid

### ❌ Mistake 1: Latin-Only Patterns
```kotlin
// WRONG - Only matches Latin
Pattern.compile("(?:Račun\\s+broj)[:\\s]+(\\d+)")
```

```kotlin
// CORRECT - Matches both scripts
Pattern.compile("(?:Račun\\s+broj|Рачун\\s+број)[:\\s]+(\\d+)")
```

### ❌ Mistake 2: Hardcoded English Terms
```kotlin
// WRONG - English only
val keyword = "Invoice number"
```

```kotlin
// CORRECT - Serbian both scripts
val keywords = listOf("Račun broj", "Рачун број", "Invoice number")
```

### ❌ Mistake 3: Case-Sensitive Matching
```kotlin
// WRONG - Won't match "RAČUN BROJ" or "рачун број"
text.contains("Račun broj")
```

```kotlin
// CORRECT - Case insensitive
text.contains("Račun broj", ignoreCase = true)
// OR use Pattern.CASE_INSENSITIVE
```

### ❌ Mistake 4: Character Encoding Issues
```kotlin
// WRONG - May not display Cyrillic correctly
val text = String(bytes, Charset.forName("ISO-8859-1"))
```

```kotlin
// CORRECT - UTF-8 handles both scripts
val text = String(bytes, Charset.forName("UTF-8"))
```

---

## 🎨 UI/UX Considerations

### Font Support
Ensure fonts support Serbian characters:
```kotlin
// Use system fonts that support Cyrillic
fontFamily = FontFamily.Default  // ✅ Supports both scripts
fontFamily = FontFamily.SansSerif // ✅ Supports both scripts
```

### Text Rendering
Test UI with longest Cyrillic words:
```
Shorter:  "Račun"         (5 chars)
Longer:   "Документ"      (8 chars)
```

### Input Fields
Allow both scripts in text inputs:
```kotlin
TextField(
    value = text,
    onValueChange = { text = it },
    // No input filtering - accept all UTF-8
)
```

---

## 📊 Success Metrics

A feature has proper Serbian support when:

✅ **Functional:**
- [ ] Detects terms in Latin script
- [ ] Detects terms in Cyrillic script
- [ ] Works with mixed-script text
- [ ] Special characters render correctly

✅ **Tested:**
- [ ] Unit tests with Latin examples
- [ ] Unit tests with Cyrillic examples
- [ ] Manual tests with real bills (both scripts)
- [ ] Logcat shows correct pattern matches

✅ **User-Facing:**
- [ ] UI displays both scripts correctly
- [ ] No garbled characters
- [ ] Text doesn't overflow
- [ ] Search works with both scripts

---

## 🔄 Future Features

When adding new features, always ask:

1. **Does this scan text?** → Add Serbian Latin + Cyrillic patterns
2. **Does this display text?** → Test with both scripts
3. **Does this search?** → Support both scripts
4. **Does this compare strings?** → Normalize both scripts

---

## 📖 Serbian Resources

### ML Kit Language Support
Google ML Kit **DOES** support Serbian with both scripts:
```kotlin
val recognizer = TextRecognition.getClient(
    TextRecognizerOptions.Builder()
        .setTextRecognizerOptions(
            // Serbian is automatically handled as Latin script language
            TextRecognizerOptions.LATIN
        )
        .build()
)
```

Note: ML Kit treats Cyrillic as "Latin-compatible" for Serbian.

### Serbian Number Formatting
```kotlin
// Serbian uses . for thousands, , for decimals
val format = NumberFormat.getInstance(Locale("sr", "RS"))
// 12345.67 → "12.345,67"
```

### Serbian Date Formatting
```kotlin
// Serbian uses dd.MM.yyyy format
val format = SimpleDateFormat("dd.MM.yyyy", Locale("sr", "RS"))
// Date → "15.12.2025"
```

---

## 🎯 Summary

**REMEMBER:** Platisa is for Serbian users. Every feature must support:

1. ✅ Serbian Latin (Latinica)
2. ✅ Serbian Cyrillic (Ћирилица)
3. ✅ Mixed scripts in same document
4. ✅ Serbian number/date formats

**English is nice to have, but NOT the priority.**

When in doubt, ask:
- "Does this work with Cyrillic bills?"
- "Have I tested with both scripts?"
- "Do Serbian special characters display correctly?"

---

**This is THE reference guide for Serbian language support in Platisa.** 🇷🇸
