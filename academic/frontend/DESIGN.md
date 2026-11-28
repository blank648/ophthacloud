---
name: OphthaCloud frontend
colors:
  primary: "#f8bbd0"
  primary-hover: "#f48fb1"
  disabled: "#fce4ec"
  page: "#f2f3f3"
  surface: "#ffffff"
  text: "#2e333c"
  muted: "#5e626b"
  control-border: "#7e7880"
  link-hover: "#92234b"
typography:
  body:
    fontFamily: "Segoe UI, Arial, sans-serif"
    fontSize: "1rem"
    lineHeight: 1.5
rounded:
  control: "8px"
  panel: "16px"
---

## Approved direction

The approved login is implemented in React at /autentificare. Its components and src/index.css are the maintained UI; the temporary prototype directory has been removed. Pale pink actions, a
neutral page and dark text define the provisional medical palette. Semantic CSS
variables are the source of truth for later palette changes.

Use Segoe UI / Arial throughout, including headings. Baseline: 18px; text scales:
100 / 120 / 150 / 180 / 200%. Use Romanian with diacritics and explicit labels.
Enabled controls change background or text color on hover. Keyboard focus remains
visible on buttons and links. Inputs retain a neutral border and visible caret,
without the rejected crimson typing outline.

## Composition and brand

The white login panel sits on the left, vertically centered in the area below the
header. The approved pink eagle faces right and holds the Snellen chart behind
its beak. Preserve its orientation, shapes and chart geometry. Its image fits the
remaining viewport area in both dimensions and cannot set the minimum page height.
Hide the mascot at widths <=1050px. Keep the head logo and OphthaCloud in the header;
use the simplified head for the favicon. Brand and mascot cannot be selected or
dragged. The mascot is decorative, with empty alt text and no pointer events.

Default desktop text fits tested viewports down to 1280x500 without page scrolling.
Compact heights reduce spacing, retaining 48px controls. Small screens and enlarged
text may scroll vertically to preserve access to content; never hide necessary
controls to force a fixed viewport. The application shell reserves a separate header row; only the main content scrolls. Use safe centering so enlarged content starts below the header, and bound the settings popup to the remaining viewport height. This applies before and after login. Avoid horizontal overflow at 360px and 200% text.

No photos, marketing copy, social sign-on, signup, chatbot, prototype disclaimer,
footer tagline or attribution links belong on the application login page.
Asset licensing stays in `THIRD_PARTY_NOTICES.md`.

## Components and interaction

The accessibility dropdown is available before and after login. Use the standing
person inside a circle, minus/plus with a percentage output, and a contrast switch
with explicit Activ/Oprit state. Escape closes it and returns focus; outside focus
or pointer interaction dismisses it. Lucide React supplies interface SVG icons.

The account explanation belongs to the italic information icon beside the email
label. Its tooltip opens on hover, focus or tap; Escape dismisses it. It fits in
unused label-row space on desktop and flows above the input when space is limited,
without obscuring other controls. Password icons describe the action: Eye shows a
hidden password, EyeOff hides a visible password. Accessible names change with it.

Login requires valid email and a nonempty password. Errors are associated with the
fields and summarized on failed requests. Pending submission prevents duplicate
requests. Help and Contactează recepția sit together and may wrap when necessary.
Workspace destinations are explicitly unfinished until their own tasks land.

## Contrast and accessibility

Normal mode uses dark text on light surfaces and pink actions. High contrast uses
black/white surfaces and text, stronger essential outlines, no shadows, and pink
actions. It is a contrast preference with explicit control states, not a claim of
WCAG certification. White/black is 21:1; black/#f8bbd0 is 13.01:1;
black/#f48fb1 is 9.41:1. Disabled controls are identified as disabled semantically.

Targets remain >=48x48 CSS pixels. Preserve password-manager support, keyboard
operation, announced errors and preference-save feedback. Reduced motion disables
the loading animation. Native forced colors retain control boundaries. Public
login has no redundant skip link; the authenticated shell retains its bypass link.


