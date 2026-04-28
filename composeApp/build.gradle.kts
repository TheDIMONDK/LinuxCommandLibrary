import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.linuxcommandlibrary"

kotlin {
    android {
        namespace = "com.linuxcommandlibrary.app"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        androidResources {
            enable = true
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":viewmodels"))
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.navigation.compose)
                implementation(libs.kotlinx.serialization.core)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.koin.android)
                implementation(libs.koin.androidx.compose)
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.preference)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

composeCompiler {
    stabilityConfigurationFiles.add(project.layout.projectDirectory.file("stability_config.conf"))
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.linuxcommandlibrary.app.resources"
    generateResClass = always
}

// Stage shared SVG icons from /assets/icons/ into composeResources/drawable.
// Single source of truth: /assets/icons/*.svg (also bundled by iOS via Xcode folder ref).
// The copies in composeResources/drawable are gitignored build artifacts.
val copySharedIcons by tasks.registering(Copy::class) {
    from(rootProject.file("assets/icons"))
    into("$projectDir/src/commonMain/composeResources/drawable")
    include("*.svg")
}

// Make resource processing depend on the icon copy
tasks
    .matching {
        val n = it.name
        n.startsWith("prepareComposeResourcesTask") ||
            n.startsWith("generateResourceAccessors") ||
            n.startsWith("generateActualResourceCollectors") ||
            n.startsWith("generateExpectResourceCollectors") ||
            n.startsWith("convertXmlValueResources") ||
            n.startsWith("copyNonXmlValueResources")
    }.configureEach {
        dependsOn(copySharedIcons)
    }

// Task to regenerate assets/commands/index.txt from the .md files in that directory
tasks.register("updateCommandIndex") {
    val commandsDir = file("${rootProject.projectDir}/assets/commands")
    val indexFile = file("$commandsDir/index.txt")

    inputs.dir(commandsDir)
    outputs.file(indexFile)

    doLast {
        val entries =
            commandsDir
                .listFiles { f -> f.extension == "md" }
                ?.map { it.name }
                ?.sorted()
                ?: emptyList()
        indexFile.writeText(entries.joinToString("\n") + "\n")
        println("Updated commands/index.txt with ${entries.size} entries")
    }
}
