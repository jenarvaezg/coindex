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

    // `DomainSurfaceTest` reads source text — the app's, to see whether it still calls what
    // `:domain` exposes, and the domain's own, because `@SuiteOnly` has SOURCE retention and
    // leaves no trace in the bytecode the task already tracks.
    //
    // Without declaring them, a change that only edits `:app` leaves this task UP-TO-DATE, which
    // is exactly the shape of #183: a caller deleted from the UI, the domain untouched, and the
    // net that exists to notice never running.
    for (tree in listOf("app/src/main/kotlin", "app/src/test/kotlin", "domain/src")) {
        inputs.dir(rootProject.layout.projectDirectory.dir(tree))
            .withPropertyName(tree.replace('/', '-'))
            .withPathSensitivity(PathSensitivity.RELATIVE)
    }
}
