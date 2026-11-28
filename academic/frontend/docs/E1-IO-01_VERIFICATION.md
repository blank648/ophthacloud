# E1-IO-01 implementation and verification

Branch: `feat/fe-shell-e01-a11y`. Ready for review; no commit, push or merge performed.
The shared root execution board is not changed because the work boundary is academic/.

## Delivered

- Approved pink login design implemented at /autentificare (temporary prototype directory removed): vertically centered left panel, responsive decorative eagle, nonselectable brand, head logo and favicon. Licensing stays in THIRD_PARTY_NOTICES.md, without login-page attribution links.
- Persistent accessibility bar before/after login: bounded text steps 1 / 1.2 / 1.5 / 1.8 / 2, explicit contrast state, local visual preferences.
- Typed mock/live auth and patient preference services using the existing API contracts.
- In-memory token, role destinations, duplicate-submit prevention, generic login errors, expiry/logout and stale-response handling.
- Semantic forms, password-manager autocomplete, password visibility, field error associations, focus handling, italic information tooltip and authenticated-shell skip link (no redundant skip link on public login).
- Patient preferences: account load, explicit pre-login choice precedence, serialized saves and visible failure feedback. Staff do not PATCH patient preferences.

## Changed files

- `.nvmrc`, `package.json`, `package-lock.json`: Node 20, test tooling, patched Vite 6.
- `src/App.tsx`: shell, route destinations and role guards.
- `src/index.css`: semantic palette and responsive/accessibility styles.
- `src/types/api.ts`: PreferencesRequest aligned with backend DTO.
- `src/lib/apiClient.ts`: distinguish public-login 401 from active-session expiry; ignore old-session responses.
- `src/features/auth/authService.ts`, `SessionProvider.tsx`, `LoginPage.tsx`: auth, session/preferences and form.
- `src/features/accessibility/preferences.ts`, `src/components/AccessibilityBar.tsx`: visual preferences and persistent controls.
- `vitest.config.ts`, `src/App.test.tsx`, `src/lib/apiClient.test.ts`, `src/features/accessibility/preferences.test.ts`: unit/component verification.
- `PRODUCT.md`, `DESIGN.md`, `.impeccable/design.json`, `.impeccable/.gitignore`, `docs/IMPLEMENTATION_REFERENCES.md`, this file: durable scope, design, source references and results.

## Results (2026-11-28)

- Node v20.20.2; clean npm ci, frontend TypeScript and production Vite build: PASS.
- `npm --prefix academic/frontend test`: 21 tests, 3 files, all PASS.
- Backend unit tests and ArchUnit: 43 tests, no failures/errors/skips, PASS. Backend source unchanged.
- Browser on the actual React route: 1920x1080, 1536x730, 1366x768 and 1280x500 have no page overflow at default text size. Mobile 360px at 200% text + high contrast has no horizontal overflow; mascot is hidden. Visible controls checked >=48px. Password action icons, tooltip dismissal, mock login/role routing, post-login settings and logout checked. No page errors captured.
- Persistent-header regression: 12 browser cases across 1920x1080, 1366x768, 1280x500 and 360x640 at 100/150/200%. No header/form overlap, horizontal document overflow or header movement during content scrolling. Settings popup remains within the viewport. Patient login, post-login 200% settings/contrast and logout also checked. Main content scrolls independently beneath the header; floating information tooltips dismiss on scroll.
- Contrast: main text/white 12.69:1; secondary text/white 6.11:1; action text/pink 7.86:1, hover 5.69:1; field border/field 4.12:1. High contrast: white/black 21:1, black/pink 13.01:1, black/hover 9.41:1. These checks do not establish whole-site WCAG conformance.
- Live API browser smoke at http://localhost:5173: login -> /pacient, GET /me, PATCH preferences; separate GET /me confirms uiFontScale 1.2 and highContrast true persisted. Logout returns to login. Restored demo patient preferences to 1 / false afterward.
- Live test configuration was temporary. Default `.env` remains VITE_USE_MOCKS=true.
- Spring CORS currently allows http://localhost:5173 only. Use that origin for live tests; 127.0.0.1 and alternate ports are distinct origins. Backend configuration was preserved.
- Final visual review performed inline, not by an independent agent. Captures are local/ignored under .impeccable/review/.

## Remaining scope

E-02, E-05, E-06 and administration destinations are explicitly unfinished scaffolds.
No dose-confirmation or clinical functionality is claimed. INT-01 remains subject to
the playbook's prerequisite merges and relay branch workflow. This smoke test does
not mark that team integration task complete.

The existing React Router 6 dependency family still reports two moderate npm audit
findings. No high/critical findings remain in the installed tree. A router-major
migration is outside this task. The existing CI workflow runs backend tests on Java 21 and the frontend clean install/build on Node 20. It does not currently run the frontend unit tests; those were run locally. No remote feature branch/PR exists yet, so there is no GitHub Actions result for these uncommitted changes.




