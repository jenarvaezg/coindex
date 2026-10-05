package com.jenarvaezg.coindex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jenarvaezg.coindex.data.photos.PhotoCacheStatus
import com.jenarvaezg.coindex.data.prices.ValuationStatus
import com.jenarvaezg.coindex.ui.CREDENTIALS_LABEL
import com.jenarvaezg.coindex.ui.DATA_EXPORT_EXPLANATION
import com.jenarvaezg.coindex.ui.NOTICES_LABEL
import com.jenarvaezg.coindex.ui.PHOTO_CACHE_HEADING
import com.jenarvaezg.coindex.ui.VALUATION_HEADING
import com.jenarvaezg.coindex.ui.components.CardAction
import com.jenarvaezg.coindex.ui.components.FieldCard
import com.jenarvaezg.coindex.ui.components.PrimaryAction
import com.jenarvaezg.coindex.ui.dataExportLabel
import com.jenarvaezg.coindex.ui.photoCacheLabel
import com.jenarvaezg.coindex.ui.syncActionLabel
import com.jenarvaezg.coindex.ui.theme.Paper
import com.jenarvaezg.coindex.ui.valuationBlamesCredentials
import com.jenarvaezg.coindex.ui.valuationLabel

/**
 * «Este teléfono»: what the phone holds and how it is kept up to date (#521). Formerly «Ajustes»,
 * renamed because most of it is maintenance, not configuration.
 *
 * «Sincronizar» comes first as the filled action, then the photo and valuation cards that report on
 * it, then the data export, then two links at the foot. The credentials moved one screen down, in
 * the shape of ADR 0026 §14; the valuation card links to them when the key is the problem.
 */
@Composable
fun PhoneScreen(
    photoCache: PhotoCacheStatus,
    /** The valuation pass's progress, and why it is held if it is (ADR 0028 §6). */
    valuation: ValuationStatus,
    /**
     * What the marked casillas add to the monthly pass, or null when nothing is marked
     * (ADR 0029 §5). Shown on the valuation card, which is where the pass's cost is stated.
     */
    wishSpend: String?,
    syncing: Boolean,
    /**
     * Whether the raw dump (#548) is being written; disables the button so two taps don't open two
     * choosers.
     */
    exporting: Boolean,
    onSync: () -> Unit,
    onExportData: () -> Unit,
    onOpenCredentials: () -> Unit,
    onOpenNotices: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // No heading: the top bar already says «Este teléfono» (ADR 0026 §5).
        PrimaryAction(
            text = syncActionLabel(syncing),
            onClick = onSync,
            enabled = !syncing,
        )

        // The only place the background photo prefetch reports (#191): «downloading» and «waiting
        // for Wi-Fi» look identical from outside.
        FieldCard(modifier = Modifier.fillMaxWidth()) {
            Text(PHOTO_CACHE_HEADING, style = MaterialTheme.typography.titleMedium)
            Text(
                photoCacheLabel(photoCache),
                style = MaterialTheme.typography.bodyMedium,
                color = Paper.muted,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // The only place the valuation pass reports (ADR 0028 §6): from «Las cifras», prices on
        // their way and an exhausted monthly allowance look the same.
        FieldCard(modifier = Modifier.fillMaxWidth()) {
            Text(VALUATION_HEADING, style = MaterialTheme.typography.titleMedium)
            Text(
                valuationLabel(valuation),
                style = MaterialTheme.typography.bodyMedium,
                color = Paper.muted,
                modifier = Modifier.padding(top = 4.dp),
            )
            // What the collector's marks add to the pass each month (ADR 0029 §5).
            wishSpend?.let { spend ->
                Text(
                    spend,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Paper.rust,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            // ADR 0028 §6.1: only in the states caused by the key. Labelled with the destination's
            // name, as the foot link is (ADR 0026 §14).
            if (valuationBlamesCredentials(valuation)) {
                CardAction(
                    text = CREDENTIALS_LABEL,
                    onClick = onOpenCredentials,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }

        FieldCard(modifier = Modifier.fillMaxWidth()) {
            // No title: it would repeat the card's only button (ADR 0026 §5).
            Text(
                DATA_EXPORT_EXPLANATION,
                style = MaterialTheme.typography.bodyMedium,
                color = Paper.muted,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            CardAction(
                text = dataExportLabel(exporting),
                onClick = onExportData,
                enabled = !exporting,
            )
        }

        // Ordered by how often they are needed (ADR 0026 §14).
        CardAction(text = CREDENTIALS_LABEL, onClick = onOpenCredentials)
        CardAction(text = NOTICES_LABEL, onClick = onOpenNotices)
    }
}
