# Echolocal Android Proof APK Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a debug-signed Android APK that reproduces Echolocal's focused local text-to-speech workflow, including an adaptive Galaxy Z Fold layout and verified offline Kokoro inference on the Samsung Galaxy Z Fold 7 and Xiaomi 17 Ultra.

**Architecture:** Add a self-contained Kotlin/Jetpack Compose project under `android/`; leave the XcodeGen Apple project unchanged. Keep all inference behind an Echolocal-owned `TtsEngine`, consume the pinned Soniqo `0.0.19` Maven SDK, install a checksum-pinned model manifest into app-private storage, then finish PCM, publish a WAV, and drive Media3 playback from a lifecycle-aware view model.

**Tech Stack:** Kotlin 2.2.21, Gradle 8.13, Android Gradle Plugin 8.13.2, Java 17, compile/target SDK 36, min SDK 26, Jetpack Compose BOM 2026.08.00, Soniqo speech 0.0.19, coroutines, DataStore, WorkManager, OkHttp, Media3, JUnit, Robolectric, and Compose UI tests.

**Spec:** `docs/superpowers/specs/2026-09-01-android-proof-apk-design.md`

## Global Constraints

- Treat `project.yml` as the Apple project source of truth; do not edit `EchoLocal.xcodeproj/project.pbxproj`.
- Put the Android project under `android/`; use application ID and namespace `com.isbkch.echolocal`.
- Use Java 17, min SDK 26, compile SDK 36, and target SDK 36.
- Pin `audio.soniqo:speech` to exactly `0.0.19`; do not use a dynamic version.
- Use only Soniqo's direct `SpeechSynthesizer`; do not initialize STT, VAD, microphone, voice-agent, or system-TTS features.
- Use `TtsModel.KOKORO_SHORT_TURN`, `useNnapi = false`, and the five approved English voices.
- Do not add the Pace control or a post-processing speed substitute in this milestone.
- Download model assets only after an explicit user action; after installation, generation must work in Airplane Mode.
- Pin the model repository to commit `2895b2025f1046fad6b51f8773debc3da8ba05df` and validate every asset by byte count and SHA-256.
- Do not bundle model weights in Git or the APK.
- Request no microphone, contacts, broad storage, accessibility, overlay, notification, or location permission.
- Preserve the current accent `#CC4F30`, serif editor, five voice names/descriptions, paragraph pause, normalization, trim, autoplay, playback, scrub, and WAV sharing.
- Compact width uses the phone layout and settings sheet; expanded width uses the approved two-pane Fold layout.
- Keep license notices readable in the app and update the root `THIRD_PARTY_NOTICES.md`.
- A build or emulator pass is not inference proof; keep physical-device evidence separate.
- Preserve unrelated files and commits already present in the working tree.

## Planned File Map

### Project and build

- `android/settings.gradle.kts` — plugin/dependency repositories and `:app` inclusion.
- `android/build.gradle.kts` — pinned Android and Kotlin plugins.
- `android/gradle/libs.versions.toml` — pinned dependency catalog.
- `android/gradle/wrapper/gradle-wrapper.properties` and wrapper files — Gradle 8.13.
- `android/gradle.properties` — AndroidX, Kotlin, JVM, and build settings.
- `android/app/build.gradle.kts` — Android app configuration, dependencies, and test setup.
- `android/app/proguard-rules.pro` — Soniqo/Media3 release keep rules retained for later release builds.
- `android/app/src/main/AndroidManifest.xml` — activity, application, network permission, and FileProvider.
- `android/app/src/main/res/drawable/` and `.../mipmap-anydpi-v26/` — Echolocal adaptive launcher artwork.
- `android/app/src/main/res/xml/file_paths.xml` — cache-only sharing path.

### Product code

- `android/app/src/main/kotlin/com/isbkch/echolocal/MainActivity.kt` — Compose host and responsive-width selection.
- `.../EcholocalApplication.kt` — application-level dependency container.
- `.../domain/KokoroVoice.kt` — five-voice catalog.
- `.../domain/SpeechSettings.kt` — persisted finishing settings.
- `.../domain/SpeechSegment.kt` — paragraph synthesis unit.
- `.../domain/ParagraphPlanner.kt` — blank-line cleanup and paragraph pauses.
- `.../domain/TextMetrics.kt` — word/character/estimated-duration values.
- `.../audio/AudioProcessor.kt` — PCM conversion, trim, normalize, fade, and silence.
- `.../audio/WaveformReducer.kt` — bounded waveform envelope.
- `.../audio/WavEncoder.kt` — 16-bit mono PCM WAV encoding.
- `.../audio/GeneratedAudioStore.kt` — staging and atomic cache publication.
- `.../audio/AudioPlayer.kt` — playback interface and snapshot types.
- `.../audio/Media3AudioPlayer.kt` — ExoPlayer adapter.
- `.../sharing/ShareIntentFactory.kt` — cache `content://` WAV share intent.
- `.../model/ModelAsset.kt` — immutable model asset metadata.
- `.../model/KokoroModelManifest.kt` — exact revision, sizes, and hashes.
- `.../model/ModelInstallState.kt` — setup state model.
- `.../model/AssetFetcher.kt` — resumable fetch interface and OkHttp implementation.
- `.../model/ModelInstaller.kt` — staging, validation, and promotion.
- `.../model/KokoroModelDownloadWorker.kt` — WorkManager bridge.
- `.../model/ModelDownloadScheduler.kt` — scheduling boundary.
- `.../model/KokoroModelRepository.kt` — observable readiness and download commands.
- `.../tts/PcmAudio.kt` — synthesizer output value.
- `.../tts/TtsEngine.kt` — Echolocal-owned inference contract.
- `.../tts/SoniqoTtsEngine.kt` — published SDK adapter.
- `.../tts/SpeechGenerator.kt` — paragraph progress, finishing, WAV publication, and metrics.
- `.../data/SettingsRepository.kt` — settings contract.
- `.../data/DataStoreSettingsRepository.kt` — DataStore implementation.
- `.../app/EcholocalUiState.kt` — immutable UI state and generation states.
- `.../app/EcholocalViewModel.kt` — orchestration and user intents.
- `.../ui/EcholocalApp.kt` — root responsive screen.
- `.../ui/CompactEcholocalScreen.kt` — conventional phone/Fold cover layout.
- `.../ui/ExpandedEcholocalScreen.kt` — unfolded Fold layout.
- `.../ui/VoiceDirection.kt` — voices, delivery, finish, model, and licenses.
- `.../ui/ModelSetupOverlay.kt` — model installation flow.
- `.../ui/components/` — wordmark, editor chrome, listening rail, waveform, and button styles.
- `.../ui/theme/` — Echolocal colors, typography, and Material theme.
- `.../ui/licenses/OpenSourceLicensesScreen.kt` — readable bundled notices.

### Verification, documentation, and licensing

- `android/app/src/test/...` — pure Kotlin, Robolectric, repository, view-model, and compact/expanded UI tests.
- `android/app/src/androidTest/...` — on-device Compose and launch tests.
- `android/app/src/main/assets/licenses/` — complete Apache/MIT license texts and consolidated notices.
- `scripts/build-android.sh` — repeatable test/build/checksum command.
- `scripts/profile-android-memory.sh` — safe per-device PSS sampler.
- `README.md` — Android build, install, and acceptance instructions.
- `THIRD_PARTY_NOTICES.md` — Android SDK/runtime/model attributions.
- `docs/android-proof-results.md` — actual device measurements and evidence, created only after both devices are tested.

---

### Task 1: Create the Android project shell

**Files:**
- Create: `android/settings.gradle.kts`
- Create: `android/build.gradle.kts`
- Create: `android/gradle/libs.versions.toml`
- Create: `android/gradle.properties`
- Create: `android/app/build.gradle.kts`
- Create: `android/app/proguard-rules.pro`
- Create: `android/app/src/main/AndroidManifest.xml`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/MainActivity.kt`
- Create: `android/app/src/main/res/drawable/ic_launcher_foreground.xml`
- Create: `android/app/src/main/res/values/ic_launcher_background.xml`
- Create: `android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Create: `android/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- Create: `android/app/src/main/res/values/strings.xml`
- Create: `android/app/src/main/res/values/themes.xml`
- Create: `android/app/src/test/kotlin/com/isbkch/echolocal/AppIdentityTest.kt`

**Interfaces:**
- Consumes: none beyond the global constraints and approved spec.
- Produces: a Gradle 8.13 wrapper, a compilable `:app`, `MainActivity`, and package `com.isbkch.echolocal` for every later task.

- [ ] **Step 1: Create the Gradle configuration and wrapper**

Bootstrap the wrapper in the otherwise empty `android/` directory first so the host Gradle version never evaluates the Android plugin:

```bash
mkdir -p android
cd android
gradle wrapper --gradle-version 8.13 --distribution-type bin
./gradlew --version
cd ..
```

Then use plugin versions `8.13.2` and `2.2.21`, Compose BOM `2026.08.00`, and these stable AndroidX versions in `libs.versions.toml`:

```toml
[versions]
agp = "8.13.2"
kotlin = "2.2.21"
composeBom = "2026.08.00"
activityCompose = "1.13.0"
lifecycle = "2.11.0"
core = "1.19.0"
datastore = "1.2.1"
work = "2.11.2"
media3 = "1.11.0"
coroutines = "1.9.0"
okhttp = "4.12.0"
soniqo = "0.0.19"
robolectric = "4.16.1"
mockk = "1.13.13"

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

After creating the root Gradle files, verify the pinned wrapper again:

```bash
cd android
./gradlew --version
```

Expected: the wrapper reports Gradle 8.13 and uses the installed JDK to compile Java 17 bytecode.

- [ ] **Step 2: Write the failing app identity test**

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppIdentityTest {
    @Test
    fun applicationUsesEcholocalPackage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("com.isbkch.echolocal", context.packageName)
        assertEquals("Echolocal", context.getString(R.string.app_name))
    }
}
```

- [ ] **Step 3: Run the identity test and observe the missing app shell**

Run:

```bash
cd android
./gradlew :app:testDebugUnitTest --tests '*.AppIdentityTest'
```

Expected: FAIL because the manifest/resources/application have not been completed.

- [ ] **Step 4: Add the minimal application and activity**

Configure `compileSdk = 36`, `minSdk = 26`, `targetSdk = 36`, Java/Kotlin target 17, Compose enabled, `buildConfig = true`, `testOptions.unitTests.isIncludeAndroidResources = true`, and `ndk.abiFilters += listOf("arm64-v8a", "x86_64")`. Add version-catalog aliases for the full Tech Stack plus JUnit 4.13.2, AndroidX Test Core/Runner/Rules 1.7.0, AndroidX Test JUnit 1.3.0, Espresso 3.7.0, coroutines-test, WorkManager testing, MockWebServer, Compose UI test JUnit4, and the debug-only Compose test manifest. Declare only `android.permission.INTERNET` explicitly. Add an adaptive launcher icon that uses the Echolocal terracotta `#CC4F30`, the existing local-waveform mark, and no third-party artwork; reference it through `android:icon` and `android:roundIcon`. `MainActivity` should render a temporary `Text("Echolocal")` inside `setContent`; later UI tasks replace it.

- [ ] **Step 5: Prove the test and clean APK build**

Run:

```bash
cd android
./gradlew :app:testDebugUnitTest --tests '*.AppIdentityTest'
./gradlew :app:assembleDebug
test -f app/build/outputs/apk/debug/app-debug.apk
```

Expected: test PASS and APK exists.

- [ ] **Step 6: Commit the Android shell**

```bash
git add android
git commit -m "Add the Echolocal Android application shell"
```

### Task 2: Implement voices, settings, paragraph planning, and text metrics

**Files:**
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/domain/KokoroVoice.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/domain/SpeechSettings.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/domain/SpeechSegment.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/domain/ParagraphPlanner.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/domain/TextMetrics.kt`
- Create: `android/app/src/test/kotlin/com/isbkch/echolocal/domain/DomainBehaviorTest.kt`

**Interfaces:**
- Consumes: Kotlin/JVM project from Task 1.
- Produces: `VoiceCatalog.curated`, `SpeechSettings`, `SpeechSegment`, `ParagraphPlanner.segments(text, paragraphPauseSeconds)`, and `TextMetrics.from(text)`.

- [ ] **Step 1: Write failing domain tests**

```kotlin
class DomainBehaviorTest {
    @Test fun curatedVoicesMatchAppleProduct() {
        assertEquals(
            listOf("af_heart", "af_bella", "am_michael", "bf_emma", "bm_george"),
            VoiceCatalog.curated.map { it.id },
        )
    }

    @Test fun paragraphsReceivePauseExceptFinalSegment() {
        val result = ParagraphPlanner.segments("First.\n\n Second. ", 0.55)
        assertEquals(listOf("First.", "Second."), result.map { it.text })
        assertEquals(listOf(0.55, 0.0), result.map { it.pauseAfterSeconds })
    }

    @Test fun blankTextProducesNoSegments() {
        assertTrue(ParagraphPlanner.segments(" \n \n", 0.42).isEmpty())
    }

    @Test fun metricsUseFixedAppleBaseline() {
        val text = List(155) { "word" }.joinToString(" ")
        val metrics = TextMetrics.from(text)
        assertEquals(155, metrics.wordCount)
        assertEquals(60.0, metrics.estimatedDurationSeconds, 0.001)
    }
}
```

- [ ] **Step 2: Run the tests and verify missing types**

Run:

```bash
cd android
./gradlew :app:testDebugUnitTest --tests '*.DomainBehaviorTest'
```

Expected: FAIL with unresolved `VoiceCatalog`, `ParagraphPlanner`, and `TextMetrics`.

- [ ] **Step 3: Implement the immutable domain types**

Use these signatures and defaults:

```kotlin
data class KokoroVoice(val id: String, val name: String, val character: String, val region: String)

object VoiceCatalog {
    val curated: List<KokoroVoice> = listOf(
        KokoroVoice("af_heart", "Heart", "Balanced and intimate", "American"),
        KokoroVoice("af_bella", "Bella", "Warm and expressive", "American"),
        KokoroVoice("am_michael", "Michael", "Clear and grounded", "American"),
        KokoroVoice("bf_emma", "Emma", "Polished and composed", "British"),
        KokoroVoice("bm_george", "George", "Measured and articulate", "British"),
    )
}

data class SpeechSettings(
    val voiceId: String = "af_heart",
    val paragraphPauseSeconds: Double = 0.42,
    val normalizesAudio: Boolean = true,
    val trimsSilence: Boolean = true,
    val autoPlays: Boolean = true,
)

data class SpeechSegment(val text: String, val pauseAfterSeconds: Double)
data class TextMetrics(val wordCount: Int, val characterCount: Int, val estimatedDurationSeconds: Double)
```

`ParagraphPlanner` splits on newline boundaries, trims whitespace, removes blank segments, and sets the final pause to zero. `TextMetrics` uses whitespace-separated words and a fixed 155 words/minute plus `0.42` seconds for each additional nonblank paragraph only when called with default settings; expose an overload accepting paragraph pause.

- [ ] **Step 4: Run the domain tests**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests '*.DomainBehaviorTest'`

Expected: PASS.

- [ ] **Step 5: Commit the product domain**

```bash
git add android/app/src/main/kotlin/com/isbkch/echolocal/domain android/app/src/test/kotlin/com/isbkch/echolocal/domain
git commit -m "Define the Android speech experience"
```

### Task 3: Port deterministic audio finishing and WAV encoding

**Files:**
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/audio/AudioProcessor.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/audio/WaveformReducer.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/audio/WavEncoder.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/audio/GeneratedAudioStore.kt`
- Create: `android/app/src/test/kotlin/com/isbkch/echolocal/audio/AudioProcessorTest.kt`
- Create: `android/app/src/test/kotlin/com/isbkch/echolocal/audio/WavEncoderTest.kt`
- Create: `android/app/src/test/kotlin/com/isbkch/echolocal/audio/GeneratedAudioStoreTest.kt`

**Interfaces:**
- Consumes: `SpeechSettings` from Task 2.
- Produces: `AudioProcessor.pcm16ToFloat`, `AudioProcessor.finish`, `AudioProcessor.silence`, `WaveformReducer.reduce`, `WavEncoder.encode`, and `GeneratedAudioStore.publish`.

- [ ] **Step 1: Write failing finishing and waveform tests**

```kotlin
class AudioProcessorTest {
    @Test fun normalizationTargetsMinusOneDecibel() {
        val output = AudioProcessor.finish(
            floatArrayOf(0f, 0.1f, -0.2f, 0.05f, 0f),
            sampleRate = 100,
            normalize = true,
            trimSilence = false,
        )
        assertEquals(0.891f, output.maxOf { abs(it) }, 0.002f)
    }

    @Test fun trimmingKeepsTwentyFiveMillisecondsPadding() {
        val input = FloatArray(100) + floatArrayOf(0.2f, 0.4f, 0.2f) + FloatArray(100)
        val output = AudioProcessor.finish(input, 100, normalize = false, trimSilence = true)
        assertTrue(output.size in 4 until input.size)
    }

    @Test fun waveformHasRequestedBoundedBuckets() {
        val input = FloatArray(1_000) { sin(it / 15.0).toFloat() }
        val output = WaveformReducer.reduce(input, 100)
        assertEquals(100, output.size)
        assertTrue(output.all { it in 0f..1f })
    }
}
```

- [ ] **Step 2: Write the failing WAV and atomic-store tests**

```kotlin
class WavEncoderTest {
    @Test fun writesMonoSixteenBitPcmHeader() {
        val data = WavEncoder.encode(floatArrayOf(0f, 0.5f, -0.5f, 1f, -1f), 24_000)
        assertEquals("RIFF", data.copyOfRange(0, 4).decodeToString())
        assertEquals("WAVE", data.copyOfRange(8, 12).decodeToString())
        assertEquals("data", data.copyOfRange(36, 40).decodeToString())
        assertEquals(54, data.size)
    }
}

class GeneratedAudioStoreTest {
    @Test fun publishesOnlyCompletedWav() {
        val root = createTempDir()
        val store = GeneratedAudioStore(root)
        val artifact = store.publish(byteArrayOf(1, 2, 3))
        assertTrue(artifact.exists())
        assertContentEquals(byteArrayOf(1, 2, 3), artifact.readBytes())
        assertTrue(root.walk().none { it.name.endsWith(".staging") })
    }
}
```

- [ ] **Step 3: Run tests and verify missing audio types**

Run:

```bash
cd android
./gradlew :app:testDebugUnitTest --tests '*.audio.*'
```

Expected: FAIL with unresolved audio classes.

- [ ] **Step 4: Port the Swift algorithms exactly**

Use the current Swift constants and order:

```kotlin
private const val TRIM_THRESHOLD = 0.004f
private const val TRIM_PADDING_SECONDS = 0.025
private const val EDGE_FADE_SECONDS = 0.008
private val NORMALIZE_TARGET = 10.0.pow(-1.0 / 20.0).toFloat()
private const val MAX_GAIN = 6f
```

Sanitize non-finite samples, trim, normalize, then fade. Decode Soniqo little-endian PCM16 with `ByteBuffer.order(ByteOrder.LITTLE_ENDIAN)` and scale by `32767f`. Clamp WAV samples to `[-1, 1]`, write the 44-byte RIFF header in little endian, and throw a named `WavTooLargeException` before a `UInt32` length overflow. `GeneratedAudioStore` writes `generated.staging`, fsyncs/closes it, then moves it to a UUID-named `.wav` in the same cache directory.

- [ ] **Step 5: Run audio tests and the Apple regression suite**

Run:

```bash
cd android
./gradlew :app:testDebugUnitTest --tests '*.audio.*'
cd ..
xcodegen generate
TEST_DERIVED_DATA=$(mktemp -d /tmp/echolocal-tests.XXXXXX)
xcodebuild -project EchoLocal.xcodeproj -scheme EchoLocal -configuration Debug \
  -derivedDataPath "$TEST_DERIVED_DATA" test CODE_SIGNING_ALLOWED=NO
```

Expected: Android audio tests PASS; existing Apple tests remain unchanged and PASS.

- [ ] **Step 6: Commit the audio pipeline**

```bash
git add android/app/src/main/kotlin/com/isbkch/echolocal/audio android/app/src/test/kotlin/com/isbkch/echolocal/audio
git commit -m "Bring Echolocal audio finishing to Android"
```

### Task 4: Implement pinned, resumable model installation

**Files:**
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/model/ModelAsset.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/model/KokoroModelManifest.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/model/ModelInstallState.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/model/AssetFetcher.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/model/ModelInstaller.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/model/KokoroModelDownloadWorker.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/model/ModelDownloadScheduler.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/model/KokoroModelRepository.kt`
- Create: `android/app/src/test/kotlin/com/isbkch/echolocal/model/KokoroModelManifestTest.kt`
- Create: `android/app/src/test/kotlin/com/isbkch/echolocal/model/ModelInstallerTest.kt`
- Create: `android/app/src/test/kotlin/com/isbkch/echolocal/model/KokoroModelRepositoryTest.kt`

**Interfaces:**
- Consumes: Android context and WorkManager from Task 1.
- Produces: `KokoroModelManifest.current`, `ModelInstaller.install`, `ModelInstallState`, and `ModelRepository.state/readyDirectory/startDownload/refresh`.

- [ ] **Step 1: Write failing manifest tests with exact production expectations**

```kotlin
class KokoroModelManifestTest {
    @Test fun manifestPinsEveryRequiredAsset() {
        val manifest = KokoroModelManifest.current
        assertEquals("2895b2025f1046fad6b51f8773debc3da8ba05df", manifest.revision)
        assertEquals(15, manifest.assets.size)
        assertEquals(171_185_109L, manifest.totalBytes)
        assertEquals(
            setOf("af_heart.bin", "af_bella.bin", "am_michael.bin", "bf_emma.bin", "bm_george.bin"),
            manifest.assets.filter { it.relativePath.startsWith("voices/") }.map { it.fileName }.toSet(),
        )
        assertTrue(manifest.assets.all { it.sha256.matches(Regex("[0-9a-f]{64}")) })
    }
}
```

- [ ] **Step 2: Define the exact manifest, including verified sizes and hashes**

```kotlin
private const val REVISION = "2895b2025f1046fad6b51f8773debc3da8ba05df"
private val ASSETS = listOf(
    asset("kokoro-e2e-realtime.onnx", 2_880_477, "52e0206f7cfdd2c622dd782de62215cba79057b6a29f02185906b3f497f1978b"),
    asset("kokoro-e2e.onnx.data", 162_095_104, "272bbac32e99c25a7fc59131371e94907b4ecbaa1b4f67a66fcfb2825e6f927e"),
    asset("vocab_index.json", 2_501, "d392acfec384f050de86c7ac4ab56833cf56a6268315f4d6aeb350e9627404b8"),
    asset("us_gold.json", 3_000_469, "dc414872a49a28ae6c141463d502fd945f3b2fde040484fdc47d00cc4612686f"),
    asset("us_silver.json", 3_099_517, "de8f67be911bb6c659187b4a65fd966b6a30e56350e0f790d763210b053ac475"),
    asset("dict_fr.json", 51_497, "445e45ae84d8a779d22b4df3b90e169ee6e737856765a812abfbbfddd08b4488"),
    asset("dict_es.json", 5_077, "0011eaaccbffb825d6d3c48543ee176053ee224036e09873170c0b89be2faea4"),
    asset("dict_it.json", 3_926, "9e920c5e24b9aea14642b0b53e2788c69c0f36b492f408bcbe586233dfb3a316"),
    asset("dict_pt.json", 37_438, "8c4f5ac84fbf822e8558ac28c4291dbceb754aadc0fad3f6124b528512129ea1"),
    asset("dict_hi.json", 3_983, "6cb736986de8966f87779e9d89438f07a9c0c935a4a975be080b87a251d37a6f"),
    asset("voices/af_heart.bin", 1_024, "3bc1d7444ad012b5c78f3fe7546e7bfcc505a487debf24c799cf97ecf34dc191"),
    asset("voices/af_bella.bin", 1_024, "64e11e8e3f5aa126915f109cecaef1fc9e2e3abd9ae545cd395eec2774509ef7"),
    asset("voices/am_michael.bin", 1_024, "f7bbad342014cea4183055d31b08912bc45042673b4c478d87f00421f3534883"),
    asset("voices/bf_emma.bin", 1_024, "857b4c91068f78bfd6ef609953ee9c09e2ba35f09589d961efd55b2895f6372c"),
    asset("voices/bm_george.bin", 1_024, "3644b5ee56b6ee914dde17e9e8662ddc486fa23efb83773be135640b88d9820f"),
)
```

Build every URL as `https://huggingface.co/soniqo/Kokoro-82M-ONNX/resolve/$REVISION/$relativePath`.

- [ ] **Step 3: Write failing installer recovery tests**

Use an in-memory/fake `AssetFetcher` and temporary directory to assert:

```kotlin
@Test fun invalidHashNeverReplacesReadyModel() = runTest {
    val oldReady = createValidReadyDirectory(root, oldManifest)
    val installer = ModelInstaller(root, corruptingFetcher, newManifest)
    assertFailsWith<ModelIntegrityException> { installer.install {} }
    assertTrue(oldReady.resolve("version.txt").exists())
    assertFalse(root.resolve("ready").resolve("corrupt.bin").exists())
}

@Test fun validStagingIsPromotedAtomically() = runTest {
    val installer = ModelInstaller(root, validFetcher, tinyManifest)
    val ready = installer.install {}
    assertTrue(ready.resolve("voices/af_heart.bin").exists())
    assertFalse(root.resolve("staging-v${tinyManifest.version}").exists())
}
```

- [ ] **Step 4: Run model tests and verify missing installer behavior**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests '*.model.*'`

Expected: manifest test passes only after Step 2; installer/repository tests FAIL until implementation.

- [ ] **Step 5: Implement downloader, installer, Worker, and observable repository**

Use these public contracts:

```kotlin
sealed interface ModelInstallState {
    data object Checking : ModelInstallState
    data object Missing : ModelInstallState
    data class Downloading(val completedBytes: Long, val totalBytes: Long, val fileName: String) : ModelInstallState
    data object Verifying : ModelInstallState
    data class Ready(val directory: File) : ModelInstallState
    data class Failed(val message: String) : ModelInstallState
}

interface ModelRepository {
    val state: StateFlow<ModelInstallState>
    fun refresh()
    fun startDownload()
    fun readyDirectory(): File?
}
```

`OkHttpAssetFetcher` must resume from an existing `.part` length only when the server returns `206`; if it returns `200`, truncate and restart. `ModelInstaller` validates existing ready assets first, downloads into `staging-v1`, validates byte count and SHA-256 while streaming, writes `version.txt`, then uses same-filesystem directory moves with a backup/restore path. The Worker reports byte progress through WorkManager. The repository maps WorkInfo into `ModelInstallState` and calls `refresh()` on construction.

Before enqueueing, compare the manifest's remaining bytes plus a 32 MiB staging margin with the app files directory's `usableSpace`; surface `InsufficientStorageException(requiredBytes, availableBytes)` as an actionable Failed message without calling the network. A size or hash mismatch removes only that asset's staging file, never a previously validated ready directory or other valid staging assets. Retry reuses every staged asset that still passes size and SHA-256 validation.

- [ ] **Step 6: Run model tests and verify no live download occurs**

Run:

```bash
cd android
./gradlew :app:testDebugUnitTest --tests '*.model.*'
```

Expected: PASS using only fake/tiny assets; no 163 MiB network fetch in unit tests.

- [ ] **Step 7: Commit pinned installation**

```bash
git add android/app/src/main/kotlin/com/isbkch/echolocal/model android/app/src/test/kotlin/com/isbkch/echolocal/model
git commit -m "Make Android model setup explicit and reproducible"
```

### Task 5: Adapt Soniqo and implement generation orchestration

**Files:**
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/tts/PcmAudio.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/tts/TtsEngine.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/tts/SoniqoTtsEngine.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/tts/SpeechGenerator.kt`
- Create: `android/app/src/test/kotlin/com/isbkch/echolocal/tts/SoniqoTtsEngineTest.kt`
- Create: `android/app/src/test/kotlin/com/isbkch/echolocal/tts/SpeechGeneratorTest.kt`

**Interfaces:**
- Consumes: `SpeechSegment`, `SpeechSettings`, audio utilities, `GeneratedAudioStore`, and a ready model directory.
- Produces: `TtsEngine`, `TtsEngineFactory`, `SoniqoTtsEngine`, `GenerationRequest`, `GenerationResult`, and `SpeechGenerator.generate/cancel`.

- [ ] **Step 1: Write failing adapter tests without loading native code**

Inject the public Soniqo `SpeechSynthesizer` interface:

```kotlin
@Test fun adapterDecodesLittleEndianPcmAndForwardsVoice() = runTest {
    val synth = RecordingSynthesizer(
        SpeechSynthesisResult(24_000, byteArrayOf(0x00, 0x40, 0x00, 0xC0.toByte())),
    )
    val engine = SoniqoTtsEngine(synth, StandardTestDispatcher(testScheduler))
    val result = engine.synthesize("Hello", "en", "bf_emma")
    assertContentEquals(shortArrayOf(16_384, -16_384), result.samples)
    assertEquals("bf_emma", synth.lastVoice)
}

@Test fun cancellationCallsPublishedStopApi() {
    val synth = RecordingSynthesizer(SpeechSynthesisResult(24_000, byteArrayOf()))
    SoniqoTtsEngine(synth, Dispatchers.Unconfined).cancel()
    assertTrue(synth.stopCalled)
}
```

- [ ] **Step 2: Write failing multi-paragraph generation tests**

```kotlin
@Test fun generationAddsParagraphSilenceAndPublishesWav() = runTest {
    val engine = FakeTtsEngine(samplesPerCall = shortArrayOf(3_000, -3_000), sampleRate = 100)
    val generator = SpeechGenerator(TtsEngineFactory { engine }, GeneratedAudioStore(cacheDir))
    val progress = mutableListOf<Double>()
    val result = generator.generate(
        GenerationRequest("First.\n\nSecond.", SpeechSettings(paragraphPauseSeconds = 0.5)),
        modelDirectory = readyModelDirectory,
        progress::add,
    )
    assertEquals(2, engine.requests.size)
    assertTrue(result.samples.size >= 54)
    assertEquals(1.0, progress.last(), 0.001)
    assertTrue(result.wavFile.exists())
}
```

Also test blank text, unknown voice fallback at the caller boundary, engine failure preserving the prior store artifact, and safe metrics that never log source text.

- [ ] **Step 3: Run the TTS tests and verify missing contracts**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests '*.tts.*'`

Expected: FAIL with unresolved TTS types.

- [ ] **Step 4: Implement the TTS engine boundary**

```kotlin
data class PcmAudio(val samples: ShortArray, val sampleRate: Int)

interface TtsEngine : AutoCloseable {
    suspend fun synthesize(text: String, language: String, voiceId: String): PcmAudio
    fun cancel()
}

fun interface TtsEngineFactory {
    fun create(modelDirectory: File): TtsEngine
}

data class GenerationRequest(val text: String, val settings: SpeechSettings)
data class GenerationResult(
    val samples: FloatArray,
    val sampleRate: Int,
    val waveform: FloatArray,
    val wavFile: File,
    val elapsedMillis: Long,
)
```

The production `TtsEngineFactory` construction must be exactly:

```kotlin
SpeechSynthesizer(
    SpeechSynthesizerConfig(
        modelDir = readyDirectory.absolutePath,
        useNnapi = false,
        ttsModel = TtsModel.KOKORO_SHORT_TURN,
    ),
)
```

Do not add Pace. `SoniqoTtsEngine` runs the blocking SDK call on `Dispatchers.Default.limitedParallelism(1)`. `SpeechGenerator` accepts the factory, creates the native engine lazily only after a verified ready directory is supplied to `generate`, caches one engine for that exact model directory, and serializes generation with a `Mutex`. It uses `ParagraphPlanner`, calls the engine once per segment, converts PCM16 to float, appends configured silence, performs final trim/normalization/fade once, reduces the waveform, encodes the WAV, and publishes it atomically. `cancel()` calls the cached engine's `cancel`; any inference or allocation failure closes and discards that engine so Retry starts fresh. Progress is completed source characters divided by total segment characters. Log only elapsed milliseconds, sample count, and voice ID under tag `EcholocalMetrics`.

- [ ] **Step 5: Run TTS tests and assemble against Maven Central**

Run:

```bash
cd android
./gradlew :app:testDebugUnitTest --tests '*.tts.*'
./gradlew :app:assembleDebug
```

Expected: tests PASS and Gradle resolves `audio.soniqo:speech:0.0.19` from Maven Central.

- [ ] **Step 6: Commit generation**

```bash
git add android/app/src/main/kotlin/com/isbkch/echolocal/tts android/app/src/test/kotlin/com/isbkch/echolocal/tts android/app/build.gradle.kts android/gradle/libs.versions.toml
git commit -m "Generate finished Kokoro speech on Android"
```

### Task 6: Add playback and secure WAV sharing

**Files:**
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/audio/AudioPlayer.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/audio/Media3AudioPlayer.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/sharing/ShareIntentFactory.kt`
- Create: `android/app/src/main/res/xml/file_paths.xml`
- Modify: `android/app/src/main/AndroidManifest.xml`
- Create: `android/app/src/test/kotlin/com/isbkch/echolocal/sharing/ShareIntentFactoryTest.kt`
- Create: `android/app/src/androidTest/kotlin/com/isbkch/echolocal/audio/Media3AudioPlayerTest.kt`

**Interfaces:**
- Consumes: generated WAV from Task 5.
- Produces: `AudioPlayer`, `PlaybackSnapshot`, `Media3AudioPlayer`, and `ShareIntentFactory.create`.

- [ ] **Step 1: Write the failing share-intent test**

```kotlin
@RunWith(RobolectricTestRunner::class)
class ShareIntentFactoryTest {
    @Test fun sharesOnlyCacheWavWithTemporaryReadPermission() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val wav = File(context.cacheDir, "generated/test.wav").apply {
            parentFile!!.mkdirs()
            writeBytes(byteArrayOf(1, 2, 3))
        }
        val intent = ShareIntentFactory.create(context, wav)
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("audio/wav", intent.type)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals("content", intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.scheme)
    }
}
```

- [ ] **Step 2: Run the share test and verify missing FileProvider configuration**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests '*.ShareIntentFactoryTest'`

Expected: FAIL because `ShareIntentFactory` and the provider are missing.

- [ ] **Step 3: Implement player and sharing contracts**

```kotlin
enum class PlaybackStatus { Empty, Ready, Playing, Paused }
data class PlaybackSnapshot(
    val status: PlaybackStatus = PlaybackStatus.Empty,
    val positionMillis: Long = 0,
    val durationMillis: Long = 0,
    val errorMessage: String? = null,
)

interface AudioPlayer : AutoCloseable {
    val snapshot: StateFlow<PlaybackSnapshot>
    fun load(file: File)
    fun playPause()
    fun seekTo(positionMillis: Long)
    fun stop()
}
```

`Media3AudioPlayer` owns one ExoPlayer, publishes a snapshot, clamps seeking, returns to Ready at end, and samples playback position every 50 ms only while playing. The FileProvider authority is `${applicationId}.files`; expose only `<cache-path name="generated_audio" path="generated/"/>` and set `exported="false"`, `grantUriPermissions="true"`.

- [ ] **Step 4: Add an instrumented playback contract test**

Create a 100 ms WAV in cache, load it, assert duration becomes nonzero, exercise `playPause()` and `seekTo()`, then release. Do not require audible output or a model.

- [ ] **Step 5: Run share tests and compile device tests**

Run:

```bash
cd android
./gradlew :app:testDebugUnitTest --tests '*.ShareIntentFactoryTest'
./gradlew :app:assembleDebugAndroidTest
```

Expected: share test PASS and Android test APK compiles.

- [ ] **Step 6: Commit playback and sharing**

```bash
git add android/app/src/main/kotlin/com/isbkch/echolocal/audio/AudioPlayer.kt android/app/src/main/kotlin/com/isbkch/echolocal/audio/Media3AudioPlayer.kt android/app/src/main/kotlin/com/isbkch/echolocal/sharing/ShareIntentFactory.kt android/app/src/main/AndroidManifest.xml android/app/src/main/res/xml/file_paths.xml android/app/src/test/kotlin/com/isbkch/echolocal/sharing/ShareIntentFactoryTest.kt android/app/src/androidTest/kotlin/com/isbkch/echolocal/audio/Media3AudioPlayerTest.kt
git commit -m "Let Android users play and share local speech"
```

### Task 7: Implement persisted settings and the application coordinator

**Files:**
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/data/SettingsRepository.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/data/DataStoreSettingsRepository.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/app/EcholocalUiState.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/app/EcholocalViewModel.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/EcholocalApplication.kt`
- Modify: `android/app/src/main/AndroidManifest.xml`
- Create: `android/app/src/test/kotlin/com/isbkch/echolocal/data/DataStoreSettingsRepositoryTest.kt`
- Create: `android/app/src/test/kotlin/com/isbkch/echolocal/app/EcholocalViewModelTest.kt`

**Interfaces:**
- Consumes: `ModelRepository`, `SpeechGenerator`, `AudioPlayer`, `ShareIntentFactory`, `SpeechSettings`, and `TextMetrics`.
- Produces: `SettingsRepository`, `EcholocalUiState`, `EcholocalEvent`, and all product intent methods on `EcholocalViewModel`.

- [ ] **Step 1: Write failing settings tests**

Assert the default settings match Task 2; a saved voice round-trips; and an unknown stored voice falls back to `af_heart` without deleting the other settings.

```kotlin
@Test fun unknownVoiceFallsBackToHeart() = runTest {
    preferences.edit { it[stringPreferencesKey("voice")] = "unknown" }
    assertEquals("af_heart", repository.settings.first().voiceId)
}
```

- [ ] **Step 2: Write failing view-model state tests**

```kotlin
@Test fun generateIsEnabledOnlyForReadyModelAndNonblankText() = runTest {
    modelState.value = ModelInstallState.Ready(modelDir)
    viewModel.onTextChanged("Hello")
    advanceUntilIdle()
    assertTrue(viewModel.state.value.canGenerate)
    viewModel.onTextChanged("  ")
    assertFalse(viewModel.state.value.canGenerate)
}

@Test fun failedRegenerationKeepsLastGoodAudio() = runTest {
    generator.completeNext(successResult)
    viewModel.onGenerate()
    advanceUntilIdle()
    generator.completeNext(Result.failure(IllegalStateException("boom")))
    viewModel.onGenerate()
    advanceUntilIdle()
    assertEquals(successResult.wavFile, viewModel.state.value.audio?.wavFile)
    assertNotNull(viewModel.state.value.errorMessage)
}

@Test fun staleGenerationCannotReplaceNewerResult() = runTest {
    viewModel.onGenerate()
    viewModel.onTextChanged("Newer text")
    viewModel.onGenerate()
    generator.completeFirst(oldResult)
    generator.completeSecond(newResult)
    advanceUntilIdle()
    assertEquals(newResult.wavFile, viewModel.state.value.audio?.wavFile)
}

@Test fun cancellationStopsEngineWithoutBecomingFailure() = runTest {
    viewModel.onGenerate()
    viewModel.onCancelGeneration()
    advanceUntilIdle()
    assertTrue(generator.cancelCalled)
    assertIs<GenerationState.Idle>(viewModel.state.value.generationState)
    assertNull(viewModel.state.value.errorMessage)
}
```

- [ ] **Step 3: Run focused tests and verify missing coordinator**

Run:

```bash
cd android
./gradlew :app:testDebugUnitTest --tests '*.DataStoreSettingsRepositoryTest' --tests '*.EcholocalViewModelTest'
```

Expected: FAIL with missing repositories/state/view model.

- [ ] **Step 4: Implement settings and explicit UI state**

Use these state shapes:

```kotlin
sealed interface GenerationState {
    data object Idle : GenerationState
    data class Generating(val progress: Double?) : GenerationState
    data object Finishing : GenerationState
    data class Ready(val durationMillis: Long, val elapsedMillis: Long) : GenerationState
    data class Failed(val message: String) : GenerationState
}

data class GeneratedAudioUi(
    val wavFile: File,
    val waveform: FloatArray,
    val durationMillis: Long,
)

data class EcholocalUiState(
    val text: String = "",
    val settings: SpeechSettings = SpeechSettings(),
    val metrics: TextMetrics = TextMetrics.from(""),
    val modelState: ModelInstallState = ModelInstallState.Checking,
    val generationState: GenerationState = GenerationState.Idle,
    val audio: GeneratedAudioUi? = null,
    val playback: PlaybackSnapshot = PlaybackSnapshot(),
    val errorMessage: String? = null,
) {
    val canGenerate: Boolean get() =
        modelState is ModelInstallState.Ready &&
            text.isNotBlank() &&
            generationState !is GenerationState.Generating &&
            generationState !is GenerationState.Finishing
}
```

Expose `onTextChanged`, `onPaste`, `onClear`, `onSelectVoice`, `onParagraphPauseChanged`, `onNormalizeChanged`, `onTrimChanged`, `onAutoplayChanged`, `onDownloadModel`, `onGenerate`, `onCancelGeneration`, `onPlayPause`, `onSeek`, `onShare`, and `dismissError`. Starting a new generation or calling `onCancelGeneration` advances the generation ID, cancels its coroutine, and invokes `TtsEngine.cancel`; late completions must fail the current-ID check before publishing state. Use `SavedStateHandle` for editor text. `EcholocalApplication` constructs production repositories/services and supplies a factory; no service locator should be read directly from composables.

- [ ] **Step 5: Run coordinator tests**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests '*.DataStoreSettingsRepositoryTest' --tests '*.EcholocalViewModelTest'`

Expected: PASS, including cancellation/stale-result behavior under `StandardTestDispatcher`.

- [ ] **Step 6: Commit application state**

```bash
git add android/app/src/main/kotlin/com/isbkch/echolocal/data android/app/src/main/kotlin/com/isbkch/echolocal/app android/app/src/main/kotlin/com/isbkch/echolocal/EcholocalApplication.kt android/app/src/main/AndroidManifest.xml android/app/src/test/kotlin/com/isbkch/echolocal/data/DataStoreSettingsRepositoryTest.kt android/app/src/test/kotlin/com/isbkch/echolocal/app/EcholocalViewModelTest.kt
git commit -m "Keep Android generation state safe across the app"
```

### Task 8: Build the compact Echolocal interface

**Files:**
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/ui/theme/EcholocalColors.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/ui/theme/EcholocalTypography.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/ui/theme/EcholocalTheme.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/ui/EcholocalApp.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/ui/CompactEcholocalScreen.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/ui/VoiceDirection.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/ui/ModelSetupOverlay.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/ui/components/EcholocalWordmark.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/ui/components/ScriptEditor.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/ui/components/ListeningRail.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/ui/components/Waveform.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/ui/components/EcholocalButtons.kt`
- Modify: `android/app/src/main/kotlin/com/isbkch/echolocal/MainActivity.kt`
- Create: `android/app/src/test/kotlin/com/isbkch/echolocal/ui/CompactEcholocalScreenTest.kt`

**Interfaces:**
- Consumes: immutable `EcholocalUiState` and callbacks from Task 7.
- Produces: `EcholocalApp`, `CompactEcholocalScreen`, Voice Direction sheet, setup overlay, and reusable UI components.

- [ ] **Step 1: Write failing Robolectric Compose tests**

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CompactEcholocalScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun missingModelBlocksEditorWithExplicitSetup() {
        compose.setContent {
            EcholocalTheme {
                EcholocalApp(state = testState(modelState = ModelInstallState.Missing), actions = noOpActions, expanded = false)
            }
        }
        compose.onNodeWithText("Private speech,\non this device.").assertIsDisplayed()
        compose.onNodeWithText("Download local model").assertHasClickAction()
    }

    @Test fun compactScreenShowsEditorAndListeningRailWhenReady() {
        compose.setContent {
            EcholocalTheme {
                EcholocalApp(state = readyState(text = "A page worth hearing."), actions = noOpActions, expanded = false)
            }
        }
        compose.onNodeWithText("SCRIPT").assertIsDisplayed()
        compose.onNodeWithContentDescription("Text to turn into speech").assertIsDisplayed()
        compose.onNodeWithText("Generate").assertIsEnabled()
        compose.onNodeWithText("On-device").assertIsDisplayed()
    }
}
```

Add tests that open Voice Direction, select a voice, confirm no Pace text exists, and verify paragraph pause plus all three Finish toggles.

- [ ] **Step 2: Run compact UI tests and verify missing composables**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests '*.CompactEcholocalScreenTest'`

Expected: FAIL with unresolved theme and screen types.

- [ ] **Step 3: Implement the approved visual system**

Map the current Swift theme to Compose:

```kotlin
val EcholocalAccent = Color(0xFFCC4F30)
val EcholocalAccentSoftLight = Color(0x1FCC4F30)
val EcholocalSuccess = Color(0xFF2E8563)
```

Use Material system background/surface/on-surface values for paper and ink in light/dark themes, but never replace the accent with dynamic color. Use `FontFamily.Serif` for editor/wordmark Local and `FontFamily.SansSerif` for controls. Keep the editor dominant, dividers thin, buttons capsule-shaped, and touch targets at least 48 dp even where their visual shape is smaller.

`ModelSetupOverlay` calculates download MiB from `KokoroModelManifest.current.totalBytes`, shows progress/state, and is present whenever state is not Ready. Compact Voice Direction uses `ModalBottomSheet`. Clipboard paste uses `LocalClipboardManager`; share uses the view-model event.

- [ ] **Step 4: Implement waveform and listening rail semantics**

Port the visual shaping exponent `0.72`, minimum amplitude `0.035`, and played-region clipping from `WaveformView.swift`. Hide the canvas from accessibility and expose a separate seek slider labeled `Playback position`. The rail must show status, On-device lock, time labels, Play/Pause, Share WAV, and Generate/Regenerate.

- [ ] **Step 5: Run compact UI and full unit tests**

Run:

```bash
cd android
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Expected: all JVM/Robolectric tests PASS and the APK assembles.

- [ ] **Step 6: Commit compact UI**

```bash
git add android/app/src/main/kotlin/com/isbkch/echolocal/ui android/app/src/main/kotlin/com/isbkch/echolocal/MainActivity.kt android/app/src/test/kotlin/com/isbkch/echolocal/ui
git commit -m "Bring Echolocal's focused phone design to Android"
```

### Task 9: Add the adaptive Galaxy Z Fold layout

**Files:**
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/ui/ExpandedEcholocalScreen.kt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/ui/EcholocalLayoutMode.kt`
- Modify: `android/app/src/main/kotlin/com/isbkch/echolocal/ui/EcholocalApp.kt`
- Modify: `android/app/src/main/kotlin/com/isbkch/echolocal/MainActivity.kt`
- Create: `android/app/src/test/kotlin/com/isbkch/echolocal/ui/ExpandedEcholocalScreenTest.kt`
- Create: `android/app/src/androidTest/kotlin/com/isbkch/echolocal/ui/ResponsiveStateTest.kt`

**Interfaces:**
- Consumes: compact components and shared view-model state.
- Produces: `EcholocalLayoutMode`, expanded two-pane composition, and dynamic selection from `WindowWidthSizeClass`.

- [ ] **Step 1: Write failing expanded-layout tests**

```kotlin
@Test fun expandedLayoutKeepsVoiceDirectionVisible() {
    compose.setContent {
        EcholocalTheme {
            EcholocalApp(state = readyState(), actions = noOpActions, expanded = true)
        }
    }
    compose.onNodeWithText("SCRIPT").assertIsDisplayed()
    compose.onNodeWithText("VOICE").assertIsDisplayed()
    compose.onNodeWithText("Heart").assertIsDisplayed()
    compose.onNodeWithContentDescription("Voice direction").assertDoesNotExist()
}

@Test fun compactLayoutKeepsVoiceDirectionBehindButton() {
    compose.setContent {
        EcholocalTheme {
            EcholocalApp(state = readyState(), actions = noOpActions, expanded = false)
        }
    }
    compose.onNodeWithContentDescription("Voice direction").assertIsDisplayed()
}
```

- [ ] **Step 2: Run expanded tests and verify the layout is not implemented**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests '*.ExpandedEcholocalScreenTest'`

Expected: FAIL because expanded mode still renders compact UI.

- [ ] **Step 3: Implement adaptive mode without duplicating state**

```kotlin
enum class EcholocalLayoutMode { Compact, Expanded }
```

Use `calculateWindowSizeClass(this)` in `MainActivity`; map `WindowWidthSizeClass.Expanded` to Expanded and all other widths to Compact. Define `EcholocalApp(state, actions, layoutModeOverride: EcholocalLayoutMode? = null)` so production passes no override and tests can deterministically choose a posture; never persist that override. Pass the same `EcholocalViewModel` and state into either composition. The expanded screen uses a weighted `Row`: editor/listening pane about 1.65× the settings pane, a divider between them, and persistent `VoiceDirectionContent` without sheet chrome.

- [ ] **Step 4: Add a state-retention instrumentation test**

Type text, recompose the test host first with `layoutModeOverride = Compact` and then `layoutModeOverride = Expanded`, and assert the same `EcholocalViewModel` retains the text and selected voice. This validates state ownership without pretending an emulator posture event proves Samsung hardware behavior.

- [ ] **Step 5: Run responsive tests and compile instrumentation APK**

Run:

```bash
cd android
./gradlew :app:testDebugUnitTest --tests '*.ExpandedEcholocalScreenTest'
./gradlew :app:assembleDebugAndroidTest
```

Expected: JVM UI tests PASS and instrumentation APK compiles.

- [ ] **Step 6: Commit Fold adaptation**

```bash
git add android/app/src/main/kotlin/com/isbkch/echolocal/ui/ExpandedEcholocalScreen.kt android/app/src/main/kotlin/com/isbkch/echolocal/ui/EcholocalLayoutMode.kt android/app/src/main/kotlin/com/isbkch/echolocal/ui/EcholocalApp.kt android/app/src/main/kotlin/com/isbkch/echolocal/MainActivity.kt android/app/src/test/kotlin/com/isbkch/echolocal/ui/ExpandedEcholocalScreenTest.kt android/app/src/androidTest/kotlin/com/isbkch/echolocal/ui/ResponsiveStateTest.kt
git commit -m "Make Echolocal feel native on the Galaxy Fold"
```

### Task 10: Package licenses, build helpers, and maintainer documentation

**Files:**
- Create: `android/app/src/main/assets/licenses/apache-2.0.txt`
- Create: `android/app/src/main/assets/licenses/onnxruntime-mit.txt`
- Create: `android/app/src/main/assets/licenses/litert-third-party-notices.txt`
- Create: `android/app/src/main/assets/licenses/third-party-notices.txt`
- Create: `android/app/src/main/kotlin/com/isbkch/echolocal/ui/licenses/OpenSourceLicensesScreen.kt`
- Modify: `android/app/src/main/kotlin/com/isbkch/echolocal/ui/VoiceDirection.kt`
- Create: `android/app/src/test/kotlin/com/isbkch/echolocal/ui/licenses/LicenseAssetsTest.kt`
- Create: `scripts/build-android.sh`
- Create: `scripts/profile-android-memory.sh`
- Modify: `README.md`
- Modify: `THIRD_PARTY_NOTICES.md`

**Interfaces:**
- Consumes: Android dependency set and Voice Direction from prior tasks.
- Produces: readable in-app notices, repeatable APK/checksum build, safe memory sampler, and Android maintainer documentation.

- [ ] **Step 1: Write the failing license asset test**

```kotlin
@RunWith(RobolectricTestRunner::class)
class LicenseAssetsTest {
    @Test fun bundledNoticesCoverNativeRuntimeAndModel() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notices = context.assets.open("licenses/third-party-notices.txt").bufferedReader().readText()
        listOf("Soniqo", "speech-core", "Kokoro", "ONNX Runtime", "LiteRT").forEach {
            assertTrue("Missing $it", notices.contains(it))
        }
        assertTrue(context.assets.open("licenses/apache-2.0.txt").available() > 0)
        assertTrue(context.assets.open("licenses/onnxruntime-mit.txt").available() > 0)
        assertTrue(context.assets.open("licenses/litert-third-party-notices.txt").available() > 0)
    }
}
```

- [ ] **Step 2: Run the test and verify notices are absent**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests '*.LicenseAssetsTest'`

Expected: FAIL with missing asset files.

- [ ] **Step 3: Add complete, source-backed license material**

Copy the full Apache 2.0 text from `https://www.apache.org/licenses/LICENSE-2.0.txt`. Copy ONNX Runtime's MIT license from tag `v1.27.0`. Preserve LiteRT's complete applicable bundled notices in `litert-third-party-notices.txt`. Build `third-party-notices.txt` from Soniqo's `speech-android` copyright, pinned `speech-core` third-party notices, the pinned Kokoro model attribution, LiteRT, and Gradle dependency attributions. Include each component name, copyright attribution, license identifier, and the local full-text filename; do not label Soniqo code as MIT or imply endorsement.

Add an Open-source licenses row under Voice Direction's model status. The screen reads the bundled text assets; it does not require network access.

- [ ] **Step 4: Add the deterministic build script**

`scripts/build-android.sh` must contain this behavior:

```bash
#!/usr/bin/env bash
set -euo pipefail
REPO_ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$REPO_ROOT/android"
./gradlew :app:testDebugUnitTest :app:assembleDebug
APK="$REPO_ROOT/android/app/build/outputs/apk/debug/app-debug.apk"
test -f "$APK"
shasum -a 256 "$APK" > "$APK.sha256"
printf 'APK: %s\nSHA-256: %s\n' "$APK" "$APK.sha256"
```

Do not invoke `clean`; incremental local builds should remain useful.

- [ ] **Step 5: Add the memory sampler with explicit targets**

`scripts/profile-android-memory.sh` accepts exactly two arguments: ADB serial and output file. It validates the serial against `adb devices`, samples `dumpsys meminfo com.isbkch.echolocal` every 250 ms while the process exists, records timestamp and `TOTAL PSS` KiB, and prints the maximum at exit. It must never choose the first attached device implicitly. Implement this exact control flow:

```bash
#!/usr/bin/env bash
set -euo pipefail
if [[ $# -ne 2 ]]; then
  printf 'Usage: %s ADB_SERIAL OUTPUT_FILE\n' "$0" >&2
  exit 64
fi
DEVICE_SERIAL=$1
OUTPUT_FILE=$2
if ! adb devices | awk -v serial="$DEVICE_SERIAL" '
  NR > 1 && $1 == serial && $2 == "device" { found = 1 }
  END { exit(found ? 0 : 1) }
'; then
  printf 'ADB device is not connected and authorized: %s\n' "$DEVICE_SERIAL" >&2
  exit 69
fi
printf 'timestamp_utc\ttotal_pss_kib\n' > "$OUTPUT_FILE"
MAX_PSS=0
report_maximum() {
  printf 'Maximum TOTAL PSS: %s KiB\n' "$MAX_PSS"
}
trap report_maximum EXIT
while adb -s "$DEVICE_SERIAL" shell pidof com.isbkch.echolocal >/dev/null; do
  TOTAL_PSS=$(adb -s "$DEVICE_SERIAL" shell dumpsys meminfo com.isbkch.echolocal | awk '
    /TOTAL PSS:/ { print $3; exit }
    /^TOTAL[[:space:]]/ { print $2; exit }
  ')
  if [[ "$TOTAL_PSS" =~ ^[0-9]+$ ]]; then
    printf '%s\t%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$TOTAL_PSS" >> "$OUTPUT_FILE"
    if (( TOTAL_PSS > MAX_PSS )); then MAX_PSS=$TOTAL_PSS; fi
  fi
  sleep 0.25
done
```

- [ ] **Step 6: Document build/install/device boundaries**

Update README with:

```bash
./scripts/build-android.sh
adb devices -l
adb -s DEVICE_SERIAL install -r android/app/build/outputs/apk/debug/app-debug.apk
```

Explain that `DEVICE_SERIAL` is replaced with the explicit value from `adb devices -l`; document the 163.3 MiB explicit model download, five voices, Android 8+ minimum, deferred Pace, debug-signing scope, and Airplane Mode acceptance flow. Keep Apple commands intact.

- [ ] **Step 7: Run notices, build, and permission checks**

Run:

```bash
cd android
./gradlew :app:testDebugUnitTest --tests '*.LicenseAssetsTest'
cd ..
./scripts/build-android.sh
/Users/ilyasbakouch/Library/Android/sdk/build-tools/36.0.0/aapt dump permissions \
  android/app/build/outputs/apk/debug/app-debug.apk
unzip -Z1 android/app/build/outputs/apk/debug/app-debug.apk | rg '^lib/arm64-v8a/.+\.so$'
unzip -Z1 android/app/build/outputs/apk/debug/app-debug.apk | rg '^lib/x86_64/.+\.so$'
```

Expected: tests PASS; checksum exists; native libraries are present for both required ABIs; permission output includes INTERNET and library scheduling permissions but no RECORD_AUDIO, READ/WRITE_EXTERNAL_STORAGE, MANAGE_EXTERNAL_STORAGE, POST_NOTIFICATIONS, accessibility, overlay, contacts, or location permission.

- [ ] **Step 8: Commit licensing and maintainer flow**

```bash
git add android/app/src/main/assets android/app/src/main/kotlin/com/isbkch/echolocal/ui/licenses android/app/src/main/kotlin/com/isbkch/echolocal/ui/VoiceDirection.kt android/app/src/test/kotlin/com/isbkch/echolocal/ui/licenses scripts/build-android.sh scripts/profile-android-memory.sh README.md THIRD_PARTY_NOTICES.md
git commit -m "Make the Android proof build transparent and repeatable"
```

### Task 11: Prove the APK on the Fold and Xiaomi

**Files:**
- Create after measurements: `docs/android-proof-results.md`
- Create after capture: `docs/images/android/galaxy-z-fold-7-cover.png`
- Create after capture: `docs/images/android/galaxy-z-fold-7-unfolded.png`
- Create after capture: `docs/images/android/xiaomi-17-ultra.png`
- Modify only if evidence reveals a defect: the smallest owning source/test files from Tasks 2–10.

**Interfaces:**
- Consumes: final APK/checksum and all automated tests.
- Produces: install, layout, offline inference, WAV, timing, and peak-memory evidence for both named devices.

- [ ] **Step 1: Run the complete automated verification from a clean command boundary**

Run:

```bash
git status --short
./scripts/build-android.sh
cd android
./gradlew :app:assembleDebugAndroidTest
cd ..
```

Expected: clean or intentionally documented worktree; all JVM/Robolectric tests PASS; app and test APKs assemble; checksum verifies with `shasum -a 256 -c`.

- [ ] **Step 2: Connect one device and identify it explicitly**

Run:

```bash
adb devices -l
```

Stop if the intended device serial is not uniquely identifiable. Use `adb -s` on every later command. Never uninstall or clear app data unless the current acceptance step explicitly requires a fresh model download.

- [ ] **Step 3: Run instrumentation tests on the selected target**

Run:

```bash
cd android
ANDROID_SERIAL=DEVICE_SERIAL ./gradlew :app:connectedDebugAndroidTest
cd ..
```

Expected: the Compose and Media3 device tests PASS on the explicitly selected serial. This is an Android interaction gate, not on-device Kokoro inference proof.

- [ ] **Step 4: Install and capture shell evidence**

For the selected serial:

```bash
adb -s DEVICE_SERIAL install -r android/app/build/outputs/apk/debug/app-debug.apk
adb -s DEVICE_SERIAL shell am force-stop com.isbkch.echolocal
adb -s DEVICE_SERIAL shell monkey -p com.isbkch.echolocal 1
adb -s DEVICE_SERIAL exec-out screencap -p > DEVICE_SCREENSHOT.png
```

Expected: install succeeds, launch has no crash, and the setup overlay appears before any model network action.

- [ ] **Step 5: Complete the explicit model and five-voice flow**

On the device, tap Download local model and wait for verified Ready. Generate a short passage with Heart, Bella, Michael, Emma, and George. For each, play, pause, scrub, regenerate, and share the WAV. Confirm one shared WAV plays outside Echolocal and reports 24 kHz mono 16-bit PCM.

If a failure occurs, first add a focused reproduction test and diagnose it before editing production code; rerun the owning task's suite and the full build afterward.

- [ ] **Step 6: Prove offline persistence**

Force-stop with ADB, enable Airplane Mode manually on the device, relaunch, and generate/play/share again. Confirm no setup overlay returns and no network is required. Restore connectivity only after recording the result.

- [ ] **Step 7: Measure generation and memory without logging user text**

Use the same short passage and representative long passage on both devices. Capture `EcholocalMetrics`:

```bash
adb -s DEVICE_SERIAL logcat -c
adb -s DEVICE_SERIAL logcat -s EcholocalMetrics:I '*:S'
```

In a second terminal during generation:

```bash
./scripts/profile-android-memory.sh DEVICE_SERIAL DEVICE-memory.tsv
```

Record the first generation after process launch as cold, the immediate repeat as warm, and the script's maximum TOTAL PSS as peak memory. Record observed values; do not turn them into an invented pass/fail threshold.

- [ ] **Step 8: Run Fold-specific adaptation checks**

On the Galaxy Z Fold 7 only:

1. Start editing and model download on the cover screen, then unfold; verify no text/progress loss.
2. Confirm the unfolded screen shows the editor/listening rail left and persistent Voice Direction right.
3. Fold and unfold during playback; verify position and audio remain.
4. Capture both approved layouts with explicit `adb -s` screenshots.

- [ ] **Step 9: Repeat Steps 2–7 on the second named device**

Use the same APK SHA-256 on the Xiaomi 17 Ultra and Galaxy Z Fold 7. Do not substitute emulator evidence or a different APK between devices.

- [ ] **Step 10: Write the actual evidence report**

Create `docs/android-proof-results.md` containing:

- APK path and SHA-256;
- source commit tested;
- each device's ADB model identifier and Android version;
- install, five-voice, long-text, playback, scrub, share, and Airplane Mode results;
- cold milliseconds, warm milliseconds, and peak TOTAL PSS KiB for each device;
- Fold cover/unfolded transition results;
- exact screenshot paths;
- any remaining limitation, including deferred Pace and debug-only signing.

Every value must come from this run. Do not use expected values or copy Soniqo's Galaxy S23 benchmark into the report.

- [ ] **Step 11: Run final verification and commit device evidence**

Run:

```bash
./scripts/build-android.sh
git diff --check
git status --short
```

Expected: build/tests PASS and only the intended evidence files or targeted regression fix remain.

```bash
git add docs/android-proof-results.md docs/images/android/galaxy-z-fold-7-cover.png docs/images/android/galaxy-z-fold-7-unfolded.png docs/images/android/xiaomi-17-ultra.png
git commit -m "Prove Echolocal speech on Android devices"
```
