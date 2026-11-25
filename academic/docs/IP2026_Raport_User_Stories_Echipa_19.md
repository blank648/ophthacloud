# UNIVERSITATEA DIN BUCUREȘTI — FACULTATEA DE MATEMATICĂ ȘI INFORMATICĂ
## OphthaCloud: Platformă Clinică Oftalmologică cu Modul de Implicare a Pacientului
# RAPORT DE USER STORIES COMPLEXE

**Disciplina:** Ingineria Programării — Semestrul I, Anul Universitar 2026–2027  
**Echipa 19 (Licență):**
- **Adrian Diaconescu** — Software Architect & Lead Backend Engineer
- **Iovan Andrei-Alexandru** — Frontend SPA & Patient Accessibility UX Engineer
- **Dobrescu Andrei-Alexandru** — Backend Engineer (`gamification`) & Owner Unit Tests

**Echipa de mentori (Master MEDS):** Echipa 20 — Ignat Eduardo, Pantaziu Andrei-Rareș  
**Coordonator disciplină:** Prof. dr. Radu Gramatovici  
**Data:** Noiembrie 2026

---

## Cuprins
1. Metodologie (INVEST + Gherkin), Persone Clinice și Harta celor 5 EPIC-uri
2. Matricea Completă de Trasabilitate, Estimare și Livrare (`US-01` – `US-16`)
3. EPIC 1 — Accesibilitate Vizuală, Autentificare și Securitate RBAC (`US-01`, `US-02`)
4. EPIC 2 — Fluxul Operațional de Recepție și Programări (`US-03`, `US-04`, `US-05`)
5. EPIC 3 — Actul Medical Oftalmologic și Prescrierea Terapeutică (`US-06`, `US-07`, `US-08`)
6. EPIC 4 — Portalul Pacientului, Gamificare Non-Punitivă și Telemedicină (`US-09` – `US-13`)
7. EPIC 5 — Guvernanță Clinică, Control Comercial și Audit GDPR Art. 9 (`US-14` – `US-16`)
8. Planul de Livrare pe Ferestre (`16 dec` vs. `12 ian – 3 feb`), Roluri la Prezentare și Criterii Globale DoR/DoD

---

## 1. Metodologie (INVEST + Gherkin), Persone Clinice și Harta celor 5 EPIC-uri

Prezentul raport formalizează cerințele din Raportul de Analiză (`CF-*`, `CQ-*`, `CST-*`), arhitectura din `ARCHITECTURE_AND_EXECUTION_PLAN.md`, board-ul operațional din `EXECUTION_BOARD_FEREASTRA_1.md`, ghidul de lucru `academic/TEAM_PLAYBOOK.md` și specificațiile de analiză ale Echipei Master 20 (`SA-01..SA-15`, `SP-01..SP-10`) în **16 User Stories complexe (89 Story Points)** grupate în **5 EPIC-uri**.

Fiecare User Story respectă criteriile **INVEST** (*Independent, Negotiable, Valuable, Estimable, Small, Testable*) și include **3 scenarii comportamentale Gherkin (`Given / When / Then`)**: (1) *Flux principal (Happy Path)*; (2) *Caz-limită / Idempotență / Concurență*; (3) *Validare / Eroare / Securitate sau Accesibilitate*, alături de **Criterii Non-Funcționale** și **Definition of Done (DoD)** aliniate la strategia agreată cu mentorii (`JUnit 5 + Mockito` și verificare manuală în browser).

### 1.1. Persone Clinice și Operaționale

| Persona | Rol RBAC | Profil & Context Clinic | Obiectiv Principal în OphthaCloud |
|---|---|---|---|
| **Dl. Ion Popescu** *(68 ani)* | `PATIENT` | Pacient cu glaucom primitiv cu unghi deschis și acuitate vizuală scăzută; utilizează telefon (`360 px`) și desktop. | Să mărească interfața la `200%`, să confirme picăturile în ≤ 2 click-uri de la login, să efectueze testul Amsler și să rămână motivat fără anxietate. |
| **Dr. Elena Ionescu** | `DOCTOR` | Medic oftalmolog curant în clinică ambulatorie; consultă între 15 și 25 pacienți/zi. | Să consemneze rapid parametrii (AV, TIO mmHg, ICD-10), să prescrie coliruri și să vizualizeze aderența reală și alertele Amsler prioritare. |
| **Maria Dumitrescu** | `RECEPTIONIST` | Recepționer medical; gestionează înregistrarea, programările pe cabinete și decontările la ghișeu. | Să prevină dublurile prin validarea CNP-ului, să caute în `< 300 ms`, să evite suprapunerile orare și să aplice corect voucherele eligibile. |
| **Andrei Marinescu** | `ADMIN` | Administrator clinică; răspunde de conturile personalului, politica de fidelizare și conformitatea GDPR. | Să gestioneze rolurile staff-ului, să comute global **Switch ON/OFF** pentru recompense și să consulte jurnalul imutabil GDPR Art. 9. |

### 1.2. Harta celor 5 EPIC-uri

| EPIC | Denumire EPIC | User Stories | SP F1 *(16 dec)* | SP F2 *(12 ian – 3 feb)* | Total SP |
|---|---|---|---|---|---|
| **EPIC 1** | Accesibilitate Vizuală, Autentificare și Securitate RBAC | `US-01`, `US-02` | 10 SP | — | **10 SP** |
| **EPIC 2** | Fluxul Operațional de Recepție și Programări | `US-03`, `US-04`, `US-05` | 15 SP | — | **15 SP** |
| **EPIC 3** | Actul Medical Oftalmologic și Prescrierea Terapeutică | `US-06`, `US-07`, `US-08` | 15 SP | — | **15 SP** |
| **EPIC 4** | Portalul Pacientului, Gamificare Non-Punitivă și Telemedicină | `US-09` (F1); `US-10..US-13` (F2) | 8 SP *(+baza US-10)* | 27 SP *(Val 1)* | **35 SP** |
| **EPIC 5** | Guvernanță Clinică, Control Comercial și Audit GDPR Art. 9 | `US-14`, `US-15`, `US-16` | — | 14 SP *(Val 2)* | **14 SP** |
| **TOTAL** | **Sistemul complet OphthaCloud (5 Epics, 16 User Stories)** | **`US-01` – `US-16`** | **48 SP *(9 US+bază)*** | **41 SP *(7 US)*** | **89 SP** |

---

## 2. Matricea Completă de Trasabilitate, Estimare și Livrare (`US-01` – `US-16`)

| Cod US | Titlu User Story | EPIC | Prioritate MoSCoW | SP | Fereastră / Sprint | Responsabili Echipa 19 | Ecrane & API Principale | Trasabilitate (`CF`/`CQ`/`SA`) |
|---|---|---|---|---|---|---|---|---|
| `US-01` | Bară accesibilitate WCAG 2.1 AA (`CIC-06.1`) & sincronizare preferințe | EPIC 1 | Must Have | 5 | F1 (EPIC Review) | Iovan + Adrian | `E-01..E-07`, `PATCH /api/me/preferences` | `CQ-UTI-01`, `SA-08`, `SP-07` |
| `US-02` | Autentificare stateless JWT, rutare RBAC 4 roluri & revocare cont | EPIC 1 | Must Have | 5 | F1 (EPIC Review) | Adrian + Iovan | `E-01`, `POST /api/auth/login`, `GET /api/me` | `CQ-SEC-01`, `CF-ADM-01`, `SA-12`, `SP-03` |
| `US-03` | Înregistrare pacient cu CNP Modulo 11, activare cont & reset credențiale | EPIC 2 | Must Have | 5 | F1 (EPIC Review) | Adrian + Iovan | `E-05`, `POST /api/patients`, `.../reset` | `CF-REC-01`, `SA-01`, `SA-02`, `SP-04` |
| `US-04` | Căutare multicriterială rapidă (`< 300 ms` pe 10k fișe) & izolare rol | EPIC 2 | Must Have | 5 | F1 (EPIC Review) | Adrian + Iovan | `E-05`/`E-06`, `GET /api/patients/search` | `CF-REC-02`, `CF-MED-01`, `CQ-PER-01`, `SA-03` |
| `US-05` | Planificare programări fără suprapuneri (`GiST`) & solicitare din portal | EPIC 2 | Must Have | 5 | F1 (EPIC Review) | Adrian + Iovan | `E-05`/`E-04`, `GET/POST /api/appointments` | `CF-REC-02`, `SA-04` |
| `US-06` | Documentare consultație (`AV`, `TIO mmHg`, biomicroscopie, `ICD-10`) | EPIC 3 | Must Have | 5 | F1 (EPIC Review) | Adrian + Iovan | `E-06`, `POST/GET /api/consultations` | `CF-MED-01`, `SA-05` |
| `US-07` | Prescriere digitală coliruri (`JSONB schedule_hours`) & versionare plan | EPIC 3 | Must Have | 5 | F1 (EPIC Review) | Adrian + Iovan | `E-06`, `POST/GET /api/treatment-plans` | `CF-MED-02`, `SA-06`, `SP-05` |
| `US-08` | Monitorizare rată aderență (%) pe doze evaluabile & istoric la medic | EPIC 3 | Must Have | 5 | F1 (EPIC Review) | Dobrescu + Iovan | `E-06`, `GET /api/adherence/{patientId}/summary` | `CF-MED-03`, `SA-09` |
| `US-09` | Confirmare doză în ≤ 2 click-uri, calcul dinamic & idempotență dublu-click | EPIC 4 | Must Have | 8 | F1 (EPIC Review) | Dobrescu + Iovan | `E-02`, `GET /today`, `POST /confirm` | `CF-PAC-01/02`, `CQ-UTI-02`, `SA-07`, `SP-05` |
| `US-10` | Seria de aderență (*Streak* bază în F1) & Scuturi *Forgiveness Day* (Val 1) | EPIC 4 | Must / Should | 8 | F1 bază + Val 1 | Dobrescu + Iovan | `E-02` (`gamification_profiles`) | `CF-PAC-02`, `CF-ADM-03`, `CQ-PER-02`, `SA-13` |
| `US-11` | Autotest Grila Amsler `20×20`, clasificare severitate & preluare alertă medic | EPIC 4 | Should Have | 8 | F2 (Val 1: 12–23 ian) | Adrian + Dobrescu + Iovan | `E-03`/`E-06`, `/api/amsler-tests`, `/amsler-alerts` | `CF-PAC-03`, `CF-MED-04`, `SA-10`, `SA-11` |
| `US-12` | Micro-provocare igienă vizuală Regula `20-20-20` & ecusoane digitale | EPIC 4 | Could Have | 3 | F2 (Val 1: 12–23 ian) | Iovan + Dobrescu | `E-03`/`E-02` (`unlocked_badges`) | `CF-PAC-04`, `CQ-UTI-01` |
| `US-13` | Emitere idempotentă vouchere (`5/10/15%`), portofel & decontare atomică | EPIC 4 | Should Have | 8 | F2 (Val 1: 12–23 ian) | Dobrescu + Adrian + Iovan | `E-04`/`E-05`, `/api/vouchers`, `.../redeem` | `CF-PAC-05`, `CF-REC-03`, `CQ-PER-02`, `SA-14` |
| `US-14` | Gestiune conturi personal clinic (`DOCTOR`/`RECEPTIONIST`) & suspendare | EPIC 5 | Should Have | 5 | F2 (Val 2: 24 ian–3 feb) | Adrian + Iovan | `E-07`, `POST/GET/PATCH /api/admin/staff` | `CF-ADM-01`, `CQ-SEC-01`, `SA-12` |
| `US-15` | Comutator global **Switch ON/OFF** recompense (menținând ACTIVE la `OFF`) | EPIC 5 | Should Have | 5 | F2 (Val 2: 24 ian–3 feb) | Adrian + Dobrescu + Iovan | `E-07`/`E-04`/`E-05`, `/api/admin/rewards-switch` | `CF-ADM-02`, `CF-ADM-03`, `SA-14`, `SP-09` |
| `US-16` | Jurnal imutabil (*append-only*) GDPR Art. 9 prin Spring AOP & consultare E-07 | EPIC 5 | Should Have | 4 | F2 (Val 2: 24 ian–3 feb) | Adrian + Iovan | `E-07`, `GET /api/admin/audit-logs` | `CF-ADM-04`, `CQ-SEC-02`, `SA-15`, `SP-10` |

---

## 3. EPIC 1 — Accesibilitate Vizuală, Autentificare și Securitate RBAC

### `US-01`: Bară Permanentă de Accesibilitate Vizuală WCAG 2.1 AA (`CIC-06.1`) și Sincronizare Preferințe
- **Metadate:** EPIC 1 · **Prioritate:** Must Have · **Estimare:** 5 SP · **Fereastră:** Fereastra 1 (EPIC Review, 16 dec)
- **Responsabili Echipa 19:** Iovan Andrei-Alexandru (FE) & Adrian Diaconescu (BE)
- **Ecrane & Endpoint-uri REST:** `E-01`–`E-07` (`CIC-06.1`) · `PATCH /api/me/preferences` · **Tabel DB:** `patients` (`ui_font_scale`, `high_contrast`)
- **Trasabilitate:** `CQ-UTI-01`, `SA-08`, `SP-07`, `E1-IO-01`, `INT-01`, `DBG-03`

> **Ca** pacient cu acuitate vizuală redusă (Dl. Ion Popescu, 68 ani),  
> **vreau să** ajustez scara fontului între 100% și 200%, să activez modul *High-Contrast* cu etichetă explicită și să folosesc ținte tactile mari chiar înainte de autentificare,  
> **astfel încât** să pot utiliza platforma autonom pe telefon sau desktop fără efort vizual și cu preferințele salvate permanent.

- **Scenarii Gherkin:**
  1. **Flux principal — Escaladare discretă și temă High-Contrast:**
     ```gherkin
     Given pacientul se află pe ecranul E-01 sau E-02 cu font de bază 18px (--font-scale = 1.0)
     When apasă controalele de mărire prin pașii discreți [1.0, 1.2, 1.5, 1.8, 2.0] până la 2.0 și activează comutatorul High-Contrast
     Then variabila CSS --font-scale devine 2.0 (200%), data-high-contrast="true" se aplică global cu stare afișată textual, contrastul este >= 4.5:1 și nu apare scroll orizontal la 360px
     ```
  2. **Caz-limită — Sincronizare preferințe pre-login în profilul PostgreSQL:**
     ```gherkin
     Given pacientul a selectat fontScale = 1.5 și highContrast = true pe E-01 înainte de autentificare
     When se autentifică cu succes și apelează PATCH /api/me/preferences
     Then preferințele sunt validate împotriva setului discret permis NUMERIC(2,1) și persistate în patients pentru sesiunile viitoare
     ```
  3. **Validare / Accesibilitate — Ținte tactile, tastatură și respingere scară invalidă:**
     ```gherkin
     Given utilizatorul navighează prin Tab/Enter sau trimite un payload cu uiFontScale = 1.25 sau 2.5
     When interacționează cu controalele sau trimite cererea PATCH
     Then toate elementele interactive au dimensiune >= 48x48 px CSS și focus ring vizibil, iar API-ul respinge scara în afara [1.0, 1.2, 1.5, 1.8, 2.0] cu 400/422
     ```
- **Criterii Non-Funcționale & DoD:** Conformitate WCAG 2.1 AA verificată manual (`CT-04`, `DBG-03`) pe `360 px` și `1920×1080`; `CHECK (ui_font_scale BETWEEN 1.0 AND 2.0)` în `V2__patients.sql`; testare unitară validare preferințe + build Vite verde.

---

### `US-02`: Autentificare Stateless JWT, Rutare RBAC pe 4 Roluri și Revocare Cont Suspendat
- **Metadate:** EPIC 1 · **Prioritate:** Must Have · **Estimare:** 5 SP · **Fereastră:** Fereastra 1 (EPIC Review, 16 dec)
- **Responsabili Echipa 19:** Adrian Diaconescu (BE) & Iovan Andrei-Alexandru (FE)
- **Ecrane & Endpoint-uri REST:** `E-01` · `POST /api/auth/login`, `GET /api/me` · **Tabel DB:** `users`
- **Trasabilitate:** `CQ-SEC-01`, `CF-ADM-01`, `SA-12`, `SP-03`, `E1-AD-01`, `INT-01`

> **Ca** utilizator al clinicii sau pacient (Dl. Ion Popescu / Dr. Elena Ionescu),  
> **vreau să** mă autentific securizat cu e-mail și parolă și să accesez exclusiv ecranele și datele autorizate rolului meu,  
> **astfel încât** portalul să respecte confidențialitatea medicală, iar un cont dezactivat să piardă accesul imediat.

- **Scenarii Gherkin:**
  1. **Flux principal — Autentificare JWT și redirecționare automată:**
     ```gherkin
     Given un cont activ în users cu rolul PATIENT și e-mail normalizat lowercase+trim
     When trimite POST /api/auth/login cu parolă validă (verificată BCrypt) din E-01
     Then serverul returnează 200 OK cu {accessToken, tokenType, expiresIn: 900, role: "PATIENT"}, fără date clinice în JWT, iar SPA stochează tokenul în memorie și navighează direct la E-02
     ```
  2. **Caz-limită — Cont suspendat cu token încă neexpirat:**
     ```gherkin
     Given un utilizator deține un token JWT valid emis cu 5 minute în urmă, dar is_active a fost comutat pe false în users
     When apelează orice endpoint protejat /api/*
     Then Spring Security verifică starea activă a contului la fiecare cerere protejată și respinge cererea cu 401 Unauthorized, fără bucle de reîncercare în SPA
     ```
  3. **Validare / Securitate — Izolare RBAC și protecție credențiale greșite:**
     ```gherkin
     Given un utilizator autentificat cu rolul PATIENT sau o parolă greșită la login
     When încearcă accesarea unei rute/endpoint DOCTOR/ADMIN sau trimite parolă incorectă
     Then API-ul răspunde cu 403 Forbidden (pentru rol nepermis) sau 401 cu mesaj generic (fără a divulga dacă e-mailul există)
     ```
- **Criterii Non-Funcționale & DoD:** Token JWT exclusiv în memorie în `apiClient.ts` (șters la logout/schimbare utilizator); niciun token sau parolă în loguri; legare `INT-01` pe 28 nov + teste unitare `JUnit 5 + Mockito`.

---

## 4. EPIC 2 — Fluxul Operațional de Recepție și Programări

### `US-03`: Înregistrare Pacient cu Validare CNP Modulo 11, Activare Atomică Cont și Reset Credențiale
- **Metadate:** EPIC 2 · **Prioritate:** Must Have · **Estimare:** 5 SP · **Fereastră:** Fereastra 1 (EPIC Review, 16 dec)
- **Responsabili Echipa 19:** Adrian Diaconescu (BE) & Iovan Andrei-Alexandru (FE)
- **Ecrane & Endpoint-uri REST:** `E-05` · `POST /api/patients`, `POST /api/patients/{id}/credentials/reset` · **Tabele DB:** `users`, `patients`
- **Trasabilitate:** `CF-REC-01`, `SA-01`, `SA-02`, `SP-04`, `E1-AD-02`, `E1-IO-02`, `INT-02`

> **Ca** recepționer (Maria Dumitrescu),  
> **vreau să** validez matematic CNP-ul în timp real și să activez automat contul de portal al pacientului în aceeași operație cu crearea fișei,  
> **astfel încât** să elimin erorile de introducere și dublurile, iar pacientul să primească imediat acces la portal.

- **Scenarii Gherkin:**
  1. **Flux principal — Creare atomică fișă + cont pacient (`no-store`):**
     ```gherkin
     Given recepționerul completează în E-05 datele lui Ion Popescu cu un CNP valid (cifra 13 verificată cu constanta 279146358279 modulo 11, rest 10 -> 1)
     When trimite POST /api/patients
     Then într-o singură tranzacție se inserează rândurile users(PATIENT) și patients(user_id), iar API-ul întoarce 201 Created cu {patientId, userId, initialCredentials} și header Cache-Control: no-store
     ```
  2. **Caz-limită / Concurență — CNP duplicat și resetare credențiale pierdute:**
     ```gherkin
     Given un CNP care există deja în patients (sau două cereri concurente pentru același CNP) sau un pacient care a pierdut parola inițială
     When se trimite al doilea POST /api/patients sau POST /api/patients/{id}/credentials/reset
     Then constrângerea UNIQUE(cnp) respinge dublura cu 409 PATIENT_EXISTS, iar resetarea emite un secret nou hash-uit BCrypt o singură dată cu Cache-Control: no-store
     ```
  3. **Validare / Eroare — CNP invalid algoritmic sau lungime greșită:**
     ```gherkin
     Given un șir CNP cu lungime != 13, caractere nenumerice, cifră de control greșită sau dată calendaristică invalidă
     When recepționerul tastează în E-05 sau trimite cererea
     Then E-05 semnalează eroarea textual înainte de submit, iar backendul respinge cererea cu 422 prin CnpModulo11Validator
     ```
- **Criterii Non-Funcționale & DoD:** CNP tratat exclusiv ca șir `CHAR(13)` și exclus din logurile tehnice; test unitar șablon `CnpModulo11ValidatorTest` (rest 0, rest 10→1, lungime, litere) + probe manuale în `E-05`.

---

### `US-04`: Căutare Multicriterială Rapidă (`< 300 ms` pe 10.000 Fișe) și Izolare pe Rol
- **Metadate:** EPIC 2 · **Prioritate:** Must Have · **Estimare:** 5 SP · **Fereastră:** Fereastra 1 (EPIC Review, 16 dec)
- **Responsabili Echipa 19:** Adrian Diaconescu (BE) & Iovan Andrei-Alexandru (FE)
- **Ecrane & Endpoint-uri REST:** `E-05`, `E-06` · `GET /api/patients/search`, `GET /api/patients/{id}` · **Tabel DB:** `patients`
- **Trasabilitate:** `CF-REC-02`, `CF-MED-01`, `CQ-PER-01`, `SA-03`, `SP-06`, `E1-AD-02`, `DBG-03`

> **Ca** recepționer sau medic oftalmolog,  
> **vreau să** caut pacienții instantaneu (`< 300 ms`) după CNP, nume/prenume sau telefon, primind strict câmpurile autorizate rolului meu,  
> **astfel încât** fluxul din clinică să fie fluent fără a expune date clinice sensibile la recepție sau către medici neasignați.

- **Scenarii Gherkin:**
  1. **Flux principal — Performanță sub 300 ms pe 10.000 de fișe:**
     ```gherkin
     Given tabela patients conține 10.000 de înregistrări cu indecși B-Tree pe cnp, compus pe (last_name, first_name) și GIN pg_trgm
     When recepționerul sau medicul apelează GET /api/patients/search după CNP exact, prefix de nume sau telefon normalizat
     Then API-ul întoarce lista paginată {items, page, size, total} (max 50/pagină) în < 300 ms
     ```
  2. **Caz-limită — Diferențiere câmpuri recepție vs. medic:**
     ```gherkin
     Given un recepționer și un medic caută același pacient
     When fiecare apelează endpoint-ul de căutare și detalii
     Then recepționerul primește strict identificarea și programările (fără AV/TIO/diagnostic), iar medicul poate filtra inclusiv după diagnostic/ultima vizită în sfera sa
     ```
  3. **Validare / Securitate — Izolare pe obiect pentru medic neasignat:**
     ```gherkin
     Given Dr. A nu are nicio programare cu pacientul X
     When trimite GET /api/patients/{patientX_id} cu un UUID schimbat manual
     Then serviciul verifică relația medic-pacient și respinge accesul cu 403/404 fără a divulga existența fișei
     ```
- **Criterii Non-Funcționale & DoD:** Proiecții DTO înguste (zero lazy-loading N+1); query-uri parametrizate; plan `EXPLAIN (ANALYZE, BUFFERS)` și benchmark `< 300 ms` verificate în `DBG-03`.

---

### `US-05`: Planificarea Programărilor fără Suprapuneri (`GiST`) și Solicitare din Portal
- **Metadate:** EPIC 2 · **Prioritate:** Must Have · **Estimare:** 5 SP · **Fereastră:** Fereastra 1 (EPIC Review, 16 dec)
- **Responsabili Echipa 19:** Adrian Diaconescu (BE) & Iovan Andrei-Alexandru (FE)
- **Ecrane & Endpoint-uri REST:** `E-05`, `E-04`, `E-06` · `GET /api/doctors`, `GET/POST /api/appointments` · **Tabel DB:** `appointments`
- **Trasabilitate:** `CF-REC-02`, `SA-04`, `E1-AD-02`, `E1-IO-02`, `INT-02`, `DBG-02`

> **Ca** recepționer (Maria Dumitrescu) sau pacient autentificat,  
> **vreau să** planific consultații pe cabinete/medici cu blocarea automată a suprapunerilor orare,  
> **astfel încât** doi pacienți să nu poată fi programați în intervale suprapuse la același medic nici în condiții de concurență.

- **Scenarii Gherkin:**
  1. **Flux principal — Programare reușită și interval adiacent `[)`:**
     ```gherkin
     Given Dr. Elena Ionescu are o programare între 10:00 și 10:30, iar intervalul 10:30–11:00 este liber
     When recepționerul trimite POST /api/appointments pentru 10:30 cu durationMin = 30
     Then serverul creează programarea (201 Created, status = SCHEDULED) permițând limita exactă a intervalului semi-deschis
     ```
  2. **Caz-limită / Concurență — Conflict parțial `EXCLUDE USING gist`:**
     ```gherkin
     Given există programarea 10:00–10:30 la Dr. Ionescu și două stații încearcă rezervare concurentă pe 10:15–10:45
     When tranzacțiile se execută pe PostgreSQL 16
     Then constrângerea EXCLUDE USING gist (doctor_id, tsrange) permite cel mult un câștigător sau respinge conflictul cu 409 APPOINTMENT_OVERLAP afișat în E-05
     ```
  3. **Validare / Securitate — Solicitare din portal cu `patientId` din JWT:**
     ```gherkin
     Given pacientul Ion Popescu solicită o programare liberă din ecranul E-04
     When trimite cererea către API (chiar cu alt patientId falsificat în body)
     Then serverul ignoră ID-ul extern, folosește exclusiv identitatea derivată din JWT sub -> users -> patients și validează durationMin > 0
     ```
- **Criterii Non-Funcționale & DoD:** Extensia `btree_gist` activă în `V3__appointments.sql`; stări admise `SCHEDULED`, `COMPLETED`, `NO_SHOW`; demonstrație live cu respingere suprapunere la `16 dec`.

---

## 5. EPIC 3 — Actul Medical Oftalmologic și Prescrierea Terapeutică

### `US-06`: Documentarea Consultației Oftalmologice (`AV`, `TIO mmHg`, Biomicroscopie, `ICD-10`)
- **Metadate:** EPIC 3 · **Prioritate:** Must Have · **Estimare:** 5 SP · **Fereastră:** Fereastra 1 (EPIC Review, 16 dec)
- **Responsabili Echipa 19:** Adrian Diaconescu (BE) & Iovan Andrei-Alexandru (FE)
- **Ecrane & Endpoint-uri REST:** `E-06` · `POST/GET /api/consultations` · **Tabele DB:** `consultations`, `appointments`
- **Trasabilitate:** `CF-MED-01`, `SA-05`, `E1-AD-03`, `E1-IO-02`, `INT-02`

> **Ca** medic oftalmolog (Dr. Elena Ionescu),  
> **vreau să** înregistrez structurat acuitatea vizuală, tensiunea intraoculară în mmHg, notele de biomicroscopie și diagnosticul ICD-10 pentru programarea pacientului meu,  
> **astfel încât** dosarul medical să fie complet, unic per vizită și accesibil doar medicului programat.

- **Scenarii Gherkin:**
  1. **Flux principal — Salvare consultație și finalizare vizită:**
     ```gherkin
     Given o programare SCHEDULED asignată lui Dr. Elena Ionescu
     When medicul trimite din E-06 POST /api/consultations cu {appointmentId, vaOd:"0.7", vaOs:"0.9", iopOd:22.0, iopOs:17.0, biomicroscopyNotes:"...", diagnosisIcd10:"H40.1"}
     Then consultația este salvată (201 Created), programarea asociată trece în COMPLETED și devine disponibilă prescrierea de tratament
     ```
  2. **Caz-limită — O singură consultație per programare:**
     ```gherkin
     Given o programare care are deja o consultație asociată
     When medicul încearcă să creeze o a doua consultație pe același appointmentId
     Then constrângerea FK UNIQUE(appointment_id) respinge inserția și API-ul întoarce 409 Conflict
     ```
  3. **Validare / Securitate — Restricție medic programat și valori clinice:**
     ```gherkin
     Given un alt medic neatribuit programării sau valori TIO negative (iopOd < 0) ori cod ICD-10 invalid
     When se apelează POST /api/consultations
     Then accesul altui medic este blocat cu 403/404, iar valorile clinice invalide primesc 400/422 cu detalii în fieldErrors
     ```
- **Criterii Non-Funcționale & DoD:** `iop_od`, `iop_os` stocate ca `NUMERIC(4,1)` (mmHg nenegativ); legare live în `INT-02` (1–2 dec); teste unitare pe reguli de autorizare și validare.

---

### `US-07`: Prescrierea Digitală a Schemei de Coliruri (`JSONB schedule_hours`) și Versionare Plan
- **Metadate:** EPIC 3 · **Prioritate:** Must Have · **Estimare:** 5 SP · **Fereastră:** Fereastra 1 (EPIC Review, 16 dec)
- **Responsabili Echipa 19:** Adrian Diaconescu (BE) & Iovan Andrei-Alexandru (FE)
- **Ecrane & Endpoint-uri REST:** `E-06`, `E-02` · `POST/GET /api/treatment-plans` · **Tabele DB:** `treatment_plans`, `consultations`
- **Trasabilitate:** `CF-MED-02`, `SA-06`, `SP-05`, `E1-AD-03`, `INT-02`, `DBG-02`

> **Ca** medic oftalmolog (Dr. Elena Ionescu),  
> **vreau să** prescriu și să actualizez schema de administrare a picăturilor (medicament, ochi, doze/zi, ore exacte, durată),  
> **astfel încât** portalul pacientului să afișeze imediat dozele corecte fără a crea false omisiuni pentru orele trecute din ziua prescrierii.

- **Scenarii Gherkin:**
  1. **Flux principal — Prescriere schemă și activare în fereastra efectivă:**
     ```gherkin
     Given o consultație existentă a lui Ion Popescu
     When medicul trimite POST /api/treatment-plans la ora 12:00 cu {consultationId, medicationName:"Latanoprost 50 mcg/ml", targetEye:"AO", dropsPerDose:1, scheduleHours:["08:00","20:00"], startDate, endDate}
     Then serverul derivă patientId din consultație, setează activated_at = NOW(), salvează planul activ și îl expune prin TreatmentPlanQueryService (fără a marca retroactiv doza de 08:00 ca MISSED)
     ```
  2. **Caz-limită — Versionare prin `replacesPlanId`:**
     ```gherkin
     Given un plan activ anterior cu orar ["08:00","20:00"]
     When medicul prescrie o schemă actualizată specificând replacesPlanId
     Then în aceeași tranzacție planul vechi primește is_active=false și stopped_at=NOW(), păstrând în istoric dozele deja scadente/confirmate anterior opririi
     ```
  3. **Validare / Eroare — Orar invalid, date inversate sau coliziune DST:**
     ```gherkin
     Given scheduleHours gol, cu ore duplicate sau neordonate, startDate > endDate ori două ore care colidează la trecerea la ora de vară/iarnă (DST)
     When medicul trimite cererea
     Then API-ul respinge cu 422 și mesaj explicit în E-06 înainte de activarea schemei
     ```
- **Criterii Non-Funcționale & DoD:** `CHECK (target_eye IN ('OD','OS','AO'))`, `drops_per_dose > 0`, `start_date <= end_date`; interfață publică `TreatmentPlanQueryService` verificată prin ArchUnit `ModuleArchitectureTest`.

---

### `US-08`: Monitorizarea Ratei Clinice de Aderență (%) și Istoricul Dozelor la Medic
- **Metadate:** EPIC 3 · **Prioritate:** Must Have · **Estimare:** 5 SP · **Fereastră:** Fereastra 1 (EPIC Review, 16 dec)
- **Responsabili Echipa 19:** Dobrescu Andrei-Alexandru (BE) & Iovan Andrei-Alexandru (FE)
- **Ecrane & Endpoint-uri REST:** `E-06` · `GET /api/adherence/{patientId}/summary` · **Tabele DB:** `adherence_logs`, `treatment_plans`
- **Trasabilitate:** `CF-MED-03`, `SA-09`, `E1-DO-01`, `E1-IO-02`, `INT-03`

> **Ca** medic oftalmolog curant (Dr. Elena Ionescu),  
> **vreau să** văd rata procentuală reală de aderență (%) și istoricul paginat al dozelor confirmate sau omise între vizite,  
> **astfel încât** să evaluez obiectiv complianța pacientului fără ca dozele viitoare din ziua curentă să denatureze procentul.

- **Scenarii Gherkin:**
  1. **Flux principal — Calcul exact pe numitor evaluabil (`100.0%` / `75.0%`):**
     ```gherkin
     Given un plan cu orele 08:00 și 20:00 la ora clinicii 12:00 (doza de 08:00 confirmată, doza de 20:00 încă viitoare)
     When medicul apelează GET /api/adherence/{patientId}/summary?from=...&to=...
     Then API-ul exclude doza viitoare din numitor și returnează expected=1, confirmedOnTime=1, adherencePercent=100.0 (spre deosebire de pacient care vede progres zilnic 50%); pentru 3 confirmări din 4 doze închise returnează 75.0
     ```
  2. **Caz-limită — Numitor zero (`null` și etichetă explicită):**
     ```gherkin
     Given o prescripție activată la 12:00 cu prima doză programată abia la 20:00 (0 doze ajunse la scadență sau confirmate)
     When medicul deschide fișa pacientului în E-06
     Then serverul returnează adherencePercent: null și expected: 0, iar interfața afișează eticheta „Nicio doză evaluabilă”
     ```
  3. **Accesibilitate & Securitate — Tabel alternativ și izolare pacient:**
     ```gherkin
     Given consola E-06 și un medic neasignat unui pacient
     When se randează rezumatul aderenței sau alt medic apelează endpoint-ul
     Then E-06 dublează graficul cu un tabel paginat accesibil {items, page, size, total}, iar medicul neasignat primește 403/404
     ```
- **Criterii Non-Funcționale & DoD:** Rotunjire la 1 zecimală; dozele protejate prin *Forgiveness Day* continuă să apară ca `MISSED` în sumarul clinic al medicului; suită unitară `JUnit 5 + Mockito` cu `Clock.fixed(...)`.

---

## 6. EPIC 4 — Portalul Pacientului, Gamificare Non-Punitivă și Telemedicină

### `US-09`: Confirmarea Dozei Curente în ≤ 2 Click-uri cu Calcul Dinamic și Idempotență
- **Metadate:** EPIC 4 · **Prioritate:** Must Have · **Estimare:** 8 SP · **Fereastră:** Fereastra 1 (EPIC Review, 16 dec)
- **Responsabili Echipa 19:** Dobrescu Andrei-Alexandru (BE) & Iovan Andrei-Alexandru (FE)
- **Ecrane & Endpoint-uri REST:** `E-02` · `GET /api/adherence/today`, `POST /api/adherence/confirm` · **Tabele DB:** `adherence_logs`, `gamification_profiles`
- **Trasabilitate:** `CF-PAC-01`, `CF-PAC-02`, `CQ-UTI-02`, `SA-07`, `SP-05`, `E1-DO-01`, `E1-IO-03`, `INT-03`

> **Ca** pacient (Dl. Ion Popescu, 68 ani),  
> **vreau să** văd imediat după autentificare cardul cu următoarea picătură și să o confirm printr-un singur click,  
> **astfel încât** raportarea dozei să dureze maximum 2 click-uri în total (login + confirmare), fără dublare la apăsări repetate.

- **Scenarii Gherkin:**
  1. **Flux principal — ≤ 2 click-uri, calcul dinamic `Europe/Bucharest` și fereastră `±120 min`:**
     ```gherkin
     Given pacientul are plan activ cu doza 10:00 (fără cron nocturn; calculată dinamic din schedule_hours) și se autentifică din E-01 (Click 1 -> E-02)
     When apasă butonul gigant [CONFIRMĂ DOZA ACUM] (Click 2) la 10:05 trimițând {planId, scheduledFor}
     Then statusul devine CONFIRMED_ON_TIME (+10 puncte), CONFIRMED_LATE (+5 puncte) dacă e peste +120 min în aceeași zi, progresul se actualizează (ex. 50% -> 100%) și aria-live anunță confirmarea
     ```
  2. **Caz-limită / Concurență — Idempotență la dublu-click sau două taburi:**
     ```gherkin
     Given pacientul a confirmat deja doza pentru perechea (planId, scheduledFor)
     When face dublu-click rapid sau retrimite cererea după pierderea răspunsului
     Then constrângerea UNIQUE (plan_id, scheduled_for) și blocarea tranzacțională returnează 200 OK cu timestampul și punctele inițiale (fără puncte sau rânduri duble), iar butonul UI se dezactivează pe durata cererii
     ```
  3. **Validare / Eroare — Fereastră închisă (`422`) și 3 stări distincte în `E-02`:**
     ```gherkin
     Given o cerere trimisă pentru o doză prea devreme sau din altă zi, ori un pacient fără plan activ / cu toate dozele confirmate
     When backendul evaluează prin Clock și E-02 randează GET /api/adherence/today
     Then cererea neeligibilă primește 422 DOSE_WINDOW_CLOSED, iar E-02 afișează clar una dintre cele 3 stări distincte (doză disponibilă / tratament complet azi / fără plan activ)
     ```
- **Criterii Non-Funcționale & DoD:** Răspuns JSON include `dailyProgressPercent`, `currentStreak`, `forgivenessShields`, `totalPoints`, `rewardsSwitchEnabled`; legare `INT-03` (4–5 dec) + teste unitare `JUnit 5 + Mockito`.

---

### `US-10`: Seria de Aderență (*Treatment Streak*) și Scutul Non-Punitiv *Forgiveness Day*
- **Metadate:** EPIC 4 · **Prioritate:** Must Have (bază în F1) / Should Have (scuturi în Val 1) · **Estimare:** 8 SP · **Fereastră:** F1 bază + F2 Val 1 (12–23 ian)
- **Responsabili Echipa 19:** Dobrescu Andrei-Alexandru (BE) & Iovan Andrei-Alexandru (FE)
- **Ecrane & Endpoint-uri REST:** `E-02` · `GET /api/adherence/today`, `POST /api/adherence/confirm` · **Tabel DB:** `gamification_profiles`
- **Trasabilitate:** `CF-PAC-02`, `CF-ADM-03`, `CQ-PER-02`, `SA-13`, `SP-09`, `CT-01`

> **Ca** pacient cu patologie cronică de lungă durată (Dl. Ion Popescu),  
> **vreau să** câștig un *Scut de Continuitate* la fiecare 7 zile perfecte care să îmi salveze seria la o singură zi omisă accidental,  
> **astfel încât** progresul să nu se prăbușească punitiv și să fiu încurajat pozitiv să continui tratamentul.

- **Scenarii Gherkin:**
  1. **Flux principal — Acordare scut la 7 zile și protecție în ziua 8 omisă:**
     ```gherkin
     Given pacientul are 7 zile consecutive complet confirmate (current_streak = 7, forgiveness_shields = 1, plafon maxim 2)
     When omite ziua 8 și accesează E-02 în ziua 9, apoi confirmă integral ziua 9
     Then la deschiderea E-02 scutul se consumă automat (streak = 7, shields = 0, fără puncte adăugate pe ziua 8), iar la finalizarea zilei 9 seria crește la 8
     ```
  2. **Caz-limită / Idempotență — Reconstrucție deterministă și resetare la a 2-a zi omisă:**
     ```gherkin
     Given ziua 8 a fost omisă și protejată de scut
     When pacientul reîncarcă GET /api/adherence/today de mai multe ori
     Then proiecția reconstruită din istoricul imuabil afișează de fiecare dată streak = 7, shields = 0 (zero dublă debitare); dacă lipsește și ziua 9 (sau 0 scuturi), current_streak revine la 0 păstrând best_streak, total_points și ecusoanele
     ```
  3. **Caz-limită — Zi fără doze prescrise & inițializare profil:**
     ```gherkin
     Given o zi calendaristică fără niciun plan activ sau un pacient aflat la prima accesare fără rând în gamification_profiles
     When rulează evaluarea zilei
     Then profilul este inițializat idempotent, iar ziua fără doze rămâne neutră pentru serie/scuturi existente (întrerupând doar șirul de 7 zile pentru un scut nou)
     ```
- **Criterii Non-Funcționale & DoD:** `CHECK (forgiveness_shields BETWEEN 0 AND 2)`, `best_streak >= current_streak`; citirea tabloului de bord nu emite niciodată vouchere; teste unitare pentru zilele 6, 7, 14, plafon 2 și omisiuni duble.

---

### `US-11`: Autotest Macular pe Grila Amsler (`20×20`), Clasificare Severitate și Preluare Alertă Medic
- **Metadate:** EPIC 4 · **Prioritate:** Should Have · **Estimare:** 8 SP · **Fereastră:** Fereastra 2 — Valul 1 (12–23 ian)
- **Responsabili Echipa 19:** Adrian Diaconescu (BE), Dobrescu Andrei-Alexandru (Scor) & Iovan Andrei-Alexandru (FE)
- **Ecrane & Endpoint-uri REST:** `E-03`, `E-06` · `POST/GET /api/amsler-tests`, `GET /api/amsler-alerts`, `PUT /api/amsler-alerts/{testId}/acknowledgment` · **Tabel DB:** `amsler_tests`
- **Trasabilitate:** `CF-PAC-03`, `CF-MED-04`, `SA-10`, `SA-11`, `SP-08`, `CAP-02`

> **Ca** pacient monitorizat pentru DMLV sau medic oftalmolog curant,  
> **vreau ca** testul macular efectuat interactiv pe Grila Amsler `20×20` să clasifice automat severitatea anomaliilor și să deschidă o alertă prioritară preluabilă o singură dată de medic,  
> **astfel încât** modificările foveale critice să fie triate rapid și trasabil între vizite.

- **Scenarii Gherkin:**
  1. **Flux principal — Test normal vs. alertă `CRITICA_URGENTA` / `MODERATA_MONITORIZARE`:**
     ```gherkin
     Given pacientul selectează ochiul OD sau OS în E-03 (AmslerGridSVG 20x20, coordonate 0..19)
     When trimite celule distorsionate din zona centrală (rând 7..12 și coloană 7..12, ex. [7,12]) sau doar periferice (ex. [7,13])
     Then serverul clasifică CRITICA_URGENTA respectiv MODERATA_MONITORIZARE, deschide alerta în E-06 (sortată prioritar), iar pentru 0 celule salvează NORMAL_FARA_DISTORSIUNI și adaugă ecusonul la prima participare
     ```
  2. **Caz-limită / Concurență — Preluare unică/idempotentă de către medic:**
     ```gherkin
     Given o alertă Amsler deschisă (alert_acknowledged_by IS NULL) pentru un pacient al Dr. Ionescu
     When Dr. Ionescu trimite PUT /api/amsler-alerts/{testId}/acknowledgment (și eventual o a doua cerere concurentă)
     Then actualizarea condiționată setează alert_acknowledged_by = doctorId și alertOpen = false; reluarea de același medic păstrează decizia inițială, alt medic este respins, iar istoricul testului rămâne intact
     ```
  3. **Validare / Accesibilitate — Navigare tastatură, mobil `360 px` și coordonate invalide:**
     ```gherkin
     Given pacientul folosește săgețile pe telefon la 200% zoom sau un client trimite coordonate duplicate ori în afara [0..19]
     When interacționează cu E-03 sau trimite POST /api/amsler-tests
     Then E-03 permite selectarea precisă fără scroll orizontal global, iar backendul normalizează duplicatele și respinge cu 400/422 coordonatele invalide
     ```
- **Criterii Non-Funcționale & DoD:** Regula `CAP-02` (orice celulă marcată deschide alertă; testele rămân deschise la restartul serverului); teste de frontieră `[7,12]` vs `[7,13]` în `JUnit 5`.

---

### `US-12`: Micro-Provocare Igienă Vizuală (`Regula 20-20-20`) și Ecusoane Educative
- **Metadate:** EPIC 4 · **Prioritate:** Could Have · **Estimare:** 3 SP · **Fereastră:** Fereastra 2 — Valul 1 (12–23 ian)
- **Responsabili Echipa 19:** Iovan Andrei-Alexandru (FE) & Dobrescu Andrei-Alexandru (BE)
- **Ecrane & Endpoint-uri REST:** `E-03`, `E-02` · **Tabel DB:** `gamification_profiles` (`unlocked_badges`)
- **Trasabilitate:** `CF-PAC-04`, `CQ-UTI-01`, `CQ-PER-02`

> **Ca** pacient care utilizează frecvent ecrane digitale,  
> **vreau să** rulez direct din portal cronometrul accesibil pentru Regula `20-20-20` (pauză de 20 secunde privind la 6 metri distanță la fiecare 20 minute),  
> **astfel încât** să reduc oboseala oculară fără penalizări și să câștig ecusoane educative de prevenție.

- **Scenarii Gherkin:**
  1. **Flux principal — Pornire, pauză și notificare multimodală:**
     ```gherkin
     Given pacientul pe fila 20-20-20 din E-03 cu buton [START PAUZĂ] >= 48x48 px
     When pornește cronometrul de 20 secunde (putând pune pauză sau relua)
     Then numărătoarea afișată cu font mare și contrast ridicat ajunge la 00:00 și declanșează atât semnal vizual, cât și sunet opțional
     ```
  2. **Caz-limită / Accesibilitate — Utilizare cu sunet oprit:**
     ```gherkin
     Given un pacient cu deficiență auditivă sau sunetul dezactivat în browser
     When cronometrul 20-20-20 finalizează pauza de 20 secunde
     Then finalizarea este semnalizată clar vizual și anunțată prin aria-live fără a depinde exclusiv de audio
     ```
  3. **Idempotență — Deblocare ecusoane non-punitivă:**
     ```gherkin
     Given pacientul finalizează sesiunile zilnice de prevenție sau întrerupe o sesiune
     When se înregistrează progresul în unlocked_badges
     Then codul ecusonului se adaugă unic în lista distinctă JSONB, fără clasamente publice și fără penalizări la întrerupere
     ```
- **Criterii Non-Funcționale & DoD:** Integrare curată în `E-03` lângă Grila Amsler; verificare manuală contrast și `aria-live` (`CT-04`).

---

### `US-13`: Emitere Idempotentă Vouchere (`5%/10%/15%`), Portofel Pacient și Decontare Atomică
- **Metadate:** EPIC 4 · **Prioritate:** Should Have · **Estimare:** 8 SP · **Fereastră:** Fereastra 2 — Valul 1 (12–23 ian)
- **Responsabili Echipa 19:** Dobrescu Andrei-Alexandru (BE), Adrian Diaconescu (BE) & Iovan Andrei-Alexandru (FE)
- **Ecrane & Endpoint-uri REST:** `E-04`, `E-05` · `GET /api/vouchers`, `POST /api/vouchers/validate`, `POST /api/vouchers/redeem` · **Tabele DB:** `discount_vouchers`, `appointments`
- **Trasabilitate:** `CF-PAC-05`, `CF-REC-03`, `CQ-PER-02`, `SA-14`, `SP-09`, `CT-01`

> **Ca** pacient fidel sau recepționer la decontare,  
> **vreau ca** pragurile de puncte (300 / 600 / 1000) să emită exact un voucher per prag, iar acesta să poată fi validat și aplicat o singură dată unei programări,  
> **astfel încât** sistemul de reduceri comerciale să fie complet protejat la dublu-click, concurență și expirare.

- **Scenarii Gherkin:**
  1. **Flux principal & Concurență — Emitere idempotentă la praguri (`Switch == ON`):**
     ```gherkin
     Given clinic_settings.rewards_switch_enabled = TRUE și pacientul încheie o zi completă depășind 300 (5%), 600 (10%) sau 1000 (15%) puncte
     When două confirmări concurente încearcă emiterea voucherului pe același prag
     Then INSERT INTO discount_vouchers ... ON CONFLICT (patient_id, points_milestone) DO NOTHING garantează exact 1 voucher ACTIVE (cod unic <= 24 car., valabil 90 zile, procent înghețat la emitere)
     ```
  2. **Caz-limită / Concurență — Decontare atomică la recepție (`E-05`):**
     ```gherkin
     Given un voucher ACTIVE (now < expires_at) și o programare a aceluiași pacient fără voucher aplicat
     When recepționerul trimite POST /api/vouchers/redeem cu {voucherCode, appointmentId} (sau două recepții concurente încearcă aplicarea pe programări diferite)
     Then tranzacția blochează voucherul și programarea în ordine unică, setează status=REDEEMED, redeemed_at=NOW() și applied_voucher_id; reluarea pe aceeași pereche e idempotentă, iar aplicarea pe o a doua programare primește 409
     ```
  3. **Validare / Temporal — Expirare strictă (`now >= expires_at`) și unicitate pe prag:**
     ```gherkin
     Given un voucher cu expires_at depășit conform orei serverului
     When pacientul deschide E-04 sau recepția apelează validate/redeem
     Then portofelul și validarea afișează EXPIRED, decontarea este refuzată cu 409/422, iar pacientul nu poate primi al doilea voucher pe același prag după expirare
     ```
- **Criterii Non-Funcționale & DoD:** Constrângeri `UNIQUE (patient_id, points_milestone)`, `UNIQUE voucher_code`, `UNIQUE (applied_voucher_id)`; teste unitare frontiere `299/300`, `599/600`, `999/1000`.

---

## 7. EPIC 5 — Guvernanță Clinică, Control Comercial și Audit GDPR Art. 9

### `US-14`: Gestiunea Conturilor Personalului Clinic (`DOCTOR`/`RECEPTIONIST`) și Suspendarea Imediată
- **Metadate:** EPIC 5 · **Prioritate:** Should Have · **Estimare:** 5 SP · **Fereastră:** Fereastra 2 — Valul 2 (24 ian – 3 feb)
- **Responsabili Echipa 19:** Adrian Diaconescu (BE) & Iovan Andrei-Alexandru (FE)
- **Ecrane & Endpoint-uri REST:** `E-07` · `POST/GET /api/admin/staff`, `PATCH /api/admin/staff/{id}` · **Tabel DB:** `users`
- **Trasabilitate:** `CF-ADM-01`, `CQ-SEC-01`, `SA-12`

> **Ca** administrator al clinicii (Andrei Marinescu),  
> **vreau să** creez, listez și activez sau suspend conturile medicilor și recepționerilor din panoul administrativ,  
> **astfel încât** accesul personalului medical să reflecte fidel structura echipei clinicii și să fie revocat instant la plecarea unui angajat.

- **Scenarii Gherkin:**
  1. **Flux principal — Creare cont personal cu predare unică credențiale:**
     ```gherkin
     Given administratorul autentificat ADMIN în ecranul E-07
     When trimite POST /api/admin/staff cu {email, role: "DOCTOR"} (sau "RECEPTIONIST")
     Then contul este creat activ cu parolă BCrypt, credențialele inițiale sunt livrate o singură dată cu no-store, iar lista paginată GET /api/admin/staff afișează noul membru
     ```
  2. **Caz-limită / Securitate — Suspendare cu revocare imediată a JWT-ului activ:**
     ```gherkin
     Given un medic autentificat cu token JWT încă în termenul de 15 minute
     When administratorul trimite PATCH /api/admin/staff/{id} cu {isActive: false}
     Then orice cerere ulterioară a medicului (inclusiv cu vechiul JWT) este blocată imediat de filtrul Spring Security
     ```
  3. **Validare / Securitate — Roluri permise și izolare endpoint admin:**
     ```gherkin
     Given un e-mail duplicat, încercare de creare cu rol invalid sau un apel din partea unui DOCTOR/PATIENT/RECEPTIONIST
     When se apelează endpoint-urile /api/admin/staff*
     Then e-mailul duplicat primește 409, rolul nepermis primește 422, iar actorii non-ADMIN primesc 403 Forbidden
     ```
- **Criterii Non-Funcționale & DoD:** Lista staff paginată stabil `{items, page, size, total}`; testare manuală + unitară pentru suspendare instantanee și protecție RBAC.

---

### `US-15`: Comutatorul Global **Switch ON/OFF** Recompense Comerciale (Menținând Voucherele `ACTIVE`)
- **Metadate:** EPIC 5 · **Prioritate:** Should Have · **Estimare:** 5 SP · **Fereastră:** Fereastra 2 — Valul 2 (24 ian – 3 feb)
- **Responsabili Echipa 19:** Adrian Diaconescu (BE), Dobrescu Andrei-Alexandru (Reguli) & Iovan Andrei-Alexandru (FE)
- **Ecrane & Endpoint-uri REST:** `E-07`, `E-04`, `E-05` · `GET/PUT /api/admin/rewards-switch` · **Tabele DB:** `clinic_settings`, `discount_vouchers`
- **Trasabilitate:** `CF-ADM-02`, `CF-ADM-03`, `SA-14`, `SP-09`

> **Ca** administrator al clinicii (Andrei Marinescu),  
> **vreau să** activez sau să opresc global emiterea voucherelor comerciale printr-un comutator **Switch ON/OFF**, fără a anula voucherele deja câștigate de pacienți,  
> **astfel încât** clinica să își controleze politica financiară flexibil, menținând neatinse motivația prin *Streak*/ecusoane și încrederea pacienților.

- **Scenarii Gherkin:**
  1. **Flux principal — Comutare pe `OFF` oprește emiterea nouă, dar păstrează voucherele `ACTIVE`:**
     ```gherkin
     Given administratorul setează PUT /api/admin/rewards-switch cu {enabled: false}
     When pacientul acumulează puncte noi atingând pragul de 600 puncte, în timp ce deține deja un voucher ACTIVE emis anterior la 300 puncte
     Then punctele, seria, scuturile și ecusoanele continuă să crească fără a emite voucherul de 600, iar voucherul ACTIVE de 300 rămâne vizibil în E-04 și decontabil la recepția E-05 până la expirare
     ```
  2. **Caz-limită / Concurență — Blocare exclusivă fără cache învechit & revenire `ON`:**
     ```gherkin
     Given o confirmare concurentă cu comanda adminului de trecere pe OFF sau o revenire ulterioară a Switch-ului pe ON
     When rândul singleton clinic_settings (clinic_id = 1) este actualizat cu blocare exclusivă
     Then niciun emitent după commit nu citește o valoare ON din cache, iar revenirea pe ON nu declanșează emitere în masă (pragurile neîncasate se reevaluează la următoarea zi completă)
     ```
  3. **Validare / Accesibilitate — Idempotență și stare textuală în `E-07`:**
     ```gherkin
     Given repetarea aceleiași valori PUT /api/admin/rewards-switch sau afișarea în E-07
     When se execută cererea sau se randează interfața
     Then operația este idempotentă (200 OK cu {enabled, updatedAt}), iar E-07 exprimă textual exact impactul comutatorului asupra emiterii și valabilității voucherelor existente
     ```
- **Criterii Non-Funcționale & DoD:** `CHECK (clinic_id = 1)` în `V7__clinic_settings_and_audit.sql`; testare unitară dedicată interacțiunii `Switch OFF` + `ACTIVE` redeem.

---

### `US-16`: Jurnalul Imutabil (*Append-Only*) de Audit GDPR Art. 9 prin Spring AOP și Consultare Paginată
- **Metadate:** EPIC 5 · **Prioritate:** Should Have · **Estimare:** 4 SP · **Fereastră:** Fereastra 2 — Valul 2 (24 ian – 3 feb)
- **Responsabili Echipa 19:** Adrian Diaconescu (BE) & Iovan Andrei-Alexandru (FE)
- **Ecrane & Endpoint-uri REST:** `E-07` · `GET /api/admin/audit-logs` · **Tabel DB:** `gdpr_audit_logs`
- **Trasabilitate:** `CF-ADM-04`, `CQ-SEC-02`, `SA-15`, `SP-10`

> **Ca** administrator al clinicii (Andrei Marinescu),  
> **vreau ca** orice operațiune de citire sau modificare a datelor clinice de sănătate (GDPR Art. 9) să genereze automat o urmă imutabilă în jurnalul de audit consultabil în `E-07`,  
> **astfel încât** să pot verifica trasabilitatea completă a accesului la dosarele pacienților.

- **Scenarii Gherkin:**
  1. **Flux principal — Audit AOP automat la citire și scriere clinică:**
     ```gherkin
     Given metodele de serviciu clinic adnotate cu @AuditedClinicalAction
     When Dr. Elena Ionescu citește fișa unui pacient, salvează o consultație sau prescrie un tratament
     Then aspectul GdprAuditAspect (executat în aceeași tranzacție după autorizare) inserează în gdpr_audit_logs {actor_user_id, target_patient_id, action_type, ip_address, created_at} cu timestamp de server
     ```
  2. **Caz-limită / Fail-Closed — Tranzacție unică cu blocare la eroare de audit:**
     ```gherkin
     Given apare o eroare la persistarea rândului în gdpr_audit_logs în timpul unei citiri/scrieri clinice
     When tranzacția încearcă să livreze DTO-ul medical
     Then întreaga operație eșuează controlat (rollback) și nicio dată de sănătate nu este divulgată fără jurnalizare
     ```
  3. **Securitate & Imutabilitate — Protecție Append-Only și filtrare în `E-07`:**
     ```gherkin
     Given rolul de aplicație PostgreSQL și tabelul de audit din E-07
     When se încearcă UPDATE/DELETE/TRUNCATE pe gdpr_audit_logs sau filtrare după pacient/actor/interval
     Then trigger-ele și privilegiile DB din V7 blochează modificarea/ștergerea, iar ADMIN citește paginat în ordine stabilă (created_at, id)
     ```
- **Criterii Non-Funcționale & DoD:** Date exclusiv sintetice în seed și demonstrații; liste clinice identificate cu pacientul expus; testare AOP + restricție non-admin (`403`).

---

## 8. Planul de Livrare pe Ferestre (`16 dec` vs. `12 ian – 3 feb`), Roluri și Criterii DoR/DoD

### 8.1. Împărțirea pe Ferestre de Livrare și Responsabilități la Prezentare

| Fereastră / Val | Interval | User Stories incluse | Capacitate | Scop Demonstrabil & Roluri Echipa 19 |
|---|---|---|---|---|
| **Fereastra 1 — EPIC Review** *(Stabilizare 8–15 dec)* | **24 nov – 16 dec 2026** | `US-01` – `US-09` + baza *Streak* din `US-10` | **48 SP *(+bază)*** | **Flux complet E-05 → E-06 → E-01/E-02 → E-06** pe `PostgreSQL 16` (Docker) + BE & FE nativ local:<br>• **Adrian:** `V1..V7`, JWT/RBAC, CNP Modulo 11, Căutare `<300ms`, Programări GiST, Consultație & Planuri;<br>• **Iovan:** `E-01` + `CIC-06.1` (`200%`, High-Contrast), `E-02` (≤2 click-uri), `E-05`, `E-06` legat treptat;<br>• **Dobrescu:** `GET /today`, `POST /confirm` (`±120m`, dublu-click), `Basic Streak`, `GET /summary`, `JUnit 5 + Mockito`. |
| **Fereastra 2 — Valul 1** *(Sprint 4 + 3 zile debug)* | **12 – 23 ian 2027** | `US-10` (scuturi), `US-11`, `US-12`, `US-13` | **27 SP** | **Gamificare completă, Vouchere & Amsler:** Scuturi *Forgiveness Day*, emitere idempotentă vouchere (`300/600/1000`), portofel `E-04`, decontare `E-05`, `AmslerGridSVG` + `20-20-20` (`E-03`) și alerte medic (`E-06`). |
| **Fereastra 2 — Valul 2** *(Sprint 5 + 4 zile debug)* | **24 ian – 3 feb 2027** | `US-14`, `US-15`, `US-16` + Docker complet | **14 SP** | **Guvernanță Admin, GDPR & Review Final:** Gestiune personal, comutator **Switch ON/OFF** (menținând vouchere `ACTIVE`), audit AOP `gdpr_audit_logs`, containerizare completă `FE + BE + DB` și rafinare CI. |

### 8.2. Criterii Globale *Definition of Ready (DoR)* și *Definition of Done (DoD)*

| *Definition of Ready (DoR)* — înainte de a începe un US | *Definition of Done (DoD)* — pentru marcarea US-ului ca finalizat |
|---|---|
| 1. Contractul REST, structura DTO și codurile de eroare sunt fixate în `ARCHITECTURE_AND_EXECUTION_PLAN.md` (§5). | 1. Codul respectă izolarea strictă a modulelor (`academic/backend/`, verificat prin ArchUnit `ModuleArchitectureTest`) și folderul `academic/frontend/`. |
| 2. Migrările PostgreSQL (`V1..V7`) pentru tabelele vizate sunt aplicate și validate în containerul Docker (`ddl-auto=validate`). | 2. Logica de domeniu are **1–2 teste unitare scrise manual** ca șablon + cazurile-limită extinse asistat de AI (`./mvnw -B test` verde offline fără DB pornit). |
| 3. Fișierele de mock data JSON corespunzătoare din `academic/frontend/src/mocks/` sunt disponibile pentru lucrul în paralel al Frontendului. | 3. Frontendul trece `npm run build`, respectă WCAG 2.1 AA (`18px`, pași `--font-scale`, ținte `≥ 48×48 px`), legat la API live și verificat manual în browser (`CT-04`). |
| 4. Owner-ul principal și punctul de legare FE–BE (`INT-01..03`) sunt stabilite în `EXECUTION_BOARD_FEREASTRA_1.md`. | 4. Modificările sunt integrate printr-un PR mic cu CI verde (`build + unit tests`), fără a atinge căile legacy din repo. |
