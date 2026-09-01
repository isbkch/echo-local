# Third-party notices

Echolocal does not include model weights or voice embeddings in this repository or its source distribution. The app can download compatible assets only after the user explicitly requests it.

| Component | Purpose | License |
| --- | --- | --- |
| [KokoroSwift](https://github.com/mlalma/kokoro-ios) | Native Kokoro runtime | MIT |
| [MLX Swift](https://github.com/ml-explore/mlx-swift) | Apple-silicon ML runtime | MIT |
| [Kokoro 82M safetensors](https://huggingface.co/brannala64/kokoro-82m-safetensors) | Optional model and curated voice embeddings | Apache-2.0 |
| [Soniqo speech-android 0.0.19](https://github.com/soniqo/speech-android/tree/v0.0.19) | Published Android SDK and JNI bridge | Apache-2.0 |
| [Soniqo speech-core](https://github.com/soniqo/speech-core/tree/e5e4adcc908234c02e01d06cb17d82dfa56a55f7) | Native Android speech pipeline | Apache-2.0 |
| [Soniqo Kokoro-82M-ONNX](https://huggingface.co/soniqo/Kokoro-82M-ONNX/tree/2895b2025f1046fad6b51f8773debc3da8ba05df) | Pinned Android model, dictionaries, and selected voice embeddings | Apache-2.0 |
| [ONNX Runtime 1.27.0](https://github.com/microsoft/onnxruntime/tree/v1.27.0) | Native inference runtime bundled by Soniqo | MIT |
| [LiteRT 2.1.5](https://github.com/google-ai-edge/LiteRT/tree/v2.1.5) | Native runtime bundled by Soniqo, with separately noticed permissive components | Apache-2.0 and component licenses |
| [Kotlin and kotlinx.coroutines](https://github.com/JetBrains/kotlin) | Android language runtime and concurrency | Apache-2.0 |
| [AndroidX, Compose, WorkManager, DataStore, and Media3](https://android.googlesource.com/platform/frameworks/support/) | Android UI, persistence, scheduling, and playback | Apache-2.0 |
| [OkHttp and Okio](https://github.com/square/okhttp) | Resumable model download transport | Apache-2.0 |

Each component remains subject to its own license and notices. The Android APK includes readable full texts under **Voice direction → Open-source licenses**, sourced from `android/app/src/main/assets/licenses/`. Review upstream terms before distributing a modified app or separately redistributing model assets.

These permissive licenses allow commercial use when their conditions are followed; they do not imply endorsement by Soniqo or any other upstream project. Existing Echolocal source releases remain under the repository's MIT license. A future proprietary premium edition should keep a clear module or distribution boundary and establish contribution and dual-licensing policy before accepting code that must ship under both terms.
