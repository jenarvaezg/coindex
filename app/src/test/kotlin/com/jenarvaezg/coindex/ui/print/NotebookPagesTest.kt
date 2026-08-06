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
import com.jenarvaezg.coindex.domain.PrintedSide
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

    /** Y compartiendo folio, que es el #232: la cabecera adelgaza y una lámina empieza donde otra acabó. */
    private val shared = printGeometry(NotebookOptions(sharePage = true))

    /** Y al 60 % del diámetro, que es el #233: la moneda encoge y la regla se va del pie. */
    private val scaled = printGeometry(NotebookOptions(actualSize = false))

    /** Los dos interruptores del papel juntos, que es donde el #233 rinde de verdad. */
    private val compact = printGeometry(NotebookOptions(actualSize = false, sharePage = true))

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
        section(catalogs.first { it.id == catalogId }).pagesAlone(paper)

    private fun pagesFor(vararg sections: PrintSection) = printPages(sections.toList(), paper)

    /**
     * Cuántas láminas ocupan una página, cuántas dos, y así.
     *
     * Se cuenta por bloques y no por páginas desde el #232: un folio puede llevar varias láminas, y
     * lo que este histograma sigue midiendo es la longitud de cada lámina —cuántos trozos se parte—
     * y no cuántos folios lleva el cuaderno, que es `pages.size`.
     */
    private fun lengths(pages: List<PrintPage>) = pages
        .flatMap { it.blocks }
        .groupBy { it.section.title }
        .map { (_, ofSection) -> ofSection.size }
        .groupingBy { it }
        .eachCount()
        .toSortedMap()

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
        assertEquals(118, catalogs.sumOf { section(it).pagesAlone(paper) })
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
        assertEquals(mapOf(1 to 51, 2 to 13, 3 to 2, 4 to 5, 6 to 1, 9 to 1), lengths(pages))
        assertEquals(118, pages.size)
        // Una lámina por folio: es la regla que el #232 levanta y que aquí sigue puesta.
        assertTrue(pages.all { it.blocks.size == 1 }, "una página lleva dos láminas sin pedirlo")

        // Y los cortes: ninguna casilla se pierde ni se repite, ninguna página va sobrecargada, y
        // sólo la última de cada lámina puede ir corta.
        sections.forEach { plate ->
            val ofPlate = pages.flatMap { it.blocks }.filter { it.section === plate }
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
        assertEquals(mapOf(1 to 46, 2 to 17, 3 to 2, 4 to 5, 5 to 1, 7 to 1, 9 to 1), lengths(pages))
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
            lengths(pages),
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
        assertEquals(mapOf(1 to 69, 2 to 2, 3 to 2), lengths(pages))
        // El suelo es una página por lámina, y sólo seis páginas del cuaderno están por encima de
        // él: lo que queda por ahorrar aquí ya no es de este interruptor.
        assertEquals(catalogs.size, pages.flatMap { it.blocks }.groupBy { it.section.title }.size)
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
            PrintPage(
                page.blocks.map { block ->
                    block.copy(
                        cells = block.cells.map { cell ->
                            cell.copy(faces = listOf(CoinPhoto()) + cell.faces.last())
                        },
                    )
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

        // Una cara es una y sólo una: la que la lámina declara —los paquillos imprimen el anverso
        // desde el #229, y cuál sea es asunto del test de más abajo y no de éste—.
        assertEquals(
            listOf(CoinPhoto(thumbnail = "anverso-180.jpg")),
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

    /**
     * Cuál de las dos caras se imprime lo declara la lámina, y su silencio es el reverso (#227).
     *
     * «Reverso de Numista» no es «la cara de la moneda»: en Haití el reverso es el escudo y la
     * sirena está en el anverso, y hoy el cuaderno imprime el escudo porque nadie eligió. Con la
     * declaración en la cabecera, la casilla saca la cara que el curador dice que **es** la moneda,
     * y la lámina que no declara nada saca el reverso, que es el cuaderno de hoy intacto.
     *
     * Lo que se calienta es la cara que se va a dibujar y no la otra (`notebookPhotographs`): una
     * lámina de anversos con el reverso en la cola de descargas sería un cuaderno con agujeros y una
     * petición inútil por moneda, que es exactamente el fallo del #169.
     */
    @Test
    fun `the plate declares which face goes to paper and silence is the reverse`() {
        val onlyPaquillos = listOf(
            CollectedItem(id = 1, quantity = 1, typeId = 1885, issueId = 8508, issueYear = 1966),
        )
        val sides = TypeImages(
            obverse = CoinPhoto(thumbnail = "anverso-180.jpg"),
            reverse = CoinPhoto(thumbnail = "reverso-180.jpg"),
        )
        val cellsOf = { shelf: List<CollectionCatalog>, options: NotebookOptions ->
            val curation = Curation(shelf)
            val assembled = curation.assemble(
                CollectionSnapshot(items = onlyPaquillos, typeMeta = typeMeta),
            )
            val state = CollectionState(assembled, images = mapOf(1885 to sides))
            notebookSections(state, assembled.index, curation, options).single().cells
        }
        val declaring = { side: PrintedSide ->
            catalogs.map { catalog ->
                if (catalog.id == "espana-paquillos") catalog.copy(printedSide = side) else catalog
            }
        }

        // Sin declaración, el reverso. Los paquillos ya no son una lámina callada —el #229 les
        // declaró el anverso, que es la cabeza de Franco—, así que el silencio se escribe aquí con
        // el valor que el silencio significa; que un fichero sin el campo se lea así lo fija
        // `FinishInferenceTest`.
        assertEquals(
            listOf(CoinPhoto(thumbnail = "reverso-180.jpg")),
            cellsOf(declaring(PrintedSide.Reverse), NotebookOptions()).first().faces,
        )
        // Declarándolo, el anverso, y en **todas** las casillas de la lámina: la excepción es de la
        // lámina entera y no de un miembro, así que aquí no hay dos criterios que puedan discrepar.
        val obverse = cellsOf(declaring(PrintedSide.Obverse), NotebookOptions())
        assertEquals(5, obverse.size)
        assertTrue(
            obverse.all { it.faces == listOf(CoinPhoto(thumbnail = "anverso-180.jpg")) },
            "una casilla de la lámina ha impreso otra cara que sus hermanas",
        )
        // Y lo que sale al papel es lo que el fichero curado declara, no lo que este test simule:
        // la lámina que viaja en el APK imprime el anverso.
        assertEquals(
            listOf(CoinPhoto(thumbnail = "anverso-180.jpg")),
            cellsOf(catalogs, NotebookOptions()).first().faces,
        )

        // Con «ambas caras» la declaración no pinta nada: se imprimen las dos, anverso y después
        // reverso, que es como se lee una ficha (#230). Es justo el caso donde deja de importar.
        val both = NotebookOptions(bothFaces = true)
        assertEquals(
            listOf(
                CoinPhoto(thumbnail = "anverso-180.jpg"),
                CoinPhoto(thumbnail = "reverso-180.jpg"),
            ),
            cellsOf(declaring(PrintedSide.Obverse), both).first().faces,
        )
        assertEquals(
            cellsOf(declaring(PrintedSide.Reverse), both),
            cellsOf(declaring(PrintedSide.Obverse), both),
        )

        // Y la cola de descargas es la de la cara declarada: una sola foto, la que se dibuja.
        val plate = PrintSection(
            eyebrow = "COINDEX · CATÁLOGO CURADO",
            title = "Paquillos",
            subtitle = null,
            facts = emptyList(),
            source = "https://en.numista.com/catalogue/pieces1885.html",
            cells = cellsOf(declaring(PrintedSide.Obverse), NotebookOptions()),
        )
        assertEquals(
            listOf("anverso-180.jpg"),
            notebookPhotographs(printPages(listOf(plate), paper)),
        )
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

    /**
     * What sharing a folio is worth, which is more than any other lever of paper (#232).
     *
     * The floor is what it takes away, and the floor was almost all of it: seventy-three plates are
     * seventy-three pages before a member is printed, nineteen of the sixty measured did not fill
     * half a page, and 29 % of the printed cells came out empty. Both halves of the switch are in
     * this one number — a plate may start where the last one ended, **and** the band over it drops
     * from the forty millimetres of the album's masthead to fourteen, which is where most of the
     * saving is (the ticket measured 90 pages sharing folios with the masthead against 73 with the
     * thin band).
     *
     * **Not the 73 of the ticket, and the shelf is why**: those are the sixty curated plates whose
     * notebook of today was 104. These are the seventy-three that are shipped now, whose notebook of
     * today is 118. It is the same correction #231 and #234 had to make — the shelf grows every week
     * and a measured fact goes stale with it, so what is pinned here is the measurement of the shelf
     * as it stands and the *shape* of the saving beside it. A third off, which is what the ticket
     * promised and what the emulator printed: 89 folios of a real collection came out as 53.
     */
    @Test
    fun `sharing a folio takes a third off the notebook and loses no cell`() {
        val sections = catalogs.map(::section)

        val pages = printPages(sections, shared)

        assertEquals(80, pages.size)
        // La cuenta de láminas no se mueve: lo que cambia es cuántas caben en un folio, no cuántas
        // hay. Ninguna se cae del cuaderno por compartir folio con otra.
        assertEquals(
            sections.size,
            pages.flatMap { it.blocks }.map { it.section.title }.distinct().size,
        )
        // En cuántos trozos se parte cada lámina, que ya no es su longitud a solas: 39 salen de una
        // pieza y una se parte en siete. La lámina más larga —el Libro Rojo de Rusia— pasa de nueve
        // páginas a siete, porque la banda fina le da una cuarta fila de casillas en cada folio; y
        // las que se parten más que antes lo hacen empezando a media hoja ajena, que es de donde
        // sale el ahorro. Son 125 trozos en 80 folios, y 39 de esos folios llevan más de una lámina.
        assertEquals(mapOf(1 to 39, 2 to 25, 3 to 6, 5 to 1, 6 to 1, 7 to 1), lengths(pages))
        assertEquals(125, pages.sumOf { it.blocks.size })
        assertEquals(39, pages.count { it.blocks.size > 1 })

        // Ninguna casilla se pierde ni se repite, y las de cada lámina siguen en su orden.
        assertEquals(
            sections.flatMap { it.cells },
            pages.flatMap { it.blocks }.flatMap { it.cells },
        )

        // Y ningún folio se sale del papel: es la aritmética que el empaquetador hace antes de
        // dibujar, sumada aquí como la sumaría una regla puesta sobre la hoja impresa.
        pages.forEach { page ->
            val used = page.blocks.sumOf { it.heightMm.toDouble() }.toFloat() +
                shared.blockGapMm * (page.blocks.size - 1)
            assertTrue(
                used <= shared.contentHeightMm + 0.01f,
                "un folio de ${page.blocks.size} láminas mide $used mm",
            )
        }
    }

    /**
     * What the coins at three fifths are worth, which is **only half of it without #232** (#233).
     *
     * 118 folios become 84 on their own and **43 sharing them**, which is the number the ticket is
     * really about: the floor of «una lámina, un folio» is seventy-three pages before a member is
     * printed, so a switch that only makes cells smaller runs into it eleven pages later. Shrinking the
     * coins and sharing the folio are the two levers that multiply — 80 pages become 43, and the album
     * of 118 comes out as a catalogue of 43 — and neither had to learn about the other to do it.
     *
     * **Not the 73 → 34 of the ticket, and the shelf is why**: those were measured on the sixty curated
     * plates whose notebook of today was 104. These are the seventy-three that are shipped now, whose
     * notebook of today is 118 — the same correction #231, #232 and #234 each had to make, because a
     * measured fact goes stale as the shelf grows. What holds is the shape: a little over a third of the
     * paper, and the plate stops being the floor — 32 of the 43 folios carry more than one.
     */
    @Test
    fun `scaling the coins halves the notebook, and halves it again on shared folios`() {
        val sections = catalogs.map(::section)

        val alone = printPages(sections, scaled)
        val pages = printPages(sections, compact)

        assertEquals(84, alone.size)
        assertEquals(43, pages.size)
        // Sola, la lámina sigue siendo el suelo: 65 de las 73 caben en un folio y no se ahorra más.
        assertEquals(mapOf(1 to 65, 2 to 6, 3 to 1, 4 to 1), lengths(alone))
        assertEquals(catalogs.size, alone.size - 11)
        // Compartiendo, 94 trozos en 43 folios y 32 de ellos con más de una lámina.
        assertEquals(mapOf(1 to 57, 2 to 13, 3 to 2, 5 to 1), lengths(pages))
        assertEquals(94, pages.sumOf { it.blocks.size })
        assertEquals(32, pages.count { it.blocks.size > 1 })
        // Ninguna casilla se pierde ni se repite por encogerla.
        assertEquals(
            sections.flatMap { it.cells },
            pages.flatMap { it.blocks }.flatMap { it.cells },
        )
        // Y ningún folio se sale del papel, sumado como lo sumaría una regla sobre la hoja impresa.
        pages.forEach { page ->
            val used = page.blocks.sumOf { it.heightMm.toDouble() }.toFloat() +
                compact.blockGapMm * (page.blocks.size - 1)
            assertTrue(
                used <= compact.contentHeightMm + 0.01f,
                "un folio de ${page.blocks.size} láminas mide $used mm",
            )
        }
    }

    /**
     * The same, cell by cell: what shrinks is the circle and the columns are what it buys.
     *
     * The grid of a plate stops being fixed by its largest coin's **printed** size alone — the floor is
     * still there, an eighteen-millimetre one — and the check the ticket asks for is that the small-coin
     * plates are the ones that gain most: the Venezuelan medios were the reason the floor exists at all,
     * and at three fifths they go from thirty cells a page to fifty-six.
     *
     * The real diameter survives all of it, on the grid and in the cell, because that is what a caption
     * with no ruler under it has to print as a number.
     */
    @Test
    fun `every plate keeps its real diameter and gains columns`() {
        val ounces = section(catalogs.first { it.id == "australian-kookaburra-perth-1oz" })
        val roubles = section(
            catalogs.first { it.id == "outstanding-personalities-russia-2-roubles" },
        )
        val medios = section(catalogs.first { it.id == "venezuela-medios" })

        // La onza: de doce casillas por página a veinticuatro, y el círculo de 40,9 sale a 24,5.
        assertEquals(12, ounces.grid(paper).cellsPerPage)
        assertEquals(24, ounces.grid(scaled).cellsPerPage)
        assertEquals(4 to 3, ounces.grid(paper).columns to ounces.grid(paper).rows)
        assertEquals(6 to 4, ounces.grid(scaled).columns to ounces.grid(scaled).rows)
        assertEquals(40.9f, ounces.grid(scaled).diameterMm)
        assertEquals(24.54f, ounces.grid(scaled).printedDiameterMm, 0.01f)
        // Las rusas de 33 mm: de veinte a cuarenta, cinco columnas a ocho.
        assertEquals(20, roubles.grid(paper).cellsPerPage)
        assertEquals(40, roubles.grid(scaled).cellsPerPage)
        // Y los medios venezolanos de 16 mm, que son los que el suelo de 28 mm estaba sosteniendo: la
        // casilla es ahora el suelo nuevo de 18 y no la moneda, que sale a 9,6.
        assertEquals(30, medios.grid(paper).cellsPerPage)
        assertEquals(56, medios.grid(scaled).cellsPerPage)
        assertEquals(18f, medios.grid(scaled).cellWidthMm)
        assertEquals(9.6f, medios.grid(scaled).printedDiameterMm, 0.01f)
    }

    /**
     * The check the ticket asks for: two short plates on one folio, each under its own heading.
     *
     * Three coins and five coins are two plates that today take a page each and leave most of it
     * white. Sharing, they are one folio with two headings on it — which is the other half of what a
     * heading is for once a page can hold two plates: not only «which collection is this» after the
     * page turned, but where one stops and the next begins.
     */
    @Test
    fun `a plate of three and a plate of five come out on the same folio`() {
        val ounces = section(catalogs.first { it.id == "australian-kookaburra-perth-1oz" })
        val three = ounces.copy(title = "Tres onzas", cells = ounces.cells.take(3))
        val five = ounces.copy(title = "Cinco onzas", cells = ounces.cells.take(5))

        // Apartadas son dos folios, cada una con una página casi vacía.
        assertEquals(2, printPages(listOf(three, five), paper).size)

        val folio = printPages(listOf(three, five), shared).single()

        assertEquals(listOf("Tres onzas", "Cinco onzas"), folio.blocks.map { it.section.title })
        assertEquals(listOf(3, 5), folio.blocks.map { it.cells.size })
        // Cada una con su cabecera fina, y ninguna diciendo «2 de 2»: son dos láminas enteras.
        assertTrue(folio.blocks.all { it.pagesInSection == 1 })
        assertEquals(14f, shared.headingMm)
        // Y cada una centrada en lo suyo: la de tres no se descoloca por la de cinco debajo.
        assertEquals(3, folio.blocks.first().columnsUsed)
        assertEquals(4, folio.blocks.last().columnsUsed)
    }

    /**
     * A plate that spills still says «2 de 4» and still lines its columns up, folio shared or not.
     *
     * That is the third thing the ticket asks for, and it is the one the packer could most easily
     * have broken: «2 de 4» is no longer `pageCount` of the plate on its own — a plate that starts
     * halfway down somebody else's folio is cut differently — so the number and the total are read
     * off the finished notebook instead. The columns are the other half: the pages of one plate are
     * read as a run, so a tail row keeps the grid's columns even where it holds one coin.
     */
    @Test
    fun `a plate that spills says two of four and keeps its columns on a shared folio`() {
        val ounces = section(catalogs.first { it.id == "australian-kookaburra-perth-1oz" })
        val three = ounces.copy(title = "Tres onzas", cells = ounces.cells.take(3))

        // La lámina de tres deja sitio para dos filas del Kookaburra, que se lleva las demás detrás.
        val pages = printPages(listOf(three, ounces), shared)
        val spilled = pages.flatMap { it.blocks }.filter { it.section === ounces }

        assertEquals(37, ounces.cells.size)
        assertEquals(listOf(8, 16, 13), spilled.map { it.cells.size })
        assertEquals(listOf(1, 2, 3), spilled.map { it.numberInSection })
        assertTrue(spilled.all { it.pagesInSection == 3 }, "la lámina no sabe cuántos trozos es")
        // Ninguna casilla perdida en los cortes, y las columnas alineadas de un folio al siguiente
        // —incluso las cinco de la cola, que no se centran porque continúan una columna.
        assertEquals(ounces.cells, spilled.flatMap { it.cells })
        assertTrue(
            spilled.all { it.columnsUsed == it.grid.columns },
            "una página de una lámina que se derrama ha movido sus columnas",
        )
    }

    /**
     * A collection with nothing in it costs its heading and not a folio (#232).
     *
     * An emptied box survives (ADR 0021 §11), and on paper it is fourteen millimetres saying there is
     * nothing in it. Sharing folios, giving that a page of its own would be the same waste the switch
     * exists to take away — so the packer asks whether the *block* fits and not whether a row of it
     * does, and zero rows is a real answer rather than «no cabe».
     */
    @Test
    fun `an empty collection costs its heading and not a whole folio`() {
        val ounces = section(catalogs.first { it.id == "australian-kookaburra-perth-1oz" })
        val three = ounces.copy(title = "Tres onzas", cells = ounces.cells.take(3))
        val nothing = ounces.copy(title = "Caja vacía", cells = emptyList())

        // Sin compartir son tres folios: cada lámina abre el suyo, la vacía incluida.
        assertEquals(3, printPages(listOf(three, nothing, three), paper).size)

        val folio = printPages(listOf(three, nothing, three), shared).single()

        assertEquals(
            listOf("Tres onzas", "Caja vacía", "Tres onzas"),
            folio.blocks.map { it.section.title },
        )
        val empty = folio.blocks[1]
        assertEquals(emptyList(), empty.cells)
        assertEquals(0, empty.rows)
        assertEquals(shared.headingMm, empty.heightMm)
    }

    @Test
    fun `a plate that spills repeats its heading and numbers its pages`() {
        val kookaburra = section(catalogs.first { it.id == "australian-kookaburra-perth-1oz" })
        val pages = pagesFor(kookaburra)
        val blocks = pages.flatMap { it.blocks }

        assertEquals(4, pages.size)
        assertEquals(listOf(1, 2, 3, 4), blocks.map { it.numberInSection })
        assertTrue(blocks.all { it.pagesInSection == 4 })
        // Every page carries the same heading, because on paper there is no scrolling back.
        assertTrue(blocks.all { it.section.title == kookaburra.title })
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
        val single = pagesFor(escudos).single().blocks.single()

        assertEquals(4, escudos.grid(paper).columns)
        assertEquals(3, single.columnsUsed)
        assertEquals(escudos.grid(paper).widthOfMm(3), single.blockWidthMm)

        val kookaburra = pagesFor(
            section(catalogs.first { it.id == "australian-kookaburra-perth-1oz" }),
        )
        assertEquals(1, kookaburra.last().cells.size)
        assertTrue(
            kookaburra.flatMap { it.blocks }.all { it.blockWidthMm == it.grid.blockWidthMm },
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
