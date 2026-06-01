# OphthaCloud Frontend

Stack: React 18 + TypeScript + Vite + TailwindCSS + shadcn/ui + React Query v5 + Zustand + Keycloak-js

Romanian-language ophthalmology EMR/ERP frontend. Talks to the OphthaCloud backend over `/api/v1/*` and authenticates against Keycloak (`ophthacloud` realm).

## Setup

```bash
cp .env.example .env
npm install
npm run dev
```

Environment variables (see `.env.example`):

| Var | Default | Purpose |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080` | Backend REST API base URL |
| `VITE_KEYCLOAK_URL` | `http://localhost:8081` | Keycloak server URL |
| `VITE_KEYCLOAK_REALM` | `ophthacloud` | Keycloak realm |
| `VITE_KEYCLOAK_CLIENT_ID` | `ophthacloud-frontend` | Keycloak client ID |

The dev server proxies `/api` and `/auth` to the configured backends, so the SPA can run on a single origin during development.

## Build

```bash
npm run build       # production build → /dist
npm run preview     # serve the built bundle locally
```

The bundle is plain static files and can be hosted behind nginx or any static host. When deployed inside a monorepo at `/frontend`, the Vite `base: '/'` setting keeps URLs root-relative.

## Project structure

```
src/
  components/      Shared UI (sidebar, layout, error boundary, guards…)
  contexts/        App + Data React contexts (demo data fallback)
  data/            Demo seed data (Romanian patients, ICD-10, etc.)
  hooks/           React Query hooks per module (patients, appointments, emr, …)
  lib/             apiClient, auth (Keycloak), permissions, queryClient
  pages/           Route-level pages
  services/        Typed API service modules
  stores/          Zustand stores (auth)
  types/           Shared TS DTOs (patients, appointments, emr, optical, …)
```

## API contract

All responses follow the standard envelope:

```ts
{ success: boolean, data: T, error?: { code, message, fieldErrors? } }
```

`apiClient` unwraps `data` automatically and surfaces `error.code` / `fieldErrors` on rejected promises. The global `<ApiErrorToaster />` translates well-known codes (`VALIDATION_ERROR`, `DOUBLE_BOOKING`, `CONSULTATION_ALREADY_SIGNED`, `403`, …) into Romanian toasts.

## Auth

`initKeycloak()` runs once at startup with `onLoad: 'check-sso'`. When the backend isn't reachable the app degrades to demo mode (login screen + seed data) so the application keeps working offline.
