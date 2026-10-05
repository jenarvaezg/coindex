package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** What claims a coin, read through [Curation.assemble], the app's only way in (#540). */
class CoinClaimsTest {
    /**
     * A curated file and a box claim the same type and neither outranks the other (ADR 0021 §10),
     * so the answer is a list, in index order (§1, §6).
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
     * The row and the type are different questions (ADR 0019): the burnished 2021 Silver Eagle
     * fills nothing, and the «Sin colección» chip has to show it (ADR 0021 §12).
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

    /** A row with no ficha can't be placed: it is the notebook's last residue (ADR 0021 §1). */
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
