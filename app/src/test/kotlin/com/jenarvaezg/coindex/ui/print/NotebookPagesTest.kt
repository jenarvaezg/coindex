package com.jenarvaezg.coindex.ui.print

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.SHIPPED_CURATION
import com.jenarvaezg.coindex.data.TypeCacheFile
import com.jenarvaezg.coindex.data.numista.NumistaTypeDto
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.data.toDomain
import com.jenarvaezg.coindex.data.typeMetaEntity
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
 * The shipped catalogs paginated on A4 (#169). Diameters come from the seeded type cache through
 * `raw`, as on the phone. Page counts are never pinned, since the shelf grows: each switch is
 * measured against the default notebook in the same run.
 */
class NotebookPagesTest {
    private val json = Json { ignoreUnknownKeys = true }

    /** La configuración por omisión (#228), contra la que se mide cada interruptor. */
    private val paper = printGeometry(NotebookOptions())

    private val catalogs: List<CollectionCatalog> = SHIPPED_CURATION.catalogs

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

    /** «QR de Numista» (#234). */
    private val coded = printGeometry(NotebookOptions(numistaQr = true))

    /** «Ambas caras» (#230). */
    private val doubled = printGeometry(NotebookOptions(bothFaces = true))

    /** Sin fotos (#231): cada casilla es una línea. */
    private val listed = printGeometry(NotebookOptions(photographs = false))

    /** Compartir folio (#232). */
    private val shared = printGeometry(NotebookOptions(sharePage = true))

    /** Al 60 % del diámetro y sin regla (#233). */
    private val scaled = printGeometry(NotebookOptions(actualSize = false))

    /** Escala y folio compartido a la vez (#233). */
    private val compact = printGeometry(NotebookOptions(actualSize = false, sharePage = true))

    /**
     * One catalog with nothing owned yet. [faces] is 2 for «ambas caras» (#230) and 0 for «sin
     * fotos» (#231); it is also how many photographs each cell needs warmed.
     */
    private fun section(catalog: CollectionCatalog, faces: Int = 1) = PrintSection(
        eyebrow = "COINDEX · CATÁLOGO CURADO",
        title = catalog.name,
        subtitle = null,
        facts = emptyList(),
        source = catalog.source,
        cells = catalog.members.map { member ->
            PrintCell(
                curatedLabel = member.label,
                state = "Me falta",
                footnote = member.year?.toString(),
                diameterMm = member.numistaTypeId
                    ?.let { typeMeta[it]?.sizeMillimetres?.toFloat() },
                // Thumbnail and original, as a cached type has; the thumbnail is what gets warmed.
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

    private fun pagesFor(vararg sections: PrintSection) = printPages(sections.toList(), paper)

    @Test
    fun `every seeded type carries the diameter the printed page is measured with`() {
        val withoutSize = typeMeta.values.filter { it.sizeMillimetres == null }
        assertTrue(withoutSize.isEmpty(), "fichas sin diámetro: ${withoutSize.map { it.id }}")
    }

    /**
     * #228 threads the geometry through `notebookSections`, `printGrid` and `printPages`; a
     * millimetre lost on the way would drop or repeat a cell.
     */
    @Test
    fun `the default configuration reproduces today's notebook plate by plate`() {
        val sections = catalogs.map(::section)

        val pages = printPages(sections, paper)

        // Una lámina por folio mientras no se pida compartir (#232).
        assertTrue(pages.all { it.blocks.size == 1 }, "una página lleva dos láminas sin pedirlo")
        assertEquals(
            sections.size,
            pages.flatMap { it.blocks }.map { it.section.title }.distinct().size,
        )

        // Sólo la última página de una lámina puede ir corta; las continuaciones caben más (#480).
        sections.forEach { plate ->
            val ofPlate = pages.flatMap { it.blocks }.filter { it.section === plate }
            assertEquals(plate.cells, ofPlate.flatMap { it.cells }, "corte roto: ${plate.title}")
            val grid = ofPlate.first().grid
            val holds = { block: PrintBlock ->
                if (block.numberInSection == 1) grid.cellsPerPage else grid.continuationCellsPerPage
            }
            assertTrue(
                ofPlate.dropLast(1).all { it.cells.size == holds(it) },
                "una página intermedia va corta en ${plate.title}",
            )
            assertTrue(
                ofPlate.last().cells.size <= holds(ofPlate.last()),
                "una página va sobrecargada en ${plate.title}",
            )
        }
    }

    /**
     * The QR grows cells in height, never in width (#234, #478), so the notebook only gets longer
     * and no plate loses a column.
     */
    @Test
    fun `the qr grows the notebook by a few pages and never takes a column`() {
        val sections = catalogs.map(::section)

        val pages = printPages(sections, coded)

        // El cuaderno crece y ninguna lámina se acorta.
        assertTrue(pages.size > printPages(sections, paper).size, "el código sale gratis")
        sections.forEach { plate ->
            assertTrue(
                plate.pagesAlone(coded) >= plate.pagesAlone(paper),
                "el código ha acortado ${plate.title}",
            )
        }
        // La casilla crece de alto y no de ancho: las rusas de 33 mm siguen cinco por fila.
        val roubles = section(
            catalogs.first { it.id == "outstanding-personalities-russia-2-roubles" },
        )
        assertEquals(5, roubles.grid(paper).columns)
        assertEquals(5, roubles.grid(coded).columns)
        assertEquals(roubles.grid(paper).rows - 1, roubles.grid(coded).rows)
    }

    /**
     * Five members of one Numista type told apart by `numista_issue_ids` (ADR 0019). Numista has no
     * URL per issue, and an anchor would make the code a version 3 (42 characters).
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
            emptyList(),
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
     * «Ambas caras» (#230): an ounce cell goes from 40,9 mm to 84,8 and a page from twelve cells to
     * six. The total isn't exactly double: three columns can drop to one, small plates still fit.
     */
    @Test
    fun `both faces doubles the cell and very nearly doubles the notebook`() {
        val sections = catalogs.map { section(it, faces = 2) }

        val pages = printPages(sections, doubled)

        val plain = printPages(catalogs.map(::section), paper)
        assertTrue(pages.size > plain.size * 1.5, "dos caras no llegan a hora y media de papel")
        assertTrue(pages.size < plain.size * 2.5, "dos caras cuestan más del doble largo")

        // La onza australiana, casilla a casilla (#230).
        val kookaburra = catalogs.first { it.id == "australian-kookaburra-perth-1oz" }
        val one = section(kookaburra).grid(paper)
        val two = section(kookaburra, faces = 2).grid(doubled)
        assertEquals(12, one.cellsPerPage)
        assertEquals(6, two.cellsPerPage)
        assertEquals(4 to 3, one.columns to one.rows)
        assertEquals(2 to 3, two.columns to two.rows)
        // Dos monedas y la calle de ancho; el alto no cambia.
        assertEquals(one.cellWidthMm * 2 + paper.gutterMm, two.cellWidthMm, 0.01f)
        assertEquals(one.cellHeightMm, two.cellHeightMm)
        assertTrue(two.blockWidthMm <= doubled.gridWidthMm, "dos caras se salen: ${two.blockWidthMm}")
    }

    /**
     * «Sin fotos» (#231): nearly every plate fits on one page, the floor until «compartir página»
     * (#232). Lines are 7 mm at a 10 mm pitch because the column gutter also separates rows; a row
     * gutter of its own wasn't worth another geometry field.
     */
    @Test
    fun `with no photographs the shelf is a list and the plate is its floor`() {
        val sections = catalogs.map { section(it, faces = 0) }

        val pages = printPages(sections, listed)

        // Una página por lámina es el suelo; bajar de ahí es cosa del #232.
        assertTrue(pages.size < printPages(catalogs.map(::section), paper).size / 1.3)
        assertEquals(catalogs.size, pages.flatMap { it.blocks }.groupBy { it.section.title }.size)
        assertTrue(pages.size >= catalogs.size, "una lámina ha compartido folio sin pedirlo")
        assertTrue(
            pages.size < catalogs.size * 1.2,
            "el cuaderno en lista se ha despegado del suelo: ${pages.size} folios",
        )

        // La onza pasa de 12 por página a 46, con la misma rejilla que los medios venezolanos.
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

    /** It follows from the cells having no faces, not from a check of its own (#231). */
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

        // Tampoco con «ambas caras» marcada de antes, que la hoja pone en gris.
        listOf(
            NotebookOptions(photographs = false),
            NotebookOptions(photographs = false, bothFaces = true),
        ).forEach { options ->
            val cells = notebookSections(photographed, assembled.index, emptyList(), curation, options)
                .single()
                .cells

            assertTrue(cells.isNotEmpty(), "una lista sin casillas no es una lista")
            assertEquals(emptyList(), cells.flatMap { it.faces }, "una línea ha pedido una foto")
        }

        val pages = printPages(catalogs.map { section(it, faces = 0) }, listed)
        assertEquals(emptyList(), notebookPhotographs(pages))
        assertEquals(0, pages.sumOf { it.photographs })
        // Sin fotos que esperar, el mensaje de cierre no habla de fotos.
        assertEquals(
            "Cuaderno completo exportado · ${pages.size} páginas",
            notebookExportMessage(pages.size, expectedPhotos = 0, loadedPhotos = 0),
        )
    }

    /** The warm-up list (#169): a face nobody photographed costs one photo, not the cell. */
    @Test
    fun `both faces asks for two photographs per type and counts them one by one`() {
        val kookaburra = catalogs.first { it.id == "australian-kookaburra-perth-1oz" }
        val pages = printPages(listOf(section(kookaburra, faces = 2)), doubled)

        val urls = notebookPhotographs(pages)

        assertEquals(kookaburra.members.size * 2, urls.size)
        assertEquals(urls.size, urls.distinct().size)
        assertEquals(kookaburra.members.size * 2, pages.sumOf { it.photographs })

        // El mismo tipo en dos casillas sigue siendo dos fotos, no cuatro.
        val repeated = section(kookaburra, faces = 2).let { plate ->
            plate.copy(cells = plate.cells.take(1) + plate.cells.take(1))
        }
        assertEquals(2, notebookPhotographs(printPages(listOf(repeated), doubled)).size)

        // Una cara sin foto es una foto menos que pedir; la otra sigue contando.
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

    /** The cells of a plate must line up, so an uncached type keeps two empty slots, not one. */
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
            notebookSections(state, assembled.index, emptyList(), curation, options).single().cells.first().faces
        }

        // Una cara: la que declara la lámina (los paquillos, el anverso desde el #229).
        assertEquals(
            listOf(CoinPhoto(thumbnail = "anverso-180.jpg")),
            faces(photographed, NotebookOptions()),
        )
        // Dos: anverso y luego reverso, como se lee una ficha.
        assertEquals(
            listOf(
                CoinPhoto(thumbnail = "anverso-180.jpg"),
                CoinPhoto(thumbnail = "reverso-180.jpg"),
            ),
            faces(photographed, NotebookOptions(bothFaces = true)),
        )
        // Un tipo sin fotos conserva los dos huecos, vacíos.
        val blank = faces(CollectionState(assembled), NotebookOptions(bothFaces = true))
        assertEquals(listOf(CoinPhoto(), CoinPhoto()), blank)
        assertTrue(blank.none { it.hasPicture }, "un hueco vacío no pide ninguna foto")
    }

    /**
     * El reverso de Numista no siempre es la cara de la moneda: en Haití es el escudo, y la sirena
     * va en el anverso (#227). Sólo se calienta la cara que se dibuja (#169).
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
            notebookSections(state, assembled.index, emptyList(), curation, options).single().cells
        }
        val declaring = { side: PrintedSide ->
            catalogs.map { catalog ->
                if (catalog.id == "espana-paquillos") catalog.copy(printedSide = side) else catalog
            }
        }

        // Sin declaración, el reverso. Los paquillos declaran el anverso (#229), así que aquí el
        // silencio va explícito; `FinishInferenceTest` fija que el campo ausente se lea así.
        assertEquals(
            listOf(CoinPhoto(thumbnail = "reverso-180.jpg")),
            cellsOf(declaring(PrintedSide.Reverse), NotebookOptions()).first().faces,
        )
        // El anverso, en todas las casillas: la declaración es de la lámina, no del miembro.
        val obverse = cellsOf(declaring(PrintedSide.Obverse), NotebookOptions())
        assertEquals(5, obverse.size)
        assertTrue(
            obverse.all { it.faces == listOf(CoinPhoto(thumbnail = "anverso-180.jpg")) },
            "una casilla de la lámina ha impreso otra cara que sus hermanas",
        )
        // La lámina que viaja en el APK imprime el anverso.
        assertEquals(
            listOf(CoinPhoto(thumbnail = "anverso-180.jpg")),
            cellsOf(catalogs, NotebookOptions()).first().faces,
        )

        // Con «ambas caras» la declaración no importa: anverso y luego reverso (#230).
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

        // Sólo se descarga la cara declarada.
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

    @Test
    fun `a plate PDF keeps labeled progress but prints no status per hole`() {
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

        val plate = notebookSections(
            CollectionState(assembled),
            listOf(assembled.index.single()),
            emptyList(),
            curation,
            NotebookOptions(),
        ).single()

        assertEquals("Progreso", plate.facts.first().first)
        assertTrue(plate.cells.all { it.state == null })
    }

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
            emptyList(),
            curation,
            NotebookOptions(),
        ).single().cells

        assertEquals(paquillos.members.size, cells.size)
        assertEquals(emptyList(), cells.mapNotNull { it.numistaUrl })
    }

    /**
     * «Compartir página» (#232): a plate may start where the last one ended, under a 14 mm band
     * instead of the 40 mm masthead. Most of the saving comes from the thinner band.
     */
    @Test
    fun `sharing a folio takes a third off the notebook and loses no cell`() {
        val sections = catalogs.map(::section)

        val pages = printPages(sections, shared)

        // Se ahorra el blanco que dejaba «una lámina, un folio».
        val alone = printPages(sections, paper).size
        assertTrue(
            pages.size < alone * 0.75,
            "compartir folio no llega al tercio: ${pages.size} de $alone",
        )
        // Ninguna lámina se cae del cuaderno por compartir folio.
        assertEquals(
            sections.size,
            pages.flatMap { it.blocks }.map { it.section.title }.distinct().size,
        )
        assertTrue(
            pages.count { it.blocks.size > 1 } > 0,
            "ningún folio ha llegado a llevar dos láminas",
        )

        // Ninguna casilla se pierde ni se repite, y las de cada lámina siguen en su orden.
        assertEquals(
            sections.flatMap { it.cells },
            pages.flatMap { it.blocks }.flatMap { it.cells },
        )

        // Ningún folio se sale del papel, sumado como lo sumaría una regla sobre la hoja.
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
     * Alone, scaling (#233) can't go below one page per plate; with shared folios (#232) the two
     * multiply and the plate stops being the floor.
     */
    @Test
    fun `scaling the coins halves the notebook, and halves it again on shared folios`() {
        val sections = catalogs.map(::section)

        val alone = printPages(sections, scaled)
        val pages = printPages(sections, compact)

        // Sola, la escala se queda justo encima del suelo de una página por lámina.
        assertTrue(alone.size >= sections.size, "una lámina ha compartido folio sin pedirlo")
        assertTrue(alone.size < sections.size * 1.25, "encoger no ha llegado al suelo")
        // Juntos, por debajo del suelo y de lo que da cada uno.
        assertTrue(pages.size < alone.size * 0.75, "los dos juntos no rinden más que la escala sola")
        assertTrue(
            pages.size < printPages(sections, shared).size,
            "los dos juntos no rinden más que compartir folio solo",
        )
        assertTrue(pages.count { it.blocks.size > 1 } > pages.size / 2)
        // Ninguna casilla se pierde ni se repite por encogerla.
        assertEquals(
            sections.flatMap { it.cells },
            pages.flatMap { it.blocks }.flatMap { it.cells },
        )
        // Ningún folio se sale del papel.
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
     * Small coins gain most, down to the 18 mm floor. The real diameter is kept, since a caption
     * with no ruler prints it as a number (#233).
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
        // Los medios de 16 mm salen a 9,6, así que la casilla es el suelo nuevo de 18 mm.
        assertEquals(30, medios.grid(paper).cellsPerPage)
        assertEquals(56, medios.grid(scaled).cellsPerPage)
        assertEquals(18f, medios.grid(scaled).cellWidthMm)
        assertEquals(9.6f, medios.grid(scaled).printedDiameterMm, 0.01f)
    }

    /** On a shared folio the heading also marks where one plate ends and the next begins (#232). */
    @Test
    fun `a plate of three and a plate of five come out on the same folio`() {
        val ounces = section(catalogs.first { it.id == "australian-kookaburra-perth-1oz" })
        val three = ounces.copy(title = "Tres onzas", cells = ounces.cells.take(3))
        val five = ounces.copy(title = "Cinco onzas", cells = ounces.cells.take(5))

        // Sin compartir, dos folios casi vacíos.
        assertEquals(2, printPages(listOf(three, five), paper).size)

        val folio = printPages(listOf(three, five), shared).single()

        assertEquals(listOf("Tres onzas", "Cinco onzas"), folio.blocks.map { it.section.title })
        assertEquals(listOf(3, 5), folio.blocks.map { it.cells.size })
        // Cabecera fina y sin «2 de 2»: son dos láminas enteras.
        assertTrue(folio.blocks.all { it.pagesInSection == 1 })
        assertEquals(14f, shared.headingMm)
        // Cada una centrada en lo suyo.
        assertEquals(3, folio.blocks.first().columnsUsed)
        assertEquals(4, folio.blocks.last().columnsUsed)
    }

    /**
     * A plate starting halfway down another's folio is cut differently, so «2 de 4» is read off the
     * finished notebook, not `pageCount` (#232). Its pages read as a run: the columns stay aligned.
     */
    @Test
    fun `a plate that spills says two of four and keeps its columns on a shared folio`() {
        val ounces = section(catalogs.first { it.id == "australian-kookaburra-perth-1oz" })
        val three = ounces.copy(title = "Tres onzas", cells = ounces.cells.take(3))

        // La de tres deja sitio para dos filas del Kookaburra, que se lleva el resto detrás.
        val pages = printPages(listOf(three, ounces), shared)
        val spilled = pages.flatMap { it.blocks }.filter { it.section === ounces }

        // La primera va corta —lo que le dejó la de tres— y las de en medio van llenas.
        assertTrue(spilled.size >= 2, "la onza ya no se derrama detrás de una lámina de tres")
        assertEquals(spilled.indices.map { it + 1 }, spilled.map { it.numberInSection })
        assertTrue(
            spilled.all { it.pagesInSection == spilled.size },
            "la lámina no sabe cuántos trozos es",
        )
        // Ninguna casilla perdida, y las columnas alineadas entre folios, cola incluida.
        assertEquals(ounces.cells, spilled.flatMap { it.cells })
        assertTrue(
            spilled.all { it.columnsUsed == it.grid.columns },
            "una página de una lámina que se derrama ha movido sus columnas",
        )
    }

    /** An emptied box survives (ADR 0021 §11); the packer fits it as zero rows (#232). */
    @Test
    fun `an empty collection costs its heading and not a whole folio`() {
        val ounces = section(catalogs.first { it.id == "australian-kookaburra-perth-1oz" })
        val three = ounces.copy(title = "Tres onzas", cells = ounces.cells.take(3))
        val nothing = ounces.copy(title = "Caja vacía", cells = emptyList())

        // Sin compartir son tres folios, la vacía incluida.
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
        val grid = kookaburra.grid(paper)

        assertTrue(pages.size > 1, "la onza australiana ya no se derrama en varias páginas")
        assertEquals(pages.indices.map { it + 1 }, blocks.map { it.numberInSection })
        assertTrue(blocks.all { it.pagesInSection == pages.size })
        // Every page carries the same heading, because on paper there is no scrolling back.
        assertTrue(blocks.all { it.section.title == kookaburra.title })
        // No cell is lost or repeated across the break.
        assertEquals(kookaburra.cells, pages.flatMap { it.cells })
        assertEquals(grid.cellsPerPage, pages.first().cells.size)
        assertTrue(
            pages.last().cells.size <= grid.continuationCellsPerPage,
            "la última página va sobrecargada",
        )
    }

    /**
     * Continuations repeat the thin band of #232 (#480): the name stays, the specification and the
     * second title line go, and that buys a row of coins.
     */
    @Test
    fun `a spilled plate prints its masthead once and its name on every page after`() {
        val kookaburra = section(catalogs.first { it.id == "australian-kookaburra-perth-1oz" })

        val blocks = pagesFor(kookaburra).flatMap { it.blocks }

        assertTrue(blocks.size > 1, "la onza australiana ya no se derrama")
        assertEquals(PrintHeading.Masthead, blocks.first().heading)
        assertTrue(
            blocks.drop(1).all { it.heading == PrintHeading.Slim },
            "una página de continuación sigue repitiendo el masthead",
        )
        // La banda fina sigue nombrando la lámina; sólo se cae la especificación.
        assertTrue(blocks.all { it.section.title == kookaburra.title })
        assertTrue(blocks.drop(1).none { it.heading.facts })
        assertTrue(blocks.drop(1).all { it.heading.titleLines >= 1 })
        // Doce onzas en la primera y dieciséis en cada continuación.
        assertEquals(12, blocks.first().cells.size)
        assertTrue(
            blocks.drop(1).dropLast(1).all { it.cells.size == 16 },
            "una continuación no se ha llevado la fila que la banda fina le deja",
        )
        // El empaquetador y `pageCount` cuentan lo mismo.
        assertEquals(kookaburra.pagesAlone(paper), blocks.size)
    }

    /**
     * With the collector's options (both faces and QR, one plate per folio), measured against the
     * same notebook with one band for every page (#480).
     */
    @Test
    fun `the thin band on continuation pages takes folios off and loses no cell`() {
        val his = printGeometry(NotebookOptions(bothFaces = true, numistaQr = true))
        val sections = catalogs.map { section(it, faces = 2) }

        val pages = printPages(sections, his)

        // Lo que costaba con una sola banda para todas las páginas de una lámina.
        val before = sections.sumOf { plate ->
            val perPage = plate.grid(his).cellsPerPage.coerceAtLeast(1)
            ((plate.cells.size + perPage - 1) / perPage).coerceAtLeast(1)
        }
        assertTrue(
            pages.size < before * 0.95,
            "la banda fina al continuar no ha ahorrado nada: ${pages.size} de $before",
        )
        // Una lámina por folio, como sus opciones piden, y ni una casilla perdida ni repetida.
        assertTrue(pages.all { it.blocks.size == 1 }, "una página lleva dos láminas sin pedirlo")
        assertEquals(
            sections.flatMap { it.cells },
            pages.flatMap { it.blocks }.flatMap { it.cells },
        )
        // Ningún folio se sale del papel.
        pages.forEach { page ->
            assertTrue(
                page.blocks.sumOf { it.heightMm.toDouble() }.toFloat() <= his.contentHeightMm + 0.01f,
                "un folio se sale del papel: ${page.blocks.map { it.heightMm }}",
            )
        }
    }

    /** Compartir folio ya imprimía la banda fina en todas las páginas: el #480 no lo cambia. */
    @Test
    fun `sharing a folio already printed the thin band on every page`() {
        val sections = catalogs.map(::section)

        val blocks = printPages(sections, shared).flatMap { it.blocks }

        assertTrue(
            blocks.all { it.heading == PrintHeading.Slim },
            "una lámina de un folio compartido ha cambiado de banda",
        )
        assertTrue(blocks.any { it.numberInSection > 1 }, "ninguna lámina se derrama")
    }

    /** A spilled plate's tail isn't centred: it continues its columns across pages. */
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
        assertTrue(
            kookaburra.flatMap { it.blocks }.all { it.blockWidthMm == it.grid.blockWidthMm },
            "una fila corta ha movido el bloque de una lámina que se continúa",
        )
    }

    /**
     * Warmed once for the whole notebook: page by page, a photo got one page's budget and no second
     * try. Thumbnails only, since the original is a fallback and warming both doubles the requests.
     */
    @Test
    fun `the photographs to warm are the thumbnails, each one once`() {
        val kookaburra = catalogs.first { it.id == "australian-kookaburra-perth-1oz" }
        val pages = pagesFor(section(kookaburra))

        val urls = notebookPhotographs(pages)

        // One per cell, however many pages the plate spans.
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
            // Como un tipo que la caché no tiene: la casilla conserva su hueco, sin foto.
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

    /** The index decides what stays out of the notebook, not the printer (#147, ADR 0021 §11). */
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
            emptyList(),
            Curation(catalogs),
            NotebookOptions(),
        )

        assertEquals(1, sections.size)
        assertEquals("Bandeja del abuelo", sections.single().title)
        assertEquals(emptyList(), sections.single().cells)
        assertEquals(1, printPages(sections, paper).size)
    }
}
