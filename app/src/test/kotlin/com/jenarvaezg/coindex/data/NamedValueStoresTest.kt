package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.ui.print.NotebookCodec
import com.jenarvaezg.coindex.ui.print.NotebookOptions
import com.jenarvaezg.coindex.ui.print.NotebookSwitch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The record the masthead reads, as one sync left it. */
private val RECORD = SyncRecord(
    atMillis = 1_724_000_000_000L,
    collectionItems = 58,
    typesFetched = 12,
    callsSpent = 13,
    partialFailure = "N#12345",
)

/**
 * The three stores that used to be an interface each, over the one seam they share (#546).
 *
 * Half of what is checked here is a **format pin**, and that is the point of the ticket: these three
 * were adapters over `SharedPreferences` with a fake apiece, and a phone that updates has to find
 * its notebook, its log and its credentials where it left them. So each store is asked not only
 * whether a value comes back, but under which key and in which shape it went in — a number stored 32
 * bits wide is not the same entry as one stored 64, and the file keeps the two apart.
 *
 * That the shapes then reach a real file as themselves is `SharedPreferenceValuesTest`'s, which is
 * instrumented: it is the half of the format that no JVM test can see.
 */
class NamedValueStoresTest {
    @Test
    fun `the notebook comes back the way it was printed`() {
        val values = FakeNamedValues()

        StoredNotebook(values).options = NotebookOptions(photographs = false, money = true)

        assertEquals(
            NotebookOptions(photographs = false, money = true),
            StoredNotebook(values).options,
        )
    }

    @Test
    fun `a switch nobody wrote reads back as its own default`() {
        val options = StoredNotebook(FakeNamedValues()).options

        assertEquals(NotebookOptions(), options)
        // The one that matters: an absent key silently becoming false is a notebook with no coins.
        assertTrue(options.photographs)
    }

    @Test
    fun `the notebook is stored as one flag per switch`() {
        val values = FakeNamedValues()

        StoredNotebook(values).options = NotebookOptions(photographs = false)

        assertEquals(
            Stored.Flag(false),
            values.read(NotebookCodec.key(NotebookSwitch.Photographs)),
        )
        assertEquals(Stored.Flag(true), values.read(NotebookCodec.key(NotebookSwitch.ActualSize)))
        assertEquals(NotebookSwitch.entries.size, values.entries.size)
    }

    @Test
    fun `a notebook written by the previous version is still read`() {
        val values = FakeNamedValues(
            mapOf(NotebookCodec.key(NotebookSwitch.Money) to Stored.Flag(true)),
        )

        assertTrue(StoredNotebook(values).options.money)
    }

    @Test
    fun `the log comes back the way the sync left it`() {
        val values = FakeNamedValues()

        StoredSyncLog(values).last = RECORD

        assertEquals(RECORD, StoredSyncLog(values).last)
    }

    @Test
    fun `an empty log is no record at all`() {
        assertNull(StoredSyncLog(FakeNamedValues()).last)
    }

    @Test
    fun `forgetting the log leaves nothing behind`() {
        val values = FakeNamedValues()
        val log = StoredSyncLog(values)
        log.last = RECORD

        log.last = null

        assertNull(log.last)
        assertTrue(values.entries.isEmpty())
    }

    @Test
    fun `the log keeps the widths it already had`() {
        val values = FakeNamedValues()

        StoredSyncLog(values).last = RECORD

        assertEquals(Stored.Int64(1_724_000_000_000L), values.read("last_sync_at"))
        assertEquals(Stored.Int32(58), values.read("last_sync_items"))
        assertEquals(Stored.Int32(12), values.read("last_sync_types"))
        assertEquals(Stored.Int32(13), values.read("last_sync_calls"))
        assertEquals(Stored.Text("N#12345"), values.read("last_sync_partial"))
    }

    @Test
    fun `a sync that failed nowhere leaves no partial failure behind`() {
        val values = FakeNamedValues(mapOf("last_sync_partial" to Stored.Text("N#1")))

        StoredSyncLog(values).last = RECORD.copy(partialFailure = null)

        assertNull(values.read("last_sync_partial"))
        assertNull(StoredSyncLog(values).last?.partialFailure)
    }

    @Test
    fun `the credentials come back decrypted`() {
        val store = credentialsOnJvm()

        store.save(apiKey = "clave", userId = 2104)

        assertEquals(Credentials("clave", 2104), store.credentials())
    }

    @Test
    fun `the api key is stored encrypted and the user id as it is`() {
        val values = FakeNamedValues()

        credentialsOnJvm(values).save(apiKey = "clave", userId = 2104)

        val stored = values.read("numista_api_key") as Stored.Text
        assertFalse(stored.value.contains("clave"))
        assertEquals(Stored.Int64(2104), values.read("numista_user_id"))
    }

    @Test
    fun `signing out forgets both keys`() {
        val values = FakeNamedValues()
        val store = credentialsOnJvm(values)
        store.save(apiKey = "clave", userId = 2104)

        store.clear()

        assertNull(store.credentials())
        assertTrue(values.entries.isEmpty())
    }

    @Test
    fun `a ciphertext this install cannot read is no credentials`() {
        val values = FakeNamedValues(
            mapOf(
                "numista_api_key" to Stored.Text("esto no es base64 de nadie"),
                "numista_user_id" to Stored.Int64(2104),
            ),
        )

        assertNull(credentialsOnJvm(values).credentials())
    }

    @Test
    fun `a stored user id with no key at all is no credentials`() {
        val values = FakeNamedValues(mapOf("numista_user_id" to Stored.Int64(2104)))

        assertNull(credentialsOnJvm(values).credentials())
    }
}
