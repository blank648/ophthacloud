

# Batch 2 Implementation: EMR Consultation + Prescriptions + Optical ERP + Remaining Modules

## Current State
- **Done**: Login, 5 dashboards, patient list, patient profile (5 tabs), appointments calendar, demo data
- **Still PlaceholderPage stubs** (8 routes): `/consultation`, `/investigations`, `/prescriptions`, `/optical`, `/inventory`, `/billing`, `/reports`, `/notifications`, `/settings`, `/audit`
- Patient portal (`/portal`) exists but is basic

## Implementation Plan

Due to the massive scope (8+ full module pages), this will be split across multiple implementation passes. This pass covers **EMR Consultation + Prescriptions + Optical ERP**.

---

### File 1: `src/pages/ConsultationPage.tsx` (~600 lines)
Split layout EMR form with all 9 sections:
- **Left panel (40%)**: Sticky patient context card (Ion Marinescu pre-loaded) + vertical stepper A–I with completion circles + progress bar
- **Right panel (60%)**: Active section form, scrollable independently
- **Section A**: OD/OS dual-column refraction grid — JetBrains Mono inputs for Sph (±0.25D steps), Cyl, Axis, Add, PD, VA selects (6/6 through NPL). Auto-computed SEQ = Sph+(Cyl/2). VA <6/12 warning banner. Cycloplegic checkbox
- **Section B**: Pupil reactions radios (direct/APD/consensual/accommodation) + motility (cover test, nistagmus) per eye
- **Section C**: Anterior segment structured fields per anatomical structure (pleoape, conjunctivă, cornee, iris, cameră anterioară, cristalin) with Normal/free-text per OD/OS
- **Section D**: Large IOP inputs (JetBrains Mono, text-xl) with live color: ≤21 green, 22-25 amber+banner, >25 red+alert. Method radios (GAT/Air/iCare). Time field
- **Section E**: Posterior segment (disc optic CDR, artere/vene, maculă, retină, vitros) per OD/OS
- **Section F**: ICD-10 autocomplete (pre-loaded ophthalmic catalog ~25 codes), selected as colored pills, primary star toggle, template recommender banners triggered by H40.x/H35.0x/H35.3x
- **Section G**: Medication list (add rows: drug + dose + eye + frequency + duration), "Generează rețetă" button, laser/surgical section, follow-up date picker
- **Section H**: 8 template buttons (2×4 grid) — click auto-fills relevant sections with amber-tinted fields
- **Section I**: Summary card (completion count, primary diagnosis, doctor info), "Semnează Digital" button → locks all fields read-only, green confirmation banner, document download buttons

### File 2: `src/pages/PrescriptionsPage.tsx` (~350 lines)
- **List view**: Table with ID, patient, date, OD/OS summary (JetBrains Mono "−2.50 / −0.75 / 90°"), status badge, doctor, actions. Search + status/date filters
- **Detail view** (inline or modal): A5-proportion card with clinic header + "REȚETĂ OFTALMOLOGICĂ" title. Refraction table (OD red border, OS blue border), all values JetBrains Mono. Lens specs as chip tags. Clinical notes. Status timeline (Emisă → Comandă → Producție → Finalizată) with milestone dots
- **Actions**: Descarcă PDF (print-preview modal), "Generează Comandă Optică" (amber button → creates order + navigates to /optical), Reînnoire, Anulează

### File 3: `src/pages/OpticalERPPage.tsx` (~400 lines)
- **5-column Kanban**: Nouă / Lab în Lucru / Control Calitate / Gata Montaj / Finalizat. Drag-and-drop between columns using HTML drag API
- **Order cards**: Patient avatar+name, prescription ID (monospace), frame brand + lens type chips, date, SLA indicator (green/red), lab name
- **Detail slide-in panel** (620px, from right): Patient summary link, full refraction table, frame+lens specs, QC checklist (6 checkboxes), lab communication thread (demo messages), invoice button (when Finalizat)
- Pre-populated with 5 demo orders from `opticalOrders` data

### File 4: `src/pages/InvestigationsPage.tsx` (~350 lines)
- **Investigation list**: Table with type, patient, date, status, doctor. Status workflow chips
- **Detail views per type**: OCT (RNFL thickness, macular volume, clinical flags), Visual Field (MD/PSD/GHT, reliability, GPA), Topography (K values, pachimetry, keratoconus indices), Fundus (image metadata, findings), Biometry (axial length, IOL calculation table)
- **DICOM-style viewer panel**: Dark background (#0F1923), 2×2 grid, toolbar (zoom/fit/fullscreen icons), metadata labels in JetBrains Mono, timeline scrubber

### File 5: `src/pages/ReportsPage.tsx` (~400 lines)
- **4 tabs**: Clinici / Comerciali / Operaționali / Stocuri
- **Global controls**: Date range picker, location filter, doctor filter, export buttons (PDF/Excel/CSV)
- **Clinici**: 6 KPI cards + investigation completion line chart + VA outcomes bar + consultation time by doctor + recall compliance donut
- **Comerciali**: Revenue area chart (12 months, medical vs optical stacked) + conversion funnel visualization + revenue per doctor bar
- **Operaționali**: Service distribution donut + top 10 diagnoses horizontal bars + age distribution + no-show cost + outstanding balances table
- **Stocuri**: Stock overview cards + fast/slow movers table + reorder suggestions

### File 6: `src/pages/NotificationsPage.tsx` (~250 lines)
- **3 tabs**: Reguli Automate / Recall Clinic / Jurnal Trimiteri
- **Reguli Automate**: Toggle cards per notification rule (from `notificationRules` data), showing type/channel/timing/template/ON-OFF toggle
- **Recall Clinic**: Diagnosis-triggered recall cards (Glaucom annual, Retinopatie 6-12mo, AMD 6mo, etc.) with interval configs
- **Jurnal Trimiteri**: Send log table (date, patient, channel, type, status, content preview)

### File 7: `src/pages/SettingsPage.tsx` (~350 lines)
- **Left sub-nav**: Utilizatori & Roluri / Configurare Clinică / Echipamente / Șabloane Notificări / GDPR / Integrări
- **Utilizatori**: User list table + role badges + create/edit modal with permission matrix
- **Configurare Clinică**: Clinic details form + locations + working hours + service catalog editor
- **Echipamente**: Device registry table (OCT Topcon, VF Nidek, etc.) with DICOM config fields
- **Integrări**: Integration cards (SMS, Email, Payment, Lab, FHIR) with status indicators

### File 8: `src/pages/AuditLogPage.tsx` (~150 lines)
- Immutable log table from `auditLog` data. Columns: timestamp, user, role, action (color-coded badge), module, entity, IP, details
- Search by user/patient/date. Filter by action type. Export buttons

### File 9: `src/pages/BillingPage.tsx` (~250 lines)
- Service catalog display (from `serviceCatalog`)
- Billing session: patient search → add services → auto-total → payment method (Cash/Card/Transfer) → generate invoice
- Outstanding balances table

### File 10: `src/pages/InventoryPage.tsx` (~250 lines)
- **4 tabs**: Rame / Lentile / Lentile contact / Consumabile
- Tables from `stockItems` data with low-stock amber highlighting
- Stock reports panel: fast movers, slow movers, total value, reorder suggestions

### File 11: Update `src/pages/PatientPortal.tsx` (~400 lines)
Full portal with top nav (no sidebar), 860px max-width:
- Programări (upcoming + 4-step booking flow with progress bar)
- Dosarul meu (consultation history, VA/IOP charts)
- Rețete (prescription cards with QR code visual, validity bar)
- Comenzi (order status tracker with friendly labels)
- Mesaje (chat-bubble thread UI)
- Setări & GDPR (consent toggles, access log, data portability)

### File 12: Update `src/App.tsx`
Replace all PlaceholderPage routes with new page imports.

---

## Technical Notes
- All state via React useState (no backend)
- Recharts for all charts
- Lucide icons throughout
- HTML5 drag-and-drop API for Kanban
- Existing design tokens preserved — no CSS changes needed
- All clinical values in JetBrains Mono via `font-clinical` class
- IOP coloring uses existing `iop-normal`/`iop-warning`/`iop-danger` classes

