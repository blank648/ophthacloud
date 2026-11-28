# OphthaCloud frontend product context

This React 18 / TypeScript / Vite application serves patients, reception staff,
doctors and later administrators in an ophthalmology clinic. Its December review
must demonstrate reception -> prescription -> patient dose confirmation -> doctor
adherence review. Patient usability has priority, especially low-vision users.

Current increment: E1-IO-01, login and persistent accessibility controls. Patient,
reception and doctor workspace screens are separate tasks. JWTs stay in memory;
the server remains authoritative for identity, permissions and clinical data.

Confirmed design preferences: consistent shared controls; solid backgrounds;
restrained borders and rounding; stable layouts; readable Romanian with diacritics;
useful information without implementation notes in product flows. Adapt the earlier
festival project's attention to detail, not its palette, hero typography or dense
filters. The medical palette is provisional and must remain easy to replace.

Operate mode: the user completes a task. Login is primary; display settings are
available before and after authentication. Use code directly for this scoped
implementation, retaining the supplied stack. The approved pink eagle is decorative; the left login panel and brand assets follow DESIGN.md. No marketing imagery is required.

Acceptance: Node 20 build; unit tests around login/session/preference behavior;
keyboard usability; 360px layout and 200% text; measured contrast and 48px targets.
Standards and chosen policies are recorded in docs/IMPLEMENTATION_REFERENCES.md.

