package com.jenarvaezg.coindex.ui.print

import com.jenarvaezg.coindex.data.CatalogFiles
import com.jenarvaezg.coindex.data.CoinPhoto
import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.TypeCacheFile
import com.jenarvaezg.coindex.data.TypeImages
import com.jenarvaezg.coindex.data.numista.NumistaTypeDto
import com.jenarvaezg.coindex.data.seed.typeMetaEntity
import com.jenarvaezg.coindex.data.toDomain
import com.jenarvaezg.coindex.domain.CatalogSeeds
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionSnapshot
import com.jenarvaezg.coindex.domain.Curation
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.OwnGrouping
import com.jenarvaezg.coindex.domain.OwnGroupingView
import com.jenarvaezg.coindex.domain.TypeMetaIndex
import com.jenarvaezg.coindex.ui.notebookExportMessage
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

    /** El mismo cuaderno con «QR de Numista» puesto, que es lo único que el #234 mueve. */
    private val coded = printGeometry(NotebookOptions(numistaQr = true))

    /** Y con «ambas caras», que es lo único que el #230 mueve: la casilla se ensancha. */
    private val doubled = printGeometry(NotebookOptions(bothFaces = true))

    /** Y con las fotos apagadas, que es el #231: la casilla deja de ser una moneda y es una línea. */
    private val listed = printGeometry(NotebookOptions(photographs = false))

    /**
     * One catalog as it would go to paper: every member a cell, nothing owned yet.
     *
     * [faces] is how many sides each cell prints, which is what «ambas caras» decides (#230) — the
     * two of them are two distinct photographs, so this is also what the warm-up has to fetch. Zero
     * is «sin fotos» (#231): a cell that is a line, and a plate with nothing to warm at all.
     */
    private fun section(catalog: CollectionCatalog, faces: Int = 1) = PrintSection(
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
                faces = listOf("anverso", "reverso").takeLast(faces).map { side ->
                    CoinPhoto(
                        thumbnail = "https://numista.invalid/${member.id}-$side-180.jpg",
                        picture = "https://numista.invalid/${member.id}-$side-original.jpg",
                    )
                },
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
    fun `the seventy-three shipped catalogs would print as one hundred and eighteen pages`() {
        assertEquals(118, catalogs.sumOf { section(it).pages(paper) })
    }

    /**
     * The default configuration reproduces today's notebook **plate by plate**, not just in total.
     *
     * This is the load-bearing test of #228: the geometry stopped being an `object` of constants and
     * became the value a configuration declares, threaded through `notebookSections`, `printGrid`,
     * `printPages` and the drawing of the page. A millimetre lost anywhere in that plumbing moves a
     * plate from one column of this histogram to the next, where the sum of 111 might well hide it.
     */
    @Test
    fun `the default configuration reproduces today's notebook plate by plate`() {
        val sections = catalogs.map(::section)

        val pages = printPages(sections, paper)

        // Cuántas láminas ocupan 1 página, cuántas 2, y así: 50 caben de una, y la más larga son
        // las nueve del Libro Rojo de Rusia. Los 2 € conmemorativos de España entran en la columna
        // de dos páginas: 29 casillas de 25,75 mm, que es la moneda más pequeña con lámina propia
        // después de los reales y los medios de Venezuela. Las dos láminas de la tanda del padre
        // —los tres 100 pesos mexicanos y el díptico italiano— caben de una cada una, y también las
        // dos venezolanas de plata del #256: 40 mm de diámetro dan una rejilla de cuatro por tres.
        assertEquals(
            mapOf(1 to 51, 2 to 13, 3 to 2, 4 to 5, 6 to 1, 9 to 1),
            pages.groupBy { it.section.title }
                .map { (_, ofSection) -> ofSection.size }
                .groupingBy { it }
                .eachCount()
                .toSortedMap(),
        )
        assertEquals(118, pages.size)

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

    /**
     * What the QR costs in paper: **nine pages on the shelf of today** (118 → 127), which was eight
     * on the sixty plates #234 measured (104 → 112).
     *
     * The caption is a constant of the layout, so this is what the switch is: the code is 10 mm and
     * every cell of every plate reserves them, whether or not that cell has a code to draw. Eight
     * pages for the 1 084 codes it was measured on, and the reason the decision was «under the name»
     * — beside it would have forced a 44 mm cell and taken a **column** from almost every coin, which
     * this grid cannot spare.
     *
     * **Eight and not the twenty-one of the first measurement**: the code was 12 mm until a printed
     * calibration folio said it did not need to be. Page count is a step function of the caption —
     * anything from 7 to 10,1 mm printed the sixty plates in 112 pages — so the 9 mm the phone read
     * on paper is not the size to ship: 10 mm is the same paper with a tenth more module.
     *
     * **Not the 73 → 91 of the ticket**, and on purpose: those are measured «con compartir página
     * puesto», and «compartir página» is #232, which has not landed. What is measured here is this
     * switch alone, on the notebook that exists — 104 pages with it off. When #232 lands, the two
     * together are what its own recount will say.
     *
     * **The recount below is not the 112 of #234 any more, and it was already stale in main**: this
     * test was red before the #216 batch of 6 August 2026 touched it — 119 pages against the 112
     * pinned — because the shelf grew from the sixty plates it was measured on to sixty-six without
     * anyone re-running it, and the histogram had kept sixty rows all along. Four batches of that one
     * day have moved both figures since.
     *
     * **So the number came out of the name** (#231): what the switch costs is a measured fact of a
     * shelf that grows every week, and a test called «eight pages» goes quietly false the day a plate
     * lands on the wrong side of a step — as it just did, at nine. What does not move is the *shape*
     * of the cost, and that is what the assertions below are for: the caption grows, the columns do
     * not, and no plate ever gets shorter.
     */
    @Test
    fun `the qr grows the notebook by a few pages and never takes a column`() {
        val sections = catalogs.map(::section)

        val pages = printPages(sections, coded)

        assertEquals(127, pages.size)
        // Ninguna lámina se acorta, y la más larga sigue siendo el Libro Rojo de Rusia.
        assertEquals(
            mapOf(1 to 46, 2 to 17, 3 to 2, 4 to 5, 5 to 1, 7 to 1, 9 to 1),
            pages.groupBy { it.section.title }
                .map { (_, ofSection) -> ofSection.size }
                .groupingBy { it }
                .eachCount()
                .toSortedMap(),
        )
        // La casilla crece de alto y no de ancho: las rusas de 33 mm siguen cinco por fila.
        val roubles = section(
            catalogs.first { it.id == "outstanding-personalities-russia-2-roubles" },
        )
        assertEquals(5, roubles.grid(paper).columns)
        assertEquals(5, roubles.grid(coded).columns)
        assertEquals(roubles.grid(paper).rows - 1, roubles.grid(coded).rows)
    }

    /**
     * The five paquillos carry the **same** code, and that is the answer the ticket asked for.
     *
     * They are five members of one Numista type qualified by `numista_issue_ids` (ADR 0019), and the
     * URL per issue the ticket sent us looking for does not exist: a type page marks each issue only
     * with the id of the empty row its collection widget fills in, nothing on Numista links to one, and
     * the fragment it would make is 42 characters — a version 3, which the whole notebook would pay
     * for. So the code promises «esta moneda en Numista», and the ficha of a paquillo is the type's.
     */
    @Test
    fun `the five paquillos share one code, because no url names an issue`() {
        val paquillos = catalogs.first { it.id == "espana-paquillos" }
        val curation = Curation(catalogs)
        val assembled = curation.assemble(
            CollectionSnapshot(
                items = paquillos.members.mapIndexed { index, member ->
                    CollectedItem(
                        id = index + 1L,
                        quantity = 1,
                        typeId = member.numistaTypeId!!,
                        issueId = member.numistaIssueIds.first(),
                        issueYear = member.year,
                    )
                },
                typeMeta = typeMeta,
            ),
        )
        val state = CollectionState(assembled)
        val card = assembled.index.single()

        val cells = notebookSections(
            state,
            listOf(card),
            curation,
            NotebookOptions(numistaQr = true),
        ).single().cells

        assertEquals(5, cells.size)
        assertEquals(
            listOf("https://es.numista.com/1885"),
            cells.mapNotNull { it.numistaUrl }.distinct(),
        )
    }

    /**
     * What both faces cost in paper, which is the most expensive of the five switches (#230).
     *
     * A cell stops being a coin wide and becomes two coins and a gutter, so an ounce goes from
     * 40,9 mm to 84,8 and its plate from twelve cells a page to **six**. That is the check the ticket
     * asks for, and it is arithmetic done before anything is drawn: the height of the cell does not
     * move at all, because the second face is paid for in width or it is not paid for.
     *
     * The ticket said 184 pages against 104 for sixty plates; these are the seventy-three that are
     * shipped now, whose notebook of today is 118 and whose notebook of both faces is 202. Doubling
     * is not exact —the 45,6 mm Lunar II went from three columns to one, so it more than doubles,
     * and a plate of two coins still fits on one page— so the measure is the measure.
     */
    @Test
    fun `both faces doubles the cell and very nearly doubles the notebook`() {
        val sections = catalogs.map { section(it, faces = 2) }

        val pages = printPages(sections, doubled)

        assertEquals(202, pages.size)
        assertEquals(
            mapOf(1 to 29, 2 to 21, 3 to 7, 4 to 6, 5 to 2, 7 to 4, 8 to 1, 9 to 1, 13 to 1, 18 to 1),
            pages.groupBy { it.section.title }
                .map { (_, ofSection) -> ofSection.size }
                .groupingBy { it }
                .eachCount()
                .toSortedMap(),
        )

        // La comprobación que pide el ticket, casilla a casilla: la onza australiana.
        val kookaburra = catalogs.first { it.id == "australian-kookaburra-perth-1oz" }
        val one = section(kookaburra).grid(paper)
        val two = section(kookaburra, faces = 2).grid(doubled)
        assertEquals(12, one.cellsPerPage)
        assertEquals(6, two.cellsPerPage)
        assertEquals(4 to 3, one.columns to one.rows)
        assertEquals(2 to 3, two.columns to two.rows)
        // La casilla dobla de ancho —dos monedas y la calle de en medio— y no crece de alto.
        assertEquals(one.cellWidthMm * 2 + paper.gutterMm, two.cellWidthMm, 0.01f)
        assertEquals(one.cellHeightMm, two.cellHeightMm)
        // Y el bloque sigue cabiendo en el papel que se midió contra él.
        assertTrue(two.blockWidthMm <= doubled.gridWidthMm, "dos caras se salen: ${two.blockWidthMm}")
    }

    /**
     * What the shelf costs with the photographs off: 118 pages become 79, and 73 of them are floor.
     *
     * A cell stops being a coin and becomes a line, so a plate of ounces goes from twelve cells a
     * page to forty-six and **sixty-nine of the seventy-three plates now fit on one page**. Four spill: the
     * two Russian plates of 104 members — the personalities and the architectural monuments — over
     * three pages each, and the Red Book (72) and the Spanish provincial capitals (52) over two.
     *
     * **Not the ~19 pages of the ticket, and two separate things account for the difference.**
     *
     * The first is the floor, and it is the larger. Those 19 are the members divided by a page of
     * lines, and that arithmetic ignores the constraint #228 names in its own text: «una sección nunca
     * comparte página». Seventy-three plates are seventy-three pages before a single member is printed,
     * and lowering that floor is exactly and only what «compartir página» (#232) is for. It is the same
     * correction #234 had to make — its «73 → 91» was measured with sharing on too.
     *
     * The second is ours, and smaller: the line is 7 mm but its **pitch is 10**, because the gutter
     * that separates two columns is the one that separates two rows. That is 46 members a page where
     * the ticket's arithmetic assumed about 57. A row gutter of its own would put four more lines in
     * each column and take this notebook from 79 pages to 76 — three pages, for a field in the
     * geometry that no other switch needs, so the gutter stays one number.
     *
     * So what this switch is worth is measured on the notebook that exists: **39 pages saved, and 6
     * above the floor left for #232 to take.** What it is not worth is 19: the shipped plates hold
     * 1 177 members, which is some 26 pages of *content* at 46 a page, and #232 as #228 scopes it is
     * «dos láminas en un folio» — a floor near 36, not 19. That recount is its ticket's to make.
     */
    @Test
    fun `with no photographs the shelf is a list and the plate is its floor`() {
        val sections = catalogs.map { section(it, faces = 0) }

        val pages = printPages(sections, listed)

        // Los 1 177 miembros del estante, que son las ~26 páginas de contenido que el suelo esconde.
        assertEquals(1_177, catalogs.sumOf { it.members.size })
        assertEquals(79, pages.size)
        assertEquals(
            mapOf(1 to 69, 2 to 2, 3 to 2),
            pages.groupBy { it.section.title }
                .map { (_, ofSection) -> ofSection.size }
                .groupingBy { it }
                .eachCount()
                .toSortedMap(),
        )
        // El suelo es una página por lámina, y sólo seis páginas del cuaderno están por encima de
        // él: lo que queda por ahorrar aquí ya no es de este interruptor.
        assertEquals(catalogs.size, pages.groupBy { it.section.title }.size)
        assertEquals(6, pages.size - catalogs.size)

        // La onza australiana, casilla a casilla: de doce por página a cuarenta y seis, y la rejilla
        // ya no la decide el diámetro — la lámina de medios venezolanos tiene exactamente la misma.
        val kookaburra = catalogs.first { it.id == "australian-kookaburra-perth-1oz" }
        val lines = section(kookaburra, faces = 0).grid(listed)
        assertEquals(12, section(kookaburra).grid(paper).cellsPerPage)
        assertEquals(46, lines.cellsPerPage)
        assertEquals(2 to 23, lines.columns to lines.rows)
        val medios = section(catalogs.first { it.id == "venezuela-medios" }, faces = 0).grid(listed)
        assertEquals(lines.columns to lines.rows, medios.columns to medios.rows)
        assertEquals(lines.cellWidthMm, medios.cellWidthMm)
        assertEquals(lines.cellHeightMm, medios.cellHeightMm)
    }

    /**
     * The half of «sin fotos» that is worth more than the paper: there is nothing left to warm.
     *
     * No face is no candidate URL, so `notebookPhotographs` is empty, no page waits on a decode and
     * the closing message divides by zero photographs — it **cannot** say that three of them failed
     * to arrive, because none were asked for. That makes this the one export of the three that cannot
     * come out incomplete, and it is not a check anywhere: it falls out of the cells being empty.
     */
    @Test
    fun `a notebook with no photographs asks for none and cannot come out incomplete`() {
        val curation = Curation(catalogs)
        val assembled = curation.assemble(
            CollectionSnapshot(
                items = listOf(CollectedItem(id = 1, quantity = 1, typeId = 1885, issueId = 8508)),
                typeMeta = typeMeta,
            ),
        )
        val photographed = CollectionState(
            assembled,
            images = mapOf(
                1885 to TypeImages(
                    obverse = CoinPhoto(thumbnail = "anverso-180.jpg"),
                    reverse = CoinPhoto(thumbnail = "reverso-180.jpg"),
                ),
            ),
        )

        // Ni con «ambas caras» marcada de antes: sin fotos no hay cara que negociar, y la hoja la
        // pone en gris precisamente porque aquí ya no significa nada.
        listOf(
            NotebookOptions(photographs = false),
            NotebookOptions(photographs = false, bothFaces = true),
        ).forEach { options ->
            val cells = notebookSections(photographed, assembled.index, curation, options)
                .single()
                .cells

            assertTrue(cells.isNotEmpty(), "una lista sin casillas no es una lista")
            assertEquals(emptyList(), cells.flatMap { it.faces }, "una línea ha pedido una foto")
        }

        val pages = printPages(catalogs.map { section(it, faces = 0) }, listed)
        assertEquals(emptyList(), notebookPhotographs(pages))
        assertEquals(0, pages.sumOf { it.photographs })
        // Y el mensaje de cierre no puede hablar de fotos que no llegaron, porque no hay ninguna
        // entre la que contarlas: el denominador es cero y la resta también.
        assertEquals(
            "Cuaderno completo exportado · 79 páginas",
            notebookExportMessage(pages.size, expectedPhotos = 0, loadedPhotos = 0),
        )
    }

    /**
     * Two faces are two photographs, and a face nobody photographed is one missing photograph.
     *
     * The warm-up is what keeps a picture from being frozen into the PDF as a hole (#169), so the
     * face that is not on this list is the hole: with «ambas caras» the list doubles, deduplicated
     * exactly as it already was. And the closing message divides by the same number — a type whose
     * obverse never arrived costs one photograph, not a broken plate.
     */
    @Test
    fun `both faces asks for two photographs per type and counts them one by one`() {
        val kookaburra = catalogs.first { it.id == "australian-kookaburra-perth-1oz" }
        val pages = printPages(listOf(section(kookaburra, faces = 2)), doubled)

        val urls = notebookPhotographs(pages)

        assertEquals(kookaburra.members.size * 2, urls.size)
        assertEquals(urls.size, urls.distinct().size)
        assertEquals(kookaburra.members.size * 2, pages.sumOf { it.photographs })

        // Y el mismo tipo en dos casillas sigue siendo dos fotos y no cuatro: la deduplicación no
        // se rompe por contar caras en vez de casillas.
        val repeated = section(kookaburra, faces = 2).let { plate ->
            plate.copy(cells = plate.cells.take(1) + plate.cells.take(1))
        }
        assertEquals(2, notebookPhotographs(printPages(listOf(repeated), doubled)).size)

        // Una cara que nadie fotografió es una foto menos que pedir, no una casilla rota: la otra
        // sigue contando, y el denominador del mensaje de cierre es el de las caras.
        val halfLit = pages.map { page ->
            page.copy(
                cells = page.cells.map { cell ->
                    cell.copy(faces = listOf(CoinPhoto()) + cell.faces.last())
                },
            )
        }
        assertEquals(kookaburra.members.size, halfLit.sumOf { it.photographs })
        assertEquals(kookaburra.members.size, notebookPhotographs(halfLit).size)
    }

    /**
     * The switch decides how many faces a cell has, and the cache only decides what is in them.
     *
     * A type the cache has never seen keeps its two slots empty rather than getting one: the cells
     * of a plate have to line up, and a lone coin where its neighbours print a pair reads as a
     * misprint. It is the same reason a hole keeps its own diameter (#169).
     */
    @Test
    fun `both faces gives every cell two slots, cached or not`() {
        val curation = Curation(catalogs)
        val assembled = curation.assemble(
            CollectionSnapshot(
                items = listOf(CollectedItem(id = 1, quantity = 1, typeId = 1885, issueId = 8508)),
                typeMeta = typeMeta,
            ),
        )
        val photographed = CollectionState(
            assembled,
            images = mapOf(
                1885 to TypeImages(
                    obverse = CoinPhoto(thumbnail = "anverso-180.jpg"),
                    reverse = CoinPhoto(thumbnail = "reverso-180.jpg"),
                ),
            ),
        )

        val faces = { state: CollectionState, options: NotebookOptions ->
            notebookSections(state, assembled.index, curation, options).single().cells.first().faces
        }

        // Una cara es el reverso y sólo el reverso, que es el cuaderno de hoy.
        assertEquals(
            listOf(CoinPhoto(thumbnail = "reverso-180.jpg")),
            faces(photographed, NotebookOptions()),
        )
        // Dos son el anverso y después el reverso, en ese orden: es como se lee una ficha.
        assertEquals(
            listOf(
                CoinPhoto(thumbnail = "anverso-180.jpg"),
                CoinPhoto(thumbnail = "reverso-180.jpg"),
            ),
            faces(photographed, NotebookOptions(bothFaces = true)),
        )
        // Y un tipo del que no hay ninguna foto conserva los dos huecos, vacíos.
        val blank = faces(CollectionState(assembled), NotebookOptions(bothFaces = true))
        assertEquals(listOf(CoinPhoto(), CoinPhoto()), blank)
        assertTrue(blank.none { it.hasPicture }, "un hueco vacío no pide ninguna foto")
    }

    /** Y con el interruptor apagado ninguna casilla lleva URL: el cuaderno de hoy, intacto. */
    @Test
    fun `with the switch off no cell carries a url at all`() {
        val paquillos = catalogs.first { it.id == "espana-paquillos" }
        val curation = Curation(catalogs)
        val assembled = curation.assemble(
            CollectionSnapshot(
                items = listOf(
                    CollectedItem(
                        id = 1,
                        quantity = 1,
                        typeId = 1885,
                        issueId = 8508,
                        issueYear = 1966,
                    ),
                ),
                typeMeta = typeMeta,
            ),
        )

        val cells = notebookSections(
            CollectionState(assembled),
            assembled.index,
            curation,
            NotebookOptions(),
        ).single().cells

        assertEquals(paquillos.members.size, cells.size)
        assertEquals(emptyList(), cells.mapNotNull { it.numistaUrl })
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
            // La casilla conserva su hueco y pierde la foto: es lo que le pasa a un tipo que la
            // caché no tiene, y lo que dibuja un hueco vacío es cosa del renderizador.
            plate.copy(cells = plate.cells.map { it.copy(faces = listOf(CoinPhoto())) })
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
