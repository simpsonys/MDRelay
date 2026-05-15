# MD Relay MVP Submit Notes

## Summary

Implement MD Relay MVP: a lightweight Android viewer/relay app for AI-generated Markdown, text, and JSON files.

## Major Changes

- Added Android app foundation using Kotlin + Jetpack Compose.
- Added file open/share intent handling for `.md`, `.markdown`, `.txt`, and `.json`.
- Added content-based clipboard workflow on file open.
- Added temporary Edit mode for relay-time corrections.
- Added Share and Copy actions based on current edited content.
- Added recent file handling.
- Added Markdown/TXT/JSON viewer support.
- Added lightweight TOC support.
- Added View-only full-screen mode.
- Added Korean filename and Uri handling improvements.
- Fixed Fold/unfold state restore crash.
- Added selectable text support in View mode.
- Added first-pass pipe table rendering.
- Added DevTool workflow for menu, build, install, run, screenshots, prompts, and multi-device testing.

## Validation

- `.\DevToolMDRelay.ps1 build` - success
- `.\DevToolMDRelay.ps1 lint` - success
- `.\DevToolMDRelay.ps1 test` - blocked by Gradle wrapper cache permission error in this sandbox
- `.\DevToolMDRelay.ps1 devices` - success, one connected device found
- `.\DevToolMDRelay.ps1 install-run` - success, built, installed, and launched on the connected device

## Known Limitations

- Persistent save/write-back is deferred.
- Mermaid diagram rendering is deferred; source card/copy only.
- True multiline Markdown table cells are unsupported.
- Escaped pipe handling in tables is unsupported.
- Full syntax highlighting is deferred.
- Header folding is deferred.

## Suggested Commit

`feat: add MD Relay Android MVP`

## Suggested Tag

`v0.1.0`
