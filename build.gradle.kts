import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinCompose) apply false
}

// The web targets download Node.js, Yarn and Binaryen from repositories that the Kotlin plugin would
// add to the project, which settings.gradle.kts forbids: they are declared there instead.
allprojects {
    plugins.withType<NodeJsPlugin> { the<NodeJsEnvSpec>().downloadBaseUrl.set(null as String?) }
    plugins.withType<WasmNodeJsPlugin> { the<WasmNodeJsEnvSpec>().downloadBaseUrl.set(null as String?) }
    plugins.withType<BinaryenPlugin> { the<BinaryenEnvSpec>().downloadBaseUrl.set(null as String?) }
    plugins.withType<YarnPlugin> { rootProject.the<YarnRootEnvSpec>().downloadBaseUrl.set(null as String?) }
    plugins.withType<WasmYarnPlugin> { rootProject.the<WasmYarnRootEnvSpec>().downloadBaseUrl.set(null as String?) }
}
