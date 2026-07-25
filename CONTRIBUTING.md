# Contributing

Thanks for considering a contribution to Echolocal.

## Before opening a pull request

- Keep the app local-first: text and generated audio must stay on the Mac unless a user explicitly chooses to download model files.
- Do not add model weights, voice embeddings, generated audio, or build products to the repository.
- Keep the main workflow focused: paste text, tune a voice, generate, play, and export.
- Include tests for changed text processing, audio processing, or file-format behaviour.

## Local development

1. Install Xcode and [XcodeGen](https://github.com/yonaskolb/XcodeGen).
2. Run `xcodegen generate`.
3. Open `EchoLocal.xcodeproj`, or build the release app with `./scripts/build.sh`.
4. Run the test command in the [README](README.md#test).

The inference smoke test is opt-in. Point `LOCAL_AUDIO_SMOKE_MODEL_DIR` at a directory containing `kokoro-v1_0.safetensors` and `af_heart.safetensors` to enable it locally.

## Pull requests

Use a focused branch and describe the user-visible change, the test coverage, and any model or audio-quality trade-offs. Do not commit secrets, signing certificates, provisioning profiles, or private user audio.
