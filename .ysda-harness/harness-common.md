# YSDA Harness Common Core v2.8.5

> Single source of the reusable, project-neutral, provider-neutral rules shared by both YSDA harness
> standards (Init and Existing-Project Adoption). Those standards reference sections here as `Common §Cn`
> and add only their lifecycle-specific content. Edit shared rules here, never in the standards.
> Bootstrap/reference document — not loaded during normal project work (see §C4).

---

## C0. Purpose & core principle

Make AI-assisted projects understandable, reviewable, resumable.

> Do not just build with AI. Build a system where AI work leaves understandable traces — so the owner is
> never forced to choose architecture or implementation options without a readable artifact first.

Standardizing terminology, structure, and workflow makes projects consistent across a portfolio and shareable.

## C1. Project-neutrality

No hardcoded project/product/repo/platform/framework/domain. Use placeholders (`<project-name>`, `<core-feature>`),
conditional examples, reusable categories (external input, private data, sync, export, release). The only
branded constant is the harness's own `ysdadev` tool name and `.ysda-harness/` home.

## C2. Documentation namespace & folder budget

Default doc namespace is `doc/` (not `docs/`; `docs/` only for a hosted site). Recommended top-level:

```text
AGENTS.md  README.md
workflow/   # STATUS, NEXT, ROLES, IO-CONTRACT, artifact-registry, traceability
spec/  arch/  quality/  qa/  tasks/  reports/  doc/  ops/  release/
scripts/         # project-local devtool adapter + helper scripts
.ysda-harness/   # harness snapshot, version, invocation prompts, devtool contract
.agents/         # provider-native skills/workflows
```

Optional: `model/`, `docs/`, `maintenance/`. Avoid other top-level folders unless listed, platform-required,
owner-requested, or separating clearly different artifact types. If both `doc/` and `docs/` exist, pick one
(prefer `doc/`) and migrate gradually.

## C3. `.ysda-harness/` home + snapshot discipline

Canonical source is split for maintainability and **does not keep every historical version as active files**:

```text
standards/
  ysda-harness-common.md
  ysda-project-init-harness-standard.md
  ysda-existing-project-harness-adoption-standard.md
```

Version history is carried by Git commits/tags and `docs/harness-evolution-history.md`, not by accumulating
`*_v1_*`, `*_v2_*`, and zip copies in the active tree. Historical files may be deleted from the working tree
after their useful decisions are captured in ADRs/history and the state is preserved by Git.

Project-local home uses a **two-file frozen snapshot** by default:

```text
.ysda-harness/
  harness-common.md      # frozen copy of canonical Common at adoption/init time
  harness-mode.md        # frozen copy of selected mode adapter: init or existing-adoption
  harness-version.json   # created/updated by the agent: version, mode, source files, checksums/date
  invocation-prompts.md  # short owner prompts for this project/mode
  devtool.json
  devtool.md
  local/ tmp/ run/ cache/ # ignored transient files
```

The owner should not manually maintain `harness-version.json`. During init/adopt/upgrade, the agent must create
or update it. A minimal valid shape:

```json
{
  "harness_version": "2.8.5",
  "applied_mode": "init | adopt | audit | scoped | upgrade | self-hosted",
  "applied_date": "YYYY-MM-DD",
  "canonical_root": "<git url or local sync path>",
  "common_file": ".ysda-harness/harness-common.md",
  "mode_file": ".ysda-harness/harness-mode.md",
  "source_common": "standards/ysda-harness-common.md",
  "source_mode": "standards/<selected-mode-standard>.md",
  "common_checksum": "<sha256>",
  "mode_checksum": "<sha256>",
  "notes": [
    "Project-local snapshot. Update through harness init/adopt/upgrade, not manual edits.",
    "Official Windows command is .\ysdadev.cmd <command>."
  ]
}
```

Discipline:

1. **Single canonical source set.** Shared rules are edited only in Common. Init/adoption hold lifecycle-specific
   rules only.
2. **Two-file project snapshot.** Project-local snapshots are frozen copies. If local edits are needed, record
   them as project-specific docs/ADRs, not as changes to the copied standard.
3. **Checksums make drift visible.** Checksums are optional for tiny experiments but recommended for maintained
   projects.
4. **Daily work does not read the standards** (§C4). Standards are read only at init/adopt/upgrade or explicit
   owner request.
5. **No default flat bundle.** A flat bundle (`Common + mode in one file`) is optional only when a specific AI
   service repeatedly misses multi-file context. Do not introduce a dist/build step by default.

Canonical harness repository exception: the repository that owns these standards may use `standards/` itself as
the source/snapshot and keep only `.ysda-harness/harness-version.json`, `invocation-prompts.md`, and devtool
metadata under `.ysda-harness/`. See §C30.

## C4. Bootstrap vs Runtime

The full standard is a bootstrap reference, not a per-task input — this is what keeps daily token/latency low.
During normal work the agent reads only: `AGENTS.md`, `workflow/STATUS.md`, the active spec/ADR/task, and
task-specific skills when needed. Read the full standard only at init/adopt/upgrade or on explicit request.

## C5. User-facing language

Owner-facing output Korean-first; English for filenames, commands, APIs, identifiers, status tokens
(`Proposed`/`Accepted`/`Rejected`/`Blocked`). `workflow/STATUS.md` is a Korean-first dashboard. Compact output
(§C19) does not mean English.

## C6. Roles & I/O contract

`workflow/ROLES.md` + `workflow/IO-CONTRACT.md`. Roles: Owner (always), Planner, Architect, Implementer,
Evaluator/QA, UX Reviewer (UI work only), Documenter. Each has explicit inputs / outputs / must-not-do
(e.g. Architect must not mark an ADR Accepted without owner approval; Implementer must not change architecture
silently). MultiAgent is optional, not default — use multiple roles only when separation improves quality
(architecture trade-off, security/storage risk, UI review, post-impl evaluation, repeated failure after 2 tries).

## C7. Spec rule

`spec/spec-{nnn}-<specific-title>.md`. Short, concrete, reviewable. Avoid broad files. Includes: 목적, 포함/제외
범위, user-visible behavior, acceptance criteria, related ADRs/tests.

## C8. ADR rule

Two-step: `Proposed → owner review → Accepted/Rejected/Superseded → implementation`. Never ask the owner to
choose without a readable `Proposed` ADR first. Format:

```markdown
## Decision brief
Recommended / Why / Owner action
## Decision drivers
- quality bar / 제약 (quality/quality-bar.md)
## 대안 비교
| 대안 | <driver-1> | <driver-2> | 복잡도 | 리스크 | 비고 |
```

Decide by driver-based comparison, not narrative. Alternatives: min 2, max 3 (hybrid/phased detail goes
inside an option). Tables default; Mermaid when clearer. Not an essay.

## C9. Task rule

`tasks/task-{nnn}-<specific-title>.md`. One task = one reviewable/committable/rollbackable unit. Fields: 목적,
연결(spec/ADR/TC), 작업 내용, Definition of Done, Rollback. Three granularities are distinct:

| Axis | Basis | Frequency |
|---|---|---|
| Task decomposition | 추적·실행·복구 단위 | as small as useful |
| User-visible milestone | 사용자가 체감하는 기능/흐름 | large |
| Owner stop (checkpoint) | risk gate only | rare |

Owner stop only for: data-loss risk, schema/migration, external auth, deploy, real-user-data write/apply,
public release, ADR-needing decision, 2 consecutive failures. Otherwise auto-proceed and update STATUS. No
micro-checkpoints.

## C10. Concept capture

Owner asks a concept/tech question during work → answer in Korean **and** persist to `doc/{title}.md` (specific
title, one topic/file) + register in `artifact-registry.md`. Format: 핵심(2~4줄) / 프로젝트 관련성 / 선택지·trade-off /
참고. Persist only reusable, project-relevant knowledge. Exclude trivial command/syntax, one-off debugging,
generic AI questions, off-project research (use a separate AI chat).

## C11. Q&A logging

`reports/qna-log.md` append-only. Log project-relevant Q&A/rationale that helps future work (architecture
discussion, clarifications, attempt→failure→cause, owner decisions, open assumptions). Promote concept → doc,
decision → ADR, leave a link. ≤ ~5 entries/session. Format `날짜 / Q / A·결론 / 연결`. Don't pollute STATUS.

## C12. ID allocation

Numbered artifacts (spec/adr/task/QS/TC/eval): scan dir, allocate `max+1`. No reuse, renumber, or gap-filling.
On suspected collision, stop and report.

## C13. Quality Bar Lite

`quality/quality-bar.md`, 5–7 bars. Categories: Data Safety, Privacy, Testability, Observability,
Recoverability, Simplicity, Maintainability. For personal/financial/health/credential/customer/private raw
data: Data Safety, Privacy, Testability, Observability mandatory.

Measurable reference (non-blocking; only for ADR-driving bars): Data Safety = write 실패 시 원본 무손실 + 복구 경로;
Privacy = 민감 원문 평문 미잔존; Testability = 외부 의존 없이 핵심 로직 테스트 + 핵심 경로 TC; Observability = 실패 원인 식별 가능,
not silent; Recoverability = 직전 상태 복귀 + rollback 문서화; Maintainability = 다음 session이 STATUS+spec/ADR로 재개.

## C14. Traceability Lite

`workflow/traceability-matrix.md` — every important feature has a decision and a test. Keep short.

| Need/Feature | Spec | ADR | Quality Bar | Test Case | Status |

## C15. Trivial / non-trivial + Fast MVP

Non-trivial if any: (a) persistence/schema, (b) external IO/network/API/webhook/file-message intake,
(c) security/privacy/credential, (d) migration/deletion, (e) ≥2 modules/files, (f) hard-to-reverse. Else
trivial — task only, no separate ADR/final-architecture.

**Fast MVP exception:** for low-blast-radius personal MVPs, don't block the first working version on full
architecture/test artifacts. Minimum: `AGENTS.md`, `STATUS.md`, `quality-bar.md`, one short spec/task,
`manual-test-checklist.md`. Require full final-architecture + test-plan + test-cases before: real-data
migration, destructive write/apply, public release, schema change, multi-device sync, irreversible storage,
credential/auth, or any ADR-level decision. Record the exception + later gate trigger in STATUS.

## C16. Test/eval gate + manual docs

Non-trivial work normally has `qa/test-plan-{nnn}-*.md` + `qa/test-cases-{nnn}-*.md` (Fast MVP may defer per
C15). For external-input projects cover: valid, missing-required, malformed, duplicate, persistence, UI/state,
export/delete/archive, privacy boundary. After impl: `reports/eval/eval-{nnn}-*.md`. Before MVP release:
`qa/manual-test-checklist.md` (prefer `ysdadev` commands), `qa/sample-inputs.md`, `ops/debugging-guide.md`,
`release/release-checklist.md`.

## C17. Implementation Readiness Gate

Before product code, verify present: AGENTS, README, STATUS, active spec, accepted ADRs, final-architecture,
quality-bar, test-plan, test-cases, task. Missing → create the missing review artifact first; stop for owner
review if a decision is needed. Exception: Fast MVP (C15) uses the minimum set and records deferred gates.

## C18. Proactive next-artifact

If the next action is a review artifact (spec/ADR/doc/test plan/final architecture/quality bar/status),
create it — don't merely recommend. But keep ADRs `Proposed`; don't implement before approval; don't mark
`Accepted` without owner approval.

## C19. Compact Output Contract

Default for all agents (override only to diagnose a failure). Omit full diffs/file contents/long logs/unchanged
code/restated project docs/intermediate reasoning. Include: one sentence per change (what/why); validation as
command + PASS/FAIL; on failure, failing command + cause + next action; code refs as path+lines. Passing tasks
≤ 30 lines. Shape: `수정한 파일 / 현재 상태 / 다음 액션`. Don't repeat compliance checklists; mention non-actions only
when owner asked, a stop gate blocked work, or safety/ambiguity requires it.

## C20. Git & safety + external data gate

- No push without explicit owner approval; on remote mismatch, stop and ask.
- No data/migration-path deletion without an ADR. No private raw data in repo docs (redact).
- **External/real user data is not source:** don't copy into repo/commit; tools take the data path as an
  explicit arg (no auto-discovery); audit read-only; migration dry-run first; apply requires backup + reviewed
  clean dry-run + explicit approval phrase + owner's direct execution; write previews to a separate path;
  read-only approval ≠ apply authorization.

## C21. Security Model (conditional — untrusted input / codegen / AI output / plugins)

Path: `intent → typed plan → validation → dry-run preview → confirmation → execution → transaction log`.
External/NL input never directly mutates; generated output is draft only (never auto-installed/executed/
imported); data-only bundles reject executables; plugins enforce declared permissions but loaded code stays
untrusted; secrets from env not repo; don't send local files/trees/secrets by default; AI providers optional,
disabled by default.

## C22. Release separation + Documentation impact (conditional — public distribution)

Two repos: private source (code/tests/prompts/AIHistory/internal docs/full history) vs public release
(artifacts/curated docs/release notes/checksums). Never publish internal detail, prompts/AIHistory, internal
branches, secrets. Public files via allowlist; fine-grained public-scoped tokens, never committed. On
user-facing change, update docs or record `Documentation impact: none` + reason in the commit body.

## C23. README rule

Entry point, not boilerplate. Create/substantially update at implementation readiness (placeholder allowed at
bootstrap / Fast MVP). Content: name + one-line purpose; STATUS link; MVP scope + non-goals; quick
build/run/test commands; `ysdadev` guide link when configured; final-architecture link; documentation map
(relative links); repo structure; data/privacy notes; workflow + harness note; release notes. README is a map,
not a duplicate. Update on readiness, major architecture change, command change, release/workflow change,
folder change — not every tiny change.

## C24. Provider adapters + Code Assist routing

`AGENTS.md` is the runtime rule file (Codex reads it directly). Thin adapters only: `CLAUDE.md`, `GEMINI.md`,
`.cursor/rules/*`, `.agents/skills/*`, `.agents/workflows/*` — point to AGENTS/STATUS, don't duplicate. Route
model/mode by risk: include Mode (local/server), Assistant (Codex: version + reasoning level), Plan-Mode-first
for schema/migration/sync/auth/external-data/broad-refactor/rollback-sensitive, and when-to-switch. Broad
analysis = analysis model read-only; routine impl = coding model medium; risk-adjacent = high reasoning + Plan
Mode first.

## C25. DevTool contract (`ysdadev`)

Mandatory = the **contract** (`.ysda-harness/devtool.json` map + `devtool.md` guide + the `ysdadev` vocabulary
and safety below). Recommended default impl = a Python CLI adapter (`scripts/ysdadev.py`) + thin root wrappers:

```text
ysdadev.cmd     # Windows PowerShell / cmd
ysdadev         # macOS / Linux / WSL
```

A stack where Python is a burden may use a stack-native adapter; the contract is unchanged. **Deferral (Fast
MVP, C15):** defer the façade until ≥2–3 commands are worth wrapping or just after the first working version;
doc/research/design-only projects may limit to `info`/`list`/`doctor` or omit.

Official invocation is project-local and explicit:

```text
Windows PowerShell: .\ysdadev.cmd <command>
cmd.exe:             ysdadev.cmd <command>
macOS/Linux/WSL:     ./ysdadev <command>
```

Bare `ysdadev <command>` is optional owner convenience only. It may be provided by the owner's shell profile
function/alias that delegates to the current directory's project-local wrapper. A global/portfolio-level
`ysdadev` command must not be required, must not shadow project-local wrappers, and should be removed or
renamed if it collides. The harness standard assumes the legacy global/portfolio `ysdadev` has been retired;
portfolio dashboards may exist later under a different name or as a delegator that never overrides local intent.

Principles: standardize the contract, not one global orchestrator. Primary user = owner, not autonomous agent —
agents must not invent commands (only those in `devtool.json` / project-local `ysdadev list` are valid) and
must not run owner-gated commands without explicit owner intent in the current task. In documentation and agent
reports, prefer the official explicit project-local form (`.\ysdadev.cmd ...` / `./ysdadev ...`) to avoid shell
resolution ambiguity.

Vocabulary + gating:

| Command | Meaning | Gated? |
|---|---|---|
| `info` / `list` / `doctor` | metadata / available commands / prereq check | agent-safe |
| `build` / `build <profile>` | build artifact/profile/variant | agent-safe |
| `test unit` / `test smoke` / `test localhost` | tests / local web smoke | agent-safe |
| `clean` | remove build/cache only (no source/data) | agent-safe |
| `test device` | device/instrumented checks | owner-gated |
| `install` / `install <profile>` | install local artifact/profile to device/runtime | owner-gated |
| `run` / `run <profile>` | launch app/process/profile | owner-gated |
| `release prepare` / `release verify` | local release readiness checks; no upload/push | owner-gated unless read-only |

`<profile>` is an optional project-local name such as `debug`, `release`, `web`, `desktop`, `package`, `device`,
or `main`. Expose only profiles that actually work today. A profile may be buildable but not installable; for
example an unsigned Android `release` APK can validate path/build mapping but may fail device install with a
signing error. Treat that as `mapping verified / install blocked by signing`, not as a successful install. If the
owner wants installable release builds, add an explicit signing decision/gate instead of silently using secrets.

push/deploy/migration/real-data: always owner-gated, `requires_owner_execution: true` in `devtool.json`.
`test localhost` manages full lifecycle (check → start server → wait URL → smoke → PASS/FAIL → shutdown unless
`--keep`); never a hanging `npm run dev`. `devtool.json` is a declarative committed map; transient outputs go
to ignored `.ysda-harness/{local,tmp,run,cache}/`. If a configured command fails, fail clearly — don't invent
a fallback. Output follows C19.

## C26. Phase Runner (optional)

Bounded auto-sprint via `workflow/NEXT.md` (current phase + after-success target + "run current only").
Runner: read AGENTS/STATUS/NEXT + active artifacts; execute exactly one phase; advance the marker only on
green; on real failure don't advance and record failure + next action; never chain; never execute the advanced
phase in the same run; on parse failure stop. Ceiling: "one phase, advance only on green, never chain."

## C27. Final architecture

Before implementation (non-trivial, outside Fast MVP), accepted ADRs integrate into
`arch/final-architecture.md`: runtime structure, components, data flow, accepted-ADR summary, implementation
boundaries, explicit non-goals, test hooks, Mermaid when useful.

## C28. Short invocation discipline

Daily prompts are short; detailed behavior lives in the standards/common. Each standard defines its owner
prompts once in its own appendix and mirrors them to `.ysda-harness/invocation-prompts.md` at bootstrap. Do not
duplicate prompt text across sections.


## C29. CI/CD and build artifact retention (conditional — projects that produce distributable artifacts)

Apply when the project produces APKs, installers, wheels, archives, static-site bundles, model files, or other
downloadable build outputs. This rule is project-neutral; Android APKs are one concrete example.

Principle: CI artifacts are temporary diagnostics; release assets are the stable owner/download surface. Do not
let every run accumulate long-lived large artifacts by default.

Default policy:

1. **CI first, release later.** Add CI only after local `ysdadev build` / relevant tests work, or when the owner
   explicitly wants CI as the validation gate.
2. **Short CI artifact retention.** Upload workflow artifacts only when useful for debugging or handoff, and set
   a short `retention-days` value appropriate to the project (personal default: 3–7 days). Do not rely on
   Actions artifacts as long-term storage.
3. **Rolling latest release for owner binaries.** For personal/internal tools where the owner mainly needs the
   newest build, use a rolling release such as `latest`/`nightly`/`dev-latest`: update or replace assets so only
   the current downloadable build is kept. This avoids artifact-storage pressure while keeping the latest build
   available.
4. **Tagged releases for real distribution.** For public/customer/user-facing releases, use versioned tags and
   release notes; do not overwrite historical release assets unless the project explicitly defines a rolling
   channel.
5. **No secrets by default.** CI release publishing must use repository-provided tokens/permissions or explicit
   secrets. Signing keys, keystores, certificates, API keys, and deploy tokens are never committed. Signing and
   public deployment are owner-gated decisions.
6. **Least permissions.** Workflow permissions should be scoped to the needed actions only, for example read
   contents for CI and write contents only when updating releases.
7. **Document the artifact contract.** README/release docs must say where to get the latest build, what is
   temporary, what is permanent, and whether artifacts are debug/unsigned/signed/release-ready.
8. **Wire `ysdadev` to lifecycle profiles.** If CI builds `debug` and `release`, mirror locally where practical:
   `ysdadev build`, `ysdadev build release`, `ysdadev install`, `ysdadev install release` only when each command
   has a real, verified mapping. Unsupported profiles fail clearly.
9. **Retention is a design decision.** If artifacts are large, private, sensitive, legally relevant, or expensive
   to rebuild, record the retention/release policy in an ADR or release checklist. Otherwise document it in
   `release/release-checklist.md` or `ops/release-artifacts.md`.

Validation checklist:

```text
local:   ysdadev list / doctor / build / relevant profile builds
CI:      workflow syntax valid; trigger intentional; cache safe; artifact paths correct
release: rolling asset update verified or intentionally deferred; docs updated
safety:  no secrets committed; push/release owner-approved; retention not excessive
```

---

## C30. Canonical harness repository maintenance (conditional — the `ysda-harness` repo itself)

Apply only to the repository that owns the harness standards.

Principles:

1. **Self-host, but do not duplicate.** The canonical repo may point `.ysda-harness/harness-version.json` directly
   at `standards/ysda-harness-common.md` and the selected mode file instead of copying them again into
   `.ysda-harness/`.
2. **Current files stay versionless.** Active standards use stable names:
   `ysda-harness-common.md`, `ysda-project-init-harness-standard.md`,
   `ysda-existing-project-harness-adoption-standard.md`. Do not keep every old version as active templates.
3. **History lives in Git.** Version changes are captured by commits and optionally tags. Do not retain
   `v1_3`, `v2_4`, `v2_8_4.zip`, or similar historical copies unless there is a specific research/archive reason.
4. **Decision history lives in ADRs.** Preserve important design decisions as ADRs, not by keeping many template
   files. At minimum, capture decisions about Common/mode split, project-local `ysdadev`, two-file snapshot vs
   flat bundle, CI artifact retention, and git-history-over-file-hoarding.
5. **Clean active tree.** A good canonical repo has a small active structure: `standards/`, `docs/` (or `doc/`),
   `prompts/`, `examples/`, `workflow/`, `arch/`, `quality/`, `reports/`, `.ysda-harness/`, and optional
   `scripts/` + project-local `ysdadev`.
6. **Local commits are allowed; push is gated.** The agent may make local commits when the owner explicitly asks
   to organize/version the harness repo. Remote push remains owner-gated (C21).

Suggested local commit flow:

```text
1. If there are valuable uncommitted historical files, create a baseline/local-snapshot commit first.
2. Reorganize to the current minimal structure.
3. Commit: chore: self-apply YSDA Harness v2.8.5
4. Commit later changes as normal version increments, e.g. chore: update harness standard to v2.8.6
```

---

## Changelog

```text
v2.8.5 — simplified snapshot operation and added canonical-repo self-hosting.
  changed C3: default project snapshot is now two files (`harness-common.md` + `harness-mode.md`), not a flat bundle.
  added C3: agent must create/update `harness-version.json`; owner should not maintain it manually.
  changed C3: flat bundle is optional only when a specific AI service needs one; no default dist/build step.
  added C30: canonical `ysda-harness` repo uses versionless active standards, Git history, and ADRs instead of file hoarding.

v2.8.4 — added CI/CD artifact retention and release-artifact policy.
  added C29: project-neutral CI/CD build artifact retention, rolling latest release, signing/secrets and docs rules.
  updated C25: profile/variant command semantics (`build release`, `install release`) and unsigned artifact handling.
  synced Init/Adoption with CI/CD discovery and short invocation guidance.

v2.8.3 — polished common/init/adoption split after project-local ysdadev rollout.
  updated C3: canonical 3-file source set + project-local sealed flat snapshot/checksum discipline.
  updated C25: explicit project-local ysdadev invocation; legacy global/portfolio ysdadev retired to prevent shadowing.
  inherits v2.8.2 common extraction, ysdadev contract+gating, Fast MVP, compact output.

v2.8.2 — extracted shared core from Init and Adoption standards.
  contains all reusable rules (C0–C28). Init/Adoption now reference Common §Cn and hold only lifecycle-specific content.
  includes snapshot discipline (C3), ysdadev contract+gating (C25), Fast MVP (C15), compact output (C19).
```

