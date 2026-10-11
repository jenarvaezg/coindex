package com.jenarvaezg.coindex.ui.ficha

import com.jenarvaezg.coindex.ui.COIN_IN_ONE_COLLECTION
import com.jenarvaezg.coindex.ui.COIN_IN_SEVERAL_COLLECTIONS
import com.jenarvaezg.coindex.ui.CardDestination
import com.jenarvaezg.coindex.ui.shelf.CoinClaim

data class FichaClaim(val claim: CoinClaim, val here: Boolean)

/** Every collection that claims the coin, in index order, [here] included and marked (#635). */
internal fun fichaClaims(claims: List<CoinClaim>, here: CardDestination?): List<FichaClaim> =
    claims.map { FichaClaim(it, here = it.destination == here) }

/** Null with no claims: ADR 0026 §13 gives that coin no heading. */
internal fun fichaClaimsHeading(claims: List<FichaClaim>): String? = when (claims.size) {
    0 -> null
    1 -> COIN_IN_ONE_COLLECTION
    else -> COIN_IN_SEVERAL_COLLECTIONS
}
