import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

plugins {
    jacoco
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "io.github.aimindseye.sablehealth"
    compileSdk = 36
    defaultConfig {
        applicationId = "io.github.aimindseye.sablehealth"
        minSdk = 30
        targetSdk = 36
        versionCode = 4
        versionName = "0.1.3-m2.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
    buildTypes {
        debug { enableUnitTestCoverage = true }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
}

// Robolectric 4.16.1: API 30 and 34 test sandboxes, plus API 36 resources.
// Resolve independently to avoid Gradle selecting just the newest module version.
// These configurations are test infrastructure only, never an APK classpath.
val robolectricSdkVersions =
    listOf(
        "11-robolectric-6757853-i7",
        "14-robolectric-10818077-i7",
        "16-robolectric-13921718-i7",
    )
val robolectricSdkJars =
    robolectricSdkVersions.map { version ->
        configurations
            .detachedConfiguration(
                dependencies.create("org.robolectric:android-all-instrumented:$version@jar"),
            ).apply {
                isTransitive = false
            }
    }
val robolectricSdkDir = layout.buildDirectory.dir("robolectric-sdks")
val prepareRobolectricSdks by tasks.registering(Sync::class) {
    from(robolectricSdkJars)
    into(robolectricSdkDir)
}

tasks.withType<Test>().configureEach {
    dependsOn(prepareRobolectricSdks)
    inputs.dir(robolectricSdkDir).withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty("robolectric.offline", "true")
    systemProperty("robolectric.dependency.dir", robolectricSdkDir.get().asFile.absolutePath)
    systemProperty("robolectric.usePreinstrumentedJars", "true")
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
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
    testImplementation("androidx.health.connect:connect-testing:1.0.0-alpha03") {
        // Use the app's stable 1.1.0 client and its matching protocol dependency in tests.
        exclude(group = "androidx.health.connect", module = "connect-client")
        exclude(group = "androidx.health.connect", module = "connect-client-proto")
    }
    testImplementation(platform("androidx.compose:compose-bom:2026.02.01"))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("org.robolectric:robolectric:4.16.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.02.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:runner:1.7.0")
}
