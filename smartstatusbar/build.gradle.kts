import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    // The multiplatform Android plugin has no lint of its own.
    alias(libs.plugins.android.lint)
    alias(libs.plugins.kotlinCompose)
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
