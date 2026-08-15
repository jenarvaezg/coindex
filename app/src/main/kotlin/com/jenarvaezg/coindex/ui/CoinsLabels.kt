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
    // Absent on a coin the collector holds none of, which is what a casilla of a lámina opens the
    // sheet of half the time (#508): «×0» would be a count of nothing dressed as a count.
    "×${row.quantity}".takeIf { row.quantity > 1 },
).joinToString(" · ")

/**
 * A coin's Numista number, said the one way the app says it.
 *
 * Two readers: the identity line of a sheet, and the title of a type no ficha on this phone names —
 * where it is the only name such a coin has (see `typeTitle`).
 */
fun numistaCodeLabel(typeId: Int): String = "N# $typeId"
