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
 * The maintenance of the inventory: what this phone holds, and how it is brought up to date (#521).
 *
 * It was «Ajustes», and it was named after the two fields it opened with. The audit of 14 August 2026
 * measured what the name was covering: 87 of the screen's 165 words — the sync, the two queues and the
 * export — are maintenance and not configuration, and the one filled action on it was «Sincronizar»,
 * sitting between the API key and «Cerrar sesión» behind a glyph of sliders that suggests filters.
 *
 * So the nesting is inverted rather than the screen split in two (ADR 0026 §14): what the trip is for
 * is at the top, and the credentials — filled once at onboarding, revisited only when Numista starts
 * refusing the key — go down into the shape §14 wrote for the licence notices, three words at the
 * foot. What blames them opens them: the valuation card grows a row in those two states, because a
 * cure two taps away with no door from the symptom is the dead end Ajustes was invented to end.
 *
 * Reading order: «Sincronizar» first and filled — FieldGuide's level 1, the one thing this screen is
 * opened to do — then the two queues that report on it, then the export, then the two feet.
 */
@Composable
fun PhoneScreen(
    photoCache: PhotoCacheStatus,
    /** How far the valuation pass has got, and why it is held if it is (ADR 0028 §6). */
    valuation: ValuationStatus,
    /**
     * What the marked casillas add to the month, or null while nothing is marked (ADR 0029 §5).
     *
     * It rides on the valuation's card because it is the same subject: that card is where the app says
     * what the pass costs, and a mark is the first thing that makes the figure the collector's own
     * decision. Absent and not zero — a phone with nothing marked has the fixed pass it always had.
     */
    wishSpend: String?,
    syncing: Boolean,
    /** Whether the raw dump of #548 is being written; two taps would open two choosers. */
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
        // No eyebrow and no heading: the masthead of this screen already says «Este teléfono», and
        // printing it twice one line apart is the furniture §5 prices.
        PrimaryAction(
            text = syncActionLabel(syncing),
            onClick = onSync,
            enabled = !syncing,
        )

        // The photographs are the one thing here that cannot be pressed. It is the only place the
        // background prefetch is allowed to speak (#191), and it is here because «faltan 320 y están
        // cayendo» and «faltan 320 porque estás con datos» look identical from the outside and are not.
        FieldCard(modifier = Modifier.fillMaxWidth()) {
            Text(PHOTO_CACHE_HEADING, style = MaterialTheme.typography.titleMedium)
            Text(
                photoCacheLabel(photoCache),
                style = MaterialTheme.typography.bodyMedium,
                color = Paper.muted,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // The pass is silent everywhere else, and this is the one line it is allowed (ADR 0028 §6):
        // «Las cifras» with no money section looks the same whether the prices are on their way or
        // the month's allowance is gone, and only one of the two is worth waiting for.
        FieldCard(modifier = Modifier.fillMaxWidth()) {
            Text(VALUATION_HEADING, style = MaterialTheme.typography.titleMedium)
            Text(
                valuationLabel(valuation),
                style = MaterialTheme.typography.bodyMedium,
                color = Paper.muted,
                modifier = Modifier.padding(top = 4.dp),
            )
            // The elastic half of the same budget (ADR 0029 §5): the line above says what the pass is
            // doing, and this one says what the collector's own marks add to it every month.
            wishSpend?.let { spend ->
                Text(
                    spend,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Paper.rust,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            // The door of ADR 0028 §6.1, in the two states of six whose cause is the key and not a
            // wait. It says the name of what it opens and nothing else, the pairing §14 holds for
            // «Avisos y licencias»: an action worded for this card would read as a second feature.
            if (valuationBlamesCredentials(valuation)) {
                CardAction(
                    text = CREDENTIALS_LABEL,
                    onClick = onOpenCredentials,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }

        FieldCard(modifier = Modifier.fillMaxWidth()) {
            // No title — the word is the button, and a card whose heading repeats its only control
            // says it twice (§5).
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

        // The two feet, in the order of how often they are needed: the key that stops a sync, then
        // the licences. Three words each and no subtitle (ADR 0026 §14).
        CardAction(text = CREDENTIALS_LABEL, onClick = onOpenCredentials)
        CardAction(text = NOTICES_LABEL, onClick = onOpenNotices)
    }
}
