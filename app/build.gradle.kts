import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GenerateReleaseRuntimeLicenseGroups : DefaultTask() {
    @get:Input
    abstract val groups: SetProperty<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(groups.get().sorted().joinToString(separator = "\n", postfix = "\n"))
    }
}

plugins {
    // AGP 9 ships built-in Kotlin support; applying `kotlin-android` is now an error.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.jenarvaezg.coindex"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jenarvaezg.coindex"
        minSdk = 29
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = 65
        // **Minor again, and for the same clause**: the twenty plates of «Explorar» are catalogs the
        // app **could not open at all** — ADR 0021 §7 kept them shut and ADR 0030 §1 opens them — so
        // this is new capability and not a change inside one. It brings a gesture that spends
        // («Tasar esta lámina · N consultas», the first amendment ADR 0028 §3 has ever taken), a
        // figure that never expires and travels with its date, and a second room inside the annex:
        // «Lo que busco» moves one door in (#498, ADR 0030). Nothing is added to the schema.
        //
        // The phones are on **1.3.0**, `versionCode` 64 — what the latest release's `update.json`
        // says, not what anybody remembers — so **1.4.0 is waiting**. `scripts/release.sh` reads that
        // manifest and refuses a `versionCode` that does not beat it; this line only says who is
        // waiting, which is why it is worth correcting after every release.
        versionName = "1.4.0"
    }

    // The curated catalogs and the type-metadata snapshot live in `data/` at the repo root,
    // next to the fixtures the tests read. They are packaged from there rather than copied.
    sourceSets["main"].assets.srcDirs("src/main/assets", "../data")

    // Updates must be signed with the same key as the installed APK, so the keystore is a
    // durable secret: it lives outside the repo and is referenced from keystore.properties.
    val keystoreProperties = rootProject.file("keystore.properties").takeIf { it.exists() }
        ?.let { file -> Properties().apply { file.inputStream().use(::load) } }

    signingConfigs {
        if (keystoreProperties != null) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    jvmToolchain(21)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

val releaseRuntimeGroupsFile =
    layout.buildDirectory.file("generated/licenses/release-runtime-groups.txt")
val generateReleaseRuntimeLicenseGroups =
    tasks.register<GenerateReleaseRuntimeLicenseGroups>("generateReleaseRuntimeLicenseGroups") {
        description = "Writes the unique external module groups in the release runtime classpath."
        outputFile.set(releaseRuntimeGroupsFile)
        groups.set(configurations.named("releaseRuntimeClasspath").map { configuration ->
            configuration.incoming.resolutionResult.allComponents
                .mapNotNull { component ->
                    (component.id as? ModuleComponentIdentifier)?.group
                }
                .toSet()
        })
    }

// The unit tests read the curated seeds and the recorded Numista responses straight from the repo
// root —`../data` and `../fixtures`, see `Fixtures.kt`— instead of from a copy on the classpath, so
// Gradle sees no dependency on either. A catalog or a fixture could change and the cached test
// result still counted as valid: the suite reported BUILD SUCCESSFUL without running a single test,
// and only `--rerun` revealed the failure. Declaring both directories as inputs is what makes a
// data-only change invalidate that cache.
tasks.withType<Test>().configureEach {
    dependsOn(generateReleaseRuntimeLicenseGroups)
    inputs.file(releaseRuntimeGroupsFile)
        .withPropertyName("releaseRuntimeDependencyGroups")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir(layout.projectDirectory.dir("src/main/assets/licenses"))
        .withPropertyName("packagedLicenseNotices")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir(rootProject.layout.projectDirectory.dir("data"))
        .withPropertyName("curatedSeedData")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir(rootProject.layout.projectDirectory.dir("fixtures"))
        .withPropertyName("recordedNumistaFixtures")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.junit)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.okhttp)

    // The QR of each coin on the printed page (#234). It goes in `:app` and not in `:domain`: the
    // Numista page of a type is not something the domain reasons about, it is something the paper
    // draws — and the encoder never touches Coil, so a QR cannot be a photograph that failed.
    implementation(libs.zxing.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
}
