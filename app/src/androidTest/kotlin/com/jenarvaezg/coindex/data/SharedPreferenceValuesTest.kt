package com.jenarvaezg.coindex.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

private const val FILE = "coindex-named-values-test"

/**
 * The device half of #546: `NamedValueStoresTest` pins keys and shapes against a fake, but only a
 * real `SharedPreferences` shows whether what the installed APK wrote reads back the same. A typed
 * getter aimed at another type throws instead of returning its default, which on update could lose
 * a notebook, a sync log or the credentials. So values are written straight through
 * `SharedPreferences`, as the previous version did, and read back through the seam.
 */
@RunWith(AndroidJUnit4::class)
class SharedPreferenceValuesTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val values = SharedPreferenceValues(context, FILE)

    @After
    fun cleanUp() {
        prefs.edit().clear().commit()
    }

    @Test
    fun everyShapeComesBackAsTheShapeItWentIn() {
        values.write(
            mapOf(
                "text" to Stored.Text("N#12345"),
                "flag" to Stored.Flag(true),
                "int32" to Stored.Int32(58),
                "int64" to Stored.Int64(1_724_000_000_000L),
            ),
        )

        assertEquals(Stored.Text("N#12345"), values.read("text"))
        assertEquals(Stored.Flag(true), values.read("flag"))
        assertEquals(Stored.Int32(58), values.read("int32"))
        assertEquals(Stored.Int64(1_724_000_000_000L), values.read("int64"))
    }

    @Test
    fun whatThePreviousVersionWroteIsWhatTheSeamReads() {
        prefs.edit()
            .putString("last_sync_partial", "N#12345")
            .putBoolean("notebook_photographs", false)
            .putInt("last_sync_items", 58)
            .putLong("last_sync_at", 1_724_000_000_000L)
            .commit()

        assertEquals(Stored.Text("N#12345"), values.read("last_sync_partial"))
        assertEquals(Stored.Flag(false), values.read("notebook_photographs"))
        assertEquals(Stored.Int32(58), values.read("last_sync_items"))
        assertEquals(Stored.Int64(1_724_000_000_000L), values.read("last_sync_at"))
    }

    @Test
    fun aNullIsARemovalAndNotAnEmptyValue() {
        values.write(mapOf("text" to Stored.Text("Venezuela")))

        values.write(mapOf("text" to null))

        assertNull(values.read("text"))
    }

    /** Nothing writes a string set, but a `getString` aimed at one would throw at launch. */
    @Test
    fun aShapeTheSeamDoesNotCarryIsAbsent() {
        prefs.edit().putStringSet("shelves", setOf("Venezuela")).commit()

        assertNull(values.read("shelves"))
    }

    @Test
    fun aKeyNobodyWroteIsAbsent() {
        assertNull(values.read("nobody"))
    }
}
