package com.jenarvaezg.coindex.data

/**
 * How long a wall that is only Numista saying «not now» is left standing.
 *
 * **Six hours**, and the number is a floor and a ceiling in the same way `BARREN_STREAK_LIMIT` is.
 * Below it the phone goes back to paying for the same answer several times a day — eight launches
 * is what a day of his looks like, so an hour would keep six of them. Above it a throttle that
 * lifted in minutes would cost a whole day of prices, and the pass is the only thing that brings
 * them.
 *
 * It is the answer to «ahora no» and not to «este mes no», which is why the quota does not use it.
 */
private const val THROTTLE_WALL_MILLIS = 6L * 60 * 60 * 1_000

/**
 * Why Numista is refusing, which is the only thing that decides how long the refusal is believed.
 *
 * [com.jenarvaezg.coindex.data.prices.ValuationRefusal.Rejected] deliberately says none of this to
 * the collector — the sentence «Este teléfono» prints is the same for the four, and ADR 0028 §6.1
 * settled that it is not a button either. What the cause is for is the **clock**: the four wait on
 * four different things, and remembering a `429` until the 1st would cost a month of prices for a
 * throttle that lifted in minutes.
 */
enum class RejectionCause {
    /**
     * `403` — the quota **Numista** counts, which is not the one [CallBudgetGate] counts.
     *
     * Theirs is 2.000 a month against the 1.500 of ADR 0003, over a key that a second phone spends
     * too (#562), so this arrives with budget still on the local counter. Their month is the
     * calendar month, the same one [startOfMonthMillis] draws for the gate, so the wall falls on the
     * 1st — the very sentence «Este teléfono» already says of the local budget.
     */
    Quota,

    /**
     * `401` — the key is being refused, and no amount of waiting is going to fix a credential.
     *
     * The only wall with no clock at all. It falls when the collector saves the field in
     * «Credenciales», which is the gesture that means «prueba otra vez» — and it is the door ADR 0028
     * §6.1 already prints in this state, so the way out is on the screen that reports it.
     */
    Credentials,

    /** `429` — the throttle saying «ahora no», which is a matter of hours and never of a month. */
    Throttled,

    /**
     * Five answers in a row that left no row (#560), whatever their statuses.
     *
     * Nobody knows what this is, so it is believed for the **shortest** of the three lives: a run of
     * `5xx` while a shard of theirs restarts must not cost the collector a month of prices.
     */
    Unreadable,
}

/**
 * Whether a wall raised at [raisedAtMillis] is still standing at [nowMillis].
 *
 * Kept apart from the file it is stored in so the forgetting can be read and tested as what it is:
 * arithmetic on a timestamp, the same shape [com.jenarvaezg.coindex.data.photos.stillGone] has.
 */
fun rejectionStands(cause: RejectionCause, raisedAtMillis: Long, nowMillis: Long): Boolean =
    when (cause) {
        RejectionCause.Credentials -> true
        // The month turning over is the whole condition, and it is asked of the *same* function the
        // budget gate counts with: not «thirty days since», which would fall mid-month and ask again
        // into a quota that has not reset.
        RejectionCause.Quota -> startOfMonthMillis(nowMillis) <= raisedAtMillis
        RejectionCause.Throttled, RejectionCause.Unreadable ->
            nowMillis - raisedAtMillis in 0 until THROTTLE_WALL_MILLIS
    }

/**
 * The wall the valuation pass stopped against, remembered so it stops costing a call to find (#579).
 *
 * #560 taught the pass to stop against a wall; without this it does not **remember** having stopped,
 * so the next pass rediscovers it by paying for it. That is one call — or five, against a streak —
 * on every launch, every sync, every marked casilla and every notebook export: some 240 to 1.200 a
 * month of an allowance of 1.500, spent to learn what the phone already knew. And each of them is
 * counted by [CallBudgetGate] **before** it is sent, so a `403` over a quota that is gone keeps
 * eating the budget that is still there.
 *
 * It is [com.jenarvaezg.coindex.data.photos.PhotoRetryPolicy]'s `isGone` read over a pass instead of
 * a photograph: some refusals are worth another try in a moment, and some are worth writing down so
 * they stop being asked for.
 */
interface RejectionWall {
    /** The refusal still in the way at this moment, or null when there is nothing to stand against. */
    fun standing(): RejectionCause?

    /** Writes down that Numista refused, and why. A second raise restarts the clock. */
    fun raise(cause: RejectionCause)

    /** Takes it down: a pass that reached Numista, or a credential the collector has just changed. */
    fun clear()
}

/** The preferences file the wall lives in. */
const val REJECTION_WALL_PREFERENCES: String = "coindex-numista-wall"

private const val KEY_CAUSE = "rejection_cause"
private const val KEY_RAISED_AT = "rejection_raised_at"

/**
 * The wall on the device, because a pass runs on every launch (ADR 0028 §6).
 *
 * Surviving the launch is the whole point: `ValuationLoop.covered` already talks a second pass of one
 * process out of asking, and it is exactly what a cold start loses — so a wall that lived in memory
 * would be forgotten by the one trigger that fires most.
 *
 * Two named values and not one, because they are two different questions — *what* refused and *when*
 * — and the cause is what picks the clock. A cause written by a version this one does not know reads
 * back as no wall at all, which is a downgrade that costs one call rather than a crash.
 */
class StoredRejectionWall(
    private val values: NamedValues,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : RejectionWall {
    override fun standing(): RejectionCause? {
        val cause = stored() ?: return null
        val raisedAt = values.int64(KEY_RAISED_AT) ?: return null
        return cause.takeIf { rejectionStands(it, raisedAt, nowMillis()) }
    }

    override fun raise(cause: RejectionCause) = values.write(
        mapOf(
            KEY_CAUSE to Stored.Text(cause.name),
            KEY_RAISED_AT to Stored.Int64(nowMillis()),
        ),
    )

    // Read before written, so the pass that goes well on a phone that has never been refused does
    // not open the file to remove two keys that were never in it.
    override fun clear() {
        if (values.read(KEY_CAUSE) == null) return
        values.write(mapOf(KEY_CAUSE to null, KEY_RAISED_AT to null))
    }

    private fun stored(): RejectionCause? =
        values.text(KEY_CAUSE)?.let { name -> RejectionCause.entries.firstOrNull { it.name == name } }
}
