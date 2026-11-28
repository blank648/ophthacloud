# Fereastra 1: frontend handoff (2026-11-28)

Authority: root EXECUTION_BOARD_FEREASTRA_1.md and academic/TEAM_PLAYBOOK.md.
This is a local handoff, not a replacement for the shared board or a claim of merged work.

## Completed locally

E1-IO-01: React login, role routing, in-memory JWT lifecycle, mock/live auth adapter,
persistent accessibility settings, and the approved pink eagle design.
See E1-IO-01_VERIFICATION.md for checks. Changes are uncommitted and unpushed;
review, a green PR and Adrian's merge remain before closing the team task.
The default local environment remains VITE_USE_MOCKS=true.

## Remaining work

| Task | Frontend deliverable / coordination | Target |
|---|---|---|
| INT-01 | After prerequisite merges, use the relay branch with Adrian: real JWT login on all three roles and database preference persistence. Live auth was smoke-tested locally; formal team integration is still pending. Live API origin currently must be localhost:5173; agree any 127.0.0.1 allowlist change with Adrian. | 28 Nov |
| E1-IO-02 | E-05 reception: CNP validation, patient creation and one-time credentials, search, appointments and overlap feedback. E-06 doctor: consultation, prescription schedule, accessible adherence graph/table. Start on mocks. | 1 Dec |
| INT-02 | Bind reception and consultation/prescription screens to Adrian's API and PostgreSQL. | 1–2 Dec |
| E1-IO-03 | E-02 patient: next dose, confirmation within two clicks including login, pending protection, aria-live feedback, progress, basic streak and three explicit states. | 3 Dec |
| INT-03 | Bind patient and doctor adherence screens to Dobrescu's API. Return planId and the exact scheduledFor UTC value supplied by today's-dose response. | 4–5 Dec |
| Feature freeze | All E-01/E-02/E-05/E-06 workflows live against PostgreSQL. | 7 Dec, 20:00 |
| Stabilization | Joint error/DTO/CORS checks; concurrency and time boundaries; keyboard, screen-reader and contrast review at 360px and 100/150/200%; coordinate search performance checks. | 8–13 Dec |
| Rehearsal | Deterministic demo seed, two full rehearsals, zero 5xx; v0.1 tag through the team's workflow. | 14–15 Dec |

The review on 16 December follows reception -> doctor prescription -> patient
login/confirmation -> doctor adherence. Each member must explain their own work.
Current clinical and patient workspace destinations are placeholders, not these screens.

No additional Node/Podman installation is needed. For this window PostgreSQL is the
only container; frontend/backend run natively. Amsler, 20-20-20, wallet/vouchers,
full administration and cloud deployment belong to later scope.
