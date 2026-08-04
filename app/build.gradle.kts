import java.util.Properties

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
        versionCode = 19
        versionName = "0.12.0"
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

// The unit tests read the curated seeds and the recorded Numista responses straight from the repo
// root —`../data` and `../fixtures`, see `Fixtures.kt`— instead of from a copy on the classpath, so
// Gradle sees no dependency on either. A catalog or a fixture could change and the cached test
// result still counted as valid: the suite reported BUILD SUCCESSFUL without running a single test,
// and only `--rerun` revealed the failure. Declaring both directories as inputs is what makes a
// data-only change invalidate that cache.
tasks.withType<Test>().configureEach {
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

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
}
