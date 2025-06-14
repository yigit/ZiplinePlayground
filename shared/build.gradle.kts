import app.cash.zipline.gradle.ZiplineCompileTask
import app.cash.zipline.gradle.ZiplinePlugin
import app.cash.zipline.loader.ManifestVerifier.Companion.NO_SIGNATURE_CHECKS
import app.cash.zipline.loader.ZiplineHttpClient
import app.cash.zipline.loader.ZiplineLoader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import okio.ByteString
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithSimulatorTests
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.zipline)
    alias(libs.plugins.skie)
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8)
                }
            }
        }
    }
    js {
        browser()
        binaries.executable()
    }
    jvm {
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
        macosX64(),
        macosArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "ziplinePlayground"
            isStatic = true
        }
    }
    applyDefaultHierarchyTemplate()
    sourceSets {
        val commonMain by getting
        val commonTest by getting
        commonMain.dependencies {
            implementation(libs.zipline.zipline)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kruth)
        }
        val hostMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.okio.okio)
                implementation(libs.zipline.loader)
            }
        }
        val hostTest by creating {
            dependsOn(commonTest)
            dependencies {
                implementation(libs.okio.fakeFileSystem)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        val androidJvmCommonMain by creating {
            dependsOn(hostMain)
            dependencies {
                implementation(libs.okio.okio)
            }
        }

        listOf(androidMain, jvmMain).forEach {
            it.dependencies {
                implementation(libs.okHttp.core)
            }
            it.configure {
                dependsOn(androidJvmCommonMain)
            }
        }
        listOf(androidInstrumentedTest, jvmTest).forEach {
            it.configure {
                dependsOn(hostTest)
            }
        }
        appleMain.configure {
            dependsOn(hostMain)
        }
        appleTest.configure {
            dependsOn(hostTest)
        }
        androidInstrumentedTest {
            dependencies {
                implementation(libs.androidx.junit)
                implementation(libs.androidx.runner)
                implementation(libs.androidx.rules)
            }
        }
    }
}

android {
    namespace = "com.birbit.ziplineplayground"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

zipline {
    mainFunction.set("com.birbit.ziplineplayground.launchZipline")
}

// see README.md to learn how this is setup.
/**
 * This task creates a ZiplineLoader to "download" the hashed & compiled zipline files into a build
 * directory.
 */
abstract class CreateZiplineBundleTask @Inject constructor(
) : DefaultTask() {
    @get:InputDirectory
    abstract val ziplineOutput: DirectoryProperty

    @get:OutputDirectory
    abstract val downloadDir: DirectoryProperty

    @get:Input
    abstract val appName: Property<String>

    init {
        description = """
            Creates a folder that resembles what ZiplineLoader would download, for first launch
            consumption.
        """.trimIndent()
    }

    @TaskAction
    @OptIn(ExperimentalStdlibApi::class)
    fun copyZiplineToIosTest() {
        runBlocking {
            val loader = ZiplineLoader(
                dispatcher = coroutineContext[CoroutineDispatcher]!!,
                manifestVerifier = NO_SIGNATURE_CHECKS,
                httpClient = object : ZiplineHttpClient() {
                    override suspend fun download(
                        url: String,
                        requestHeaders: List<Pair<String, String>>
                    ): ByteString {
                        val filePath = url.substringAfter("${FAKE_URL}/",
                            missingDelimiterValue = "")
                        check(filePath.isNotEmpty()) {
                            "Unexpected URL: $url"
                        }
                        return FileSystem.SYSTEM.read(
                            ziplineOutput.get().asFile.resolve(filePath).toOkioPath()
                        ) {
                            readByteString()
                        }
                    }

                }
            )
            loader.download(
                applicationName = appName.get(),
                downloadFileSystem = FileSystem.SYSTEM,
                downloadDir = downloadDir.asFile.get().toOkioPath(),
                manifestUrl = "$FAKE_URL/manifest.zipline.json"
            )
        }
    }

    companion object Companion {
        const val FAKE_URL = "http://not_a_real_host"
    }
}

/**
 * This combines all bunldes generated from JS tasks. There is only 1 here but could be more in
 * theory.
 */
abstract class PackageAllZiplineBundlesTask @Inject constructor(
    private val fileSystemOperations: FileSystemOperations
) : DefaultTask() {
    @get:Nested
    abstract val bundles : ListProperty<ZiplineBundle>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun combineAllBundles() {
        val outputDirectory = outputDirectory.asFile.get()
        outputDirectory.deleteRecursively()
        outputDirectory.mkdirs()
        bundles.get().forEach {
            fileSystemOperations.sync {
                from(it.directory)
                into(outputDirectory.resolve("zipline/${it.appName}"))
            }
        }
    }
    class ZiplineBundle(
        @get:Input
        val appName: String,
        @get:InputDirectory
        val directory: Provider<DirectoryProperty>
    )
}

val packageDevelopmentZipline = tasks.register<PackageAllZiplineBundlesTask>("packageDevelopmentZipline") {
    outputDirectory.set(project.layout.buildDirectory.dir("packagedZiplineBundlesForDevelopment"))
}
val packageProductionZipline = tasks.register<PackageAllZiplineBundlesTask>("packageProductionZipline") {
    outputDirectory.set(project.layout.buildDirectory.dir("packagedZiplineBundlesForProduction"))
}

kotlin {
    sourceSets {
        // add combined zipline bundle to resources.
        val jvmMain by getting {
            resources.srcDir(packageDevelopmentZipline.map { it.outputDirectory })
        }
    }
}

android {
    // add zipline bundle as resources
    libraryVariants.configureEach {
        val debuggable = buildType.isDebuggable
        val packageTask = if (debuggable) {
            packageDevelopmentZipline
        } else {
            packageProductionZipline
        }
        processJavaResourcesProvider?.configure {
            dependsOn(packageDevelopmentZipline)
            from(packageTask.map { it.outputDirectory }) {
                into("/")
            }
        }
    }
}

plugins.withType<ZiplinePlugin>().configureEach {
    val appName = project.name // TODO this should be configurable
    kotlin.targets.withType(KotlinJsIrTarget::class.java).configureEach {
        binaries.withType(JsIrBinary::class.java).configureEach {
            // DANGER: so many assumptions :)
            val compileZiplineTaskName = "${linkTaskName}Zipline"
            val ziplineCompileTask = tasks.named<ZiplineCompileTask>(compileZiplineTaskName)
            val bundleTaskName = "${compileZiplineTaskName}CreateBundle"
            val bundleTask = tasks.register<CreateZiplineBundleTask>(bundleTaskName) {
                this.ziplineOutput.set(ziplineCompileTask.flatMap { it.outputDir })
                this.downloadDir.set(
                    project.layout.buildDirectory.dir("ziplineBundles/$bundleTaskName")
                )
                this.appName.set(project.name)
            }
            val isDevelopment = compileZiplineTaskName.contains("development", ignoreCase = true)
            val packageAllTask = if (isDevelopment) {
                packageDevelopmentZipline
            } else {
                packageProductionZipline
            }
            packageAllTask.configure {
                bundles.add(
                    PackageAllZiplineBundlesTask.ZiplineBundle(
                        appName = appName,
                        directory = bundleTask.map { it.downloadDir }
                    )
                )
            }
            if (isDevelopment) {
                kotlin {
                    targets.forEach { target ->
                        // see: https://github.com/icerockdev/moko-resources/pull/107/files
                        val binaries: DomainObjectSet<org.jetbrains.kotlin.gradle.plugin.mpp.NativeBinary>? = when(target) {
                            is KotlinNativeTargetWithHostTests -> target.binaries
                            is KotlinNativeTargetWithSimulatorTests -> target.binaries
                            else -> null
                        }
                        if (target is KotlinNativeTargetWithHostTests) {
                            target.binaries.matching {
                                it is TestExecutable
                            }
                        }
                        binaries?.matching {
                            it is TestExecutable
                        }?.configureEach {
                            val testExecutable = this as TestExecutable
                            testExecutable.linkTaskProvider
                            val bundleZiplineTask = tasks.register<Sync>("$bundleTaskName${testExecutable.linkTaskName}CopyForIosTests") {
                                from(packageDevelopmentZipline.map { it.outputDirectory })
                                into(
                                    project.layout.dir(
                                        testExecutable.linkTaskProvider.flatMap {
                                            it.outputFile.map { it.parentFile }
                                        }
                                    )
                                )
                            }
                            testExecutable.linkTaskProvider.configure {
                                dependsOn(bundleZiplineTask)
                            }
                        }
                    }
                }
            }
        }
    }
}
