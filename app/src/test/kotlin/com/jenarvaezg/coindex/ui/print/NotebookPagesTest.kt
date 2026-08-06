package com.jenarvaezg.coindex.ui.print

import com.jenarvaezg.coindex.data.CatalogFiles
import com.jenarvaezg.coindex.data.CoinPhoto
import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.TypeCacheFile
import com.jenarvaezg.coindex.data.numista.NumistaTypeDto
import com.jenarvaezg.coindex.data.seed.typeMetaEntity
import com.jenarvaezg.coindex.data.toDomain
import com.jenarvaezg.coindex.domain.CatalogSeeds
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.Curation
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.OwnGrouping
import com.jenarvaezg.coindex.domain.OwnGroupingView
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

    /**
     * El cuaderno de hoy es el que la configuración por omisión produce (#228).
     *
     * Todos los recuentos de este archivo se miden contra esta geometría y no contra un `object` de
     * constantes: que estos números no se muevan es la prueba de que la plomería del #228 no ha
     * cambiado nada detrás de la puerta.
     */
    private val paper = printGeometry(NotebookOptions())

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
                // Thumbnail and original, as a cached type has: what is warmed is the first.
                reverse = CoinPhoto(
                    thumbnail = "https://numista.invalid/${member.id}-180.jpg",
                    picture = "https://numista.invalid/${member.id}-original.jpg",
                ),
                filled = false,
            )
        },
    )

    private fun pagesOf(catalogId: String) =
        section(catalogs.first { it.id == catalogId }).pages(paper)

    private fun pagesFor(vararg sections: PrintSection) = printPages(sections.toList(), paper)

    @Test
    fun `every seeded type carries the diameter the printed page is measured with`() {
        val withoutSize = typeMeta.values.filter { it.sizeMillimetres == null }
        assertTrue(withoutSize.isEmpty(), "fichas sin diámetro: ${withoutSize.map { it.id }}")
        val sizes = typeMeta.values.mapNotNull { it.sizeMillimetres }
        assertEquals(14.5, sizes.min())
        assertEquals(45.6, sizes.max())
    }

    /**
     * The two catalogs #169 names, and the numbers it asks the printed notebook to hit. The seven
     * pages it measured were the 121 members the Russian personalities had before #159 split them
     * by fineness: the 104 of .925 print six, and the seventeen of .500 fit on a single page.
     */
    @Test
    fun `the two measured catalogs come out at the length the ticket asks for`() {
        assertEquals(6, pagesOf("outstanding-personalities-russia-2-roubles"))
        assertEquals(1, pagesOf("outstanding-personalities-russia-2-roubles-plata-500"))
        assertEquals(4, pagesOf("australian-kookaburra-perth-1oz"))
    }

    /**
     * The whole shelf, which is the upper bound of any collector's notebook: nobody owns a piece of
     * every variant, and a card without a catalog prints its pieces instead of 121 empty slots.
     */
    @Test
    fun `the sixty-one shipped catalogs would print as one hundred and six pages`() {
        assertEquals(106, catalogs.sumOf { section(it).pages(paper) })
    }

    /**
     * The default configuration reproduces today's notebook **plate by plate**, not just in total.
     *
     * This is the load-bearing test of #228: the geometry stopped being an `object` of constants and
     * became the value a configuration declares, threaded through `notebookSections`, `printGrid`,
     * `printPages` and the drawing of the page. A millimetre lost anywhere in that plumbing moves a
     * plate from one column of this histogram to the next, where the sum of 106 might well hide it.
     */
    @Test
    fun `the default configuration reproduces today's notebook plate by plate`() {
        val sections = catalogs.map(::section)

        val pages = printPages(sections, paper)

        // Cuántas láminas ocupan 1 página, cuántas 2, y así: 39 caben de una, y la más larga son
        // las nueve del Libro Rojo de Rusia. Los 2 € conmemorativos de España entran en la columna
        // de dos páginas: 29 casillas de 25,75 mm, que es la moneda más pequeña con lámina propia
        // después de los reales y los medios de Venezuela.
        assertEquals(
            mapOf(1 to 39, 2 to 13, 3 to 2, 4 to 5, 6 to 1, 9 to 1),
            pages.groupBy { it.section.title }
                .map { (_, ofSection) -> ofSection.size }
                .groupingBy { it }
                .eachCount()
                .toSortedMap(),
        )
        assertEquals(106, pages.size)

        // Y los cortes: ninguna casilla se pierde ni se repite, ninguna página va sobrecargada, y
        // sólo la última de cada lámina puede ir corta.
        sections.forEach { plate ->
            val ofPlate = pages.filter { it.section === plate }
            assertEquals(plate.cells, ofPlate.flatMap { it.cells }, "corte roto: ${plate.title}")
            val perPage = ofPlate.first().grid.cellsPerPage
            assertTrue(
                ofPlate.dropLast(1).all { it.cells.size == perPage },
                "una página intermedia va corta en ${plate.title}",
            )
            assertTrue(
                ofPlate.last().cells.size <= perPage,
                "una página va sobrecargada en ${plate.title}",
            )
        }
    }

    @Test
    fun `a plate that spills repeats its heading and numbers its pages`() {
        val kookaburra = section(catalogs.first { it.id == "australian-kookaburra-perth-1oz" })
        val pages = pagesFor(kookaburra)

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

    /**
     * A plate of one short row is centred on what it holds; a plate that spills is not.
     *
     * The three 20 escudos of Portugal in a four-column grid printed visibly left of centre. The
     * Kookaburra's tail page holds one coin and must **not** be centred: it continues the column it
     * started in, and the four pages are read as a run.
     */
    @Test
    fun `a plate of one short row is centred on the cells it has`() {
        val escudos = section(catalogs.first { it.id == "portugal-20-escudos-plata" })
        val single = pagesFor(escudos).single()

        assertEquals(4, escudos.grid(paper).columns)
        assertEquals(3, single.columnsUsed)
        assertEquals(escudos.grid(paper).widthOfMm(3), single.blockWidthMm)

        val kookaburra = pagesFor(
            section(catalogs.first { it.id == "australian-kookaburra-perth-1oz" }),
        )
        assertEquals(1, kookaburra.last().cells.size)
        assertTrue(
            kookaburra.all { it.blockWidthMm == it.grid.blockWidthMm },
            "una fila corta ha movido el bloque de una lámina que se continúa",
        )
    }

    /**
     * The photographs the notebook has to fetch, once each.
     *
     * This list is the fix for an export that came out with 64 photographs out of some 600: asked
     * page by page, a picture got one page's budget and no second chance. Two properties make it
     * work — **deduplicated**, because a type shows up on several pages, and **the thumbnail only**,
     * because the original behind it is a fallback and warming both would double the requests.
     */
    @Test
    fun `the photographs to warm are the thumbnails, each one once`() {
        val kookaburra = catalogs.first { it.id == "australian-kookaburra-perth-1oz" }
        val pages = pagesFor(section(kookaburra))

        val urls = notebookPhotographs(pages)

        // One per cell, and the four pages of the plate do not ask for anything four times over.
        assertEquals(kookaburra.members.size, urls.size)
        assertEquals(urls.size, urls.distinct().size)
        assertTrue(urls.all { it.startsWith("https://numista.invalid/") }, "no son las miniaturas")

        // The same coin on two pages of two cards is one photograph to fetch.
        val repeated = section(kookaburra).let { plate ->
            plate.copy(cells = plate.cells.take(1) + plate.cells.take(1))
        }
        assertEquals(1, notebookPhotographs(pagesFor(repeated)).size)
    }

    @Test
    fun `a cell with no picture asks for nothing`() {
        val bare = section(catalogs.first()).let { plate ->
            plate.copy(cells = plate.cells.map { it.copy(reverse = null) })
        }

        assertEquals(emptyList(), notebookPhotographs(pagesFor(bare)))
    }

    @Test
    fun `a collection with nothing in it still gets one page rather than none`() {
        val empty = section(catalogs.first()).copy(cells = emptyList())

        val pages = pagesFor(empty)

        assertEquals(1, pages.size)
        assertEquals(emptyList(), pages.single().cells)
    }

    /**
     * One card in, one section out — including the box the collector emptied.
     *
     * A box survives with nothing in it (ADR 0021 §11), and what stays out of the notebook is a
     * question for the index and not for the printer (#147): dropping it here would be a second
     * rule about what a collection is, kept only in the exporter, and the button's count would have
     * to learn it too.
     */
    @Test
    fun `an emptied box is still a section of the notebook`() {
        val emptied = IndexCard.Box(
            name = "Bandeja del abuelo",
            issuer = null,
            box = OwnGroupingView(
                OwnGrouping(id = 1, name = "Bandeja del abuelo", typeIds = emptyList()),
                emptyList(),
            ),
        )

        val sections = notebookSections(
            CollectionState(),
            listOf(emptied),
            Curation(catalogs),
            NotebookOptions(),
        )

        assertEquals(1, sections.size)
        assertEquals("Bandeja del abuelo", sections.single().title)
        assertEquals(emptyList(), sections.single().cells)
        assertEquals(1, printPages(sections, paper).size)
    }
}
