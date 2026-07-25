# Echolocal

**A focused, native macOS app for turning pasted text into natural speech on your own Mac.**

Echolocal runs [KokoroSwift](https://github.com/mlalma/kokoro-ios) on Apple silicon through [MLX Swift](https://github.com/ml-explore/mlx-swift). Once the optional model is installed, synthesis, audio processing, and export happen locally. Your text is never sent to a server.

## Highlights

- Paste or write long-form text in a distraction-free editor.
- Choose from five curated American and British Kokoro voices.
- Tune speaking pace, paragraph breathing, loudness normalization, silence trimming, and autoplay.
- Play, pause, scrub, regenerate, and export standard mono WAV audio.
- Split long passages at sentence boundaries before inference, then join them with natural paragraph spacing.
- Download the model only when you explicitly choose to; work offline afterwards.

## Requirements

- Apple-silicon Mac
- macOS 15 or later
- Xcode 16 or later
- [XcodeGen](https://github.com/yonaskolb/XcodeGen)

## Quick start

```sh
git clone git@github.com:isbkch/EchoLocal.git
cd echo-local
brew install xcodegen
./scripts/build.sh
open ".build/Build/Products/Release/Echolocal.app"
```

`build.sh` produces a self-contained local release build, embeds the required dynamic frameworks, and verifies its signature. It uses ad-hoc signing for local use. Distribute public builds only after signing and notarizing them with your own Apple Developer credentials.

For development, generate and open the project instead:

```sh
xcodegen generate
open EchoLocal.xcodeproj
```

## Install the optional local model

At first launch, choose **Download local model**. The app downloads about 330 MB to:

```text
~/Library/Application Support/Echolocal/Kokoro/
```

You can instead choose **Use existing model files…** and select a folder containing:

```text
kokoro-v1_0.safetensors
af_heart.safetensors
af_bella.safetensors
am_michael.safetensors
bf_emma.safetensors
bm_george.safetensors
```

Model weights and voice embeddings are intentionally excluded from this repository and from source distributions.

## Privacy

The App Sandbox is enabled. Network access is used only for an explicit model download; file access is limited to locations you select through macOS system panels. After the model is installed, generation and WAV export are entirely local.

## Test

```sh
xcodegen generate
xcodebuild \
  -project EchoLocal.xcodeproj \
  -scheme EchoLocal \
  -configuration Debug \
  -derivedDataPath .build \
  test \
  CODE_SIGNING_ALLOWED=NO
```

The suite covers text chunking, audio finishing, waveform reduction, and WAV encoding. The optional inference smoke test runs when `kokoro-v1_0.safetensors` and `af_heart.safetensors` exist in `/tmp/echo-local-smoke-model`, or in the directory named by `LOCAL_AUDIO_SMOKE_MODEL_DIR`.

## License and notices

Echolocal is released under the [MIT License](LICENSE). See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for dependency and model-asset licenses. In particular, compatible Kokoro 82M safetensors are downloaded from [Hugging Face](https://huggingface.co/brannala64/kokoro-82m-safetensors) under Apache-2.0; review upstream terms before redistributing model assets.

## Contributing and security

Contributions are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request. For vulnerabilities, follow [SECURITY.md](SECURITY.md) rather than opening a public issue.
