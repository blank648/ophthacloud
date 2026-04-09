# GUIDE_00 — Project Brief: OphthaCloud

> **Document type:** Project Brief — Business & Product Context  
> **Version:** 1.0  
> **Last updated:** 2026-04-02  
> **Author:** Project Architect  
> **Intended audience:** Antigravity (AI development agent), all project contributors  
> **Reading order:** This is document 0 of 8. Read before any other guide.

---

## Purpose of This Document

This document establishes the **"why"** behind every technical decision in this project.
Before writing any code, Antigravity must internalize the product context, user needs, competitive positioning, and non-negotiable constraints defined here. Every architectural decision in GUIDE_01 through GUIDE_07 traces back to this document.

When facing an ambiguous implementation choice, ask: *"Which option better serves the clinical user described in Section 4?"* That question resolves most ambiguity.

---

## 1. Product Vision

**OphthaCloud** is a cloud-native, multi-tenant SaaS platform purpose-built for ophthalmology clinics and optical retail businesses in Romania, with a roadmap toward broader EU market adoption.

It is the **only platform on the Romanian market** that unifies:
- A complete **Electronic Medical Record (EMR)** system tailored to ophthalmic clinical workflows
- A full **Optical Retail ERP** (orders, stock, lab management, billing, POS)
- A **Patient Portal** with self-service booking, record access, and PRO questionnaires
- **Intelligent recall and notification automation** driven by clinical diagnoses

**Tagline (Romanian UI only):** *"Viziunea ta clinică. Digitalizată."*

**North Star metric:** A doctor completes a full ophthalmic consultation — from patient arrival to signed EMR with digital prescription — in under 8 minutes, without touching paper.

---

## 2. Problem Statement

### The current landscape in Romanian ophthalmology

Romanian ophthalmology clinics currently operate with one of three setups:

| Setup | Prevalence | Core Problem |
|---|---|---|
| **OptiSoft** (desktop, legacy) | ~40% of clinics | Windows-only, no cloud, no patient portal, cramped UI designed in the early 2000s, no optical ERP integration |
| **AppSmart** (generic medical SaaS) | ~25% of clinics | Not ophthalmology-specific — missing refraction inputs, IOP tracking, DICOM viewer, optical order workflow |
| **Paper + Excel hybrid** | ~35% of clinics | No digital records, manual recall, zero analytics, GDPR non-compliant |

### The gap OphthaCloud fills

1. **No existing solution integrates EMR + optical retail ERP** in one platform. Clinics using OptiSoft for clinical records manage optical orders in a separate spreadsheet or paper system.
2. **Legacy software is not cloud-native.** OptiSoft requires a local server, manual backups, and on-site IT support. There is no mobile access, no patient portal, no automated notifications.
3. **Generic EMRs miss ophthalmology-specific clinical logic.** A general EMR has no concept of OD/OS dual-column refraction input, IOP color-coded alerts, RNFL trend analysis, or keratoconus staging.
4. **GDPR compliance is absent or retroactively patched** in existing solutions. Consent management, audit logs, and data subject rights (access, portability, erasure) are missing or incomplete.
5. **No intelligent recall system.** Glaucoma patients missing their annual check, diabetic patients overdue for retinopathy screening — these represent both clinical risk and lost revenue. No current solution automates this.

---

## 3. Target Market

### Primary market: Romania

- **Clinic types:** Private ophthalmology clinics (solo practitioner to multi-location chains), ophthalmology departments within private hospital groups, optical retail chains with in-house medical consultation
- **Clinic size:** 1–6 doctors, 1–4 locations — the sweet spot for the OphthaCloud pricing model
- **Geography:** Urban-first (București, Cluj, Timișoara, Iași, Brașov), then secondary cities
- **Estimated addressable clinics in Romania:** ~1,200 active private ophthalmology practices

### Secondary market (Phase 2, post-production stabilization)

- Bulgaria, Hungary, Poland — similar regulatory context (EU GDPR, similar healthcare structure)
- Language layer abstraction is built in from day one (Romanian / English toggle per user)

### Who pays

The **clinic owner / manager** buys OphthaCloud. The **doctor** is the daily power user whose satisfaction determines renewal. The **receptionist** is the most frequent user by session count. The **patient** is the end beneficiary of the portal. All four must be served well.

---

## 4. User Roles & Personas

These are the **eight predefined roles** in the system. Each clinic (tenant) configures which modules each role can access — this is the core RBAC requirement (see GUIDE_05).

---

### Role 1: Doctor (Medic Oftalmolog)

**Who:** Licensed ophthalmologist, primary clinical user.

**Goals:**
- Complete a full consultation (9-section EMR) rapidly and with zero data entry friction
- Sign prescriptions digitally without printing
- Monitor chronic patients (glaucoma, AMD, diabetic retinopathy) longitudinally via trend charts
- Receive clinical alerts when a patient's IOP exceeds safe thresholds or a recall is overdue

**Pain points with current tools:**
- OptiSoft forces sequential, slow form navigation — cannot see patient history while entering current consultation
- No IOP auto-flagging — doctor must remember normal ranges mentally
- Prescription generation requires re-entering data already in the EMR
- No longitudinal visual field or OCT trend visualization

**Critical UX requirement:** The consultation split-layout (left: patient context sticky / right: active section) must be maintained. The doctor must never lose sight of the patient's identity and primary diagnosis while typing. This was a key driver of the client's design approval.

**Session pattern:** 2–4 hour consultation blocks, 10–20 patients/day, high cognitive load, often in darkened examination rooms → dark mode is clinically relevant, not cosmetic.

---

### Role 2: Optometrist

**Who:** Optometrist performing refraction assessments and visual screenings, may or may not have prescribing authority depending on clinic configuration.

**Goals:**
- Perform and record refraction exams with full OD/OS dual-column input
- Order investigations (topography, OCT) and attach results to patient records
- Hand off pre-filled refraction data to the doctor for prescription signing

**Distinct from Doctor:** Cannot sign prescriptions digitally (configurable per tenant). Has access to EMR but not to billing module by default.

---

### Role 3: Recepție (Receptionist)

**Who:** Front desk staff handling patient intake, appointment management, and basic billing.

**Goals:**
- Book, confirm, reschedule, and cancel appointments without friction
- Check in patients on arrival (status: Booked → Confirmed → Checked-in)
- Collect payments and issue receipts at POS
- See today's full schedule across all doctors at a glance

**Pain points:**
- Double-booking is a daily stress — needs hard prevention, not just warnings
- No way to track which patients have outstanding balances across visits
- Manual SMS confirmation is time-consuming — must be fully automated

**Session pattern:** All-day, continuous, high interruption rate. Dashboard must be glanceable in 2 seconds.

---

### Role 4: Manager / Clinic Owner

**Who:** Clinic owner or practice manager. May or may not be medically trained.

**Goals:**
- Monitor revenue (daily, monthly, YTD) broken down by doctor, service type, and medical vs. optical
- Track prescription-to-optical-order conversion rate (target: 70%)
- Identify no-show patterns and lost revenue
- Manage stock levels and trigger supplier orders
- Configure which roles access which modules (RBAC matrix)
- Onboard/offboard staff

**Critical insight:** The Manager dashboard is the primary **sales tool** during the sales cycle. When evaluating OphthaCloud, a clinic owner opens the Manager dashboard first. It must make them feel in control and informed within 30 seconds of first login.

**Session pattern:** Short, focused check-ins (5–15 minutes) at start/end of day or week. High-level overview, drill-down on demand.

---

### Role 5: Optician (Optician / Lab Technician)

**Who:** Optical retail staff responsible for lens fitting, frame selection, lab order management, and quality control.

**Goals:**
- Receive optical orders from prescriptions and process them through the production pipeline
- Track order status with lab partners (sent → in production → QC → ready for fitting)
- Complete QC checklist before delivering glasses to patient
- Manage frame and lens inventory, trigger reorder when stock falls below minimum

**Distinct context:** The optician works primarily in the **Optical ERP module** (Kanban board). They rarely access EMR or clinical sections. Their dashboard is the Kanban board, not the clinical appointment list.

---

### Role 6: Asistent Medical (Medical Assistant / Nurse)

**Who:** Clinical support staff who prepare patients for examination (initial VA recording, preliminary measurements).

**Goals:**
- Record initial visual acuity (VOsC) and basic measurements before the doctor enters
- Assist with investigations (dilation preparation, patient instructions)
- Does not sign or finalize any clinical documentation

**Access:** Limited EMR access (Sections A, D input only). No prescription generation. No billing.

---

### Role 7: Admin (System Administrator)

**Who:** Technical administrator, typically the clinic's IT-responsible person or the clinic owner in small practices.

**Goals:**
- Manage all users, roles, and location assignments
- Configure clinic settings (working hours, service catalog, notification templates)
- Access audit logs for GDPR compliance reviews
- Configure integrations (SMS gateway, email, payment, lab EDI)

**Access:** Full access to Admin & Settings module. Restricted from clinical EMR data (GDPR separation of duties).

---

### Role 8: Pacient (Patient — Portal Only)

**Who:** The clinic's patient accessing their own records through the Patient Portal.

**Goals:**
- Book appointments without calling the clinic
- View their consultation history, active prescriptions, and investigation results
- Track optical order status
- Manage GDPR consents and download their full medical record
- Complete PRO questionnaires (OSDI, VFQ-25, Amsler Grid) before appointments

**Key distinction:** This role has no access to the staff-facing application. The Patient Portal is a **separate UI context** (different layout, wider content, more whitespace, consumer-grade UX) running on the same backend. See GUIDE_02 for Patient Portal design specifications.

**Authentication:** Email + password, Google SSO, or CNP + date of birth (for elderly patients without email). Optional 2FA via SMS.

---

## 5. Product Scope

### 5.1 The 11 Modules (In Scope — Phase 1)

| # | Module | Primary Role(s) | Core Function |
|---|---|---|---|
| 1 | **Role-Aware Dashboard** | All roles | Personalized KPIs, alerts, and quick actions per role |
| 2 | **Patient Management** | Doctor, Receptionist, Manager | Demographics, medical history, consents, documents, consultation history |
| 3 | **Appointments & Calendar** | Receptionist, Doctor, Manager | Multi-resource scheduling, automated notifications, waitlist |
| 4 | **EMR — Clinical Consultation** | Doctor, Optometrist, Asistent | 9-section consultation form (VA, pupils, anterior/posterior segment, IOP, diagnoses, treatment plan, digital signature) |
| 5 | **Investigations** | Doctor, Optometrist | OCT, visual field, corneal topography, fundus photography, fluorescein angiography, biometry + IOL calculation |
| 6 | **Rețetă Oftalmologică** | Doctor | HL7 FHIR R4 VisionPrescription, digital signature, prescription → optical order pipeline |
| 7 | **Optical ERP** | Optician, Receptionist, Manager | Order Kanban, lab management, QC, stock management, POS billing, invoicing |
| 8 | **Notifications & Recall** | All (automated) | Appointment reminders, diagnosis-triggered recall, prescription expiry, post-sale follow-up |
| 9 | **Patient Portal** | Patient | Self-service booking, record access, PRO questionnaires, consent management, order tracking |
| 10 | **Reports & KPIs** | Manager, Doctor | Clinical, commercial, operational, and stock reports with export (PDF/Excel/CSV) |
| 11 | **Admin & Settings** | Admin, Manager | Users, roles, permissions matrix, clinic config, integrations, GDPR, audit log |

### 5.2 Explicitly Out of Scope (Phase 1)

The following will **not** be built in Phase 1. Mentioning them in sprint planning is grounds for deprioritization:

- **Insurance billing integration** (CAS / CNAS reimbursement) — Phase 2
- **Accounting software integration** (SAGA, SmartBill) — Phase 2 (invoices are generated but not synced to accounting)
- **Native mobile apps** (iOS/Android) — the PWA-capable web app is the only client in Phase 1
- **Real-time video telemedicine** (WebRTC implementation) — the appointment slot and UI placeholder exist, but actual video call infrastructure is Phase 2
- **AI-assisted diagnosis suggestions** — clinical decision support beyond rule-based alerts (e.g., IOP thresholds, recall protocols) is out of scope
- **PACS integration** (real DICOM server, DIMSE protocol) — the DICOM viewer is a UI component for displaying uploaded files; a full PACS integration is Phase 2
- **Multi-language support beyond RO/EN** — French, Hungarian, or other languages are not Phase 1
- **Public-facing clinic website builder** — the Patient Portal is a private, authenticated space; it is not a public clinic website

---

## 6. Competitive Context

### Why OphthaCloud beats the alternatives

| Criterion | OptiSoft | AppSmart | OphthaCloud |
|---|---|---|---|
| **Cloud-native** | ✗ Desktop only | ✓ | ✓ |
| **Ophthalmology-specific EMR** | Partial | ✗ Generic | ✓ Full |
| **Optical ERP integrated** | Partial | ✗ | ✓ |
| **Patient Portal** | ✗ | Basic | ✓ Full (PRO questionnaires, FHIR export) |
| **Automated recall** | ✗ | Basic | ✓ Diagnosis-driven |
| **GDPR-compliant architecture** | Partial | Partial | ✓ By design |
| **HL7 FHIR R4** | ✗ | ✗ | ✓ |
| **Dark mode** | ✗ | ✗ | ✓ (clinical relevance) |
| **Multi-tenant, multi-location** | ✗ | Partial | ✓ |
| **Modular RBAC per tenant** | ✗ | ✗ | ✓ |
| **Digital prescription signature** | ✗ | ✗ | ✓ |
| **IOP longitudinal trend charts** | ✗ | ✗ | ✓ |

### The design standard

The approved prototype (Prompt 2 / Lovable) was described by the client as superior "by far" to existing alternatives. The design system (color tokens, typography, clinical input patterns, dark sidebar) is **frozen and non-negotiable**. Any implementation that deviates from GUIDE_02 (Design System) in visual output requires explicit re-approval from the architect and client.

---

## 7. Success Metrics (Phase 1 KPIs)

These are the metrics that determine whether Phase 1 is considered successful. They are measurable within the prototype and early production environment.

### Product KPIs

| Metric | Target | Measured by |
|---|---|---|
| Consultation completion time (EMR, 9 sections, signature) | < 8 minutes | Session analytics |
| Prescription generation time (from signed consultation) | < 60 seconds | Analytics |
| Time from prescription to optical order creation | < 2 minutes | Order creation timestamp |
| Prescription → optical order conversion rate | ≥ 70% | Reports module |
| No-show rate for clinics using automated reminders | < 10% | Appointment data |
| Recall compliance (patients recalled who book) | ≥ 30% | Notification module |
| NPS score from doctor persona | ≥ 8.0 / 10 | Post-session survey |
| NPS score from patient portal | ≥ 7.5 / 10 | Portal survey |

### Technical KPIs (Phase 1 non-negotiables)

| Metric | Target |
|---|---|
| API response time (95th percentile, simple queries) | < 200ms |
| API response time (complex reports) | < 2,000ms |
| Frontend initial load (LCP) | < 2.0s on 4G |
| Uptime (Hetzner production) | ≥ 99.5% monthly |
| Zero patient data cross-tenant leakage | Absolute |
| GDPR audit log completeness | 100% of data access events logged |

---

## 8. Non-Technical Constraints

### 8.1 Legal & Regulatory Compliance (Romania)

All of the following are **hard constraints** — not optional features:

**GDPR (Regulation EU 2016/679):**
- Every access to patient personal data must be logged in the immutable audit log
- Patients have the right to: access their data (Art. 15), portability (Art. 20), erasure (Art. 17), restriction (Art. 18)
- Data breach notification to supervisory authority (ANSPDCP) within 72 hours
- Data Processing Agreements (DPA) must be in place with all sub-processors (Hetzner, SMTP provider, SMS gateway)
- Consent must be: freely given, specific, informed, unambiguous — mandatory consents (medical care, data processing) are locked ON; optional consents (marketing, research) are toggleable by patient at any time
- Data minimization: collect only what is clinically or operationally necessary

**Legea 46/2003 — Drepturile pacientului:**
- Patient right to informed consent for all medical procedures
- Patient right to access their own medical records
- Confidentiality obligations for all medical staff (enforced by role-based access controls)
- Minor patients (< 18): parental/guardian consent required for data processing

**Legea 95/2006 — Reforma în domeniul sănătății (Title IV — Medical practice):**
- Medical records must be retained for minimum 10 years after last consultation
- Digital signatures on medical documents must be verifiable
- Prescription validity: 1 year standard, extendable to 2 years for stable cases

**Ordinul MS 1101/2016 (Electronic health records):**
- EMR must record: date, time, author (name + license number), and content of every clinical entry
- Once digitally signed, a consultation record is legally immutable — no editing allowed, only addenda

### 8.2 HL7 FHIR R4 Compliance

The following resources must conform to **HL7 FHIR R4** specification:

| FHIR Resource | OphthaCloud Entity |
|---|---|
| `Patient` | Patient demographics |
| `Practitioner` | Doctor / Optometrist profiles |
| `Organization` | Clinic (tenant) |
| `Appointment` | Calendar appointment |
| `Encounter` | Clinical consultation |
| `VisionPrescription` | Rețetă oftalmologică |
| `Observation` | IOP readings, VA measurements, clinical findings |
| `DiagnosticReport` | Investigation results (OCT, VF, topography) |
| `Condition` | Diagnoses (ICD-10-CM coded) |
| `MedicationRequest` | Ophthalmic medication prescriptions |

FHIR compliance means: outbound API endpoints under `/fhir/r4/` must return valid FHIR JSON bundles. Internal data model does not need to be FHIR-structured — translation happens at the API layer.

### 8.3 ICD-10-CM Coding

All diagnoses in the EMR must be coded using **ICD-10-CM** (International Classification of Diseases, 10th revision, Clinical Modification). A pre-filtered catalog of ophthalmic codes (H00–H59 range) is embedded in the application. See GUIDE_06 (Module Specs) for the full list of supported codes.

### 8.4 Data Residency

All patient data — structured (PostgreSQL), unstructured (files, DICOM uploads), and logs — must reside in **EU data centers exclusively**. The chosen provider is **Hetzner Cloud** (Frankfurt, Germany / Helsinki, Finland). Any sub-processor introduced later (SMTP, SMS, analytics) must process EU resident data within the EU or under Standard Contractual Clauses (SCCs).

Under no circumstances should patient data be transmitted to US-based services without explicit legal basis and ANSPDCP notification.

### 8.5 Language

- **Application UI:** Romanian primary, English secondary (user preference per account)
- **All generated documents** (prescriptions, medical letters, invoices): Romanian only in Phase 1
- **Notification templates:** Romanian and English versions required from day one
- **Technical codebase** (variable names, comments, API field names, documentation): **English** throughout — this is a hard convention, not a preference
- **This document and all GUIDE_XX documents:** English

---

## 9. Key Architectural Decisions (Decision Log)

These decisions are **final for Phase 1**. Antigravity must not re-open them or suggest alternatives unless the architect explicitly requests a reconsideration.

### ADR-001: Spring Modulith over Microservices

**Decision:** The backend is a modular monolith using Spring Modulith, not a microservices architecture.

**Rationale:**
- 11 modules is well within the complexity range where a modular monolith outperforms microservices in development velocity, operational simplicity, and debuggability
- The team size (effectively 1 developer + AI agent) makes distributed systems overhead impractical
- Spring Modulith enforces module boundaries at compile time — the same discipline as microservices without the operational cost
- Migration to microservices is possible later because boundaries are already defined; the reverse is not true
- **Do not introduce separate Spring Boot applications, separate databases per module, or inter-service HTTP calls. All modules run in one JVM process sharing one PostgreSQL database.**

### ADR-002: Keycloak for Identity and Access Management

**Decision:** All authentication and authorization token issuance is handled by Keycloak, not custom Spring Security + JWT implementation.

**Rationale:**
- Keycloak Organizations feature (v25+) provides native multi-tenancy in a single realm
- OIDC / OAuth2 compliance is built in — reduces security risk vs. custom implementation
- MFA, SSO, and social login (Google) are available without custom code
- Spring Security is used downstream of Keycloak only for permission evaluation (custom `PermissionEvaluator`)

### ADR-003: Row-Level Multi-Tenancy (tenant_id column)

**Decision:** Multi-tenancy is implemented via a `tenant_id` column on all tenant-scoped tables, not separate schemas per tenant.

**Rationale:**
- Schema-per-tenant makes migrations significantly more complex (must run on N schemas)
- Row-level security in PostgreSQL enforces isolation at the database level
- Single schema simplifies backup, monitoring, and reporting
- **Every query touching tenant data must include a `tenant_id` filter. This is enforced via Hibernate filters applied at the Session level, not individual query parameters.**

### ADR-004: The Frontend Stack Originates from Lovable (Approved Prototype)

**Decision:** The frontend codebase is the React/TypeScript/Tailwind/shadcn-ui code exported from Lovable (the approved prototype). It is not rewritten from scratch.

**Rationale:**
- The client has explicitly approved the Lovable-generated design and declared it "by far" the preferred option
- Re-implementing the same design from scratch introduces regression risk with no benefit
- Lovable generates standard, maintainable React code with no proprietary lock-in
- The only adaptation required is replacing Lovable/Supabase data layer with a service layer pointing to the Spring Boot REST API

**Consequence:** Antigravity must not alter any UI component, CSS variable, color value, or layout that is visible in the prototype without explicit architect approval. API integration work is in `src/services/` — component files are read-only unless a bug fix is required.

### ADR-005: Hetzner Cloud for all Infrastructure

**Decision:** All production infrastructure runs on Hetzner Cloud (Frankfurt region).

**Rationale:**
- EU data residency by default (GDPR compliance)
- 4–8× cheaper than AWS/Azure/GCP for equivalent compute
- Sufficient for Phase 1 scale (< 50 concurrent clinics, < 500 concurrent users)
- No proprietary managed services that create lock-in — only standard VPS + managed PostgreSQL (optional) or self-managed Docker Swarm
- AWS Educate credits may be used for development/learning environments, not for production patient data

### ADR-006: PostgreSQL as the Single Source of Truth

**Decision:** PostgreSQL 16 is the only persistent data store. No MongoDB, no Cassandra, no DynamoDB.

**Rationale:**
- Medical records require ACID transactions — NoSQL eventually-consistent stores are inappropriate
- PostgreSQL's JSONB columns handle semi-structured clinical data (investigation results, custom fields) without requiring a separate document store
- Full-text search via PostgreSQL `tsvector` is sufficient for Phase 1 patient search — no Elasticsearch

---

## 10. Prototype Status & Transition

### What exists today

- A **fully interactive frontend prototype** built in Lovable, based on Prompt 2 (design system) + Prompt 2B (functional specification)
- The prototype uses mock/static data — no real backend
- The design has been **approved by the client** and is considered frozen
- The prototype code has been (or will be) exported from Lovable to GitHub

### What Phase 1 builds

Phase 1 transitions from the approved prototype to a **working pre-production system** with:
1. Real Spring Boot backend with Spring Modulith architecture
2. Keycloak authentication + RBAC matrix (DB-driven, per-tenant configurable)
3. PostgreSQL database with proper multi-tenant data model
4. All 11 modules functional with real data persistence
5. All frontend API mock calls replaced with real Spring Boot REST calls
6. Deployment on Hetzner Cloud (Docker Swarm)

### What Phase 1 does NOT include

- Performance optimization for > 50 concurrent tenants (Phase 2)
- Penetration testing and security audit (Phase 2 — before public launch)
- PACS/real DICOM server integration (Phase 2)
- Mobile app (Phase 2)
- Insurance billing integrations (Phase 2)

---

## 11. Glossary

| Term | Definition |
|---|---|
| **Tenant** | A single clinic or clinic chain — the unit of multi-tenancy. Each tenant has isolated data. |
| **EMR** | Electronic Medical Record — the digital clinical record for a patient consultation |
| **ERP** | Enterprise Resource Planning — the optical retail management component (orders, stock, billing) |
| **OD** | Oculus Dexter — right eye (Latin). Always displayed with a red badge. |
| **OS** | Oculus Sinister — left eye (Latin). Always displayed with a blue badge. |
| **OU** | Oculus Uterque — both eyes. Used in medication instructions. |
| **IOP** | Intraocular Pressure — measured in mmHg. Normal range: 10–21 mmHg. |
| **VA** | Visual Acuity — measured in Snellen notation (6/6, 6/12, etc.) |
| **BCVA** | Best Corrected Visual Acuity — VA with optimal correction |
| **VOsC** | Vedere fără corecție — uncorrected visual acuity (Romanian clinical abbreviation) |
| **SEQ** | Spherical Equivalent — calculated: Sph + (Cyl ÷ 2) |
| **Sph** | Sphere — refractive power in diopters |
| **Cyl** | Cylinder — astigmatic correction in diopters (minus convention in Romania) |
| **Axis** | Axis of astigmatism, in degrees (0–180°) |
| **Add** | Addition — near vision addition for presbyopia correction |
| **PD** | Pupillary Distance — in millimeters |
| **GAT** | Goldmann Applanation Tonometry — gold standard IOP measurement method |
| **OCT** | Optical Coherence Tomography — cross-sectional retinal imaging |
| **RNFL** | Retinal Nerve Fiber Layer — OCT measurement, key for glaucoma monitoring |
| **CDR** | Cup-to-Disc Ratio — optic disc excavation metric (glaucoma indicator) |
| **AMD** | Age-related Macular Degeneration — retinal condition |
| **IVT** | Intravitreal injection (injecție intravitreană) — anti-VEGF treatment for AMD/DR |
| **DR** | Diabetic Retinopathy (Retinopatie diabetică) |
| **PRO** | Patient Reported Outcome — standardized questionnaire (OSDI, VFQ-25, Amsler) |
| **FHIR** | Fast Healthcare Interoperability Resources — HL7 standard for health data exchange |
| **CNP** | Cod Numeric Personal — Romanian national identity number (13 digits) |
| **CIF** | Cod de Identificare Fiscală — Romanian fiscal identification code (companies) |
| **RBAC** | Role-Based Access Control |
| **ADR** | Architecture Decision Record — a documented, final architectural decision |
| **Sprint** | A 1–2 week development cycle delivering specific modules or features |
| **DPA** | Data Processing Agreement (GDPR required with sub-processors) |
| **ANSPDCP** | Autoritatea Națională de Supraveghere a Prelucrării Datelor cu Caracter Personal — Romanian data protection authority |

---

*End of GUIDE_00 — Project Brief*  
*Next document: GUIDE_01 — Architecture*
