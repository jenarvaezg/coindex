plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
}

tasks.withType<Test>().configureEach {
    useJUnit()

    for (tree in listOf("app/src/main/kotlin", "app/src/test/kotlin", "domain/src")) {
        inputs.dir(rootProject.layout.projectDirectory.dir(tree))
            .withPropertyName(tree.replace('/', '-'))
            .withPathSensitivity(PathSensitivity.RELATIVE)
    }
}
