package com.jenarvaezg.coindex.data

/**
 * The part of the monthly budget (1500 by default, ADR 0003) the valuation pass may not spend, so
 * the inventory always can (#605). An inventory refresh is two calls, the token and the unpaginated
 * `/users/{id}/collected_items` (ADR 0006), so about 60 a month if daily; new fichas and the
 * ADR 0025 gesture add a few more. Without it, a runaway pass that hits the cap freezes the
 * inventory until the 1st, as in August 2026 (#560).
 *
 * Not a setting: the only figure the collector is promised is «+2 consultas al mes» per casilla
 * (ADR 0029 §5), and a second dial over the same allowance would muddle it.
 */
const val INVENTORY_RESERVE: Int = 300

/**
 * Whether this call comes from the valuation pass, the only spender with a lower ceiling. Only
 * [com.jenarvaezg.coindex.data.prices.ValuationPass] asks for the two `/issues` endpoints; the
 * token, collected items and `/types/{id}` belong to the sync and the ficha gesture. A new endpoint
 * would get the whole budget, so `BudgetReserveTest` pins the client's five.
 */
fun isValuationCall(endpoint: String): Boolean = endpoint.contains("/issues")

/** How much of [monthlyBudget] this endpoint may spend; never below zero for the pass. */
fun ceilingFor(endpoint: String, monthlyBudget: Int): Int =
    if (isValuationCall(endpoint)) (monthlyBudget - INVENTORY_RESERVE).coerceAtLeast(0) else monthlyBudget
