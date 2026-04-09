# OphthaCloud

This is a multi-tenant SaaS for ophthalmology clinic management.

## Prerequisites

- Homebrew (macOS)
- SDKMAN (or manual java installation: Java 21, Maven 3.9)
- Node.js 20
- Docker Desktop

## Starting the Development Environment

1. **Start all infrastructure services** (PostgreSQL, Redis, Keycloak, MinIO):
   ```bash
   docker compose -f docker/docker-compose.dev.yml up -d
   ```

2. Wait for Keycloak to be healthy (takes ~30s on first run):
   ```bash
   docker compose -f docker/docker-compose.dev.yml logs -f keycloak
   ```

3. **Start Spring Boot backend** (in IntelliJ or terminal):
   ```bash
   cd backend
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

4. **Start Next.js frontend**:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

## Default Access Points
- **Frontend:** [http://localhost:3000](http://localhost:3000)
- **Backend API Docs:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Keycloak Admin:** [http://localhost:8180/admin](http://localhost:8180/admin) (creds: `admin` / `admin`)
- **MinIO Console:** [http://localhost:9001](http://localhost:9001) (creds: `minioadmin` / `minioadmin`)

## Architecture Rules

This project enforces strict modularity via Spring Modulith. Ensure you run `./mvnw compile` frequently. All cross-module communications must occur via the module facade or through published application events.
