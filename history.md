# MD Relay History

## 2026-05-15

- Started MD Relay MVP.
- Scope: lightweight Android viewer/relay for `.md`, `.markdown`, `.txt`, and `.json`.
- Core flow: open downloaded file -> preview -> temporary edit -> share/copy current content.
- Important design choice: temporary edit is required for relay-time corrections.
- Important design choice: persistent save/write-back is deferred.
- Important design choice: Mermaid/code readability matters, but full editor features are out of scope.
- Important boundary: this app must not become InkNote.
- Deferred: header folding, full syntax highlighting, diagram export, advanced Markdown plugins, persistent document management.
