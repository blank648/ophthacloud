# GUIDE_06 — Module Specifications: OphthaCloud

> **Document type:** Functional Specifications Reference
> **Version:** 1.0 | **Last updated:** 2026-04-02
> **Author:** Project Architect | **Status:** FINAL
> **Prerequisites:** GUIDE_00, GUIDE_01, GUIDE_03, GUIDE_04, GUIDE_05

***

## Scop

Acest document definește **business logic-ul pur** al fiecărui modul — regulile, workflow-urile, calculele automate și trigger-ele de notificare. UI-ul este descris în GUIDE_02. API-ul este descris în GUIDE_04. Aici se documentează *ce face* sistemul, nu *cum arată* sau *cum se apelează*.

***

## 1. Modul: Patients

### 1.1 Generarea MRN (Medical Record Number)

MRN-ul este unic per tenant și are formatul `OC-{6 cifre}`. Se generează automat la crearea pacientului:

```
1. SELECT MAX(numeric suffix) FROM patients WHERE tenant_id = :tenantId
2. Incrementează cu 1
3. Formatează cu zero-padding la 6 cifre: OC-000001, OC-000002...
4. Dacă nu există niciun pacient → primul MRN este OC-000001
```

Operația se execută în cadrul **aceleiași tranzacții** cu INSERT-ul pacientului, cu lock la nivel de row pe un tabel de secvențe per tenant, pentru a evita race conditions în medii cu trafic concurent.

### 1.2 Vârsta Calculată Automat

Câmpul `age` din response este **calculat la runtime** (nu stocat):

```java
int age = Period.between(patient.getDateOfBirth(), LocalDate.now()).getYears();
```

Nu se stochează în baza de date — se calculează la fiecare response.

### 1.3 Diagnoze Active

Lista `activeDiagnoses` din patient summary se construiește din ultima consultație semnată care conține Secțiunea G (Diagnostic) completată. Dacă există mai multe consultații semnate, se ia cea mai recentă.

### 1.4 Validări la Creare / Actualizare

- `firstName` + `lastName`: minim 2 caractere, maxim 100, fără cifre
- `dateOfBirth`: obligatoriu, nu poate fi în viitor, nu poate fi mai veche de 150 de ani
- `cnp`: dacă furnizat, validare algoritm CNP românesc (13 cifre, cifra de control)
- `phone`: format liber dar minim 10 caractere dacă furnizat
- `email`: validare RFC dacă furnizat
- `gender`: enum strict — `MALE`, `FEMALE`, `OTHER`


### 1.5 Invitarea la Portal

La `POST /api/v1/patients/{id}/portal-invite`:

1. Verifică că pacientul are `email` completat — dacă nu → 422
2. Verifică că `has_portal_access = false` — dacă deja are acces → 409
3. Creează user Keycloak cu rol `PATIENT` și atribut `patient_id`
4. Trimite email de invitație prin Keycloak (template configurat în realm)
5. Setează `has_portal_access = true`, `portal_invite_sent_at = NOW()`
6. Loghează în `audit_log` cu `action = 'PORTAL_INVITE_SENT'`

***

## 2. Modul: Appointments

### 2.1 Calculul `endAt`

`endAt` nu este trimis de frontend și nu este stocat — se calculează:

```java
LocalDateTime endAt = startAt.plus(Duration.ofMinutes(durationMinutes));
```

La citire din DB, `endAt` se calculează la runtime din `start_at + duration_minutes`.

### 2.2 Algoritmul Anti-Double Booking

La `POST` și `PUT` de programare, înainte de salvare:

```sql
SELECT COUNT(*) FROM appointments
WHERE tenant_id     = :tenantId
  AND doctor_id     = :doctorId
  AND id           != :currentId  -- exclude programarea curentă la UPDATE
  AND status       NOT IN ('CANCELLED', 'NO_SHOW', 'COMPLETED')
  AND start_at      < :proposedEndAt
  AND (start_at + (duration_minutes * INTERVAL '1 minute')) > :proposedStartAt
```

Dacă `COUNT > 0` → returnează 400 `DOUBLE_BOOKING` cu detalii despre conflictul existent.

Se verifică și `blocked_slots` în același pas:

```sql
SELECT COUNT(*) FROM blocked_slots
WHERE tenant_id  = :tenantId
  AND doctor_id  = :doctorId
  AND start_at   < :proposedEndAt
  AND end_at     > :proposedStartAt
```


### 2.3 State Machine Completă

```
BOOKED ──────────────────────────────────────────┐
   │                                              │
   ├──[doctor confirmă]──► CONFIRMED              │
   │      │                    │                  │
   │      │    [pacient soseşte] │                │
   │      └──► CHECKED_IN ◄────┘                  │
   │                │                             │
   │   [consultatia începe]                        │
   │                └──► IN_PROGRESS               │
   │                           │                  │
   │            [consultatia se finalizează]       │
   │                           └──► COMPLETED     │
   │                                              │
   └─────────────────────────────► CANCELLED ◄────┘
                                                  │
BOOKED/CONFIRMED ──[pacient nu se prezintă]──► NO_SHOW
```

Tranzițiile `CANCELLED` și `NO_SHOW` sunt terminale — nu pot fi reactivate. Dacă pacientul revine după `NO_SHOW`, se creează o programare nouă.

### 2.4 Event: `AppointmentStatusChangedEvent`

La orice schimbare de status se publică un Spring Modulith event:

```java
public record AppointmentStatusChangedEvent(
    String appointmentId,
    String patientId,
    String tenantId,
    AppointmentStatus previousStatus,
    AppointmentStatus newStatus,
    LocalDateTime changedAt
) {}
```

Modulul `notifications` ascultă acest event pentru a declanșa notificări automate (§7.3).

### 2.5 Sloturi Blocate (Blocked Slots)

Admin-ul sau doctorul poate bloca intervale (concedii, conferințe). Un slot blocat nu are pacient asociat. La crearea unui slot blocat se verifică dacă există programări active în acel interval — dacă da, lista este returnată și admin-ul trebuie să le reprogrameze manual înainte de a confirma blocarea.

***

## 3. Modul: EMR (Electronic Medical Records)

### 3.1 Cele 9 Secțiuni ale Consultației

| Cod | Denumire | Câmpuri cheie |
| :-- | :-- | :-- |
| **A** | Refracție \& Acuitate Vizuală | Sph, Cyl, Axis, Add, VA sc/cc OD/OS, PD |
| **B** | Biomicroscopie (Lampă cu Fantă) | Cornee, AC, Iris, Cristalin OD/OS + note |
| **C** | Tensiune Oculară (IOP) | IOP OD/OS, metodă (GAT/non-contact), ora măsurătorii |
| **D** | Oftalmoscopie \& Fundus | Papilă, cup/disc ratio, retină, macula, vase OD/OS |
| **E** | Câmp Vizual | Rezultate perimetrie, pattern deviation, MD, PSD |
| **F** | Investigații Suplimentare | Referințe la investigation IDs (OCT, topografie, etc.) |
| **G** | Diagnostic | Lista ICD-10, lateralitate, tip (principal/secundar/diferențial) |
| **H** | Tratament \& Medicație | Medicație curentă, prescripție recomandată, flags chirurgicale |
| **I** | Recomandări \& Plan | Control următor, trimiteri, instrucțiuni pacient, notițe |

### 3.2 Calculul Automat SEQ (Spherical Equivalent)

La salvarea Secțiunii A, dacă `sph` și `cyl` sunt completate:

```java
// Formula: SEQ = Sph + Cyl/2
double seqOd = sectionData.getOd().getSph() + (sectionData.getOd().getCyl() / 2.0);
double seqOs = sectionData.getOs().getSph() + (sectionData.getOs().getCyl() / 2.0);

// Rotunjire la 0.25D (standard clinic)
seqOd = Math.round(seqOd * 4.0) / 4.0;
seqOs = Math.round(seqOs * 4.0) / 4.0;
```

Rezultatul se salvează în `section_data.od.seq` și `section_data.os.seq` și este returnat în response.

### 3.3 Calculul Automat ADD (Near Addition)

La completarea Add Power în Secțiunea A:

```java
// Near VA predicted from distance Rx + Add
double nearSphOd = sectionData.getOd().getSph() + sectionData.getOd().getAddPower();
double nearSphOs = sectionData.getOs().getSph() + sectionData.getOs().getAddPower();
```


### 3.4 Validări Oftalmologice Secțiunea A

- `sph`: între -30.00 și +30.00 dioptrii, pași de 0.25
- `cyl`: între -10.00 și +10.00 dioptrii, pași de 0.25, **de regulă negativ** (convenție clinică — se loghează warning dacă pozitiv, nu eroare)
- `axis`: între 1 și 180 grade (nu 0, nu > 180)
- `add`: între 0.50 și 4.00 dioptrii (relevant numai pentru presbiopi, > 40 ani de obicei)
- `pd`: distanța pupilară binoculară între 50 și 80 mm; monoculară OD+OS sumă = binoculară ±2mm


### 3.5 Bitmask `sections_completed`

Câmpul `sections_completed` din tabela `consultations` este un integer de 9 biți:

```
Bit 0 (valoare 1)   = Secțiunea A completată
Bit 1 (valoare 2)   = Secțiunea B completată
Bit 2 (valoare 4)   = Secțiunea C completată
Bit 3 (valoare 8)   = Secțiunea D completată
Bit 4 (valoare 16)  = Secțiunea E completată
Bit 5 (valoare 32)  = Secțiunea F completată
Bit 6 (valoare 64)  = Secțiunea G completată ← OBLIGATORIE pentru semnare
Bit 7 (valoare 128) = Secțiunea H completată
Bit 8 (valoare 256) = Secțiunea I completată
```

La `PATCH .../complete` pe orice secțiune, backend-ul face OR bitmask:

```java
int sectionBit = 1 << sectionIndex(sectionCode); // A=0, B=1, ...I=8
consultation.setSectionsCompleted(consultation.getSectionsCompleted() | sectionBit);
```

Consultația completă are `sections_completed = 511` (toate 9 biți setați).

### 3.6 Pre-condiții pentru Semnare

Verificate în ordine, prima eșuare returnează eroarea corespunzătoare:

1. `consultation.status` trebuie să fie `DRAFT` sau `IN_PROGRESS` → altfel 409 `CONSULTATION_ALREADY_SIGNED`
2. Secțiunea G (Diagnostic) trebuie să aibă `is_completed = true` → altfel 422 cu mesaj `"Secțiunea G (Diagnostic) trebuie completată înainte de semnare."`
3. Utilizatorul curent trebuie să aibă permisiunea `emr.SIGN` → altfel 403
4. `consultationId` trebuie să aparțină aceluiași `tenant_id` ca cel din JWT → altfel 404

### 3.7 Addendum (Post-Semnare)

Un addendum nu modifică consultația originală. Se salvează în `consultation_sections` cu `section_code = 'ADDENDUM'` și un index de secvență. Poate fi adăugat doar de `ADMIN` sau de doctorul care a semnat consultația.

```
consultation_sections
  section_code = 'ADDENDUM_1'
  section_data = { "text": "Adăugat: ...", "addedBy": "Dr. X", "addedAt": "..." }
  is_completed = true
```


### 3.8 Template-uri Clinice

Un template reține `section_data` predefinit pentru o secțiune. La aplicare (`apply-template`), câmpurile din template suprascriu câmpurile goale din secțiunea curentă. Câmpurile deja completate manual **nu sunt suprascrise** (merge, nu replace).

Template-urile pot fi:

- `is_global = true` → vizibile tuturor doctorilor din tenant
- `is_global = false` → private, vizibile doar doctorului creator

***

## 4. Modul: Investigations

### 4.1 Categorii de Investigații

Categorii predefinite (enum în DB):

```
OCT             — Tomografie Optică Coherentă (RNFL, Macula, Disc)
VISUAL_FIELD    — Câmp Vizual (Humphrey, Octopus)
TOPOGRAPHY      — Topografie Corneală (Pentacam, Atlas)
ANGIOGRAPHY     — Angiografie Fluoresceină / ICG
BIOMETRY        — Biometrie (IOLMaster, Lenstar) — calcul IOL
ELECTROPHYSIOLOGY — ERG, VEP, EOG
PHOTOGRAPHY     — Retinografie (fundus foto)
PACHYMETRY      — Pahimetrie Corneală
OTHER           — Altele
```


### 4.2 Status Flow Investigație

```
ORDERED → IN_PROGRESS → COMPLETED
                     └──► CANCELLED
```

Tranziția `ORDERED → IN_PROGRESS` se face automat la primul upload de fișier asociat investigației.

### 4.3 Upload Fișiere — Validări

Tipuri acceptate: `DICOM`, `PDF`, `IMAGE` (JPEG, PNG, TIFF)
Dimensiune maximă per fișier: **50 MB**
Număr maxim de fișiere per investigație: **20**

La upload:

1. Fișierul se stochează în MinIO în path-ul: `{tenant_id}/investigations/{investigation_id}/{uuid}-{originalFileName}`
2. Se calculează SHA-256 hash și se stochează pentru verificarea integrității
3. Dacă `fileType = DICOM`, se extrage metadata DICOM (patient name, study date, modality) și se validează că corespunde pacientului din investigație
4. URL-ul presemnat din response expiră în **1 oră**

### 4.4 Interpretare Automată IOP (din Secțiunea C)

Valorile IOP introduse în Secțiunea C a EMR-ului sunt **copiate automat** ca investigație de tip `IOP_MEASUREMENT` în modulul Investigations, pentru a permite construirea graficelor de trend (GUIDE_04 §9.4).

Aceasta se realizează printr-un `@TransactionalEventListener` pe `ConsultationSectionSavedEvent`:

```java
@TransactionalEventListener
void onSectionSaved(ConsultationSectionSavedEvent event) {
    if (event.sectionCode().equals("C") && event.isCompleted()) {
        investigationService.recordIopMeasurement(
            event.patientId(),
            event.consultationId(),
            event.sectionData()
        );
    }
}
```


***

## 5. Modul: Prescriptions

### 5.1 Generarea Numărului de Prescripție

Format: `{PREFIX}-{YYYY}-{MRN-suffix}`

```java
String prescriptionNumber = String.format("%s-%d-%s",
    clinicSettings.getPrescriptionPrefix(),  // ex: "RX"
    LocalDate.now().getYear(),               // ex: 2026
    patient.getMrn().replace("OC-", "")     // ex: 004821
);
// Rezultat: RX-2026-004821
```

Dacă există deja o prescripție cu același număr (pacient cu mai multe prescripții în același an), se adaugă sufixul `-2`, `-3`, etc.

### 5.2 Generarea QR Code Token

```java
String qrToken = UUID.randomUUID().toString().replace("-", "") // 32 hex chars
    + Base62.encode(System.currentTimeMillis()); // timestamp suffix
// Rezultat: a3f8b2c1d4e5...2kX9mN
```

Token-ul este unic global și este stocat în `prescriptions.qr_code_token`. URL-ul QR:

```
https://app.ophthacloud.ro/verify/{qrToken}
```

Acest URL este **public** — se poate accesa fără autentificare de la orice optician care scanează codul.

### 5.3 Supersedere Automată

La semnarea unei prescripții noi pentru același pacient și același tip (`DISTANCE`, `NEAR`, `PROGRESSIVE`):

```sql
UPDATE prescriptions
SET status = 'SUPERSEDED', superseded_by_id = :newPrescriptionId
WHERE patient_id    = :patientId
  AND tenant_id     = :tenantId
  AND prescription_type = :type
  AND status        = 'ACTIVE';
```

Aceasta asigură că un pacient are cel mult **o prescripție activă per tip** la orice moment.

### 5.4 Expirare Automată

Un Spring Scheduler rulează zilnic la 01:00 și expiră prescripțiile cu `valid_until < CURRENT_DATE`:

```sql
UPDATE prescriptions
SET status = 'EXPIRED'
WHERE status = 'ACTIVE'
  AND valid_until < CURRENT_DATE;
```


### 5.5 Generare PDF

PDF-ul prescripției se generează cu **JasperReports** sau **iText** la primul acces. Template-ul include:

- Antetul clinicii (logo, adresă, CIF, cod parafă medic)
- Datele pacientului (nume, data nașterii, CNP mascat: `163XXXXXXXX`)
- Tabelul binocular OD/OS cu toate valorile refracției
- QR code embed (generare cu ZXing)
- Semnătura digitală a medicului (timestamp + hash)
- Footer cu `validFrom` — `validUntil`

PDF-ul se stochează în MinIO la `{tenant_id}/prescriptions/{prescription_id}/prescription.pdf` și este returnat ca URL presemnat (expirare 1h).

***

## 6. Modul: Optical ERP

### 6.1 Generarea Numărului de Comandă

Format: `{PREFIX}-{YYYY}-{6 cifre secvențial}`

```
CMD-2026-000001, CMD-2026-000002...
```

Secvența este per tenant, resetată anual (1 ianuarie).

### 6.2 Kanban — Stage Flow Complet

```
RECEIVED
   │
   ├──[trimis la laborator]──► SENT_TO_LAB
   │                               │
   │               [lentile primite de la lab]
   │                               │
   │                           QC_CHECK ──[QC eșuat]──► SENT_TO_LAB (retrimitere)
   │                               │
   │               [QC trecut, adaptare programată]
   │                               │
   │                      READY_FOR_FITTING
   │                               │
   │            [adaptare efectuată, pacient mulțumit]
   │                               │
   │                           COMPLETED
   │
   └──[anulare oricând înainte de COMPLETED]──► CANCELLED
```


### 6.3 QC Checklist (Quality Control)

La mutarea în `QC_CHECK`, se activează un checklist obligatoriu în UI care trebuie bifat complet înainte de a putea avansa la `READY_FOR_FITTING`:

```
□ Verificare valori gravate pe lentilă (Sph, Cyl, Axis) vs prescripție
□ Centrare optică OD
□ Centrare optică OS
□ Distanță interpupilară verificată
□ Înălțime segment (pentru progressive/bifocale) — N/A pentru single vision
□ Tratamente aplicate corect (AR, anti-albastru, fotocromatic)
□ Calitate montaj — fără șocuri, zârâituri, presiune inegală
□ Curățire finală
```

Rezultatul QC (PASS/FAIL + notes) se salvează în `optical_orders.qc_result` (JSONB).

### 6.4 Alertă Stoc Redus

La fiecare operație de deducere din stoc (adăugarea unui item la comandă), se verifică:

```java
if (stockItem.getQuantityOnHand() <= stockItem.getLowStockThreshold()) {
    eventPublisher.publishEvent(new LowStockAlertEvent(stockItem));
}
```

`LowStockAlertEvent` este preluat de modulul `notifications` care trimite un email/notificare in-app administratorului opticii.

Pragul implicit de `low_stock_threshold` este 5 unități, configurabil per item.

### 6.5 Calculul Automat Total Comandă

```java
BigDecimal subtotal = orderItems.stream()
    .map(item -> {
        BigDecimal lineTotal = item.getUnitPrice()
            .multiply(BigDecimal.valueOf(item.getQuantity()));
        BigDecimal discount = lineTotal
            .multiply(item.getDiscountPercent())
            .divide(BigDecimal.valueOf(100));
        return lineTotal.subtract(discount);
    })
    .reduce(BigDecimal.ZERO, BigDecimal::add);

BigDecimal totalAfterDeposit = subtotal.subtract(order.getDeposit());
```

Toate valorile monetare sunt stocate cu **4 zecimale** în DB (NUMERIC(12,4)) și returnate în API cu **2 zecimale**.

### 6.6 Generare Factură

La `POST /api/v1/optical/invoices`:

1. Se generează `invoice_number` în format `{PREFIX}/{YYYY}/{secvență}/{serie}` conform legislației fiscale române
2. Se calculează automat `vat_amount` pentru fiecare linie (`unit_price * quantity * vat_rate / 100`)
3. Se calculează `total_net`, `total_vat`, `total_gross`
4. Status inițial: `ISSUED`
5. La `PATCH .../pay`: status → `PAID`, `paid_at = NOW()`, `payment_method` stocat

***

## 7. Modul: Notifications

### 7.1 Arhitectura de Notificări

Modulul funcționează pe baza unui **event-driven pipeline**:

```
Spring Modulith Event
       ↓
NotificationRuleEngine
  (evaluează toate regulile active pentru tenant)
       ↓
NotificationJob (creat în notification_log cu status PENDING)
       ↓
NotificationScheduler (rulează la fiecare 5 minute)
       ↓
NotificationDispatcher
  ├── EmailDispatcher  (Spring Mail / SendGrid)
  └── SmsDispatcher    (Twilio / SMSLink RO)
       ↓
notification_log.status = SENT / FAILED
```


### 7.2 Tipuri de Trigger

| Trigger | Eveniment | Exemplu regulă |
| :-- | :-- | :-- |
| `APPOINTMENT_CONFIRMED` | Programare confirmată | "Trimite email confirmare cu detalii" |
| `APPOINTMENT_REMINDER` | N ore/zile înainte de programare | "SMS cu 24h înainte" |
| `APPOINTMENT_NO_SHOW` | Status schimbat în NO_SHOW | "Email cu ofertă reprogramare" |
| `RECALL` | Protocol recall activ pentru pacient | "Control glaucom la 3 luni" |
| `PRESCRIPTION_EXPIRING` | Prescripție expiră în N zile | "Reamintire reînnoire prescripție" |
| `ORDER_READY` | Comandă optică în READY_FOR_FITTING | "Ochelarii sunt gata!" |
| `LOW_STOCK` | Stoc sub prag | "Email admin: stoc redus Tamsulosin 0.4mg" |
| `BIRTHDAY` | Ziua de naștere a pacientului | "La mulți ani + ofertă control" |

### 7.3 Motorul de Template-uri

Template-urile de mesaje suportă variabile în format Mustache:

```
Variabile disponibile:
{{patientName}}         — numele complet al pacientului
{{patientFirstName}}    — prenumele
{{appointmentDate}}     — data programării (format localizat: "09 aprilie 2026")
{{appointmentTime}}     — ora programării ("09:30")
{{doctorName}}          — "Dr. Alexandru Ionescu"
{{clinicName}}          — "Clinica Oftalmologică Demo SRL"
{{clinicPhone}}         — numărul de contact al clinicii
{{orderNumber}}         — numărul comenzii optice
{{prescriptionExpiry}}  — data expirării prescripției
{{portalUrl}}           — link portal pacient
```


### 7.4 Protocoale de Recall — Logică de Calcul

Un protocol de recall definește intervalul de rechemare per diagnostic ICD-10:

```json
{
  "icd10Codes": ["H40.1", "H40.2"],
  "diagnosisName": "Glaucom",
  "recallIntervalMonths": 3,
  "description": "Control tensiune oculară și câmp vizual"
}
```

**Algoritmul de recall:**

```java
// Rulat zilnic la 03:00
List<Patient> patientsForRecall = patientRepository
    .findPatientsNeedingRecall(icd10Codes, LocalDate.now().plusDays(7));

// Un pacient "necesită recall" dacă:
// 1. Are diagnosticul ICD-10 din protocol în ultima consultație semnată
// 2. lastConsultationDate + recallIntervalMonths <= today + 7 zile (fereastra de anticipare)
// 3. NU are o programare activă (BOOKED/CONFIRMED) în viitor pentru același doctor
// 4. NU a primit o notificare de recall în ultimele 30 de zile (anti-spam)
```


### 7.5 Reîncercări la Eșec

La `FAILED`, notificarea se reîncearcă de **3 ori** cu backoff exponențial:

- Încercarea 1: imediat
- Încercarea 2: după 15 minute
- Încercarea 3: după 1 oră

După 3 eșecuri, `status = FAILED_FINAL` și se trimite alert intern administratorului.

***

## 8. Modul: Reports

### 8.1 KPI-uri Dashboard — Definiții

Toate KPI-urile se calculează **live** (nu sunt pre-agregate) la primul acces din zi, apoi se cachează în Redis timp de **15 minute**.

**`todayAppointments`:**

```sql
SELECT
    COUNT(*) as total,
    COUNT(*) FILTER (WHERE status IN ('COMPLETED')) as completed,
    COUNT(*) FILTER (WHERE status NOT IN ('COMPLETED','CANCELLED','NO_SHOW')) as pending
FROM appointments
WHERE tenant_id = :tenantId
  AND DATE(start_at AT TIME ZONE 'Europe/Bucharest') = CURRENT_DATE;
```

**`weekRevenue`:**

```sql
SELECT COALESCE(SUM(total_gross), 0)
FROM invoices
WHERE tenant_id = :tenantId
  AND status = 'PAID'
  AND DATE(paid_at) >= DATE_TRUNC('week', CURRENT_DATE)
  AND DATE(paid_at) < DATE_TRUNC('week', CURRENT_DATE) + INTERVAL '7 days';
```

`trendPercent` = comparație față de săptămâna precedentă.

**`pendingOrders`:**

```sql
SELECT COUNT(*) FROM optical_orders
WHERE tenant_id = :tenantId
  AND stage NOT IN ('COMPLETED','CANCELLED');
```

`overdueCount` = comenzi în `SENT_TO_LAB` de mai mult de 14 zile (pragul configurat în `clinic_settings`).

### 8.2 IOP Trend — Construirea Seriei

Seria de trend IOP pentru un pacient se construiește din tabelul `investigations` (tip `IOP_MEASUREMENT`) sau direct din `consultation_sections` cu `section_code = 'C'` și `is_completed = true`:

```sql
SELECT
    c.consultation_date,
    c.id as consultation_id,
    (cs.section_data -> 'od' ->> 'iop')::float as iop_od,
    (cs.section_data -> 'os' ->> 'iop')::float as iop_os,
    cs.section_data -> 'od' ->> 'method' as method
FROM consultation_sections cs
JOIN consultations c ON cs.consultation_id = c.id
WHERE c.patient_id  = :patientId
  AND c.tenant_id   = :tenantId
  AND c.status      = 'SIGNED'
  AND cs.section_code = 'C'
  AND cs.is_completed = true
ORDER BY c.consultation_date ASC;
```


***

## 9. Modul: Admin

### 9.1 Crearea unui Staff Member

`POST /api/v1/admin/staff` declanșează un workflow în doi pași în aceeași tranzacție:

**Pasul 1 — Creare Keycloak User:**

```
keycloak.users().create(UserRepresentation) → returnează Keycloak user ID
keycloak.users().get(userId).roles().add(TENANT_MEMBER role)
keycloak.users().get(userId).sendVerifyEmail() — dacă sendInviteEmail = true
```

**Pasul 2 — Creare staff_member în DB:**

```sql
INSERT INTO staff_members
  (tenant_id, keycloak_user_id, first_name, last_name, email, phone,
   role, specialization, license_number, is_active)
VALUES (:tenantId, :keycloakUserId, ...)
```

**Pasul 3 — Setare atribute Keycloak:**

```
user.attributes["tenant_id"] = tenantId
user.attributes["staff_id"]  = staffMemberId (din DB)
user.attributes["staff_role"] = role
user.attributes["permissions_json"] = defaultPermissionsForRole
```

Dacă oricare dintre pași eșuează, se face rollback complet (inclusiv ștergerea user-ului Keycloak dacă a fost creat).

### 9.2 Modificarea Permisiunilor

La `PUT /api/v1/admin/permissions`:

1. Se actualizează `tenant_role_module_permissions` în DB
2. Se regenerează `permissions_json` pentru toți utilizatorii cu rolul respectiv
3. Se actualizează atributul `permissions_json` pe fiecare user Keycloak afectat
4. Sesiunile active ale utilizatorilor afectați sunt **invalidate** (forțat re-login) prin Keycloak Session API — asigură că noul JWT cu permisiunile actualizate este emis la proxima autentificare

### 9.3 Clinic Settings — Validări

- `workingHours`: `open` < `close` pentru fiecare zi activă
- `bookingSlotMinutes`: valori acceptate — 10, 15, 20, 30, 45, 60
- `bookingAdvanceDays`: între 1 și 365
- `vatRateDefault`: 0, 5, 9, 19 (cotele TVA valide în România)
- Prefixele (invoice, order, prescription) — maxim 5 caractere, alfanumeric

***

## 10. Modul: Portal Pacient

### 10.1 Autentificare

Pacienții se autentifică prin același Keycloak realm `ophthacloud`, dar prin client-ul `ophthacloud-portal`. JWT-ul lor conține:

```json
{
  "staff_role": "PATIENT",
  "tenant_id": "uuid",
  "patient_id": "uuid"   ← în loc de staff_id
}
```

Spring Security identifică rolul `PATIENT` și direcționează toate request-urile exclusiv spre endpoint-urile `/api/v1/portal/me/...`.

### 10.2 Vizibilitate Date

Pacientul vede **exclusiv propriile date**. `SecurityUtils.currentPatientId()` extrage `patient_id` din JWT și îl injectează în toate query-urile — pacientul nu poate specifica un alt `patientId` în request.

### 10.3 Restricții

- **Read-only** pentru toate modulele medicale (consultații, investigații, prescripții)
- **Poate descărca** PDF-uri prescripții proprii
- **Poate actualiza** consimțămintele GDPR proprii
- **Nu poate** crea programări (funcționalitate portal Phase 2 — dacă clientul confirmă)
- **Nu poate** vedea notițele interne ale doctorilor (`internal_notes` din programări — câmp exclus din serialiazarea portal)

***

## 11. Modul: Audit

### 11.1 Ce se Auditează

**Obligatoriu** (orice modificare care afectează date medicale sau de acces):


| Acțiune | Entitate |
| :-- | :-- |
| CREATE, UPDATE, DELETE | patients, consultations, prescriptions |
| SIGN | consultations, prescriptions |
| VIEW | consultations, prescriptions (acces dosar medical) |
| UPLOAD, DOWNLOAD | investigation_files, prescription PDFs |
| STAGE_CHANGE | optical_orders |
| LOGIN, LOGOUT | staff_members, patients (portal) |
| PERMISSION_CHANGE | tenant_role_module_permissions |
| GDPR_ERASURE | patients |
| PORTAL_INVITE_SENT | patients |
| ADDENDUM | consultation_sections |

**Nu se auditează** (prea mult noise): GET list endpoints, refresh token, health checks.

### 11.2 Capturarea IP-ului

IP-ul clientului se extrage din request cu fallback pentru proxy:

```java
String ip = Optional.ofNullable(request.getHeader("X-Forwarded-For"))
    .map(h -> h.split(",")[0].trim())
    .orElse(request.getRemoteAddr());
```


***