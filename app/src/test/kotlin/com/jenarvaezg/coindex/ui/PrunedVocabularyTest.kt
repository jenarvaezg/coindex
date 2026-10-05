package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.PlateUnavailable
import com.jenarvaezg.coindex.data.SyncRecord
import com.jenarvaezg.coindex.data.TypeRefreshReport
import com.jenarvaezg.coindex.data.numista.NumistaException
import com.jenarvaezg.coindex.data.prices.ValuationRefusal
import com.jenarvaezg.coindex.data.prices.ValuationStatus
import com.jenarvaezg.coindex.ui.shelf.ANY_FILTER
import com.jenarvaezg.coindex.ui.shelf.OunceBand
import com.jenarvaezg.coindex.ui.shelf.coinsTally
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * One string, one owner (ADR 0026 §5): the places where a string used to be said more than once.
 * `CopyLivesInOnePlaceTest` checks where copy is written; this pins the text of the merged strings.
 *
 * No word counts on purpose (§6): a count would pass a worse rewording of the same length.
 */
class PrunedVocabularyTest {
    /** Shared by a dead route, an emptied derived collection and a plate whose variant is gone. */
    @Test
    fun `a collection that is gone is said the same way wherever it is said`() {
        assertEquals("Esta colección ya no existe. Vuelve al índice.", COLLECTION_NO_LONGER_EXISTS)
        assertEquals(
            COLLECTION_NO_LONGER_EXISTS,
            plateUnavailableLabel(PlateUnavailable.NotACollection),
        )
    }

    /** A box survives empty because the collector typed it (ADR 0021 §11), and it says so. */
    @Test
    fun `an empty box keeps a sentence of its own`() {
        assertTrue(EMPTY_BOX_EXPLANATION != COLLECTION_NO_LONGER_EXISTS)
        assertTrue("Sigue aquí" in EMPTY_BOX_EXPLANATION)
    }

    /**
     * Replaces the per-facet `Todos`, `Todas`, `Todo`, `Cualquier año` and `Da igual`. The chips
     * are drawn by two screens, so only the word itself is held here.
     */
    @Test
    fun `the no-filter chip is one word that agrees with no facet`() {
        assertEquals("Cualquiera", ANY_FILTER)
    }

    /** One string on two screens, so the encryption promise can't drift between them. */
    @Test
    fun `the credential promise says both of its halves once`() {
        assertTrue("cifrados" in CREDENTIALS_EXPLANATION)
        assertTrue("nunca salen de él" in CREDENTIALS_EXPLANATION)
        assertTrue("API key" in CREDENTIALS_EXPLANATION)
    }

    /** Neither a title above the button nor the explanation under it repeats it (ADR 0026 §5). */
    @Test
    fun `signing out is said once, on the control that does it`() {
        assertEquals("Cerrar sesión", SIGN_OUT_ACTION)
        assertTrue(SIGN_OUT_ACTION !in SIGN_OUT_EXPLANATION)
    }

    /** A row opening a screen with another name would read as two features (ADR 0026 §14). */
    @Test
    fun `the notices entry and the screen it opens share a name`() {
        assertEquals("Avisos y licencias", NOTICES_LABEL)
        assertEquals(NOTICES_LABEL, screenTitle(Routes.NOTICES))
    }

    /** Only on Avisos y licencias, not on every masthead (#410). */
    @Test
    fun `the installed version is named once with the notices`() {
        assertEquals("Coindex · v1.2.2", installedVersionLabel("1.2.2"))
        assertEquals("Coindex", installedVersionLabel(""))
    }

    /** «Este teléfono» since #521: «Ajustes» named fields the screen no longer leads with. */
    @Test
    fun `the phone's own screen is named by the masthead`() {
        assertEquals("Este teléfono", PHONE_LABEL)
        assertEquals(PHONE_LABEL, screenTitle(Routes.PHONE))
    }

    /**
     * The §14 pairing across every door (#521): the row on «Este teléfono», the valuation card's
     * row when it blames the key, and the sync refusals that send the collector there.
     */
    @Test
    fun `the credentials entry, the doors that blame it and the screen share a name`() {
        assertEquals("Credenciales", CREDENTIALS_LABEL)
        assertEquals(CREDENTIALS_LABEL, screenTitle(Routes.CREDENTIALS))
        assertTrue(CREDENTIALS_LABEL in syncErrorLabel(NumistaException.EmptyApiKey()))
        assertTrue(
            CREDENTIALS_LABEL in syncErrorLabel(NumistaException.Api("/oauth_token", 401, "")),
        )
        assertTrue(
            CREDENTIALS_LABEL in
                syncErrorLabel(NumistaException.Api("/users/1/collected_items", 404, "")),
        )
    }

    /** The snackbar names what was saved because it is read after leaving (ADR 0026 §5). */
    @Test
    fun `the save button does not repeat the screen it is on`() {
        assertEquals("Guardar", CREDENTIALS_SAVE_ACTION)
        assertTrue(CREDENTIALS_LABEL !in CREDENTIALS_SAVE_ACTION)
        assertEquals("Credenciales guardadas.", CREDENTIALS_SAVED_MESSAGE)
    }

    /**
     * «Colección» on every surface, the word ADR 0021 §2 chose (#516): «caja» and «Agrupar» marked
     * provenance. The code keeps `OwnGrouping`, since §2 is about labels. The ounce chips are swept
     * too because one of them used to read «Conjunto o caja».
     */
    @Test
    fun `the box the collector makes is a colección wherever it is named`() {
        assertEquals("Hacer una colección", boxDoorLabel(seeded = false, shown = 191))
        assertEquals("Hacer una colección con estas 59", boxDoorLabel(seeded = true, shown = 59))
        assertEquals("Nombrar la colección · 2", namePickedBoxLabel(2))
        assertEquals("Tu colección", BOX_EYEBROW)
        assertEquals("Varias onzas", OunceBand.Spanning.label)
        listOf(
            boxDoorLabel(seeded = false, shown = 191),
            boxDoorLabel(seeded = true, shown = 59),
            selectionHintLabel(seeded = false, shown = 191),
            selectionHintLabel(seeded = true, shown = 59),
            namePickedBoxLabel(2),
            BOX_EYEBROW,
            boxDialogHeading(2),
            boxCreatedMessage("Las francesas"),
        ).plus(OunceBand.entries.map { band -> band.label }).forEach { said ->
            listOf("caja", "grupa").forEach { stray ->
                assertTrue(
                    !said.contains(stray, ignoreCase = true),
                    "«$stray» sigue en la interfaz: $said",
                )
            }
        }
    }

    /**
     * Everything that spends the key's monthly budget names it in consultas, the unit the marking
     * mode promises in (#516); a 429 throttles the same budget. All four exhausted-month sentences
     * are listed, #600's key-side one included. Checks ignore case because a capitalised
     * «Llamadas…» once slipped past a case-sensitive sweep.
     */
    @Test
    fun `the api spend is counted in consultas wherever it is named`() {
        assertEquals("1 consulta", queriesLabel(1))
        assertEquals("2 consultas", queriesLabel(2))
        listOf(
            fichaRefreshLabel(refreshing = false),
            fichaRefreshMessage(TypeRefreshReport(596_807, changed = false)),
            showcaseValueAction(calls = 34, valued = false, valuing = false),
            showcaseRefusalMessage(ValuationRefusal.BudgetExhausted),
            valuationLabel(
                ValuationStatus(wanted = 223, missing = 83, held = ValuationRefusal.BudgetExhausted),
            ),
            syncErrorLabel(NumistaException.BudgetExhausted(1_500, 1_500)),
            syncErrorLabel(NumistaException.Api("/types/1", 429, "")),
            syncErrorLabel(NumistaException.Api("/types/1", 429, "Quota exceeded")),
            syncReportLabel(SyncRecord(0L, 22, 3, 5, null)),
            WishLabels.MARK_HINT,
        ).forEach { said ->
            assertTrue(said.contains("consulta", ignoreCase = true), "no dice la unidad: $said")
            listOf("llamada", "petici").forEach { stray ->
                assertTrue(
                    !said.contains(stray, ignoreCase = true),
                    "«$stray» sigue siendo una segunda unidad: $said",
                )
            }
        }
    }

    /**
     * The exception to `showcaseRefusalMessage`, which otherwise rewords per surface (#560): no
     * second wording would be true, since it is neither the month's allowance nor fixed by waiting.
     */
    @Test
    fun `Numista refusing is said the same way on both surfaces`() {
        assertEquals("Numista está rechazando las consultas.", NUMISTA_IS_REFUSING)
        assertEquals(
            NUMISTA_IS_REFUSING,
            showcaseRefusalMessage(ValuationRefusal.Rejected).removePrefix("No se ha podido tasar: "),
        )
        assertTrue(
            NUMISTA_IS_REFUSING in valuationLabel(
                ValuationStatus(wanted = 223, missing = 83, held = ValuationRefusal.Rejected),
            ),
        )
    }

    /**
     * The hierarchy bar's middle cell counts Numista types, one card per type, and says «Tipos»
     * like the sewn edge and the tally (#516). Counting pieces would put one number under two
     * words (#400).
     */
    @Test
    fun `the middle cell of the bar counts what it says`() {
        assertEquals("Tipos · 15", typesCellLabel(15))
        assertEquals("Tipos · $UNKNOWN_COUNT", typesCellLabel(null))
        val edge = sewnEdgeLabel(SewnEdgeCounts(collections = 6, pieces = 72, types = 15))
        assertTrue("15 tipos" in edge, edge)
        assertTrue("15 tipos" in coinsTally(shown = 15, total = 15))
    }
}
