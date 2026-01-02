# Platiša 🧾

**Platiša** je napredna Android aplikacija dizajnirana da pojednostavi upravljanje ličnim finansijama i plaćanje računa. Aplikacija koristi veštačku inteligenciju i mašinsko učenje za automatsko skeniranje, prepoznavanje i organizaciju računa.

## ✨ Glavne Funkcionalnosti

*   **Pametno Skeniranje:** Skeniranje QR kodova (Slikaj QR) i OCR prepoznavanje teksta sa papirnih računa.
*   **Gmail Sinhronizacija:** Automatsko preuzimanje i parsiranje računa direktno iz Gmail sandučeta.
*   **Analitika:** Detaljni grafički prikazi potrošnje i statistika (Line i Bar grafikoni).
*   **Pretplata:** Sistem pretplate za pristup premium funkcijama.
*   **AI Integracija:** Korišćenje Gemini AI modela za precizno izvlačenje podataka sa računa (npr. EPS ćirilica).

## 🚀 Kako Pokrenuti Projekat

### Preduslovi
*   **Android Studio:** Najnovija verzija (preporučeno Ladybug ili novija).
*   **JDK:** Java Development Kit 17 ili noviji.

### Koraci
1.  **Klonirajte repozitorijum:**
    ```bash
    git clone https://github.com/Aidevthings/Platisa.git
    ```
2.  **Otvorite u Android Studio-u:**
    Pokrenite Android Studio, odaberite `Open` i pronađite folder `Platisa`.
3.  **Gradle Sinhronizacija:**
    Sačekajte da Android Studio preuzme sve potrebne biblioteke i indeksira projekat.

## 🔑 Potrebni API Ključevi

Aplikacija zahteva određene tajne ključeve da bi funkcionalisala ispravno (posebno AI funkcije). Ovi ključevi se **ne čuvaju** u GitHub repozitorijumu iz bezbednosnih razloga.

1.  U *root* folderu projekta (gde se nalazi `gradlew`), kreirajte fajl pod nazivom `local.properties` (ako već ne postoji).
2.  Dodajte sledeće linije:

```properties
# Google Gemini AI API Ključ (Za analizu računa)
GEMINI_API_KEY=vas_gemini_api_kljuc_ovde
```

*(Napomena: Bez ovog ključa, funkcije koje koriste AI analizu neće raditi ili će aplikacija prijaviti grešku prilikom build-a).*

## 🏗️ Arhitektura

Projekat je strukturiran prateći principe **Modern Android Development (MAD)** i **Clean Architecture**:

*   **Jezik:** 100% Kotlin.
*   **UI Framework:** Jetpack Compose (deklarativni UI).
*   **Arhitektonski Obrazac:** MVVM (Model-View-ViewModel).
    *   **UI Layer:** Composable funkcije i Ekrani.
    *   **ViewModel:** Drži stanje ekrana (`StateFlow`) i komunicira sa slojem podataka.
    *   **Data/Domain Layer:** Repozitorijumi (`Repository`), Use Cases i Data Sources.
*   **Dependency Injection:** Hilt (Dagger).
*   **Asinhrono Programiranje:** Kotlin Coroutines i Flow.
*   **Lokalna Baza:** Room Database.
*   **Mreža:** Retrofit & OkHttp.

## 🛠️ Uputstvo za Build

Aplikacija koristi Gradle build sistem. Komande možete pokretati iz terminala u Android Studio-u.

### Pokretanje na Emulatoru/Uređaju (Debug)
```bash
./gradlew installDebug
```

### Kreiranje Release Verzije (APK/Bundle)
Za kreiranje potpisane verzije za Google Play ili distribuciju:

```bash
./gradlew assembleRelease
# Izlazni fajl će biti u: app/build/outputs/apk/release/
```
ili za App Bundle (.aab):
```bash
./gradlew bundleRelease
# Izlazni fajl će biti u: app/build/outputs/bundle/release/
```

*Napomena: Za uspešan release build, potrebno je konfigurisati Keystore potpisivanje u `build.gradle.kts` ili to uraditi ručno kroz Android Studio (Build > Generate Signed Bundle / APK).*
