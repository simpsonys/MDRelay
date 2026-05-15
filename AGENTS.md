# MD Relay Agent Instructions

## Project Identity

MD Relay is a lightweight Android viewer/relay app for AI-generated `.md`, `.txt`, and `.json` files.

It is not InkNote.

Primary workflow:

`open downloaded file -> preview -> temporary edit if needed -> share/copy current content`

The app is for quick viewing, verification, tiny relay-time edits, and sending content to other apps.

It is not a note-management app, wiki app, file manager, or full Markdown authoring environment.

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
