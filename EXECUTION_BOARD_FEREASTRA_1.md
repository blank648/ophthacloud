# OphthaCloud — Board de Execuție & Checklist Echipa 19 (Fereastra 1: 24 nov – 16 dec 2026)

> **STATUT DOCUMENT:** **APROBAT DE ECHIPA 19 — EXECUȚIE ACTIVĂ (Etapa 0 finalizată și integrată pe `main` la 25 nov 2026).**  
> **Ghid operațional de start rapid pentru colegi & CLI tools:** [academic/TEAM_PLAYBOOK.md](academic/TEAM_PLAYBOOK.md) · Pe parcursul lucrului, bifați direct căsuțele `- [ ]` pe măsură ce fiecare livrabil atinge Definition of Done.

**Documente de referință (Sursa unică de adevăr):**
- Specificația tehnică completă: [ARCHITECTURE_AND_EXECUTION_PLAN.md](ARCHITECTURE_AND_EXECUTION_PLAN.md)
- Rezumatul executiv al echipei: [PLAN_PE_SCURT_ECHIPA_19.md](PLAN_PE_SCURT_ECHIPA_19.md)
- Ghidul practic de execuție (Quickstart, Git & AI Prompts): [academic/TEAM_PLAYBOOK.md](academic/TEAM_PLAYBOOK.md)

---

## 1. Context, Reguli Obligatorii & Jaloane Critice

- **Fereastra curentă:** 24 noiembrie – 16 decembrie 2026 (**EPIC Review** cu Prof. dr. Radu Gramatovici).
- **Mentori de Master (Echipa Master 20):** Ignat Eduardo & Pantaziu Andrei-Rareș.
- **Prioritatea #1 la prezentare:** Fiecare dintre cei 3 membri demonstrează live funcționalitățile proprii, cu accent prioritar pe **UX-ul Pacientului** (WCAG 2.1 AA, scalare text 100%–200%, High-Contrast, confirmare picătură în ≤2 click-uri de la login).

**Reguli operaționale agreate cu mentorii de Master (24 noiembrie):**
1. **Docker în decembrie = DOAR `PostgreSQL 16` (`docker compose up -d`):** Backendul Spring Boot și Frontendul React/Vite rulează **nativ local**; containerizarea completă (`FE + BE + DB`) se mută în Valul 2 (ianuarie).
2. **CI minimal acum:** Un simplu workflow GitHub Actions (`build + unit tests` pe PR/MR); rafinarea avansată a CI-ului se lasă spre final.
3. **ZERO deployment extern / cloud** în această fereastră.
4. **Focus exclusiv pe teste unitare (`JUnit 5 + Mockito`):** Fără teste de integrare (Testcontainers/MockMvc) sau E2E (Playwright) obligatorii. Echipa scrie **1–2 teste unitare manual** ca șablon, apoi extinde restul asistat de AI + verificare manuală în browser pe traseul cap-la-cap.
5. **Lucru în paralel + mock data + legare timpurie FE–BE:** Ecranele Frontend pornesc pe mock data JSON și se leagă incremental la API imediat ce endpoint-urile devin gata.

### Jaloane Critice (Fereastra 1)

| Dată | Jalon | Obiectiv verificabil |
|---|---|---|
| **25 nov** | **Deblocare Etapa 0** | DB PostgreSQL 16 în Docker, CI minimal, schelet Spring Boot + Flyway `V1..V7`, interfață `TreatmentPlanQueryService` și `src/mocks/*.json` pe `main`. |
| **28 nov** | **`INT-01` — Legare Login/JWT** | `E-01` autentifică pe JWT real (`POST /api/auth/login`) și sincronizează preferințele de accesibilitate (`uiFontScale`, `highContrast`). |
| **1–2 dec** | **`INT-02` — Legare Recepție & Medic** | `E-05` și `E-06` (fără graficul de aderență încă) salvează pacientul, programarea, consultația și rețeta în PostgreSQL 16. |
| **4–5 dec** | **`INT-03` — Legare Pacient & Aderență** | `E-02` și sumarul din `E-06` rulează pe API-ul real (`VITE_USE_MOCKS=false`); confirmarea pacientului apare imediat la medic. |
| **7 dec** | **Feature Freeze (20:00)** | Toate cele 4 ecrane (`E-01`, `E-02`, `E-05`, `E-06`) legate complet pe backendul real și PostgreSQL 16; listă de defecte deschisă. |
| **8–15 dec** | **Rezerva de Debug & 2 Repetiții** | 8 zile exclusiv pentru remediere bug-uri, verificare `360 px` & `200%`, performanță `<300 ms` pe 10.000 fișe, seed demo și 2 repetiții cap-la-cap. |
| **16 dec** | **EPIC Review** | Demonstrație live în fața Prof. dr. Radu Gramatovici (`E-05 → E-06 → E-01/E-02 → E-06`), zero erori `5xx`. |

---

## 2. Board de Execuție (WBS) cu Task-uri Bifabile

> **Convenție Git:** Branch principal protejat `main`; PR-uri mici cu CI verde (`build + unit tests`).

### Etapa 0 — Deblocare (24–25 nov)

- [x] **`E0-01`** | **Adrian** | Branch: `feat/e0-stage0-foundation` | Dep: — | Termen: **24 nov** *(Finalizat 25 nov)*
  - **DoD:** `academic/docker-compose.yml` pornește **exclusiv PostgreSQL 16** (port `5435`) cu volum + healthcheck (`docker compose -f academic/docker-compose.yml up -d`); workflow GitHub Actions minimal (`./mvnw -B test` + `npm ci && npm run build` la PR) activ pe `main`.
- [x] **`E0-02`** | **Adrian** | Branch: `feat/e0-stage0-foundation` | Dep: `E0-01` | Termen: **25 nov (prânz)** *(Finalizat 25 nov)*
  - **DoD:** Schelet Spring Boot 3.3 (Java 21) cu cele 4 pachete (`patientbooking`, `clinicalamsler`, `gamification`, `securityaudit`) + reguli ArchUnit, `Clock` injectabil (`Europe/Bucharest`), erori standardizate și migrările **Flyway `V1..V7`** aplicate curat cu `ddl-auto=validate`.
- [x] **`E0-03`** | **Adrian** | Branch: `feat/e0-stage0-foundation` | Dep: `E0-02` | Termen: **25 nov (seara)** *(Finalizat 25 nov)*
  - **DoD:** Interfața internă `TreatmentPlanQueryService` + DTO-uri + entități `gamification` predate lui Dobrescu; schelet Vite/React + pachetul `mocks/*.json` (UUID-uri coerente) + proxy `/api` predate lui Iovan; suita șablon `CnpModulo11ValidatorTest` (9 teste) pe repo.

### Etapa 1 — Dezvoltare în paralel & 3 puncte de legare timpurie FE–BE (26 nov – 5 dec)

- [x] **`E1-AD-01`** | **Adrian** | Branch: `feat/security-jwt-auth` | Dep: `E0-02` | Termen: **27 nov** *(Finalizat 26 nov)*
  - **DoD:** Spring Security 6 + JWT (`15 min`, `sub=userId`, `role`) + verificare `is_active`; `POST /api/auth/login`, `GET /api/me`, `PATCH /api/me/preferences` + teste unitare RBAC/login.
- [ ] **`E1-IO-01`** | **Iovan** | Branch: `feat/fe-shell-e01-a11y` | Dep: `E0-03` | Termen: **27 nov**
  - **DoD:** Client HTTP cu switch `VITE_USE_MOCKS`, `E-01` (Login) și bara persistentă `CIC-06.1` funcțională înainte și după login (`--font-scale` `1.0–2.0` / `100%–200%`, High-Contrast cu etichetă text, ținte ≥`48×48 px`).
- [ ] **`INT-01`** | **Iovan + Adrian** | Branch: `feat/int-01-auth-live` | Dep: `E1-AD-01`, `E1-IO-01` | Termen: **28 nov**
  - **DoD (Legare FE–BE #1 — Login & Preferințe):** `E-01` autentifică pe JWT real, rutează pe rol (`PATIENT`, `RECEPTIONIST`, `DOCTOR`) și sincronizează `uiFontScale`/`highContrast` în BD.
- [ ] **`E1-AD-02`** | **Adrian** | Branch: `feat/patientbooking-core` | Dep: `E1-AD-01` | Termen: **30 nov**
  - **DoD:** `CnpModulo11Validator` (1–2 teste scrise manual + cazuri-limită AI: rest 0, rest 10→1, lungime, dublură), `POST /api/patients` (fișă + user activ + credențiale inițiale `no-store` în aceeași tranzacție), `GET /api/patients/search` (`<300 ms`), `GET /api/doctors`, `GET/POST /api/appointments` (`409 APPOINTMENT_OVERLAP` pe GiST).
- [ ] **`E1-AD-03`** | **Adrian** | Branch: `feat/clinical-consult-plans` | Dep: `E1-AD-02` | Termen: **1 dec**
  - **DoD:** `POST/GET /api/consultations` (AV OD/OS, TIO mmHg, biomicroscopie, ICD-10, doar medicul programat) și `POST/GET /api/treatment-plans` (derivare `patient_id`, validare `schedule_hours` `HH:mm`, versionare `replacesPlanId`).
- [ ] **`E1-IO-02`** | **Iovan** | Branch: `feat/fe-e05-e06-clinical` | Dep: `E1-IO-01` | Termen: **1 dec**
  - **DoD:** Ecranele `E-05` (Recepție: validator CNP live, creare pacient cu afișare unică credențiale, căutare, agendă cu eroare explicită la suprapunere) și `E-06` (Medic: consultație, editor rețetă cu ore, tabel + grafic aderență) gata pe mock data.
- [ ] **`INT-02`** | **Iovan + Adrian** | Branch: `feat/int-02-clinical-live` | Dep: `E1-AD-03`, `E1-IO-02` | Termen: **1–2 dec**
  - **DoD (Legare FE–BE #2 — Recepție & Medic):** `E-05` și `E-06` (fără sumarul real de aderență încă) operează pe API-ul real și salvează pacientul, programarea, consultația și rețeta în PostgreSQL 16.
- [ ] **`E1-DO-01`** | **Dobrescu** | Branch: `feat/gamification-adherence` | Dep: `E0-03` | Termen: **3 dec**
  - **DoD:** `GET /api/adherence/today` (doze dinamice din `schedule_hours` în `[activated_at, stopped_at)`), `POST /api/adherence/confirm` (`±120 min` = `CONFIRMED_ON_TIME` 10p, `>+120 min` azi = `CONFIRMED_LATE` 5p, altfel `422 DOSE_WINDOW_CLOSED`, idempotent la dublu-click), *Basic Streak* și `GET /api/adherence/{patientId}/summary` + suită unitară `JUnit 5 + Mockito` (1–2 manuale + extindere AI).
- [ ] **`E1-IO-03`** | **Iovan** | Branch: `feat/fe-e02-patient-dash` | Dep: `E1-IO-01` | Termen: **3 dec**
  - **DoD:** `E-02` (Dashboard Pacient) pe mock data: card „Următoarea picătură”, confirmare în **≤2 click-uri de la login** (click 1 = submit login, click 2 = `[CONFIRMĂ DOZA ACUM]`), disable pe request în curs, anunț `aria-live`, bară progres `0–100%`, serie și stări distincte (fără plan / complet / în așteptare).
- [ ] **`INT-03`** | **Toți trei** | Branch: `feat/int-03-adherence-live` | Dep: `E1-DO-01`, `E1-IO-03`, `INT-02` | Termen: **4–5 dec**
  - **DoD (Legare FE–BE #3 — Pacient & Aderență):** `E-02` și sumarul din `E-06` folosesc API-ul real (`VITE_USE_MOCKS=false`); confirmarea pacientului se vede imediat în consola medicului.

### Etapa 2 — Feature Freeze, Rezerva de Debug & Demo (6–15 dec)

- [ ] **`E2-01`** | **Toți trei** | Branch: `release/epic-review-rc1` | Dep: `INT-03` | Termen: **6–7 dec**
  - **DoD:** **Feature Freeze (7 dec, ora 20:00):** Traseul `E-05 → E-06 → E-01/E-02 → E-06` legat cap-la-cap pe PostgreSQL 16 (Docker), CI verde, inventar de bug-uri deschis pentru stabilizare.
- [ ] **`DBG-01`** | **Toți trei** | Branch: `fix/dbg-cors-jwt-dto` | Dep: `E2-01` | Termen: **8–9 dec**
  - **DoD:** Remediere bug-uri CORS/proxy Vite, expirare JWT `401` fără bucle la confirmare, aliniere serializare JSON (`camelCase`, enum-uri, ISO-8601 UTC vs. `Europe/Bucharest`).
- [ ] **`DBG-02`** | **Dobrescu + Adrian** | Branch: `fix/dbg-concurrency-edges` | Dep: `DBG-01` | Termen: **10–11 dec**
  - **DoD:** Verificare dublu-click / 2 tab-uri la confirmare (1 singur rând în `adherence_logs`), suprapuneri programări (`409`), rețetă emisă la prânz (fără doză retroactivă `MISSED` la `08:00`) + completare unit tests cu AI.
- [ ] **`DBG-03`** | **Iovan + Adrian** | Branch: `fix/dbg-wcag-and-perf` | Dep: `DBG-02` | Termen: **12–13 dec**
  - **DoD:** Audit manual WCAG 2.1 AA pe desktop și mobil (`360 px`, text `100%/150%/200%`, contrast ≥`4.5:1`, navigare tastatură `Tab`/`Enter`) + probă performanță căutare `<300 ms` pe 10.000 fișe sintetice (`EXPLAIN ANALYZE`).
- [ ] **`DBG-04`** | **Toți trei** | Branch: `chore/demo-seed-and-rehearsal` | Dep: `DBG-03` | Termen: **14–15 dec**
  - **DoD:** Script de seed determinist pe BD rasă de la zero + **2 repetiții cronometrate cap-la-cap** (fiecare membru pe bucata lui), zero erori `5xx`, tag Git `v0.1-epic-review` înghețat înainte de **16 decembrie**.

---

## 3. Checklist Detaliat Etapa 0 (Adrian — 24–25 nov)

> **Notă de izolare (`SP-01`):** Repository-ul conține deja o bază de cod mai veche (`backend/ophthacloud-backend/`, `frontend/`, `docker/` cu Keycloak și Tauri). Pentru a păstra schema Flyway `V1..V7` curată, fără conflicte de porturi/dependențe și cu un CI rapid, modulul academic trebuie izolat într-o structură dedicată (de ex. `academic/backend`, `academic/frontend`, `academic/docker-compose.yml`) sau cu țintele curățate explicit.

### A. Infrastructură locală & CI minimal (`24 nov`)
- [x] `academic/docker-compose.yml` (strict serviciul `postgres:16-alpine`, baza `ophthacloud_academic` pe portul host `5435`, volum numit și healthcheck `pg_isready`, fără servicii Keycloak/MinIO/Redis/FE/BE, configurat în `academic/.env.example`)
- [x] `.github/workflows/ci.yml` — workflow minimal la `pull_request` și `push` pe `main`:
  - [x] Job Java 21 Temurin: `./mvnw -B test`
  - [x] Job Node 20: `npm ci && npm run build`

### B. Schelet Backend Spring Boot 3.3 + Migrări Flyway `V1..V7` (`24–25 nov`)
- [x] `pom.xml` (Spring Boot `3.3.x`, Java `21`, `spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `flyway-core`, `flyway-database-postgresql`, `postgresql`, `jjwt-api/impl/jackson`, `spring-boot-starter-test` cu JUnit 5 + Mockito + ArchUnit)
- [x] `src/main/resources/application.yml` (`spring.jpa.hibernate.ddl-auto=validate`, `spring.jpa.properties.hibernate.jdbc.time_zone=UTC`, `app.clinic-zone=Europe/Bucharest`)
- [x] Structura de pachete sub `ro.unibuc.fmi.ophthacloud`:
  - [x] `common/` (`ClockConfig` cu `Clock` injectabil, `ApiErrorResponse` `{code, message, fieldErrors, requestId}`, `GlobalExceptionHandler`)
  - [x] `securityaudit/`
  - [x] `patientbooking/`
  - [x] `clinicalamsler/`
  - [x] `gamification/`
- [x] Cele 7 migrări Flyway în `src/main/resources/db/migration/`:
  - [x] `V1__users.sql` (`users`, `LOWER(TRIM(email))`, `CHECK role`)
  - [x] `V2__patients.sql` (`patients`, `UNIQUE cnp`, `CHECK ui_font_scale BETWEEN 1.0 AND 2.0`, indecși B-Tree + `pg_trgm`)
  - [x] `V3__appointments.sql` (`CREATE EXTENSION IF NOT EXISTS btree_gist`, `appointments`, `EXCLUDE USING gist` pe intervale suprapuse per `doctor_id`)
  - [x] `V4__consultations_and_treatment_plans.sql` (`consultations`, `treatment_plans` cu `patient_id` denormalizat controlat, `activated_at`, `stopped_at`, index `(patient_id, start_date, end_date)`)
  - [x] `V5__adherence_and_amsler.sql` (`adherence_logs` cu `UNIQUE (plan_id, scheduled_for)`, `amsler_tests`)
  - [x] `V6__gamification_and_vouchers.sql` (`gamification_profiles`, `discount_vouchers` cu `UNIQUE (patient_id, points_milestone)`, FK `appointments.applied_voucher_id`)
  - [x] `V7__clinic_settings_and_audit.sql` (`clinic_settings` cu rândul `clinic_id = 1` pre-inserat `{"300":5,"600":10,"1000":15}`, `gdpr_audit_logs` + trigger append-only care blochează `UPDATE`/`DELETE`/`TRUNCATE`)

### C. Kit de deblocare pentru Dobrescu (`25 nov`)
- [x] Interfața publică + DTO în `clinicalamsler.api.TreatmentPlanQueryService`:
  - `List<TreatmentPlanSnapshotDto> findPlansForPatientInWindow(UUID patientId, LocalDate from, LocalDate to)` (câmpuri: `planId`, `patientId`, `medicationName`, `targetEye`, `dropsPerDose`, `List<String> scheduleHours`, `startDate`, `endDate`, `Instant activatedAt`, `Instant stoppedAt`, `boolean active`)
- [x] Scheletul entităților JPA (`AdherenceLogEntity`, `GamificationProfileEntity`) mapate pe `V5`/`V6` + DTO-urile exacte din §5.1 (`TodayAdherenceResponse`, `ConfirmDoseRequest`, `ConfirmDoseResponse`, `AdherenceSummaryResponse`)
- [x] 1 test unitar șablon (`CnpModulo11ValidatorTest.java` cu JUnit 5) ca referință de stil pentru extinderea asistată de AI

### D. Schelet Frontend + Mock Data JSON pentru Iovan (`25 nov`)
- [x] `vite.config.ts` cu proxy `/api` → `http://localhost:8080` și `.env.example` cu `VITE_USE_MOCKS=true`
- [x] Directorul `src/mocks/` cu același set canonic de UUID-uri pe toate fișierele (`patientId = "20000000-0000-4000-8000-000000000019"`, `doctorId = "30000000-0000-4000-8000-000000000019"`, `appointmentId = "40000000-0000-4000-8000-000000000019"`, `planId = "10000000-0000-4000-8000-000000000019"`):
  - [x] `src/mocks/auth.mock.json` — răspunsuri Login pe cele 3 roluri (`PATIENT`, `RECEPTIONIST`, `DOCTOR`) + `GET /api/me` (`uiFontScale: 1.0`, `highContrast: false`)
  - [x] `src/mocks/reception.mock.json` — `POST /api/patients` (cu `initialCredentials`), `GET /api/patients/search` (`items/page/size/total`), `GET /api/doctors`, `GET & POST /api/appointments`
  - [x] `src/mocks/clinical.mock.json` — `POST & GET /api/consultations`, `POST & GET /api/treatment-plans` (`scheduleHours: ["10:00", "20:00"]`, `targetEye: "AO"`, `dropsPerDose: 1`)
  - [x] `src/mocks/adherence.mock.json` — `GET /api/adherence/today` (cu 3 variante de stare UI: `has_next_dose`, `all_done_today`, `no_active_plan`), `POST /api/adherence/confirm`, `GET /api/adherence/{patientId}/summary`
  - [x] `src/mocks/errors.mock.json` — `409 PATIENT_EXISTS`, `409 APPOINTMENT_OVERLAP`, `422 DOSE_WINDOW_CLOSED`, `401 UNAUTHORIZED` în format `{code, message, fieldErrors, requestId}`
- [x] Verificare chei JSON obligatorii în `adherence.mock.json` (pentru legarea `INT-03` fără rescrieri):
  - `GET /api/adherence/today`: `{ clinicDate, nextDose: { planId, medicationName, targetEye, dropsPerDose, scheduledFor, localTime }, doses: [...], confirmedToday, expectedToday, dailyProgressPercent, currentStreak, totalPoints, rewardsSwitchEnabled }`
  - `POST /api/adherence/confirm`: request `{ planId, scheduledFor }` → response `{ status, confirmedAt, confirmedToday, expectedToday, dailyProgressPercent, currentStreak, forgivenessShields, totalPoints, rewardsSwitchEnabled }`
  - `GET /api/adherence/{patientId}/summary`: `{ from, to, expected, confirmedOnTime, confirmedLate, missed, pending, adherencePercent, history: { items, page, size, total } }`

---

## 4. Checklist-uri Individuale pe Rol (Brief-uri Operaționale)

### 4.1. Dobrescu Andrei-Alexandru — Backend `gamification` & Owner Teste Unitare
- **Branch:** `feat/gamification-adherence` | **Interval:** 26 nov – 3 dec (legare live `INT-03`: 4–5 dec)
- [ ] **`GET /api/adherence/today`:** Generează dinamic dozele zilei curente (`Clock` injectabil în `Europe/Bucharest`) din `TreatmentPlanQueryService` (`schedule_hours`), filtrate pe intervalul efectiv `[activated_at, stopped_at)`, și le suprapune peste `adherence_logs` (fără job cron nocturn).
- [ ] **`POST /api/adherence/confirm`:** Primește strict `{planId, scheduledFor}`; în fereastra `±120 min` salvează `CONFIRMED_ON_TIME` (+10p), peste `+120 min` până la sfârșitul zilei `CONFIRMED_LATE` (+5p), altfel returnează `422 DOSE_WINDOW_CLOSED`.
- [ ] **Idempotență & Basic Streak:** Confirmarea este 100% idempotentă la dublu-click sau cereri paralele pe `(planId, scheduledFor)` (păstrează confirmarea inițială și nu dublează punctele); actualizează seria de bază (*Basic Treatment Streak*) când toate dozele zilei sunt confirmate.
- [ ] **`GET /api/adherence/{patientId}/summary`:** Calculează procentul de aderență pentru medic raportat exclusiv la dozele scadente/confirmate (exclude dozele viitoare neconfirmate din ziua curentă; dacă numitorul este 0, returnează `null`) + listă paginată de istoric.
- [ ] **Teste unitare (`JUnit 5 + Mockito`):** Scrie manual **1–2 teste unitare** (fereastra `±120 min` și idempotența la dublu-click), apoi extinde cu AI restul cazurilor-limită (inclusiv rețetă emisă la prânz și sumar cu numitor 0).
- [ ] **Integrare & Stabilizare:** Leagă endpoint-urile reale în `INT-03` (4–5 dec) și verifică cazurile-limită în `DBG-02` (10–11 dec).

### 4.2. Iovan Andrei-Alexandru — Frontend SPA & Patient Accessibility UX
- **Branch-uri:** `feat/fe-shell-e01-a11y`, `feat/fe-e05-e06-clinical`, `feat/fe-e02-patient-dash` | **Interval:** 26 nov – 5 dec
- [ ] **Infrastructură FE & Mock Adaptor:** Client HTTP cu comutator `VITE_USE_MOCKS=true/false` pe baza `src/mocks/*.json` și gestionare curată a `401` fără bucle de reîncercare.
- [ ] **`E-01` Login + Bară permanentă `CIC-06.1` (Prioritate zero):** Activă înainte și după login; scalare text `100%–200%` prin `--font-scale`, buton High-Contrast cu etichetă textuală de stare, font bază `18 px`, ținte tactile ≥`48×48 px CSS`.
- [ ] **`E-02` Dashboard Pacient (≤2 click-uri de la login):** Click 1 = submit login (rutare directă pe cardul „Următoarea picătură”), Click 2 = `[CONFIRMĂ DOZA ACUM]`; dezactivare buton în timpul cererii, anunț `aria-live`, bară progres `0–100%`, serie de bază și 3 stări distincte (doză disponibilă / totul confirmat azi / fără plan activ).
- [ ] **`E-05` Recepție (pe mock, apoi API):** Validator live CNP Modulo 11 în formular, creare pacient cu afișare unică a credențialelor inițiale, căutare rapidă, programări cu afișare clară a erorii `409 APPOINTMENT_OVERLAP`.
- [ ] **`E-06` Medic (pe mock, apoi API):** Editor consultație (`AV OD/OS`, `TIO OD/OS mmHg`, biomicroscopie, `ICD-10`), editor prescripție cu ore (`scheduleHours`), grafic + tabel accesibil pentru sumarul de aderență.
- [ ] **Cele 3 puncte de legare timpurie FE–BE:** `28 nov` (`INT-01` Login/JWT) → `1–2 dec` (`INT-02` `E-05`/`E-06`) → `4–5 dec` (`INT-03` `E-02` + aderență `E-06`).
- [ ] **Audit manual WCAG 2.1 AA (12–13 dec):** Verificare pe desktop și mobil (`360 px`), text la `100%/150%/200%`, contrast ≥`4.5:1` și navigare completă din tastatură (`Tab`/`Enter`).

---

## 5. Protocol de Follow-up Zilnic & Jurnal de Bug-uri (8–15 dec)

### Format de Check-in zilnic (5 linii)

```markdown
1. DATA & ETAPA: [zz nov/dec] | [Etapa 0 / Etapa 1 / Integrare INT-0x / Debug]
2. ADRIAN (BE/Arhitectură): [ce s-a închis pe branch] | [ce urmează în 24h]
3. DOBRESCU (Gamification & Tests) & IOVAN (FE & UX): [Dobrescu: status] | [Iovan: status]
4. STATUS CI & LEGARE FE–BE: [CI: Verde/Roșu] | [Ultimul endpoint legat live / Mock activ]
5. BLOCAJE / BUG-URI CRITICE: [0 blocaje SAU id + descriere scurtă a erorii/riscului]
```

### Jurnal de Bug-uri — Rezerva de Stabilizare (8–15 dec)

| ID | Severitate | Ecran / Endpoint | Pași de reproducere | Responsabil | Retestat de | Status |
|---|---|---|---|---|---|---|
| `BUG-01` | — | — | — | — | — | — |

---

## 6. Aprobare & Feedback Echipa 19 (înainte de execuție)

### Semnături de aliniere pe roluri, contracte și termene
- [x] **Adrian Diaconescu** (Software Architect & Lead Backend Engineer) — Aprobat (24 nov 2026)
- [x] **Dobrescu Andrei-Alexandru** (Backend Engineer `gamification` & Owner Unit Tests) — Aprobat (24 nov 2026)
- [x] **Iovan Andrei-Alexandru** (Frontend SPA & Patient Accessibility UX Engineer) — Aprobat (24 nov 2026)

### Feedback / Ajustări cerute de echipă înainte de start
- *(Completați aici eventualele modificări de termene, nume de branch-uri sau clarificări pe contracte înainte de pornirea Etapei 1.)*
