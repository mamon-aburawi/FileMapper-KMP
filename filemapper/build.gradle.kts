@file:OptIn(ExperimentalWasmDsl::class)
@file:Suppress("DEPRECATION")

import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    // Android Target
    androidLibrary {
        namespace = "io.filemapper.kmp"
        compileSdk = 36
        minSdk = 24

        withJava()
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }



    }

    // Desktop (JVM) Target
    jvm()

    // Web (WasmJS) Target
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }
    js {
        browser()
        binaries.executable()
    }

    // iOS Targets
    val xcfName = "filemapper"

    iosX64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)

                 implementation(compose.runtime)
                 implementation(compose.foundation)
                 implementation(compose.material3)


                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)

                implementation(libs.compose.components.resources)


            }
        }

        androidMain {
            dependencies {

                implementation(libs.core)
                implementation(libs.androidx.activity.ktx)
                implementation(libs.androidx.activity.compose)
            }
        }


        nativeMain{
            dependencies {
                implementation(libs.kmp.zip)
            }
        }

        jvmMain {
            dependencies {


            }
        }

        wasmJsMain {
            dependencies {
                implementation(compose.ui)
                implementation(npm("jszip", "3.10.1"))
            }
        }
    }


    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-Xallow-unstable-dependencies")
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    tasks.withType<Test>().configureEach {
        systemProperty("file.encoding", "UTF-8")
    }


}

group = "io.github.mamon-aburawi" // this group name in maven central repository
version = "1.0.0" // version of library

mavenPublishing {
    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Empty(),
            sourcesJar = true,
            androidVariantsToPublish = listOf("release", "debug"),
        )
    )

    coordinates(
        groupId = group.toString(),
        version = version.toString(),
        artifactId = "filemapper-kmp"
    )

    pom {
        name = "FileMapper KMP"
        description = "A lightweight, powerful Kotlin Multiplatform (KMP) library designed to seamlessly map files (XLSX, JSON) to Kotlin Data Classes. Supporting Android, iOS, Desktop, and Web (Wasm/JS), it provides both Compose-ready UI triggers and pure logic-based APIs."
        inceptionYear = "2026"
        url = "https://github.com/mamon-aburawi/FileMapper-KMP"
        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/licenses/MIT"
            }
        }
        developers {
            developer {
                name = "Mamon Aburawi"
                email = "mamon.aburawi@gmail.com"
            }
        }
        scm {
            url = "https://github.com/mamon-aburawi/FileMapper-KMP"
        }
    }

    publishToMavenCentral()
    signAllPublications()
}