package com.jenarvaezg.coindex.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val NOW = 1_754_600_000_000L
private const val HOUR = 60L * 60 * 1_000
private const val DAY = 24L * HOUR

/** The first millisecond of the month after [NOW], computed as the budget gate does. */
private val NEXT_MONTH = startOfMonthMillis(NOW + 40 * DAY)

/**
 * The rejection wall of #579: which cause it believes, for how long, and how it is stored. Each
 * cause is checked at its last moment standing and its first moment down: falling too soon spends
 * calls, and never falling switches prices off on this phone for good.
 */
class RejectionWallTest {
    /**
     * Numista's contract declares `429` both for «too many simultaneous requests» and for an
     * exhausted monthly quota, so only the body tells them apart; the one exhausted month measured
     * answered `429 «Quota exceeded»` (#600). A `429` without that body counts as the throttle: if
     * wrong, it costs six hours of prices instead of a month.
     */
    @Test
    fun `the status alone does not say which refusal it is`() {
        assertEquals(RejectionCause.Quota, rejectionCauseFor(429, "Quota exceeded"))
        assertEquals(RejectionCause.Quota, rejectionCauseFor(429, """{"error":"quota exceeded"}"""))
        assertEquals(RejectionCause.Throttled, rejectionCauseFor(429, "Too many simultaneous requests"))
        assertEquals(RejectionCause.Throttled, rejectionCauseFor(429, ""))
        assertEquals(RejectionCause.Credentials, rejectionCauseFor(401, ""))
        assertEquals(RejectionCause.Credentials, rejectionCauseFor(403, "not activated for this endpoint"))
        assertNull(rejectionCauseFor(404, "Quota exceeded"), "un 404 no es la cuota aunque lo diga")
        assertNull(rejectionCauseFor(500, ""))
    }

    @Test
    fun `the wall of a refused key is never taken down by the clock`() {
        assertTrue(rejectionStands(RejectionCause.Credentials, NOW, NOW + 365 * DAY))
    }

    /** Numista's quota resets with the calendar month, not «thirty days since» the refusal. */
    @Test
    fun `the wall of the quota stands until the first of the next month`() {
        assertTrue(rejectionStands(RejectionCause.Quota, NOW, NEXT_MONTH - 1))
        assertFalse(rejectionStands(RejectionCause.Quota, NOW, NEXT_MONTH))
    }

    @Test
    fun `the throttle and the unreadable run are believed for six hours`() {
        assertTrue(rejectionStands(RejectionCause.Throttled, NOW, NOW + 6 * HOUR - 1))
        assertFalse(rejectionStands(RejectionCause.Throttled, NOW, NOW + 6 * HOUR))
        assertTrue(rejectionStands(RejectionCause.Unreadable, NOW, NOW + 6 * HOUR - 1))
        assertFalse(rejectionStands(RejectionCause.Unreadable, NOW, NOW + 6 * HOUR))
    }

    /**
     * A clock set back would otherwise leave a wall outliving its six hours by however far it
     * moved; falling early only costs one call.
     */
    @Test
    fun `a wall raised in the future is no wall at all`() {
        assertFalse(rejectionStands(RejectionCause.Throttled, NOW, NOW - DAY))
    }

    /** A cold start is a new instance over the same stored values. */
    @Test
    fun `the wall survives the launch that raised it`() {
        val values = FakeNamedValues()

        StoredRejectionWall(values) { NOW }.raise(RejectionCause.Quota)

        assertEquals(RejectionCause.Quota, StoredRejectionWall(values) { NOW + DAY }.standing())
        assertNull(StoredRejectionWall(values) { NEXT_MONTH }.standing())
    }

    /**
     * Format pin: an updated app must find the wall where it was, and a 32-bit number is not the
     * same entry as a 64-bit one ([Stored]).
     */
    @Test
    fun `the wall is one text and one 64-bit stamp`() {
        val values = FakeNamedValues()

        StoredRejectionWall(values) { NOW }.raise(RejectionCause.Credentials)

        assertEquals(
            mapOf(
                "rejection_cause" to Stored.Text("Credentials"),
                "rejection_raised_at" to Stored.Int64(NOW),
            ),
            values.entries,
        )
    }

    /** A cause from another version (a downgrade, or a newer cause) costs one call, not a crash. */
    @Test
    fun `a cause nobody recognises is not believed`() {
        val values = FakeNamedValues(
            mapOf(
                "rejection_cause" to Stored.Text("Sabotage"),
                "rejection_raised_at" to Stored.Int64(NOW),
            ),
        )

        assertNull(StoredRejectionWall(values) { NOW }.standing())
    }

    @Test
    fun `half a wall is not a wall`() {
        val orphanStamp = FakeNamedValues(mapOf("rejection_raised_at" to Stored.Int64(NOW)))
        val orphanCause = FakeNamedValues(mapOf("rejection_cause" to Stored.Text("Quota")))

        assertNull(StoredRejectionWall(orphanStamp) { NOW }.standing())
        assertNull(StoredRejectionWall(orphanCause) { NOW }.standing())
    }

    @Test
    fun `clearing an empty wall writes nothing`() {
        val values = FakeNamedValues()

        StoredRejectionWall(values) { NOW }.clear()

        assertTrue(values.entries.isEmpty())
    }

    /**
     * Saving credentials takes down any wall (#579): it is the only way out of the credentials
     * wall, and a different key brings its own quota (#562).
     */
    @Test
    fun `a key the collector has just saved takes the wall down`() {
        val values = FakeNamedValues()
        val wall = StoredRejectionWall(values) { NOW }
        wall.raise(RejectionCause.Credentials)

        credentialsOnJvm(wall = wall).save(apiKey = "otra", userId = 2104)

        assertNull(wall.standing(), "guardar la clave es «prueba otra vez», y vale para las cuatro causas")
        assertTrue(values.entries.isEmpty())
    }
}
