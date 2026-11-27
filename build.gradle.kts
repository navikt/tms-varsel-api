import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(managed.plugins.kotlin)
    alias(managed.plugins.tms.jarBundling)

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
    implementation(managed.bundles.jackson)
    implementation(managed.bundles.ktorClient)
    implementation(managed.bundles.ktorServer)

    runtimeOnly(managed.bundles.logback)

    implementation(managed.kotlinLogging)
    implementation(managed.ktor.serialization.jackson)
    implementation(managed.ktor.server.cors)
    implementation(managed.tms.ktorTokenSupport.userToken.verification)
    implementation(managed.tms.ktorTokenSupport.userToken.exchange)
    implementation(managed.tms.commonLib.metrics)
    implementation(managed.tms.commonLib.teamLogger)
    implementation(managed.tms.commonLib.utils)

    testRuntimeOnly(managed.junit.launcher)
    testRuntimeOnly(managed.junit.engine)

    testImplementation(managed.junit.api)
    testImplementation(managed.kotest.assertion)
    testImplementation(managed.ktor.test.server)
    testImplementation(managed.mockk)
    testImplementation(managed.tms.ktorTokenSupport.userToken.verificationMock)
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
