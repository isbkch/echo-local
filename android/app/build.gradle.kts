import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

fun signingValue(name: String): String? = providers.environmentVariable(name)
    .orElse(providers.gradleProperty(name)).orNull?.takeIf { it.isNotBlank() }

val uploadSigning = listOf(
    "ECHOLOCAL_ANDROID_KEYSTORE_PATH",
    "ECHOLOCAL_ANDROID_KEYSTORE_PASSWORD",
    "ECHOLOCAL_ANDROID_KEY_ALIAS",
    "ECHOLOCAL_ANDROID_KEY_PASSWORD",
).associateWith(::signingValue)
val hasUploadSigning = uploadSigning.values.all { it != null }

if (providers.gradleProperty("ECHOLOCAL_ANDROID_REQUIRE_SIGNING").orNull == "true") {
    val missing = uploadSigning.filterValues { it == null }.keys
    require(missing.isEmpty()) {
        "Release signing is required. Set ${missing.joinToString()} in environment variables " +
            "or your user Gradle properties file."
    }
    val keystore = file(uploadSigning.getValue("ECHOLOCAL_ANDROID_KEYSTORE_PATH")!!)
    require(File(uploadSigning.getValue("ECHOLOCAL_ANDROID_KEYSTORE_PATH")!!).isAbsolute && keystore.isFile) {
        "ECHOLOCAL_ANDROID_KEYSTORE_PATH must point to an existing keystore using an absolute path."
    }
}

android {
    namespace = "com.isbkch.echolocal"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.isbkch.echolocal"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        providers.gradleProperty("ECHOLOCAL_ANDROID_VERSION_CODE").orNull?.let { override ->
            val code = override.toIntOrNull()
            require(code != null && code in 1..2_100_000_000) {
                "ECHOLOCAL_ANDROID_VERSION_CODE must be an integer from 1 to 2100000000."
            }
            versionCode = code
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    signingConfigs {
        if (hasUploadSigning) {
            create("release") {
                storeFile = file(uploadSigning.getValue("ECHOLOCAL_ANDROID_KEYSTORE_PATH")!!)
                storePassword = uploadSigning.getValue("ECHOLOCAL_ANDROID_KEYSTORE_PASSWORD")
                keyAlias = uploadSigning.getValue("ECHOLOCAL_ANDROID_KEY_ALIAS")
                keyPassword = uploadSigning.getValue("ECHOLOCAL_ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (hasUploadSigning) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    packaging {
        resources.excludes += setOf("META-INF/LICENSE.md", "META-INF/LICENSE-notice.md")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.media3.exoplayer)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.soniqo.speech)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
