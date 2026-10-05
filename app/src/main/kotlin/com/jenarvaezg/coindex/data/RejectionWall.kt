package com.jenarvaezg.coindex.data

/**
 * How long a «not now» from Numista is believed. Shorter, and the phone pays for the same answer
 * several times a day; longer, and a throttle that lifted in minutes costs a day of prices. The quota
 * («not this month») has its own clock.
 */
private const val THROTTLE_WALL_MILLIS = 6L * 60 * 60 * 1_000

/**
 * Why Numista is refusing, which decides only how long the refusal is believed. The collector sees
 * the same sentence for all four, and no button (ADR 0028 §6.1).
 */
enum class RejectionCause {
    /**
     * `429` with the quota named in its body: Numista's own monthly count, not [CallBudgetGate]'s.
     * The key is shared with another phone (#562), so this can arrive with local budget left. It
     * lasts until the 1st, the same calendar month [startOfMonthMillis] draws for the gate.
     *
     * The body is the only thing that tells it from [Throttled] (#600). Numista's OpenAPI 3.36
     * declares the `429` as «too many simultaneous requests or you reached the limit of your monthly
     * quota» on every route the app uses, and documents no body; the marker comes from the one
     * observed answer, `HTTP 429 «Quota exceeded»` from `/types/{id}` to
     * `scripts/seed-type-cache.py --refresh` on 14 August 2026. A body without it falls back to the
     * six-hour wall.
     */
    Quota,

    /**
     * `401` or `403`: the key is refused, and waiting won't fix it. No clock: the wall falls when the
     * collector saves «Credenciales», the way out ADR 0028 §6.1 already shows in this state.
     *
     * A `403` is not the quota (#600). The OpenAPI 3.36 declares it only on two paid routes the app
     * never calls (`/types/{type_id}/sales_records`, `/search_by_image`: «Your API key is not
     * activated for using this API endpoint»), so on the app's routes it means a revoked key or
     * Cloudflare. `syncErrorLabel` reads it the same way.
     */
    Credentials,

    /**
     * `429` without the quota in its body: a throttle, lasting hours. A bare `429` can mean either,
     * so the short reading is assumed: a quota taken for a throttle costs one call every six hours,
     * the reverse would cost a month of prices.
     */
    Throttled,

    /**
     * Five answers in a row that wrote nothing (#560), whatever their statuses. The cause is unknown,
     * so it gets the shortest clock: a run of `5xx` must not cost a month of prices.
     */
    Unreadable,
}

/** The statuses Numista refuses with. */
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_TOO_MANY_REQUESTS = 429

/** What a quota `429` carried in its body, the one time it was observed (#600). */
private const val QUOTA_MARKER = "quota"

/**
 * What a refused answer means, and so which clock it is believed on. Shared by the valuation pass
 * (the wall's clock) and [com.jenarvaezg.coindex.ui.syncErrorLabel] (the sentence) so both read each
 * status the same way. Null means it isn't Numista refusing.
 */
fun rejectionCauseFor(status: Int, body: String): RejectionCause? = when (status) {
    HTTP_TOO_MANY_REQUESTS ->
        if (body.contains(QUOTA_MARKER, ignoreCase = true)) RejectionCause.Quota else RejectionCause.Throttled
    HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> RejectionCause.Credentials
    else -> null
}

/** Whether a wall raised at [raisedAtMillis] still stands at [nowMillis]. */
fun rejectionStands(cause: RejectionCause, raisedAtMillis: Long, nowMillis: Long): Boolean =
    when (cause) {
        RejectionCause.Credentials -> true
        // The quota resets with the calendar month, counted as the budget gate counts it; «thirty
        // days since» would fall mid-month, into a quota that hasn't reset.
        RejectionCause.Quota -> startOfMonthMillis(nowMillis) <= raisedAtMillis
        RejectionCause.Throttled, RejectionCause.Unreadable ->
            nowMillis - raisedAtMillis in 0 until THROTTLE_WALL_MILLIS
    }

/**
 * The refusal the valuation pass stopped against, remembered so that finding it again doesn't cost a
 * call on every launch, sync, marked casilla and export (#579). [CallBudgetGate] counts those calls
 * before sending them, so without the wall a refused key keeps eating the local budget. It is
 * `PhotoRetryPolicy`'s `isGone` applied to a pass.
 */
interface RejectionWall {
    /** The refusal still standing, or null. */
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
 * The wall on the device, so it survives the launch: a pass runs on every launch (ADR 0028 §6), and
 * a cold start loses `ValuationLoop.covered`. A cause this version doesn't know reads as no wall,
 * which costs one call rather than a crash.
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

    // Checked first, so a phone that was never refused doesn't rewrite the file on every pass.
    override fun clear() {
        if (values.read(KEY_CAUSE) == null) return
        values.write(mapOf(KEY_CAUSE to null, KEY_RAISED_AT to null))
    }

    private fun stored(): RejectionCause? =
        values.text(KEY_CAUSE)?.let { name -> RejectionCause.entries.firstOrNull { it.name == name } }
}
