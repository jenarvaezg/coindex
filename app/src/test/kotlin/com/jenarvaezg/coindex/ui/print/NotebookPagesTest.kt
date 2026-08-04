package com.jenarvaezg.coindex.ui.print

import com.jenarvaezg.coindex.data.CatalogFiles
import com.jenarvaezg.coindex.data.CoinPhoto
import com.jenarvaezg.coindex.data.TypeCacheFile
import com.jenarvaezg.coindex.data.numista.NumistaTypeDto
import com.jenarvaezg.coindex.data.seed.typeMetaEntity
import com.jenarvaezg.coindex.data.toDomain
import com.jenarvaezg.coindex.domain.CatalogSeeds
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.TypeMetaIndex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * How many A4 pages the shipped catalogs take at 1:1, which is the one thing #169 can be checked
 * against without a printer.
 *
 * The diameters are read from the seeded type cache exactly as the phone reads them — through
 * `raw`, so this also pins that `size` survives the trip from the asset to [PrintCell] — and the
 * page count follows from them alone. A catalog gaining a member, or a curated diameter changing,
 * moves a number here on purpose: the notebook's length is a measured fact.
 */
class NotebookPagesTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val catalogs: List<CollectionCatalog> = CatalogSeeds.parseAll(CatalogFiles.all())

    private val typeMeta: TypeMetaIndex = json
        .parseToJsonElement(TypeCacheFile.read())
        .jsonObject
        .entries
        .mapNotNull { (typeIdText, element) ->
            val raw = element as? JsonObject ?: return@mapNotNull null
            val typeId = typeIdText.toIntOrNull() ?: return@mapNotNull null
            val dto = runCatching {
                json.decodeFromJsonElement(NumistaTypeDto.serializer(), raw)
            }.getOrNull() ?: return@mapNotNull null
            typeMetaEntity(typeId, dto, raw.toString(), 0L).toDomain()
        }
        .associateBy { it.id }

    /** One catalog as it would go to paper: every member a cell, nothing owned yet. */
    private fun section(catalog: CollectionCatalog) = PrintSection(
        eyebrow = "COINDEX · CATÁLOGO CURADO",
        title = catalog.name,
        subtitle = null,
        facts = emptyList(),
        source = catalog.source,
        cells = catalog.members.map { member ->
            PrintCell(
                label = member.label,
                state = "Me falta",
                footnote = member.year?.toString(),
                diameterMm = member.numistaTypeId
                    ?.let { typeMeta[it]?.sizeMillimetres?.toFloat() },
                reverse = CoinPhoto(thumbnail = "https://example.invalid/$member.jpg"),
                filled = false,
            )
        },
    )

    private fun pagesOf(catalogId: String) = section(catalogs.first { it.id == catalogId }).pages

    @Test
    fun `every seeded type carries the diameter the printed page is measured with`() {
        val withoutSize = typeMeta.values.filter { it.sizeMillimetres == null }
        assertTrue(withoutSize.isEmpty(), "fichas sin diámetro: ${withoutSize.map { it.id }}")
        val sizes = typeMeta.values.mapNotNull { it.sizeMillimetres }
        assertEquals(14.5, sizes.min())
        assertEquals(45.6, sizes.max())
    }

    /** The two catalogs #169 names, and the numbers it asks the printed notebook to hit. */
    @Test
    fun `the two measured catalogs come out at the length the ticket asks for`() {
        assertEquals(7, pagesOf("outstanding-personalities-russia-2-roubles"))
        assertEquals(4, pagesOf("australian-kookaburra-perth-1oz"))
    }

    /**
     * The whole shelf, which is the upper bound of any collector's notebook: nobody owns a piece of
     * every variant, and a card without a catalog prints its pieces instead of 121 empty slots.
     */
    @Test
    fun `the fifty-six shipped catalogs would print as one hundred and one pages`() {
        assertEquals(101, catalogs.sumOf { section(it).pages })
    }

    @Test
    fun `a plate that spills repeats its heading and numbers its pages`() {
        val kookaburra = section(catalogs.first { it.id == "australian-kookaburra-perth-1oz" })
        val pages = printPages(listOf(kookaburra))

        assertEquals(4, pages.size)
        assertEquals(listOf(1, 2, 3, 4), pages.map { it.numberInSection })
        assertTrue(pages.all { it.pagesInSection == 4 })
        // Every page carries the same heading, because on paper there is no scrolling back.
        assertTrue(pages.all { it.section.title == kookaburra.title })
        // And no cell is lost or repeated across the break.
        assertEquals(kookaburra.cells, pages.flatMap { it.cells })
        assertEquals(12, pages.first().cells.size)
        assertEquals(1, pages.last().cells.size)
    }

    @Test
    fun `a collection with nothing in it still gets one page rather than none`() {
        val empty = section(catalogs.first()).copy(cells = emptyList())

        val pages = printPages(listOf(empty))

        assertEquals(1, pages.size)
        assertEquals(emptyList(), pages.single().cells)
    }
}
