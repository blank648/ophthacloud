# GUIDE_01 — Architecture: OphthaCloud

> **Document type:** Technical Architecture Reference  
> **Version:** 1.0  
> **Last updated:** 2026-04-02  
> **Author:** Project Architect  
> **Prerequisite:** GUIDE_00 (Project Brief) must be read first.  
> **Status:** FINAL — decisions in this document are not open for re-evaluation in Phase 1.

---

## Purpose of This Document

This document defines **how the system is built** — the structural decisions that all code must conform to.
Every class, package, database table, and API endpoint written during Phase 1 must be traceable to a decision documented here.

When Antigravity faces an implementation choice, this document provides the answer.
When this document does not provide the answer, escalate to the architect before proceeding.

---

## 1. Architecture Style: Modular Monolith

OphthaCloud Phase 1 is a **Modular Monolith** — a single deployable unit with enforced internal module boundaries.

### Why not Microservices

This decision is final (ADR-001 in GUIDE_00). Summary:

```
Microservices cost:                    Modular Monolith gives:
─────────────────────────────────────  ─────────────────────────────────────
Service mesh overhead                  Single JVM, zero network latency
Distributed transactions               Full ACID transactions
N×CI/CD pipelines                      One pipeline
Distributed tracing infrastructure     Standard stack traces
Inter-service contract versioning      Compile-time module boundary checks
Kubernetes complexity                  Docker Compose / Swarm simplicity
```

Spring Modulith provides the same architectural discipline as microservices — enforced module boundaries, documented dependencies, integration tests per module — at a fraction of the operational cost. If a future module must scale independently (e.g., the Notifications engine at 10,000 tenants), it can be extracted because the boundary is already clean.

### Deployment Topology

```
┌─────────────────────────────────────────────────────────────────┐
│                        Hetzner Cloud (Frankfurt)                │
│                                                                 │
│  ┌──────────────┐    ┌──────────────────────────────────────┐   │
│  │   Caddy      │    │          App Server (CX32)           │   │
│  │ (Reverse     │───▶│  ┌──────────────┐  ┌─────────────┐  │   │
│  │  Proxy +     │    │  │  Next.js     │  │ Spring Boot │  │   │
│  │  TLS)        │    │  │  Frontend    │  │ Backend     │  │   │
│  └──────────────┘    │  │  :3000       │  │ :8080       │  │   │
│                      │  └──────────────┘  └──────┬──────┘  │   │
│  ┌──────────────┐    │  ┌──────────────┐         │         │   │
│  │  Keycloak    │    │  │    Redis     │         │         │   │
│  │  (Auth)      │◀───┤  │    :6379     │         │         │   │
│  │  :8443       │    │  └──────────────┘         │         │   │
│  └──────────────┘    └──────────────────────────────────────┘   │
│                                                │                │
│  ┌─────────────────────────────────────────────▼──────────────┐ │
│  │                  DB Server (CX22)                          │ │
│  │   ┌──────────────────────┐   ┌──────────────────────────┐  │ │
│  │   │    PostgreSQL 16      │   │        MinIO             │  │ │
│  │   │    :5432              │   │   (Object Storage)       │  │ │
│  │   │    ophthacloud_db     │   │        :9000             │  │ │
│  │   └──────────────────────┘   └──────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

**Development environment** (macOS, Docker Compose):
All services run locally via `docker-compose.yml` — PostgreSQL, Redis, Keycloak, MinIO.
Spring Boot runs directly in IntelliJ (not in Docker during development).
Next.js runs with `npm run dev`.

---

## 2. Technology Stack (Exact Versions — Do Not Upgrade Without Architect Approval)

### Backend

| Component | Technology | Version | Notes |
|---|---|---|---|
| **Runtime** | Java (Temurin distribution) | **21 LTS** | GraalVM native image not required in Phase 1 |
| **Framework** | Spring Boot | **3.4.x** | Latest patch within 3.4 minor |
| **Modularity** | Spring Modulith | **1.3.x** | Aligned with Spring Boot 3.4 |
| **Security** | Spring Security | **6.4.x** | Via Spring Boot starter |
| **IAM** | Keycloak | **26.x** | Organizations feature for multi-tenancy |
| **ORM** | Hibernate via Spring Data JPA | **6.6.x** | Via Spring Boot starter |
| **Database** | PostgreSQL | **16** | |
| **Migrations** | Flyway | **10.x** | Via Spring Boot starter |
| **Cache** | Redis via Spring Cache | **7.4.x** | Lettuce client (default) |
| **Object Storage** | MinIO | **RELEASE.2025-01-x** | S3-compatible API |
| **API Docs** | SpringDoc OpenAPI | **2.8.x** | Swagger UI at `/swagger-ui.html` |
| **Build** | Maven | **3.9.x** | Wrapper committed to repo (`mvnw`) |
| **Validation** | Jakarta Bean Validation via Hibernate Validator | **8.x** | Via Spring Boot starter |
| **Mapping** | MapStruct | **1.6.x** | DTO ↔ Entity mapping |
| **Utility** | Lombok | **1.18.x** | `@Data`, `@Builder`, `@Slf4j` |
| **Testing** | JUnit 5 + Mockito + Testcontainers | Latest stable | PostgreSQL + Keycloak containers |
| **HTTP Client** | Spring WebClient (reactive, non-blocking) | Via Spring Boot | For outbound calls (SMS, email APIs) |

### Frontend (Read-Only Source from Lovable Export)

| Component | Technology | Version | Notes |
|---|---|---|---|
| **Framework** | Next.js | **15.x** | App Router |
| **Language** | TypeScript | **5.x** | Strict mode enabled |
| **Styling** | Tailwind CSS | **4.x** | Config in `tailwind.config.ts` |
| **Components** | shadcn/ui | Latest at export | Do not upgrade without testing |
| **State / Data** | TanStack Query | **5.x** | Server state, API calls |
| **Global State** | Zustand | **5.x** | Client-only UI state |
| **Charts** | Recharts | **2.x** | |
| **Icons** | Lucide React | **0.4x** | |
| **Auth** | `@keycloak/keycloak-js` + `keycloak-js` adapter | **26.x** | Must match Keycloak server version |
| **HTTP** | Axios | **1.x** | In `src/services/apiClient.ts` |
| **Forms** | React Hook Form + Zod | **7.x + 3.x** | |

### Infrastructure

| Component | Technology | Notes |
|---|---|---|
| **Containerization** | Docker + Docker Compose | Dev environment |
| **Orchestration (prod)** | Docker Swarm | Simple, sufficient for Phase 1 scale |
| **Reverse Proxy** | Caddy | Auto HTTPS via Let's Encrypt |
| **CI/CD** | GitHub Actions | `.github/workflows/` |
| **Monitoring** | Grafana + Prometheus | Via Docker Compose in prod |
| **Log aggregation** | Grafana Loki + Promtail | |
| **Secret management** | Docker Swarm secrets | No HashiCorp Vault in Phase 1 |

---

## 3. Repository Structure

```
ophthacloud/
├── backend/                          ← Spring Boot application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/dev/ophthacloud/
│   │   │   │   ├── OphthaCloudApplication.java
│   │   │   │   ├── modules/          ← 11 business modules
│   │   │   │   │   ├── dashboard/
│   │   │   │   │   ├── patients/
│   │   │   │   │   ├── appointments/
│   │   │   │   │   ├── emr/
│   │   │   │   │   ├── investigations/
│   │   │   │   │   ├── prescriptions/
│   │   │   │   │   ├── optical/
│   │   │   │   │   ├── notifications/
│   │   │   │   │   ├── portal/
│   │   │   │   │   ├── reports/
│   │   │   │   │   └── admin/
│   │   │   │   ├── shared/           ← Cross-cutting, not a business module
│   │   │   │   │   ├── domain/
│   │   │   │   │   ├── events/
│   │   │   │   │   ├── security/
│   │   │   │   │   ├── audit/
│   │   │   │   │   └── fhir/
│   │   │   │   └── infrastructure/   ← Technical plumbing only
│   │   │   │       ├── config/
│   │   │   │       ├── persistence/
│   │   │   │       └── web/
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       ├── application-dev.yml
│   │   │       ├── application-prod.yml
│   │   │       └── db/migration/     ← Flyway migrations
│   │   │           ├── V1__baseline_schema.sql
│   │   │           ├── V2__patients_module.sql
│   │   │           └── ...
│   │   └── test/
│   │       └── java/dev/ophthacloud/
│   │           ├── modules/          ← Module-level integration tests
│   │           ├── shared/           ← Shared infrastructure tests
│   │           └── architecture/     ← Spring Modulith structure tests
│   └── pom.xml
│
├── frontend/                         ← Lovable export (Next.js)
│   ├── src/
│   │   ├── app/                      ← Next.js App Router pages
│   │   ├── components/               ← React components (DO NOT MODIFY)
│   │   ├── services/                 ← API layer (NEW — added post-export)
│   │   │   ├── apiClient.ts          ← Axios instance + interceptors
│   │   │   ├── auth.ts               ← Keycloak adapter
│   │   │   └── modules/              ← Per-module API functions
│   │   │       ├── patients.ts
│   │   │       ├── appointments.ts
│   │   │       └── ...
│   │   ├── hooks/                    ← TanStack Query hooks
│   │   └── types/                    ← TypeScript types (match API contract)
│   ├── tailwind.config.ts
│   └── package.json
│
├── infrastructure/
│   ├── docker-compose.yml            ← Dev environment (all services)
│   ├── docker-compose.prod.yml       ← Production Docker Swarm stack
│   ├── caddy/
│   │   └── Caddyfile
│   └── keycloak/
│       └── realm-export.json         ← Keycloak realm configuration
│
├── docs/
│   ├── GUIDE_00_PROJECT_BRIEF.md
│   ├── GUIDE_01_ARCHITECTURE.md      ← this file
│   ├── GUIDE_02_DESIGN_SYSTEM.md
│   ├── GUIDE_03_DATA_MODEL.md
│   ├── GUIDE_04_API_CONTRACT.md
│   ├── GUIDE_05_SECURITY.md
│   ├── GUIDE_06_MODULE_SPECS.md
│   └── GUIDE_07_DEV_STANDARDS.md
│
└── .github/
    └── workflows/
        ├── backend-ci.yml
        └── frontend-ci.yml
```

---

## 4. Spring Modulith — Module Architecture

### The Golden Rule of Spring Modulith

> A module may only access another module's **public API** (classes in the root package of that module).  
> Accessing classes in a module's `internal` sub-package from another module is a **compile-time error**.

Spring Modulith enforces this automatically. The architecture test `ModuleStructureTest.java` verifies it on every build.

### Module Internal Structure

Every module follows this exact package layout:

```
modules/patients/
├── PatientManagementFacade.java     ← PUBLIC API: the only class other modules may call
├── PatientCreatedEvent.java         ← PUBLIC: events other modules may listen to
├── PatientSummaryDto.java           ← PUBLIC: DTOs other modules may reference
└── internal/
    ├── Patient.java                 ← PRIVATE: JPA entity
    ├── PatientRepository.java       ← PRIVATE: Spring Data repository
    ├── PatientService.java          ← PRIVATE: business logic
    ├── PatientController.java       ← PRIVATE: REST controller
    ├── PatientMapper.java           ← PRIVATE: MapStruct mapper
    └── PatientValidator.java        ← PRIVATE: domain validation
```

**Rule:** REST controllers live inside `internal/`. The `PatientController` is not a public API — it is the HTTP interface. Other modules talk to `PatientManagementFacade`, never to `PatientController`.

### Module Public APIs (Facade Pattern)

Each module exposes a single `@Service`-annotated Facade class. This is the only entry point for other modules.

```java
// Example: PatientManagementFacade.java (in module root — public)
@Service
@RequiredArgsConstructor
public class PatientManagementFacade {

    private final PatientService patientService; // internal

    /** Called by EMR module to get patient context for consultation header */
    public PatientContextDto getPatientContext(UUID patientId) {
        return patientService.buildContext(patientId);
    }

    /** Called by Appointments module to validate patient exists */
    public boolean patientExists(UUID patientId) {
        return patientService.exists(patientId);
    }
}
```

### Module Dependency Graph

Arrows represent allowed dependencies (A → B means A may call B's public API).
**No circular dependencies are permitted.**

```
                    ┌──────────────┐
                    │   DASHBOARD  │
                    └──────┬───────┘
                           │ reads from all modules
    ┌──────────────────────▼──────────────────────────────────┐
    │                                                         │
    ▼           ▼           ▼           ▼           ▼         ▼
PATIENTS   APPOINTMENTS   EMR   INVESTIGATIONS  PRESCRIPTIONS  OPTICAL
    │           │           │           │              │         │
    │           └───────────┤           └──────────────┤         │
    │                       ▼                          ▼         │
    │                     NOTIFICATIONS ◀──────────────┘         │
    │                       │                                    │
    └───────────────────────┴────────────────────────────────────┤
                            │                                    │
                            ▼                                    ▼
                         PORTAL                              REPORTS
                            │
                            ▼
                    (reads patients, appointments,
                     prescriptions, optical — read-only)

ADMIN ──────────────────────────────────────────────────────────▶ ALL
(admin reads/writes configuration for all modules via Admin Facade)

SHARED ◀─────────────────────────────────────────────────────── ALL
(all modules depend on shared — shared depends on nothing)
```

### Module Interaction: Synchronous vs Asynchronous

| When to use | Pattern | Example |
|---|---|---|
| Module A needs data from Module B **immediately** (same request) | Synchronous Facade call | `emr` calls `patients.getPatientContext()` to populate consultation header |
| Module A needs to **notify** Module B something happened | Asynchronous Application Event | `appointments` publishes `AppointmentCompletedEvent`, `notifications` listens |
| Module A needs to **trigger** a complex workflow in Module B | Asynchronous Application Event | `prescriptions` publishes `PrescriptionSignedEvent`, `optical` creates draft order |

**Asynchronous events use `@Async` + `@ApplicationModuleListener` (Spring Modulith).** They are transactional — if the publishing transaction fails, the event is not delivered. Pending events are stored in the `application_events` table (Spring Modulith managed) and retried on restart.

---

## 5. Application Events Catalog

These are the events that cross module boundaries. Every event is a plain Java record in `shared/events/`.

```java
// Location: shared/events/ — visible to all modules
```

| Event | Published by | Consumed by | Trigger |
|---|---|---|---|
| `PatientCreatedEvent` | patients | notifications, portal | New patient registered |
| `AppointmentBookedEvent` | appointments | notifications | Appointment created |
| `AppointmentCancelledEvent` | appointments | notifications | Appointment cancelled |
| `AppointmentCompletedEvent` | appointments | emr, notifications | Status set to Completed |
| `PatientCheckedInEvent` | appointments | dashboard | Status set to Checked-in |
| `ConsultationSignedEvent` | emr | prescriptions, notifications, reports | Doctor digitally signs EMR |
| `InvestigationResultAvailableEvent` | investigations | emr, notifications, portal | Result uploaded/attached |
| `PrescriptionSignedEvent` | prescriptions | optical, notifications, portal | Prescription digitally signed |
| `PrescriptionExpiringEvent` | prescriptions (scheduled) | notifications | Triggered by nightly job, 30 days before expiry |
| `OpticalOrderCreatedEvent` | optical | notifications | Order placed |
| `OpticalOrderStatusChangedEvent` | optical | notifications, portal | Kanban stage change |
| `OpticalOrderReadyEvent` | optical | notifications, portal | Status = Gata de montaj |
| `InvoiceGeneratedEvent` | optical | notifications, portal | Invoice created |
| `RecallDueEvent` | notifications (scheduled) | notifications | Triggered by nightly job per diagnosis protocol |
| `PatientConsentUpdatedEvent` | portal | patients, audit | Patient changes consent in portal |
| `LowStockAlertEvent` | optical (scheduled) | notifications, dashboard | Stock falls below minimum |

---

## 6. Multi-Tenancy Architecture

### Strategy: Row-Level with Hibernate Filters (ADR-003)

Every table that contains tenant-scoped data has a `tenant_id UUID NOT NULL` column.
A global Hibernate `@Filter` ensures that **no query can return data for a different tenant**, regardless of how it is written.

### TenantContext — Thread-Local Propagation

```
HTTP Request
     │
     ▼
TenantResolutionFilter (Spring Security filter chain, before any controller)
     │  1. Extracts JWT from Authorization header
     │  2. Validates with Keycloak public key
     │  3. Reads claim: tenant_id (UUID)
     │  4. Calls: TenantContext.set(tenantId)
     ▼
Controller → Service → Repository
     │  TenantContext.get() is available anywhere in the call stack
     │
     ▼
TenantAwareHibernateInterceptor
     │  Applies @Filter("tenantFilter") with tenantId parameter
     │  to all JPA Sessions on open
     ▼
Database Query: WHERE tenant_id = '<uuid>'  ← appended automatically
     │
     ▼
TenantContext.clear()  ← in filter's finally block
```

### Base Entity

All tenant-scoped entities extend `TenantAwareEntity`:

```java
@MappedSuperclass
@FilterDef(
    name = "tenantFilter",
    parameters = @ParamDef(name = "tenantId", type = UUID.class)
)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public abstract class TenantAwareEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @PrePersist
    void assignTenant() {
        this.tenantId = TenantContext.require();
    }
}

@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Version
    private Long version; // optimistic locking
}
```

### System-Level Entities (Not Tenant-Scoped)

These tables do NOT have `tenant_id` and do NOT extend `TenantAwareEntity`:

- `tenants` (the tenant registry itself)
- `roles` (predefined role catalog)
- `modules` (predefined module catalog)
- `keycloak_realm_mappings` (Keycloak realm/org ↔ tenant mapping)
- `flyway_schema_history`
- `application_events` (Spring Modulith event store)

---

## 7. Security Architecture

Full details in GUIDE_05 (Security). Architecture-level summary:

### Authentication Flow

```
Patient/Staff Browser
        │
        │  1. Redirect to Keycloak login page
        ▼
   Keycloak Server (:8443)
        │  2. Authenticate (username+password / Google SSO / 2FA)
        │  3. Issue JWT (access token + refresh token)
        ▼
   Browser stores tokens (memory only — no localStorage per sandbox rules)
        │
        │  4. Every API request: Authorization: Bearer <access_token>
        ▼
   Spring Boot API (:8080)
        │  5. TenantResolutionFilter: validate JWT, extract tenant_id + role + permissions
        │  6. SecurityContextHolder populated with OphthaPrincipal
        ▼
   Controller → @PreAuthorize("hasPermission('patients', 'VIEW')")
        │  7. OphthaClinicalPermissionEvaluator checks DB-cached permission matrix
        ▼
   Business logic executes (or 403 Forbidden returned)
```

### JWT Claims Structure

```json
{
  "sub": "user-uuid",
  "email": "doctor@clinica.ro",
  "name": "Dr. Ionescu",
  "tenant_id": "clinic-uuid",
  "tenant_name": "Clinica Exemplu",
  "role": "DOCTOR",
  "permissions": {
    "patients":      ["VIEW", "EDIT", "CREATE"],
    "appointments":  ["VIEW", "EDIT"],
    "emr":           ["VIEW", "EDIT", "CREATE", "SIGN"],
    "investigations":["VIEW", "CREATE"],
    "prescriptions": ["VIEW", "CREATE", "SIGN"],
    "optical":       ["VIEW"],
    "notifications": ["VIEW"],
    "portal":        [],
    "reports":       ["VIEW"],
    "admin":         []
  },
  "preferred_language": "ro",
  "iat": 1743620400,
  "exp": 1743624000
}
```

The `permissions` map is computed by Keycloak at token issuance from the `tenant_role_module_permissions` table (via a Keycloak custom Protocol Mapper that calls the backend).

---

## 8. Data Architecture

Full schema in GUIDE_03 (Data Model). Architecture-level decisions:

### Database: Single Schema, Row-Level Tenancy

```
PostgreSQL Database: ophthacloud_db
└── Schema: public
    ├── System tables (no tenant_id)
    │   ├── tenants
    │   ├── roles
    │   ├── modules
    │   └── tenant_role_module_permissions
    │
    └── Tenant-scoped tables (all have tenant_id)
        ├── patients
        ├── patient_medical_history
        ├── patient_consents
        ├── appointments
        ├── consultations
        ├── consultation_sections (JSONB per section A–I)
        ├── investigations
        ├── investigation_results (JSONB per type)
        ├── prescriptions
        ├── optical_orders
        ├── optical_order_items
        ├── stock_items
        ├── invoices
        ├── invoice_lines
        ├── notification_rules
        ├── notification_log
        ├── audit_log          ← IMMUTABLE, append-only
        └── ...
```

### JSONB Usage Policy

Clinical data with variable structure is stored as PostgreSQL `JSONB`:

| Table | JSONB Column | Content |
|---|---|---|
| `consultation_sections` | `section_data` | Per-section clinical data (VA/refraction in A, IOP in D, etc.) |
| `investigation_results` | `result_data` | OCT measurements, VF indices, topography K-values, etc. |
| `notification_rules` | `trigger_config` | Configurable timing, recipient, template per rule |

**Structured fields** (queried, filtered, indexed) stay in dedicated columns.
**Clinical free-text and variable measurement sets** go in JSONB.
**Never store JSONB where you need to filter or join** — if you query it, it needs a column.

### File Storage: MinIO (S3-Compatible)

All binary files stored in MinIO, referenced by URL in PostgreSQL:

| Bucket | Content |
|---|---|
| `patient-documents` | Uploaded PDFs, consent documents |
| `investigation-images` | DICOM files, fundus photos, OCT exports |
| `generated-documents` | System-generated PDFs (prescriptions, invoices, letters) |
| `profile-photos` | Patient and staff avatar images |

Files are accessed via **pre-signed URLs** (expiry: 1 hour for clinical images, 24 hours for documents). URLs are never stored permanently — they are generated on demand from the stored file path.

### Caching Strategy (Redis)

| Cache key | Content | TTL | Invalidation trigger |
|---|---|---|---|
| `permissions:{tenantId}:{userId}` | Full permissions map from JWT | 5 minutes | User role change, module config change |
| `icd10:search:{query}` | ICD-10 search results | 24 hours | Never (static data) |
| `tenant:config:{tenantId}` | Clinic settings (working hours, service catalog) | 15 minutes | Settings save event |
| `stock:low:{tenantId}` | Low-stock item list | 5 minutes | Stock update event |

Session tokens are managed by Keycloak — **do not cache JWT tokens in Redis**.

---

## 9. API Architecture

Full contract in GUIDE_04. Architecture-level decisions:

### URL Structure

```
/api/v1/{module}/{resource}
/api/v1/{module}/{resource}/{id}
/api/v1/{module}/{resource}/{id}/{sub-resource}

/fhir/r4/{FhirResourceType}       ← FHIR R4 endpoints (separate from /api)
/fhir/r4/{FhirResourceType}/{id}

Examples:
GET  /api/v1/patients
POST /api/v1/patients
GET  /api/v1/patients/{id}
GET  /api/v1/patients/{id}/consultations
POST /api/v1/emr/consultations
PUT  /api/v1/emr/consultations/{id}/sign
GET  /api/v1/prescriptions/{id}
POST /api/v1/optical/orders

GET  /fhir/r4/Patient
GET  /fhir/r4/Patient/{id}
GET  /fhir/r4/VisionPrescription/{id}
```

### Standard Response Envelope

```json
// Success — collection
{
  "data": [...],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 187,
    "totalPages": 10
  }
}

// Success — single resource
{
  "data": { ... }
}

// Error
{
  "error": {
    "code": "PATIENT_NOT_FOUND",
    "message": "Patient with ID abc-123 not found in tenant xyz-456",
    "timestamp": "2026-04-02T19:00:00Z",
    "path": "/api/v1/patients/abc-123",
    "requestId": "req-uuid"
  }
}
```

### CORS Configuration

In `application.yml`:
```yaml
spring:
  web:
    cors:
      allowed-origins:
        - "http://localhost:3000"       # dev
        - "https://app.ophthacloud.ro"  # prod
      allowed-methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
      allowed-headers: Authorization, Content-Type, X-Tenant-ID
      max-age: 3600
```

---

## 10. Frontend Architecture

### The Adapter Layer (New Code — Added Post Lovable Export)

The **only** new code added to the frontend after Lovable export lives in `src/services/`. All existing components, hooks, and pages are treated as read-only.

```typescript
// src/services/apiClient.ts — Axios instance (single source of truth)
import axios from 'axios';
import { keycloakAdapter } from './auth';

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL, // http://localhost:8080 in dev
  headers: { 'Content-Type': 'application/json' },
});

// Attach JWT to every request
apiClient.interceptors.request.use(async (config) => {
  await keycloakAdapter.updateToken(30); // refresh if expiring in < 30s
  config.headers.Authorization = `Bearer ${keycloakAdapter.token}`;
  return config;
});

// Global error handling
apiClient.interceptors.response.use(
  (response) => response.data.data, // unwrap envelope
  (error) => {
    if (error.response?.status === 401) keycloakAdapter.logout();
    if (error.response?.status === 403) router.push('/forbidden');
    return Promise.reject(error.response?.data?.error);
  }
);

export default apiClient;
```

```typescript
// src/services/modules/patients.ts — Per-module API functions
import apiClient from '../apiClient';
import type { Patient, CreatePatientDto } from '@/types/patients';

export const patientsApi = {
  list: (params?: PatientListParams) =>
    apiClient.get<PaginatedResponse<Patient>>('patients', { params }),

  getById: (id: string) =>
    apiClient.get<Patient>(`patients/${id}`),

  create: (data: CreatePatientDto) =>
    apiClient.post<Patient>('patients', data),

  update: (id: string, data: UpdatePatientDto) =>
    apiClient.put<Patient>(`patients/${id}`, data),
};
```

```typescript
// src/hooks/usePatients.ts — TanStack Query hook (replaces Lovable/Supabase hook)
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { patientsApi } from '@/services/modules/patients';

export const usePatients = (params?: PatientListParams) =>
  useQuery({
    queryKey: ['patients', params],
    queryFn: () => patientsApi.list(params),
  });

export const useCreatePatient = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: patientsApi.create,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['patients'] }),
  });
};
```

### Route Protection

Every route checks Keycloak authentication status and role-based module access before rendering:

```typescript
// src/components/ProtectedRoute.tsx
export function ProtectedRoute({
  module,
  permission,
  children
}: {
  module: ModuleName;
  permission: Permission;
  children: React.ReactNode;
}) {
  const { permissions, isAuthenticated } = useAuth();
  if (!isAuthenticated) return <RedirectToLogin />;
  if (!permissions[module]?.includes(permission)) return <ForbiddenPage />;
  return <>{children}</>;
}
```

---

## 11. Infrastructure Setup

### docker-compose.yml (Development)

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: ophthacloud_db
      POSTGRES_USER: ophthacloud
      POSTGRES_PASSWORD: dev_password_change_in_prod
    ports: ["5432:5432"]
    volumes: [postgres_data:/var/lib/postgresql/data]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ophthacloud"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
    command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru

  keycloak:
    image: quay.io/keycloak/keycloak:26.1
    command: start-dev --import-realm
    environment:
      KC_BOOTSTRAP_ADMIN_USERNAME: admin
      KC_BOOTSTRAP_ADMIN_PASSWORD: admin
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak_db
      KC_DB_USERNAME: ophthacloud
      KC_DB_PASSWORD: dev_password_change_in_prod
    ports: ["8080:8080"]
    volumes: [./infrastructure/keycloak:/opt/keycloak/data/import]
    depends_on:
      postgres: { condition: service_healthy }

  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports: ["9000:9000", "9001:9001"]
    volumes: [minio_data:/data]

volumes:
  postgres_data:
  minio_data:
```

### application.yml (Spring Boot)

```yaml
spring:
  application:
    name: ophthacloud-backend

  datasource:
    url: jdbc:postgresql://localhost:5432/ophthacloud_db
    username: ophthacloud
    password: ${DB_PASSWORD:dev_password_change_in_prod}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

  jpa:
    hibernate:
      ddl-auto: validate        # Flyway manages schema — Hibernate validates only
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        default_schema: public
        format_sql: false
        show_sql: false

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false

  data:
    redis:
      host: localhost
      port: 6379

  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/ophthacloud

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html

ophthacloud:
  minio:
    endpoint: http://localhost:9000
    access-key: minioadmin
    secret-key: ${MINIO_SECRET:minioadmin}
    buckets:
      patient-documents: patient-documents
      investigation-images: investigation-images
      generated-documents: generated-documents
      profile-photos: profile-photos
  keycloak:
    admin-url: http://localhost:8080
    realm: ophthacloud
    client-id: ophthacloud-backend
    client-secret: ${KEYCLOAK_CLIENT_SECRET}
```

### CI/CD Pipeline (GitHub Actions)

```yaml
# .github/workflows/backend-ci.yml
name: Backend CI
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16-alpine
        env: { POSTGRES_DB: ophthacloud_test, POSTGRES_USER: test, POSTGRES_PASSWORD: test }
        ports: ["5432:5432"]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - name: Run tests
        run: ./mvnw verify
        working-directory: backend
      - name: Verify Spring Modulith structure
        run: ./mvnw test -pl backend -Dtest=ModuleStructureTest
```

---

## 12. Local Development Setup (macOS)

### Prerequisites Installation

```bash
# 1. Homebrew (if not installed)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 2. SDKMAN for Java version management
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# 3. Java 21 (Temurin — Eclipse Foundation build)
sdk install java 21.0.5-tem
sdk default java 21.0.5-tem

# 4. Verify Java
java -version   # Must show: openjdk 21.x.x

# 5. Maven (via SDKMAN or Homebrew)
sdk install maven 3.9.9

# 6. Node.js (for frontend)
brew install node@20
echo 'export PATH="/opt/homebrew/opt/node@20/bin:$PATH"' >> ~/.zshrc

# 7. Docker Desktop
# Download from: https://www.docker.com/products/docker-desktop/
# Apple Silicon (M-series): download arm64 build

# 8. Verify Docker
docker --version
docker compose version  # Must be v2 (no hyphen)
```

### Starting the Development Environment

```bash
# 1. Start all infrastructure services (PostgreSQL, Redis, Keycloak, MinIO)
docker compose -f infrastructure/docker-compose.yml up -d

# 2. Wait for Keycloak to be healthy (takes ~30s first run)
docker compose -f infrastructure/docker-compose.yml logs -f keycloak

# 3. Start Spring Boot backend (in IntelliJ or terminal)
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 4. Start Next.js frontend
cd frontend
npm install
npm run dev

# 5. Access points:
# Frontend:       http://localhost:3000
# Backend API:    http://localhost:8080/swagger-ui.html
# Keycloak Admin: http://localhost:8080/admin (admin/admin)
# MinIO Console:  http://localhost:9001 (minioadmin/minioadmin)
```

### IntelliJ IDEA Configuration

1. Open `backend/` as the Maven project root (not the monorepo root)
2. Set Project SDK to Java 21 (Temurin)
3. Enable annotation processing: Settings → Build → Compiler → Annotation Processors → Enable
4. Install plugins: Lombok, MapStruct, Spring Boot
5. Run configuration: `OphthaCloudApplication` with VM option `-Dspring.profiles.active=dev`
6. Enable "Hot Swap" for faster iteration during development

---

## 13. Architecture Rules (Enforced by Tests)

The following rules are verified by `architecture/ModuleStructureTest.java` on every build.
A failing architecture test blocks the CI pipeline.

```java
@SpringModulithTest
class ModuleStructureTest {

    @Test
    void modulesShouldBeCompliant(ApplicationModules modules) {
        // Verifies: no module accesses another module's internal packages
        // Verifies: no circular dependencies between modules
        // Verifies: all cross-module calls go through public Facade or Events
        modules.verify();
    }

    @Test
    void modulesShouldBeDocumented(ApplicationModules modules) {
        // Ensures every module has a package-info.java with @ApplicationModule annotation
        modules.forEach(module ->
            assertThat(module.getBasePackage().getAnnotation(ApplicationModule.class))
                .isNotNull()
        );
    }
}
```

Additionally, `ArchitectureRulesTest.java` (using ArchUnit) enforces:
- No `@RestController` in module root packages (only in `internal/`)
- No direct `@Repository` injection across module boundaries
- No `new` keyword on entities outside their own module
- All entities extending `TenantAwareEntity` (except system entities — explicit whitelist)
- `TenantContext.get()` only called from `TenantAwareHibernateInterceptor` and test utilities

---

## 14. Decisions That Antigravity Must Not Override

This section is a checklist. Before every sprint, Antigravity confirms it has not violated any of these:

- [ ] **No microservices** — one Spring Boot application, one JVM process
- [ ] **No new databases** — PostgreSQL is the only persistent store (Redis is cache only)
- [ ] **No Supabase, Firebase, or BaaS** — all backend logic runs in Spring Boot
- [ ] **No direct repository calls across module boundaries** — only via Facade or Events
- [ ] **No circular module dependencies**
- [ ] **No hardcoded tenant_id** — always from TenantContext
- [ ] **No skipping the Hibernate tenant filter** — never use `EntityManager.unwrap(Session.class).disableFilter()`
- [ ] **No modification of frontend component files** — only `src/services/` and `src/hooks/` are modified
- [ ] **No changes to CSS variables or design tokens** — GUIDE_02 is frozen
- [ ] **No Flyway migration file edits after merge** — create a new migration file instead
- [ ] **No JWT secret hardcoded** — always from environment variable or Docker secret
- [ ] **No `ddl-auto: create` or `ddl-auto: update`** — only `validate` in all environments

---

*End of GUIDE_01 — Architecture*  
*Next document: GUIDE_02 — Design System*
