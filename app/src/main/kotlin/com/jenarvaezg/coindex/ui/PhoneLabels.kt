package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.prices.ValuationRefusal
import com.jenarvaezg.coindex.data.prices.ValuationStatus

/**
 * Everything «Este teléfono» says (#521). ADR 0026 §5 lets it explain at length because it is
 * visited only to sync or to see why a queue is held; in exchange, none of these explanations may
 * appear on a notebook screen. The sync label and the status lines ([syncActionLabel],
 * [photoCacheLabel], [valuationLabel]) belong to the subjects that produce them.
 */

const val PHOTO_CACHE_HEADING: String = "Fotos del catálogo"
const val VALUATION_HEADING: String = "Precios de catálogo"

/**
 * The raw database leaving the phone (#548). The explanation says what the file is for, loading
 * elsewhere, unlike a lámina export meant to be looked at, and why it is safe to send: everything
 * the phone holds, without the API key. It stays on this screen when sign-out moves to
 * «Credenciales» (#521).
 */
const val DATA_EXPORT_ACTION: String = "Exportar datos"
const val DATA_EXPORT_EXPLANATION: String =
    "Comparte una copia de la base de datos de este teléfono: la colección, las fichas y los " +
        "precios, en un solo fichero para cargar en otro sitio. La API key no va dentro."

/**
 * The same button while the copy is written: nothing shows until the chooser opens, and a second
 * tap would open a second chooser.
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
 * Whether the valuation card adds a row into «Credenciales» (ADR 0028 §6.1): only when the key is
 * missing or Numista refuses it. The other refusals are waits nobody can act on. The conditions
 * follow [valuationLabel]'s branches, so the row never hangs under a line reporting no refusal.
 */
fun valuationBlamesCredentials(valuation: ValuationStatus): Boolean =
    valuation.wanted > 0 &&
        !valuation.settled &&
        (valuation.held == ValuationRefusal.NoApiKey || valuation.held == ValuationRefusal.Rejected)
