pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Route only the three pinned Robolectric test framework JARs to the
        // Google-hosted Central mirror. All runtime/plugin repositories stay unchanged.
        exclusiveContent {
            forRepository {
                maven {
                    name = "RobolectricSdkCentralMirror"
                    url = uri("https://maven-central.storage-download.googleapis.com/maven2/")
                }
            }
            filter {
                includeVersion("org.robolectric", "android-all-instrumented", "11-robolectric-6757853-i7")
                includeVersion("org.robolectric", "android-all-instrumented", "14-robolectric-10818077-i7")
                includeVersion("org.robolectric", "android-all-instrumented", "16-robolectric-13921718-i7")
            }
        }
        google()
        mavenCentral()
    }
}
rootProject.name = "sablehealth"
include(":core", ":app")
