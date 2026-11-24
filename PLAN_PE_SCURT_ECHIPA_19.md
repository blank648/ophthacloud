# OphthaCloud — Plan pe scurt (Echipa 19)

**Pe scurt:** Construim o aplicație web de clinică oftalmologică și implicare a pacientului (**React + Spring Boot + PostgreSQL**). Lucrăm ghidat de întâlnirile cu Prof. dr. Radu Gramatovici: următorul prag este demonstrația din **16 decembrie (EPIC Review)**, iar după feedback-ul profesorului continuăm în ianuarie până la predarea finală (**3 februarie**).  
*(Pentru detalii tehnice complete — contracte API, schemă bază de date, constrângeri și cazuri-limită — există documentul tehnic [ARCHITECTURE_AND_EXECUTION_PLAN.md](ARCHITECTURE_AND_EXECUTION_PLAN.md)).*

---

## 1. Reguli noi agreate cu mentorii de Master (24 noiembrie)

- **Docker în decembrie = doar baza de date (`PostgreSQL 16`):** Backendul și Frontendul rulează nativ local. Containerizarea completă (backend + frontend + db) se lasă spre final, în ianuarie.
- **CI minimal acum:** Un workflow simplu pe GitHub Actions (`build + teste unitare` la PR/MR), fără configurări grele; îl rafinăm pe final.
- **Lucrăm toți 3 în paralel + Mock Data + Legare FE–BE din timp:** Iovan și Dobrescu nu așteaptă după finalizarea întregului backend; Iovan construiește ecranele pe mock data JSON, iar fiecare ecran se leagă treptat la API imediat ce endpoint-ul este gata.
- **Focus exclusiv pe teste unitare:** Fără teste de integrare sau E2E obligatorii. Scriem **1–2 teste unitare manual** ca să înțelegem structura, apoi generăm restul cu **AI** + testare manuală în browser.

---

## 2. Ce trebuie să meargă la demo-ul din 16 decembrie (EPIC Review)

Profesorul urmărește în special **UX-ul pentru Pacient**, **rolurile clare ale fiecărui membru** și un **flux cap-la-cap fără erori**:

1. **Recepție (`E-05`):** Înregistrăm pacient nou (cu validare CNP și activare cont), îl căutăm rapid și îi facem o programare (fără suprapuneri de ore).
2. **Medic (`E-06`):** Deschidem fișa pacientului, trecem consultația (acuitate vizuală, tensiune oculară, diagnostic) și prescriem picăturile cu orele zilnice.
3. **Pacient (`E-01` + `E-02`):** Ne logăm cu contul primit, folosim **bara de accesibilitate** (text mărit 100%–200%, *High-Contrast*) și **confirmăm doza curentă în maximum 2 click-uri** de la login (vedem bara de progres zilnic și seria de bază).
4. **Verificare la Medic (`E-06`):** Medicul vede confirmarea pacientului și procentul de aderență actualizat.

> **Ce NU facem până pe 16 decembrie (rămân pentru ianuarie):**  
> Fără Docker complet pe FE/BE sau cloud, fără Grila Amsler, fără timer 20-20-20, fără scut *Forgiveness Day*, fără Vouchere și fără panou Admin / Audit GDPR.

---

## 3. Cine ce face și calendarul (24 nov – 16 dec)

| Membru | Rol la prezentare | Ce lucrează în paralel (24 nov – 7 dec) | Integrare timpurie & Debug (până pe 15 dec) |
|---|---|---|---|
| **Adrian Diaconescu** | Arhitect & Lead Backend | `24–30 nov`: Docker doar pt. PostgreSQL, CI simplu (`build + unit test`), Flyway `V1..V7`, mock data JSON (25 nov), JWT/Login, backend Recepție & Medic + 1–2 teste unitare șablon. | Coordonează legarea treptată FE–BE (`27 nov` Login → `1–2 dec` Recepție/Medic → `4–7 dec` Aderență) și stabilizarea. |
| **Iovan Andrei-Alexandru** | Frontend SPA & UX Pacient | `24 nov – 7 dec`: Pornește direct pe mock data (`E-01` + bară accesibilitate 200%/High-Contrast, `E-02` Dashboard ≤2 click-uri, `E-05` Recepție, `E-06` Medic) și leagă fiecare ecran la API pe măsură ce devine disponibil. | Verifică afișarea pe desktop și mobil (`360 px`), contrastul, tastatura și navigarea ≤2 click-uri. |
| **Dobrescu Andrei-Alexandru** | Gamificare & Implicare | `24 nov – 6 dec`: Calcul dinamic doze zilnice din prescripție, confirmare picătură, serie de bază (*Streak*) și procent aderență pentru medic; 1–2 teste unitare manual + extindere cu AI (`JUnit 5 + Mockito`). | Verifică dublu-click-ul la confirmare, calculul procentului și testele unitare în CI. |
| **Toți trei** | **Rezerva de Debug & Demo** | **8 – 15 dec (8 zile dedicate):** Reparăm bug-urile de integrare, verificăm tot fluxul în browser pe PostgreSQL real (Docker), pregătim datele demo și repetăm prezentarea de 2 ori cap-la-cap. | Versiune candidată înghețată și verificată pentru **16 decembrie (EPIC Review)**. |

---

## 4. Ce facem după 16 decembrie (Ianuarie – 3 Februarie 2027)

După întâlnirea din 16 decembrie ajustăm planul pe baza feedback-ului profesorului, apoi livrăm în paralel pe mock data și cu integrare din timp:

- **Valul 1 (12 – 23 ianuarie) — Gamificare completă, Vouchere & Amsler:**
  - **12–16 ian (Dobrescu):** Scutul *Forgiveness Day* (la 7 zile), ecusoane și vouchere (5% / 10% / 15% fără duplicate) + teste unitare asistate de AI.
  - **12–20 ian (Iovan în paralel pe mock data):** Grila Amsler SVG + timer 20-20-20 (`E-03`), portofel vouchere (`E-04`), alerte medic (`E-06`) și decontare recepție (`E-05`), legate treptat la API (`17–20 ian`).
  - **16–19 ian (Adrian + Dobrescu):** Backend Amsler (alertare + preluare) și decontare voucher; **21–23 ian:** **3 zile de debug și testare manuală integrată**.

- **Valul 2 (24 ianuarie – 3 februarie) — Admin, GDPR, Docker complet & Review Final:**
  - **24–27 ian (Adrian + Dobrescu + Iovan în paralel):** Panou Admin (conturi personal, **Switch ON/OFF** vouchere — la OFF nu se mai emit vouchere noi, dar cele active rămân valabile la recepție) + jurnal audit GDPR + UI `E-07`.
  - **28–30 ian (Adrian):** **Docker complet (`FE + BE + DB`)** într-un singur `docker-compose.yml` și rafinarea finală a CI-ului.
  - **31 ian – 3 feb (Toți trei):** **4 zile de debug, testare manuală completă pe cele 4 roluri și pregătire Review Final (3 februarie).**

---

## 5. Unde găsim detaliile tehnice?

Rutele exacte REST, exemplele JSON pentru mock data, structura tabelelor PostgreSQL și tratarea cazurilor-limită se află în [**ARCHITECTURE_AND_EXECUTION_PLAN.md**](ARCHITECTURE_AND_EXECUTION_PLAN.md).
