# OphthaCloud Academic — Team Playbook & Ghid de Execuție (Echipa 19)

Acest document este ghidul practic de lucru zilnic pentru **Adrian Diaconescu**, **Dobrescu Andrei-Alexandru** și **Iovan Andrei-Alexandru** în Fereastra 1 (`25 nov – 16 dec 2026`).

> [!WARNING]
> **Izolare strictă:** Lucrăm **exclusiv** în folderul `academic/`. Nu modificați nimic în folderele vechi din rădăcină (`backend/`, `frontend/`, `docker/`, `guidelines/GUIDE_00..07`) și **nu folosiți `git add .` sau `git add -A`** (adăugați mereu căi explicite, ex. `git add academic/backend/...` sau `git add academic/frontend/...`).

---

## 1. Pornire Locală în 3 Comenzi (Quickstart)

Din rădăcina repository-ului (`ophthacloud/`):

```bash
# 1. Pornește DOAR PostgreSQL 16 în Docker (mapat pe localhost:5435 -> zero conflicte cu 5432/5434)
docker compose -f academic/docker-compose.yml up -d

# 2. Pornește Backendul Spring Boot 3.3 nativ pe http://localhost:8080 (aplică automat Flyway V1..V7)
cd academic/backend && ./mvnw spring-boot:run

# 3. (În alt terminal) Pornește Frontendul React/Vite nativ pe http://localhost:5173
cd academic/frontend && cp -n .env.example .env && npm install && npm run dev
```

**Comenzile obligatorii de verificare locală (identice cu GitHub Actions CI):**
```bash
# Backend unit tests + ArchUnit (trebuie să treacă chiar și cu Docker oprit!)
./academic/backend/mvnw -f academic/backend/pom.xml -B test

# Frontend TypeScript + Vite production build
npm --prefix academic/frontend run build
```

---

## 2. Ghid de Start pe Roluri (Etapa 1: 26 nov – 5 dec)

### A. Dobrescu Andrei-Alexandru — Modulul `gamification` & Teste Unitare
1. **Creează branch-ul tău din `main` actualizat:**
   ```bash
   git switch main && git pull origin main
   git switch -c feat/gamification-adherence
   ```
2. **Ce ai deja pregătit în `academic/backend`:**
   - Interfața pe care o injectezi pentru citirea prescripțiilor (fără să depinzi de codul lui Adrian): `ro.unibuc.fmi.ophthacloud.clinicalamsler.api.TreatmentPlanQueryService` (`TreatmentPlanSnapshotDto`).
   - Ceasul clinicii injectabil: `java.time.Clock` (configurat deja în `Europe/Bucharest` prin `ClockConfig`).
   - Entitățile gata mapate pe tabelele `V5`/`V6`: `AdherenceLogEntity`, `GamificationProfileEntity`, `AdherenceStatus`.
   - Toate DTO-urile tale REST gata definite în `ro.unibuc.fmi.ophthacloud.gamification.dto.*`.
   - Șablonul oficial de test unitar al echipei: `CnpModulo11ValidatorTest.java` (structură `Arrange / Act / Assert`).
3. **Reguli de aur pentru `ModuleArchitectureTest` (ArchUnit):**
   - Tot codul tău rămâne în `ro.unibuc.fmi.ophthacloud.gamification.*` (`repository`, `service`, `controller`, `entity`, `dto`, `domain`).
   - Nu importa clase interne din `patientbooking`, `clinicalamsler` sau `securityaudit` — folosește doar pachetele lor `.api.*` și `common.*` (`BusinessRuleException` pt. `422 DOSE_WINDOW_CLOSED`, `ConflictException` pt. `409`, `ResourceNotFoundException` pt. `404`).
   - Nu folosi `@SpringBootTest` în testele unitare (scrie teste rapide cu `@ExtendWith(MockitoExtension.class)` mock-uind `TreatmentPlanQueryService`, repository-urile și `Clock.fixed(...)`).

### B. Iovan Andrei-Alexandru — Frontend SPA & Accesibilitate Pacient (`WCAG 2.1 AA`)
1. **Creează primul branch din `main` actualizat:**
   ```bash
   git switch main && git pull origin main
   git switch -c feat/fe-shell-e01-a11y
   ```
2. **Ce ai deja pregătit în `academic/frontend`:**
   - Tipurile TypeScript aliniate 1:1 cu backend-ul: `src/types/api.ts`.
   - Toate cele 5 fișiere de mock cu UUID-uri canonice coerente: `src/mocks/*.mock.json`.
   - Adaptorul HTTP mock→live: `withMockFallback(mockFn, liveFn, forceLive)` și token-ul JWT în memorie în `src/lib/apiClient.ts`.
   - Variabilele CSS de bază (`--font-scale`, `data-high-contrast="true"`, `.touch-target` ≥`48×48px`) și demonstrația barei `CIC-06.1` în `src/index.css` și `src/App.tsx`.
3. **Atenții critice de compatibilitate FE–BE:**
   - În PostgreSQL, `patients.ui_font_scale` este `NUMERIC(2,1)` (o singură zecimală!). Folosește pași cu 1 zecimală (ex. `1.0`, `1.2`, `1.5`, `1.8`, `2.0` — **nu** `1.25` sau `1.75`).
   - La confirmarea picăturii în `E-02`, trimite în `POST /api/adherence/confirm` exact perechea `{ planId, scheduledFor }` primită din `GET /api/adherence/today` (fără reconversie locală a timestamp-ului ISO UTC!).
   - Când legăm pe rând ecranele la API (`INT-01` pe 28 nov, `INT-02` pe 1–2 dec, `INT-03` pe 4–5 dec), poți trece fie individual un serviciu pe `forceLive = true` în `withMockFallback`, fie global `VITE_USE_MOCKS=false` în `.env`.

---

## 3. Protocol Git când lucrăm 2–3 colegi pe același task (`INT-0x` și `DBG-0x`)

Ca să avem **zero conflicte de merge**, nu edităm niciodată simultan aceleași fișiere pe același branch fără predare de ștafetă (*Relay Workflow*):

### Cazul 1 — Punctele de Integrare FE–BE (`INT-01`, `INT-02`, `INT-03`)
1. **Precondiție obligatorie:** PR-ul de Backend și PR-ul de Frontend (pe mock) intră ambele în `main` cu CI verde **înainte** să deschidem branch-ul `feat/int-0x-...`.
2. **Pasul 1 — Frontend deschide ștafeta (Iovan):** Din `main` la zi, Iovan creează `feat/int-0x-...`, comută apelurile ecranului de pe mock pe API live, testează local cu backend-ul pornit, face `commit & push` în `academic/frontend/` și anunță pe grup.
3. **Pasul 2 — Backend ajustează dacă e nevoie (Adrian / Dobrescu):** Colegul de backend face `git switch feat/int-0x-... && git pull`, adaugă eventualele ajustări în `academic/backend/` (zero conflict, directoare diferite!), verifică `./mvnw -B test` și face `push`.
4. **Pasul 3 — Merge în `main` (Adrian):** După ce CI-ul este verde și proba în browser trece, Adrian face merge PR-ului în `main` și bifează `INT-0x` în `EXECUTION_BOARD_FEREASTRA_1.md`.

### Cazul 2 — Săptămâna de Debug (`DBG-01`..`DBG-04`, 8–15 decembrie)
- Fiecare bug înregistrat în tabelul din `EXECUTION_BOARD_FEREASTRA_1.md` (`BUG-01`, `BUG-02`...) primește **un responsabil principal** și un branch scurt dedicat (`fix/bug-01-descriere`).
- Dacă bug-ul atinge și FE și BE, primul coleg face `commit & push` pe partea sa și pasează explicit ștafeta pe grup celui de-al doilea înainte de `git pull`. Un alt membru decât autorul retestează în browser înainte de merge în `main`.

---

## 4. Șablon Outcome-Based dacă folosiți CLI AI (`claude`, `codex`, `agy`)

Fișierele `AGENTS.md` și `CLAUDE.md` din rădăcina repo-ului încarcă deja automat regulile proiectului în CLI. Când dați un task agentului vostru CLI, folosiți acest șablon scurt **Contract-Anchored Outcome-Based**:

```markdown
Read `AGENTS.md`, `ARCHITECTURE_AND_EXECUTION_PLAN.md` (relevant section), and `EXECUTION_BOARD_FEREASTRA_1.md` (Task <ID>).

Goal (Outcome): Implement Task `<ID>` on branch `<branch-name>` strictly inside `<your-module-or-folder>`.
Contract & Constraints:
- Match exact endpoints, DTO field names, status codes, and business rules from §5 of `ARCHITECTURE_AND_EXECUTION_PLAN.md`.
- Respect all module/accessibility guardrails in `AGENTS.md`. Never stage outside `academic/`.

Self-Verification Gate (must pass before finishing):
1. Write/extend unit tests (or TS types) covering normal flow + edge cases from the spec.
2. Run `./academic/backend/mvnw -f academic/backend/pom.xml -B test` and/or `npm --prefix academic/frontend run build` until 100% green.
3. Report modified files, test count, and verification status without pushing.
```
