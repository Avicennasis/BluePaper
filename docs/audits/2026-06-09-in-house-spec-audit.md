# 2026-06-09 — in-house-spec v1.2.0 baseline audit

**Spec**: in-house-spec v1.2.0 (`IN-HOUSE-CONVENTIONS.md`)
**Auditor**: Claude (automated `bin/check-spec.py --audit` + manual review)
**Repo state at audit**: branch `main`, commit `b315a25`

## Deployment-model assessment

BluePaper is **not a deployed fleet service**. It is a local-only
cross-platform Bluetooth label-printer application (Kotlin
Multiplatform / Compose Multiplatform; `shared/`, `androidApp/`,
`desktopApp/` modules) distributed as GitHub release builds or local
`./gradlew :desktopApp:run`. Its only external communication is the
Bluetooth link to a Niimbot printer. No server component, no network
API, no systemd unit, no container. The wiki has no
`projects/bluepaper.md` service page; the git-standards rollout log
(`operations/git-standards-rollout.md`) records it as a real KMP
project repo with no release pipeline obligations (Finding Y not
applicable at rollout time). The in-house-spec's service baseline
(systemd/Docker profile, FastAPI auth/CSRF/rate-limiting/health/
metrics contracts, deploy.sh, Python dependency lockfiles) therefore
does not apply.

**Recommendation: adopt-on-deploy.** If a hosted companion service
(template gallery, sync backend) is ever built, it must adopt the full
in-house-spec baseline at deploy time. Until then, only the cheap
universal items apply.

## Checker output (before)

`check-spec.py --audit BluePaper` — 7 findings:

1. missing required file: `requirements.in`
2. missing required file: `requirements.lock`
3. missing required file: `.pre-commit-config.yaml`
4. missing required file: `docs/audits/README.md`
5. missing required file: `deploy.sh`
6. `.gitignore` does not ignore `venv/`
7. missing unit file `BluePaper-host.service` (and no compose file for
   the Docker profile)

## Disposition

| Finding | Disposition |
|---|---|
| `requirements.in` / `requirements.lock` | **N/A** — no Python; Kotlin/Gradle (KMP) project. Gradle dependency management + Dependabot `gradle` ecosystem are the language-appropriate equivalent. Adopt-on-deploy for any future Python service component. |
| `.pre-commit-config.yaml` | **Deferred** — spec hook baseline is `py_compile` over Python entry points; none exist. A Kotlin-appropriate hook set (ktlint/detekt) would be a future improvement, not a spec mapping. |
| `docs/audits/README.md` | **Fixed** — index created, this shard is the first entry. |
| `deploy.sh` | **N/A** — no deploy target; distribution is Gradle builds / GitHub releases. Adopt-on-deploy. |
| `.gitignore` venv/ | **Fixed** — `venv/` added (cheap universal item; guards throwaway Python tooling venvs). |
| unit file / compose file | **N/A** — no runtime host. Adopt-on-deploy. |
| `SECURITY.md` threat model (§Documentation, manual check) | **Fixed** — threat model added: untrusted-Bluetooth-peripheral boundary, platform sandbox, user-supplied asset/barcode-payload input, distribution chain; Authelia assumption explicitly N/A. |

Checker quirk (same as TaskAlarm audit): expected unit name is derived
from the directory name verbatim (`BluePaper-host.service`). Not
actionable — unit is N/A.

## Checker output (after)

5 findings remain, all N/A or deferred per the table above
(`requirements.in`, `requirements.lock`, `.pre-commit-config.yaml`,
`deploy.sh`, unit/compose file). Findings 4 and 6 (docs/audits index,
`.gitignore` venv/) are resolved.

## Secrets scan

No inline secrets found in tracked files. `local.properties` (SDK path
only) is untracked and gitignored. No `.env`, no hardcoded tokens, no
keystore material in the repo.
