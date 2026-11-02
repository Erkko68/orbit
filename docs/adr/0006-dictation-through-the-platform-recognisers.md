# ADR 0006: Voice dictation uses each platform's own speech recogniser

- Status: Accepted
- Date: 2026-11-02
- Issue: #73

## Context

The assistant's interview (#72) has a free-text answer, and #73 adds a microphone button that dictates into it. The speech has to become text in the phone's language (Spanish, Catalan or English), on Android and iOS, and the text stays editable until the user sends it.

Two ways to turn speech into text on the phone were on the table:

- The recogniser each platform ships: `SpeechRecognizer` on Android, `SFSpeechRecognizer` on iOS.
- ML Kit's GenAI Speech Recognition API, which runs a speech model on the phone and, on some devices, Gemini Nano.

The assistant itself runs in the cloud (AGENTS.md, "AI"), and that is a separate decision: dictation only fills a text field.

## Decision

**Dictation goes through the platform's own recogniser, behind an `expect`/`actual` in `shared/`: `SpeechRecognizer` on Android, `SFSpeechRecognizer` on iOS.**

- **Both platforms, one API each.** iOS has no ML Kit speech API, so `SFSpeechRecognizer` is needed anyway. Using the platform recogniser on Android too keeps the two sides the same shape.
- **Stable.** Both have been in the platforms for years. AGENTS.md asks for stable dependencies only.
- **Every phone Orbit supports.** It works from Orbit's `minSdk` (24) and needs no particular chipset.
- **Our languages.** Google's recogniser on Android and Apple's on iOS handle Spanish, Catalan and English.
- **No new dependency and no cost.** Nothing to add to `libs.versions.toml`, no server, no billing.

## Alternatives considered

- **ML Kit GenAI Speech Recognition (`com.google.mlkit:genai-speech-recognition`).** Lost because:
  - It is alpha (`1.0.0-alpha1`), with no SLA or deprecation policy; AGENTS.md allows no alpha without team agreement.
  - It is Android only, so iOS would still need `SFSpeechRecognizer`.
  - It needs `minSdk` 26 (Orbit's is 24), its basic mode Android 12 (API 31), and its advanced mode a recent high-end phone (Gemini Nano).
  - Catalan is not among its languages.
  - Its advanced mode runs Gemini Nano on the phone, which AGENTS.md rules out ("No on-device LLM"), even though here it would only transcribe.
- **Sending the audio to a cloud speech API.** Needs a server or a key, costs money, and sends the user's voice off the phone for something the phone already does.

## Consequences

- The `expect`/`actual` follows the other platform access in `shared/` (notifications): an interface in `commonMain`, the actuals in `androidMain` and `iosMain`, registered in `platformModule`, and a fake in `commonTest` for the ViewModel and UI tests.
- Each platform asks for its own permissions: microphone on Android; microphone and speech recognition on iOS, with their texts in `Info.plist`.
- Without a recogniser or a permission, the button says why and typing still works (#73).
- On Android, where the recognition runs (on the phone or Google's servers) is up to the system's recogniser; the app does not choose. Revisit ML Kit when its speech API is stable and covers our languages, as an Android-only improvement behind the same interface.
