package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.prices.ValuationRefusal
import com.jenarvaezg.coindex.data.prices.ValuationStatus

/**
 * Everything «Este teléfono» says — the maintenance of the inventory (#521).
 *
 * One of the screens §5 exempts **by the frequency rule**: it is visited to sync and to read why a
 * queue is held, a paragraph there costs once per visit, and avoiding a phone call pays for it. In
 * exchange it is watched the other way round: **none of these explanations may appear on a notebook
 * screen**.
 *
 * What is **not** here is the sync's own label ([syncActionLabel]) and the two status lines
 * ([photoCacheLabel], [valuationLabel]): they are owned by the subjects that produce them, and this
 * screen is where they are read rather than where they are written.
 */

const val PHOTO_CACHE_HEADING: String = "Fotos del catálogo"
const val VALUATION_HEADING: String = "Precios de catálogo"

/**
 * The raw base leaving the phone, which is a card with one line and one button (#548).
 *
 * The one word it has to say is **what the file is for**, because «Exportar datos» next to «Exportar
 * lámina» reads as the same gesture with a wider subject, and it is not: a lámina is made for a
 * person to look at and this is the base itself, made for a machine to load. The line names the two
 * things that make it safe to send — everything the phone holds, and not the API key.
 *
 * It stays on this screen when the credentials go down one (#521): what leaves is a copy of what
 * this phone holds, which is this screen's whole subject. It sat beside `Cerrar sesión` because both
 * were maintenance; only one of the two still is.
 */
const val DATA_EXPORT_ACTION: String = "Exportar datos"
const val DATA_EXPORT_EXPLANATION: String =
    "Comparte una copia de la base de datos de este teléfono: la colección, las fichas y los " +
        "precios, en un solo fichero para cargar en otro sitio. La API key no va dentro."

/**
 * The same button while the copy is being written.
 *
 * The base is a few megabytes and the copy is quick, but nothing appears on screen until the chooser
 * opens, so the gap is long enough for a second tap — and two taps are two choosers. The button says
 * what it is doing rather than going quietly grey.
 */
fun dataExportLabel(exporting: Boolean): String =
    if (exporting) "Exportando…" else DATA_EXPORT_ACTION

/** The chooser's own title, which is the only word Android puts over the list of destinations. */
const val DATA_EXPORT_CHOOSER_TITLE: String = "Compartir los datos"

/** Said when the copy could not be written, with whatever the failure had to say for itself. */
fun dataExportFailure(cause: String?): String =
    if (cause.isNullOrBlank()) {
        "No se pudieron exportar los datos."
    } else {
        "No se pudieron exportar los datos: $cause"
    }

/**
 * Whether the valuation card grows a door into «Credenciales» (ADR 0028 §6.1).
 *
 * Two of the six states this card can be in, and only those two, have a cause the collector can act
 * on: the key is missing, or Numista is refusing it. The other four say «wait» — for the network, for
 * the sync, for the 1st of the month — and the pass has no handle by design, so a row on those would
 * be a door to somewhere that changes nothing.
 *
 * The three conditions are [valuationLabel]'s own branches, in its order: with nothing to price and
 * with the prices settled that function never reaches its refusal, so a door printed there would
 * hang under a line that is not complaining about anything.
 */
fun valuationBlamesCredentials(valuation: ValuationStatus): Boolean =
    valuation.wanted > 0 &&
        !valuation.settled &&
        (valuation.held == ValuationRefusal.NoApiKey || valuation.held == ValuationRefusal.Rejected)
