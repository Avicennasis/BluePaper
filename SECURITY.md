# Security Policy

## Supported versions

Only the latest release tag is supported. Fixes will land on `main` and be
cut as a new patch release; older tags will not be back-patched.

## Reporting a vulnerability

Please **do not** open a public GitHub issue for security problems.

Email **Avicennasis@gmail.com** with:

- A description of the issue.
- Steps to reproduce (or a proof-of-concept).
- The version or commit SHA you found it against.
- Any suggested mitigation if you have one.

Expect an acknowledgement within a week. This is a side-project — there is
no bug bounty and no SLA — but security issues are taken seriously and a
fix and disclosure will be coordinated with you.

## Out of scope

- Issues in upstream dependencies (report upstream).
- Misconfiguration by consumers of this project.

## Threat model

BluePaper is a **local-only cross-platform Bluetooth label-printer app**
(Kotlin Multiplatform / Compose Multiplatform; Android + desktop JVM
targets), distributed as installable builds via GitHub releases / local
Gradle builds. There is no server component, no account system, no
telemetry, and no network API owned by this project — its only external
communication is the Bluetooth link to a Niimbot label printer.

### Trust boundaries

- **Bluetooth link to the printer** — the primary boundary. The printer
  is an untrusted peripheral: responses to the Niimbot protocol
  (status, RFID/label metadata, ack frames) are untrusted input and
  must be length-checked and parsed defensively. Pairing trust and
  link-layer security are delegated to the platform Bluetooth stack.
- **Platform app sandbox** — on Android, label designs and preferences
  live in app-private storage; on desktop, in the user's home
  directory. Other apps/processes are outside the boundary per
  platform rules.
- **User-supplied content** — imported images, fonts, and barcode
  payload data are untrusted input to the rendering/encoding pipeline
  (image decoding, ZXing-style barcode generation); malformed input
  should fail validation, not crash or corrupt output.
- **Distribution chain** — users trust the GitHub release artifact /
  their own Gradle build; no fleet deploy.

### Sensitive data handled

Label designs and their data payloads — usually low sensitivity, but
structured barcode standards (vCard, WiFi, AAMVA, GS1) can embed
personal data, WiFi credentials, or licence data. All of it stays
on-device; nothing is transmitted except to the printer the user
selects.

### Adversaries in scope

- A hostile or spoofed Bluetooth peripheral sending malformed or
  oversized protocol responses.
- Malicious or malformed imported assets (images, barcode payloads).

### Adversaries out of scope

- A compromised OS, JVM, or platform Bluetooth stack.
- An attacker with physical access to an unlocked device.
- Vulnerabilities in upstream libraries (Compose, barcode/image
  libraries) — report upstream.

### Fleet-spec note

The in-house-spec (v1.2.0) Authelia/Traefik proxy-auth assumptions do
**not** apply: BluePaper has no deployed service, no proxy, and no
proxy-injected identity headers. If a hosted component (template
gallery, sync) is ever added, it must adopt the full in-house-spec
baseline (auth, CSRF, rate limiting, health contract, metrics) at
deploy time.
