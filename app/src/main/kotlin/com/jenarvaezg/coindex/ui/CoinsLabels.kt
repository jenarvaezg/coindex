package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.ui.shelf.CoinRow
import com.jenarvaezg.coindex.ui.shelf.coinYearsLabel

const val COIN_VIEW_ON_NUMISTA: String = "Ver en Numista"
const val COIN_IN_ONE_COLLECTION: String = "En esta colección"
const val COIN_IN_SEVERAL_COLLECTIONS: String = "En estas colecciones"

fun coinFichaIdentity(row: CoinRow): String = listOfNotNull(
    row.issuer,
    coinYearsLabel(row.years),
    numistaCodeLabel(row.typeId),
    objectClassLabel(row.objectClass),
    // The guard that stops «×1» is what keeps a hole quiet too, now that a casilla of a lámina opens
    // this sheet and half of them hold nothing (#508): zero is not greater than one, so nothing is
    // said — and «×0» would have been a count of nothing dressed as a count.
    "×${row.quantity}".takeIf { row.quantity > 1 },
).joinToString(" · ")

/**
 * A coin's Numista number, said the one way the app says it.
 *
 * Two readers: the identity line of a sheet, and the title of a type no ficha on this phone names —
 * where it is the only name such a coin has (see `typeTitle`).
 */
fun numistaCodeLabel(typeId: Int): String = "N# $typeId"
