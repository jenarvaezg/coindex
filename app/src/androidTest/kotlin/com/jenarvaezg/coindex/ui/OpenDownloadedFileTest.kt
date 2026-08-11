package com.jenarvaezg.coindex.ui

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Abrir must not take the app down when nothing can open the file (#436).
 *
 * A made-up MIME type is the portable stand-in for a phone that shipped without a PDF viewer:
 * resolveActivity answers no, and startActivity is caught instead of crashing.
 */
@RunWith(AndroidJUnit4::class)
class OpenDownloadedFileTest {
    @Test
    fun anUnknownMimeIsNotOpenableAndDoesNotCrash() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = Uri.parse("content://downloads/coindex-no-viewer.bin")
        val mimeType = "application/x-coindex-no-viewer"

        assertFalse(canViewDownloadedFile(context, uri, mimeType))
        assertFalse(openDownloadedFile(context, uri, mimeType))
    }
}
