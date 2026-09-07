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
 * The one class of the seam that needs a device, and the one risk of #546.
 *
 * `NamedValueStoresTest` pins what each store writes down to the key and the shape, against a fake
 * that cannot disagree with itself. What it cannot check is the half that matters on a phone that
 * updates: whether a shape written by the APK the collector already has comes back as the same
 * shape here. `SharedPreferences` stores a **typed** entry per key and a typed getter aimed at
 * another type throws rather than returning its default, so every one of these four mappings is a
 * chance to lose a notebook, a sync log or — worst — the credentials, and no JVM test can see it.
 *
 * So the writing is done here **the way the previous version did it**, straight through
 * `SharedPreferences`, and read back through the seam.
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

    /** The upgrade: what the stores of `main` wrote with the typed setters is what this one reads. */
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

    /**
     * A shape this seam does not carry is a null, and never a throw.
     *
     * Nothing writes a set of strings — but a `getString` aimed at one would have thrown, and the
     * store asking would have crashed the launch instead of falling back on its own default.
     */
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
