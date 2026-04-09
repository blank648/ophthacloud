# GUIDE_02 — Design System: OphthaCloud

> **Document type:** Frozen Design Specification  
> **Version:** 1.0  
> **Last updated:** 2026-04-02  
> **Source:** Approved Lovable prototype (Prompt 2 design system)  
> **Status:** FROZEN — no visual change is permitted without explicit client re-approval.  
> **Prerequisite:** GUIDE_00 (Project Brief), GUIDE_01 (Architecture)

---

## Purpose & Authority of This Document

The client approved the Lovable prototype design "by far" over all alternatives.
That design is now frozen. This document translates every visual specification from the
approved prototype into implementation-ready developer rules.

**Antigravity must not:**
- Change any color value, even by 1 hex digit
- Change any font family or weight
- Change any spacing value outside the 4px system
- Add components not defined here without architect approval
- Use Tailwind utility classes that contradict the tokens below

**Antigravity may:**
- Add new component variants that extend (not replace) existing ones
- Implement animations and transitions not explicitly specified, as long as they are subtle
- Adapt layouts for mobile responsiveness, following the rules in Section 13

When in doubt: match the Lovable prototype pixel-for-pixel.

---

## 1. Design Philosophy

OphthaCloud is used by doctors in **darkened examination rooms**, receptionists under **fluorescent office light**, and managers on **laptops in between meetings**. Every visual decision must serve clinical clarity, not aesthetic novelty.

**Five non-negotiable principles:**

1. **Clinical data is always visually distinct from UI chrome.** Refraction values, IOP readings, and VA notations use JetBrains Mono. Regular UI text uses Inter. This distinction is never broken.

2. **Semantic color is clinical-grade.** Red means danger (IOP > 25, overdue recalls). Amber means warning (IOP 22–25, pending items). Green means normal/completed. These mappings are never repurposed for decoration.

3. **Dark mode is a clinical requirement.** Doctors work in darkened rooms. Dark mode is not a preference toggle — it is a primary use case. Both modes receive equal design quality.

4. **Information density is intentional.** The consultation form is dense because doctors are efficient. The patient portal is spacious because patients are not. Different screens have different density targets. Never uniformly space everything.

5. **The sidebar is the product's spine.** The dark navy sidebar (#0D2D3D) is the most recognizable element of the UI. It must be consistent, stable, and always present in staff-facing screens.

---

## 2. Color System

### 2.1 Primary Brand (Deep Medical Teal)

```css
--color-primary-900: #0A3D52;
--color-primary-800: #0D4F6A;
--color-primary-700: #106283;
--color-primary-600: #13759C;   /* PRIMARY ACTION: buttons, active nav, links */
--color-primary-500: #1A8FB8;
--color-primary-400: #3AADD4;
--color-primary-300: #7ACCE6;
--color-primary-200: #B8E6F4;
--color-primary-100: #E0F4FA;
--color-primary-50:  #F0FAFD;
```

### 2.2 Accent (Optical Amber — use sparingly)

```css
--color-accent-600: #C96A00;
--color-accent-500: #F08000;    /* PRIMARY ACCENT: cross-module CTAs, optical order button */
--color-accent-400: #F5A020;
--color-accent-100: #FEF3E0;
```

Use accent-500 exclusively for the "Generează Comandă Optică" button and other
cross-module action buttons that move a workflow from clinical → commercial.
Do not use amber for decorative purposes.

### 2.3 Clinical Status (Semantic — Never Repurpose)

```css
--color-success:  #1A7F5A;   /* completed, normal IOP, paid, active prescription */
--color-warning:  #C97B00;   /* pending, IOP 22-25 mmHg, expiring soon, borderline */
--color-danger:   #C0392B;   /* urgent, overdue, IOP >25 mmHg, no-show, expired */
--color-info:     #2563EB;   /* informational, FHIR badges, DICOM tags, OS badge */
--color-neutral:  #6B7280;   /* inactive, archived, cancelled, secondary text */
```

### 2.4 Surface & Background

```css
/* Light mode */
--color-bg-canvas:          #F4F7FA;   /* App background — NOT pure white */
--color-bg-surface:         #FFFFFF;   /* Cards, panels, modals */
--color-bg-sidebar:         #0D2D3D;   /* Dark sidebar — the product's spine */
--color-bg-sidebar-hover:   #1A4255;
--color-bg-sidebar-active:  #13759C;   /* = primary-600 */
--color-bg-elevated:        #FFFFFF;   /* Elevated panels with shadow */
```

### 2.5 Borders

```css
--color-border-subtle:  #E2E8F0;
--color-border-default: #CBD5E1;
--color-border-strong:  #94A3B8;
```

### 2.6 Text

```css
--color-text-primary:   #0F172A;   /* Main text — near-black */
--color-text-secondary: #475569;   /* Labels, metadata */
--color-text-muted:     #94A3B8;   /* Placeholders, disabled states */
--color-text-inverse:   #FFFFFF;   /* Text on dark backgrounds */
--color-text-clinical:  #0A3D52;   /* Clinical values — IOP, VA, refraction params */
```

### 2.7 Dark Mode Overrides

Dark mode is toggled via `data-theme="dark"` on `<html>`. All overrides below apply.
Clinical status colors (--color-success, --color-warning, --color-danger, --color-info)
and all primary-600 action colors remain UNCHANGED in dark mode.

```css
[data-theme="dark"] {
  --color-bg-canvas:       #0F1923;
  --color-bg-surface:      #162230;
  --color-bg-elevated:     #1E2F40;
  --color-border-subtle:   #2A3F52;
  --color-border-default:  #3A526A;
  --color-border-strong:   #4E6E8A;
  --color-text-primary:    #E2EEF6;
  --color-text-secondary:  #8FAFC4;
  --color-text-muted:      #4E6E8A;
  --color-text-inverse:    #0F172A;
  --color-text-clinical:   #7ACCE6;   /* primary-300 in dark — readable on dark surface */
  /* Sidebar colors unchanged — already dark */
  /* Primary action colors unchanged */
  /* Clinical status colors unchanged */
}
```

### 2.8 Tailwind Configuration (tailwind.config.ts)

Copy this entire `colors` block into `tailwind.config.ts` under `theme.extend.colors`:

```typescript
colors: {
  primary: {
    50:  '#F0FAFD', 100: '#E0F4FA', 200: '#B8E6F4',
    300: '#7ACCE6', 400: '#3AADD4', 500: '#1A8FB8',
    600: '#13759C', 700: '#106283', 800: '#0D4F6A', 900: '#0A3D52',
  },
  accent: {
    100: '#FEF3E0', 400: '#F5A020', 500: '#F08000', 600: '#C96A00',
  },
  clinical: {
    success: '#1A7F5A',
    warning: '#C97B00',
    danger:  '#C0392B',
    info:    '#2563EB',
    neutral: '#6B7280',
  },
  canvas:   '#F4F7FA',
  surface:  '#FFFFFF',
  sidebar:  '#0D2D3D',
  elevated: '#FFFFFF',
},
```

---

## 3. Typography

### 3.1 Font Families

**Two fonts only. No others are permitted.**

| Font | Use case | Source |
|---|---|---|
| **Inter** | All UI text — labels, body, nav, buttons, headings | Google Fonts |
| **JetBrains Mono** | ALL clinical numeric data — Sph, Cyl, Axis, IOP, VA, SEQ, PD, DP, any mmHg or diopter value | Google Fonts |

```html
<!-- Add to <head> in _document.tsx or layout.tsx -->
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500;600&display=swap" rel="stylesheet">
```

```css
--font-ui:       'Inter', system-ui, -apple-system, sans-serif;
--font-clinical: 'JetBrains Mono', 'Courier New', monospace;
```

```typescript
// tailwind.config.ts
fontFamily: {
  ui:       ['Inter', 'system-ui', 'sans-serif'],
  clinical: ['JetBrains Mono', 'Courier New', 'monospace'],
},
```

### 3.2 Type Scale

```css
--text-xs:   11px;  /* line-height: 16px — badges, footnotes, audit timestamps */
--text-sm:   13px;  /* line-height: 20px — secondary labels, table cells, metadata */
--text-base: 15px;  /* line-height: 24px — body text, form labels */
--text-md:   17px;  /* line-height: 26px — section headers within panels */
--text-lg:   20px;  /* line-height: 28px — page titles, module headers */
--text-xl:   24px;  /* line-height: 32px — dashboard KPI numbers */
--text-2xl:  30px;  /* line-height: 38px — large metric values */
--text-3xl:  38px;  /* line-height: 46px — hero stats on manager dashboard */
```

Note: These are fixed pixel values (not fluid clamp). Clinical data entry requires
predictable, stable text sizing — fluid scaling is inappropriate here.

### 3.3 Font Weights

```css
400 — body text, descriptions, clinical free-text fields
500 — labels, nav items (non-active), secondary content
600 — section headers, active nav items, button text, clinical field labels, OD/OS headers
700 — KPI values, page titles, critical alerts, SIGN button
```

Do not use weight 300. Clinical environments require legible text at all times.

### 3.4 Special Typography Rules

**OD / OS / IOP / BCVA / VA labels (uppercase anatomical abbreviations):**
```css
letter-spacing: 0.06em;
font-weight: 600;
text-transform: uppercase;
font-family: var(--font-ui);   /* Inter, not JetBrains Mono */
```

**Badge text:**
```css
letter-spacing: 0.04em;
font-weight: 600;
```

**Clinical data values (any measurement):**
```css
font-family: var(--font-clinical);  /* JetBrains Mono */
font-weight: 500;
color: var(--color-text-clinical);
```

---

## 4. Spacing System

Base unit: 4px. All spacing must use these tokens. Never use arbitrary pixel values.

```css
--space-1:  4px;    --space-2:  8px;    --space-3:  12px;
--space-4:  16px;   --space-5:  20px;   --space-6:  24px;
--space-8:  32px;   --space-10: 40px;   --space-12: 48px;
--space-16: 64px;
```

```typescript
// tailwind.config.ts
spacing: {
  '1': '4px',  '2': '8px',   '3': '12px',  '4': '16px',  '5': '20px',
  '6': '24px', '8': '32px',  '10': '40px', '12': '48px', '16': '64px',
},
```

---

## 5. Border Radius

```css
--radius-sm:   4px;     /* inputs, small badges, table rows */
--radius-md:   8px;     /* cards, buttons, dropdowns, modals inner elements */
--radius-lg:   12px;    /* modals, large panels */
--radius-xl:   16px;    /* dashboard KPI cards */
--radius-full: 9999px;  /* pill badges, avatar circles, toggle switches */
```

```typescript
// tailwind.config.ts
borderRadius: {
  sm: '4px', md: '8px', lg: '12px', xl: '16px', full: '9999px',
},
```

---

## 6. Shadows

```css
--shadow-sm:    0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.05);
--shadow-md:    0 4px 12px rgba(0,0,0,0.10), 0 2px 4px rgba(0,0,0,0.06);
--shadow-lg:    0 10px 30px rgba(0,0,0,0.12), 0 4px 8px rgba(0,0,0,0.08);
--shadow-focus: 0 0 0 3px rgba(19,117,156,0.25);   /* primary-600 at 25% opacity */
```

Usage:
- Cards: `--shadow-sm` default, `--shadow-md` on hover
- Modals: `--shadow-lg`
- Focus rings: `--shadow-focus` (all focusable elements)
- Sidebar: no shadow (dark background provides natural separation)

---

## 7. Layout System

### 7.1 Global Layout Structure

```
┌─────────────────────────────────────────────────────────────────┐
│                         Sidebar (240px)                         │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                  Main Content Area                         │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │           Page Header (56px sticky)                  │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │           Scrollable Content Area                    │  │  │
│  │  │                                                      │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

- **Sidebar:** 240px expanded / 64px collapsed (icon-only + tooltip)
- **Page header:** 56px height, sticky, `--color-bg-surface`, 1px border-bottom `--color-border-subtle`
- **Content max-width:** 1440px
- **Grid:** 12-column, 24px gutters desktop / 4-column tablet (768px) / 2-column mobile (375px)

### 7.2 Clinical Consultation — Exception Layout

The consultation form (EMR Module) uses a unique split layout:

```
┌───────────────────────────────┬──────────────────────────────────┐
│   Left Panel (40%)            │   Right Panel (60%)              │
│   Sticky, independently       │   Independently scrollable       │
│   scrollable                  │                                  │
│                               │                                  │
│   Patient context             │   Active section form            │
│   Section stepper (A-I)       │   content                        │
│   Progress bar                │                                  │
│                               │                                  │
└───────────────────────────────┴──────────────────────────────────┘
```

Both panels scroll independently. The left panel sticks at the top.
The patient context bar in the left panel is always visible — never hidden.

### 7.3 Page Header — Patient Context Bar (Consultation Pages)

When inside any consultation screen for a patient, the page header center shows:

```
[Avatar 28px] [Patient Name — weight 600] [ID: OC-004821 — monospace] 
[Age: 62 ani] [Status badge] [Last visit: 15.01.2026] [⚠ Glaucom — diagnosis badge]
```

This bar is permanently visible. It must not be removed, hidden, or collapsed.

---

## 8. Sidebar Component

### Specifications

```css
.sidebar {
  width: 240px;                           /* expanded */
  width: 64px;                            /* collapsed */
  background: var(--color-bg-sidebar);    /* #0D2D3D */
  height: 100vh;
  position: fixed;
  left: 0; top: 0;
  display: flex;
  flex-direction: column;
  transition: width 200ms ease;
}
```

### Logo Area (top, 56px height)

- **Expanded:** OphthaCloud iris logo (32px SVG) + "OphthaCloud" text (`--color-primary-300`, weight 600, `--text-sm`)
- **Collapsed:** Iris logo only, centered
- Below logo: clinic name in `--text-sm`, `--color-text-secondary`, truncated with ellipsis

### Module Group Labels (section headers)

```css
text-transform: uppercase;
font-size: var(--text-xs);         /* 11px */
color: var(--color-primary-400);
letter-spacing: 0.10em;
font-weight: 600;
padding: 20px 0 4px 16px;
/* Hidden in collapsed mode */
```

### Nav Items

```css
.nav-item {
  height: 44px;
  padding: 0 16px;
  margin: 2px 4px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: var(--space-3);               /* 12px */
  cursor: pointer;
  transition: background 150ms ease;
}

.nav-item icon {
  width: 20px; height: 20px;
  color: var(--color-primary-300);   /* default */
}

.nav-item label {
  font-size: var(--text-sm);
  font-weight: 500;
  color: rgba(255,255,255,0.80);     /* default */
}

.nav-item:hover {
  background: var(--color-bg-sidebar-hover);  /* #1A4255 */
}

.nav-item.active {
  background: var(--color-bg-sidebar-active); /* #13759C */
  border-left: 3px solid #FFFFFF;
  padding-left: 13px;                /* compensate for 3px border */
}

.nav-item.active icon,
.nav-item.active label {
  color: #FFFFFF;
}
```

### Sub-nav Items (indented)

```css
.sub-nav-item {
  height: 36px;
  padding-left: 40px;
  font-size: var(--text-sm);
  color: var(--color-primary-300);   /* default */
  /* Show/hide: animated expand 200ms ease-in-out */
}
.sub-nav-item.active { color: #FFFFFF; }
```

### Sidebar Bottom Bar

Contains: user avatar (32px) + name + role badge, dark mode toggle, notification bell, settings icon.

### Lucide React Icon Assignments

```typescript
import {
  LayoutDashboard,   // Dashboard
  Users,             // Patients
  CalendarDays,      // Appointments
  Stethoscope,       // EMR Consultation
  ScanEye,           // Investigations
  FileText,          // Prescriptions
  Glasses,           // Optical ERP
  Bell,              // Notifications
  UserCircle,        // Patient Portal
  BarChart3,         // Reports & KPIs
  Settings2,         // Settings & Admin
  Package,           // Stock/Inventory
  FlaskConical,      // Lab/Orders
  CreditCard,        // Billing/POS
  Shield,            // Audit Log
} from 'lucide-react';
```

---

## 9. Button Components

### Primary Button

```css
.btn-primary {
  background: var(--color-primary-600);   /* #13759C */
  color: #FFFFFF;
  font-weight: 600;
  font-size: var(--text-sm);
  height: 40px;
  padding: 0 20px;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
  transition: background 150ms ease, box-shadow 150ms ease;
}
.btn-primary:hover  { background: var(--color-primary-700); box-shadow: var(--shadow-sm); }
.btn-primary:active { background: var(--color-primary-800); }
.btn-primary:focus-visible { box-shadow: var(--shadow-focus); }
.btn-primary:disabled { opacity: 0.40; cursor: not-allowed; }
```

### Secondary Button

```css
.btn-secondary {
  background: transparent;
  border: 1.5px solid var(--color-primary-600);
  color: var(--color-primary-600);
  /* same height, padding, radius as primary */
}
.btn-secondary:hover { background: var(--color-primary-50); }
```

### Danger Button

```css
.btn-danger {
  background: var(--color-danger);   /* #C0392B */
  color: #FFFFFF;
  /* same structure as primary */
}
.btn-danger:hover { background: #a93226; }
```

### Ghost Button

```css
.btn-ghost {
  background: transparent;
  border: none;
  color: var(--color-text-secondary);
}
.btn-ghost:hover { background: var(--color-bg-canvas); }
```

### Icon Button (32×32px)

```css
.btn-icon {
  width: 32px; height: 32px;
  padding: 0;
  display: flex; align-items: center; justify-content: center;
  background: transparent;
  border: none;
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
}
.btn-icon:hover { background: var(--color-bg-canvas); color: var(--color-text-primary); }
```

Used for: table row actions (edit, delete, view), inline controls.

### Clinical Action Button (SIGN / GENERATE PRESCRIPTION)

```css
.btn-clinical-action {
  width: 100%;
  height: 48px;
  background: linear-gradient(135deg, var(--color-primary-600), var(--color-primary-700));
  color: #FFFFFF;
  font-size: var(--text-md);   /* 17px */
  font-weight: 600;
  border-radius: var(--radius-md);
  border: none;
  display: flex; align-items: center; justify-content: center; gap: 8px;
}
.btn-clinical-action:hover {
  box-shadow: var(--shadow-focus);  /* glow effect */
}
```

### Amber CTA Button (Generează Comandă Optică)

```css
.btn-cross-module {
  background: var(--color-accent-500);   /* #F08000 */
  color: #FFFFFF;
  font-weight: 600;
  height: 40px;
  padding: 0 20px;
  border-radius: var(--radius-md);
}
.btn-cross-module:hover { background: var(--color-accent-600); }
```

---

## 10. Form Inputs

### Standard Text Input

```css
.input {
  height: 40px;
  border-radius: var(--radius-sm);
  border: 1.5px solid var(--color-border-default);
  background: #FFFFFF;
  padding: 0 12px;
  font-size: var(--text-base);
  font-family: var(--font-ui);
  color: var(--color-text-primary);
  width: 100%;
  transition: border-color 150ms ease, box-shadow 150ms ease;
}
.input:focus {
  outline: none;
  border-color: var(--color-primary-600);
  box-shadow: var(--shadow-focus);
}
.input::placeholder { color: var(--color-text-muted); }
.input:disabled { background: var(--color-bg-canvas); color: var(--color-text-muted); }
.input.error { border-color: var(--color-danger); }
```

Dark mode:
```css
[data-theme="dark"] .input {
  background: var(--color-bg-elevated);
  border-color: var(--color-border-default);
}
```

### Clinical Data Input (Sph / Cyl / Axis / IOP / VA)

```css
.input-clinical {
  font-family: var(--font-clinical);   /* JetBrains Mono */
  font-size: var(--text-base);
  background: var(--color-primary-50);  /* #F0FAFD — tinted bg */
  border: 1.5px solid var(--color-primary-200);
  color: var(--color-text-clinical);
}
.input-clinical:focus {
  border-color: var(--color-primary-500);
  background: #FFFFFF;
  box-shadow: var(--shadow-focus);
}
```

Fixed widths per field type:

```css
.input-sph   { width: 80px; }
.input-cyl   { width: 80px; }
.input-axis  { width: 64px; }
.input-iop   { width: 56px; }
.input-va    { width: 72px; }
.input-add   { width: 72px; }
.input-pd    { width: 64px; }
.input-seq   { width: 80px; cursor: not-allowed; opacity: 0.85; }  /* read-only */
```

Unit suffix labels (inline, after input):

```css
.input-suffix {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
  margin-left: 4px;
  font-family: var(--font-ui);
}
/* Examples: "D", "mmHg", "°", "mm" */
```

### OD / OS Dual-Column Layout

The standard layout for all refraction and binocular inputs:

```
┌─────────────────────────────────────────────────────────────┐
│  Row label    │      OD (Ochi Drept)     │  OS (Ochi Stâng) │
│               │  [red badge]             │  [blue badge]    │
├───────────────┼──────────────────────────┼──────────────────┤
│  Sferă        │  [±__.__D input]         │  [±__.__D input] │
│  Cilindru     │  [−__.__D input]         │  [−__.__D input] │
│  Ax           │  [____° input]           │  [____° input]   │
│  Add          │  [+__.__D input]         │  [+__.__D input] │
│  VA / BCVA    │  [6/__ select]           │  [6/__ select]   │
│  IOP          │  [__ mmHg input]         │  [__ mmHg input] │
│  SEQ (auto)   │  [__.__D read-only]      │  [__.__D r.o.]   │
└───────────────┴──────────────────────────┴──────────────────┘
```

OD header badge: `background: #FEE2E2; color: #991B1B; border: 1px solid #FECACA`
OS header badge: `background: #DBEAFE; color: #1E40AF; border: 1px solid #BFDBFE`
Separator between OD and OS columns: `1px solid var(--color-border-subtle)`

### SEQ Auto-Calculation

SEQ (Spherical Equivalent) = Sph + (Cyl ÷ 2)

This must be computed in real-time as the user types in the Sph or Cyl fields:

```typescript
const computeSEQ = (sph: number, cyl: number): string => {
  if (isNaN(sph) || isNaN(cyl)) return '';
  const seq = sph + (cyl / 2);
  return seq.toFixed(2) + ' D';
};
```

SEQ field: `pointer-events: none`, `background: var(--color-primary-50)`,
`color: var(--color-primary-600)`, `font-family: var(--font-clinical)`, `font-weight: 600`.

### Toggle Switch

```css
.toggle {
  width: 44px; height: 24px;
  border-radius: var(--radius-full);
  background: var(--color-border-default);  /* off state */
  position: relative;
  cursor: pointer;
  transition: background 200ms ease;
}
.toggle.on { background: var(--color-primary-600); }
.toggle-thumb {
  width: 20px; height: 20px;
  background: #FFFFFF;
  border-radius: var(--radius-full);
  box-shadow: var(--shadow-sm);
  position: absolute;
  top: 2px; left: 2px;
  transition: left 200ms ease;
}
.toggle.on .toggle-thumb { left: 22px; }
```

---

## 11. Badge / Status Pill Components

All badges: `border-radius: var(--radius-full)`, `padding: 2px 10px`,
`font-size: var(--text-xs)`, `font-weight: 600`, `letter-spacing: 0.04em`

### Appointment Status Badges

```css
.badge-booked     { background: #EFF6FF; color: #1D4ED8; }
.badge-confirmed  { background: #ECFDF5; color: #065F46; }
.badge-checked-in { background: #F0FDF4; color: #15803D; }
.badge-in-progress{ background: #FEF9C3; color: #854D0E; }
.badge-completed  { background: #ECFDF5; color: #1A7F5A; }
.badge-no-show    { background: #FEF2F2; color: #991B1B; }
.badge-cancelled  { background: #F3F4F6; color: #6B7280; }
```

### Clinical Diagnosis Flags

These appear in patient headers, list rows, and consultation section headers.
Each has a colored border in addition to background and text:

```css
.flag-glaucom     { background: #FEE2E2; color: #991B1B; border: 1px solid #FECACA; }
.flag-diabet      { background: #FEF3C7; color: #92400E; border: 1px solid #FDE68A; }
.flag-amd         { background: #EDE9FE; color: #5B21B6; border: 1px solid #DDD6FE; }
.flag-post-op     { background: #DBEAFE; color: #1E40AF; border: 1px solid #BFDBFE; }
.flag-keratoconus { background: #FCE7F3; color: #9D174D; border: 1px solid #FBCFE8; }
.flag-normal      { background: #ECFDF5; color: #065F46; border: 1px solid #A7F3D0; }
```

### Prescription Status Badges

```css
.badge-activa    { background: #ECFDF5; color: #1A7F5A; }  /* success */
.badge-expirata  { background: #FEF2F2; color: #991B1B; }  /* danger */
.badge-anulata   { background: #F3F4F6; color: #6B7280; }  /* neutral */
.badge-revizuita { background: #EFF6FF; color: #1D4ED8; }  /* info */
```

### Optical Order Progress Badges

```css
.badge-order-new       { background: #EFF6FF; color: #1D4ED8; }
.badge-order-lab       { background: #FEF9C3; color: #854D0E; }
.badge-order-qc        { background: #FEF3E0; color: #C96A00; }
.badge-order-ready     { background: #ECFDF5; color: #065F46; }
.badge-order-completed { background: #F0FDF4; color: #15803D; }
```

---

## 12. IOP Semantic Color Coding

IOP coloring applies to **every location where IOP is displayed**: consultation form (Section D),
patient overview, history timeline, dashboards, reports. No exceptions.

```typescript
// iop-utils.ts — use this helper everywhere
export function getIopStyle(mmHg: number): {
  color: string; icon: string | null; label: string;
} {
  if (mmHg <= 21) {
    return { color: '#1A7F5A', icon: null, label: 'Normal' };
  } else if (mmHg <= 25) {
    return { color: '#C97B00', icon: '⚠', label: 'Hipertensiune oculară' };
  } else {
    return { color: '#C0392B', icon: '⚠', label: 'IOP ridicat — evaluare urgentă' };
  }
}
```

In Section D of the consultation form, IOP > 25 additionally triggers a red alert banner:

```
🚨 IOP [value] mmHg — IOP ridicat — evaluare glaucom urgentă
```

---

## 13. Card Components

### Standard Card

```css
.card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-xl);      /* 16px */
  box-shadow: var(--shadow-sm);
  padding: 20px 24px;
  transition: box-shadow 200ms ease, border-color 200ms ease;
}
.card.interactive:hover {
  box-shadow: var(--shadow-md);
  border-color: var(--color-border-default);
}
```

### KPI Stat Card

```css
.card-kpi {
  /* extends .card */
  display: flex; flex-direction: column; gap: var(--space-2);
}
.card-kpi:hover {
  border-left: 3px solid var(--color-primary-600);
  padding-left: 21px; /* compensate */
}

.kpi-icon-wrapper {
  width: 40px; height: 40px;
  border-radius: var(--radius-md);
  background: var(--color-primary-100);  /* icon tinted circle */
  display: flex; align-items: center; justify-content: center;
}
.kpi-icon { color: var(--color-primary-600); width: 20px; height: 20px; }

.kpi-label { font-size: var(--text-sm); color: var(--color-text-secondary); }

.kpi-value {
  font-size: var(--text-2xl);   /* 30px */
  font-weight: 700;
  color: var(--color-primary-900);
  font-family: var(--font-clinical);   /* JetBrains Mono for numbers */
}

.kpi-trend {
  font-size: var(--text-xs);
  /* ▲ +12% last month: color success */
  /* ▼ -3% last month: color danger */
}
```

### Alert / Warning Card

```css
.card-alert {
  border-left: 4px solid var(--color-warning);  /* or --color-danger */
  background: #FFFBEB;                           /* amber-50 / or #FEF2F2 for danger */
  border-radius: var(--radius-md);
  padding: var(--space-3) var(--space-4);
}
```

### Clinical Section Divider Card (Consultation Form)

```css
.section-divider {
  background: var(--color-primary-50);
  border-top: 2px solid var(--color-primary-200);
  border-radius: var(--radius-md);
  padding: var(--space-3) var(--space-4);
  display: flex; align-items: center; justify-content: space-between;
}
.section-title {
  font-size: var(--text-md);   /* 17px */
  font-weight: 600;
  color: var(--color-primary-700);
}
.section-subtitle {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}
```

---

## 14. Table Component

### Header Row

```css
.table-header {
  background: var(--color-bg-canvas);
  border-bottom: 2px solid var(--color-border-default);
  height: 44px;
}
.table-header th {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  padding: 0 var(--space-4);
  text-align: left;
}
```

### Data Rows

```css
.table-row {
  height: 56px;
  border-bottom: 1px solid var(--color-border-subtle);
  transition: background 100ms ease;
}
.table-row:nth-child(even) { background: var(--color-bg-canvas); }
.table-row:hover           { background: var(--color-primary-50); }
```

### Clinical Data Cells

```css
.cell-clinical {
  font-family: var(--font-clinical);  /* JetBrains Mono */
  color: var(--color-text-clinical);
  font-size: var(--text-sm);
}
/* Example: "−2.50 / −0.75 / 90°" in prescription table */
```

### Avatar Cell

```css
.cell-avatar {
  width: 32px; height: 32px;
  border-radius: var(--radius-full);
  background: var(--color-primary-100);
  color: var(--color-primary-700);
  font-size: var(--text-xs);
  font-weight: 600;
  display: flex; align-items: center; justify-content: center;
}
```

### Empty State

```css
.table-empty {
  padding: var(--space-16) var(--space-8);
  text-align: center;
  color: var(--color-text-muted);
}
.table-empty-icon { color: var(--color-primary-200); width: 48px; height: 48px; margin: 0 auto var(--space-4); }
.table-empty-title { font-size: var(--text-md); font-weight: 600; color: var(--color-text-primary); }
.table-empty-subtitle { font-size: var(--text-sm); max-width: 320px; margin: 0 auto var(--space-4); }
```

---

## 15. Modal Component

```css
.modal-overlay {
  background: rgba(0,0,0,0.50);
  backdrop-filter: blur(4px);
  position: fixed; inset: 0;
  z-index: 50;
  display: flex; align-items: center; justify-content: center;
}

.modal {
  background: var(--color-bg-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  max-height: 85vh;
  display: flex; flex-direction: column;
}

/* Width variants */
.modal-sm   { width: 480px; }
.modal-md   { width: 640px; }   /* default */
.modal-lg   { width: 800px; }
.modal-xl   { width: 1100px; }  /* investigation viewer */

.modal-header {
  padding: 24px;
  border-bottom: 1px solid var(--color-border-subtle);
  display: flex; align-items: center; justify-content: space-between;
}
.modal-title { font-size: var(--text-lg); font-weight: 600; }

.modal-body { padding: 24px; overflow-y: auto; flex: 1; }

.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid var(--color-border-subtle);
  display: flex; justify-content: flex-end; gap: var(--space-3);
}
/* Destructive action button: position absolute-left within footer */
```

---

## 16. Toast Notification Component

```css
.toast-container {
  position: fixed;
  bottom: 16px; right: 16px;
  z-index: 100;
  display: flex; flex-direction: column; gap: var(--space-2);
}

.toast {
  width: 360px;
  background: var(--color-bg-surface);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  padding: var(--space-3) var(--space-4);
  display: flex; align-items: flex-start; gap: var(--space-3);
  border-left: 4px solid transparent;
  animation: slideInRight 250ms ease;
}

.toast-success { border-left-color: var(--color-success); }
.toast-error   { border-left-color: var(--color-danger); }
.toast-warning { border-left-color: var(--color-warning); }
.toast-info    { border-left-color: var(--color-info); }

/* Auto-dismiss: 4 seconds. Max 3 toasts visible. Oldest dismisses first. */
```

---

## 17. Calendar Appointment Blocks

### Appointment Type Colors

```css
/* Each type: left-border solid color + background at 12% opacity */
--appt-initial:    #13759C;   /* Consultație inițială — primary-600 */
--appt-followup:   #10B981;   /* Follow-up — green */
--appt-investig:   #8B5CF6;   /* Investigație — purple */
--appt-procedure:  #EF4444;   /* Procedură — red */
--appt-montaj:     #F59E0B;   /* Montaj ochelari — amber */
--appt-optic:      #06B6D4;   /* Consiliere optică — cyan */
--appt-tele:       #6366F1;   /* Telemedicină — indigo */
```

```css
.appt-block {
  border-radius: 6px;
  border-left: 4px solid var(--type-color);
  background: color-mix(in srgb, var(--type-color) 12%, transparent);
  padding: 4px 8px;
  overflow: hidden;
}
.appt-block-name { font-size: var(--text-sm); font-weight: 600; }
.appt-block-type { font-size: var(--text-xs); color: var(--color-text-secondary); }
/* Height proportional to duration: 1px per minute */
```

**Current time indicator:**
```css
.time-indicator {
  height: 2px;
  background: var(--color-danger);
  position: absolute; left: 0; right: 0;
}
.time-indicator::before {
  content: '';
  width: 8px; height: 8px;
  border-radius: var(--radius-full);
  background: var(--color-danger);
  position: absolute; left: -4px; top: -3px;
}
```

---

## 18. Investigation Viewer (DICOM-Style Panels)

```css
.investigation-viewer {
  background: #0F1923;   /* intentionally dark — medical image viewing */
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.viewer-panel {
  background: #000000;
  display: flex; align-items: center; justify-content: center;
  position: relative;
}

.viewer-toolbar {
  position: absolute; bottom: 0; left: 0; right: 0;
  background: rgba(15, 25, 35, 0.85);
  padding: var(--space-2) var(--space-3);
  display: flex; gap: var(--space-2);
}

.viewer-label {
  font-family: var(--font-clinical);
  font-size: var(--text-xs);
  color: rgba(255,255,255,0.85);
}
/* Grid layout: 2×2 default, 1×1 fullscreen toggle */
```

---

## 19. Loading States

### Skeleton Shimmer (Use instead of spinners)

```css
@keyframes shimmer {
  0%   { background-position: -200% 0; }
  100% { background-position:  200% 0; }
}

.skeleton {
  background: linear-gradient(
    90deg,
    var(--color-border-subtle) 25%,
    var(--color-border-default) 50%,
    var(--color-border-subtle) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  border-radius: var(--radius-sm);
}

.skeleton-text    { height: 13px; margin-bottom: 6px; }
.skeleton-heading { height: 20px; width: 40%; margin-bottom: 12px; }
.skeleton-avatar  { width: 32px; height: 32px; border-radius: var(--radius-full); }
.skeleton-card    { height: 120px; border-radius: var(--radius-xl); }
```

Respect `prefers-reduced-motion`: if reduced motion is preferred, replace animation with static gray.

---

## 20. Consultation Section Stepper

### States

```css
/* Step circle states */
.step-pending {
  width: 28px; height: 28px;
  border-radius: var(--radius-full);
  border: 2px solid var(--color-border-default);
  background: transparent;
  color: var(--color-text-muted);
  font-size: var(--text-sm); font-weight: 600;
  display: flex; align-items: center; justify-content: center;
}

.step-active {
  border-color: var(--color-primary-600);
  background: var(--color-primary-600);
  color: #FFFFFF;
}

.step-completed {
  border-color: var(--color-success);
  background: var(--color-success);
  color: #FFFFFF;
  /* Show checkmark icon instead of number */
}

/* Progress bar below stepper */
.stepper-progress {
  height: 6px;
  border-radius: var(--radius-full);
  background: var(--color-border-subtle);
  margin-top: var(--space-3);
}
.stepper-progress-fill {
  height: 100%;
  border-radius: var(--radius-full);
  background: var(--color-primary-600);
  transition: width 300ms ease;
}
```

---

## 21. Clinical Template Buttons (Section H)

```css
.template-btn {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: var(--space-2);
  padding: var(--space-4);
  border: 1.5px solid var(--color-border-default);
  border-radius: var(--radius-md);
  background: var(--color-bg-surface);
  cursor: pointer;
  text-align: center;
  transition: all 150ms ease;
}
.template-btn:hover {
  border-color: var(--color-primary-400);
  background: var(--color-primary-50);
}
.template-btn-icon  { color: var(--color-primary-600); width: 24px; height: 24px; }
.template-btn-label { font-size: var(--text-xs); font-weight: 600; color: var(--color-text-secondary); }
```

After template is applied, pre-filled fields show amber tint:

```css
.input.template-prefilled {
  background: var(--color-accent-100);   /* #FEF3E0 */
  border-color: var(--color-accent-400);
}
```

And a banner appears above the section:

```css
.template-applied-banner {
  background: var(--color-accent-100);
  border: 1px solid var(--color-accent-400);
  border-radius: var(--radius-md);
  padding: var(--space-2) var(--space-3);
  font-size: var(--text-sm);
  color: var(--color-accent-600);
  display: flex; align-items: center; gap: var(--space-2);
}
```

---

## 22. Digital Signature State

### Unsigned State

Normal consultation form, all fields editable.

### Signed State

All fields become read-only. Show locked state visually:

```css
.consultation-signed .input,
.consultation-signed select,
.consultation-signed textarea {
  background: var(--color-bg-canvas);
  color: var(--color-text-secondary);
  pointer-events: none;
  cursor: default;
}

.signature-banner {
  background: #ECFDF5;
  border: 1px solid #A7F3D0;
  border-radius: var(--radius-md);
  padding: var(--space-3) var(--space-4);
  display: flex; align-items: center; gap: var(--space-3);
}
.signature-banner-icon { color: var(--color-success); width: 20px; height: 20px; }
.signature-banner-text {
  font-size: var(--text-sm); font-weight: 600;
  color: var(--color-success);
}
/* Content: "✓ Semnat digital · Dr. Ionescu · 29.03.2026 · 14:32" */
```

---

## 23. PDF / Print Document Styles

### Prescription PDF (A5 format)

```css
@media print {
  .prescription-document {
    width: 148mm;       /* A5 width */
    min-height: 210mm;  /* A5 height */
    background: #FFFFFF;
    font-family: var(--font-ui);
    color: #000000;
    padding: 16mm;
  }

  .prescription-title {
    text-align: center;
    font-size: 18px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    margin-bottom: 16px;
  }

  .prescription-table {
    width: 100%;
    border-collapse: collapse;
    font-family: var(--font-clinical);  /* JetBrains Mono for values */
    font-size: 14px;
  }
  .prescription-table td, .prescription-table th {
    border: 1px solid #CBD5E1;
    padding: 6px 10px;
  }

  .prescription-od-row { border-left: 3px solid #C0392B; }  /* OD: red */
  .prescription-os-row { border-left: 3px solid #2563EB; }  /* OS: blue */

  .prescription-footer {
    margin-top: 24px;
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
  }

  .signature-line {
    border-top: 1px solid #000;
    padding-top: 4px;
    font-size: 11px;
    color: #475569;
    width: 200px;
    text-align: center;
  }
}
```

---

## 24. Patient Portal — Distinct Design Context

The Patient Portal uses the **same color tokens** but different layout rules:

```css
.portal-layout {
  max-width: 860px;         /* wider than staff app, centered */
  margin: 0 auto;
  padding: 0 var(--space-6);
}

.portal-body {
  font-size: 16px;          /* 1px larger than staff app for readability */
  line-height: 1.7;         /* more generous line-height */
}

.portal-card {
  /* Same card tokens, but more generous padding */
  padding: 28px 32px;       /* vs 20px 24px in staff app */
}

/* No sidebar in portal — uses top navigation instead */
.portal-header {
  height: 64px;             /* taller than staff page header */
  border-bottom: 1px solid var(--color-border-subtle);
  background: var(--color-bg-surface);
}
```

The QR code on prescriptions (Portal view):

```css
.prescription-qr {
  width: 80px; height: 80px;
  position: absolute; bottom: 16px; right: 16px;
  border: 2px solid var(--color-border-subtle);
  border-radius: var(--radius-sm);
  padding: 4px;
}
```

Prescription validity progress bar (Portal):

```css
.validity-bar {
  height: 6px;
  border-radius: var(--radius-full);
  background: var(--color-border-subtle);
  margin-top: var(--space-2);
}
.validity-fill-high   { background: var(--color-success); }   /* > 6 months */
.validity-fill-medium { background: var(--color-warning); }   /* 1–6 months */
.validity-fill-low    { background: var(--color-danger); }    /* < 1 month */
```

---

## 25. CSS Variables — Complete Root Definition

Copy this into `src/styles/globals.css` or `src/app/globals.css`:

```css
:root {
  /* Primary palette */
  --color-primary-50:  #F0FAFD;
  --color-primary-100: #E0F4FA;
  --color-primary-200: #B8E6F4;
  --color-primary-300: #7ACCE6;
  --color-primary-400: #3AADD4;
  --color-primary-500: #1A8FB8;
  --color-primary-600: #13759C;
  --color-primary-700: #106283;
  --color-primary-800: #0D4F6A;
  --color-primary-900: #0A3D52;

  /* Accent */
  --color-accent-100: #FEF3E0;
  --color-accent-400: #F5A020;
  --color-accent-500: #F08000;
  --color-accent-600: #C96A00;

  /* Semantic status */
  --color-success: #1A7F5A;
  --color-warning: #C97B00;
  --color-danger:  #C0392B;
  --color-info:    #2563EB;
  --color-neutral: #6B7280;

  /* Surfaces */
  --color-bg-canvas:         #F4F7FA;
  --color-bg-surface:        #FFFFFF;
  --color-bg-sidebar:        #0D2D3D;
  --color-bg-sidebar-hover:  #1A4255;
  --color-bg-sidebar-active: #13759C;
  --color-bg-elevated:       #FFFFFF;

  /* Borders */
  --color-border-subtle:  #E2E8F0;
  --color-border-default: #CBD5E1;
  --color-border-strong:  #94A3B8;

  /* Text */
  --color-text-primary:   #0F172A;
  --color-text-secondary: #475569;
  --color-text-muted:     #94A3B8;
  --color-text-inverse:   #FFFFFF;
  --color-text-clinical:  #0A3D52;

  /* Typography */
  --font-ui:       'Inter', system-ui, sans-serif;
  --font-clinical: 'JetBrains Mono', 'Courier New', monospace;

  /* Type scale */
  --text-xs:   11px;
  --text-sm:   13px;
  --text-base: 15px;
  --text-md:   17px;
  --text-lg:   20px;
  --text-xl:   24px;
  --text-2xl:  30px;
  --text-3xl:  38px;

  /* Spacing */
  --space-1:  4px;  --space-2:  8px;   --space-3:  12px;
  --space-4:  16px; --space-5:  20px;  --space-6:  24px;
  --space-8:  32px; --space-10: 40px;  --space-12: 48px;
  --space-16: 64px;

  /* Radius */
  --radius-sm:   4px;
  --radius-md:   8px;
  --radius-lg:   12px;
  --radius-xl:   16px;
  --radius-full: 9999px;

  /* Shadows */
  --shadow-sm:    0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.05);
  --shadow-md:    0 4px 12px rgba(0,0,0,0.10), 0 2px 4px rgba(0,0,0,0.06);
  --shadow-lg:    0 10px 30px rgba(0,0,0,0.12), 0 4px 8px rgba(0,0,0,0.08);
  --shadow-focus: 0 0 0 3px rgba(19,117,156,0.25);
}

[data-theme="dark"] {
  --color-bg-canvas:       #0F1923;
  --color-bg-surface:      #162230;
  --color-bg-elevated:     #1E2F40;
  --color-border-subtle:   #2A3F52;
  --color-border-default:  #3A526A;
  --color-border-strong:   #4E6E8A;
  --color-text-primary:    #E2EEF6;
  --color-text-secondary:  #8FAFC4;
  --color-text-muted:      #4E6E8A;
  --color-text-inverse:    #0F172A;
  --color-text-clinical:   #7ACCE6;
  /* All other tokens unchanged in dark mode */
}
```

---

## 26. Anti-Patterns — What Antigravity Must Never Do

These are violations of the frozen design system. Each one is grounds for a sprint rejection:

- **Never use colored left-borders on cards** as status indicators. Status is shown with badges, not borders.
- **Never use a gradient button** except `.btn-clinical-action` (the Sign button). All other buttons use flat background colors.
- **Never use a font other than Inter or JetBrains Mono.** System fonts are fallback only.
- **Never display a clinical numeric value** (IOP, Sph, Cyl, Axis, VA) in Inter font. Always JetBrains Mono.
- **Never display OD without a red badge** or OS without a blue badge. These are clinical safety identifiers.
- **Never repurpose amber (--color-warning) for non-clinical decoration.** Amber means "borderline / pending / expiring" — always.
- **Never change the sidebar background.** `#0D2D3D` is the product's visual identity.
- **Never add icons with colored background circles** (e.g., icon inside a colored rounded square). This is the generic SaaS look the client explicitly rejected when choosing the Lovable design.
- **Never show a raw spinner** where a skeleton can be used. Skeleton screens are required for all data-loading states.
- **Never omit the OD/OS dual-column layout** in any screen that shows refraction data.
- **Never show IOP without semantic coloring** (green/amber/red based on value).
- **Never show a consultation as editable after digital signature.** All fields must be read-only post-signature.

---

*End of GUIDE_02 — Design System*  
*Next document: GUIDE_03 — Data Model*
