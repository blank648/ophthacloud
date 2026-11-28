# Frontend implementation references

Scope: E1-IO-01, on `feat/fe-shell-e01-a11y`. Project authority remains
`AGENTS.md`, `ARCHITECTURE_AND_EXECUTION_PLAN.md`, `EXECUTION_BOARD_FEREASTRA_1.md`
and `academic/TEAM_PLAYBOOK.md`. References checked on 2026-11-28.

## Standards and security guidance

- [RFC 9110, HTTP semantics](https://www.rfc-editor.org/rfc/rfc9110.html): distinguish 401 (authentication required/invalid) from 403 (access forbidden). A failed login stays on the login form; a protected request's 401 ends the active session once. No retry loop.
- [RFC 6750, bearer token usage](https://www.rfc-editor.org/rfc/rfc6750): use `Authorization: Bearer <token>`, never query strings. This does not make our custom password-login endpoint an OAuth authorization server. Remote deployment requires HTTPS; the current repo explicitly targets local development.
- [RFC 7519, JWT](https://www.rfc-editor.org/rfc/rfc7519): JWT is the backend's token format. The frontend treats the token as opaque. Signature verification, expiry, active-account checks and permissions remain on the backend.
- [OWASP session guidance](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html): no credentials/tokens in localStorage or sessionStorage. The repo mandates in-memory JWTs and reauthentication, so reload ends the frontend session. HttpOnly cookie/BFF designs are alternatives requiring a coordinated backend contract change, not a frontend-only improvement.
- [OWASP authentication guidance](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html): use a generic invalid-email-or-password message. Do not disclose missing or disabled accounts.
- [OWASP authorization guidance](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html): server validates every request. Role routes are UX guards, not the security boundary.

## Accessibility

[WCAG 2.1](https://www.w3.org/TR/WCAG21/) is the project's target: 1.4.3 contrast,
1.4.4 text resizing, 1.4.10 reflow, 2.1.1 keyboard access, 2.4.1 bypass blocks,
2.4.7 visible focus, 3.3.1 errors, 3.3.2 labels, 4.1.2 names/roles/values and
4.1.3 status messages. Semantic HTML comes first; ARIA supplements it.

[WAI form guidance](https://www.w3.org/WAI/tutorials/forms/) informs explicit labels,
associated field errors, focus on invalid input and an announced error summary.
Login allows paste and password managers (`autocomplete=username/current-password`),
also consistent with WCAG 2.2 accessible-authentication guidance. We do not claim a
full WCAG certification from these checks.

Project-specific requirements are stricter/additional: 18px baseline, 48x48px
interactive targets, scales [1, 1.2, 1.5, 1.8, 2], explicit high-contrast state,
and the later patient confirmation flow within two clicks including login submit.
48px targets are not a WCAG 2.1 AA criterion.

## Framework conventions

[React hooks](https://react.dev/reference/rules/rules-of-hooks) and
[effect cleanup](https://react.dev/reference/react/useEffect) guide state and lifecycle.
[React Router 6](https://reactrouter.com/6.30.1/start/overview) supplies routes and
redirects. TypeScript DTOs and service functions are maintainability conventions,
not a formal international standard. Native fetch and the existing client suffice
for this increment; no custom router, crypto, refresh protocol or global query cache.

## Preferences and session decisions

Only visual settings persist on the device. A patient's account preferences load
on login unless the user explicitly changed display settings before login during
this session; that explicit choice then saves to the account. This precedence is
an implementation decision, not specified by WCAG or HTTP. PATCH operations are
serialized to preserve the latest choice. Failure retains local usability and
announces the account-save failure. Staff do not call patient-only preference APIs.
Token expiry is timed from expiresIn; backend 401 remains authoritative. Old-session
responses cannot terminate a new session. Logout clears session identity and token.

## Development and verification

Use Node 20 (`.nvmrc`, package engines). In PowerShell:

```powershell
. "$env:LOCALAPPDATA\ophthacloud-tools\Activate-OphthaCloud.ps1"
npm --prefix academic/frontend ci
npm --prefix academic/frontend test
npm --prefix academic/frontend run build
podman compose -f academic/docker-compose.yml up -d
```

`VITE_USE_MOCKS=true` uses the canonical synthetic demo accounts. Mock errors never
fall back to live and live errors never fall back to mock. `VITE_USE_MOCKS=false`
selects the existing backend via Vite /api proxy. Demo credentials are not displayed in the application login UI. The service implements the live
contract for later INT-01; that integration task still requires the team's merge
and relay workflow before being marked complete.

E-02/E-05/E-06 and administration are explicitly unfinished workspace destinations.
This increment does not claim dose confirmation or clinical workflows.

Live API checks must use http://localhost:5173: Spring's CORS allowlist currently permits that exact origin. 127.0.0.1 and alternate ports are different origins.

