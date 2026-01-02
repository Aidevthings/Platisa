# Platiša - Walkthrough (Korisničko Uputstvo) 📱

Dobrodošli u **Platišu**, vašeg pametnog asistenta za upravljanje računima! Ovaj dokument vas vodi kroz ključne funkcionalnosti aplikacije, prikazujući kako vam Platiša pomaže da uštedite vreme i novac.

---

## 1. Početni Ekran i Prijava (Login)
Pri prvom pokretanju, aplikacija nudi jednostavnu prijavu putem **Google naloga**.
*   **Sigurnost:** Vaši podaci su bezbedni.
*   **Brzina:** Nema potrebe za pamćenjem novih lozinki.

*(Ovde možete ubaciti screenshot Login ekrana: `![Login](screenshots/login.png)`) - Napravi folder 'docs/screenshots' i ubaci slike.*

---

## 2. Glavna Kontrolna Tabla (Home Dashboard)
Odmah nakon prijave, vidite jasan pregled vaših finansija.
*   **Mesečni Cilj:** Grafički prikaz (merač) pokazuje koliko ste potrošili u odnosu na zadati limit.
*   **Brze Akcije:** Dugme za brzo dodavanje računa.
*   **Lista Računa:** Prikaz poslednjih, neplaćenih i plaćenih računa na jednom mestu.

*(Ovde ubaciti screenshot Home ekrana: `![Home](screenshots/home.png)`)*

---

## 3. Dodavanje Računa (Smart Scan) 📸
Platiša nudi tri načina da dodate račun:

### A. Slikaj QR (Najbrži način)
1.  Kliknite na dugme **"Slikaj QR"**.
2.  Usmerite kameru ka NBS IPS QR kodu na računu.
3.  Aplikacija trenutno prepoznaje: Iznos, Primaoca, Poziv na broj i Datum.

### B. Slikaj Račun (OCR)
Za račune bez QR koda ili stare račune.
1.  Uslikajte ceo račun.
2.  Naša AI tehnologija čita tekst i izvlači podatke (čak i ćirilicu!).

### C. Učitaj PDF / Gmail
Automatski uvezite račune koji vam stižu na e-mail (npr. EPS, SBB, Telekom).

*(Ovde ubaciti screenshot kamere/skenera: `![Scan](screenshots/scan.png)`)*

---

## 4. Detalji Računa
Klikom na bilo koji račun dobijate detaljan pregled.
*   **Stavke:** Jasna lista šta plaćate (npr. VT/NT struja).
*   **Status:** Obeležite račun kao "Plaćen" jednim klikom.
*   **Original:** Uvek možete videti originalnu sliku računa.

*(Ovde ubaciti screenshot detalja računa: `![Details](screenshots/details.png)`)*

---

## 5. Analitika i Statistika 📊
Razumite gde odlazi vaš novac.
*   **Grafikoni:** Linijski i Stubičasti (Bar) prikazi potrošnje po mesecima.
*   **Kategorije:** Vidite koliko trošite na struju, telefon, infostan, itd.
*   **Trendovi:** Poređenje sa prethodnim mesecima.

*(Ovde ubaciti screenshot analitike: `![Analytics](screenshots/analytics.png)`)*

---

## 6. Podešavanja i Pretplata
U sekciji Profil možete:
*   Podesiti mesečni budžet.
*   Upravljati **Premium pretplatom** (za neograničeno čuvanje računa i naprednu AI analizu).
*   Promeniti temu (Tamna/Svetla).

*(Ovde ubaciti screenshot profila: `![Profile](screenshots/profile.png)`)*

---

## 💡 Za Programere (Tehnički Pregled)
*   **Arhitektura:** MVVM + Clean Architecture
*   **UI:** 100% Jetpack Compose
*   **Baza:** Room Database (Lokalno) + Firebase (Cloud)
*   **AI:** Google Gemini API za OCR i analizu podataka.
