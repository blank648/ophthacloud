# AGENTS.md — OphthaCloud Academic Module Rules (FMI Team 19)

These rules govern ALL human and AI CLI agents (`agy`, `codex`, `claude`) working in this repository.

## 1. Strict Workspace Isolation (`SP-01`)
- **Active codebase:** Work **ONLY** inside `academic/` (`academic/backend/`, `academic/frontend/`, `academic/docker-compose.yml`).
- **Forbidden legacy paths:** NEVER read, copy patterns from, modify, stage, or delete files in legacy `backend/`, `frontend/`, `docker/`, `guidelines/GUIDE_00..07`, or `docs/rapoarte_ingprog/`.
- **Single Source of Truth:** Always align strictly with:
  1. `ARCHITECTURE_AND_EXECUTION_PLAN.md` (DB schema §4, REST contracts & business rules §5)
  2. `EXECUTION_BOARD_FEREASTRA_1.md` (WBS tasks, branches, DoD)
  3. `academic/TEAM_PLAYBOOK.md` (local commands, role boundaries, Git workflow)

## 2. Non-Negotiable Technical & Operational Rules (Window 1: until Dec 16, 2026)
1. **Docker = PostgreSQL 16 ONLY:** Run `docker compose -f academic/docker-compose.yml up -d` (host port `5435` → `5432`, DB `ophthacloud_academic`). Backend (`:8080`) and Frontend (`:5173`) run natively on the host. No cloud/Redis/Keycloak/MinIO.
2. **Frozen Flyway Schema (`V1..V7`):** NEVER edit existing `V1__*.sql`..`V7__*.sql` migrations. Any schema fix approved by the architect requires a new `V8__*.sql` migration. Keep `spring.jpa.hibernate.ddl-auto=validate` and UTC storage (`app.clinic-zone=Europe/Bucharest` via injectable `Clock`).
3. **Modular Monolith Boundaries (ArchUnit enforced):** Under `ro.unibuc.fmi.ophthacloud`, the 4 domain modules (`patientbooking`, `clinicalamsler`, `gamification`, `securityaudit`) MUST NOT access another module's internal packages (`entity`, `repository`, `service`, `domain`, `dto`) — communicate strictly through `<module>.api.*` interfaces/records and `common.*`. `ModuleArchitectureTest` must always pass.
4. **Unit Tests Only in CI (`JUnit 5 + Mockito`):** `./mvnw -B test` in `academic/backend` MUST pass offline with **zero running database/Docker containers** (do NOT add `@SpringBootTest` tests that connect to PostgreSQL in the default `test` phase). Write 1–2 clean manual unit tests following `CnpModulo11ValidatorTest`, then extend edge cases.
5. **Patient Accessibility Priority (`WCAG 2.1 AA`):** Base font `18px`, `--font-scale` discrete steps `[1.0, 1.2, 1.5, 1.8, 2.0]` (matching PostgreSQL `NUMERIC(2,1)`), High-Contrast mode with explicit text state, interactive targets `>= 48x48 px`, and `<= 2 clicks` from login submit to dose confirmation on `E-02`.

## 3. Team Ownership & Git Hygiene
- **Adrian Diaconescu:** Architecture, `securityaudit`, `patientbooking`, `clinicalamsler`, integration merges into `main`.
- **Dobrescu Andrei-Alexandru:** `gamification` module (`GET /api/adherence/today`, `POST /api/adherence/confirm`, `GET /api/adherence/{patientId}/summary`) & unit test suite.
- **Iovan Andrei-Alexandru:** `academic/frontend/` SPA (`E-01`, `E-02`, `E-05`, `E-06`), `CIC-06.1` accessibility bar, mock-to-live binding.
- **Git safety:** NEVER run `git add .` or `git add -A` (stage explicit paths only). Verify `./academic/backend/mvnw -f academic/backend/pom.xml -B test` and `npm --prefix academic/frontend run build` before opening/merging PRs into `main`.
