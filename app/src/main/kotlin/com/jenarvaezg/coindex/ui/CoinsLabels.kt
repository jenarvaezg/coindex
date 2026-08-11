package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.ui.shelf.CoinRow
import com.jenarvaezg.coindex.ui.shelf.coinYearsLabel

const val COIN_VIEW_ON_NUMISTA: String = "Ver en Numista"
const val COIN_IN_ONE_COLLECTION: String = "En esta colección"
const val COIN_IN_SEVERAL_COLLECTIONS: String = "En estas colecciones"

fun coinFichaIdentity(row: CoinRow): String = listOfNotNull(
    row.issuer,
    coinYearsLabel(row.years),
    "N# ${row.typeId}",
    objectClassLabel(row.objectClass),
    "×${row.quantity}".takeIf { row.quantity > 1 },
).joinToString(" · ")
