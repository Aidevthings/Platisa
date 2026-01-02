# Hako objaviti Platišu na Google Play Store-u 🚀

Ovaj vodič će vas provesti kroz korake potrebne za prvo objavljivanje vaše aplikacije.

## 1. Kreiranje Naloga i Aplikacije

1.  Idite na [Google Play Console](https://play.google.com/console).
2.  Prijavite se svojim Google nalogom (plaća se jednokratna registracija od $25 ako nemate nalog).
3.  Kliknite na **Create app** (Kreiraj aplikaciju).
4.  Popunite osnovne detalje:
    *   **App name**: Platiša
    *   **Default language**: English (ili Serbian - preporučujem English kao default, a Serbian kao prevod, ali za lokalnu app može i Serbian).
    *   **App or game**: App
    *   **Free or paid**: Free
5.  Prihvatite "Declarations" i kliknite **Create app**.

## 2. Popunjavanje Podataka (Store Listing)

Koristite fajl `APP_DESCRIPTION.md` koji sam vam generisao.

1.  U meniju levo izaberite **Main store listing**.
2.  **App name**: Platiša
3.  **Short description**: (Kopirajte iz `APP_DESCRIPTION.md`)
4.  **Full description**: (Kopirajte iz `APP_DESCRIPTION.md`)
5.  **Graphics**:
    *   App icon (512x512 png)
    *   Feature graphic (1024x500 png)
    *   Screenshots (koristite one iz walkthrough-a ili napravite nove).

## 3. Generisanje Release Verzije (.aab)

Da biste otpremili aplikaciju, potreban vam je **Android App Bundle (.aab)** fajl. On mora biti *digitalno potpisan*.

### Najlakši način (preko Android Studio-a):
1.  Otvorite Android Studio.
2.  U meniju idite na **Build > Generate Signed Bundle / APK...**.
3.  Izaberite **Android App Bundle** i kliknite **Next**.
4.  **Key store path**: Kliknite na **Create new...**.
    *   Izaberite lokaciju (npr. u folderu projekta, ali NE uploadujte ovo na GitHub!).
    *   Unesite lozinku.
    *   Alias: `key0` (ili `platisa`).
    *   Unesite podatke (First and Last Name, City, itd.).
5.  Kliknite **Next**.
6.  Izaberite **Release** verziju.
7.  Kliknite **Create** (ili Finish).

Fajl će se nalaziti u: `app/release/app-release.aab`.

## 4. Otpremanje na Play Console

Preporučujem da prvo koristite **Internal testing** ili **Closed testing**.

1.  U meniju levo izaberite **Testing > Internal testing**.
2.  Kliknite **Create new release**.
3.  Pratite uputstva za **Play App Signing** (dozvolite Google-u da upravlja ključevima - "Use Google-generated key").
4.  **App bundles**: Kliknite na "Upload" i izaberite vaš `app-release.aab` fajl.
5.  **Release name**: `1.1` (automatski će se popuniti).
6.  **Release notes**: Unesite kratak opis šta je novo (npr. "Prva verzija sa AI skenerom i Gmail integracijom").
7.  Kliknite **Next** i zatim **Start rollout**.

## 5. Dodatni Zahtevi

Console će vas tražiti da popunite još nekoliko sekcija pre nego što možete da objavite (Dashboard sekcija):
*   **Privacy Policy**: Linkujte vaš hosted `privacy_policy.html`.
*   **App Access**: "All functionality is available without special access" (ili ako traži login, navedite test nalog).
*   **Ads**: "No, my app does not contain ads".
*   **Content Rating**: Popunite upitnik (PEGI 3).
*   **Target Audience**: 18+.
*   **News apps**: No.
*   **Data Safety**: Ovo je važno. Morate deklarisati da skupljate:
    *   Personal info -> Email address (za funkcionalnost aplikacije).
    *   Photos and videos (za skeniranje računa).

Srećno! 🚀
