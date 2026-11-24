plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

/**
 * Mac-owner signing only. CI receives no signing credentials and assembles unsigned release candidates.
 * No signing key/certificate fingerprint is inherited from TenSeven Launcher.
 */
/** CI and the Mac build produce an unsigned release APK; the owner signs with tools/build-release.sh. */
android {
    namespace = "io.github.aimindseye.sablehealth"
    compileSdk = 36
    defaultConfig {
        applicationId = "io.github.aimindseye.sablehealth"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-m0"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(platform("androidx.compose:compose-bom:2026.02.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.health.connect:connect-client:1.1.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
