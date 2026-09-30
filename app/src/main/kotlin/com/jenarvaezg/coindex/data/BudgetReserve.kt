package com.jenarvaezg.coindex.data

/**
 * The slice of the month the valuation pass may not touch, so the inventory always can (#605).
 *
 * **300 of the 1.500 of ADR 0003**, and the number comes from what a month of the cheap things
 * actually costs rather than from a proportion that reads well. A refresh of the inventory is two
 * consultas — the token and `/users/{id}/collected_items`, which is unpaginated (ADR 0006) — so a
 * daily one is 60 a month; the fichas of what he buys are a handful more; and the gesture of
 * ADR 0025 is one each. 300 leaves that with room to spare and still hands the pass 1.200, which is
 * two and a half times the 442 of a cold month.
 *
 * What it is for is the shape of August 2026, which is in both phones' `api_call_log`: on the 11th
 * the father's spent 1.484 consultas in a single day against the same plan (#560), hit the cap, and
 * from then until the 1st the gate refused **everything** — including the two consultas that would
 * have told him what he owned. His inventory stayed frozen from 10 August. The bug is fixed; the
 * reason a bug in the expensive thing could freeze the cheap thing is not, and that is this.
 *
 * It is deliberately not a setting. The collector has no way to know what 300 buys, and the one
 * figure the app already promises him is «+2 consultas al mes» per casilla (ADR 0029 §5) — a second
 * dial over the same allowance would make that promise unreadable.
 */
const val INVENTORY_RESERVE: Int = 300

/**
 * Whether this call is the valuation pass asking, which is the only spender with a ceiling.
 *
 * The endpoint is the purpose here, and not by coincidence: `NumistaClient` has one method per
 * endpoint, and the two that carry `/issues` — `/types/{id}/issues` and
 * `/types/{id}/issues/{issue_id}/prices` — are asked for by [com.jenarvaezg.coindex.data.prices.ValuationPass]
 * and by nothing else. `/oauth_token`, `/users/{id}/collected_items` and `/types/{id}` belong to the
 * sync and to the ficha gesture, which are what the reserve exists to keep possible.
 *
 * A sixth endpoint added to the client without a thought here would get the whole budget, so
 * `BudgetReserveTest` pins the five that exist against the strings the client builds.
 */
fun isValuationCall(endpoint: String): Boolean = endpoint.contains("/issues")

/**
 * How much of [monthlyBudget] this endpoint may spend up to.
 *
 * A budget smaller than the reserve leaves the pass at zero rather than below it, which is the
 * honest reading of «there is nothing here that is not spoken for».
 */
fun ceilingFor(endpoint: String, monthlyBudget: Int): Int =
    if (isValuationCall(endpoint)) (monthlyBudget - INVENTORY_RESERVE).coerceAtLeast(0) else monthlyBudget
