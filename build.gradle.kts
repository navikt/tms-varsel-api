import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("jvm").version(libs.versions.kotlin)

    alias(libs.plugins.tms.jarBundling)

    application
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    mavenLocal()
}

dependencies {
    implementation(libs.bundles.jackson)
    implementation(libs.bundles.ktorClient)
    implementation(libs.bundles.ktorServer)
    implementation(libs.bundles.logback)

    implementation(libs.kotlinLogging)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.defaultHeaders)
    implementation(libs.tms.ktorTokenSupport.userToken.verification)
    implementation(libs.tms.ktorTokenSupport.userToken.exchange)
    implementation(libs.tms.commonLib.metrics)
    implementation(libs.tms.commonLib.teamLogger)
    implementation(libs.tms.commonLib.utils)

    testImplementation(libs.bundles.junit)
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.ktor.test.server)
    testImplementation(libs.mockk)
    testImplementation(libs.tms.ktorTokenSupport.userToken.verificationMock)
}

application {
    mainClass.set("no.nav.tms.varsel.api.ApplicationKt")
}

tasks {
    test {
        useJUnitPlatform()
        testLogging {
            showExceptions = true
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
            events("passed", "skipped", "failed")
        }
    }
}
