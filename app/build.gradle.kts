import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// 서명 정보는 저장소에 커밋하지 않고 keystore/keystore.properties 에서 읽는다.
val keystorePropsFile = rootProject.file("keystore/keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}
val keystoreFileName = keystoreProps.getProperty("storeFile") ?: "ndokhak-release.keystore"
val hasReleaseKeystore = keystorePropsFile.exists() &&
    rootProject.file("keystore/$keystoreFileName").exists()

android {
    namespace = "com.example.scorerecorder"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.scorerecorder"
        minSdk = 26
        targetSdk = 34
        versionCode = 5
        versionName = "1.0.4"
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file("keystore/$keystoreFileName")
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseKeystore) signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
            }
        }
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    applicationVariants.all {
        outputs.all {
            // debug/release 산출물 이름을 구분해 혼동 방지
            val suffix = if (buildType.name == "debug") "-debug" else ""
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "ndokhak_parkScoreCard$suffix.apk"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.google.code.gson:gson:2.10.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("androidx.compose.ui:ui-test-junit4:1.5.8")
}