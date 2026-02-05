plugins {
    alias(libs.plugins.android.application)
    // FIX: Mengikuti Project-level build.gradle
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

ksp {
    arg("room.schemaLocation", "${projectDir}/schemas")
}

android {
    namespace = "com.example.crashcourse"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.crashcourse"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.1" // Naikkan ke 1.1 untuk Auth System

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                cppFlags += ""
            }
        }

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    sourceSets["main"].jniLibs.srcDirs("src/main/jniLibs")

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        // Otomatis dihandle oleh Kotlin 2.0+
    }

    androidResources {
        noCompress += "tflite"
    }

    lint {
        disable += "Instantiatable"
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)

    // Compose
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Icons
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.camerax.extensions)

    // ML Kit & TensorFlow
    implementation(libs.mlkit.face.detection)
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)

    // Permissions
    implementation(libs.accompanist.permissions)

    // Image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // HTTP client for photo downloading
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Desugaring for LocalDateTime support on lower API levels
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // PDF and CSV export
    implementation("com.itextpdf:itext7-core:7.2.5")
    implementation("com.opencsv:opencsv:5.7.1")

    // Lifecycle Compose Support
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

   // --- AZURATECH FIREBASE STACK (FIXED VERSION) ---
    // Kita tidak pakai platform BoM dulu untuk sementara agar Gradle tidak bingung
    
    // Authentication
    implementation("com.google.firebase:firebase-auth-ktx:22.3.1")

    // Firestore (Database)
    implementation("com.google.firebase:firebase-firestore-ktx:24.10.1")

    // Analytics & Functions
    implementation("com.google.firebase:firebase-analytics-ktx:21.5.0")
    implementation("com.google.firebase:firebase-functions-ktx:20.4.0")
    
    // Jangan lupa common ktx untuk interoperabilitas coroutines
    implementation("com.google.firebase:firebase-common-ktx:20.4.2")
    implementation("com.google.code.gson:gson:2.10.1")
}