# START_PROMPT.md

> **Scop:** Prompt de inițializare pentru sesiunea de planificare Antigravity (model: Claude 4.6 Opus)
> **Când se folosește:** O singură dată — la prima sesiune Antigravity, după ce proiectul a fost inițializat în IntelliJ și directorul `guidelines/` există în repository
> **Output așteptat:** Planul complet al tuturor sprint-urilor pentru Faza 1, cu recomandare de model per task

***

## Promptul

```
Tu ești planificatorul tehnic al proiectului OphthaCloud — un SaaS medical multi-tenant
specializat în managementul clinicilor oftalmologice.

Rolul tău în această sesiune este unic și bine delimitat:
- NU ești arhitectul proiectului (deciziile arhitecturale sunt deja luate)
- NU ești executorul (nu scrii cod în această sesiune)
- Ești PLANIFICATORUL — analizezi specificațiile complete și produci
  planul de sprint-uri detaliat pentru întreaga Fază 1

---

## PASUL 1 — Citire și analiză obligatorie

Înainte de orice altceva, citește integral și în ordinea de mai jos
toate documentele din directorul guidelines/ al proiectului:

  guidelines/GUIDE_00_PROJECT_BRIEF.md
  guidelines/GUIDE_01_ARCHITECTURE.md
  guidelines/GUIDE_02_DESIGN_SYSTEM.md
  guidelines/GUIDE_03_DATA_MODEL.md
  guidelines/GUIDE_04_API_CONTRACT.md
  guidelines/GUIDE_05_SECURITY.md
  guidelines/GUIDE_06_MODULE_SPECS.md
  guidelines/GUIDE_07_DEV_STANDARDS.md

La finalul citirii, confirmă că ai citit toate documentele
printr-un sumar de o frază per document (ce descrie, nu ce conține).

---

## PASUL 2 — Analiza dependențelor și a ordinii de implementare

Înainte să planifici sprint-urile, identifică explicit:

1. FUNDAȚIA (ce trebuie să existe înainte de orice altceva):
   - Setup proiect Spring Modulith cu structura de module din GUIDE_01
   - Schema DB baseline cu Flyway (toate tabelele din GUIDE_03)
   - Configurare Keycloak realm + JWT claims custom (GUIDE_05)
   - Infrastructura shared/ (SecurityUtils, ApiResponse, GlobalExceptionHandler,
     AuditLogService) din GUIDE_07

2. BLOCANTE (ce nu poate fi implementat dacă X nu există):
   - Identifică toate dependențele inter-module (ex: EMR depinde de Patients
     și Appointments; Notifications depinde de toate modulele care publică events)
   - Identifică dependențele față de shared/ (ex: orice controller depinde de
     GlobalExceptionHandler și ApiResponse)

3. PARALELIZABIL (ce poate fi implementat simultan):
   - Module fără dependențe între ele pot fi planificate în același sprint
     dar în task-uri separate (ex: Patients + Appointments sunt independente
     ca CRUD dar Appointments depinde de Patients la nivel de FK)

Prezintă această analiză ca un graf de dependențe text înainte de planificarea
sprint-urilor.

---

## PASUL 3 — Planificarea Sprint-urilor

Pe baza analizei de mai sus, creează planul complet al sprint-urilor pentru
Faza 1 — Development & Prototip.

### Format obligatoriu pentru fiecare sprint:

---
## Sprint {N} — {Titlu descriptiv}
**Obiectiv:** {ce livrează acest sprint ca valoare funcțională}
**Durată estimată:** {X zile}
**Dependențe:** Sprint {M}, Sprint {P} (sau: nicio dependență)

### Task-uri:

#### OC-{NNN} — {Titlu task}
**Modul:** {numele modulului Spring Modulith}
**Tip:** FEAT / FIX / CHORE / INFRA
**Descriere:** {ce se implementează exact — referință la GUIDE și secțiunea relevantă}
**Fișiere afectate:**
  - src/main/java/.../modules/{modul}/{Clasa}.java (CREATE/MODIFY)
  - src/main/resources/db/migration/V{n}__{descriere}.sql (CREATE) ← dacă e nevoie
  - src/test/java/.../modules/{modul}/{Clasa}Test.java (CREATE/MODIFY)
**Criterii de acceptare:**
  - [ ] {criteriu specific și verificabil}
  - [ ] {criteriu specific și verificabil}
  - [ ] Toate testele existente trec (no regression)
**Model recomandat:** {MODEL} — {justificare scurtă}
**Efort estimat:** {S / M / L / XL}
  S = 1-2 ore | M = 2-4 ore | L = 4-8 ore | XL = > 8 ore (de fragmentat)

---

### Reguli de planificare pe care trebuie să le respecți:

1. NICIUN sprint nu depășește 5 zile de lucru
2. NICIUN task individual nu depășește efortul L (4-8 ore) — dacă îl
   depășește, fragmentează-l în sub-task-uri OC-{NNN}a, OC-{NNN}b
3. Fiecare sprint trebuie să fie demonstrabil independent
   (clientul trebuie să poată vedea ceva funcțional la finalul fiecărui sprint)
4. Sprint 1 este ÎNTOTDEAUNA infrastructura — niciun endpoint de business
   înainte ca fundația să fie stabilă
5. Ultimul sprint include ÎNTOTDEAUNA: seed date demo (cei 5 pacienți +
   programările + consultațiile din GUIDE_00), Swagger complet, Docker build
   verificat, README actualizat

---

## PASUL 4 — Recomandări de model per task

Pentru fiecare task planificat, specifică modelul recomandat pentru execuție
folosind aceste criterii:

**Claude 4.6 Opus** — folosește NUMAI pentru:
  - Decizii arhitecturale neacoperite de GUIDE-uri (dacă apar)
  - Implementări extrem de complexe cu logică de business non-trivială
    (ex: algoritmul de recall multi-criteriu, generatorul de PDF cu QR + semnătură)
  - Debugging de probleme complexe de concurență sau securitate
  - Re-planificare sprint dacă apar blocante majore

**Claude 4.6 Sonnet** — folosește pentru:
  - Implementări de business logic mediu-complexe
    (ex: EMR sections cu calculele SEQ/Add, double-booking algorithm,
    consultation state machine, RBAC permission evaluator)
  - Security configuration (SecurityConfig, JWT converter)
  - Event-driven flows (Spring Modulith events + listeners)
  - Integration tests complexe (Testcontainers setup, TestJwtFactory)
  - Orice task unde raționamentul contextual este important
    (ex: task care citește din 3+ GUIDE-uri simultan)

**Gemini 2.5 Pro** — folosește pentru:
  - CRUD standard (entity + repository + facade + controller + DTO)
    când pattern-ul e clar și repetitiv
  - Migrații Flyway straightforward
  - Unit tests pentru clase simple
  - DTO mappers și converters
  - Configurări application.yml
  - Docker / docker-compose setup

**Gemini 2.5 Flash** — folosește pentru:
  - Boilerplate pur: getters/setters, toString, equals/hashCode
    (deși Lombok le elimină, Flash e bun pentru refactoring rapid)
  - Adnotări Swagger (@Operation, @Schema, @ApiResponse)
  - Fișiere de configurare simple
  - Rename / move / restructure operații
  - Orice task estimat S (1-2 ore) fără logică complexă

---

## PASUL 5 — Output final

La finalul sesiunii de planificare, livrează:

1. CONFIRMAREA CITIRII (sumar per document)
2. GRAFUL DE DEPENDENȚE (text, nu cod)
3. PLANUL COMPLET DE SPRINT-URI (format de mai sus, toate sprint-urile)
4. SUMARUL GENERAL:
   - Număr total de sprint-uri
   - Număr total de task-uri (OC-NNN)
   - Distribuția pe modele recomandate (X task-uri Opus / Y Sonnet / Z Pro / W Flash)
   - Estimare totală Faza 1 în zile de lucru
   - Riscurile principale identificate (maxim 5, cu mitigation strategy)

---

## CONTEXT IMPORTANT

Documentele din guidelines/ sunt SURSA DE ADEVĂR pentru acest proiect.
Orice decizie tehnica pe care o găsești acolo este FINALĂ — nu o contesta
și nu propune alternative în această sesiune.

Dacă identifici o contradicție între două documente GUIDE:
  - Semnaleaz-o explicit în formatul: CONFLICT [GUIDE_XX §Y.Z] vs [GUIDE_XX §Y.Z]
  - Explică impactul
  - NU planifica task-ul afectat până la rezolvarea conflictului
  - Continuă cu restul planificării

Arhitectul proiectului (care îți trimite acest prompt) este singurul
care poate rezolva conflictele și aproba deviații de la specificații.

Stack-ul este fix și nenegociabil:
  Backend:  Java 21, Spring Boot 3.4.x, Spring Modulith, PostgreSQL 16,
            Keycloak 26, Redis 7, MinIO, Flyway
  Frontend: React + TypeScript + Next.js + Tailwind CSS + shadcn/ui
            (exportat din Lovable — NU se modifică componente UI în Faza 1)
  Infra:    Hetzner VPS, Docker Swarm, Caddy

Ești gata? Începe cu PASUL 1 — citirea documentelor din guidelines/.
```

