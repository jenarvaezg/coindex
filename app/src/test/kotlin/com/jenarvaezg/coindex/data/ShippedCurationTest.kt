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
 * The shipped files loaded through the phone's own loader (#545): the parse of each file, ids and
 * card names per species, claims no two catalogs may share, and the name no catalog may share with
 * a grouping (#22). Other tests pin the contents; this one only that the load goes through.
 */
class ShippedCurationTest {
    @Test
    fun `every shipped file arrives as a curated species`() {
        // The load already throws on a broken file, a duplicate id or claim, an empty species and
        // the card name of #22; what is left is that no file was dropped on the way in.
        assertEquals(fileCount(CuratedSpecies.Catalogs), SHIPPED_CURATION.catalogs.size)
        assertEquals(fileCount(CuratedSpecies.Groupings), SHIPPED_CURATION.groupings.size)
        assertEquals(fileCount(CuratedSpecies.Programmes), SHIPPED_CURATION.programmes.size)
    }

    private fun fileCount(species: CuratedSpecies): Int = RepoCuratedFiles.read(species).size

    /** The load above passing shows the names are distinct; this shows the check is why. */
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
