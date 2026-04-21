# OphthaCloud EMR/ERP

> Cloud-native Electronic Medical Record and ERP platform
> for ophthalmology clinics — built with Spring Boot 4 +
> Spring Modulith, multi-tenant, GDPR-compliant.

---

## Repository Structure

This monorepo contains two independent sub-projects
with distinct roles:

### `./frontend` — Visual Prototype (Vibecoded UI)
Role: design reference and UX prototype only.
Built with Lovable (React + TypeScript + Tailwind CSS +
shadcn/ui). This prototype was developed to validate and
freeze the visual design and user experience with the client
before backend development began.
It is NOT connected to the backend and does NOT contain
business logic. It serves as the approved design specification
that the production UI will implement.
Stack: React 18, TypeScript, Tailwind CSS, shadcn/ui, Vite.
Status: Design approved by client — frozen as visual reference.

### `./backend` — Production Backend (Spring Boot)
Role: production-grade REST API backend.
Implements all business logic, data persistence, security,
and multi-tenancy. This is the active development target.
See detailed documentation below.

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

The backend uses Spring Modulith to enforce explicit module
boundaries. Each business domain is an independent module
with a public facade API. Cross-module communication happens
exclusively through public facades and Spring application
events — never via direct repository access.

Multi-tenancy is implemented at the data layer: every entity
extends TenantAwareEntity and carries a tenant_id column.
Tenant resolution is automatic via TenantContext propagated
from the JWT on every request.

---

## Development Progress

### Completed Sprints

#### Sprint 1 — Infrastructure & Shared Foundation
Objective: Project skeleton, shared infrastructure, Docker setup.
Key deliverables:
- Spring Boot 4 + Spring Modulith project initialized
- Docker Compose for PostgreSQL 16 + Redis 7 + Keycloak 26
- Flyway baseline migration (V1–V7)
- Shared API response wrappers (ApiResponse, PagedApiResponse)
- Global exception handler with structured error codes
- TenantAwareEntity base class
- Audit log module (AuditLogService + AuditLogEntity)

#### Sprint 2 — Security & Multi-Tenancy
Objective: JWT security, RBAC permission evaluator, tenant isolation.
Key deliverables:
- Keycloak JWT integration (OphthaClinicalJwtConverter)
- Custom PermissionEvaluator (OphthaClinicalPermissionEvaluator)
- RBAC model: tenant × role × module × action (VIEW/CREATE/EDIT/DELETE/SIGN)
- TenantContext propagation from JWT claims
- SecurityUtils helper
- Full security test suite

#### Sprint 3 — Patients Module
Objective: Complete patient management with MRN generation,
medical history, and soft delete.
Key deliverables:
- V8__create_patients.sql migration
- PatientEntity (extends TenantAwareEntity)
- PatientManagementFacade (public API)
- 9 REST endpoints (PatientController):
  GET list (paginated + search), POST (201 + MRN),
  GET by ID (with history + statistics), PUT, DELETE (204),
  GET consultations, GET prescriptions, GET appointments,
  POST portal-invite
- MRN auto-generation format: [CLINIC]-[YEAR]-[SEQ]
- PatientCreatedEvent published on registration
- Full test suite: unit + integration + module boundary

#### Sprint 4 — Appointments Module
Objective: Calendar scheduling with double-booking prevention,
status state machine, blocked slots, and event publishing.
Key deliverables:
- V9__create_appointments.sql migration
  (tables: appointment_types, appointments, blocked_slots)
- AppointmentEntity, AppointmentTypeEntity, BlockedSlotEntity
- AppointmentManagementFacade with:
  - Anti-double-booking (dual overlap check: appointments + blocked_slots)
  - endAt computed at runtime (never stored) as startAt + durationMinutes
  - State machine: BOOKED→CONFIRMED→CHECKED_IN→IN_PROGRESS→COMPLETED
    terminal states: COMPLETED, CANCELLED, NO_SHOW
- AppointmentController (8 endpoints):
  GET calendar range (max 31 days), POST (201), GET by ID,
  PUT, PATCH status, DELETE (204)
- AppointmentTypeController (list, create, update)
- Events: AppointmentBookedEvent, AppointmentCompletedEvent,
  PatientCheckedInEvent, AppointmentStatusChangedEvent
- Cross-module validation: patientExists() via PatientManagementFacade
- Full test suite: unit + integration + module boundary

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

Prerequisites: Docker Desktop, Java 21, Maven 3.9+

```bash
# Start infrastructure
docker compose -f docker/docker-compose.dev.yml up -d

# Run backend
cd backend/ophthacloud-backend
./mvnw spring-boot:run

# Run tests
./mvnw verify
```

Backend API: http://localhost:8080
Swagger UI: http://localhost:8080/swagger-ui.html
Keycloak admin: http://localhost:8180

---

## API Documentation

Full REST API contract is defined in GUIDE_04_API_CONTRACT.md
(located in /guidelines).

Base URL: /api/v1
Auth: Bearer JWT (issued by Keycloak)
Response format: ApiResponse<T> or PagedApiResponse<T>
Error format: { "success": false, "errorCode": "...",
               "message": "...", "fieldErrors": [...] }

---

## Project Guidelines

Architecture, security, data model, API contract, module specs,
dev standards, and design system are documented in /guidelines:

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
