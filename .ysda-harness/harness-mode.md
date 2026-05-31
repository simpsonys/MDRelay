# YSDA Existing Project Harness Adoption Standard v2.8.5

> For existing projects — or a slice of a large one. All shared rules live in **YSDA Harness Common Core v2.8.5**
> (`ysda-harness-common.md`), referenced as `Common §Cn`. This file holds only adoption-specific behavior.
> First goal is **visibility, not refactoring.** Primary mode for company work and pre-AGENTS.md projects.
> Bootstrap/reference document — not loaded during normal work (Common §C4).

---

## E1. When to use & main principle

Use to bring `AGENTS.md`/STATUS/ADR/QA/devtool discipline into a repo that already has history, or to analyze
a subsystem of a large codebase. Two situations: you own the whole repo (modes Lite/Standard/Audit/Full, §E3),
or you analyze part of a large project you may not own (mode Scoped, §E3 + §E10).

```text
Understand current state → install lightweight harness (or sidecar)
→ identify risky decisions & legacy debt → mark blocking vs checkpoint → continue with guardrails
```

Anti-pattern (forbidden): scan → find many issues → create many ADRs → block everything. That makes the
harness a burden.

## E2. `.ysda-harness/` for adoption

Apply Common §C3 with `applied_mode` ∈ `adopt`/`audit`/`scoped`/`upgrade`.

Default project-local form:

```text
.ysda-harness/
  harness-common.md      # frozen copy of canonical Common
  harness-mode.md        # frozen copy of this adoption mode
  harness-version.json   # created/updated by the agent, not the owner
  invocation-prompts.md
  devtool.json
  devtool.md
```

Do **not** create or require a flat bundle by default. A flat bundle is optional only when a specific AI service
misses multi-file context. **Scoped mode:** the host repo is often not owned, so place `.ysda-harness/` in the
sidecar bundle (`analysis/<slice>/.ysda-harness/`), never in the host repo root.

## E3. Adoption modes

| Mode | Use when | Required output | Blocking |
|---|---|---|---|
| Lite | small mostly-working tool | AGENTS.md, STATUS.md, doc/current-state.md, .gitignore review | minimal |
| Standard | actively maintained project | Lite + adr-000-baseline, current-architecture, quality-bar, manual-test-checklist, qna-log | current-risk only |
| Audit | messy/risky, not refactoring yet | Standard + legacy-architecture-review, adr-candidates, technical-debt-register, repo-hygiene-report | none by default |
| Scoped | analyze a slice of a large/unowned project | sidecar bundle (§E10); host repo unchanged | none (analysis only) |
| Full | certification / design review / public template | + traceability, final-architecture, eval, release-checklist | explicit owner approval |

Default for owned personal projects: Standard. Unknown structural risk: Audit. Large/company/partial: Scoped.

## E4. Required output (Standard)

```text
AGENTS.md  workflow/STATUS.md  workflow/artifact-registry.md
doc/current-state.md
arch/adr-000-existing-architecture-baseline.md  arch/current-architecture.md
quality/quality-bar.md  qa/manual-test-checklist.md
reports/progress.md  reports/qna-log.md
```

Audit adds: `arch/legacy-architecture-review.md`, `workflow/adr-candidates.md`,
`quality/technical-debt-register.md`, `maintenance/repo-hygiene-report.md`. Don't create
spec/test-cases/traceability/final-architecture unless the next milestone needs them. Apply Common §C7/§C8/§C16
only to **new** work going forward.

## E5. Baseline ADR

`arch/adr-000-existing-architecture-baseline.md` (Status: Accepted). Records what exists, not a new decision:
current language/build/UI/storage/external-deps/deployment/CI-artifact policy; known uncertainty (rationale may be undocumented);
new ADRs only when a change affects architecture/data-safety/deployment/privacy/rollback. Treat current
structure as current truth, not ideal.

## E6. Legacy issue classification

| Type | Blocking? | Where |
|---|---|---|
| Active blocker (breaks build/test/data safety now) | yes | STATUS |
| Missing decision (undocumented architectural choice) | maybe | adr-candidates |
| Structural debt (awkward but working) | no | legacy-architecture-review |
| Technical debt (code/build/test quality) | no | technical-debt-register |
| Repo hygiene (.gitignore/secrets/binaries) | commit gate only | repo-hygiene-report |

Only these block: data-loss risk, privacy/security leak, build/test can't run for the active milestone,
irreversible migration/schema without ADR, push/public-release with unreviewed sensitive files. Everything
else is a checkpoint (§E9), not a gate.

## E7. Hidden decision discovery

The engine for "decisions you didn't know you made." Read-only; discover, don't fix. Lenses: (1) storage/
schema, (2) sync/merge/conflict, (3) encoding/line-ending, (4) error/failure mode, (5) concurrency/session,
(6) auth/secrets, (7) data lifecycle, (8) external dependency assumptions, (9) build/deploy/CI reproducibility,
(10) artifact retention/release channel, (11) privacy boundary, (12) rollback/migration. Found implicit decision → `adr-candidates.md` (decision) or
`technical-debt-register.md` (quality), default Blocking=No. Scoped mode adds boundary lenses: external
contracts/interfaces, ownership, assumptions about the rest of the system, blast radius/coupling.

## E8. ADR candidates + promotion + debt register

`workflow/adr-candidates.md`: max 5 candidates/run, max 3 alternatives each, with Priority + Blocking (default
No) + why-it-matters; extras to a backlog (title only). `ADR-CAND-xxx` is a proposal; on owner approval →
allocate `arch/adr-{nnn}` (Common §C12), `Proposed→Accepted`, adopt the Common §C8 driver-matrix format, link
back. `quality/technical-debt-register.md`: max 10 items/run; don't block on old debt unless it hits the active
milestone; link decision-needing items to adr-candidates. Not a guilt list.

## E9. Checkpoint mechanism

A checkpoint is a deferred decision/debt revisited at a trigger, not now. Stored in adr-candidates /
debt-register; surfaced as a single glance queue in STATUS:

```markdown
## Checkpoints (나중에 검토)
- `ADR-CAND-002` (Medium): … → 트리거: 다음 milestone
- `DEBT-003` (Low): … → 트리거: 해당 영역 수정 시
```

Triggers: next milestone start, before release, when touching the area, owner-initiated (no time-based
nagging). Never blocks the current milestone unless it rises to an active blocker (§E6).

## E10. Scoped mode — large / company slice

Analyze part of a large codebase you may not own/restructure/commit to. Output = a shareable analysis bundle,
not host-repo files.

1. **Scope first** (`scope.md`): the question; in-scope dirs/modules/files; explicit out-of-scope; what you
   will NOT read. Follow only the in-scope dependency graph — never scan the whole monorepo.
2. **Sidecar output** — never inject into host root, never add host top-level folders:
   ```text
   analysis/<slice>/  scope.md  current-architecture.md  adr-candidates.md  risk-register.md  open-questions.md  qna-log.md  .ysda-harness/
   ```
3. **Read-only / no-commit default**; if commit allowed, branch/fork + explicit approval only.
4. **Confidentiality** (Common §C20): don't copy host code out / send externally without approval; redact
   identifiers/secrets in the bundle.
5. **Scoped discovery** (§E7 + boundary lenses).
6. **Shareable deliverable** — self-contained for a colleague; analyze and hand off, don't fix; unknowns →
   `open-questions.md`. Caps still apply (5 candidates, 10 debt).

## E11. ysdadev adoption — map, don't rewrite

Existing projects may already have a devtool (`DevToolAndroid.ps1`, `DevToolFora.ps1`, Gradle/NPM scripts,
Makefile, etc.). Adoption maps it onto the Common §C25 contract; it does not replace working scripts.

```text
1. Detect existing devtool scripts and their commands.
2. .ysda-harness/devtool.json maps ysdadev vocabulary → existing commands (no rewrite of the underlying script).
3. Add thin project-root wrappers: ysdadev.cmd and/or ysdadev, plus scripts/ysdadev.py if useful.
4. Classify each command agent-safe vs owner-gated (Common §C25).
5. devtool.md lists only commands that work today and shows official explicit invocation.
```

Do not install or depend on a global/portfolio `ysdadev`. If a legacy global `ysdadev` exists and shadows the
local wrapper, remove/rename it or document that official commands use `.\ysdadev.cmd` / `./ysdadev` only.
Bare `ysdadev ...` is a local shell convenience, not the project contract.

Scoped mode: expose only read-only verbs (`info`/`list`/`doctor`/`status`); never wire
install/run/device/migration into a repo you don't own.

## E12. current-architecture snapshot

`arch/current-architecture.md` is a snapshot, not a redesign: one-line summary, runtime flow, **sync/main
flows**, key modules/dirs, data flow, build/test/run commands, external deps, assumptions, known risks; Mermaid
when useful. A former `project_map` decomposes here: structure+flows → this file; history → `reports/progress.md`;
status → `workflow/STATUS.md`.

## E13. Decision rule + owner preflight

```text
current milestone can continue safely → continue with AGENTS.md + STATUS.md
else active blocker → resolve first
else legacy debt → record as checkpoint (§E9) and continue
```

Don't pause the project because the audit found old problems. Before running, answer: Project/slice; purpose/
question; Mode; Owned repo? (Scoped: often No); modify code? (No default); change deps? (No); push? (No);
legacy debt blocking? (No unless active risk).


## E14. CI/CD adoption — observe, map, then improve

For existing projects, CI/CD is adopted conservatively. Apply Common §C29, but do not replace working release
flows just because a cleaner workflow is possible.

1. Detect existing workflows under `.github/workflows/`, external CI, release scripts, signing scripts, and
   artifact locations.
2. Record current behavior in `arch/current-architecture.md` or `ops/release-artifacts.md`: triggers, build
   variants, artifact paths, retention, signing status, and release channel.
3. If no CI exists and the project produces downloadable artifacts, propose the smallest useful workflow first:
   build/test + short artifact retention. Add rolling release only when the owner wants latest downloadable
   binaries.
4. For Android-like projects, `debug` and `release` profiles are examples only. Generalize to whatever the
   project actually ships: npm static bundle, desktop installer, Python wheel, CLI archive, model bundle, etc.
5. Do not introduce signing secrets, deploy tokens, store uploads, or public release publication without owner
   approval. Unsigned/temporary artifacts must be labeled as such in README/devtool/release docs.
6. Reflect verified local lifecycle in `ysdadev` (Common §C25): if CI builds a release profile, add local
   `build <profile>`; add `install <profile>` only if the artifact can actually be installed or the failure mode
   is explicitly documented as signing-blocked.

---

## Appendix A. Owner prompts (single source; mirror to `.ysda-harness/invocation-prompts.md`)

### A.1 Adopt — short default
```text
이 기존 프로젝트를 현재 YSDA Harness 기준으로 점검하고 필요한 부분만 보강해줘.
제품 코드 변경 없이 workflow, .ysda-harness, .gitignore, owner-facing ysdadev façade, 필요한 경우 CI/CD artifact retention 정책을 프로젝트 성격에 맞게 준비해줘.
결과는 한국어로 compact하게 요약해줘.
```

### A.2 Adopt — strict / audit mode
```text
이 기존 프로젝트를 현재 YSDA Harness 기준으로 점검하고 필요한 부분만 보강해줘. 제품 코드 수정 금지.
현재 구조를 먼저 이해(§E5/E12)하고, §E7 11개 렌즈로 숨은 결정 발굴 → adr-candidates(최대 5, 기본 Blocking=No).
기존 devtool은 다시 짜지 말고 ysdadev로 매핑(§E11). CI/CD/릴리즈 산출물이 있으면 retention/release 정책은 Common §C29에 맞춰 점검. global ysdadev에 의존하지 말고 project-local wrapper만 사용.
legacy debt는 active blocker가 아니면 Checkpoint(§E9). dependency 추가·migration·push 금지.
결과는 한국어로 compact하게(수정 파일 / 현재 상태 / 발견 문제 / ADR 후보 / Checkpoints / 다음 액션).
```

### A.3 Scoped analysis (large / company slice)
```text
거대 코드베이스의 일부분을 분석한다. host repo는 수정·재구성·push 금지. 결과는 analysis/<slice>/ sidecar로만.
1) scope.md 먼저: 질문, in/out-of-scope, "읽지 않을 것" — monorepo 전체 스캔 금지, in-scope 의존 그래프만.
2) read-only. 기밀: host 코드 외부 반출 금지, 산출물 redact(Common §C20).
3) §E7 렌즈 + 경계 렌즈(contract/ownership/가정/blast radius)로 발굴.
4) 산출물(self-contained): scope / current-architecture(slice) / adr-candidates(≤5) / risk-register(≤10) / open-questions / qna-log.
고치지 말고 분석·핸드오프. 한국어 compact 요약.
```

## Changelog
```text
v2.8.5 — aligned with Common v2.8.5 snapshot simplification.
  changed E2: project-local adoption snapshot is `harness-common.md` + `harness-mode.md`; no default flat bundle.
  clarified E2: agent creates/updates `.ysda-harness/harness-version.json`.
  synced: canonical repo cleanup/version history direction is handled by Common C30.

v2.8.4 — added E14 CI/CD adoption: observe/map/improve existing workflows and artifact retention.
  synced with Common C29 and ysdadev build/install profile handling.

v2.8.2 — extracted shared rules into Common Core; this file holds adoption lifecycle only.
```

