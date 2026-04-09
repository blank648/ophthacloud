# GUIDE_07 — Dev Standards: OphthaCloud

> **Document type:** Development Standards Reference
> **Version:** 1.0 | **Last updated:** 2026-04-02
> **Author:** Project Architect | **Status:** FINAL
> **Prerequisites:** GUIDE_00, GUIDE_01, GUIDE_03, GUIDE_04, GUIDE_05, GUIDE_06

***

## Scop

Acest document definește standardele de cod, testare, Git workflow și procesul de lucru cu Antigravity. Respectarea acestor standarde este **non-negociabilă** — asigură consistența bazei de cod între sprint-uri și între sesiuni de lucru cu AI.

***

## 1. Structura Proiectului

### 1.1 Directorul Rădăcină

```
ophthacloud-backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── ro/ophthacloud/
│   │   │       ├── OphthaCloudApplication.java
│   │   │       ├── shared/          ← utilitare cross-module
│   │   │       └── modules/         ← toate modulele Spring Modulith
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/        ← fișiere Flyway
│   └── test/
│       └── java/
│           └── ro/ophthacloud/
│               ├── shared/
│               └── modules/
├── docker/
│   ├── docker-compose.dev.yml
│   └── docker-compose.prod.yml
├── docs/
│   └── adr/                         ← Architecture Decision Records
├── guidelines/                      ← toate documentele GUIDE_XX
├── pom.xml
└── README.md
```


### 1.2 Structura unui Modul Spring Modulith

Fiecare modul urmează **același pattern intern**. Exemplu pentru modulul `patients`:

```
modules/patients/
├── PatientManagementFacade.java      ← SINGURA clasă publică a modulului
├── PatientModuleConfig.java          ← configurare Spring specifică modulului (dacă e nevoie)
├── internal/
│   ├── PatientController.java        ← REST controller
│   ├── PatientRepository.java        ← Spring Data JPA repository
│   ├── PatientEntity.java            ← entitate JPA
│   ├── PatientMedicalHistoryEntity.java
│   ├── PatientConsentEntity.java
│   └── PatientAttachmentEntity.java
├── dto/
│   ├── PatientDto.java               ← response DTO (full)
│   ├── PatientSummaryDto.java        ← response DTO (list)
│   ├── CreatePatientRequest.java     ← request DTO
│   └── UpdatePatientRequest.java     ← request DTO
└── event/
    ├── PatientCreatedEvent.java
    └── PatientPortalInvitedEvent.java
```

**Regula de vizibilitate Spring Modulith:**

- `PatientManagementFacade` — `public` — poate fi injectată din alte module
- Tot ce este în `internal/` — package-private sau `public` dar **niciodată injectat direct din alt modul**
- Alte module apelează **exclusiv** prin `PatientManagementFacade`, niciodată prin `PatientRepository` sau `PatientEntity` direct


### 1.3 Pachetul `shared/`

Conține clase utilitare fără logică de business, utilizabile din orice modul:

```
shared/
├── security/
│   ├── OphthaPrincipal.java
│   ├── OphthaClinicalAuthenticationToken.java
│   ├── OphthaClinicalJwtConverter.java
│   ├── OphthaClinicalPermissionEvaluator.java
│   └── SecurityUtils.java
├── api/
│   ├── ApiResponse.java
│   ├── PagedApiResponse.java
│   ├── PaginationMeta.java
│   ├── ErrorResponse.java
│   └── GlobalExceptionHandler.java
├── audit/
│   ├── AuditLogService.java
│   ├── AuditLogEntity.java
│   └── AuditEntry.java
├── tenant/
│   └── TenantContext.java
└── util/
    ├── MrnGenerator.java
    ├── AesGcmUtil.java
    └── DateTimeUtils.java
```


***

## 2. Convenții Java

### 2.1 Naming

| Element | Convenție | Exemplu |
| :-- | :-- | :-- |
| Clasă / Interfață | PascalCase | `PatientManagementFacade` |
| Metodă / Câmp | camelCase | `findActivePatients()` |
| Constantă | UPPER_SNAKE_CASE | `MAX_RETRY_ATTEMPTS` |
| Pachet | lowercase, fără underscore | `ro.ophthacloud.modules.patients` |
| Tabel DB (în cod Java) | PascalCase în `@Table` | `@Table(name = "patients")` |
| Enum | PascalCase tip, UPPER_SNAKE_CASE valori | `AppointmentStatus.CHECKED_IN` |
| DTO Request | sufix `Request` | `CreatePatientRequest` |
| DTO Response (full) | sufix `Dto` | `PatientDto` |
| DTO Response (list) | sufix `SummaryDto` | `PatientSummaryDto` |
| Event | sufix `Event` | `PatientCreatedEvent` |
| Facade | sufix `Facade` | `PatientManagementFacade` |
| Repository | sufix `Repository` | `PatientRepository` |
| Entity JPA | sufix `Entity` | `PatientEntity` |

### 2.2 Utilizarea Lombok

Lombok este permis și recomandat pentru eliminarea boilerplate-ului, dar cu restricții clare:

**Permis:**

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"sectionData"})  // exclude câmpuri mari
@EqualsAndHashCode(of = {"id"})       // ÎNTOTDEAUNA doar pe ID pentru entități JPA
```

**Interzis:**

```java
@Data           // prea permisiv — generează equals/hashCode pe toate câmpurile, probleme cu JPA proxies
@SneakyThrows   // ascunde excepții checked, face debugging imposibil
@Cleanup        // folosiți try-with-resources în schimb
```

**Pentru entități JPA — pattern obligatoriu:**

```java
@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"medicalHistory", "consultations"})
@EqualsAndHashCode(of = {"id"})
public class PatientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    // Nu folosiți @Builder pe entitățile JPA — folosiți constructori expliciți sau factory methods
    public static PatientEntity create(UUID tenantId, String firstName, ...) {
        PatientEntity e = new PatientEntity();
        e.tenantId = tenantId;
        e.firstName = firstName;
        ...
        return e;
    }
}
```

**Pentru DTO-uri și Records — preferați Java Records:**

```java
// Records pentru DTO-uri imutabile (response)
public record PatientSummaryDto(
    UUID id,
    String mrn,
    String firstName,
    String lastName,
    LocalDate dateOfBirth,
    int age,
    String phone
) {
    public static PatientSummaryDto from(PatientEntity entity) {
        return new PatientSummaryDto(
            entity.getId(),
            entity.getMrn(),
            entity.getFirstName(),
            entity.getLastName(),
            entity.getDateOfBirth(),
            Period.between(entity.getDateOfBirth(), LocalDate.now()).getYears(),
            entity.getPhone()
        );
    }
}

// @Builder pe request DTO-uri (mutabile, validare)
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePatientRequest {
    @NotBlank
    @Size(min = 2, max = 100)
    private String firstName;

    @NotBlank
    @Size(min = 2, max = 100)
    private String lastName;

    @NotNull
    @Past
    private LocalDate dateOfBirth;
    ...
}
```


### 2.3 Gestionarea Excepțiilor

**Excepții custom per domeniu** — definite în `internal/` al fiecărui modul:

```java
// Naming: {Entity}{Reason}Exception
public class PatientNotFoundException extends RuntimeException {
    public PatientNotFoundException(UUID patientId) {
        super("Patient with ID " + patientId + " not found.");
    }
}

public class ConsultationAlreadySignedException extends RuntimeException {
    public ConsultationAlreadySignedException(UUID consultationId) {
        super("Consultation " + consultationId + " is already signed and cannot be modified.");
    }
}

public class DoubleBookingException extends RuntimeException {
    private final UUID conflictingAppointmentId;
    public DoubleBookingException(UUID conflictingId) {
        super("Doctor already has an appointment at the requested time slot.");
        this.conflictingAppointmentId = conflictingId;
    }
    public UUID getConflictingAppointmentId() { return conflictingAppointmentId; }
}
```

**`GlobalExceptionHandler`** în `shared/api/` mapează fiecare excepție la HTTP status și error code:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PatientNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorResponse handlePatientNotFound(PatientNotFoundException ex, HttpServletRequest req) {
        return buildError("PATIENT_NOT_FOUND", ex.getMessage(), req);
    }

    @ExceptionHandler(ConsultationAlreadySignedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ErrorResponse handleAlreadySigned(ConsultationAlreadySignedException ex, HttpServletRequest req) {
        return buildError("CONSULTATION_ALREADY_SIGNED", ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> new FieldError(e.getField(), "INVALID_FORMAT", e.getDefaultMessage()))
            .toList();
        return buildValidationError(fieldErrors, req);
    }

    // Niciodată: catch Exception generick care înghite erori neașteptate fără logging
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ErrorResponse handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error on {}", req.getRequestURI(), ex);
        return buildError("INTERNAL_ERROR", "An unexpected error occurred.", req);
    }
}
```


### 2.4 Tranzacții

- `@Transactional` **numai pe metode Facade** — nu pe Repository, nu pe Controller
- `@Transactional(readOnly = true)` pe toate metodele de citire din Facade — optimizare Hibernate
- Niciodată `@Transactional` pe constructori sau pe metode private
- Dimensiunea tranzacției: **cât mai mică posibil** — nu includeți apeluri externe (Keycloak, MinIO, SendGrid) în interiorul tranzacției DB

```java
@Service
@Transactional(readOnly = true)  // default read-only pentru toate metodele
public class PatientManagementFacade {

    @Transactional  // suprascrie la metodele de scriere
    public PatientDto createPatient(CreatePatientRequest request) {
        // DB operations here — OK
        PatientEntity saved = patientRepository.save(...);
        auditLogService.log(...);
        // NU apelați Keycloak sau email în interiorul acestei tranzacții
        return PatientDto.from(saved);
    }

    // readOnly = true moștenit — nu necesită adnotare
    public Page<PatientSummaryDto> listPatients(String query, Pageable pageable) {
        return patientRepository.search(tenantId, query, pageable).map(PatientSummaryDto::from);
    }
}
```


### 2.5 Queries JPA

- Folosiți **Spring Data JPA derived queries** pentru queries simple
- Folosiți **`@Query` cu JPQL** pentru queries cu JOIN-uri sau filtre complexe
- Folosiți **`@Query` cu native SQL** numai când JPQL nu permite (window functions, JSONB operators, CTE)
- **Niciodată** `EntityManager.createNativeQuery()` direct în cod de business — totul trece prin Repository

```java
// GOOD — derived query
List<PatientEntity> findByTenantIdAndIsActiveTrue(UUID tenantId);

// GOOD — JPQL cu @Query
@Query("""
    SELECT p FROM PatientEntity p
    WHERE p.tenantId = :tenantId
      AND p.isActive = true
      AND (
        LOWER(p.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR
        LOWER(p.lastName)  LIKE LOWER(CONCAT('%', :q, '%')) OR
        p.mrn LIKE CONCAT('%', :q, '%')
      )
    ORDER BY p.lastName ASC
    """)
Page<PatientEntity> searchByTenant(
    @Param("tenantId") UUID tenantId,
    @Param("q") String query,
    Pageable pageable
);

// GOOD — native pentru JSONB
@Query(value = """
    SELECT cs.* FROM consultation_sections cs
    WHERE cs.consultation_id = :consultationId
      AND cs.section_data @> :jsonFilter::jsonb
    """, nativeQuery = true)
List<ConsultationSectionEntity> findByJsonFilter(
    @Param("consultationId") UUID consultationId,
    @Param("jsonFilter") String jsonFilter
);
```


### 2.6 Logging

Framework: **SLF4J + Logback** (inclus în Spring Boot)

```java
@Slf4j  // Lombok — generează: private static final Logger log = LoggerFactory.getLogger(...)
public class PatientManagementFacade {

    public PatientDto createPatient(CreatePatientRequest request) {
        log.debug("Creating patient for tenant {}", SecurityUtils.currentTenantId());
        ...
        log.info("Patient created: mrn={}, tenantId={}", patient.getMrn(), patient.getTenantId());
        ...
    }
}
```

**Niveluri:**

- `DEBUG` — detalii de execuție (query params, valori intermediare) — numai în dev
- `INFO` — operații semnificative (creare resurse, schimbări de status, semnări)
- `WARN` — situații neașteptate dar recuperabile (retry, fallback, valori ciudate dar valide)
- `ERROR` — erori care necesită atenție (excepții neașteptate, eșecuri de integrare externă)

**Niciodată** nu logați CNP, parolă, token, sau date medicale complete la nivel INFO sau mai sus.

***

## 3. Strategie de Testare

### 3.1 Piramida de Teste

```
         /──────────────────────────────\
        /   E2E / Integration (puțin)    \      ← Testcontainers + PostgreSQL
       /──────────────────────────────────\
      /      Module Integration Tests      \     ← Spring Modulith @ApplicationModuleTest
     /──────────────────────────────────────\
    /         Unit Tests (majoritate)        \   ← JUnit 5 + Mockito
   /────────────────────────────────────────────\
```

**Target de acoperire:**

- Unit tests: minim **80%** line coverage per Facade și Service class
- Integration tests: minim **1 test per endpoint** din GUIDE_04
- Module boundary tests: minim **1 test per event publicat**


### 3.2 Unit Tests — JUnit 5 + Mockito

**Convenție de naming:** `{Clasa}Test` în același pachet ca clasa testată.

**Pattern Arrange-Act-Assert obligatoriu:**

```java
@ExtendWith(MockitoExtension.class)
class PatientManagementFacadeTest {

    @Mock PatientRepository patientRepository;
    @Mock AuditLogService auditLogService;
    @Mock SecurityUtils securityUtils;

    @InjectMocks PatientManagementFacade facade;

    @Test
    @DisplayName("createPatient: should generate MRN and return PatientDto")
    void createPatient_shouldGenerateMrnAndReturnDto() {
        // Arrange
        var request = CreatePatientRequest.builder()
            .firstName("Gheorghe")
            .lastName("Ionescu")
            .dateOfBirth(LocalDate.of(1963, 8, 15))
            .gender(Gender.MALE)
            .build();

        var savedEntity = PatientEntity.create(TENANT_ID, "Gheorghe", "Ionescu", ...);
        savedEntity.setMrn("OC-004821");

        when(patientRepository.save(any())).thenReturn(savedEntity);
        when(patientRepository.findMaxMrnSuffix(TENANT_ID)).thenReturn(Optional.of(4820));

        // Act
        PatientDto result = facade.createPatient(request);

        // Assert
        assertThat(result.getMrn()).isEqualTo("OC-004821");
        assertThat(result.getFirstName()).isEqualTo("Gheorghe");
        verify(auditLogService).log(argThat(e -> e.getAction().equals("CREATE")));
    }

    @Test
    @DisplayName("createPatient: should throw when dateOfBirth is in future")
    void createPatient_shouldThrow_whenDateOfBirthInFuture() {
        // Arrange
        var request = CreatePatientRequest.builder()
            .firstName("Test")
            .lastName("Patient")
            .dateOfBirth(LocalDate.now().plusDays(1))  // viitor
            .gender(Gender.MALE)
            .build();

        // Act + Assert
        assertThatThrownBy(() -> facade.createPatient(request))
            .isInstanceOf(ConstraintViolationException.class);
    }
}
```

**Reguli:**

- `@DisplayName` obligatoriu pe fiecare test — format: `methodName: should [expected] when [condition]`
- Un singur `assert` logic per test (pot fi multiple `assertThat` pe același obiect)
- Nu folosiți `@SpringBootTest` în unit tests — e lent și testează prea mult
- Folosiți `AssertJ` (`assertThat`) nu `JUnit assert*` — mai expresiv


### 3.3 Integration Tests — Testcontainers

**Convenție de naming:** `{Clasa}IntegrationTest`

**Configurare bază — clasă abstractă reutilizabilă:**

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("ophthacloud_test")
        .withUsername("test")
        .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired protected TestRestTemplate restTemplate;
    @Autowired protected JdbcTemplate jdbcTemplate;

    // Helper pentru autentificare în teste
    protected HttpHeaders headersForRole(String role) {
        String token = TestJwtFactory.createToken(role, TEST_TENANT_ID, TEST_STAFF_ID);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE patients, appointments, consultations CASCADE");
    }
}
```

**Exemplu test de endpoint:**

```java
class PatientControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("POST /api/v1/patients: should create patient and return 201 with MRN")
    void createPatient_shouldReturn201WithMrn() {
        // Arrange
        var request = Map.of(
            "firstName", "Gheorghe",
            "lastName", "Ionescu",
            "dateOfBirth", "1963-08-15",
            "gender", "MALE"
        );

        // Act
        var response = restTemplate.exchange(
            "/api/v1/patients",
            HttpMethod.POST,
            new HttpEntity<>(request, headersForRole("DOCTOR")),
            Map.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKey("data");
        var data = (Map<?, ?>) response.getBody().get("data");
        assertThat(data.get("mrn").toString()).startsWith("OC-");
        assertThat(data.get("firstName")).isEqualTo("Gheorghe");
    }

    @Test
    @DisplayName("POST /api/v1/patients: should return 403 when role lacks CREATE permission")
    void createPatient_shouldReturn403_whenMissingPermission() {
        var response = restTemplate.exchange(
            "/api/v1/patients",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("firstName", "Test"), headersForRole("PATIENT")),
            Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
```


### 3.4 Spring Modulith Module Tests

Testează că granițele modulelor sunt respectate și că event-urile sunt publicate corect:

```java
@ApplicationModuleTest
class PatientsModuleTest {

    @Autowired PatientManagementFacade facade;
    @RecordApplicationEvents ApplicationEvents events;

    @Test
    @DisplayName("Module boundary: PatientRepository should not be accessible from outside")
    void verifyModuleStructure() {
        // Spring Modulith verifică automat că internal/ nu e accesat din afară
        // Acest test verifică structura la startup — dacă compilează, e valid
    }

    @Test
    @DisplayName("createPatient: should publish PatientCreatedEvent")
    @Transactional
    void createPatient_shouldPublishEvent() {
        facade.createPatient(validRequest());

        assertThat(events.ofType(PatientCreatedEvent.class)).hasSize(1);
        assertThat(events.ofType(PatientCreatedEvent.class).findFirst().get().tenantId())
            .isEqualTo(TEST_TENANT_ID);
    }
}
```


### 3.5 `TestJwtFactory` — Helper pentru Tokens de Test

```java
public class TestJwtFactory {

    private static final String SECRET = "test-secret-key-for-testing-only-32chars";

    public static String createToken(String role, UUID tenantId, UUID staffId) {
        Map<String, Object> permissions = buildDefaultPermissions(role);

        return Jwts.builder()
            .subject("test-user-" + role.toLowerCase())
            .claim("tenant_id", tenantId.toString())
            .claim("staff_id", staffId.toString())
            .claim("staff_role", role)
            .claim("permissions", permissions)
            .claim("email", role.toLowerCase() + "@test.ophthacloud.ro")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 3600_000))
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
            .compact();
    }
}
```


***

## 4. Git Workflow

### 4.1 Branch Strategy

```
main          ← producție — cod livrat, testat, aprobat
  └── dev     ← integrare sprint — merge-uri de feature branches
        └── feature/OC-XXX-descriere-scurta
        └── fix/OC-XXX-descriere-bug
        └── chore/OC-XXX-descriere-task
```

**Reguli:**

- **Niciodată push direct pe `main`** — numai prin PR din `dev`
- **Niciodată push direct pe `dev`** — numai prin PR din feature branch
- Un branch per task Jira / sprint item
- Branch-urile se șterg după merge


### 4.2 Naming Convention Branch-uri

```
feature/OC-{nr}-{descriere-kebab-case}
fix/OC-{nr}-{descriere-bug}
chore/OC-{nr}-{descriere-task}
hotfix/OC-{nr}-{descriere-urgenta}

Exemple:
feature/OC-001-patient-crud-endpoints
feature/OC-012-emr-section-a-autosave
fix/OC-047-double-booking-race-condition
chore/OC-003-flyway-baseline-migration
hotfix/OC-089-jwt-tenant-id-null-check
```


### 4.3 Commit Message Format

Urmează **Conventional Commits** (`type(scope): description`):

```
feat(patients): add MRN generation with per-tenant sequence locking
fix(appointments): correct double booking check to exclude cancelled status
test(emr): add integration tests for consultation sign endpoint
refactor(shared): extract TenantContext from SecurityUtils
chore(deps): upgrade Spring Boot to 3.4.5
docs(adr): add ADR-003 for choosing Spring Modulith over microservices
```

**Tipuri valide:** `feat`, `fix`, `test`, `refactor`, `chore`, `docs`, `perf`, `ci`

**Scope-uri valide:** numele modulului — `patients`, `appointments`, `emr`, `investigations`, `prescriptions`, `optical`, `notifications`, `reports`, `admin`, `shared`, `security`, `deps`, `ci`

**Reguli pentru mesaj:**

- Linia 1 (subject): maxim 72 caractere, imperativ prezent ("add", "fix", "update" — nu "added", "fixing")
- Linia 2: goală
- Linia 3+ (body): opțional, explică *de ce* nu *ce* (ce e vizibil în cod)

```
feat(emr): add SEQ auto-computation on section A save

SEQ = Sph + Cyl/2, rounded to nearest 0.25D per clinical standard.
Computed server-side to ensure consistency regardless of client.
Stored in section_data.od.seq and section_data.os.seq.
```


### 4.4 PR Checklist (Pull Request)

Fiecare PR trebuie să bifeze toate punctele înainte de merge. Antigravity include acest checklist în orice PR pe care îl generează:

```markdown
## PR Checklist

### Cod
- [ ] Codul respectă naming conventions din GUIDE_07 §2.1
- [ ] Nicio clasă din `internal/` nu este injectată direct din alt modul
- [ ] Toate metodele Facade au `@Transactional` sau `@Transactional(readOnly=true)`
- [ ] Niciun apel extern (Keycloak, MinIO, email) în interiorul tranzacției DB
- [ ] `@PreAuthorize` prezent pe fiecare endpoint controller
- [ ] `tenant_id` extras din JWT (SecurityUtils), niciodată din request params

### Teste
- [ ] Unit tests scrise pentru toate metodele Facade noi/modificate
- [ ] Integration test scris pentru fiecare endpoint nou
- [ ] `./mvnw test` rulează fără erori
- [ ] Acoperire nou cod ≥ 80% (verificat cu JaCoCo)

### Baza de Date
- [ ] Fișier Flyway `V{n}__descriere.sql` adăugat pentru orice modificare de schemă
- [ ] Nicio modificare manuală în DB fără migration Flyway corespunzătoare
- [ ] Migration testată pe DB curat (drop + flyway migrate)

### Documentație
- [ ] Swagger `@Operation` și `@Schema` actualizate pentru endpoint-uri noi/modificate
- [ ] ADR creat dacă s-a luat o decizie arhitecturală semnificativă (§5)
- [ ] GUIDE relevant actualizat dacă specificația s-a schimbat (cu notă în PR)
```


***

## 5. Architecture Decision Records (ADR)

Orice decizie arhitecturală **ireversibilă sau semnificativă** se documentează ca ADR în `docs/adr/`.

### 5.1 Format ADR

Fișier: `docs/adr/ADR-{NNN}-{titlu-kebab}.md`

```markdown
# ADR-001: Spring Modulith în loc de Microservicii

## Status
Accepted

## Data
2026-04-02

## Context
OphthaCloud trebuie să gestioneze 11 domenii funcționale (patients, emr, appointments etc.)
cu granițe clare între ele. Inițial s-a considerat arhitectura microservicii.

## Decizie
Se folosește Spring Modulith (monolith modular) în loc de microservicii.

## Motive
1. Echipa de development este mică (1-2 persoane + AI) — overhead-ul operațional
   al microserviciilor (service discovery, distributed tracing, network latency) este disproporționat
2. Clientul are o singură clinică în Faza 1 — nu există nevoie de scalare independentă per modul
3. Spring Modulith asigură granițele între module la compile time, prevenind coupling-ul accidental
4. Migrarea la microservicii rămâne posibilă în Faza 3 dacă volumul o cere

## Consecințe
+ Deploy simplu (un singur container Spring Boot)
+ Tranzacții locale în loc de saga distributed
+ Debugging simplificat
- Scalare orizontală afectează toate modulele simultan
- Viitor: dacă modulul Optical crește semnificativ, va necesita extragere separată
```


### 5.2 ADR-uri Deja Decise (la start proiect)

```
ADR-001 — Spring Modulith vs microservicii                 → Accepted
ADR-002 — PostgreSQL schema-per-tenant vs row-level        → Accepted (row-level, tenant_id pe fiecare tabel)
ADR-003 — Keycloak pentru autentificare vs Spring Security standalone → Accepted
ADR-004 — MinIO pentru file storage vs S3                  → Accepted (Hetzner Object Storage compatibil S3)
ADR-005 — Flyway pentru migrații vs Liquibase               → Accepted
ADR-006 — iText pentru PDF vs JasperReports                → Pending (de decis în Sprint 3)
```


***

## 6. Definition of Done (DoD) per Sprint

Un sprint item este **DONE** numai când toate criteriile sunt bifate:

### 6.1 DoD pentru Feature (endpoint nou sau flux de business)

```
Cod:
□ Implementare completă conform GUIDE_04 (request/response exact ca în contract)
□ Validări conform GUIDE_06 (business rules implementate)
□ Tenant isolation: tenant_id extras din JWT, prezent în toate queries
□ Permisiuni: @PreAuthorize pe controller, logica în PermissionEvaluator
□ Audit log: AuditLogService.log() apelat pentru acțiunile din GUIDE_06 §11.1
□ Error handling: excepții custom mapate în GlobalExceptionHandler

Teste:
□ Unit tests pentru Facade: happy path + minim 2 edge cases per metodă
□ Integration test: happy path + 403 (forbidden) + 404 (not found) per endpoint
□ Toate testele existente trec (no regression)

DB:
□ Flyway migration adăugată dacă schema s-a modificat
□ Indecsi adăugați conform GUIDE_03 (tabelul de indecși)

Documentație:
□ Swagger adnotat complet
□ ADR creat dacă a fost necesară o decizie arhitecturală
```


### 6.2 DoD pentru Bug Fix

```
□ Test care reproduce bug-ul ÎNAINTE de fix (test eșuează fără fix)
□ Fix implementat
□ Test trece cu fix-ul
□ Commit message include "fix:" prefix și numărul ticket-ului
```


### 6.3 DoD pentru Sprint Complet

```
□ Toate feature items au DoD individual bifat
□ ./mvnw verify rulează fără erori (compile + test + checkstyle)
□ Acoperire generală ≥ 75% (raport JaCoCo generat)
□ Docker image builduit cu succes: docker build -t ophthacloud-backend .
□ Flyway migrate rulează cu succes pe DB curat
□ Swagger UI accesibil și afișează toate endpoint-urile noi
□ README.md actualizat dacă setup-ul local s-a schimbat
```


***

## 7. Instrucțiuni pentru Antigravity

Această secțiune definește modul în care Antigravity trebuie să opereze pe parcursul Fazei 1.

### 7.1 Rolul Antigravity

Antigravity este **executorul tehnic** al acestui proiect. Rolul său este să implementeze specificațiile din GUIDE_00 — GUIDE_07 cu fidelitate maximă, nu să le reinterpreteze.

Antigravity **nu ia decizii arhitecturale**. Dacă o specificație este ambiguă sau contradictorie, escaladează — nu inventează o soluție și nu alege alternativa care i se pare mai elegantă.

### 7.2 Ordinea de Citire a Documentelor la Start

La prima sesiune și la orice sesiune nouă după o pauză, Antigravity citește documentele în această ordine înainte de a scrie orice cod:

```
1. GUIDE_00 — context de business și obiective
2. GUIDE_01 — arhitectura și stack-ul tehnic
3. GUIDE_03 — schema DB (ce există deja în DB)
4. GUIDE_04 — API contract (ce trebuie să returneze codul)
5. GUIDE_05 — securitate (cum se autorizează fiecare endpoint)
6. GUIDE_06 — specificații module (ce face codul în interior)
7. GUIDE_07 — standardele de cod (cum se scrie codul)
```


### 7.3 Planificarea unui Sprint

La începutul fiecărui sprint, Antigravity:

1. **Citește** task-urile de sprint primite
2. **Identifică** documentele GUIDE relevante pentru fiecare task
3. **Propune** ordinea de implementare (dependențe între task-uri)
4. **Confirmă** cu arhitectul planul înainte de a scrie cod
5. **Implementează** task cu task, în ordinea confirmată
6. **Raportează** după fiecare task: ce s-a implementat, ce teste au trecut, ce deviații față de spec (dacă există)

### 7.4 Cum Gestionează Conflictele între Specificații

Dacă Antigravity identifică o contradicție între două documente GUIDE:

```
PROTOCOL:
1. Oprește implementarea task-ului afectat
2. Citează exact cele două specificații contradictorii (documentul + secțiunea + textul)
3. Explică impactul fiecărei variante
4. Cere decizie arhitectului
5. NU implementează nicio variantă până la confirmare
6. Documentează decizia primită ca ADR dacă este semnificativă
```

**Exemplu de escaladare corectă:**

```
Conflict identificat între:
- GUIDE_04 §6.2: "Backend auto-generează prescriptionNumber în format RX-{YYYY}-{MRN-suffix}"
- GUIDE_03 §prescriptions: "prescription_number VARCHAR(50) NOT NULL UNIQUE"

Implicație: dacă un pacient primește 2 prescripții în același an, formatul curent
generează duplicate (RX-2026-004821 de două ori).

Opțiuni:
A) Adaug sufix secvențial: RX-2026-004821, RX-2026-004821-2, RX-2026-004821-3
B) Folosesc un counter global per tenant: RX-2026-000001, RX-2026-000002...
C) Folosesc UUID parțial: RX-2026-004821-a3f8

Aștept decizia arhitectului înainte de implementare.
```


### 7.5 Ce NU face Antigravity fără aprobare

- Nu schimbă schema DB (tabel nou, coloană nouă, index nou) fără Flyway migration aprobată
- Nu modifică structura de module Spring Modulith (nu mută clase între `internal/` și root)
- Nu adaugă dependențe noi în `pom.xml` fără justificare explicită
- Nu modifică `SecurityConfig` sau `OphthaClinicalPermissionEvaluator`
- Nu elimină `@PreAuthorize` de pe niciun endpoint "temporar pentru testare"
- Nu hardcodează `tenant_id` sau credențiale în cod sau teste


### 7.6 Formatul de Raportare Progress

La finalul fiecărui task implementat, Antigravity raportează:

```markdown
## Task OC-XXX completat: {descriere task}

### Ce s-a implementat
- [listă scurtă de fișiere create/modificate]

### Teste
- Unit tests: X teste, toate trec
- Integration tests: Y teste, toate trec

### Devieri față de spec
- [NONE] sau [descriere deviație + motiv]

### Blocat / Necesită decizie
- [NONE] sau [descriere blocaj]

### Următorul task propus
- OC-YYY: {descriere}
```


***

## 8. Configurare Mediu Local

### 8.1 `docker-compose.dev.yml`

```yaml
version: '3.9'
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: ophthacloud_dev
      POSTGRES_USER: ophthacloud
      POSTGRES_PASSWORD: devpassword
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  keycloak:
    image: quay.io/keycloak/keycloak:26.0
    command: start-dev --import-realm
    environment:
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/ophthacloud_dev
      KC_DB_USERNAME: ophthacloud
      KC_DB_PASSWORD: devpassword
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
    ports:
      - "8180:8080"
    volumes:
      - ./docker/keycloak-realm-dev.json:/opt/keycloak/data/import/realm.json
    depends_on:
      - postgres

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio_data:/data

volumes:
  postgres_data:
  minio_data:
```


### 8.2 `application-dev.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ophthacloud_dev
    username: ophthacloud
    password: devpassword
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate   # Flyway gestionează schema — Hibernate nu modifică nimic
  flyway:
    enabled: true
    locations: classpath:db/migration
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/ophthacloud

logging:
  level:
    ro.ophthacloud: DEBUG
    org.springframework.security: DEBUG
    org.flywaydb: INFO

app:
  security:
    cors-allowed-origins: http://localhost:3000
    encryption-key: dev-encryption-key-32chars-minimum
  storage:
    minio-endpoint: http://localhost:9000
    minio-access-key: minioadmin
    minio-secret-key: minioadmin
    bucket-name: ophthacloud-dev
```


### 8.3 Comanda de Start Rapid

```bash
# Start infrastructură
docker compose -f docker/docker-compose.dev.yml up -d

# Run aplicație
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run teste cu Testcontainers (nu necesită Docker Compose pornit — Testcontainers pornește propria instanță)
./mvnw test

# Verificare acoperire teste
./mvnw verify   # generează raport JaCoCo în target/site/jacoco/index.html
```


***