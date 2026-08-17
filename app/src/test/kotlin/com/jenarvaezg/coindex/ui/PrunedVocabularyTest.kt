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
 * One string, one owner (ADR 0026 §5).
 *
 * `CopyLivesInOnePlaceTest` defends **where** copy is written; it counts nothing and cannot tell one
 * wording from four. This is the other half: the handful of places where a string was said more than
 * once, and where saying it twice again would go unnoticed because both copies would be in a copy
 * file and both would be green.
 *
 * It does not test every formatter in the copy files. What it holds are the pruning's own promises,
 * and each `assertEquals` here is a sentence somebody could reintroduce.
 *
 * **It counts nothing.** An `assertTrue(words <= 8)` was written here first and taken back out: the
 * bar is not a test (§6), and a word count is exactly the shape §6 refused — it would go red at a
 * better rewording and green at a worse one of the same length. What the sentences are pinned by is
 * their own text, which a reviewer reads in the diff.
 */
class PrunedVocabularyTest {
    /**
     * Four wordings became one, and the one is the short one.
     *
     * 13, 16, 8 and 32 words in three files, for one event. The three surfaces that say it now say
     * it identically — a route with nothing behind it, a derived collection whose last piece went,
     * and a plate whose variant no longer describes anything.
     */
    @Test
    fun `a collection that is gone is said the same way wherever it is said`() {
        assertEquals("Esta colección ya no existe. Vuelve al índice.", COLLECTION_NO_LONGER_EXISTS)
        assertEquals(
            COLLECTION_NO_LONGER_EXISTS,
            plateUnavailableLabel(PlateUnavailable.NotACollection),
        )
    }

    /**
     * An empty box is **not** the sentence above: it survives with nothing in it, because it is the
     * one thing the collector typed (ADR 0021 §11), and it says so.
     */
    @Test
    fun `an empty box keeps a sentence of its own`() {
        assertTrue(EMPTY_BOX_EXPLANATION != COLLECTION_NO_LONGER_EXISTS)
        assertTrue("Sigue aquí" in EMPTY_BOX_EXPLANATION)
    }

    /**
     * Ten chips said six words; they say one.
     *
     * `Todos`, `Todas`, `Todo`, `Cualquier año` and `Da igual` each agreed with the noun of the facet
     * they were written next to, and read as a shelf they were six ways of saying nothing is
     * narrowed. There is no way to assert the ten call sites from here — the chips are drawn by two
     * screens — so what is held is the word itself and that it agrees with nothing.
     */
    @Test
    fun `the no-filter chip is one word that agrees with no facet`() {
        assertEquals("Cualquiera", ANY_FILTER)
    }

    /**
     * The credential promise is one string read on two screens.
     *
     * Never on screen together, so it saves no words on either one; what it saves is the pair
     * drifting apart, which is what happens to a promise about encryption written twice. Both halves
     * of it have to survive: what is stored, and that it does not leave the phone.
     */
    @Test
    fun `the credential promise says both of its halves once`() {
        assertTrue("cifrados" in CREDENTIALS_EXPLANATION)
        assertTrue("nunca salen de él" in CREDENTIALS_EXPLANATION)
        assertTrue("API key" in CREDENTIALS_EXPLANATION)
    }

    /**
     * Signing out is a word on a button and not also a title above it (§5).
     *
     * The explanation under it may not repeat the button, because the card is three lines and one of
     * them would be the same two words twice.
     */
    @Test
    fun `signing out is said once, on the control that does it`() {
        assertEquals("Cerrar sesión", SIGN_OUT_ACTION)
        assertTrue(SIGN_OUT_ACTION !in SIGN_OUT_EXPLANATION)
    }

    /**
     * The way into the notices and the name of what it opens are one string (§14).
     *
     * Three literals for three words, and the masthead of the screen it opens is one of the three:
     * a row in Ajustes that opened a screen called something else would read as two features.
     */
    @Test
    fun `the notices entry and the screen it opens share a name`() {
        assertEquals("Avisos y licencias", NOTICES_LABEL)
        assertEquals(NOTICES_LABEL, screenTitle(Routes.NOTICES))
    }

    /**
     * The installed APK is named once, on Avisos y licencias (#410) — not on every masthead.
     */
    @Test
    fun `the installed version is named once with the notices`() {
        assertEquals("Coindex · v1.2.2", installedVersionLabel("1.2.2"))
        assertEquals("Coindex", installedVersionLabel(""))
    }

    /** Ajustes is named once, by the masthead, and the screen no longer repeats it. */
    @Test
    fun `Ajustes is the masthead's word`() {
        assertEquals(SETTINGS_LABEL, screenTitle(Routes.SETTINGS))
    }

    /**
     * What the collector makes by hand is a **colección**, from the door to the baptism (#516).
     *
     * Three words for one thing: the door said «Agrupar piezas», the mode said «meter en la caja»,
     * and the code calls them `own_groupings`. The word that survives on screen is the one ADR 0021
     * §2 had already chosen — everything in the índice is a collection, and «caja» was exactly the
     * word of provenance §2 removed, ranking a box below the rest on the one gesture whose own
     * snackbar already answers «Colección «…» creada».
     *
     * The code keeps `OwnGrouping`: what §2 forbids is a **label**, and renaming a table teaches the
     * collector nothing.
     *
     * The índice's ounce chip is in here because it is the fourth surface that said «caja», three
     * screens away from the other three, and it was the only one where the word changed the question
     * the facet asks: «Conjunto o caja» answers *which kind of card* beside three chips that answer
     * *how much it weighs*.
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
     * The API spend has one unit on every surface that names it, and it is **consultas** (#516).
     *
     * «Tasar esta lámina · 34 consultas» and «Actualizar la ficha · 1 llamada» spend the same
     * monthly budget of the same key, so a collector reading both was told the app has two
     * currencies. `consultas` is the one kept because the marking mode promises the month in it —
     * «+2 consultas al mes» per casilla — and that promise is the only one of these figures the
     * collector cannot check by looking at the screen it is on.
     *
     * The 429 is here too: «peticiones» was a third word, and Numista throttling the same thing the
     * budget counts is not a different object.
     *
     * **There are three exhausted-month sentences and not two**, which is what the first pass of this
     * test got wrong: `valuationLabel`'s state in Ajustes, `showcaseRefusalMessage`'s answer to a
     * press, and `syncErrorLabel`'s snackbar. The third was missed because it opens with the word
     * capitalised — «Llamadas a la API agotadas este mes» — and a case-sensitive sweep of the copy
     * files walked straight past it. Every check here reads `ignoreCase`, for that reason and no
     * other.
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
     * The middle cell of the hierarchy bar names the grain it counts (#516).
     *
     * It said «Monedas · 15» over a count of Numista types, on a phone holding 72 coins, so the one
     * number in the bar that never moves was under the wrong word. The **count** is the right one —
     * it is what the destination draws, one card per type, and what that screen's own filter shelf
     * tallies underneath — so what changes is the label, and it changes to the word the sewn edge
     * and the tally already spend on this magnitude.
     *
     * Counting pieces instead was the other candidate and it costs more: «Monedas · 72» over a sewn
     * edge reading «72 piezas» is one number under two words, which is the clash of #400 rebuilt.
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
