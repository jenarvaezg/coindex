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
    // Only above one, which also keeps the sheet of an empty casilla from saying «×0» (#508).
    "×${row.quantity}".takeIf { row.quantity > 1 },
).joinToString(" · ")

/**
 * A coin's Numista number as the app writes it: on a sheet's identity line, and as the title of a
 * type with no ficha on this phone (see `typeTitle`).
 */
fun numistaCodeLabel(typeId: Int): String = "N# $typeId"
