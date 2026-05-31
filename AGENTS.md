# MD Relay Agent Instructions (Harness v2.8.5 Aligned)

## Project Identity

MD Relay is a lightweight Android viewer/relay app for AI-generated `.md`, `.txt`, and `.json` files.

It is not InkNote.

Primary workflow:

`open downloaded file -> preview -> temporary edit if needed -> share/copy current content`

The app is for quick viewing, verification, tiny relay-time edits, and sending content to other apps.

It is not a note-management app, wiki app, file manager, or full Markdown authoring environment.

---

## Core Priorities

Prioritize in this order:

1. Fast launch
2. Lightweight implementation
3. Correct Android Uri/file handling
4. Reliable file association for `.md`, `.markdown`, `.txt`, and `.json`
5. Smooth viewing and scrolling
6. Temporary text edit for relay-time correction
7. Share/copy current content
8. Markdown readability
9. Mermaid/code readability
10. Fold-friendly responsive layout

---

## Hard Boundaries

Do not add:

- sync
- database
- wiki structure
- folder management
- tags/categories
- backlinks
- persistent note management
- cloud integration
- account/login
- heavy editor framework
- whole-document WebView renderer
- broad storage permission
- IDE-grade syntax highlighting
- complex save/write-back workflow
- release automation beyond simple local helper commands
- Graphify or large architecture documentation

---

## Allowed Viewer Improvements

The following are allowed because they support viewing and relay:

- Markdown preview
- `.txt` plain text view
- `.json` view and optional lightweight pretty display
- table rendering
- link highlight and external link open
- code block readability
- Mermaid block detection
- Mermaid diagram rendering only if lightweight and offline
- Mermaid raw fallback with copy action
- TOC
- full-screen viewer
- temporary edit mode
- share/copy current content
- recent opened files
- compact/Fold responsive layout

---

## Deferred Features

These are deferred unless explicitly requested later:

- persistent save/write-back
- header folding
- full syntax highlighting
- diagram export as PNG/SVG
- advanced Markdown plugins
- visual Mermaid editing
- full text search/indexing
- library management

---

## Font Policy

Do not bundle custom fonts.

Use the user's Android system font setting by default.

Requirements:

- Do not specify a custom font family for normal UI text or Markdown body text.
- Normal Markdown body text should use the device/system default font so Samsung/Galaxy user font settings can be reflected.
- Respect Android font size and accessibility scale as much as practical.
- Use system monospace font only for inline code and code blocks.
- Avoid WebView for normal Markdown rendering.
- WebView is allowed only for isolated Mermaid diagram cards if Mermaid rendering is implemented.
- Do not add custom font files to the APK.

---

## Agent Working Style

Before editing code:

- Read the relevant files first.
- Identify the smallest change that satisfies the request.
- Keep the app lightweight and fast.
- Prefer boring, native Android solutions over clever abstractions.
- Do not expand the product scope without explicit user request.

When implementing:

- Work in small, verifiable steps.
- Preserve existing behavior unless the task requires changing it.
- Avoid large refactors during feature work.
- Do not introduce heavy dependencies without a clear reason.
- If a feature has a lightweight fallback, implement the fallback first.
- Prefer Android-native APIs and Compose basics over large frameworks.
- Do not add broad storage permission to solve Uri/path issues.
- Do not convert the whole Markdown renderer to WebView just to support one feature.

When reporting:

- Do not dump long reasoning.
- Do not request or print detailed chain-of-thought.
- Provide a concise summary:
  - what changed
  - files changed
  - commands run
  - result
  - known limitations
- Be explicit about anything not implemented.

Decision policy:

If there is a trade-off, choose:

- fast launch
- low complexity
- correct Android Uri/file behavior
- reliable share/copy behavior
- viewer readability
- feature richness only after the above are stable

Implementation boundary:

- If a requested feature starts turning MD Relay into InkNote, stop and explain the risk.
- Viewer improvements are allowed.
- Document management features are not allowed.
- Keep the app as a quick viewer/relay utility.

---

## YSDA Harness v2.8.5 Runtime Instructions

### 1. Daily Workspace Protocol
* **Core Rule:** 에이전트의 일일 토큰/컨텍스트 및 지연(latency)을 줄이기 위해, 평상시에는 이 `AGENTS.md`와 `workflow/STATUS.md`, 활성 `tasks/` 파일만 읽고 작업합니다. 공통 규격(`harness-common.md` 및 `harness-mode.md`)은 bootstrap 단계 이후에는 반복 로드하지 않습니다.
* **Korean-First User Language:** 소스코드와 식별자, 명령어 구문 및 상태 토큰(`Proposed`, `Accepted`, `Rejected`, `Blocked`)을 제외한 에이전트 보고 및 대시보드(`workflow/STATUS.md` 등)는 **한국어**로 작성합니다.
* **Compact Output:** 보고 시 Common §C19 규격에 따라 불필요한 추론과 전체 diff 출력을 최소화하고 `수정 파일 / 현재 상태 / 다음 액션` 포맷을 준수하여 간결하게 요약합니다.

### 2. Architecture & Decision Policy
* **Decision Gate (Proposed ADR):** 영구 저장 방식 변경, 마이그레이션, 외부 연동(Intent 수신 규칙 변경, FolderSync 연동 규칙 등)을 다룰 때는 반드시 설계 단계에서 `arch/adr-{nnn}.md`를 **Proposed** 상태로 작성해 Owner의 수락(`Accepted`)을 얻은 후 제품 코드를 수정합니다.
* **External User Data Safety:** 외부 파일 URI 수신 및 FolderSync 가상 인입 시, 사용자 실데이터를 원본 수정/유실 위협으로부터 철저히 고립(Isolate)시킵니다. broad storage permission 획득을 금지하고, 로컬 가상 검증만을 장려합니다.

### 3. Local Façade `ysdadev`
* **Local-Only:** 전역 `ysdadev` 도구에 의존하지 않고 프로젝트 루트에 구성된 로컬 Façade(`.\ysdadev.cmd` 및 `scripts/ysdadev.py`)만 활용하여 빌드 및 디바이스 배포를 제어합니다.
* **Commands Gating:** `build`, `test unit`, `list`, `doctor` 등은 에이전트가 단독 수행 가능하지만, 디바이스 설치(`install`), 앱 구동(`run`), 릴리즈 배포 등 단말기에 영향을 미치는 명령은 Owner가 직접 수행하거나 명시적 수락을 득한 후 실행합니다.
