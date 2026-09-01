# Echolocal Android Proof APK Design

**Status:** Approved in design review on 2026-09-01  
**Milestone:** Installable proof APK with end-to-end on-device Kokoro synthesis  
**Acceptance devices:** Samsung Galaxy Z Fold 7 and Xiaomi 17 Ultra

## Summary

Add a native Android implementation of Echolocal to this repository. The Android app will preserve the existing paper-and-ink design language and focused text-to-speech workflow while using Android-native technology: Kotlin, Jetpack Compose, Media3, and Soniqo's published Kokoro SDK.

The first milestone is not a Play Store release. It is a debug-signed APK that installs on both acceptance devices and proves the complete local workflow: explicitly download a pinned Kokoro model, generate speech with five curated voices, finish the audio, play and scrub it, and share a valid WAV. After setup, the workflow must survive force-quit and work in Airplane Mode.

The Galaxy Z Fold 7 receives an adaptive interface. Its cover display uses the same compact composition as a conventional phone. Its unfolded display keeps the editor and listening rail primary while placing Voice Direction in a persistent second pane.

## Goals

- Add a self-contained Android Gradle project under `android/` without changing the XcodeGen source of truth or Apple targets.
- Preserve Echolocal's local-first privacy boundary: the network is used only for an explicit model download.
- Match the existing compact product workflow and visual language with Android-native interactions.
- Support the five curated English voices already used by the Apple apps:
  - `af_heart`
  - `af_bella`
  - `am_michael`
  - `bf_emma`
  - `bm_george`
- Preserve paragraph spacing, silence trimming, normalization, autoplay, waveform display, playback, seeking, regeneration, and WAV sharing.
- Keep inference behind an Echolocal-owned interface so the Soniqo dependency can be upgraded, forked, or replaced without changing product UI or state management.
- Produce separate test, build, install, and physical-device evidence.

## Non-goals

- Google Play submission, store listing, Play signing, billing, subscriptions, or premium features.
- Android system-wide `TextToSpeechService` integration.
- Speech recognition, microphone capture, voice agents, or any Soniqo pipeline feature beyond direct TTS.
- Bundling model weights in the APK.
- Multilingual product UI or additional voices.
- Sharing Swift source with Android or converting the Apple app to a cross-platform framework.
- Maintaining synthesis in the background after Android kills the app process.
- A Pace control in the proof APK. Soniqo `0.0.19` has native speed support internally but does not expose it through its public Kotlin/JNI synthesizer API. No resampling substitute will be used because it could change pitch or export behavior.

## Repository and toolchain boundary

The existing Apple project remains unchanged:

```text
EchoLocal/                 SwiftUI application and services
EchoLocalTests/            Apple tests
project.yml                XcodeGen source of truth
```

Android is added as a sibling project:

```text
android/
  settings.gradle.kts
  build.gradle.kts
  gradle.properties
  gradlew
  gradlew.bat
  gradle/wrapper/
  app/
    build.gradle.kts
    src/main/
    src/test/
    src/androidTest/
```

The Android application ID and namespace are `com.isbkch.echolocal`. The project uses Java 17, a minimum SDK of 26, and compile/target SDK 36. Dependency versions are pinned in Gradle rather than resolved from floating ranges. The Soniqo dependency is exactly `audio.soniqo:speech:0.0.19` for this milestone.

The proof artifact is:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

The build flow also emits a SHA-256 file adjacent to the APK. The APK includes `arm64-v8a` for both acceptance devices and `x86_64` for emulator shell testing.

## Android component design

### Presentation

`MainActivity` hosts one Compose application. A lifecycle-aware `EcholocalViewModel` owns the screen state and coordinates repositories and services. Composables render immutable state and send user intents back to the view model.

Suggested package boundaries:

```text
com.isbkch.echolocal
  app/           application state, intents, and EcholocalViewModel
  ui/            screens, responsive layouts, components, and theme
  model/         pinned model manifest, repository, and download worker
  tts/           TtsEngine contract and SoniqoTtsEngine adapter
  audio/         finishing, waveform reduction, WAV encoding, and player
  data/          persisted settings
```

These are package boundaries inside one `:app` module. Separate Gradle modules would add ceremony without improving the proof milestone.

### State ownership

The view model exposes a single `StateFlow<EcholocalUiState>` containing:

- text and derived word/character/duration estimates;
- selected voice and finishing settings;
- model installation state;
- generation state and progress;
- last successfully generated audio metadata;
- playback position and state;
- transient, user-actionable error information.

Voice, paragraph pause, normalization, trimming, and autoplay are persisted with DataStore. Editor text is kept in the saved-state handle so a configuration change or ordinary activity recreation does not clear it. Generated audio lives in the app cache and remains available across layout changes; it is not treated as durable user data.

### TTS boundary

Echolocal owns a small `TtsEngine` contract that accepts text, language, and voice and returns signed little-endian mono PCM16 with its sample rate. It also supports cancellation and close. No Soniqo type escapes this adapter boundary.

`SoniqoTtsEngine`:

- creates `SpeechSynthesizer` with `TtsModel.KOKORO_SHORT_TURN`;
- keeps `useNnapi` disabled for the measured, portable CPU path;
- is created lazily after model readiness is proven;
- caches one synthesizer instance;
- serializes synthesis on a limited-parallelism background dispatcher;
- calls `stop()` when the active generation is cancelled;
- closes and discards the native engine after an inference or out-of-memory failure.

The short-turn graph shares the same weights as the full graph and lets speech-core split and retry long input safely. Echolocal still invokes synthesis per nonblank paragraph so it can insert the user's requested paragraph silence and report honest paragraph-weighted progress. A single long paragraph displays indeterminate progress while its synchronous native call runs.

Every request has a generation ID. Cancelling or regenerating advances the ID, and a late result from an older request is discarded rather than replacing newer state.

## Model installation

Echolocal owns model installation instead of calling Soniqo's floating `ModelManager` directly. This is necessary because the proof app needs four additional curated English voice embeddings and because commercial/reproducible builds should not silently fetch a mutable `main` branch.

The initial manifest pins the Soniqo Kokoro repository revision:

```text
2895b2025f1046fad6b51f8773debc3da8ba05df
```

The manifest contains exactly the files required by the selected short-turn engine:

- `kokoro-e2e-realtime.onnx` and its shared `kokoro-e2e.onnx.data` weights;
- `vocab_index.json`, `us_gold.json`, and `us_silver.json`;
- the English-adjacent dictionaries Soniqo's Kokoro constructor loads (`dict_fr.json`, `dict_es.json`, `dict_it.json`, `dict_pt.json`, and `dict_hi.json`);
- the five curated voice files under `voices/`.

Each entry records its fixed revision URL, byte count, and SHA-256. The UI calculates the download size from this manifest rather than hard-coding marketing copy. The unused full-capacity graph and non-curated voice files are not downloaded for this milestone.

Installation uses app-private storage and a WorkManager job with a network constraint. It follows these rules:

1. The user must explicitly start the model download.
2. Files download to a versioned staging directory.
3. Partial downloads use range requests where supported and remain resumable.
4. Every file must match both expected size and SHA-256.
5. Only a complete verified staging directory is atomically promoted to ready storage.
6. A failed update never removes a previously valid model installation.
7. A manifest-version change determines whether the current installation stays valid.

Download failures preserve validated files and present one clear Retry action. A hash mismatch removes only the bad staging asset. A low-storage error reports the required space without claiming the model is corrupt.

## Audio pipeline and playback

The Soniqo adapter returns 24 kHz mono PCM16. Echolocal's pure Kotlin audio layer then:

1. concatenates paragraph results with the configured silence;
2. trims leading and trailing silence using the behavior ported from the Swift implementation;
3. normalizes peak loudness when enabled, without clipping;
4. reduces samples into waveform buckets;
5. writes a standards-compliant 16-bit mono PCM WAV to a staging cache file;
6. atomically publishes the finished WAV as the new playable artifact.

Media3 owns playback, pause, seek, duration, and position updates. The waveform is a product visualization driven from the finished sample data, while the seek position comes from the player. The last good WAV remains loaded if regeneration fails.

Sharing uses an Android `FileProvider` and `ACTION_SEND` with a temporary read grant. Echolocal requests no broad storage permission. Temporary export files are confined to the cache and can be cleaned on a later launch.

## Responsive interface and visual language

The Android theme maps the existing semantics rather than reproducing Apple controls literally:

- system-aware canvas, paper, raised surface, ink, secondary ink, faint ink, and divider colors;
- fixed Echolocal accent `#CC4F30` and matching soft accent;
- system sans-serif for controls and system serif for script/editor typography;
- restrained capsule actions, fine dividers, generous editor whitespace, and the Echo Local wordmark;
- light and dark system surfaces with sufficient contrast and no dynamic-color replacement of the Echolocal accent.

Compact width devices, including the Xiaomi and Fold cover screen, use:

- the wordmark header and Voice Direction button;
- script header with Paste and Clear;
- the full-height serif editor;
- word, character, and approximate-duration metadata;
- a bottom listening rail with status, waveform, player controls, share, and Generate/Regenerate;
- Voice Direction in a modal bottom sheet.

Expanded width, including the unfolded Fold, uses a two-pane studio:

- the editor, metadata, and listening rail remain in the larger left pane;
- Voice Direction is persistent in the right pane;
- folding or unfolding recomposes the layout without creating a new workflow state.

Voice Direction contains Voice, Delivery, Finish, local-model status, and Open-source licenses. Delivery contains paragraph spacing but not Pace in this milestone. The five voice names and descriptions match the Apple app.

The approximate-duration label uses the Apple app's fixed 155-words-per-minute baseline. It remains deliberately approximate until device measurements justify a platform-specific estimate.

The setup overlay is driven only by model readiness. It shows Missing, Downloading, Verifying, Ready, and Failed states; it cannot remain visible after the repository reaches Ready.

## Runtime and error behavior

### Model states

```text
Missing -> Downloading -> Verifying -> Ready
             |               |
             +---- Failed ---+
                     |
                    Retry
```

Downloads survive activity recreation, fold changes, and backgrounding. If the process is killed, WorkManager resumes eligible work and uses the staging files. Only Ready enables generation.

### Generation states

```text
Idle/AudioReady -> Generating -> Finishing -> AudioReady
                       |            |
                       +-- Failed --+
```

An error is rendered as an actionable message rather than a raw exception. Missing or corrupt model files return the app to setup. Inference failures close the engine and offer Retry. A recoverable allocation failure preserves the last good audio and explains that generation could not be completed; an operating-system process kill follows the process-death behavior below. Cancellation is not shown as failure.

When Android kills the process, incomplete generation and partial WAV output are discarded. Validated models and persisted settings remain available. A previously committed WAV is never replaced by partial output, but cache audio is not promised to survive process death or ordinary Android cache eviction.

## Privacy and permissions

Echolocal explicitly declares only `INTERNET` for the user-started model download. WorkManager may merge its own scheduling permissions from the library manifest; Echolocal does not add runtime permission prompts for them. The app does not request microphone, contacts, broad file storage, accessibility, overlay, notification, or location access.

No analytics, account system, cloud API, or telemetry is introduced. Device proof includes force-quitting after model installation, enabling Airplane Mode, relaunching, and completing generation, playback, and WAV sharing.

## Licensing and future commercial use

Soniqo `speech-android`, `speech-core`, and the selected Kokoro model are Apache-2.0 and permit commercial use. The aggregate Echolocal project can remain MIT-licensed, but third-party components retain their original terms.

The Android app must include a readable Open-source licenses view and update the repository's `THIRD_PARTY_NOTICES.md`. At minimum, notices cover:

- Soniqo `speech-android` and `speech-core` under Apache-2.0;
- Kokoro and Soniqo's ONNX conversion under Apache-2.0;
- ONNX Runtime under MIT;
- LiteRT and its applicable bundled third-party components;
- Kotlin, coroutines, OkHttp, AndroidX, Compose, WorkManager, and Media3.

The published Soniqo AAR does not itself carry the full license documents, so Echolocal owns this packaging step. The app must not imply Soniqo endorsement or affiliation.

This milestone does not decide Echolocal's future premium architecture. Existing MIT releases remain MIT. Proprietary premium features, if desired later, should be kept in a separate module or distribution boundary, and contribution/licensing policy should be established before accepting changes that must be dual-licensed.

## Verification strategy

### Pure Kotlin unit tests

- text/paragraph chunk behavior and pause insertion;
- PCM16 conversion, trimming, normalization, concatenation, and silence;
- waveform reduction;
- WAV header fields, payload length, and sample integrity;
- model-manifest parsing, size/hash validation, and atomic state transitions;
- view-model readiness, failure/retry, cancellation, and stale-generation rejection;
- settings persistence and fallback for unknown voice IDs.

### Compose tests

- setup overlay follows model readiness;
- compact layout opens Voice Direction as a sheet;
- expanded layout renders persistent Voice Direction;
- Generate, Regenerate, playback, and share actions enable only in valid states;
- folding-size recomposition preserves editor and settings state;
- accessibility labels, focus order, and enlarged text remain usable.

### Build and install gates

1. Run pure Kotlin and Robolectric tests with `:app:testDebugUnitTest`.
2. Assemble the debug APK from a clean checkout with `:app:assembleDebug`.
3. Run Compose instrumentation tests with `:app:connectedDebugAndroidTest` on an x86_64 emulator or connected device.
4. Verify the APK and emit its SHA-256.
5. Install through ADB and prove launch on an emulator or connected device.
6. Confirm the merged manifest requests no microphone or broad storage permission.
7. Capture compact and expanded screenshots as layout evidence.

Simulator/emulator success proves only the Android shell. It does not prove on-device inference performance or memory stability.

### Physical-device acceptance

On both the Galaxy Z Fold 7 and Xiaomi 17 Ultra:

- install the same APK;
- explicitly download and verify the model;
- generate short text with each of the five voices;
- generate a representative long-form passage;
- play, pause, scrub, regenerate, and share the WAV;
- verify the shared file is 24 kHz, mono, 16-bit PCM and plays outside Echolocal;
- force-quit, enable Airplane Mode, relaunch, and repeat generation and playback;
- record cold generation time, warm generation time, and peak memory.

Additional Fold checks:

- begin editing and downloading on the cover screen, then unfold without lost state;
- confirm Voice Direction appears as the persistent second pane when expanded;
- fold and unfold during playback without resetting position or generated audio.

The milestone does not invent a pass/fail latency threshold. Measurements establish the baseline for later optimization.

## Definition of done

The proof milestone is complete when:

- one documented command produces the debug APK and checksum;
- automated tests and APK assembly pass;
- the APK installs and launches on both named arm64 devices;
- both compact and adaptive Fold layouts match the approved design language;
- all five curated voices complete the generate-to-share workflow;
- the installed model remains usable after force-quit in Airplane Mode;
- cold/warm time and peak memory are recorded separately for both devices;
- third-party license notices are present in the repository and app;
- Play Store release, billing, and Pace remain clearly documented as later work.
