package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * What claims a coin, asked of the assembly itself (#540).
 *
 * The reading used to be an `internal` class of the shelf's UI, recomputed from scratch by its four
 * callers — the rows of Coins, the sheet a casilla opens, the «Sin colección» set and the country
 * axis — so nothing could exercise it on its own and the two grains it answers at had to be learned
 * again by whoever asked. Here they are read through [Curation.assemble], which is the only door the
 * app comes through: if the assembly gets this wrong, the four surfaces are wrong together, and this
 * is where it is said once.
 */
class CoinClaimsTest {
    /**
     * A coin links back to **every** collection that claims it, in the one order of the index
     * (ADR 0021 §1, §6, §10).
     *
     * The two hands of §10 in one snapshot: a curated file claims the type and a box the collector
     * typed claims it too, and on screen neither outranks the other. A single card per type would
     * have to pick one of them, which is exactly the «home» that §10 refused to invent — so the
     * answer is a list, and the list is in index order because the card that says «1 de 2» is the
     * one the collector recognises first.
     */
    @Test
    fun `a coin links back to every collection that claims it, in index order`() {
        val eagle = silverEagleCatalog()
        val curation = Curation(listOf(eagle))
        val eagleRow = ownedRow(id = 1, typeId = SILVER_EAGLE_TYPE, issueId = BULLION_ISSUE)
        val morganRow = ownedRow(id = 2, typeId = MORGAN_TYPE, year = 1898)

        val assembled = curation.assemble(
            CollectionSnapshot(
                items = listOf(eagleRow, morganRow),
                typeMeta = mapOf(
                    typeMeta(SILVER_EAGLE_TYPE),
                    // Sin familia en Numista, así que la caja es lo único que la reclama.
                    typeMeta(MORGAN_TYPE),
                ),
                ownGroupings = listOf(
                    OwnGrouping(
                        id = 7,
                        name = "Las americanas",
                        typeIds = listOf(SILVER_EAGLE_TYPE, MORGAN_TYPE),
                    ),
                ),
            ),
        )

        // El catálogo lleva ratio y la caja no, así que el comparador del §6 los ordena así.
        assertEquals(
            listOf("American Silver Eagle", "Las americanas"),
            assembled.claims.of(SILVER_EAGLE_TYPE).map { it.name },
        )
        assertEquals(
            listOf("Las americanas"),
            assembled.claims.of(MORGAN_TYPE).map { it.name },
        )
    }

    /**
     * The row and the type are **not** the same question (ADR 0019).
     *
     * Measured on the father's collection: his American Silver Eagle N#298883 is two rows, and the
     * catalog qualifies its members by issue, so the bullion row fills the 2021 casilla and the
     * burnished row of the same type and the same year fills nothing. The type is in a collection
     * and one of his two coins is not — which is what the «Sin colección» chip of Coins exists to
     * say (ADR 0021 §12), and reading membership off the type alone made that second coin
     * invisible in the one place left for it.
     */
    @Test
    fun `an issue-qualified catalog claims one row of a type and leaves its sibling loose`() {
        val curation = Curation(listOf(silverEagleCatalog()))
        val bullion = ownedRow(id = 1, typeId = SILVER_EAGLE_TYPE, issueId = BULLION_ISSUE)
        val burnished = ownedRow(id = 2, typeId = SILVER_EAGLE_TYPE, issueId = BURNISHED_ISSUE)

        val assembled = curation.assemble(
            CollectionSnapshot(
                items = listOf(bullion, burnished),
                typeMeta = mapOf(typeMeta(SILVER_EAGLE_TYPE)),
            ),
        )

        assertEquals(
            listOf("American Silver Eagle"),
            assembled.claims.of(SILVER_EAGLE_TYPE).map { it.name },
        )
        assertTrue(assembled.claims.claimed(bullion))
        assertFalse(assembled.claims.claimed(burnished))
        assertEquals(1, assembled.claims.unclaimedPieces(listOf(bullion, burnished)))
    }

    /**
     * A coin no card claims answers with silence, and it is a fact about the coin and not a gap in
     * the reading (ADR 0021 §1).
     *
     * The row with no ficha on the phone cannot be placed at all — no family, no catalog, no card —
     * so it is the residue the notebook's last lámina prints. Asked of the assembly it says «no
     * collection», which is the same sentence a coin outside every plate says in Coins.
     */
    @Test
    fun `a coin no card claims is claimed by nothing at either grain`() {
        val curation = Curation(listOf(silverEagleCatalog()))
        val uncached = ownedRow(id = 3, typeId = 999_999)

        val assembled = curation.assemble(CollectionSnapshot(items = listOf(uncached)))

        assertEquals(emptyList(), assembled.claims.of(999_999))
        assertFalse(assembled.claims.claimed(uncached))
        assertEquals(1, assembled.claims.unclaimedPieces(listOf(uncached)))
    }
}

private const val SILVER_EAGLE_TYPE = 298_883

private const val MORGAN_TYPE = 3_262

/** The two issues Numista files under N#298883 for 2021: bullion, and burnished. */
private const val BULLION_ISSUE = 760_576

private const val BURNISHED_ISSUE = 1_059_386

/**
 * The date run of #91, qualified by issue: N#298883 mixes bullion, proof and burnished in one
 * ficha, so without the list a burnished row would fill the bullion casilla of its year.
 */
private fun silverEagleCatalog() = CollectionCatalog(
    schemaVersion = 2,
    id = "estados-unidos-silver-eagle-1oz",
    name = "American Silver Eagle · Estados Unidos · 1 oz",
    shortName = "American Silver Eagle",
    issuerCode = "etats-unis",
    family = "American Silver Eagle",
    weightMillioz = 1_000,
    finish = Finish.Bullion,
    metal = Metal.Silver,
    seriesStatus = SeriesStatus.Open,
    source = "https://en.numista.com/catalogue/pieces298883.html",
    updatedAt = "2026-09-01",
    members = listOf(
        CollectionCatalogMember(
            id = "2021",
            label = "2021",
            year = 2021,
            numistaTypeId = SILVER_EAGLE_TYPE,
            numistaIssueIds = listOf(BULLION_ISSUE),
        ),
        CollectionCatalogMember(
            id = "2022",
            label = "2022",
            year = 2022,
            numistaTypeId = SILVER_EAGLE_TYPE,
            numistaIssueIds = listOf(1_275_112),
        ),
    ),
)

private fun ownedRow(id: Long, typeId: Int, year: Int = 2021, issueId: Int? = null) = CollectedItem(
    id = id,
    quantity = 1,
    typeId = typeId,
    issueYear = year,
    issueId = issueId,
)

private fun typeMeta(typeId: Int) = typeId to TypeMeta(
    id = typeId,
    title = "N# $typeId",
    issuerCode = "etats-unis",
    issuerName = "Estados Unidos",
    weightOz = 1.0,
    metal = Metal.Silver,
)
