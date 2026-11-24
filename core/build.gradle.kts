import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    id("org.jetbrains.kotlin.jvm")
    jacoco
}

kotlin { jvmToolchain(17) }

dependencies { testImplementation("junit:junit:4.13.2") }

tasks.test { useJUnit() }

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
