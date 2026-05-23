# OphthaCloud Backend

OphthaCloud is a multi-tenant ophthalmology SaaS.

## Sprint 11 Status (Production Readiness)
All 10 modules completed:
- `admin`
- `appointments`
- `emr`
- `investigations`
- `notifications`
- `optical`
- `patients`
- `portal`
- `prescriptions`
- `reports`

Infrastructure & Documentation:
- Swagger documentation added for all modules
- Multi-stage Dockerfile included
- End-to-end Smoke Test covers auth to report KPIs
- V24 Flyway demo tenant seed data included

## Prerequisites
- Java 21
- Maven (or use included `mvnw`)
- Docker & Docker Compose (for PostgreSQL, Redis, MinIO, Keycloak)

## Quick Start

1. **Start infrastructure**
   ```bash
   docker compose --profile dev up -d
   ```

2. **Run the backend**
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

## Environment Variables
- `SPRING_DATASOURCE_URL` - PostgreSQL URL (default: `jdbc:postgresql://localhost:5432/ophthacloud`)
- `SPRING_DATASOURCE_USERNAME` - DB User (default: `postgres`)
- `SPRING_DATASOURCE_PASSWORD` - DB Password (default: `postgres`)
- `SPRING_DATA_REDIS_HOST` - Redis Host (default: `localhost`)
- `SPRING_DATA_REDIS_PORT` - Redis Port (default: `6379`)
- `MINIO_URL` - MinIO endpoint (default: `http://localhost:9000`)
- `MINIO_ACCESS_KEY` - MinIO access key (default: `minioadmin`)
- `MINIO_SECRET_KEY` - MinIO secret key (default: `minioadmin`)
- `JWT_SECRET` - JWT HMAC Secret for Dev (default: `a-very-long-and-secure-secret-key-for-dev-only-change-in-prod`)

## API Documentation
Once the server is running, visit:
- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`
