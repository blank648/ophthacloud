# GUIDE_04 — API Contract: OphthaCloud

> **Document type:** API Contract Reference  
> **Version:** 1.0  
> **Last updated:** 2026-04-02  
> **Author:** Project Architect  
> **Status:** FINAL — all endpoints, request shapes, and response shapes defined here are the contract.  
> **Prerequisites:** GUIDE_00, GUIDE_01, GUIDE_03

---

## Purpose

This document is the single source of truth for how the **Next.js frontend** and the **Spring Boot backend** communicate.

Every endpoint, every request field, every response field, and every error code is defined here.
If it is not in this document, it does not exist yet — both sides must implement from this spec,
not invent their own variations.

---

## 1. Global Conventions

### 1.1 Base URLs

```
Development:  http://localhost:8080/api/v1
Production:   https://api.ophthacloud.ro/api/v1

FHIR R4 base (separate prefix):
Development:  http://localhost:8080/fhir/r4
Production:   https://api.ophthacloud.ro/fhir/r4
```

### 1.2 HTTP Methods

| Method | Semantics |
|---|---|
| `GET` | Read — never has a request body |
| `POST` | Create a new resource |
| `PUT` | Replace an existing resource entirely |
| `PATCH` | Partial update — only sent fields are changed |
| `DELETE` | Soft delete (sets `is_active = false`) — hard delete only where explicitly noted |

### 1.3 Headers (All Requests)

```
Authorization: Bearer <keycloak_access_token>
Content-Type: application/json
Accept: application/json
```

### 1.4 Field Naming: camelCase

All JSON field names use **camelCase** — both in requests sent by the frontend and responses returned by the backend. Never snake_case in JSON.

```json
// CORRECT
{ "firstName": "Gheorghe", "dateOfBirth": "1963-08-15", "tenantId": "uuid" }

// WRONG
{ "first_name": "Gheorghe", "date_of_birth": "1963-08-15", "tenant_id": "uuid" }
```

Jackson configuration in Spring Boot (`application.yml`):
```yaml
spring:
  jackson:
    property-naming-strategy: LOWER_CAMEL_CASE
    default-property-inclusion: NON_NULL
    serialization:
      write-dates-as-timestamps: false
    deserialization:
      fail-on-unknown-properties: false
```

### 1.5 Date & Time Formats

| Type | Format | Example |
|---|---|---|
| Date only | ISO 8601 `YYYY-MM-DD` | `"2026-04-02"` |
| DateTime with TZ | ISO 8601 UTC `YYYY-MM-DDTHH:mm:ssZ` | `"2026-04-02T09:30:00Z"` |
| Time only | `HH:mm` | `"14:30"` |
| Duration | minutes as integer | `30` |

Frontend displays in `Europe/Bucharest` timezone (UTC+2/+3). The conversion is always done on the frontend — the API always sends and receives UTC.

### 1.6 Success Response Envelope

**Single resource:**
```json
{
  "data": {
    "id": "uuid",
    "firstName": "Gheorghe"
  }
}
```

**Collection (paginated):**
```json
{
  "data": [...],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 187,
    "totalPages": 10,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

**Collection (non-paginated — small reference lists):**
```json
{
  "data": [...]
}
```

**No-content responses (DELETE, status change):**
HTTP 204 No Content, no body.

### 1.7 Error Response Envelope

All 4xx and 5xx responses return:

```json
{
  "error": {
    "code": "PATIENT_NOT_FOUND",
    "message": "Patient with ID abc-123 not found.",
    "timestamp": "2026-04-02T09:30:00Z",
    "path": "/api/v1/patients/abc-123",
    "requestId": "req-uuid-here"
  }
}
```

**Error codes and their HTTP status:**

| HTTP Status | Error Code | Meaning |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Request body fails validation (field errors in `details` array) |
| 400 | `INVALID_DATE_RANGE` | `startAt` >= `endAt`, or invalid date combination |
| 400 | `DOUBLE_BOOKING` | Doctor already has an appointment in the requested slot |
| 401 | `UNAUTHORIZED` | Missing or expired JWT token |
| 403 | `FORBIDDEN` | Valid token but insufficient permissions for this action |
| 404 | `{ENTITY}_NOT_FOUND` | e.g. `PATIENT_NOT_FOUND`, `CONSULTATION_NOT_FOUND` |
| 409 | `CONSULTATION_ALREADY_SIGNED` | Attempt to edit a signed consultation |
| 409 | `PRESCRIPTION_ALREADY_CANCELLED` | Attempt to sign/cancel an already-cancelled prescription |
| 409 | `DUPLICATE_MRN` | MRN already exists for this tenant |
| 422 | `SEQ_COMPUTATION_ERROR` | Cannot compute SEQ — missing Sph or Cyl values |
| 429 | `RATE_LIMIT_EXCEEDED` | Too many requests |
| 500 | `INTERNAL_ERROR` | Unexpected server error |

**Validation error response (400 VALIDATION_ERROR):**
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request body contains invalid fields.",
    "details": [
      { "field": "dateOfBirth", "code": "REQUIRED", "message": "Data nașterii este obligatorie." },
      { "field": "phone", "code": "INVALID_FORMAT", "message": "Număr de telefon invalid." }
    ],
    "timestamp": "2026-04-02T09:30:00Z",
    "path": "/api/v1/patients",
    "requestId": "req-uuid-here"
  }
}
```

### 1.8 Pagination Query Parameters

```
page      integer, default 0     — zero-based page index
size      integer, default 20    — items per page, max 100
sort      string                 — field name, e.g. "lastName"
direction string, default "asc"  — "asc" or "desc"
```

### 1.9 Soft Delete Behavior

`DELETE /api/v1/{resource}/{id}` sets `is_active = false`. The record remains in the database.
All GET endpoints filter `WHERE is_active = true` by default.
Add `?includeInactive=true` to GET list endpoints to include soft-deleted records (requires `ADMIN` role).

---

## 2. Module: Patients

### 2.1 List Patients

```
GET /api/v1/patients
```

**Query Parameters:**

| Parameter | Type | Description |
|---|---|---|
| `q` | string | Full-text search: first name, last name, phone, email, MRN |
| `page` | int | Default 0 |
| `size` | int | Default 20 |
| `sort` | string | Default `lastName` |
| `direction` | string | Default `asc` |

**Response 200:**
```json
{
  "data": [
    {
      "id": "33333333-0000-0000-0000-000000000001",
      "mrn": "OC-004821",
      "firstName": "Gheorghe",
      "lastName": "Ionescu",
      "dateOfBirth": "1963-08-15",
      "age": 62,
      "gender": "MALE",
      "phone": "0722 111 222",
      "email": "g.ionescu@email.ro",
      "hasPortalAccess": false,
      "isActive": true,
      "activeDiagnoses": [
        { "icd10Code": "H40.1", "icd10Name": "Glaucom cu unghi deschis", "laterality": "BILATERAL" }
      ],
      "lastConsultationDate": "2026-01-15",
      "nextAppointmentAt": "2026-04-02T09:00:00Z",
      "createdAt": "2025-03-10T08:00:00Z"
    }
  ],
  "pagination": {
    "page": 0, "size": 20, "totalElements": 187, "totalPages": 10,
    "hasNext": true, "hasPrevious": false
  }
}
```

### 2.2 Create Patient

```
POST /api/v1/patients
```

**Request Body:**
```json
{
  "firstName": "Gheorghe",
  "lastName": "Ionescu",
  "dateOfBirth": "1963-08-15",
  "gender": "MALE",
  "phone": "0722 111 222",
  "email": "g.ionescu@email.ro",
  "cnp": "1630815XXXXXX",
  "address": "Str. Exemplu nr. 5",
  "city": "București",
  "county": "Ilfov",
  "bloodType": "A+",
  "occupation": "Pensionar",
  "insuranceProvider": "CNAS",
  "insuranceNumber": "INS-123456",
  "emergencyContactName": "Ana Ionescu",
  "emergencyContactPhone": "0722 999 888",
  "notes": ""
}
```

**Required fields:** `firstName`, `lastName`, `dateOfBirth`, `gender`

**Response 201:**
```json
{
  "data": {
    "id": "uuid",
    "mrn": "OC-004826",
    ...all patient fields...
  }
}
```

### 2.3 Get Patient

```
GET /api/v1/patients/{id}
```

**Response 200:** Full patient object including `medicalHistory`, `consents` summary, and `statistics`:
```json
{
  "data": {
    "id": "uuid",
    "mrn": "OC-004821",
    "firstName": "Gheorghe",
    "lastName": "Ionescu",
    ...all core fields...,
    "medicalHistory": {
      "hasDiabetes": false,
      "hasGlaucomaHistory": true,
      "activeDiagnoses": [...],
      "knownAllergies": null,
      "currentMedications": "Latanoprost 0.005%"
    },
    "statistics": {
      "totalConsultations": 8,
      "totalPrescriptions": 3,
      "totalInvestigations": 5,
      "totalOpticalOrders": 2,
      "lastVisitDate": "2026-01-15"
    }
  }
}
```

### 2.4 Update Patient

```
PUT /api/v1/patients/{id}
```

Request body: same as Create (full replacement). Returns updated patient object (200).

### 2.5 Soft Delete Patient

```
DELETE /api/v1/patients/{id}
```

Response: 204 No Content.

### 2.6 Get Patient Consultation History

```
GET /api/v1/patients/{id}/consultations?page=0&size=10
```

Returns paginated list of consultation summaries (id, date, doctor, status, primaryDiagnosis).

### 2.7 Get Patient Prescriptions

```
GET /api/v1/patients/{id}/prescriptions?status=ACTIVE
```

Optional filter: `status` (ACTIVE | EXPIRED | CANCELLED | SUPERSEDED | all)

### 2.8 Get Patient Appointments

```
GET /api/v1/patients/{id}/appointments?from=2026-01-01&to=2026-12-31
```

### 2.9 Invite Patient to Portal

```
POST /api/v1/patients/{id}/portal-invite
```

Request body: `{}` (no body needed — email is taken from patient record)
Response 200: `{ "data": { "invitedAt": "2026-04-02T09:00:00Z" } }`

---

## 3. Module: Appointments

### 3.1 List Appointments (Calendar Feed)

```
GET /api/v1/appointments
```

**Query Parameters:**

| Parameter | Type | Description |
|---|---|---|
| `from` | date (YYYY-MM-DD) | **Required** — start of range |
| `to` | date (YYYY-MM-DD) | **Required** — end of range (max 31 days span) |
| `doctorId` | uuid | Filter by doctor |
| `status` | string | Filter by status (comma-separated) |
| `patientId` | uuid | All appointments for one patient |

**Response 200:**
```json
{
  "data": [
    {
      "id": "uuid",
      "patientId": "uuid",
      "patientName": "Gheorghe Ionescu",
      "patientMrn": "OC-004821",
      "patientAvatarUrl": null,
      "activeDiagnosisFlags": ["H40.1"],
      "doctorId": "uuid",
      "doctorName": "Dr. Alexandru Ionescu",
      "appointmentTypeId": "uuid",
      "appointmentTypeName": "Control Glaucom",
      "appointmentTypeColor": "#13759C",
      "startAt": "2026-04-02T07:00:00Z",
      "endAt": "2026-04-02T07:30:00Z",
      "durationMinutes": 30,
      "status": "CHECKED_IN",
      "channel": "IN_PERSON",
      "room": "Cabinet 1",
      "chiefComplaint": "Control glaucom — vedere încetoșată matinal",
      "consultationId": null,
      "createdAt": "2026-03-15T10:00:00Z"
    }
  ]
}
```

Note: non-paginated — calendar always fetches a date range.

### 3.2 Create Appointment

```
POST /api/v1/appointments
```

**Request Body:**
```json
{
  "patientId": "uuid",
  "doctorId": "uuid",
  "appointmentTypeId": "uuid",
  "startAt": "2026-04-10T08:00:00Z",
  "durationMinutes": 30,
  "channel": "IN_PERSON",
  "room": "Cabinet 1",
  "chiefComplaint": "Control anual",
  "patientNotes": "Pacientul preferă dimineața",
  "internalNotes": ""
}
```

**Required fields:** `patientId`, `doctorId`, `startAt`, `durationMinutes`

**Validation:**
- If doctor has any appointment with status IN (BOOKED, CONFIRMED, CHECKED_IN, IN_PROGRESS) that overlaps [`startAt`, `endAt`] → return 400 `DOUBLE_BOOKING`
- If `startAt` falls in a `blocked_slots` record for the doctor → return 400 `DOUBLE_BOOKING`

**Response 201:** Full appointment object.

### 3.3 Get Appointment

```
GET /api/v1/appointments/{id}
```

Returns full appointment object including patient snapshot and linked consultation ID if exists.

### 3.4 Update Appointment

```
PUT /api/v1/appointments/{id}
```

Same body as create. Only allowed if status is BOOKED or CONFIRMED.
Returns 409 if status is CHECKED_IN or beyond.

### 3.5 Update Appointment Status

```
PATCH /api/v1/appointments/{id}/status
```

**Request Body:**
```json
{
  "status": "CHECKED_IN",
  "cancellationReason": null
}
```

**Allowed status transitions:**

```
BOOKED → CONFIRMED, CANCELLED, NO_SHOW
CONFIRMED → CHECKED_IN, CANCELLED, NO_SHOW
CHECKED_IN → IN_PROGRESS
IN_PROGRESS → COMPLETED
COMPLETED → (terminal)
CANCELLED → (terminal)
NO_SHOW → (terminal)
```

Response 200: updated appointment. Invalid transition → 400 `INVALID_STATUS_TRANSITION`.

### 3.6 Delete Appointment

```
DELETE /api/v1/appointments/{id}
```

Only allowed if status is BOOKED or CONFIRMED. Sets status = CANCELLED.

### 3.7 List Appointment Types

```
GET /api/v1/appointment-types
```

Response: all active types for the tenant (no pagination).

### 3.8 Create / Update Appointment Type

```
POST /api/v1/appointment-types
PUT  /api/v1/appointment-types/{id}
```

```json
{
  "name": "Control Glaucom",
  "colorHex": "#13759C",
  "durationMinutes": 30,
  "description": ""
}
```

---

## 4. Module: EMR (Consultations)

### 4.1 Create Consultation

```
POST /api/v1/emr/consultations
```

**Request Body:**
```json
{
  "patientId": "uuid",
  "appointmentId": "uuid",
  "consultationDate": "2026-04-02",
  "chiefComplaint": "Control glaucom — vedere încetoșată matinal"
}
```

Creates the consultation record AND creates 9 empty `consultation_sections` rows (A–I) in one transaction.
Sets `appointments.consultation_id` if `appointmentId` provided.

**Response 201:**
```json
{
  "data": {
    "id": "uuid",
    "patientId": "uuid",
    "appointmentId": "uuid",
    "doctorId": "uuid",
    "doctorName": "Dr. Alexandru Ionescu",
    "status": "DRAFT",
    "consultationDate": "2026-04-02",
    "chiefComplaint": "...",
    "sectionsCompleted": 0,
    "sections": {
      "A": { "id": "uuid", "sectionCode": "A", "isCompleted": false, "sectionData": {} },
      "B": { "id": "uuid", "sectionCode": "B", "isCompleted": false, "sectionData": {} },
      ...
    },
    "createdAt": "2026-04-02T07:00:00Z"
  }
}
```

### 4.2 Get Consultation

```
GET /api/v1/emr/consultations/{id}
```

Returns full consultation with all sections and their `sectionData` JSONB populated.

### 4.3 Get Consultation Section

```
GET /api/v1/emr/consultations/{id}/sections/{sectionCode}
```

`sectionCode` is A through I (uppercase).

**Response 200:**
```json
{
  "data": {
    "id": "uuid",
    "consultationId": "uuid",
    "sectionCode": "A",
    "isCompleted": false,
    "sectionData": {
      "od": { "sph": null, "cyl": null, "axis": null, "add": null, "vaCC": null, "seq": null },
      "os": { "sph": null, "cyl": null, "axis": null, "add": null, "vaCC": null, "seq": null },
      "pdBinocular": null,
      "notes": ""
    },
    "updatedAt": "2026-04-02T07:05:00Z"
  }
}
```

### 4.4 Save Consultation Section (Auto-Save)

```
PUT /api/v1/emr/consultations/{id}/sections/{sectionCode}
```

**Request Body:**
```json
{
  "sectionData": {
    "od": { "sph": -2.50, "cyl": -0.75, "axis": 90, "add": null, "vaCC": "6/6" },
    "os": { "sph": -3.00, "cyl": -0.50, "axis": 85, "add": null, "vaCC": "6/9" },
    "pdBinocular": 64.0,
    "notes": ""
  },
  "isCompleted": false
}
```

- Returns 409 if consultation `status = SIGNED`
- Backend validates JSONB structure against the section schema
- Backend auto-computes SEQ fields for Section A (sph + cyl/2) if sph and cyl are present
- Returns 200 with updated section including computed SEQ values

**Response 200:**
```json
{
  "data": {
    "sectionCode": "A",
    "isCompleted": false,
    "sectionData": {
      "od": { "sph": -2.50, "cyl": -0.75, "axis": 90, "vaCC": "6/6", "seq": -2.875 },
      "os": { "sph": -3.00, "cyl": -0.50, "axis": 85, "vaCC": "6/9", "seq": -3.25 }
    },
    "updatedAt": "2026-04-02T07:10:00Z"
  }
}
```

### 4.5 Mark Section Complete

```
PATCH /api/v1/emr/consultations/{id}/sections/{sectionCode}/complete
```

Request body: `{}` — no fields needed.
Sets `is_completed = true`, `completed_at = NOW()`.
Returns 200 with updated consultation `sectionsCompleted` bitmask.

### 4.6 Sign Consultation (Digital Signature)

```
POST /api/v1/emr/consultations/{id}/sign
```

**Request Body:**
```json
{
  "signatureConfirmation": true
}
```

**Pre-conditions checked by backend:**
1. Consultation status must be DRAFT or IN_PROGRESS (not already SIGNED)
2. Section G (Diagnosis) must be completed (`is_completed = true`)
3. Calling user must have `emr.SIGN` permission

**On success:**
- Sets `status = SIGNED`, `signed_at = NOW()`, `signed_by_id = current_user`
- Publishes `ConsultationSignedEvent`
- Returns 200 with full consultation including `signedAt`

**Response 200:**
```json
{
  "data": {
    "id": "uuid",
    "status": "SIGNED",
    "signedAt": "2026-04-02T09:45:00Z",
    "signedById": "uuid",
    "doctorName": "Dr. Alexandru Ionescu"
  }
}
```

### 4.7 List Consultations

```
GET /api/v1/emr/consultations?patientId={id}&from=2026-01-01&to=2026-12-31&page=0&size=10
```

Returns paginated list of consultation summaries.

### 4.8 Clinical Templates

```
GET  /api/v1/emr/templates?sectionCode=B
POST /api/v1/emr/templates
PUT  /api/v1/emr/templates/{id}
DELETE /api/v1/emr/templates/{id}
```

**POST request body:**
```json
{
  "name": "Biomicroscopie normală bilateral",
  "sectionCode": "B",
  "category": "Normal bilateral",
  "isGlobal": true,
  "templateData": {
    "od": { "cornea": "transparentă", "ac": "normală profunzime", "iris": "normocromă", "cristalin": "transparent" },
    "os": { "cornea": "transparentă", "ac": "normală profunzime", "iris": "normocromă", "cristalin": "transparent" },
    "templateUsed": "Biomicroscopie normală bilateral"
  }
}
```

### 4.9 Apply Template to Section

```
POST /api/v1/emr/consultations/{id}/sections/{sectionCode}/apply-template/{templateId}
```

Request body: `{}` — pre-fills `section_data` with template values, marks fields with amber tint signal in response.
Returns 200 with updated section data.

---

## 5. Module: Investigations

### 5.1 List Investigations

```
GET /api/v1/investigations?patientId={id}&category=OCT&status=ORDERED&page=0&size=20
```

### 5.2 Create Investigation Order

```
POST /api/v1/investigations
```

**Request Body:**
```json
{
  "patientId": "uuid",
  "consultationId": "uuid",
  "category": "OCT",
  "name": "OCT RNFL OD+OS",
  "device": "Heidelberg Spectralis",
  "isUrgent": false,
  "notes": ""
}
```

Response 201: investigation object with `status = ORDERED`.

### 5.3 Get Investigation

```
GET /api/v1/investigations/{id}
```

Returns full object including `resultData` and list of attached files.

### 5.4 Update Investigation Result

```
PUT /api/v1/investigations/{id}
```

**Request Body:**
```json
{
  "status": "COMPLETED",
  "performedAt": "2026-04-02T09:00:00Z",
  "resultData": {
    "device": "Heidelberg Spectralis",
    "protocol": "RNFL",
    "od": { "averageRnfl": 89, "superior": 112, "inferior": 115, "temporal": 68, "nasal": 72 },
    "os": { "averageRnfl": 76, "superior": 95, "inferior": 98, "temporal": 61, "nasal": 64 }
  },
  "interpretation": "OD - limita normala. OS - subtiere RNFL sectoarele superior si inferior — compatibil glaucom."
}
```

### 5.5 Upload Investigation File

```
POST /api/v1/investigations/{id}/files
Content-Type: multipart/form-data
```

Form fields: `file` (binary), `fileType` (DICOM|PDF|IMAGE), `laterality` (OD|OS|BOTH)

**Response 201:**
```json
{
  "data": {
    "id": "uuid",
    "fileName": "oct_rnfl_od_20260402.pdf",
    "fileSizeBytes": 245760,
    "mimeType": "application/pdf",
    "laterality": "OD",
    "downloadUrl": "https://minio.../presigned-url?expires=3600"
  }
}
```

### 5.6 Get File Download URL

```
GET /api/v1/investigations/{id}/files/{fileId}/download-url
```

**Response 200:**
```json
{
  "data": {
    "downloadUrl": "https://minio.../presigned-url",
    "expiresAt": "2026-04-02T10:00:00Z"
  }
}
```

---

## 6. Module: Prescriptions

### 6.1 List Prescriptions

```
GET /api/v1/prescriptions?patientId={id}&status=ACTIVE&page=0&size=20
```

### 6.2 Create Prescription

```
POST /api/v1/prescriptions
```

**Request Body:**
```json
{
  "patientId": "uuid",
  "consultationId": "uuid",
  "prescriptionType": "DISTANCE",
  "validFrom": "2026-04-02",
  "validUntil": "2027-04-02",
  "pdBinocular": 64.0,
  "pdOd": 32.0,
  "pdOs": 32.0,
  "lensType": "SINGLE_VISION",
  "lensMaterial": "Poliuretan 1.67",
  "lensCoating": "Antireflectantă + tratament anti-albastru",
  "frameRecommendation": "Ramă mică, distanța nasopupilară redusă",
  "clinicalNotes": "",
  "patientInstructions": "De purtat permanent.",
  "lines": [
    {
      "eye": "OD",
      "sph": -2.50,
      "cyl": -0.75,
      "axis": 90,
      "addPower": null,
      "vaCc": "6/6"
    },
    {
      "eye": "OS",
      "sph": -3.00,
      "cyl": -0.50,
      "axis": 85,
      "addPower": null,
      "vaCc": "6/9"
    }
  ]
}
```

Backend auto-generates `prescriptionNumber` (e.g. `RX-2026-004821`) and `qrCodeToken`.
Response 201: full prescription object.

### 6.3 Get Prescription

```
GET /api/v1/prescriptions/{id}
```

Returns full object including lines, issuing doctor, status, QR token URL.

### 6.4 Sign Prescription

```
POST /api/v1/prescriptions/{id}/sign
```

Request body: `{ "signatureConfirmation": true }`
Sets status = ACTIVE (prescription was created in ACTIVE state, sign means doctor confirms).
If consultation had a previous ACTIVE prescription, sets that one to SUPERSEDED.
Publishes `PrescriptionSignedEvent`.
Response 200: updated prescription.

### 6.5 Cancel Prescription

```
POST /api/v1/prescriptions/{id}/cancel
```

Request body: `{ "reason": "Prescripție eronată." }`
Returns 409 if already CANCELLED or EXPIRED.
Response 200.

### 6.6 Get Prescription PDF Download URL

```
GET /api/v1/prescriptions/{id}/pdf
```

Generates PDF on demand (if not already generated), stores in MinIO, returns presigned URL.

**Response 200:**
```json
{
  "data": {
    "downloadUrl": "https://minio.../presigned-url",
    "expiresAt": "2026-04-02T10:00:00Z"
  }
}
```

### 6.7 Verify Prescription via QR (Public Endpoint)

```
GET /api/v1/public/prescriptions/verify/{qrToken}
```

**No Authorization header required.** Used by optician shops scanning the QR code.

**Response 200:**
```json
{
  "data": {
    "prescriptionNumber": "RX-2026-004821",
    "patientName": "Gheorghe Ionescu",
    "dateOfBirth": "1963-08-15",
    "prescriptionType": "DISTANCE",
    "status": "ACTIVE",
    "validFrom": "2026-04-02",
    "validUntil": "2027-04-02",
    "issuedByName": "Dr. Alexandru Ionescu",
    "clinicName": "Clinica Oftalmologică Demo SRL",
    "lines": [
      { "eye": "OD", "sph": -2.50, "cyl": -0.75, "axis": 90, "seq": -2.875 },
      { "eye": "OS", "sph": -3.00, "cyl": -0.50, "axis": 85, "seq": -3.25 }
    ],
    "pdBinocular": 64.0
  }
}
```

Returns 404 if token not found, 200 with `status: EXPIRED` if expired (does not return 404 for expired).

---

## 7. Module: Optical ERP

### 7.1 List Optical Orders

```
GET /api/v1/optical/orders?stage=RECEIVED&patientId={id}&page=0&size=20
```

Optional filters: `stage`, `patientId`, `from`, `to`, `assignedToId`

### 7.2 Create Optical Order

```
POST /api/v1/optical/orders
```

**Request Body:**
```json
{
  "patientId": "uuid",
  "prescriptionId": "uuid",
  "consultationId": "uuid",
  "orderType": "GLASSES",
  "labName": "Essilor România",
  "notes": "Ramă aleasă: Silhouette 5530",
  "deposit": 200.00,
  "items": [
    {
      "description": "Lentile Essilor Varilux X Series 1.67 AR",
      "quantity": 2,
      "unitPrice": 650.00,
      "eye": null,
      "discountPercent": 10.0,
      "lensSpecifications": {
        "od": { "sph": -2.50, "cyl": -0.75, "axis": 90, "add": null },
        "os": { "sph": -3.00, "cyl": -0.50, "axis": 85, "add": null }
      }
    },
    {
      "description": "Ramă Silhouette 5530",
      "quantity": 1,
      "unitPrice": 450.00,
      "eye": null,
      "discountPercent": 0
    }
  ]
}
```

Backend auto-generates `orderNumber` (e.g. `CMD-2026-001234`).
Response 201: full order object with `stage = RECEIVED`.

### 7.3 Get Optical Order

```
GET /api/v1/optical/orders/{id}
```

Returns full object including items, patient snapshot, prescription summary.

### 7.4 Update Order Stage (Kanban Move)

```
PATCH /api/v1/optical/orders/{id}/stage
```

**Request Body:**
```json
{
  "stage": "SENT_TO_LAB",
  "labReference": "ESS-2026-48291",
  "notes": ""
}
```

**Allowed stage transitions:**
```
RECEIVED → SENT_TO_LAB, CANCELLED
SENT_TO_LAB → QC_CHECK, CANCELLED
QC_CHECK → READY_FOR_FITTING, SENT_TO_LAB (if QC fail — back to lab)
READY_FOR_FITTING → COMPLETED
COMPLETED → (terminal)
CANCELLED → (terminal)
```

Publishes `OpticalOrderStatusChangedEvent`. Returns 200 with updated order.

### 7.5 Stock Management

```
GET  /api/v1/optical/stock?category=FRAME&lowStockOnly=false&page=0&size=50
POST /api/v1/optical/stock
PUT  /api/v1/optical/stock/{id}
PATCH /api/v1/optical/stock/{id}/adjust
```

**PATCH adjust (restock or correction):**
```json
{
  "adjustment": 10,
  "reason": "Received new shipment from supplier"
}
```

### 7.6 Service Catalog

```
GET    /api/v1/optical/services?category=LENS&page=0&size=50
POST   /api/v1/optical/services
PUT    /api/v1/optical/services/{id}
DELETE /api/v1/optical/services/{id}
```

### 7.7 Invoices

```
GET  /api/v1/optical/invoices?patientId={id}&status=ISSUED&page=0&size=20
POST /api/v1/optical/invoices
GET  /api/v1/optical/invoices/{id}
```

**POST request body:**
```json
{
  "patientId": "uuid",
  "opticalOrderId": "uuid",
  "lines": [
    { "description": "Lentile Essilor Varilux X Series", "quantity": 2, "unitPrice": 585.00, "vatRate": 19.00 },
    { "description": "Ramă Silhouette 5530", "quantity": 1, "unitPrice": 450.00, "vatRate": 19.00 }
  ],
  "discountTotal": 0,
  "notes": ""
}
```

**Mark Invoice Paid:**
```
PATCH /api/v1/optical/invoices/{id}/pay
```
```json
{
  "paymentMethod": "CARD",
  "paymentReference": "TXN-AB12345",
  "amountPaid": 1620.00
}
```

**Get Invoice PDF:**
```
GET /api/v1/optical/invoices/{id}/pdf
```
Same response pattern as prescription PDF.

---

## 8. Module: Notifications

### 8.1 Notification Rules

```
GET    /api/v1/notifications/rules
POST   /api/v1/notifications/rules
PUT    /api/v1/notifications/rules/{id}
DELETE /api/v1/notifications/rules/{id}
PATCH  /api/v1/notifications/rules/{id}/toggle
```

**POST request body:**
```json
{
  "name": "Recall Glaucom — 3 luni",
  "configData": {
    "triggerType": "RECALL",
    "recallProtocolId": "uuid",
    "timingOffsetsDays": [-7, -1],
    "channels": ["EMAIL", "SMS"],
    "templateEmailSubject": "Reamintire programare control glaucom — {{clinicName}}",
    "templateEmailBody": "Stimate/ă {{patientName}}, ...",
    "templateSms": "Control glaucom programat pentru {{appointmentDate}}.",
    "isActive": true
  }
}
```

### 8.2 Notification Log

```
GET /api/v1/notifications/log?patientId={id}&status=FAILED&page=0&size=50
```

**Response:**
```json
{
  "data": [
    {
      "id": "uuid",
      "patientName": "Gheorghe Ionescu",
      "channel": "SMS",
      "status": "SENT",
      "recipientAddress": "0722 111 222",
      "subject": null,
      "bodyPreview": "Control glaucom programat pentru 09.04.2026.",
      "sentAt": "2026-04-02T08:00:00Z",
      "externalMessageId": "SMS-ID-123456"
    }
  ]
}
```

### 8.3 Recall Protocols

```
GET    /api/v1/notifications/recall-protocols
POST   /api/v1/notifications/recall-protocols
PUT    /api/v1/notifications/recall-protocols/{id}
DELETE /api/v1/notifications/recall-protocols/{id}
```

---

## 9. Module: Reports & KPIs

All report endpoints are `GET` only. No pagination — responses are aggregated data for charts and KPI cards.

### 9.1 Dashboard KPIs

```
GET /api/v1/reports/dashboard-kpis
```

**Response 200:**
```json
{
  "data": {
    "todayAppointments": { "count": 24, "completed": 8, "pending": 16 },
    "weekRevenue": { "amount": 12450.00, "currency": "RON", "trendPercent": 8.5 },
    "activePatients": { "count": 847, "newThisMonth": 23 },
    "pendingOrders": { "count": 12, "overdueCount": 2 },
    "pendingRecalls": { "count": 34 },
    "lowStockItems": { "count": 5 },
    "upcomingAppointments": [
      {
        "startAt": "2026-04-02T07:30:00Z",
        "patientName": "Gheorghe Ionescu",
        "status": "CHECKED_IN",
        "typeName": "Control Glaucom"
      }
    ]
  }
}
```

### 9.2 Appointment Statistics

```
GET /api/v1/reports/appointments?from=2026-01-01&to=2026-03-31&groupBy=week
```

**Response 200:**
```json
{
  "data": {
    "groupBy": "week",
    "series": [
      { "period": "2026-W01", "total": 112, "completed": 98, "noShow": 8, "cancelled": 6 }
    ],
    "byStatus": { "COMPLETED": 872, "NO_SHOW": 45, "CANCELLED": 31 },
    "byType": { "Control Glaucom": 234, "Consultație inițială": 156 }
  }
}
```

### 9.3 Revenue Statistics

```
GET /api/v1/reports/revenue?from=2026-01-01&to=2026-03-31&groupBy=month
```

### 9.4 IOP Trends (Per Patient)

```
GET /api/v1/reports/patients/{id}/iop-trends?from=2024-01-01&to=2026-04-02
```

**Response 200:**
```json
{
  "data": {
    "patientId": "uuid",
    "patientName": "Gheorghe Ionescu",
    "series": [
      {
        "date": "2026-01-15",
        "consultationId": "uuid",
        "od": { "iop": 16, "method": "GAT" },
        "os": { "iop": 18, "method": "GAT" }
      }
    ]
  }
}
```

### 9.5 Patient Demographics

```
GET /api/v1/reports/patients/demographics
```

Returns age distribution, gender distribution, top 10 diagnoses, registration trend.

---

## 10. Module: Admin

### 10.1 Staff Management

```
GET    /api/v1/admin/staff?role=DOCTOR&page=0&size=20
POST   /api/v1/admin/staff
PUT    /api/v1/admin/staff/{id}
DELETE /api/v1/admin/staff/{id}
```

**POST request body (creates Keycloak user + staff_member record):**
```json
{
  "firstName": "Ioana",
  "lastName": "Popa",
  "email": "dr.popa@clinica-demo.ro",
  "phone": "0733 456 789",
  "role": "DOCTOR",
  "specialization": "Oftalmologie",
  "licenseNumber": "CMR-12345",
  "sendInviteEmail": true
}
```

### 10.2 Permissions Matrix

```
GET /api/v1/admin/permissions?role=DOCTOR
PUT /api/v1/admin/permissions
```

**GET Response:**
```json
{
  "data": [
    {
      "role": "DOCTOR",
      "moduleCode": "emr",
      "canView": true, "canCreate": true, "canEdit": true,
      "canDelete": false, "canSign": true, "canExport": true
    }
  ]
}
```

**PUT request body (replace full permissions for one role):**
```json
{
  "role": "RECEPTIONIST",
  "permissions": [
    { "moduleCode": "patients", "canView": true, "canCreate": true, "canEdit": true, "canDelete": false, "canSign": false, "canExport": false },
    { "moduleCode": "appointments", "canView": true, "canCreate": true, "canEdit": true, "canDelete": true, "canSign": false, "canExport": false }
  ]
}
```

### 10.3 Clinic Settings

```
GET /api/v1/admin/settings
PUT /api/v1/admin/settings
```

**GET Response:**
```json
{
  "data": {
    "workingHours": {
      "mon": { "open": "08:00", "close": "18:00" },
      "fri": { "open": "08:00", "close": "16:00" },
      "sat": { "open": "09:00", "close": "13:00" },
      "sun": null
    },
    "bookingSlotMinutes": 15,
    "bookingAdvanceDays": 90,
    "currency": "RON",
    "vatRateDefault": 19.00,
    "portalEnabled": true,
    "portalAppointmentBooking": false,
    "invoicePrefix": "FC",
    "orderNumberPrefix": "CMD",
    "prescriptionPrefix": "RX"
  }
}
```

---

## 11. Audit Log (Read-Only)

```
GET /api/v1/audit/log?entityType=consultations&entityId={id}&from=2026-01-01&page=0&size=50
```

**Query parameters:** `entityType`, `entityId`, `userId`, `action`, `from`, `to`

**Response:**
```json
{
  "data": [
    {
      "id": "uuid",
      "userEmail": "dr.ionescu@clinica-demo.ro",
      "userRole": "DOCTOR",
      "action": "SIGN",
      "entityType": "consultations",
      "entityId": "uuid",
      "changedFields": null,
      "ipAddress": "192.168.1.10",
      "occurredAt": "2026-04-02T09:45:00Z"
    }
  ]
}
```

---

## 12. Patient Portal API

These endpoints are called by authenticated patients (Keycloak `patient` role). They have read-only access to their own data only.

```
GET /api/v1/portal/me                            — patient profile
GET /api/v1/portal/me/appointments               — upcoming appointments
GET /api/v1/portal/me/prescriptions              — active prescriptions
GET /api/v1/portal/me/prescriptions/{id}         — prescription detail with QR display
GET /api/v1/portal/me/investigations             — investigation results
GET /api/v1/portal/me/optical-orders             — optical order status
GET /api/v1/portal/me/notifications              — messages from clinic
PUT /api/v1/portal/me/consents                   — update GDPR consent
```

All portal endpoints: tenant is determined from JWT `tenant_id`. Patient can only see their own records — `patientId` from JWT is always enforced server-side, never from a query parameter.

---

## 13. FHIR R4 Endpoints

These endpoints follow the [FHIR R4 specification](https://hl7.org/fhir/R4/) and are used for interoperability with external health systems. Authentication is the same JWT as the main API.

### Base URL: `/fhir/r4`

### Supported Resources

| FHIR Resource | Map to | Endpoint |
|---|---|---|
| `Patient` | `patients` table | `/fhir/r4/Patient` |
| `Practitioner` | `staff_members` table | `/fhir/r4/Practitioner` |
| `Appointment` | `appointments` table | `/fhir/r4/Appointment` |
| `Encounter` | `consultations` table | `/fhir/r4/Encounter` |
| `VisionPrescription` | `prescriptions` + `prescription_lines` | `/fhir/r4/VisionPrescription` |

### FHIR Patient (Example)

```
GET /fhir/r4/Patient/{id}
GET /fhir/r4/Patient?identifier=OC-004821
```

**Response 200:**
```json
{
  "resourceType": "Patient",
  "id": "33333333-0000-0000-0000-000000000001",
  "meta": { "versionId": "1", "lastUpdated": "2026-04-02T09:00:00Z" },
  "identifier": [
    { "system": "https://ophthacloud.ro/patient-mrn", "value": "OC-004821" }
  ],
  "name": [
    { "use": "official", "family": "Ionescu", "given": ["Gheorghe"] }
  ],
  "telecom": [
    { "system": "phone", "value": "0722 111 222", "use": "mobile" },
    { "system": "email", "value": "g.ionescu@email.ro" }
  ],
  "gender": "male",
  "birthDate": "1963-08-15",
  "address": [
    { "use": "home", "text": "Str. Exemplu nr. 5", "city": "București", "country": "RO" }
  ]
}
```

### FHIR VisionPrescription (Example)

```
GET /fhir/r4/VisionPrescription/{id}
```

**Response 200:**
```json
{
  "resourceType": "VisionPrescription",
  "id": "prescription-uuid",
  "status": "active",
  "created": "2026-04-02",
  "patient": { "reference": "Patient/33333333-0000-0000-0000-000000000001" },
  "prescriber": { "reference": "Practitioner/doctor-uuid" },
  "lensSpecification": [
    {
      "eye": "right",
      "sphere": -2.50,
      "cylinder": -0.75,
      "axis": 90,
      "add": null,
      "prism": []
    },
    {
      "eye": "left",
      "sphere": -3.00,
      "cylinder": -0.50,
      "axis": 85
    }
  ]
}
```

### FHIR Operations (Phase 1 Scope)

Phase 1 implements **read-only FHIR** (GET only). Write operations via FHIR (`POST`, `PUT`) are not in scope for Phase 1.

---

## 14. Backend Implementation Notes

### Spring Controller Structure

Each module's REST controller lives in `internal/` and maps to the API paths:

```java
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
@Tag(name = "Patients", description = "Patient management")
class PatientController {

    private final PatientManagementFacade patientFacade;

    @GetMapping
    @PreAuthorize("hasPermission('patients', 'VIEW')")
    ResponseEntity<ApiResponse<Page<PatientSummaryDto>>> listPatients(
        @RequestParam(required = false) String q,
        @PageableDefault(size = 20, sort = "lastName") Pageable pageable
    ) { ... }

    @PostMapping
    @PreAuthorize("hasPermission('patients', 'CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    ResponseEntity<ApiResponse<PatientDto>> createPatient(
        @Valid @RequestBody CreatePatientRequest request
    ) { ... }
}
```

### API Response Wrapper

```java
public record ApiResponse<T>(T data) {}
public record PagedApiResponse<T>(List<T> data, PaginationMeta pagination) {}
public record PaginationMeta(int page, int size, long totalElements, int totalPages, boolean hasNext, boolean hasPrevious) {}
public record ErrorResponse(ErrorDetail error) {}
public record ErrorDetail(String code, String message, String timestamp, String path, String requestId, List<FieldError> details) {}
```

### Permission Annotation Mapping

```java
@PreAuthorize("hasPermission('patients', 'VIEW')")    // canView
@PreAuthorize("hasPermission('patients', 'CREATE')")  // canCreate
@PreAuthorize("hasPermission('patients', 'EDIT')")    // canEdit
@PreAuthorize("hasPermission('patients', 'DELETE')")  // canDelete
@PreAuthorize("hasPermission('emr', 'SIGN')")         // canSign
@PreAuthorize("hasPermission('reports', 'EXPORT')")   // canExport
```

`hasPermission` is implemented by `OphthaClinicalPermissionEvaluator` (see GUIDE_05).

### Swagger / OpenAPI

Every controller and DTO must have SpringDoc annotations:
- `@Tag(name = "ModuleName")` on controller class
- `@Operation(summary = "...", description = "...")` on each endpoint method
- `@Schema(description = "...")` on each DTO field
- Swagger UI available at `/swagger-ui.html` in all environments

---

## 15. Endpoint Reference Index

Quick reference of all endpoints:

```
PATIENTS
  GET    /api/v1/patients
  POST   /api/v1/patients
  GET    /api/v1/patients/{id}
  PUT    /api/v1/patients/{id}
  DELETE /api/v1/patients/{id}
  GET    /api/v1/patients/{id}/consultations
  GET    /api/v1/patients/{id}/prescriptions
  GET    /api/v1/patients/{id}/appointments
  GET    /api/v1/patients/{id}/investigations
  POST   /api/v1/patients/{id}/portal-invite

APPOINTMENTS
  GET    /api/v1/appointments
  POST   /api/v1/appointments
  GET    /api/v1/appointments/{id}
  PUT    /api/v1/appointments/{id}
  PATCH  /api/v1/appointments/{id}/status
  DELETE /api/v1/appointments/{id}
  GET    /api/v1/appointment-types
  POST   /api/v1/appointment-types
  PUT    /api/v1/appointment-types/{id}

EMR
  GET    /api/v1/emr/consultations
  POST   /api/v1/emr/consultations
  GET    /api/v1/emr/consultations/{id}
  GET    /api/v1/emr/consultations/{id}/sections/{code}
  PUT    /api/v1/emr/consultations/{id}/sections/{code}
  PATCH  /api/v1/emr/consultations/{id}/sections/{code}/complete
  POST   /api/v1/emr/consultations/{id}/sections/{code}/apply-template/{templateId}
  POST   /api/v1/emr/consultations/{id}/sign
  GET    /api/v1/emr/templates
  POST   /api/v1/emr/templates
  PUT    /api/v1/emr/templates/{id}
  DELETE /api/v1/emr/templates/{id}

INVESTIGATIONS
  GET    /api/v1/investigations
  POST   /api/v1/investigations
  GET    /api/v1/investigations/{id}
  PUT    /api/v1/investigations/{id}
  POST   /api/v1/investigations/{id}/files
  GET    /api/v1/investigations/{id}/files/{fileId}/download-url

PRESCRIPTIONS
  GET    /api/v1/prescriptions
  POST   /api/v1/prescriptions
  GET    /api/v1/prescriptions/{id}
  POST   /api/v1/prescriptions/{id}/sign
  POST   /api/v1/prescriptions/{id}/cancel
  GET    /api/v1/prescriptions/{id}/pdf
  GET    /api/v1/public/prescriptions/verify/{qrToken}   ← public, no auth

OPTICAL
  GET    /api/v1/optical/orders
  POST   /api/v1/optical/orders
  GET    /api/v1/optical/orders/{id}
  PUT    /api/v1/optical/orders/{id}
  PATCH  /api/v1/optical/orders/{id}/stage
  GET    /api/v1/optical/stock
  POST   /api/v1/optical/stock
  PUT    /api/v1/optical/stock/{id}
  PATCH  /api/v1/optical/stock/{id}/adjust
  GET    /api/v1/optical/services
  POST   /api/v1/optical/services
  PUT    /api/v1/optical/services/{id}
  DELETE /api/v1/optical/services/{id}
  GET    /api/v1/optical/invoices
  POST   /api/v1/optical/invoices
  GET    /api/v1/optical/invoices/{id}
  PATCH  /api/v1/optical/invoices/{id}/pay
  GET    /api/v1/optical/invoices/{id}/pdf

NOTIFICATIONS
  GET    /api/v1/notifications/rules
  POST   /api/v1/notifications/rules
  PUT    /api/v1/notifications/rules/{id}
  DELETE /api/v1/notifications/rules/{id}
  PATCH  /api/v1/notifications/rules/{id}/toggle
  GET    /api/v1/notifications/log
  GET    /api/v1/notifications/recall-protocols
  POST   /api/v1/notifications/recall-protocols
  PUT    /api/v1/notifications/recall-protocols/{id}
  DELETE /api/v1/notifications/recall-protocols/{id}

REPORTS
  GET    /api/v1/reports/dashboard-kpis
  GET    /api/v1/reports/appointments
  GET    /api/v1/reports/revenue
  GET    /api/v1/reports/patients/demographics
  GET    /api/v1/reports/patients/{id}/iop-trends

ADMIN
  GET    /api/v1/admin/staff
  POST   /api/v1/admin/staff
  PUT    /api/v1/admin/staff/{id}
  DELETE /api/v1/admin/staff/{id}
  GET    /api/v1/admin/permissions
  PUT    /api/v1/admin/permissions
  GET    /api/v1/admin/settings
  PUT    /api/v1/admin/settings

AUDIT
  GET    /api/v1/audit/log

PORTAL
  GET    /api/v1/portal/me
  GET    /api/v1/portal/me/appointments
  GET    /api/v1/portal/me/prescriptions
  GET    /api/v1/portal/me/prescriptions/{id}
  GET    /api/v1/portal/me/investigations
  GET    /api/v1/portal/me/optical-orders
  GET    /api/v1/portal/me/notifications
  PUT    /api/v1/portal/me/consents

FHIR R4
  GET    /fhir/r4/Patient
  GET    /fhir/r4/Patient/{id}
  GET    /fhir/r4/Practitioner/{id}
  GET    /fhir/r4/Appointment/{id}
  GET    /fhir/r4/Encounter/{id}
  GET    /fhir/r4/VisionPrescription/{id}
```

---

*End of GUIDE_04 — API Contract*  
*Next document: GUIDE_05 — Security*
