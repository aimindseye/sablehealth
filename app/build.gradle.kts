import java.security.KeyStore
import java.security.MessageDigest
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

/**
 * Mac-owner signing only. CI receives no signing credentials and assembles unsigned release candidates.
 * No signing key/certificate fingerprint is inherited from TenSeven Launcher.
 */
val localSigning =
    Properties().apply {
        val path = rootProject.file("keystore.properties")
        if (path.isFile) path.inputStream().use { load(it) }
    }

fun signingInput(
    property: String,
    environmentVariable: String,
): String? = (localSigning.getProperty(property) ?: System.getenv(environmentVariable))?.takeIf(String::isNotBlank)

val signingStore = signingInput("storeFile", "SABLE_HEALTH_KEYSTORE")
val signingStorePassword = signingInput("storePassword", "SABLE_HEALTH_KEYSTORE_PASSWORD")
val signingAlias = signingInput("keyAlias", "SABLE_HEALTH_KEY_ALIAS")
val signingKeyPassword = signingInput("keyPassword", "SABLE_HEALTH_KEY_PASSWORD")
val signingInputs = listOf(signingStore, signingStorePassword, signingAlias, signingKeyPassword)
val releaseSigned = signingInputs.all { it != null }

if (signingInputs.any { it != null } && !releaseSigned) {
    throw GradleException("Incomplete Sable Health signing configuration; provide all four values or none.")
}

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
    signingConfigs {
        if (releaseSigned) {
            create("release") {
                storeFile = rootProject.file(signingStore!!)
                storePassword = signingStorePassword
                keyAlias = signingAlias
                keyPassword = signingKeyPassword
            }
        }
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
            if (releaseSigned) signingConfig = signingConfigs.getByName("release")
        }
    }
}

val verifyRegisteredReleaseKey by tasks.registering {
    group = "verification"
    description = "Fail closed unless the local signing key matches the owner's registered SHA-256 certificate"
    doLast {
        check(releaseSigned) { "Release signing is not configured; no registered identity verified." }
        val certificateFile = rootProject.file("config/release-certificate.sha256")
        check(certificateFile.isFile) {
            "Release identity not pinned. Create config/release-certificate.sha256 from the registered public certificate."
        }
        val expected = certificateFile.readText().trim().lowercase()
        check(Regex("[0-9a-f]{64}").matches(expected)) {
            "Invalid pinned signing certificate fingerprint format."
        }
        val keyStoreFile = rootProject.file(signingStore!!)
        check(keyStoreFile.isFile) { "Configured release keystore does not exist." }
        val store = KeyStore.getInstance(keyStoreFile, signingStorePassword!!.toCharArray())
        check(store.isKeyEntry(signingAlias!!)) { "Release alias is not a private-key entry." }
        val certificate =
            store.getCertificate(signingAlias!!)
                ?: error("Registered release key certificate not found.")
        val actual =
            MessageDigest
                .getInstance("SHA-256")
                .digest(certificate.encoded)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        check(actual == expected) {
            "Release signing certificate does not match the registered public certificate fingerprint."
        }
        println("PASS_SABLE_HEALTH_REGISTERED_CERTIFICATE=$actual")
    }
}

// Fail before signing/packaging whenever a signing configuration is supplied.
// CI without credentials builds the release candidate unsigned and verifies that status independently.
if (releaseSigned) {
    tasks
        .matching { it.name in setOf("assembleRelease", "packageRelease", "bundleRelease", "signReleaseBundle") }
        .configureEach {
            dependsOn(verifyRegisteredReleaseKey)
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
