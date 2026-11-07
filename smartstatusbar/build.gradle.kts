import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    // The multiplatform Android plugin has no lint of its own.
    alias(libs.plugins.android.lint)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    explicitApi()

    android {
        namespace = "dev.kyrylost.smartstatusbar"
        compileSdk = 37
        minSdk = 23

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        withHostTest {}
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    // Every other Compose Multiplatform target, so that the library can be used from common code.
    // There it does nothing (see Platform.kt).
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()
    js {
        browser { testTask { enabled = false } }
        nodejs()
    }
    // Compose needs a browser on wasm (skiko), so its tests are not run: common tests run on the other targets.
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser { testTask { enabled = false } }
    }

    applyDefaultHierarchyTemplate {
        common {
            group("nonAndroid") {
                withJvm()
                withNative()
                withJs()
                withWasmJs()
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Modifier is part of the public API. No BOM: see libs.versions.toml.
            api(libs.lib.compose.runtime)
            api(libs.lib.compose.ui)
        }
        androidMain.dependencies {
            implementation(libs.lib.core.ktx)
            implementation(libs.lib.compose.foundation.layout)
            implementation(libs.lib.lifecycle.runtime.compose)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutines.test)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(project.dependencies.platform(libs.androidx.compose.bom))
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.compose.foundation)
            implementation(libs.androidx.espresso.core)
            implementation(libs.androidx.junit)
            implementation(libs.androidx.compose.ui.test.junit4)
            implementation(libs.androidx.compose.ui.test.manifest)
        }
    }
}

// Group and version come from GROUP and VERSION_NAME in gradle.properties.
mavenPublishing {
    publishToMavenCentral()
    // Maven Central requires signatures; local publishing works without a key.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    coordinates(artifactId = "smartstatusbar")
    pom {
        name.set("SmartStatusBar")
        description.set("Automatic light/dark status bar icons for Jetpack Compose and Compose Multiplatform, based on the content underneath.")
        inceptionYear.set("2026")
        url.set(providers.gradleProperty("POM_URL"))
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("kyrylost")
                name.set("Kyrylo Stukalo")
                url.set("https://github.com/kyrylost")
            }
        }
        scm {
            url.set(providers.gradleProperty("POM_URL"))
            connection.set("scm:git:git://github.com/kyrylost/SmartStatusBar.git")
            developerConnection.set("scm:git:ssh://git@github.com/kyrylost/SmartStatusBar.git")
        }
    }
}
