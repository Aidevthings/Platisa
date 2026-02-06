# GEMINI.md - Context & Instructions for Platisa

This file serves as the primary context source for the Gemini AI agent working on the "Platisa" project.

## 📱 Project Overview
**Platisa** is a sophisticated Android application for personal finance and bill management, specifically tailored for the **Serbian market**. It leverages AI (Gemini), ML Kit (OCR), and Gmail integration to automate bill tracking.

*   **Primary Goal:** Simplify bill payment and tracking for Serbian users.
*   **Key Feature:** Robust support for both Serbian scripts: **Latin (Latinica)** and **Cyrillic (Ћирилица)**.
*   **Aesthetic:** "Cyberpunk/Neon" dark mode (Deep void blue, Neon cyan/purple accents).

## 🛠️ Technology Stack
*   **Language:** Kotlin (100%)
*   **UI Toolkit:** Jetpack Compose (Material 3)
*   **Architecture:** Clean Architecture + MVVM (Model-View-ViewModel)
*   **Dependency Injection:** Dagger Hilt
*   **Local Data:** Room Database
*   **Network:** Retrofit + OkHttp
*   **Async:** Coroutines & Flow
*   **AI/ML:** 
    *   Google Gemini API (Generative AI for text analysis)
    *   ML Kit (On-device OCR & Barcode scanning)
*   **Backend:** Firebase (Firestore, Auth)

## 📂 Project Structure
Root package: `com.platisa.app` (located in `app/src/main/java/com/platisa/app/`)

```
app/src/main/java/com/platisa/app/
├── core/
│   ├── common/         # Extension functions, formatters, constants
│   ├── data/           # Repositories, Data Sources, Room Entities
│   ├── domain/         # UseCases, Models, Parsers (CRITICAL)
│   │   └── parser/     # Invoice parsing logic (Regex for Latin/Cyrillic)
│   └── worker/         # WorkManager jobs (e.g., Gmail Sync)
├── di/                 # Hilt Modules
├── ui/                 # Jetpack Compose Screens
│   ├── common/         # Reusable UI components
│   ├── screens/        # Feature screens (Home, BillDetails, etc.)
│   ├── theme/          # Color, Type, Shape definitions
│   └── navigation/     # NavHost and Route definitions
├── BaseActivity.kt
├── MainActivity.kt
└── PlatisaApplication.kt
```

## 🚨 CRITICAL DEVELOPMENT RULES

### 1. Serbian Language Support (NON-NEGOTIABLE)
**Every feature involving text manipulation or display MUST support both Serbian scripts.**
*   **Latin:** "Račun", "Datum", "Iznos"
*   **Cyrillic:** "Рачун", "Датум", "Износ"
*   **Regex Rule:** When parsing text, always account for both.
    *   *Bad:* `Pattern.compile("Račun")`
    *   *Good:* `Pattern.compile("(?:Račun|Рачун)", Pattern.CASE_INSENSITIVE)`
*   **Reference:** Consult `SERBIAN_LANGUAGE_GUIDE.md` for specific terms and patterns.

### 2. UI/Design Consistency
*   **Theme:** Strictly Dark Mode.
*   **Colors:** Use established theme colors (Neon accents on dark backgrounds).
*   **Components:** Reuse components from `ui/common/` whenever possible.
*   **Typography:** Ensure fonts support Serbian characters (č, ć, ž, š, đ, љ, њ, ђ...).

### 3. Coding Conventions
*   **State Management:** Use `StateFlow` in ViewModels collected via `collectAsStateWithLifecycle` in Compose.
*   **Dependency Injection:** Always use Hilt (`@Inject`, `@HiltViewModel`, `@AndroidEntryPoint`).
*   **Async:** Use `viewModelScope.launch` for ViewModel operations; `suspend` functions for Domain/Data layers.

## ⚡ Key Commands

### Build & Run
*   **Assemble Debug APK:**
    ```bash
    ./gradlew assembleDebug
    ```
*   **Run Unit Tests:**
    ```bash
    ./gradlew test
    ```
*   **Clean Project:**
    ```bash
    ./gradlew clean
    ```
*   **Install APK (via script):**
    ```bash
    ./install_apk.bat
    ```

### Dependency Management
*   Dependencies are currently defined in `app/build.gradle.kts`.
*   Check `gradle/libs.versions.toml` if migrating or checking for version catalogs (though direct implementation in build.gradle.kts seems prevalent).

## 🔍 Debugging & Logs
*   **Log Tags:** Use standard tags. The app uses `Timber` for logging.
*   **Parser Debugging:** When debugging OCR, check logs for "Found invoice number" or similar distinct log lines defined in `core/domain/parser`.

## 📝 Documentation References
*   `PLATISA.md`: General project philosophy and architecture.
*   `SERBIAN_LANGUAGE_GUIDE.md`: **Read before touching any regex/parsing logic.**
*   `platisa_implementation_plan.md`: Current roadmap and feature status.
