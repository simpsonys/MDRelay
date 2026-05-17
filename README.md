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

Use **Save to outbox** (⋮ menu) to write a Markdown capture to a local folder. FolderSync uploads that folder to Google Drive. Use Share or Copy for immediate relay output.

## Current Limitations

- Real filesystem paths are copied only for `file://` Uris. For SAF and Downloads `content://` Uris, the Uri string is copied.
- Markdown rendering is lightweight Compose, not a full CommonMark engine.
- TOC jumps to approximate rendered block position.
- Mermaid fenced blocks are displayed as source cards with `Copy Mermaid`. Diagram rendering is deferred to keep the app offline and small.
- Edit mode is plain text only, intended for quick corrections before saving.
- FolderSync raw broadcast trigger is legacy/best-effort. Reliable upload requires FolderSync Instant sync / Monitor device folder watching the outbox.
- Google Drive upload is FolderSync's responsibility, not MDRelay's.

## FolderSync Setup

MDRelay writes Markdown files to a local outbox folder. FolderSync watches that folder and uploads files to Google Drive.

Recommended FolderPair config:

| Setting | Value |
|---|---|
| Name | `YSDAWAY_MDRelay_Outbox_to_Drive` |
| Direction | To remote folder |
| Local | Documents/MDRelay/outbox (or your chosen outbox) |
| Remote | Google Drive/YSDAWAY-LLM-Wiki/inbox/mobile/ |
| Sync deletions | OFF |
| Instant sync / Monitor device folder | **ON** (primary method) |
| Schedule | Optional fallback, e.g. every 15 min |

**Legacy broadcast trigger** (in MDRelay Capture settings): optional, best-effort only. Some Android/FolderSync versions ignore it. The reliable path is FolderSync Instant sync watching the outbox folder.

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
13. Open Capture settings, select an outbox folder, set FolderPair name to `YSDAWAY_MDRelay_Outbox_to_Drive`.
14. Tap `New capture`, enter text, tap `Save to outbox`. Confirm one `.md` file appears in the outbox.
15. Tap `Save to outbox` again. Confirm a second file with no repeated filename fragments appears.
16. Open Chrome, share a URL to MDRelay. Save to outbox. Confirm `## Source` block is present in the output file.
17. Confirm FolderSync (configured with Instant sync) picks up the new files.
