# GUIDE_05 — Security: OphthaCloud

> **Document type:** Security Architecture Reference
> **Version:** 1.0 | **Last updated:** 2026-04-02
> **Author:** Project Architect | **Status:** FINAL
> **Prerequisites:** GUIDE_00, GUIDE_01, GUIDE_03, GUIDE_04

***

## 1. Principii Generale

- **Security by default** — orice endpoint este blocat dacă nu are o adnotare `@PreAuthorize` explicită. Spring Security se configurează cu `.anyRequest().denyAll()`.
- **Zero trust per request** — fiecare request validează JWT-ul și extrage permisiunile din el. Nu există sesiuni server-side.
- **Tenant isolation absolut** — nicio interogare SQL nu returnează date fără `WHERE tenant_id = :tenantId`. Tenant-ul se extrage exclusiv din JWT, niciodată din query parameters sau request body.
- **GDPR by design** — datele medicale sunt criptate la rest, accesul este logat în audit log imutabil, consimțământul este condiție pentru activarea contului de pacient.

***

## 2. Keycloak — Configurare Realm

### 2.1 Structura Realm

Un singur realm Keycloak: `ophthacloud`

```
Realm: ophthacloud
├── Clients
│   ├── ophthacloud-backend   ← confidential client (Spring Boot)
│   ├── ophthacloud-frontend  ← public client (Next.js / SPA)
│   └── ophthacloud-portal    ← public client (Patient Portal SPA)
│
├── Realm Roles (globale — pentru management platformă)
│   ├── PLATFORM_ADMIN        ← super-admin platformă (nu are acces la date clinice)
│   └── TENANT_MEMBER         ← orice utilizator dintr-un tenant
│
└── Client Scopes
    ├── ophthacloud-claims    ← scope custom care adaugă tenant_id + permissions în JWT
    └── fhir                  ← scope pentru accesul FHIR R4
```


### 2.2 Configurare Client `ophthacloud-backend`

```
Client ID:              ophthacloud-backend
Client Protocol:        openid-connect
Access Type:            confidential
Service Accounts:       enabled (pentru token introspection)
Authorization:          disabled (gestionăm noi în Spring)
Valid Redirect URIs:    http://localhost:3000/* (dev), https://app.ophthacloud.ro/*
Web Origins:            + (CORS gestionat de Spring Boot)
```


### 2.3 Configurare Client `ophthacloud-frontend`

```
Client ID:              ophthacloud-frontend
Access Type:            public
Standard Flow:          enabled
PKCE:                   S256 (obligatoriu pentru SPA)
Valid Redirect URIs:    http://localhost:3000/*, https://app.ophthacloud.ro/*
Post Logout URIs:       http://localhost:3000/, https://app.ophthacloud.ro/
```


### 2.4 Mapper Custom — `ophthacloud-claims` Scope

Acesta este cel mai important element de configurare. Adaugă în JWT câmpurile `tenant_id` și `permissions`.

Se implementează ca **Protocol Mapper de tip Script** (sau ca SPI custom în Keycloak):

```javascript
// Keycloak Protocol Mapper — adaugă tenant_id și permissions la token
var tenantId = user.getAttribute("tenant_id");
var permissionsJson = user.getAttribute("permissions_json");

token.setOtherClaims("tenant_id", tenantId);
token.setOtherClaims("permissions", JSON.parse(permissionsJson));
token.setOtherClaims("staff_role", user.getAttribute("staff_role"));
token.setOtherClaims("staff_id", user.getAttribute("staff_id"));
```

**Alternativ (recomandat pentru simplitate):** atributele `tenant_id`, `staff_id`, `staff_role` se stochează pe user-ul Keycloak. La login, `permissions_json` se generează din baza de date prin un **Keycloak Event Listener SPI** care citește tabelul `tenant_role_module_permissions`.

***

## 3. Structura JWT

### 3.1 Payload Complet

```json
{
  "exp": 1743634800,
  "iat": 1743631200,
  "jti": "uuid-token-id",
  "iss": "https://auth.ophthacloud.ro/realms/ophthacloud",
  "aud": "ophthacloud-backend",
  "sub": "keycloak-user-uuid",
  "typ": "Bearer",
  "azp": "ophthacloud-frontend",
  "session_state": "uuid",
  "acr": "1",

  "realm_roles": ["TENANT_MEMBER"],
  "preferred_username": "dr.ionescu@clinica-demo.ro",
  "email": "dr.ionescu@clinica-demo.ro",
  "email_verified": true,
  "name": "Alexandru Ionescu",
  "given_name": "Alexandru",
  "family_name": "Ionescu",

  "tenant_id": "11111111-0000-0000-0000-000000000001",
  "staff_id": "22222222-0000-0000-0000-000000000001",
  "staff_role": "DOCTOR",

  "permissions": {
    "patients":      { "view": true,  "create": true,  "edit": true,  "delete": false, "sign": false, "export": true  },
    "appointments":  { "view": true,  "create": true,  "edit": true,  "delete": false, "sign": false, "export": true  },
    "emr":           { "view": true,  "create": true,  "edit": true,  "delete": false, "sign": true,  "export": true  },
    "investigations":{ "view": true,  "create": true,  "edit": true,  "delete": false, "sign": false, "export": true  },
    "prescriptions": { "view": true,  "create": true,  "edit": true,  "delete": false, "sign": true,  "export": true  },
    "optical":       { "view": true,  "create": false, "edit": false, "delete": false, "sign": false, "export": false },
    "notifications": { "view": true,  "create": false, "edit": false, "delete": false, "sign": false, "export": false },
    "reports":       { "view": true,  "create": false, "edit": false, "delete": false, "sign": false, "export": true  },
    "admin":         { "view": false, "create": false, "edit": false, "delete": false, "sign": false, "export": false }
  }
}
```


### 3.2 Durate Token

```
Access Token:   15 minute (scurt — reduce fereastra de atac)
Refresh Token:  8 ore (sesiune de lucru normală)
Session:        8 ore idle / 12 ore max
```

Frontend-ul folosește `keycloak-js` sau `@react-keycloak/web` cu silent refresh automat înainte de expirarea access token-ului.

***

## 4. Spring Security — Configurare

### 4.1 `SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    JwtAuthenticationConverter jwtConverter) throws Exception {
        http
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/v1/public/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // FHIR — requires authentication
                .requestMatchers("/fhir/r4/**").authenticated()
                // Toate celelalte sunt blocate implicit
                .anyRequest().denyAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
            );

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "http://localhost:3000",
            "https://app.ophthacloud.ro"
        ));
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```


### 4.2 `JwtAuthenticationConverter.java`

Extrage `tenant_id`, `staff_id`, `staff_role`, și `permissions` din JWT și le pune în `Authentication` principal.

```java
@Component
public class OphthaClinicalJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String tenantId   = jwt.getClaimAsString("tenant_id");
        String staffId    = jwt.getClaimAsString("staff_id");
        String staffRole  = jwt.getClaimAsString("staff_role");
        Map<String, Object> permissions = jwt.getClaimAsMap("permissions");

        // Verificare obligatorie — orice token fără tenant_id este respins
        if (tenantId == null) {
            throw new InvalidBearerTokenException("Token does not contain tenant_id claim.");
        }

        OphthaPrincipal principal = new OphthaPrincipal(
            jwt.getSubject(),
            tenantId,
            staffId,
            staffRole,
            parsePermissions(permissions)
        );

        Collection<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_" + staffRole)
        );

        return new OphthaClinicalAuthenticationToken(principal, jwt, authorities);
    }

    private Map<String, ModulePermissions> parsePermissions(Map<String, Object> raw) {
        // Deserializare din JWT claim map în ModulePermissions records
        ...
    }
}
```


### 4.3 `OphthaPrincipal.java`

```java
public record OphthaPrincipal(
    String keycloakUserId,
    String tenantId,
    String staffId,
    String staffRole,
    Map<String, ModulePermissions> permissions
) {}

public record ModulePermissions(
    boolean view,
    boolean create,
    boolean edit,
    boolean delete,
    boolean sign,
    boolean export
) {}
```


***

## 5. RBAC — Permission Evaluator

### 5.1 `OphthaClinicalPermissionEvaluator.java`

```java
@Component
public class OphthaClinicalPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) {
        return false; // nu folosim forma cu targetDomainObject
    }

    @Override
    public boolean hasPermission(Authentication auth,
                                  Serializable moduleCode,
                                  String targetType,
                                  Object permission) {
        if (!(auth.getPrincipal() instanceof OphthaPrincipal principal)) return false;

        String module = (String) moduleCode;
        String action = (String) permission;

        ModulePermissions perms = principal.permissions().get(module);
        if (perms == null) return false;

        return switch (action) {
            case "VIEW"   -> perms.view();
            case "CREATE" -> perms.create();
            case "EDIT"   -> perms.edit();
            case "DELETE" -> perms.delete();
            case "SIGN"   -> perms.sign();
            case "EXPORT" -> perms.export();
            default       -> false;
        };
    }
}
```


### 5.2 Utilizare în Controllers

```java
// Forma corectă — moduleCode, targetType (ignorat), action
@PreAuthorize("hasPermission('patients', 'MODULE', 'VIEW')")
@PreAuthorize("hasPermission('emr', 'MODULE', 'SIGN')")
@PreAuthorize("hasPermission('admin', 'MODULE', 'CREATE')")

// Shorthand mai lizibil — prin alias-uri configurate în SpEL
@PreAuthorize("canView('patients')")
@PreAuthorize("canSign('emr')")
```


### 5.3 Tenant Isolation la Nivel de Service

**Fiecare** metod de service care face o query la DB primește `tenantId` din principal și nu din request:

```java
@Service
@RequiredArgsConstructor
public class PatientManagementFacade {

    private final PatientRepository patientRepository;

    public Page<PatientSummaryDto> listPatients(String searchQuery, Pageable pageable) {
        // tenantId extras din SecurityContextHolder — NICIODATĂ din parametri externi
        String tenantId = SecurityUtils.currentTenantId();
        return patientRepository.searchByTenant(tenantId, searchQuery, pageable)
                                .map(PatientSummaryDto::from);
    }
}
```

```java
@Component
public class SecurityUtils {
    public static String currentTenantId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.getPrincipal() instanceof OphthaPrincipal p) return p.tenantId();
        throw new IllegalStateException("No authenticated principal in SecurityContext");
    }

    public static String currentStaffId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.getPrincipal() instanceof OphthaPrincipal p) return p.staffId();
        throw new IllegalStateException("No authenticated principal in SecurityContext");
    }
}
```


***

## 6. Matricea RBAC — Role × Module × Acțiune

Valorile de mai jos sunt **default-urile la inițializarea unui tenant nou** (Flyway seed). Admin-ul clinicii le poate modifica ulterior din modulul Admin (GUIDE_04 §10.2).


| Modul | ADMIN | DOCTOR | NURSE | OPTICIAN | RECEPTIONIST | PATIENT |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| **patients** VIEW | ✅ | ✅ | ✅ | ✅ | ✅ | own only |
| **patients** CREATE | ✅ | ✅ | ✅ | ❌ | ✅ | ❌ |
| **patients** EDIT | ✅ | ✅ | ✅ | ❌ | ✅ | ❌ |
| **patients** DELETE | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **patients** EXPORT | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| **appointments** VIEW | ✅ | ✅ | ✅ | ❌ | ✅ | own only |
| **appointments** CREATE | ✅ | ✅ | ✅ | ❌ | ✅ | ❌ |
| **appointments** EDIT | ✅ | ✅ | ✅ | ❌ | ✅ | ❌ |
| **appointments** DELETE | ✅ | ✅ | ❌ | ❌ | ✅ | ❌ |
| **emr** VIEW | ✅ | ✅ | ✅ | ❌ | ❌ | own only |
| **emr** CREATE | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| **emr** EDIT | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| **emr** SIGN | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| **emr** EXPORT | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| **investigations** VIEW | ✅ | ✅ | ✅ | ❌ | ❌ | own only |
| **investigations** CREATE | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| **investigations** EDIT | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| **prescriptions** VIEW | ✅ | ✅ | ✅ | ✅ | ❌ | own only |
| **prescriptions** CREATE | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| **prescriptions** SIGN | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| **optical** VIEW | ✅ | ✅ | ❌ | ✅ | ✅ | own only |
| **optical** CREATE | ✅ | ✅ | ❌ | ✅ | ✅ | ❌ |
| **optical** EDIT | ✅ | ❌ | ❌ | ✅ | ✅ | ❌ |
| **optical** DELETE | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **notifications** VIEW | ✅ | ✅ | ❌ | ❌ | ✅ | ❌ |
| **notifications** EDIT | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **reports** VIEW | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| **reports** EXPORT | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| **admin** ALL | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |

> **PATIENT role** este special — autorizat exclusiv prin scopul `ophthacloud-portal` client. Toate resursele sunt filtrate `WHERE patient_id = :currentPatientId`. Nu are access la endpoints din `/api/v1/` — folosește exclusiv `/api/v1/portal/me/...`.

***

## 7. GDPR — Implementare

### 7.1 Audit Log Imutabil

Tabelul `audit_log` din GUIDE_03 are un trigger PostgreSQL care blochează fizic UPDATE și DELETE:

```sql
CREATE OR REPLACE FUNCTION audit_log_immutability_guard()
RETURNS TRIGGER AS $$
BEGIN
  RAISE EXCEPTION 'audit_log records are immutable. Action: %, Table: audit_log', TG_OP;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER enforce_audit_log_immutability
BEFORE UPDATE OR DELETE ON audit_log
FOR EACH ROW EXECUTE FUNCTION audit_log_immutability_guard();
```

Înregistrările se scriu prin `AuditLogService` care este injectat în fiecare Facade și se apelează explicit:

```java
auditLogService.log(AuditEntry.builder()
    .action("SIGN")
    .entityType("consultations")
    .entityId(consultationId)
    .userId(SecurityUtils.currentStaffId())
    .changedFields(null)
    .ipAddress(requestContext.getIpAddress())
    .build());
```


### 7.2 Criptare Date Sensibile

Coloanele cu date medicale sensibile sunt criptate la rest prin **PostgreSQL pgcrypto** sau prin **JPA AttributeConverter**:

```java
@Converter
public class SensitiveFieldEncryptor implements AttributeConverter<String, String> {

    @Value("${app.encryption.key}")
    private String encryptionKey;

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null) return null;
        return AesGcmUtil.encrypt(plaintext, encryptionKey);
    }

    @Override
    public String convertToEntityAttribute(String ciphertext) {
        if (ciphertext == null) return null;
        return AesGcmUtil.decrypt(ciphertext, encryptionKey);
    }
}
```

Câmpurile criptate:

- `patients.cnp`
- `patients.insurance_number`
- `consultation_sections.section_data` (JSONB — criptare la nivel de coloană)
- `patient_attachments.minio_path`

Cheia de criptare se stochează în **Hetzner Secret Manager** / variabilă de mediu injectată prin Docker secret — niciodată în `application.yml` sau în repository.

### 7.3 Consimțământ GDPR (Consent Management)

La crearea unui cont de pacient în Portal, fluxul de onboarding colectează consimțămintele și le salvează în `patient_consents`:

```
Tipuri de consimțământ obligatorii:
- TREATMENT             ← consimțământ pentru tratament medical
- DATA_PROCESSING       ← prelucrare date cu caracter personal (obligatoriu GDPR)
- DATA_SHARING          ← partajare cu terți (opțional)
- MARKETING             ← comunicări de marketing (opțional)
- PORTAL_TERMS          ← termeni și condiții portal pacient
```

Endpoint:

```
PUT /api/v1/portal/me/consents
Body: { "consentType": "MARKETING", "granted": false, "revokedAt": "2026-04-02T10:00:00Z" }
```

Retragerea consimțământului `DATA_PROCESSING` declanșează automat **Right to Erasure Flow** (§7.4).

### 7.4 Right to Erasure (Dreptul la Uitare)

Implementat ca un job asincron declanșat manual de `ADMIN`:

```
POST /api/v1/admin/patients/{id}/gdpr-erasure
Body: { "erasureType": "ANONYMIZE", "reason": "Cerere pacient conform GDPR Art.17" }
```

**Tipuri:**

- `ANONYMIZE` — șterge toate câmpurile identificabile (nume, CNP, telefon, email, adresă), păstrează datele clinice anonimizate pentru statistici (obligatoriu legal în RO)
- `FULL_DELETE` — șterge fizic toate înregistrările (numai dacă nu există obligații legale de retenție)

**Câmpuri anonimizate la `ANONYMIZE`:**

```sql
UPDATE patients SET
  first_name     = 'ANONYMIZED',
  last_name      = 'ANONYMIZED',
  cnp            = NULL,
  phone          = NULL,
  email          = NULL,
  address        = NULL,
  city           = NULL,
  county         = NULL,
  insurance_number = NULL,
  emergency_contact_name  = NULL,
  emergency_contact_phone = NULL,
  has_portal_access = false,
  gdpr_erasure_requested_at = NOW(),
  gdpr_erasure_completed_at = NOW()
WHERE id = :patientId AND tenant_id = :tenantId;
```

Evenimentul se loghează în `audit_log` cu `action = 'GDPR_ERASURE'` și nu poate fi șters.

### 7.5 Data Retention Policy

Politicile de retenție se configurează în `clinic_settings` și sunt aplicate printr-un **Spring Batch job** rulat în fiecare noapte la 02:00 ora României:

```
Consultations signed:    10 ani (Legea 46/2003 — dosarul medical)
Prescriptions:           5 ani
Invoices:                10 ani (legislație fiscală RO)
Audit log:               permanent (nu se poate șterge)
Notification log:        2 ani
Soft-deleted patients:   90 zile (înainte de anonymize automat)
```


### 7.6 Export Date Pacient (Art. 20 GDPR — Portabilitate)

```
GET /api/v1/admin/patients/{id}/gdpr-export
```

Generează un ZIP cu:

- `patient_profile.json` — datele personale
- `consultations/` — toate consultațiile în format JSON + PDF
- `prescriptions/` — toate prescripțiile PDF
- `investigations/` — fișierele atașate
- `audit_log.json` — istoricul acceselor la dosarul pacientului

Export disponibil în `downloads` timp de 24h, după care URL-ul expiră.

***

## 8. Securitate Specifică Oftalmologiei

### 8.1 Flagging Medicamente cu Risc Chirurgical

La completarea Secțiunii H (Tratament — `current_medications`) din consultație, backend-ul verifică automat dacă medicamentele menționate sunt pe lista de medicamente cu risc perioperator.

**Tabel de referință** (seeded la inițializare):

```sql
CREATE TABLE surgical_risk_medications (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name        TEXT NOT NULL,
  active_substance TEXT,
  risk_class  TEXT NOT NULL,  -- 'STOP_REQUIRED' | 'CAUTION' | 'INFORM_ANESTHESIOLOGIST'
  stop_days_before_surgery INT,
  notes       TEXT
);
```

**Exemple seed:**

```
Aspirina (Acid Acetilsalicilic)  → STOP_REQUIRED, 7 zile înainte
Warfarină / Acenocumarol         → STOP_REQUIRED, 5 zile înainte (cu bridging)
Metformin                         → STOP_REQUIRED, 48h înainte (risc acidoză lactică)
Tamsulosin (Flomax)              → CAUTION (IFIS — Intraoperative Floppy Iris Syndrome)
Silodosin, Alfuzosin             → CAUTION (IFIS)
Clopidogrel                       → STOP_REQUIRED, 7 zile înainte
NOAC (Apixaban, Rivaroxaban)     → STOP_REQUIRED, 24-48h înainte
```

**IFIS (Intraoperative Floppy Iris Syndrome)** este deosebit de important în oftalmologie — alfa-blocantele pentru hipertrofia de prostată cauzează complicații majore la chirurgia cataractei. Flagul IFIS se afișează cu badge roșu în Secțiunea A a consultației dacă pacientul este pe alfa-blocante.

**Endpoint de verificare (apelat de frontend la save Secțiunea H):**

```
POST /api/v1/emr/consultations/{id}/check-medication-risks
Body: { "medications": "Tamsulosin 0.4mg, Metformin 500mg, Aspirina 75mg" }
```

**Response:**

```json
{
  "data": {
    "hasRisks": true,
    "flags": [
      {
        "medication": "Tamsulosin",
        "riskClass": "CAUTION",
        "category": "IFIS",
        "notes": "Risc IFIS la chirurgia cataractei. Informați chirurgul.",
        "displayBadge": "IFIS ⚠️"
      },
      {
        "medication": "Aspirina",
        "riskClass": "STOP_REQUIRED",
        "stopDaysBeforeSurgery": 7,
        "notes": "Se oprește cu 7 zile înainte de orice intervenție chirurgicală oculară.",
        "displayBadge": "STOP PRE-OP ⛔"
      },
      {
        "medication": "Metformin",
        "riskClass": "STOP_REQUIRED",
        "stopDaysBeforeSurgery": 2,
        "notes": "Se oprește cu 48h înainte de anestezie generală.",
        "displayBadge": "STOP PRE-OP ⛔"
      }
    ]
  }
}
```

Flag-urile se salvează în `consultation_sections.section_data` al Secțiunii H și se afișează vizibil în UI.

### 8.2 Consultation Lock după Semnare

Odată semnată, o consultație este **imutabilă**. Orice tentativă de PUT/PATCH pe consultația semnată sau pe secțiunile ei returnează 409:

```java
// In ConsultationService
if (consultation.getStatus() == ConsultationStatus.SIGNED) {
    throw new ConsultationAlreadySignedException(consultationId);
}
```

**Excepție:** un `ADMIN` poate adăuga o **notă de addendum** (`POST /api/v1/emr/consultations/{id}/addendum`) care se salvează separat și este log-ată în audit — nu modifică consultația originală.

### 8.3 IOP Alert Thresholds

La salvarea Secțiunii C (Tensiune oculară), backend-ul evaluează valorile IOP și atașează un alert dacă sunt în afara parametrilor normali:

```java
// IOP Alert Logic
if (iop > 21) alert = "HIGH";           // > 21 mmHg — posibil glaucom
if (iop > 30) alert = "CRITICAL";       // > 30 mmHg — urgenție
if (iop < 8)  alert = "HYPOTONY";       // < 8 mmHg — hipotonie oculară
if (previousIop != null && Math.abs(iop - previousIop) > 5) alert = "SIGNIFICANT_CHANGE";
```

Alertele sunt returnate în response-ul `PUT /api/v1/emr/consultations/{id}/sections/C` și afișate în UI cu badge colorat.

***

## 9. Rate Limiting

Implementat cu **Bucket4j + Redis** ca Spring Filter:

```
Endpoints autenticate:     200 req/min per user
Endpoints publice (/public): 20 req/min per IP
Login (Keycloak):           10 tentative/15min per IP (configurat în Keycloak Brute Force Protection)
File upload:                10 uploads/min per user
PDF generation:             5 req/min per user
```

Depășirea limitei → 429 `RATE_LIMIT_EXCEEDED` cu header `Retry-After: 60`.

***

## 10. Configurare `application.yml` — Security Properties

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8180/realms/ophthacloud}
          jwk-set-uri: ${KEYCLOAK_JWK_URI:http://localhost:8180/realms/ophthacloud/protocol/openid-connect/certs}

app:
  security:
    cors-allowed-origins: ${CORS_ORIGINS:http://localhost:3000}
    encryption-key: ${APP_ENCRYPTION_KEY}   # din Docker secret / Hetzner Secret Manager
    jwt-tenant-claim: tenant_id
    jwt-staff-claim: staff_id
    jwt-role-claim: staff_role
    jwt-permissions-claim: permissions
  rate-limiting:
    enabled: ${RATE_LIMITING_ENABLED:true}
    authenticated-rpm: 200
    public-rpm: 20
```


***
