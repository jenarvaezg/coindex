package com.jenarvaezg.coindex.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val NOW = 1_754_600_000_000L
private const val HOUR = 60L * 60 * 1_000
private const val DAY = 24L * HOUR

/** The first millisecond of the month after [NOW], drawn the way the budget gate draws its own. */
private val NEXT_MONTH = startOfMonthMillis(NOW + 40 * DAY)

/**
 * The wall of #579: what it believes, for how long, and where it writes it down.
 *
 * Half of this is the **forgetting**, which is the half that costs the collector if it is wrong in
 * either direction — a wall that falls too soon buys back the 240 calls a month the ticket exists to
 * save, and one that never falls switches the prices off on this phone permanently and invisibly. So
 * each of the four causes is asked for its last moment standing and its first moment down.
 */
class RejectionWallTest {
    /**
     * Which answer means what, which is the whole of #600 and the reason it is one function.
     *
     * The published contract (OpenAPI 3.36, read on 7 September 2026) declares the `429` on all five
     * routes this app asks for, as «too many simultaneous requests **or** you reached the limit of
     * your monthly quota», and the `403` on two paid routes it never asks for, as a permission. So
     * the status alone cannot pick a clock, and the body is all there is: the one exhausted month
     * anybody measured — 14 August 2026, Jose's key, `scripts/seed-type-cache.py --refresh` over
     * `/types/{id}` — came back as `HTTP 429 «Quota exceeded»`.
     *
     * A `429` whose body stops saying it is read as the throttle, deliberately: the doubt costs six
     * hours of prices that way, and a month of them the other.
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

    /** A credential is not a matter of time, so no amount of it takes this wall down. */
    @Test
    fun `the wall of a refused key is never taken down by the clock`() {
        assertTrue(rejectionStands(RejectionCause.Credentials, NOW, NOW + 365 * DAY))
    }

    /**
     * The quota is Numista's own, and their month is the calendar month the gate already counts.
     *
     * Not «thirty days since», which would fall mid-month and spend a call into an allowance that has
     * not reset — and would then fall again thirty days after that, drifting further off the 1st every
     * time.
     */
    @Test
    fun `the wall of the quota stands until the first of the next month`() {
        assertTrue(rejectionStands(RejectionCause.Quota, NOW, NEXT_MONTH - 1))
        assertFalse(rejectionStands(RejectionCause.Quota, NOW, NEXT_MONTH))
    }

    /** A throttle says «ahora no» and is believed for six hours, not for a month. */
    @Test
    fun `the throttle and the unreadable run are believed for six hours`() {
        assertTrue(rejectionStands(RejectionCause.Throttled, NOW, NOW + 6 * HOUR - 1))
        assertFalse(rejectionStands(RejectionCause.Throttled, NOW, NOW + 6 * HOUR))
        assertTrue(rejectionStands(RejectionCause.Unreadable, NOW, NOW + 6 * HOUR - 1))
        assertFalse(rejectionStands(RejectionCause.Unreadable, NOW, NOW + 6 * HOUR))
    }

    /**
     * A clock that has gone backwards takes the wall down rather than leaving it for ever.
     *
     * The collector who corrects a badly set date, or a phone that picks up the network time, would
     * otherwise be left with a `raisedAt` in the future and a wall that outlives the six hours by
     * however far the clock moved. Falling early costs one call; not falling costs the prices.
     */
    @Test
    fun `a wall raised in the future is no wall at all`() {
        assertFalse(rejectionStands(RejectionCause.Throttled, NOW, NOW - DAY))
    }

    /** What a cold start is: another instance over the same file, and the wall is still there. */
    @Test
    fun `the wall survives the launch that raised it`() {
        val values = FakeNamedValues()

        StoredRejectionWall(values) { NOW }.raise(RejectionCause.Quota)

        assertEquals(RejectionCause.Quota, StoredRejectionWall(values) { NOW + DAY }.standing())
        assertNull(StoredRejectionWall(values) { NEXT_MONTH }.standing())
    }

    /**
     * The format pin: which key, and in which shape.
     *
     * A phone that updates has to find the wall where it left it, and a number stored 32 bits wide is
     * not the same entry as one stored 64 (see [Stored]).
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

    /**
     * A cause this version does not know reads back as no wall, and costs one call to find out.
     *
     * The alternative is a downgrade — or a file written by a version that added a fifth cause —
     * taking the app down on a launch, which is a great deal worse than one call.
     */
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

    /** A stamp with no cause, or a cause with no stamp, is half a wall and is not one. */
    @Test
    fun `half a wall is not a wall`() {
        val orphanStamp = FakeNamedValues(mapOf("rejection_raised_at" to Stored.Int64(NOW)))
        val orphanCause = FakeNamedValues(mapOf("rejection_cause" to Stored.Text("Quota")))

        assertNull(StoredRejectionWall(orphanStamp) { NOW }.standing())
        assertNull(StoredRejectionWall(orphanCause) { NOW }.standing())
    }

    /** Taking down a wall that was never raised does not open the file to remove nothing. */
    @Test
    fun `clearing an empty wall writes nothing`() {
        val values = FakeNamedValues()

        StoredRejectionWall(values) { NOW }.clear()

        assertTrue(values.entries.isEmpty())
    }

    /**
     * Saving the credentials takes the wall down, whatever raised it — the criterion of #579.
     *
     * It is the only thing that ends the credentials wall, which has no clock; and it ends the quota
     * too, because a second key is exactly what undoes an allowance shared with another phone (#562).
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
