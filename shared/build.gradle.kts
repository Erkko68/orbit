import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kover)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            binaryOption("bundleId", "com.orbit.app.shared")
        }
    }

    android {
        namespace = "com.orbit.app.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.koin.android)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.playServicesAuth)
            implementation(libs.googleid)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.navigation.compose)

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            implementation(libs.ktor.client.core)

            implementation(libs.multiplatformSettings)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.kermit)

            implementation(libs.firebase.auth)
            implementation(libs.firebase.firestore)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
            implementation(libs.multiplatformSettingsTest)
            implementation(libs.compose.uiTest)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.robolectric)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

// Robolectric reaches into JDK internals that Java 17+ no longer exports by default.
tasks.withType<Test>().configureEach {
    jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

// The Firebase KMP klibs ask the linker for the Firebase iOS frameworks, which only the Xcode app
// build provides (SwiftPM). Test binaries never call Firebase (repositories are faked), so they link
// against empty static archives instead of downloading the real SDK.
val firebaseTestStubs = tasks.register("firebaseTestStubs") {
    val dir = layout.buildDirectory.dir("firebaseTestStubs")
    outputs.dir(dir)
    doLast {
        val stubDir = dir.get().asFile
        listOf("FirebaseCore", "FirebaseAuth", "FirebaseFirestore", "FirebaseFirestoreInternal").forEach {
            val frameworkDir = File(stubDir, "$it.framework")
            frameworkDir.mkdirs()
            File(frameworkDir, it).writeText("!<arch>\n")
        }
        if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) {
            val stubC = File(stubDir, "stubs.c").apply {
                writeText(
                    """
                    void *FIRAuthErrorDomain = 0;
                    void *FIRFirestoreErrorDomain = 0;
                    void *OBJC_CLASS_${'$'}_FIRAuth = 0;
                    void *OBJC_CLASS_${'$'}_FIRAuthDataResult = 0;
                    void *OBJC_CLASS_${'$'}_FIRCollectionReference = 0;
                    void *OBJC_CLASS_${'$'}_FIRDocumentChange = 0;
                    void *OBJC_CLASS_${'$'}_FIRDocumentReference = 0;
                    void *OBJC_CLASS_${'$'}_FIRDocumentSnapshot = 0;
                    void *OBJC_CLASS_${'$'}_FIRFieldPath = 0;
                    void *OBJC_CLASS_${'$'}_FIRFieldValue = 0;
                    void *OBJC_CLASS_${'$'}_FIRFilter = 0;
                    void *OBJC_CLASS_${'$'}_FIRFirestore = 0;
                    void *OBJC_CLASS_${'$'}_FIRGeoPoint = 0;
                    void *OBJC_CLASS_${'$'}_FIRGoogleAuthProvider = 0;
                    void *OBJC_CLASS_${'$'}_FIRMultiFactorResolver = 0;
                    void *OBJC_CLASS_${'$'}_FIROAuthCredential = 0;
                    void *OBJC_CLASS_${'$'}_FIRQuery = 0;
                    void *OBJC_CLASS_${'$'}_FIRQuerySnapshot = 0;
                    void *OBJC_CLASS_${'$'}_FIRSnapshotMetadata = 0;
                    void *OBJC_CLASS_${'$'}_FIRTimestamp = 0;
                    void *OBJC_CLASS_${'$'}_FIRWriteBatch = 0;
                    """.trimIndent(),
                )
            }
            listOf("FirebaseAuth", "FirebaseFirestore").forEach { name ->
                val objFile = File(stubDir, "$name.o")
                val binary = File(File(stubDir, "$name.framework"), name)
                ProcessBuilder("clang", "-target", "arm64-apple-ios-simulator", "-c", stubC.absolutePath, "-o", objFile.absolutePath)
                    .inheritIO()
                    .start()
                    .waitFor()
                ProcessBuilder("ar", "rcs", binary.absolutePath, objFile.absolutePath)
                    .inheritIO()
                    .start()
                    .waitFor()
            }
        }
    }
}
kotlin.targets.withType<KotlinNativeTarget>().configureEach {
    binaries.withType<TestExecutable>().configureEach {
        linkerOpts("-F${layout.buildDirectory.dir("firebaseTestStubs").get().asFile}")
        linkTaskProvider.configure { dependsOn(firebaseTestStubs) }
    }
}
