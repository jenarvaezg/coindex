package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogMember
import com.jenarvaezg.coindex.domain.SeriesStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * What a plate cell has left to say once the heading has said the rest.
 *
 * A date run repeats one type across years, so «1879 · Numista 10340» under a cell titled 1879
 * was the same two facts twice, twenty-one times over. An issue run repeats the year as well.
 * Whatever every member shares belongs in the heading; the cell keeps only what tells it apart.
 */
class PlateLabelsTest {
    private fun member(id: String, label: String, year: Int, typeId: Int) =
        CollectionCatalogMember(id = id, label = label, year = year, numistaTypeId = typeId)

    private val dateRun = listOf(
        member("1879", "1879", 1879, 10_340),
        member("1886", "1886", 1886, 10_340),
    )

    private val issueRun = listOf(
        member("estrella-66", "Estrella 66", 1966, 1_885),
        member("estrella-67", "Estrella 67", 1966, 1_885),
    )

    private val typeRun = listOf(
        member("2011-koala", "Koala - Silver Bullion Coin", 2011, 25_340),
        member("2012-koala", "Koala Silver Bullion Coin", 2012, 32_572),
    )

    /**
     * The sheet that gets shared is the product, so what it is called after exporting has to
     * match what is on it. It used to call any sheet «completa» as long as every picture had
     * reported back, and a picture that failed reported back exactly like one that arrived:
     * twelve empty cells of the 1000 escudos were announced as a complete plate (issue #67).
     */
    @Test
    fun `an exported sheet is only called complete when every picture is on it`() {
        assertEquals(
            "Lámina completa exportada · 19 casillas",
            plateExportMessage(members = 19, expectedPhotos = 38, loadedPhotos = 38),
        )
    }

    @Test
    fun `a sheet with holes says how many, in the plural it needs`() {
        assertEquals(
            "Lámina exportada, pero 10 fotos no llegaron a cargar",
            plateExportMessage(members = 19, expectedPhotos = 38, loadedPhotos = 28),
        )
        assertEquals(
            "Lámina exportada, pero una foto no llegó a cargar",
            plateExportMessage(members = 19, expectedPhotos = 38, loadedPhotos = 37),
        )
    }

    /** A catalog whose types have no cached pictures exports a complete sheet of silhouettes. */
    @Test
    fun `a sheet that asked for no pictures is complete`() {
        assertEquals(
            "Lámina completa exportada · 3 casillas",
            plateExportMessage(members = 3, expectedPhotos = 0, loadedPhotos = 0),
        )
    }

    @Test
    fun `a date run says its type once and never repeats the year it is titled with`() {
        val common = plateCommonFacts(dateRun)

        assertEquals(10_340, common.numistaTypeId)
        assertNull(common.year)
        assertNull(plateCellFootnote(dateRun[0], common))
    }

    @Test
    fun `an issue run shares its year too, so the cell keeps only its label`() {
        val common = plateCommonFacts(issueRun)

        assertEquals(1_885, common.numistaTypeId)
        assertEquals(1966, common.year)
        assertNull(plateCellFootnote(issueRun[0], common))
    }

    @Test
    fun `a catalog of distinct types keeps the year and the type in every cell`() {
        val common = plateCommonFacts(typeRun)

        assertNull(common.numistaTypeId)
        assertNull(common.year)
        assertEquals("2011 · Numista 25340", plateCellFootnote(typeRun[0], common))
    }

    private fun catalog(members: List<CollectionCatalogMember>) = CollectionCatalog(
        schemaVersion = 2,
        id = "venezuela-5-bolivares",
        name = "5 Bolívares · Venezuela",
        issuerCode = "venezuela",
        family = "5 Bolívares de Venezuela",
        weightMillioz = 804,
        finish = null,
        seriesStatus = SeriesStatus.Closed,
        closedNote = "La plata venezolana se acabó en 1965.",
        source = "https://en.numista.com/catalogue/pieces10340.html",
        updatedAt = "2026-08-01",
        members = members,
    )

    @Test
    fun `what every cell shares moves into the specification of the plate`() {
        val entries = plateEntries(catalog(dateRun), ownedMembers = 1)

        assertEquals(
            listOf(
                "Progreso" to "1 / 2 emisiones",
                "Peso" to "0,804 oz",
                "Acabado" to "Sin confirmar",
                "Tipo" to "Numista 10340",
                "Actualizado" to "2026-08-01",
            ),
            entries,
        )
    }

    @Test
    fun `a plate whose cells differ in everything adds nothing to its specification`() {
        val entries = plateEntries(catalog(typeRun), ownedMembers = 2)

        assertEquals(
            listOf(
                "Progreso" to "2 / 2 emisiones",
                "Peso" to "0,804 oz",
                "Acabado" to "Sin confirmar",
                "Actualizado" to "2026-08-01",
            ),
            entries,
        )
    }

    @Test
    fun `the shared year of an issue run is a fact about the plate`() {
        val entries = plateEntries(catalog(issueRun), ownedMembers = 0)

        assertEquals("Año" to "1966", entries[entries.size - 2])
    }
}
