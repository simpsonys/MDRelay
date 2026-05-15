# MD Relay

MD Relay is a lightweight Android Markdown, text, and JSON relay viewer. It is designed for quickly opening AI-generated files from Downloads or Files, checking the content, making small temporary edits, then sharing or copying the current raw content.

## Supported Files

- Markdown: `.md`, `.markdown`, `text/markdown`
- Plain text: `.txt`, `text/plain`
- JSON: `.json`, `application/json`
- `application/octet-stream` is accepted as a fallback for downloaded files that lose a specific MIME type.

The app does not request network permission or broad storage permission.

## Toolbar

- `✏` / `👁`: switch between rendered view and raw temporary edit mode
- `📤`: share the current raw content, including temporary edits
- `📋`: copy the current raw content
- `📑`: show or hide the heading table of contents
- `⛶`: enter full-screen view mode
- `⋮`: copy file Uri/path, toggle theme, show recent files, open a file, or see save status

Save/write-back is intentionally deferred in this MVP. Use Share or Copy for relay output.

## Current MVP Limitations

- Real filesystem paths are copied only for `file://` Uris. For Storage Access Framework and Downloads `content://` Uris, the Uri string is copied instead.
- Markdown rendering is lightweight Compose rendering, not a full CommonMark engine.
- TOC jumps to the approximate rendered block.
- Mermaid fenced blocks are detected and displayed as labeled source cards with a `Copy Mermaid` action. Diagram rendering is deferred to keep the app offline and small.
- Edit mode is plain text only and intended for temporary corrections.

## Manual Device Checklist

1. Build and install `app/build/outputs/apk/debug/app-debug.apk`.
2. Open a `.md`, `.txt`, and `.json` file from Files or Downloads.
3. Confirm MD Relay appears as a handler.
4. Confirm the file Uri/path is copied on open.
5. Confirm Markdown preview scrolls and code blocks use monospace styling.
6. Tap the filename to expand/collapse it; long-press to copy the reference.
7. Toggle Edit mode, change text, then use Share and Copy to confirm edited content is used.
8. Use `📑` and tap headings to check approximate jumps.
9. Use `⛶`, then press Back to exit full-screen before leaving the app.
10. Toggle dark/light theme and relaunch to confirm persistence.
11. Open the recent list and reopen a recent file.
12. Rotate or fold/unfold the device and confirm the current content remains visible.
