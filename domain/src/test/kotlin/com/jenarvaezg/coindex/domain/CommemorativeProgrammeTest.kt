package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * El segundo eje del ADR 0022: la moneda cuenta en su lista por denominación y **además** en el
 * programa conmemorativo que la emitió, sin que ninguna de las dos lecturas toque a la otra.
 */
class CommemorativeProgrammeTest {
    private fun programme(
        id: String = "portugal-1977-alexandre-herculano",
        shortName: String = "Serie Alexandre Herculano 1977",
        members: List<CommemorativeProgrammeMember> = listOf(
            CommemorativeProgrammeMember("2,50 escudos", 6_071),
            CommemorativeProgrammeMember("5 escudos", 10_126),
            CommemorativeProgrammeMember("25 escudos", 7_338),
        ),
        source: String = "https://example.org/serie-1977",
        sourceNote: String = "Tres denominaciones para el centenario, vendidas como carteira.",
    ) = CommemorativeProgramme(
        schemaVersion = 1,
        id = id,
        name = "$shortName · Portugal",
        shortName = shortName,
        issuerCode = "portugal",
        year = 1977,
        source = source,
        sourceNote = sourceNote,
        updatedAt = "2026-08-04",
        members = members,
    )

    /** The standings of one catalog, read the way the assembly builds them for all of them (#539). */
    private fun standingsOf(
        catalog: CollectionCatalog,
        items: List<CollectedItem> = emptyList(),
    ): List<ProgrammeStanding> =
        CatalogProgrammes.over(listOf(catalog), listOf(programme()), items)[catalog]

    private fun item(typeId: Int, quantity: Int = 1) = CollectedItem(
        id = typeId.toLong(),
        quantity = quantity,
        typeId = typeId,
        issueYear = 1977,
    )

    @Test
    fun `progress counts every member, including the one no catalog claims`() {
        // Exactamente el caso del padre: tiene el 2,50 y le faltan el 5 y el 25.
        val progress = programme().progress(listOf(item(6_071)))
        assertEquals(ProgrammeProgress(owned = 1, total = 3), progress)
    }

    /**
     * La razón de que un programa sea fichero y no un campo del miembro: su tercera moneda no
     * está en ningún catálogo, así que cruzar catálogos por un id habría impreso «1 de 2».
     */
    @Test
    fun `the denominator is the programme and not what the catalogs hold`() {
        val catalog = catalogOf("portugal-2-50-escudos-cuproniquel", listOf(6_071, 9_828, 9_829))
        val standings = standingsOf(catalog, listOf(item(6_071)))
        assertEquals(1, standings.size)
        assertEquals(3, standings.single().progress.total)
        assertEquals(1, standings.single().progress.owned)
    }

    @Test
    fun `a piece the collector no longer owns does not count`() {
        assertEquals(0, programme().progress(listOf(item(6_071, quantity = 0))).owned)
    }

    @Test
    fun `a catalog that shares no type with the programme gets no standing`() {
        val catalog = catalogOf("portugal-50-escudos-plata-650", listOf(4_930, 13_026))
        assertEquals(emptyList(), standingsOf(catalog))
    }

    /**
     * One walk of the files answers for every catalog at once (#539), and it answers the same thing.
     *
     * What the assembly saves is the walk of the inventory and of the programme list, never the rule:
     * the standings of a catalog that touches nothing are absent rather than empty, so the map is two
     * entries and not forty-nine — and a catalog nobody keyed reads as «ningún programa», which is
     * what a second reading beside a plate is when there is none (ADR 0022).
     */
    @Test
    fun `the standings of every catalog come out of one walk, in file order`() {
        val herculano = programme()
        val fao = programme(
            id = "portugal-1983-dia-mundial-alimentacion",
            shortName = "Serie FAO 1983",
            members = listOf(
                CommemorativeProgrammeMember("2,50 escudos", 6_071),
                CommemorativeProgrammeMember("25 escudos", 9_831),
            ),
        )
        val touching = catalogOf("portugal-2-50-escudos-cuproniquel", listOf(6_071, 9_828))
        val untouched = catalogOf("portugal-50-escudos-plata-650", listOf(4_930, 13_026))

        val standings = CatalogProgrammes.over(
            catalogs = listOf(touching, untouched),
            programmes = listOf(herculano, fao),
            items = listOf(item(6_071)),
        )

        // El orden es el de los ficheros y no el del tipo que los emparejó: los dos programas nombran
        // la misma moneda, así que agrupar por tipo los habría podido intercambiar entre dos lecturas.
        assertEquals(
            listOf("Serie Alexandre Herculano 1977", "Serie FAO 1983"),
            standings[touching].map { it.programme.shortName },
        )
        // Y el progreso sigue siendo sobre el programa: una pieza de tres y una de dos.
        assertEquals(listOf(1, 1), standings[touching].map { it.progress.owned })
        assertEquals(listOf(3, 2), standings[touching].map { it.progress.total })
        assertEquals(emptyList(), standings[untouched])
    }

    @Test
    fun `a well formed programme validates`() {
        assertNull(programme().validate())
    }

    @Test
    fun `a programme of one member is inert and refused`() {
        val single = programme(members = listOf(CommemorativeProgrammeMember("2,50", 6_071)))
        assertTrue(
            single.validate() is CommemorativeProgrammeValidationError.NotEnoughMembers,
            "${single.validate()}",
        )
    }

    @Test
    fun `a repeated type inside one programme is refused`() {
        val repeated = programme(
            members = listOf(
                CommemorativeProgrammeMember("2,50 escudos", 6_071),
                CommemorativeProgrammeMember("otra vez", 6_071),
            ),
        )
        assertTrue(
            repeated.validate() is CommemorativeProgrammeValidationError.DuplicateNumistaTypeId,
        )
    }

    @Test
    fun `the boundary needs a URL and prose, and neither may be blank`() {
        assertEquals(
            CommemorativeProgrammeValidationError.InvalidSource.message,
            programme(source = "no-es-una-url").validate()?.message,
        )
        assertTrue(
            programme(sourceNote = "  ").validate()
                is CommemorativeProgrammeValidationError.BlankField,
        )
    }

    @Test
    fun `the card sized name must be a prefix of the editorial one`() {
        val drifted = programme().copy(name = "Otra cosa entera")
        assertTrue(
            drifted.validate() is CommemorativeProgrammeValidationError.ShortNameNotPrefix,
        )
    }

    private fun catalogOf(id: String, typeIds: List<Int>) = CollectionCatalog(
        schemaVersion = 1,
        id = id,
        name = "$id · prueba",
        shortName = id,
        issuerCode = "portugal",
        family = "familia de $id",
        weightMillioz = 113,
        finish = null,
        metal = Metal.Cupronickel,
        seriesStatus = SeriesStatus.Closed,
        closedNote = "cerrado para la prueba",
        source = "https://en.numista.com/catalogue/pieces6071.html",
        updatedAt = "2026-08-04",
        members = typeIds.mapIndexed { index, typeId ->
            CollectionCatalogMember(
                id = "m$index",
                label = "miembro $index",
                year = 1977 + index,
                numistaTypeId = typeId,
            )
        },
    )
}
