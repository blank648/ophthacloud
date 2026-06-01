# OphthaCloud EMR/ERP

> Cloud-native Electronic Medical Record and ERP platform
> for ophthalmology clinics — built with Spring Boot 4 +
> Spring Modulith, React, multi-tenant, GDPR-compliant.

---

## Project Overview

OphthaCloud is a full-stack, cloud-native system for ophthalmology clinics. It operates as a cohesive platform comprising a robust backend server and a responsive frontend web application. The platform is actively integrating a production-ready UI layer with the backend business services.

## Repository Structure

This monorepo contains two primary components:

### `./frontend` — Official Frontend (React)
Role: The official user interface and client application for OphthaCloud.
Built with React 18, TypeScript, Tailwind CSS, shadcn/ui, and Vite. This directory serves as the working UI foundation and interacts with the backend APIs to deliver features to users. 
Stack: React 18, TypeScript, Tailwind CSS, shadcn/ui, Vite, React Query, Zustand.
Status: Active integration phase. Formerly a prototype, it is now being adapted as the production UI, consuming backend services.

### `./backend` — Production Backend (Spring Boot)
Role: The REST API backend.
Implements all business logic, data persistence, security, and multi-tenancy.
Stack: Java 21, Spring Boot 4.0.5, Spring Modulith, PostgreSQL 16, Keycloak 26.
Status: Active development and integration target.

---

## Current Status & Integration Direction

The project is currently in an active integration phase where the frontend UI components are being connected to backend business logic.
- **Backend:** Provides robust multi-tenancy, JWT-based RBAC via Keycloak, and domain modules (Patients, Appointments) accessible via `/api/v1`.
- **Frontend:** Consumes backend APIs using an `apiClient` with `axios` and `@tanstack/react-query`. It relies on `keycloak-js` for authentication and `zustand` for state management.
- **Next Steps:** Complete the end-to-end integration of all modules (e.g., Auth, Patients, Appointments), enforce unified security and validation rules, and establish reliable error handling and routing across the stack.

---

## Backend — Technical Overview

### Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 21 |
| Framework | Spring Boot | 4.0.5 |
| Architecture | Spring Modulith | 2.0.0 |
| Database | PostgreSQL | 16 |
| Migrations | Flyway | latest |
| Cache | Redis | 7 |
| Security | Keycloak | 26 |
| Build | Maven | 3.9+ |
| Containerization | Docker + Compose | latest |

### Architecture

The backend uses Spring Modulith to enforce explicit module boundaries. Each business domain is an independent module with a public facade API. Cross-module communication happens exclusively through public facades and Spring application events — never via direct repository access.

Multi-tenancy is implemented at the data layer: every entity extends `TenantAwareEntity` and carries a `tenant_id` column. Tenant resolution is automatic via `TenantContext` propagated from the JWT on every request.

---

## Development Progress

### Completed Sprints

#### Sprint 1 — Infrastructure & Shared Foundation
- Spring Boot 4 + Spring Modulith project initialized
- Docker Compose for PostgreSQL 16 + Redis 7 + Keycloak 26
- Flyway baseline migration (V1–V7)
- Shared API response wrappers (`ApiResponse`, `PagedApiResponse`)
- Global exception handler with structured error codes
- `TenantAwareEntity` base class
- Audit log module (`AuditLogService` + `AuditLogEntity`)

#### Sprint 2 — Security & Multi-Tenancy
- Keycloak JWT integration (`OphthaClinicalJwtConverter`)
- Custom PermissionEvaluator (`OphthaClinicalPermissionEvaluator`)
- RBAC model: tenant × role × module × action (VIEW/CREATE/EDIT/DELETE/SIGN)
- `TenantContext` propagation from JWT claims
- `SecurityUtils` helper
- Full security test suite

#### Sprint 3 — Patients Module
- `V8__create_patients.sql` migration
- `PatientEntity` (extends `TenantAwareEntity`)
- `PatientManagementFacade` (public API)
- 9 REST endpoints (`PatientController`): list, create, get, update, delete, consultations, prescriptions, appointments, portal-invite.
- MRN auto-generation format: `[CLINIC]-[YEAR]-[SEQ]`
- `PatientCreatedEvent` published on registration

#### Sprint 4 — Appointments Module
- `V9__create_appointments.sql` migration
- `AppointmentEntity`, `AppointmentTypeEntity`, `BlockedSlotEntity`
- `AppointmentManagementFacade` with anti-double-booking and state machine
- `AppointmentController` (8 endpoints)
- Events: `AppointmentBookedEvent`, `AppointmentCompletedEvent`, `PatientCheckedInEvent`, `AppointmentStatusChangedEvent`

---

### Module Status

| Module | Status | Migration | Facade | Controller | Tests |
|---|---|---|---|---|---|
| patients | ✅ Complete | V8 | ✅ | ✅ | ✅ |
| appointments | ✅ Complete | V9 | ✅ | ✅ | ✅ |
| emr | 🔲 Sprint 5 | — | — | — | — |
| investigations | 🔲 Sprint 6 | — | — | — | — |
| prescriptions | 🔲 Sprint 6 | — | — | — | — |
| optical | 🔲 Sprint 7 | — | — | — | — |
| notifications | 🔲 Sprint 8 | — | — | — | — |
| portal | 🔲 Sprint 9 | — | — | — | — |
| reports | 🔲 Sprint 10 | — | — | — | — |
| admin | 🔲 Sprint 10 | — | — | — | — |

---

## Database Migrations

| Version | File | Description |
|---|---|---|
| V1 | V1__baseline_schema.sql | Baseline schema (tenants, clinics, staff) |
| V2 | V2__audit_log.sql | Audit log infrastructure |
| V3 | V3__rename_user_id_to_actor_id.sql | Schema cleanup (audit log) |
| V4 | V4__create_event_publication.sql | Spring Modulith event publication table |
| V5 | V5__fix_event_publication_complete.sql | Fix for event publication visibility |
| V6 | V6__add_event_publication_resubmission.sql | Event resubmission support |
| V7 | V7__add_event_publication_status.sql | Event status tracking |
| V8 | V8__create_patients.sql | Patients module schema |
| V9 | V9__create_appointments.sql | Appointments module schema |
| V10 | V10__fix_appointments_enum_types.sql | Fix for Appointment PostgreSQL enums |

---

## Running Locally

Prerequisites: Docker Desktop, Java 21, Maven 3.9+, Node.js 20+

```bash
# Start infrastructure (PostgreSQL, Redis, Keycloak)
docker compose -f docker/docker-compose.dev.yml up -d

# Run backend
cd backend/ophthacloud-backend
./mvnw spring-boot:run

# Run frontend (in a separate terminal)
cd frontend
npm install
npm run dev
```

- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Frontend UI: http://localhost:5173
- Keycloak admin: http://localhost:8180

---

## API Documentation

Full REST API contract is defined in `GUIDE_04_API_CONTRACT.md` (located in `/guidelines`).

Base URL: `/api/v1`
Auth: Bearer JWT (issued by Keycloak)
Response format: `ApiResponse<T>` or `PagedApiResponse<T>`
Error format: `{ "success": false, "errorCode": "...", "message": "...", "fieldErrors": [...] }`

---

## Project Guidelines

Architecture, security, data model, API contract, module specs, dev standards, and design system are documented in `/guidelines`:

| File | Contents |
|---|---|
| GUIDE_00_PROJECT_BRIEF.md | Vision, users, constraints |
| GUIDE_01_ARCHITECTURE.md | Spring Modulith, stack, infra |
| GUIDE_02_DESIGN_SYSTEM.md | UI components, tokens (frontend) |
| GUIDE_03_DATA_MODEL.md | PostgreSQL schema, all tables |
| GUIDE_04_API_CONTRACT.md | REST endpoints, request/response |
| GUIDE_05_SECURITY.md | Keycloak, JWT, RBAC matrix |
| GUIDE_06_MODULE_SPECS.md | Business logic per module |
| GUIDE_07_DEV_STANDARDS.md | Conventions, testing, Git |
