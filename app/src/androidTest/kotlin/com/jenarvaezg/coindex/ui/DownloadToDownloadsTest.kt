package com.jenarvaezg.coindex.ui

import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A download reaches Descargas as a durable MediaStore entry (#285).
 *
 * The snackbar and the notification are the human face of it; this is the bit that has to survive
 * «Borrar caché» — the cache file is only the intermediate.
 */
@RunWith(AndroidJUnit4::class)
class DownloadToDownloadsTest {
    @Test
    fun aWrittenFileLandsInDownloadsUnderItsDatedName() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val displayName = datedExportFileName("coindex-test-download", "png")
        val cache = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
        val source = File(cache, "coindex-test-download.png").also { file ->
            file.writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))
        }

        handToDownloads(context, source, "image/png", displayName)

        val resolver = context.contentResolver
        resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.IS_PENDING,
            ),
            "${MediaStore.MediaColumns.DISPLAY_NAME}=?",
            arrayOf(displayName),
            null,
        ).use { cursor ->
            assertNotNull(cursor)
            assertTrue(cursor!!.moveToFirst())
            assertEquals(displayName, cursor.getString(0))
            assertEquals("image/png", cursor.getString(1))
            assertEquals(0, cursor.getInt(2))
        }

        resolver.delete(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            "${MediaStore.MediaColumns.DISPLAY_NAME}=?",
            arrayOf(displayName),
        )
    }
}
