package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.domain.CatalogSeedException
import com.jenarvaezg.coindex.domain.CuratedGrouping
import com.jenarvaezg.coindex.domain.CuratedSpecies
import com.jenarvaezg.coindex.domain.Curation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The shipped files, read through the door the phone reads them through (#545).
 *
 * Every other test of `data/` asks this curation for one species and pins what is in it. This one
 * asks nothing of the contents: what it checks is that the load itself goes through — the parse of
 * each file, the ids and card names of each species, the claims no two catalogs may share, and the
 * name no catalog may share with a grouping (#22). Before the door, that last rule was applied by
 * the app's container alone, so the one reading with the real files in front of it could not see a
 * collision it was standing on.
 */
class ShippedCurationTest {
    @Test
    fun `every shipped file arrives as a curated species`() {
        // Reaching the assertions is already most of it: the load throws on a file that no longer
        // parses, an id or an issue claimed twice, a species that brought nothing, and the card
        // name of #22. What is left to say is that nothing was quietly dropped on the way in —
        // one catalog per file in `data/collection-catalogs`, and so for the other two.
        assertEquals(fileCount(CuratedSpecies.Catalogs), SHIPPED_CURATION.catalogs.size)
        assertEquals(fileCount(CuratedSpecies.Groupings), SHIPPED_CURATION.groupings.size)
        assertEquals(fileCount(CuratedSpecies.Programmes), SHIPPED_CURATION.programmes.size)
    }

    private fun fileCount(species: CuratedSpecies): Int = RepoCuratedFiles.read(species).size

    /**
     * That the load above went through says the shipped names are distinct. This says the check is
     * the reason, and not that nobody looked: the same catalogs with one grouping stealing a card
     * name are refused.
     */
    @Test
    fun `a grouping that steals a shipped card name is refused`() {
        val stolen = SHIPPED_CURATION.catalogs.first().shortName

        val error = assertFailsWith<CatalogSeedException> {
            Curation(
                catalogs = SHIPPED_CURATION.catalogs,
                groupings = SHIPPED_CURATION.groupings + thief(stolen),
            )
        }

        assertTrue(error.message!!.contains(stolen), error.message!!)
    }

    private fun thief(shortName: String): CuratedGrouping = CuratedGrouping(
        schemaVersion = 1,
        id = "ladrona-de-nombres",
        name = shortName,
        shortName = shortName,
        family = "Familia de prueba",
        issuerCode = "espagne",
        source = "https://en.numista.com/catalogue/pieces1885.html",
        updatedAt = "2026-09-07",
        typeIds = listOf(1_885),
    )
}
