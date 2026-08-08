package com.jenarvaezg.coindex.debug.calibration

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalibrationVariantIsolationTest {
    @Test
    fun `calibration activity is declared only in the debug manifest`() {
        val projectDir = File(requireNotNull(System.getProperty("user.dir")))
        val debugManifest = projectDir.resolve("src/debug/AndroidManifest.xml").readText()
        val mainSources = projectDir.resolve("src/main")

        assertTrue(debugManifest.contains(".debug.calibration.CalibrationActivity"))
        assertTrue(debugManifest.contains("android:exported=\"true\""))
        assertFalse(
            mainSources.walkTopDown()
                .filter(File::isFile)
                .any { it.readText().contains("CalibrationActivity") },
        )
    }
}
