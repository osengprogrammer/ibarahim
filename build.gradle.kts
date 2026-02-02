// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    
    // FIX: Force Kotlin to 2.1.0 to match the new Firebase requirements
    // We replace the alias with the direct ID to force the version update
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false

    // FIX: Add the Compose Compiler plugin (Required for Kotlin 2.0+)
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false

    // FIX: Update KSP to the version compatible with Kotlin 2.1.0 (Required for Room)
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false

    // Google Services
    id("com.google.gms.google-services") version "4.4.4" apply false
}