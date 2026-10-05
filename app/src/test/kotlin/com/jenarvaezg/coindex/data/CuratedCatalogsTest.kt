package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogMemberStatus
import com.jenarvaezg.coindex.domain.Finish
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.SeriesStatus
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.UnclassifiedReason
import com.jenarvaezg.coindex.domain.buildCollectionCatalogAlbum
import com.jenarvaezg.coindex.domain.deriveCollection
import com.jenarvaezg.coindex.domain.validate
import com.jenarvaezg.coindex.ui.plateSubject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins what the shipped catalogs contain. Every `numista_type_id` was checked by hand against
 * numista.com, so a bad edit has to fail here and not as a wrong «me falta» on the phone.
 */
class CuratedCatalogsTest {
    private val catalogs: List<CollectionCatalog> = SHIPPED_CURATION.catalogs

    private fun find(id: String) = catalogs.first { it.id == id }

    /**
     * Una tirada abierta crece cada año, así que se fija dónde empieza y que no tenga huecos; fijar
     * el último año la pondría en rojo cada enero.
     */
    private fun assertOpenRunFrom(first: Int, catalog: CollectionCatalog) {
        val years = catalog.members.mapNotNull { it.year }.distinct()
        assertEquals(SeriesStatus.Open, catalog.seriesStatus, catalog.id)
        assertEquals(first, years.min(), "${catalog.id} no empieza donde dice")
        assertEquals((first..years.max()).toList(), years, "hueco en ${catalog.id}")
    }

    /** Los tipos de las casillas; en un date run de un solo tipo, el que se repite en todas. */
    private fun assertTypes(vararg typeIds: Int, catalog: CollectionCatalog) {
        assertEquals(typeIds.toSet(), catalog.members.mapNotNull { it.numistaTypeId }.toSet())
    }

    @Test
    fun `every shipped catalog parses and validates`() {
        assertTrue(catalogs.isNotEmpty())
        catalogs.forEach { catalog -> assertNull(catalog.validate(), "inválido: ${catalog.id}") }
    }

    /**
     * La chapa con el año es el asa que abre la ficha de la moneda (#337, #508). Un miembro
     * anunciado puede no tener ni ficha ni año.
     */
    @Test
    fun `todo miembro con ficha de Numista trae su año, que es lo que la lámina hace pulsable`() {
        val sinAño = catalogs.flatMap { catalog ->
            catalog.members
                .filter { it.numistaTypeId != null && it.year == null }
                .map { "${catalog.id}/${it.id}" }
        }
        assertEquals(emptyList(), sinAño)
    }

    /**
     * Los cerrados llevan nota (#28). Gothic Horror no es catálogo: su único miembro, N#519925, ya
     * trae `series` de Numista y la lista no añadía nada.
     */
    @Test
    fun `every shipped catalog declares whether its series is still open`() {
        val closed = catalogs.filter { it.seriesStatus == SeriesStatus.Closed }
        closed.forEach { catalog ->
            assertTrue(
                catalog.closedNote?.isNotBlank() == true,
                "cerrado sin nota: ${catalog.id}",
            )
        }
        assertTrue(catalogs.none { it.id == "gothic-horror-uk-1oz" })
        // Abiertas aunque Numista se quede en 2024-2025: el plan de emisión del Banco de Rusia
        // para 2026 trae Libro Rojo y Monumentos arquitectónicos, y la Tesla de 2026 ya salió.
        listOf("red-data-book-russia", "architectural-monuments-russia-3-roubles", "nikola-tesla-serbia-1oz")
            .forEach { assertEquals(SeriesStatus.Open, find(it).seriesStatus) }
    }

    /**
     * El programa del Banco Estatal de la URSS: dos monedas de 3 rublos de plata .900 por año entre
     * 1989 y 1991. Numista no pone las dos de 1991 en la serie 13245 ni les da `series`, así que
     * sin este catálogo salen huérfanas.
     */
    @Test
    fun `the 500th anniversary programme is six three rouble coins and ends in 1991`() {
        val programme = find("united-russian-state-500th-3-roubles")
        assertEquals(SeriesStatus.Closed, programme.seriesStatus)
        assertEquals(6, programme.members.size)
        assertEquals(
            listOf(1989, 1989, 1990, 1990, 1991, 1991),
            programme.members.map { it.year },
        )
        assertEquals(
            listOf(47_619, 48_338, 40_619, 44_092, 29_011, 35_012),
            programme.members.map { it.numistaTypeId },
        )
    }

    /**
     * La FNMT anunció diez piezas y ocho son de 10 € en plata: la colección de 2025-2026 está
     * completa, así que este es un cerrado por programa anunciado y no por silencio.
     */
    @Test
    fun `the spanish 250th anniversary collection closes at its eight silver tens`() {
        val independence = find("us-independence-250th-spain-10-euros")
        assertEquals(SeriesStatus.Closed, independence.seriesStatus)
        assertEquals(8, independence.members.size)
        assertEquals(3, independence.members.count { it.year == 2025 })
        assertEquals(5, independence.members.count { it.year == 2026 })
    }

    /**
     * Cierran por aritmética: las 50 provincias más Ceuta y Melilla, en tandas de 12, 20 y 20.
     * Numista no dice «proof» en el título de ninguna (lo dice la FNMT), así que la finish inferida
     * es `null` y sólo la que declara el catálogo (ADR 0016) lleva las piezas a esta lámina.
     */
    @Test
    fun `the 52 provincial capitals close by arithmetic and are declared proof`() {
        val capitales = find("espana-capitales-de-provincia-5-euros")
        assertEquals(SeriesStatus.Closed, capitales.seriesStatus)
        assertEquals(Finish.Proof, capitales.finish)
        assertEquals(434, capitales.weightMillioz)
        assertEquals("Capitales de provincia y ciudades autónomas", capitales.family)
        assertEquals(52, capitales.members.size)
        assertEquals(12, capitales.members.count { it.year == 2010 })
        assertEquals(20, capitales.members.count { it.year == 2011 })
        assertEquals(20, capitales.members.count { it.year == 2012 })
        // Ceuta y Melilla son las dos que no son capital de provincia.
        assertTrue(capitales.members.any { it.label == "Ceuta" })
        assertTrue(capitales.members.any { it.label == "Melilla" })
        assertEquals(45_425, capitales.members.first { it.label == "Madrid" }.numistaTypeId)
    }

    /**
     * Dos gamas de lingote de la Royal Mint cuyo límite pone la ceca. Siguen siendo catálogos
     * aunque Numista aceptara las series 10572 y 13401 (#85, #86): una serie propone agrupación y
     * no afirma cobertura (#83).
     *
     * St George tiene un tipo por año, así que es un catálogo simple; The Lion and the Eagle repite
     * N#404024 en 2024 y 2025 (el mismo reverso de Mercanti), así que es un date run.
     */
    @Test
    fun `the two royal mint bullion ranges are one slot per year`() {
        val george = find("st-george-dragon-uk-1oz-bullion")
        assertEquals(Finish.Bullion, george.finish)
        assertEquals(1_000, george.weightMillioz)
        assertOpenRunFrom(2024, george)
        assertFalse(george.isDateRun)
        assertEquals(
            george.members.size,
            george.members.mapNotNull { it.numistaTypeId }.distinct().size,
        )

        val eagle = find("lion-eagle-uk-1oz-bullion")
        assertTrue(eagle.isDateRun)
        assertEquals(Finish.Bullion, eagle.finish)
        assertEquals(1_000, eagle.weightMillioz)
        assertOpenRunFrom(2024, eagle)
        assertEquals(
            listOf(404_024, 404_024),
            eagle.members.filter { it.year in 2024..2025 }.map { it.numistaTypeId },
        )
    }

    /**
     * El British Lion volvió en 2026 con el mismo reverso de David Lawrence, así que dejó la
     * agrupación de onzas sueltas: es un date run sobre N#476689, que Numista fecha con una emisión
     * por año. Una agrupación pierde la familia ante el catálogo que nombre el tipo (ADR 0013).
     */
    @Test
    fun `the british lion stopped being a loose ounce when 2026 returned`() {
        val lion = find("uk-british-lion-1oz-bullion")
        assertTrue(lion.isDateRun)
        assertEquals(Finish.Bullion, lion.finish)
        assertEquals(1_000, lion.weightMillioz)
        assertEquals(Metal.Silver, lion.metal)
        assertOpenRunFrom(2025, lion)
        assertEquals(
            listOf(476_689, 476_689),
            lion.members.map { it.numistaTypeId },
        )
    }

    /**
     * Empieza en 2018: la Premium Uncirculated del cincuentenario de 2017 es otra emisión. Numista
     * no propone serie, así que la fuente es la página de N#143754 y cada año repite ese tipo.
     */
    @Test
    fun `the silver krugerrand annual bullion date run starts in 2018`() {
        val krugerrand = find("south-africa-silver-krugerrand-1oz-bullion")
        assertEquals(2, krugerrand.schemaVersion)
        assertTrue(krugerrand.isDateRun)
        // El alcance lo dice `name`; la `family` sólo identifica y agrupa.
        assertEquals("Silver Krugerrand bullion anual", krugerrand.family)
        assertEquals(
            "Silver Krugerrand · Sudáfrica · 1 oz bullion anual desde 2018 " +
                "(excluye 2017 Premium Uncirculated)",
            krugerrand.name,
        )
        assertEquals("afrique_du_sud", krugerrand.issuerCode)
        assertEquals(1_000, krugerrand.weightMillioz)
        assertEquals(Finish.Bullion, krugerrand.finish)
        assertEquals(Metal.Silver, krugerrand.metal)
        assertNull(krugerrand.closedNote)
        assertEquals("https://en.numista.com/catalogue/pieces143754.html", krugerrand.source)
        assertOpenRunFrom(2018, krugerrand)
        assertTrue(krugerrand.members.none { it.year == 2017 })
        assertTypes(143_754, catalog = krugerrand)
        assertTrue(krugerrand.members.all { it.numistaIssueIds.isEmpty() })
    }

    /**
     * Southern Cross de ABC Mint (Niue): un solo tipo, N#485082, sin `series` en Numista. Programa
     * bullion de tirada ilimitada; el oro (N#476767, N#476766) es otra clave de variante.
     */
    @Test
    fun `the southern cross niue silver ounce is an open date run from 2025`() {
        val southern = find("niue-southern-cross-1oz-bullion")
        assertEquals(2, southern.schemaVersion)
        assertTrue(southern.isDateRun)
        assertEquals("Southern Cross bullion anual", southern.family)
        assertEquals(
            "Southern Cross · Niue · ABC Mint · 1 oz bullion anual desde 2025",
            southern.name,
        )
        assertEquals("niue", southern.issuerCode)
        assertEquals(1_000, southern.weightMillioz)
        assertEquals(Finish.Bullion, southern.finish)
        assertEquals(Metal.Silver, southern.metal)
        assertNull(southern.closedNote)
        assertEquals("https://en.numista.com/catalogue/pieces485082.html", southern.source)
        assertOpenRunFrom(2025, southern)
        assertTypes(485_082, catalog = southern)
        assertTrue(southern.members.all { it.numistaIssueIds.isEmpty() })
    }

    /**
     * Programas anuales que ya tenían serie en Numista y por tanto tarjeta sin denominador (#57,
     * #95). Una casilla por año sin cualificar emisiones: la ficha no mezcla acabados, y las filas
     * repetidas de un año son variedades de tirada del mismo bullion. Del Arca sólo se catalogan
     * las fracciones que hay en las colecciones.
     */
    @Test
    fun `vienna philharmonic and noahs ark are open single-type bullion date runs`() {
        val philharmonic = find("austria-vienna-philharmonic-1oz-bullion")
        assertEquals(2, philharmonic.schemaVersion)
        assertTrue(philharmonic.isDateRun)
        assertEquals("Vienna Philharmonic bullion anual", philharmonic.family)
        assertEquals(
            "Vienna Philharmonic · Austria · 1 oz bullion anual desde 2008",
            philharmonic.name,
        )
        assertEquals("autriche", philharmonic.issuerCode)
        assertEquals(1_000, philharmonic.weightMillioz)
        assertEquals(Finish.Bullion, philharmonic.finish)
        assertEquals(Metal.Silver, philharmonic.metal)
        assertNull(philharmonic.closedNote)
        assertEquals("https://en.numista.com/catalogue/pieces9165.html", philharmonic.source)
        assertOpenRunFrom(2008, philharmonic)
        assertTypes(9_165, catalog = philharmonic)
        assertTrue(philharmonic.members.all { it.numistaIssueIds.isEmpty() })

        val arks = listOf(
            Triple(
                "armenia-noahs-ark-1oz-bullion",
                1_000 to 26_279,
                "Noah's Ark 1 oz · Armenia · bullion anual desde 2011",
            ),
            Triple(
                "armenia-noahs-ark-half-oz-bullion",
                500 to 31_963,
                "Noah's Ark ½ oz · Armenia · bullion anual desde 2011",
            ),
            Triple(
                "armenia-noahs-ark-quarter-oz-bullion",
                250 to 31_623,
                "Noah's Ark ¼ oz · Armenia · bullion anual desde 2011",
            ),
        )
        for ((id, weightAndType, name) in arks) {
            val (weight, typeId) = weightAndType
            val catalog = find(id)
            assertEquals(2, catalog.schemaVersion)
            assertTrue(catalog.isDateRun)
            assertEquals(name, catalog.name)
            assertEquals("armenie", catalog.issuerCode)
            assertEquals(weight, catalog.weightMillioz)
            assertEquals(Finish.Bullion, catalog.finish)
            assertEquals(Metal.Silver, catalog.metal)
            assertNull(catalog.closedNote)
            assertEquals("https://en.numista.com/catalogue/pieces$typeId.html", catalog.source)
            assertOpenRunFrom(2011, catalog)
            assertTypes(typeId, catalog = catalog)
            assertTrue(catalog.members.all { it.numistaIssueIds.isEmpty() })
        }
        assertEquals(
            setOf(
                "Noah's Ark 1 oz bullion anual",
                "Noah's Ark ½ oz bullion anual",
                "Noah's Ark ¼ oz bullion anual",
            ),
            arks.map { find(it.first).family }.toSet(),
        )
    }

    /**
     * Onza Libertad bullion, one open date run from the 1982 decree that replaced the Onza Troy.
     * Its three types (N#14465 at 36 mm until 1995, N#17818 and N#13855 at 40 mm) mix proof and
     * reverse/antiqued, so every slot is qualified by its bullion emission; die varieties of one
     * finish share the year. Emission ids read from the type pages (#92).
     */
    @Test
    fun `the mexican libertad is an issue-qualified bullion run over three types`() {
        val libertad = find("mexico-libertad-1oz-bullion")
        assertEquals(2, libertad.schemaVersion)
        assertTrue(libertad.isDateRun)
        assertEquals("Onza Libertad bullion anual", libertad.family)
        assertEquals("mexique", libertad.issuerCode)
        assertEquals(1_000, libertad.weightMillioz)
        assertEquals(Finish.Bullion, libertad.finish)
        assertEquals(Metal.Silver, libertad.metal)
        assertNull(libertad.closedNote)
        assertEquals("https://en.numista.com/catalogue/pieces14465.html", libertad.source)
        assertOpenRunFrom(1982, libertad)
        // Las tres fichas, en el orden en que se relevan: el 36 mm hasta 1995 y los dos de 40 mm.
        assertTypes(14_465, 17_818, 13_855, catalog = libertad)
        assertEquals(
            listOf(14_465, 17_818, 13_855),
            libertad.members.mapNotNull { it.numistaTypeId }.distinct(),
        )
        assertTrue(libertad.members.all { it.numistaIssueIds.isNotEmpty() })
        // The 2024 bullion fills; the proof of that year must not.
        assertTrue(898_129 in libertad.members.single { it.year == 2024 }.numistaIssueIds)
        assertTrue(libertad.members.none { 929_350 in it.numistaIssueIds })
        val bullion2024 = CollectedItem(
            id = 1,
            quantity = 1,
            typeId = 13_855,
            issueYear = 2024,
            issueId = 898_129,
        )
        val proof2024 = CollectedItem(
            id = 2,
            quantity = 1,
            typeId = 13_855,
            issueYear = 2024,
            issueId = 929_350,
        )
        assertTrue(libertad.memberMatches(libertad.members.single { it.year == 2024 }, bullion2024))
        assertTrue(libertad.members.none { libertad.memberMatches(it, proof2024) })
    }

    /**
     * The antiqued high relief Nautical Ounce is a second plate over five of the bullion plate's
     * types, the case ADR 0019 exists for: Numista keeps the standard, proof and antiqued rows in
     * one type, so without emissions an antiqued piece would fill the bullion slot of its year.
     *
     * The variant closes in 2024 while the series stays open, like the .500 Canadian dollar (#52):
     * gold.de gives the antiqued 1 oz as struck 2020-2024 and not before, and AgAuNEWS lists the
     * 2025 and 2026 ranges without it. Its 2020 row is emission 1116210 of N#220874, split out by
     * a referee; until then the slot had no type, and a member without one never matches (#596).
     * Emission ids from `/types/{id}/issues`.
     */
    @Test
    fun `the antiqued nautical ounce is five closed years sharing every type with the bullion`() {
        val antiqued = find("rwanda-nautical-50-francs-antiqued")
        assertEquals(1, antiqued.schemaVersion)
        assertFalse(antiqued.isDateRun)
        assertEquals("Nautical Ounce", antiqued.family)
        assertEquals("Nautical Ounce antique de Ruanda", antiqued.shortName)
        assertEquals("rwanda", antiqued.issuerCode)
        assertEquals(1_000, antiqued.weightMillioz)
        assertEquals(Finish.Antiqued, antiqued.finish)
        assertEquals(Metal.Silver, antiqued.metal)
        assertEquals(SeriesStatus.Closed, antiqued.seriesStatus)
        assertNotNull(antiqued.closedNote)
        assertTrue(antiqued.closedNote!!.contains("2020 a 2024"))
        assertEquals((2020..2024).toList(), antiqued.members.map { it.year })

        // Every slot names its type and qualifies one emission.
        val issued = antiqued.members.filter { it.isIssued }
        assertEquals(antiqued.members, issued)
        assertEquals(
            listOf(220_874, 301_537, 362_115, 415_674, 442_604),
            issued.map { it.numistaTypeId },
        )
        assertEquals(
            listOf(
                listOf(1_116_210),
                listOf(796_570),
                listOf(796_577),
                listOf(864_080),
                listOf(908_445),
            ),
            issued.map { it.numistaIssueIds },
        )

        // Both plates share these types, so both sides are qualified and disjoint: an antiqued
        // 2022 fills the antiqued slot and leaves the bullion one open.
        val bullion = find("rwanda-nautical-50-francs")
        assertTrue(bullion.members.all { it.numistaIssueIds.isNotEmpty() })
        assertEquals(
            emptySet(),
            bullion.members.flatMap { it.numistaIssueIds }
                .intersect(antiqued.members.flatMap { it.numistaIssueIds }.toSet()),
        )
        val antiqued2022 = CollectedItem(
            id = 1,
            quantity = 1,
            typeId = 362_115,
            issueYear = 2022,
            issueId = 796_577,
        )
        assertTrue(antiqued.memberMatches(antiqued.members.single { it.year == 2022 }, antiqued2022))
        assertTrue(bullion.members.none { bullion.memberMatches(it, antiqued2022) })
        // The proof of that same type fills neither plate.
        val proof2022 = CollectedItem(
            id = 2,
            quantity = 1,
            typeId = 362_115,
            issueYear = 2022,
            issueId = 796_578,
        )
        assertTrue(antiqued.members.none { antiqued.memberMatches(it, proof2022) })
        assertTrue(bullion.members.none { bullion.memberMatches(it, proof2022) })
        // The 2020 antiqued Mayflower, from the year Numista used to keep in a single row.
        val antiqued2020 = CollectedItem(
            id = 3,
            quantity = 1,
            typeId = 220_874,
            issueYear = 2020,
            issueId = 1_116_210,
        )
        assertTrue(antiqued.memberMatches(antiqued.members.single { it.year == 2020 }, antiqued2020))
        assertTrue(bullion.members.none { bullion.memberMatches(it, antiqued2020) })
    }

    /**
     * Onza Troy .925, the closed run the Libertad replaced. At 33.625 g / 41.5 mm (1081 millioz)
     * it never shares a key with the Libertad. N#13333 is 1949 and N#13398 covers 1978-1980.
     * Closed by Banxico's stated succession and the DOF decree of 21/28 December 1981 (#92).
     */
    @Test
    fun `the mexican onza troy is four closed years at 1081 millioz`() {
        val troy = find("mexico-onza-troy-925")
        assertEquals(2, troy.schemaVersion)
        assertTrue(troy.isDateRun)
        assertEquals("Onza Troy de México", troy.family)
        assertEquals("mexique", troy.issuerCode)
        assertEquals(1_081, troy.weightMillioz)
        assertNull(troy.finish)
        assertEquals(Metal.Silver, troy.metal)
        assertEquals(SeriesStatus.Closed, troy.seriesStatus)
        assertNotNull(troy.closedNote)
        assertTrue(troy.closedNote!!.contains("Banxico"))
        assertTrue(troy.closedNote!!.contains("33.625"))
        assertEquals("https://en.numista.com/catalogue/pieces13333.html", troy.source)
        assertEquals(listOf(1949, 1978, 1979, 1980), troy.members.map { it.year })
        assertEquals(listOf(13_333, 13_398, 13_398, 13_398), troy.members.map { it.numistaTypeId })
        assertTrue(troy.members.all { it.numistaIssueIds.isEmpty() })
        val owned1979 = CollectedItem(
            id = 1,
            quantity = 1,
            typeId = 13_398,
            issueYear = 1979,
            issueId = 69_445,
        )
        assertTrue(troy.memberMatches(troy.members.single { it.year == 1979 }, owned1979))
    }

    /**
     * American Silver Eagle bullion, one date run over N#1493 (Type 1, 1986-2021) and N#298883
     * (Type 2, from 2021). 2021 keeps two slots because the reverse changed mid-year and the
     * market names them apart (#57). Both types mix proof and burnished, so every slot is
     * issue-qualified; Star Privy 2024 and Eagle Privy 2025 stay out as thematic privies (#91).
     *
     * Dos casillas aceptan además otro acabado, decidido caso a caso (#216). La de 2023 acepta la
     * proof 760576, la de la colección: el eagle no tiene lámina proof hermana, así que la pieza no
     * marca dos casillas (ADR 0019). La de 2021 Type 2 acepta la burnished 675331 de forma
     * provisional: sí cabría una lámina de burnished, pero el dominio no tiene ese acabado e
     * `inferFinish` sólo lee el título, «Bullion Coin» en las dos fichas. La nota de la casilla
     * dice cómo deshacerlo, y este test vigila que no se borre.
     */
    @Test
    fun `the american silver eagle is an issue-qualified bullion run over two types`() {
        val eagle = find("us-american-silver-eagle-1oz-bullion")
        assertEquals(2, eagle.schemaVersion)
        assertTrue(eagle.isDateRun)
        assertEquals("American Silver Eagle bullion anual", eagle.family)
        assertEquals("etats-unis", eagle.issuerCode)
        assertEquals(1_000, eagle.weightMillioz)
        assertEquals(Finish.Bullion, eagle.finish)
        assertEquals(Metal.Silver, eagle.metal)
        assertNull(eagle.closedNote)
        assertEquals("https://en.numista.com/catalogue/pieces1493.html", eagle.source)
        assertOpenRunFrom(1986, eagle)
        val type1 = eagle.members.filter { it.numistaTypeId == 1_493 }
        val type2 = eagle.members.filter { it.numistaTypeId == 298_883 }
        // El relevo es en 2021 y ese año tiene dos casillas, una de cada tipo: es el único solape.
        assertTypes(1_493, 298_883, catalog = eagle)
        assertEquals((1986..2021).toList(), type1.map { it.year })
        assertEquals(2021, type2.first().year)
        assertEquals("Type 1", type1.single { it.year == 2021 }.label)
        assertEquals("Type 2", type2.single { it.year == 2021 }.label)
        assertTrue(eagle.members.all { it.numistaIssueIds.isNotEmpty() })
        // Standard bullion rows, and the widened 2023 proof.
        assertTrue(64_283 in type1.single { it.year == 1987 }.numistaIssueIds)
        assertTrue(1_059_386 in type2.single { it.year == 2026 }.numistaIssueIds)
        assertTrue(760_576 in type2.single { it.year == 2023 }.numistaIssueIds)
        // La burnished 2021-W, con la deuda escrita en la nota de la casilla.
        val slot2021 = type2.single { it.year == 2021 }
        assertTrue(675_331 in slot2021.numistaIssueIds)
        assertTrue(
            slot2021.variantNote?.contains("provisional") == true,
            "la ampliación de 2021 sin nota que diga cómo se deshace",
        )
        // La otra fila «Proof» de 2023 no está: nadie la tiene y un id no se escribe por simetría.
        assertTrue(eagle.members.none { 760_578 in it.numistaIssueIds })
        assertTrue(eagle.members.none { 897_759 in it.numistaIssueIds })
        assertTrue(eagle.members.none { 948_319 in it.numistaIssueIds })

        val proof2023 = CollectedItem(
            id = 1,
            quantity = 1,
            typeId = 298_883,
            issueYear = 2023,
            issueId = 760_576,
        )
        val bullion2026 = CollectedItem(
            id = 2,
            quantity = 1,
            typeId = 298_883,
            issueYear = 2026,
            issueId = 1_059_386,
        )
        assertTrue(eagle.memberMatches(type2.single { it.year == 2023 }, proof2023))
        assertTrue(eagle.memberMatches(type2.single { it.year == 2026 }, bullion2026))
        val burnished2021 = CollectedItem(
            id = 3,
            quantity = 1,
            typeId = 298_883,
            issueYear = 2021,
            issueId = 675_331,
        )
        assertTrue(eagle.memberMatches(slot2021, burnished2021))
        // Las dos ampliaciones llevan nota, y sólo la de 2021 es provisional.
        assertEquals(
            listOf("2021-type-2", "2023"),
            eagle.members.filter { it.variantNote != null }.map { it.id },
        )
    }

    /**
     * Silver Maple Leaf bullion (#96), under the Numista series «SML». Six types from 1988
     * (N#18655, N#6735, N#381278, N#58596, N#356135, N#401696) mix proof, reverse-proof privies,
     * incuse, gilded and specimen, so every slot is issue-qualified. A bullion privy on the same
     * type fills its year and is never a slot of its own; 2000 has no plain bullion row, so
     * Firework, Dragon and Expo fill it. Ids from `/types/{id}/issues`.
     */
    @Test
    fun `the silver maple leaf is an issue-qualified bullion run over six types`() {
        val maple = find("canada-silver-maple-leaf-1oz-bullion")
        assertEquals(2, maple.schemaVersion)
        assertTrue(maple.isDateRun)
        assertEquals("Silver Maple Leaf bullion anual", maple.family)
        assertEquals(
            "Silver Maple Leaf · Canadá · 1 oz bullion anual desde 1988 " +
                "(sin proof, reverse proof, incuse, doradas ni aniversario; " +
                "el privy del mismo tipo rellena el año)",
            maple.name,
        )
        assertEquals("canada", maple.issuerCode)
        assertEquals(1_000, maple.weightMillioz)
        assertEquals(Finish.Bullion, maple.finish)
        assertEquals(Metal.Silver, maple.metal)
        assertNull(maple.closedNote)
        assertEquals("https://en.numista.com/catalogue/pieces18655.html", maple.source)
        assertOpenRunFrom(1988, maple)
        // Las seis fichas se relevan en orden y ninguna vuelve después de que otra la sustituya.
        assertEquals(
            listOf(18_655, 6_735, 381_278, 58_596, 356_135, 401_696),
            maple.members.mapNotNull { it.numistaTypeId }.distinct(),
        )
        assertTrue(maple.members.all { it.numistaIssueIds.isNotEmpty() })
        // Bullion rows fill; the 1989 proof and the 2018 incuse must not.
        assertTrue(814_014 in maple.members.single { it.year == 2007 }.numistaIssueIds)
        assertTrue(228_562 in maple.members.single { it.year == 2014 }.numistaIssueIds)
        assertTrue(maple.members.none { 118_027 in it.numistaIssueIds }) // 1989 proof
        assertTrue(maple.members.none { 394_320 in it.numistaIssueIds }) // 2018 incuse
        assertTrue(maple.members.none { 247_767 in it.numistaIssueIds }) // 2001 reverse-proof privy
        assertTrue(maple.members.none { 726_344 in it.numistaIssueIds }) // 2019 gilded
        assertTrue(maple.members.none { it.numistaTypeId == 138_478 }) // 2018 anniversary type
        // 2000 has no plain row: Firework 2000 single-date fills the year.
        assertTrue(814_168 in maple.members.single { it.year == 2000 }.numistaIssueIds)
        // Bullion privy on the same type fills the year (Tiger 1998).
        assertTrue(247_759 in maple.members.single { it.year == 1998 }.numistaIssueIds)

        val bullion2007 = CollectedItem(
            id = 1,
            quantity = 1,
            typeId = 381_278,
            issueYear = 2007,
            issueId = 814_014,
        )
        val proof1989 = CollectedItem(
            id = 2,
            quantity = 1,
            typeId = 18_655,
            issueYear = 1989,
            issueId = 118_027,
        )
        val incuse2018 = CollectedItem(
            id = 3,
            quantity = 1,
            typeId = 58_596,
            issueYear = 2018,
            issueId = 394_320,
        )
        assertTrue(maple.memberMatches(maple.members.single { it.year == 2007 }, bullion2007))
        assertTrue(maple.members.none { maple.memberMatches(it, proof1989) })
        assertTrue(maple.members.none { maple.memberMatches(it, incuse2018) })
    }

    /**
     * Australian Kangaroo 1 oz .9999 (#98): Perth Mint bullion from the 2016 inaugural year.
     * Four types cover 2016-2026 (N#76663 / N#153925 / N#359552 / N#404064). N#76663 mixes
     * «Proof in 4 Coin Set» on 2016-2017, so those years are issue-qualified; the clean later
     * types are not. 2019 splits like the Eagle of 2021 — 4th and 6th portrait are two slots.
     * Outside: 2015 forerunner .999 (N#105293), gilded, coloured, high relief and Charles proof.
     */
    @Test
    fun `the australian kangaroo is a bullion run over four types with the 2019 split`() {
        val kangaroo = find("australia-silver-kangaroo-1oz-bullion")
        assertEquals(2, kangaroo.schemaVersion)
        assertTrue(kangaroo.isDateRun)
        assertEquals("Australian Kangaroo bullion anual", kangaroo.family)
        assertEquals(
            "Australian Kangaroo · Australia · 1 oz bullion anual desde 2016 " +
                "(sin proof, doradas, coloreadas ni high relief; 2019 partido 4.º/6.º retrato)",
            kangaroo.name,
        )
        assertEquals("australie", kangaroo.issuerCode)
        assertEquals(1_000, kangaroo.weightMillioz)
        assertEquals(Finish.Bullion, kangaroo.finish)
        assertEquals(Metal.Silver, kangaroo.metal)
        assertNull(kangaroo.closedNote)
        assertEquals("https://en.numista.com/catalogue/series.php?id=1550", kangaroo.source)
        assertOpenRunFrom(2016, kangaroo)
        // Las cuatro fichas se relevan en orden, y 2019 es el único año con dos casillas.
        assertEquals(
            listOf(76_663, 153_925, 359_552, 404_064),
            kangaroo.members.mapNotNull { it.numistaTypeId }.distinct(),
        )
        assertEquals(
            listOf(2019),
            kangaroo.members.groupBy { it.year }.filterValues { it.size > 1 }.keys.toList(),
        )
        assertEquals(
            listOf("4th Portrait", "6th Portrait"),
            kangaroo.members.filter { it.year == 2019 }.map { it.label },
        )
        // N#76663 mixes proof-in-set; later types are clean bullion rows.
        assertTrue(kangaroo.members.filter { it.numistaTypeId == 76_663 }.all { it.numistaIssueIds.isNotEmpty() })
        assertTrue(kangaroo.members.filter { it.numistaTypeId != 76_663 }.all { it.numistaIssueIds.isEmpty() })
        assertEquals(listOf(267_041), kangaroo.members.single { it.year == 2016 }.numistaIssueIds)
        assertEquals(listOf(325_758), kangaroo.members.single { it.year == 2017 }.numistaIssueIds)
        assertEquals(listOf(368_364), kangaroo.members.single { it.year == 2018 }.numistaIssueIds)
        assertEquals(
            listOf(488_646),
            kangaroo.members.single { it.year == 2019 && it.numistaTypeId == 76_663 }.numistaIssueIds,
        )
        assertTrue(kangaroo.members.none { it.numistaTypeId == 105_293 }) // 2015 .999 forerunner
        assertTrue(kangaroo.members.none { it.numistaTypeId == 168_801 }) // gilded
        assertTrue(kangaroo.members.none { it.numistaTypeId == 393_476 }) // Charles proof

        val bullion2016 = CollectedItem(
            id = 1,
            quantity = 1,
            typeId = 76_663,
            issueYear = 2016,
            issueId = 267_041,
        )
        val proofInSet2016 = CollectedItem(
            id = 2,
            quantity = 1,
            typeId = 76_663,
            issueYear = 2016,
            issueId = 1_012_796,
        )
        val portrait4th2019 = CollectedItem(
            id = 3,
            quantity = 1,
            typeId = 76_663,
            issueYear = 2019,
            issueId = 488_646,
        )
        val portrait6th2019 = CollectedItem(
            id = 4,
            quantity = 1,
            typeId = 153_925,
            issueYear = 2019,
            issueId = null,
        )
        assertTrue(kangaroo.memberMatches(kangaroo.members.single { it.year == 2016 }, bullion2016))
        assertTrue(kangaroo.members.none { kangaroo.memberMatches(it, proofInSet2016) })
        assertTrue(
            kangaroo.memberMatches(
                kangaroo.members.single { it.year == 2019 && it.numistaTypeId == 76_663 },
                portrait4th2019,
            ),
        )
        assertTrue(
            kangaroo.memberMatches(
                kangaroo.members.single { it.year == 2019 && it.numistaTypeId == 153_925 },
                portrait6th2019,
            ),
        )
    }

    /**
     * Silver Britannia (#97): the 2013 fineness change splits the 1 oz line like the Canadian
     * dollar of #52 — .958 at 32,45 g closes in 2012; .999 at 31,21 g opens from 2013. The ¼ oz
     * never had .958 bullion (only proof), so it is one open catalog with gaps where Numista
     * only lists proof (2016-2020 and 2022). Privies and city marks on the same type fill the
     * year; plain-field BU, Oriental Border, Coronation and Gairsoppa stay out. 2023 has two
     * slots (Elizabeth / Charles), same rule as the Eagle Type 1 / Type 2 of #91.
     */
    @Test
    fun `silver britannia splits on the 2013 fineness change and catalogs the quarter separately`() {
        val closed958 = find("uk-silver-britannia-1oz-958")
        assertEquals(2, closed958.schemaVersion)
        assertTrue(closed958.isDateRun)
        assertEquals("Silver Britannia .958 1 oz", closed958.family)
        assertEquals(1_043, closed958.weightMillioz)
        assertEquals(Finish.Bullion, closed958.finish)
        assertEquals(Metal.Silver, closed958.metal)
        assertEquals(SeriesStatus.Closed, closed958.seriesStatus)
        assertNotNull(closed958.closedNote)
        assertTrue(closed958.closedNote!!.contains(".999"))
        assertEquals("https://en.numista.com/catalogue/pieces13410.html", closed958.source)
        assertEquals((1998..2012).toList(), closed958.members.map { it.year })
        assertTrue(closed958.members.all { it.numistaIssueIds.isNotEmpty() })
        assertTrue(closed958.members.none { it.year == 1997 })
        assertTrue(closed958.members.none { 115_756 in it.numistaIssueIds }) // 1998 proof
        assertTrue(closed958.members.none { 677_334 in it.numistaIssueIds }) // 2011 Matt BU

        val open999 = find("uk-silver-britannia-1oz-bullion")
        assertEquals(2, open999.schemaVersion)
        assertTrue(open999.isDateRun)
        assertEquals("Silver Britannia .999 bullion anual", open999.family)
        assertEquals(1_000, open999.weightMillioz)
        assertEquals(Finish.Bullion, open999.finish)
        assertEquals(SeriesStatus.Open, open999.seriesStatus)
        assertNull(open999.closedNote)
        assertEquals("https://en.numista.com/catalogue/pieces295025.html", open999.source)
        assertOpenRunFrom(2013, open999)
        // 2023 es el único año con dos casillas, Isabel y Carlos, como el Eagle de 2021.
        assertEquals(
            listOf(2023),
            open999.members.groupBy { it.year }.filterValues { it.size > 1 }.keys.toList(),
        )
        assertTrue(open999.members.all { it.numistaIssueIds.isNotEmpty() })
        // Textured bullion is the 2015/2016 standard; plain-field BU must not fill.
        assertTrue(555_707 in open999.members.single { it.year == 2015 }.numistaIssueIds)
        assertTrue(open999.members.none { 254_785 in it.numistaIssueIds })
        assertTrue(open999.members.none { 552_302 in it.numistaIssueIds })
        // Lunar / city privies on the same type fill; Coronation and Oriental Border stay out.
        assertTrue(667_940 in open999.members.single { it.year == 2013 }.numistaIssueIds)
        assertTrue(open999.members.none { it.numistaTypeId == 370_264 })
        assertTrue(open999.members.none { it.numistaTypeId == 134_668 })
        val elizabeth2023 = open999.members.single { it.id == "2023-elizabeth-silver-britannia" }
        val charles2023 = open999.members.single { it.id == "2023-charles-silver-britannia" }
        assertEquals(240_613, elizabeth2023.numistaTypeId)
        assertEquals(353_110, charles2023.numistaTypeId)

        val bullion2015 = CollectedItem(
            id = 1,
            quantity = 1,
            typeId = 224_428,
            issueYear = 2015,
            issueId = 555_707,
        )
        val plainBu2015 = CollectedItem(
            id = 2,
            quantity = 1,
            typeId = 59_887,
            issueYear = 2015,
            issueId = 254_785,
        )
        assertTrue(open999.memberMatches(open999.members.single { it.year == 2015 }, bullion2015))
        assertTrue(open999.members.none { open999.memberMatches(it, plainBu2015) })

        val quarter = find("uk-silver-britannia-quarter-oz-bullion")
        assertEquals(2, quarter.schemaVersion)
        assertTrue(quarter.isDateRun)
        assertEquals("Silver Britannia ¼ oz bullion", quarter.family)
        assertEquals(250, quarter.weightMillioz)
        assertEquals(Finish.Bullion, quarter.finish)
        assertEquals(SeriesStatus.Open, quarter.seriesStatus)
        assertNull(quarter.closedNote)
        assertEquals("https://en.numista.com/catalogue/pieces384610.html", quarter.source)
        // Ésta sí tiene huecos, y son la afirmación: los años en que Numista sólo lista proof.
        assertEquals(2013, quarter.members.mapNotNull { it.year }.min())
        assertTrue(quarter.members.all { it.numistaIssueIds.isNotEmpty() })
        assertTrue(quarter.members.none { 726_963 in it.numistaIssueIds }) // Gairsoppa
        assertTrue(quarter.members.none { it.year in 2016..2020 })
        assertTrue(quarter.members.none { it.year == 2022 })
        assertEquals(
            274_495,
            quarter.members.single { it.id == "2023-elizabeth-silver-britannia-quarter" }.numistaTypeId,
        )
        assertEquals(
            384_610,
            quarter.members.single { it.id == "2023-charles-silver-britannia-quarter" }.numistaTypeId,
        )
    }

    /**
     * The stars of the 100 pesetas are keyed on issues because the year cannot tell them apart:
     * the six issues of N#1885 are all dated 1966 and the star is a variety of the issue. Ids from
     * `/types/1885/issues`, whose comment on each issue names its star.
     */
    @Test
    fun `the paquillos are five stars over six numista issues`() {
        val paquillos = find("espana-paquillos")
        assertEquals(5, paquillos.schemaVersion)
        assertTrue(paquillos.isIssueRun)
        assertEquals("100 Pesetas de Franco", paquillos.family)
        assertEquals(611, paquillos.weightMillioz)
        assertEquals(
            listOf("Estrella 66", "Estrella 67", "Estrella 68", "Estrella 69", "Estrella 70"),
            paquillos.members.map { it.label },
        )
        assertTrue(paquillos.members.all { it.numistaTypeId == 1_885 && it.year == 1966 })
        // El 69 son dos emisiones —nueve curvo y nueve recto— en una sola casilla.
        assertEquals(
            listOf(listOf(8_508), listOf(33_204), listOf(33_205), listOf(33_206, 368_163), listOf(33_207)),
            paquillos.members.map { it.numistaIssueIds },
        )
    }

    /**
     * Three Numista types make one date run because all three weigh 10 g. N#10399 is dated 1945
     * and struck in 1947: the type's `min_year`/`max_year` say 1947, but the issue's `year` is 1945
     * and `recordedYear` prefers the date on the coin, so a 1947 member could never be filled.
     */
    @Test
    fun `the venezuelan 2 bolivares date run spans its three types`() {
        val bolivares = find("venezuela-2-bolivares")
        assertEquals(2, bolivares.schemaVersion)
        assertTrue(bolivares.isDateRun)
        assertEquals("2 Bolívares de Venezuela", bolivares.family)
        assertEquals(322, bolivares.weightMillioz)
        assertNull(bolivares.finish)
        assertEquals(25, bolivares.members.size)
        // Verificados en numista.com/catalogue/pieces10339.html: 22 años entre 1879 y 1936.
        assertEquals(
            listOf(
                1879, 1886, 1887, 1888, 1889, 1894, 1900, 1902, 1903, 1904, 1905,
                1911, 1912, 1913, 1919, 1922, 1924, 1926, 1929, 1930, 1935, 1936,
            ),
            bolivares.members.filter { it.numistaTypeId == 10_339 }.map { it.year },
        )
        assertEquals(
            listOf(1945),
            bolivares.members.filter { it.numistaTypeId == 10_399 }.map { it.year },
        )
        assertEquals(
            "1945 (acuñada en 1947)",
            bolivares.members.first { it.numistaTypeId == 10_399 }.label,
        )
        // Ninguna casilla se indexa por el año de acuñación: la llave es la fecha de la moneda.
        assertTrue(bolivares.members.none { it.year == 1947 })
        // Y un date run no nombra emisiones; eso es cosa de un issue run.
        assertTrue(bolivares.members.all { it.numistaIssueIds.isEmpty() })
        assertEquals(
            listOf(1960, 1965),
            bolivares.members.filter { it.numistaTypeId == 7_775 }.map { it.year },
        )
    }

    /**
     * El 1 bolívar pasa de agrupación a date run (#113) con el tronco N#10338, que la agrupación no
     * nombraba, más N#10398, N#7034 y N#5316. Los años no se copian del 2 bolívares: aquí hay 1893,
     * 1901 y 1921, y no hay 1894 ni 1930.
     */
    @Test
    fun `the venezuelan 1 bolivar date run spans its four types including the trunk`() {
        val bolivar = find("venezuela-1-bolivar")
        assertEquals(2, bolivar.schemaVersion)
        assertTrue(bolivar.isDateRun)
        assertEquals("1 Bolívar de Venezuela", bolivar.family)
        assertEquals(161, bolivar.weightMillioz)
        assertEquals(Metal.Silver, bolivar.metal)
        assertNull(bolivar.finish)
        assertEquals(SeriesStatus.Closed, bolivar.seriesStatus)
        assertEquals(22, bolivar.members.size)
        assertEquals(
            listOf(
                1879, 1886, 1887, 1888, 1889, 1893, 1900, 1901, 1903,
                1911, 1912, 1919, 1921, 1924, 1926, 1929, 1935, 1936,
            ),
            bolivar.members.filter { it.numistaTypeId == 10_338 }.map { it.year },
        )
        assertEquals(
            listOf(1945),
            bolivar.members.filter { it.numistaTypeId == 10_398 }.map { it.year },
        )
        assertEquals(
            "1945 (acuñada en 1947)",
            bolivar.members.first { it.numistaTypeId == 10_398 }.label,
        )
        assertEquals(
            listOf(1954),
            bolivar.members.filter { it.numistaTypeId == 7_034 }.map { it.year },
        )
        assertEquals(
            "1954 (acuñada en 1955)",
            bolivar.members.first { it.numistaTypeId == 7_034 }.label,
        )
        assertTrue(bolivar.members.none { it.year == 1947 || it.year == 1955 })
        assertTrue(bolivar.members.all { it.numistaIssueIds.isEmpty() })
        assertEquals(
            listOf(1960, 1965),
            bolivar.members.filter { it.numistaTypeId == 5_316 }.map { it.year },
        )
    }

    /**
     * Los reales (½ bolívar y 50 céntimos, 2,5 g de plata .835), con la forma del 1 bolívar (#113):
     * date run cerrado, una casilla por año, y la etiqueta nombra el año de acuñación si Numista
     * lo apunta. No hay 1926, aunque sí en el 1 bolívar y en los fuertes. La familia es «Reales de
     * Venezuela» y no «½ Bolívar» porque en Venezuela «medio» es el ¼ (#114).
     */
    @Test
    fun `the venezuelan reales date run spans its four types including the trunk`() {
        val reales = find("venezuela-reales")
        assertEquals(2, reales.schemaVersion)
        assertTrue(reales.isDateRun)
        assertEquals("Reales de Venezuela", reales.family)
        assertEquals(80, reales.weightMillioz)
        assertEquals(Metal.Silver, reales.metal)
        assertNull(reales.finish)
        assertEquals(SeriesStatus.Closed, reales.seriesStatus)
        assertEquals(22, reales.members.size)
        assertEquals(
            listOf(
                1879, 1886, 1887, 1888, 1889, 1893, 1900, 1901, 1903,
                1911, 1912, 1919, 1921, 1924, 1929, 1935, 1936,
            ),
            reales.members.filter { it.numistaTypeId == 17_945 }.map { it.year },
        )
        assertEquals(
            listOf(1944, 1945, 1946),
            reales.members.filter { it.numistaTypeId == 7_727 }.map { it.year },
        )
        assertEquals(
            "1944 (acuñada en 1945)",
            reales.members.first { it.numistaTypeId == 7_727 }.label,
        )
        assertEquals(
            listOf(1954),
            reales.members.filter { it.numistaTypeId == 2_971 }.map { it.year },
        )
        assertEquals(
            "1954 (acuñada en 1955)",
            reales.members.first { it.numistaTypeId == 2_971 }.label,
        )
        assertEquals(
            listOf(1960),
            reales.members.filter { it.numistaTypeId == 7_297 }.map { it.year },
        )
        assertTrue(reales.members.none { it.year == 1926 })
        // Los años de acuñación no abren casilla: viven en la etiqueta.
        assertTrue(reales.members.none { it.year in listOf(1947, 1955) })
        assertTrue(reales.members.all { it.numistaIssueIds.isEmpty() })
    }

    /**
     * Los medios: ¼ bolívar o 25 céntimos, 1,25 g de plata .835, con el 1954 (N#5317, acuñado en
     * 1955) como en el 1 bolívar. El ⅕ de 1879 (N#59789, 1 g) queda fuera por clave de variante y
     * por el mínimo de dos de #33. «Medios de Venezuela» hace pareja con «Reales de Venezuela»: en
     * la calle «medio» es el cuarto (#114, #115).
     */
    @Test
    fun `the venezuelan medios date run spans its three types including 1954`() {
        val medios = find("venezuela-medios")
        assertEquals(2, medios.schemaVersion)
        assertTrue(medios.isDateRun)
        assertEquals("Medios de Venezuela", medios.family)
        assertEquals(40, medios.weightMillioz)
        assertEquals(Metal.Silver, medios.metal)
        assertNull(medios.finish)
        assertEquals(SeriesStatus.Closed, medios.seriesStatus)
        assertEquals(18, medios.members.size)
        assertEquals(
            listOf(
                1894, 1900, 1901, 1903, 1911, 1912, 1919, 1921, 1924, 1929,
                1935, 1936, 1944, 1945, 1946, 1948,
            ),
            medios.members.filter { it.numistaTypeId == 4_369 }.map { it.year },
        )
        assertEquals(
            listOf(1954),
            medios.members.filter { it.numistaTypeId == 5_317 }.map { it.year },
        )
        assertEquals(
            "1954 (acuñada en 1955)",
            medios.members.first { it.numistaTypeId == 5_317 }.label,
        )
        assertEquals(
            listOf(1960),
            medios.members.filter { it.numistaTypeId == 9_488 }.map { it.year },
        )
        assertTrue(medios.members.none { it.year == 1955 })
        assertTrue(medios.members.all { it.numistaIssueIds.isEmpty() })
        assertTrue(medios.closedNote!!.contains("59789"), medios.closedNote!!)
    }

    @Test
    fun `the portuguese annual set lists the seven 500 escudos in silver 500`() {
        val escudos = find("portugal-500-escudos-plata-500")
        assertEquals(1, escudos.schemaVersion)
        assertEquals("500 escudos conmemorativos de plata .500 de Portugal", escudos.family)
        assertEquals(450, escudos.weightMillioz)
        assertNull(escudos.finish)
        // Verificados uno a uno en numista.com: KM 686, 702, 701, 705, 723, 725, 733 y
        // Gomes R 144.01 a R 150.01, un año por moneda entre 1995 y 2001.
        assertEquals(
            listOf(13_042, 11_696, 13_043, 13_044, 10_207, 13_045, 13_046),
            escudos.members.map { it.numistaTypeId },
        )
        assertEquals((1995..2001).toList(), escudos.members.map { it.year })
    }

    /**
     * Las cinco fichas traen `series: null`, así que `source` es la página de un tipo y el límite
     * lo pone el Handboek van de Nederlandse munten 1795-2001: los 10 gulden de Beatrix son LSch.
     * 1168 a 1172, y los de Juliana pesan 25 g y son otra variante. Caben dos leyes, .720 en 1994
     * y .800 las demás, todas de 15 g, porque el catálogo manda sobre la variante de sus miembros
     * (ADR 0016).
     */
    @Test
    fun `the ten gulden of Beatrix are five and cite a type page because no series proposed them`() {
        val tientjes = find("paises-bajos-10-gulden-beatrix")
        assertEquals(1, tientjes.schemaVersion)
        assertEquals("10 gulden conmemorativos de Beatrix", tientjes.family)
        assertEquals(482, tientjes.weightMillioz)
        assertNull(tientjes.finish)
        assertEquals(SeriesStatus.Closed, tientjes.seriesStatus)
        assertTrue(tientjes.source.startsWith("https://en.numista.com/catalogue/pieces"))
        // Verificados uno a uno en numista.com: KM 216, 220, 223, 224 y 228, cinco años sin 1998.
        assertEquals(
            listOf(7_962, 7_963, 7_964, 7_965, 7_966),
            tientjes.members.map { it.numistaTypeId },
        )
        assertEquals(listOf(1994, 1995, 1996, 1997, 1999), tientjes.members.map { it.year })
    }

    /**
     * Seis escudos sueltos que salían como cinco tarjetas, dos homónimas (#157). Ninguna serie de
     * Numista propone estas listas (las fichas cuelgan de «System 1927-1968», «System 1969-1980» y
     * «System 1981-2001»), así que citan la página de un tipo. El criterio del coleccionista es
     * denominación más metal, como en los 500 y 1000 escudos de plata .500; los programas de 1977
     * y 1983 viven aparte como programas conmemorativos (ADR 0022), porque su tercera moneda no
     * está en ningún catálogo.
     */
    @Test
    fun `the six loose escudos become four catalogs by denomination and metal`() {
        val veinte = find("portugal-20-escudos-plata")
        assertEquals(675, veinte.weightMillioz)
        assertEquals(Metal.Silver, veinte.metal)
        assertEquals(listOf(11_158, 11_161, 6_580), veinte.members.map { it.numistaTypeId })
        assertEquals(listOf(1953, 1960, 1966), veinte.members.map { it.year })
        // El módulo reducido de 1966 (10,12 g de plata .650 contra 21 g de .800) entra a propósito:
        // el catálogo manda sobre la variante de sus miembros (ADR 0016) y lo dice en prosa.
        val salazar = veinte.members.last()
        assertEquals(6_580, salazar.numistaTypeId)
        assertTrue(salazar.variantNote!!.contains("10,12 g"), salazar.variantNote!!)

        val cincuenta = find("portugal-50-escudos-plata-650")
        assertEquals(579, cincuenta.weightMillioz)
        assertEquals(Metal.Silver, cincuenta.metal)
        assertEquals(
            listOf(4_930, 13_026, 13_173, 13_174, 13_027),
            cincuenta.members.map { it.numistaTypeId },
        )
        // 1970 no falta: en las 135 conmemorativas circulantes de la era del escudo no hay
        // ninguna de ese año, y en un catálogo cerrado ese hueco no se señala.
        assertEquals(listOf(1968, 1969, 1969, 1971, 1972), cincuenta.members.map { it.year })

        val dosCincuenta = find("portugal-2-50-escudos-cuproniquel")
        assertEquals(113, dosCincuenta.weightMillioz)
        assertEquals(Metal.Cupronickel, dosCincuenta.metal)
        assertEquals(listOf(6_071, 9_828, 9_829), dosCincuenta.members.map { it.numistaTypeId })

        val cinco = find("portugal-5-escudos-cuproniquel")
        assertEquals(225, cinco.weightMillioz)
        assertEquals(Metal.Cupronickel, cinco.metal)
        assertEquals(listOf(10_126, 7_337, 9_830), cinco.members.map { it.numistaTypeId })

        listOf(veinte, cincuenta, dosCincuenta, cinco).forEach { catalog ->
            assertEquals(1, catalog.schemaVersion)
            assertEquals(SeriesStatus.Closed, catalog.seriesStatus, catalog.id)
            assertNull(catalog.finish)
            assertTrue(
                catalog.source.startsWith("https://en.numista.com/catalogue/pieces"),
                catalog.id,
            )
            // El límite no lo dibuja Numista, así que cada fichero dice en prosa de dónde sale.
            assertTrue(catalog.sourceNote!!.contains("135"), catalog.id)
            assertTrue(catalog.members.all { it.numistaIssueIds.isEmpty() }, catalog.id)
        }
    }

    @Test
    fun `the 1983 portuguese trio is a set with no physical variant`() {
        val trio = find("portugal-1983-exposicion-europea-de-arte")
        assertEquals(3, trio.schemaVersion)
        assertTrue(trio.isSet)
        assertNull(trio.weightMillioz)
        assertNull(trio.finish)
        assertNull(trio.key().weightMillioz)
        // 500, 750 y 1000 escudos de plata .835, emitidas juntas en un mismo estuche.
        assertEquals(listOf(22_178, 22_179, 22_180), trio.members.map { it.numistaTypeId })
        assertTrue(trio.members.all { it.year == 1983 })
    }

    /**
     * A set (#146): 28,28 g and 35 g never share a variant key, so no derivation would propose
     * this plate. It is the two-coin silver case the Royal Mint struck for the Banco Central de
     * Venezuela, not the conservation programme, whose gold third coin (the Gallito de las Rocas)
     * was sold apart.
     */
    @Test
    fun `the 1975 venezuelan pair is the silver case and not the whole programme`() {
        val conservacion = find("venezuela-1975-conservacion-plata")
        assertEquals(3, conservacion.schemaVersion)
        assertTrue(conservacion.isSet)
        assertNull(conservacion.weightMillioz)
        assertNull(conservacion.metal)
        assertNull(conservacion.finish)
        assertNull(conservacion.key().weightMillioz)
        assertEquals(SeriesStatus.Closed, conservacion.seriesStatus)
        // Verificados uno a uno: N#37246 jaguar 25 Bs 28,28 g, N#37247 cachicamo 50 Bs 35 g.
        assertEquals(listOf(37_246, 37_247), conservacion.members.map { it.numistaTypeId })
        assertTrue(conservacion.members.all { it.year == 1975 })
        assertTrue(
            conservacion.closedNote!!.contains("Gallito"),
            "la nota tiene que decir que el programa tenía una tercera en oro",
        )
    }

    /**
     * El rótulo de una casilla lleva peso o ley sólo cuando se desvía de la lámina (#412): aquí,
     * los 22 g del de 1980 y la .835 del de 1981. La fuente de cada desviación va en su
     * `variant_note`, que no se imprime en ninguna superficie.
     */
    @Test
    fun `a member says its own weight only where it deviates from the plate`() {
        val bolivares = find("venezuela-100-bolivares-plata")
        // La lámina declara la onza de plata, que es lo que dos de las cuatro casillas pesan.
        assertEquals(1_000, bolivares.weightMillioz)
        assertEquals(Metal.Silver, bolivares.metal)
        val labelled = bolivares.members.filter { it.label.contains(" g de plata") }
        assertEquals(
            listOf("1980-muerte-del-libertador", "1981-natalicio-de-andres-bello"),
            labelled.map { it.id },
        )
        assertTrue(
            labelled.all { it.variantNote != null },
            "una cola en el rótulo sin nota que la sostenga",
        )
        assertEquals(
            listOf(
                "Bicentenario del natalicio del Libertador",
                "Bicentenario del natalicio de José María Vargas",
            ),
            bolivares.members.filterNot { it in labelled }.map { it.label },
        )
    }

    @Test
    fun `catalogs target their exact derived collection variants`() {
        val tesla = find("nikola-tesla-serbia-1oz")
        assertEquals(1, tesla.schemaVersion)
        assertEquals("Nikola Tesla", tesla.family)
        assertEquals(1_000, tesla.weightMillioz)
        assertNull(tesla.finish)
        assertEquals(
            listOf(
                150_352, 162_242, 195_591, 302_302, 334_411, 371_257, 359_331, 421_848, 421_849,
                448_067, 493_347, 493_329, null,
            ),
            tesla.members.map { it.numistaTypeId },
        )

        val spain = find("spain-face-value-18g")
        assertEquals("Serie de monedas de plata obtenidas a valor facial", spain.family)
        assertEquals(579, spain.weightMillioz)
        assertEquals(37, spain.members.size)

        val beasts = find("queens-beasts-uk-2oz")
        assertEquals("The Queen's Beasts", beasts.family)
        assertEquals(2_000, beasts.weightMillioz)
        assertEquals(11, beasts.members.size)

        val independence = find("us-independence-250th-spain-10-euros")
        assertEquals(
            "250th anniversary of the United States Declaration of Independence",
            independence.family,
        )
        assertEquals(868, independence.weightMillioz)
        assertEquals(8, independence.members.size)

        val tudorBullion = find("tudor-beasts-uk-2oz-bullion")
        assertEquals(Finish.Bullion, tudorBullion.finish)
        val tudorProof = find("tudor-beasts-uk-1oz-proof")
        assertEquals(Finish.Proof, tudorProof.finish)
    }

    @Test
    fun `the six visible slots distinguish issued unlisted and announced coins`() {
        val tesla = find("nikola-tesla-serbia-1oz")
        val energyMedicine = tesla.members.single { it.year == 2026 }
        assertTrue(energyMedicine.isUnlisted)
        assertEquals("Energy Medicine", energyMedicine.label)

        val redDataBook = find("red-data-book-russia")
        assertTrue(redDataBook.members.none { it.year == 2025 })
        assertEquals(
            listOf("Caucasian Wildcat", "Spectacled Eider", "Toad-headed Agama"),
            redDataBook.members.filter { it.year == 2026 }.map { it.label },
        )
        assertTrue(redDataBook.members.filter { it.year == 2026 }.all { it.isAnnounced })

        val monuments = find("architectural-monuments-russia-3-roubles")
        val tulaMuseum = monuments.members.single { it.year == 2025 }
        assertEquals("Tula State Museum of Arms", tulaMuseum.label)
        assertEquals(583_338, tulaMuseum.numistaTypeId)
        val mirozhsky = monuments.members.single { it.year == 2026 }
        assertTrue(mirozhsky.isAnnounced)
        assertEquals("Holy Transfiguration Mirozhsky Monastery in Pskov", mirozhsky.label)

        val rwanda = find("rwanda-lunar-50-francs")
        val snake = rwanda.members.single { it.year == 2025 }
        assertEquals("Year of the Snake", snake.label)
        assertEquals(448_800, snake.numistaTypeId)
    }

    @Test
    fun `a numista subseries still fills the curated rwanda lunar catalog`() {
        val rwanda = find("rwanda-lunar-50-francs")
        val snake = CollectedItem(id = 1, quantity = 1, typeId = 448_800)
        val metadata = TypeMeta(
            id = 448_800,
            title = "50 Francs (Year of the Snake)",
            family = "Lunar ounce - Year of the Snake",
            issuerCode = "rwanda",
            minYear = 2025,
            maxYear = 2025,
            weightOz = 1.0,
            metal = Metal.Silver,
        )

        val derivation = deriveCollection(listOf(snake), mapOf(metadata.id to metadata), catalogs)

        assertEquals(listOf(rwanda.key()), derivation.derivedCollections.map { it.key() })
        assertEquals(listOf(snake), derivation.itemsByKey[rwanda.key()])
        assertTrue(derivation.unclassified.isEmpty())
        assertEquals(1, buildCollectionCatalogAlbum(rwanda, listOf(snake)).ownedMembers())
    }

    /**
     * Los fuertes son veintidós años y dos tipos: el venezolano de 1876 es la misma moneda con otro
     * nombre (la ley de 1871 llamó venezolano a la unidad y la del 31 de marzo de 1879 la renombró
     * bolívar), y por eso la familia dice «fuertes», como el coleccionista. Los dos ensayos de 1874
     * comparten los 25 g de plata .900, pero son patterns y no abren hueco (lo dice `closed_note`).
     */
    @Test
    fun `the venezuelan fuertes run from the 1876 venezolano to 1936`() {
        val fuertes = find("venezuela-fuertes")
        assertEquals(2, fuertes.schemaVersion)
        assertTrue(fuertes.isDateRun)
        assertEquals("Fuertes de Venezuela", fuertes.family)
        assertEquals(804, fuertes.weightMillioz)
        assertEquals(22, fuertes.members.size)
        assertEquals(22, fuertes.members.map { it.year }.distinct().size)
        val venezolano = fuertes.members.first()
        assertEquals(1876, venezolano.year)
        assertEquals(48_672, venezolano.numistaTypeId)
        assertEquals("1 Venezolano", venezolano.label)
        // Las otras veintiuna son un solo tipo y su etiqueta es el año.
        val fuerte = fuertes.members.drop(1)
        assertEquals(21, fuerte.size)
        assertTrue(fuerte.all { it.numistaTypeId == 10_340 })
        assertTrue(fuerte.all { it.label == it.year.toString() })
        assertTrue(fuertes.closedNote!!.contains("352550"), fuertes.closedNote!!)
    }

    /**
     * Lunar Series III runs 2020-2031; the 2019 pig closes Lunar II. Numista files the Royal
     * Australian Mint's lunar line under the same series, so the members are Perth's: .9999 and
     * 40,9 mm against RAM's .999 and 40 mm. 2028-2031 are announced slots from Perth's zodiac
     * guide, scheduled subjects and not released designs.
     */
    @Test
    fun `lunar iii bullion fixes the perth cycle from 2020 to 2031`() {
        val lunar = find("lunar-iii-perth-1oz-bullion")
        assertEquals(1, lunar.schemaVersion)
        assertEquals("Lunar Series III", lunar.family)
        assertEquals(1_000, lunar.weightMillioz)
        assertEquals(Finish.Bullion, lunar.finish)
        assertEquals((2020..2031).toList(), lunar.members.map { it.year })
        assertEquals(12, lunar.members.size)
        assertEquals(8, lunar.members.count { !it.isAnnounced })
        assertEquals(4, lunar.members.count { it.isAnnounced })
        assertEquals(
            listOf(179_438, 235_118, 307_024, 342_221, 386_213, 441_816, 483_798, 602_440) +
                List(4) { null },
            lunar.members.map { it.numistaTypeId },
        )
        assertEquals(
            listOf(
                listOf(459_056, 698_372),
                listOf(582_780),
                listOf(698_366, 698_365),
                listOf(747_609),
                listOf(841_265),
                listOf(923_283),
                listOf(979_731),
                listOf(1_125_086),
            ) + List(4) { emptyList() },
            lunar.members.map { it.numistaIssueIds },
        )
        val announced = lunar.members.filter { it.isAnnounced }
        assertEquals(
            listOf(
                "Year of the Monkey",
                "Year of the Rooster",
                "Year of the Dog",
                "Year of the Pig",
            ),
            announced.map { it.label },
        )
        assertTrue(announced.all { it.designTypeId == null })
        assertTrue(announced.all { it.source?.startsWith("https://www.perthmint.com/") == true })
        assertTrue(announced.all { it.sourceNote?.contains("tema programado") == true })

        val album = buildCollectionCatalogAlbum(lunar, emptyList())
        assertEquals(8, album.issuedMembers())
        assertEquals(4, album.announcedMembers())
        val entries = plateSubject(PlateResult.Available(lunar, album)).entries
        assertEquals("Progreso" to "0 / 8 emisiones", entries[0])
        assertEquals("" to "4 anunciadas", entries[1])
        // La línea de la Royal Australian Mint: misma serie en Numista, otra moneda.
        val royalAustralianMint =
            listOf(219_663, 266_550, 309_870, 355_589, 406_506, 444_584, 529_884)
        assertTrue(lunar.members.none { it.numistaTypeId in royalAustralianMint })
        // El cerdo de 2019 cierra Lunar II; esta empieza en el ratón de 2020.
        assertEquals(2019, find("lunar-ii-perth-1oz-bullion").members.last().year)
    }

    /**
     * La proof coloreada repite tres tipos de la bullion entre 2021 y 2023 y se separa por emisión:
     * sin esos ids una proof llenaría la casilla de bullion, o al revés. Las demás casillas también
     * los declaran, por otros acabados del mismo tipo. La de 2027 declara sólo la emisión del
     * estuche de tres de 1 oz, la única de la ficha al curarla; la suelta añadirá otra.
     */
    @Test
    fun `lunar iii proof coloured is issue qualified from 2020 to 2027`() {
        val proofColoured = find("lunar-iii-perth-1oz-proof-coloured")
        assertEquals(1, proofColoured.schemaVersion)
        assertEquals("Lunar Series III", proofColoured.family)
        assertEquals(1_000, proofColoured.weightMillioz)
        assertEquals(Finish.ProofColoured, proofColoured.finish)
        assertEquals(Metal.Silver, proofColoured.metal)
        assertEquals(SeriesStatus.Open, proofColoured.seriesStatus)
        assertEquals((2020..2027).toList(), proofColoured.members.map { it.year })
        assertEquals(
            listOf(185_343, 235_118, 307_024, 342_221, 394_043, 576_294, 507_204, 602_446),
            proofColoured.members.map { it.numistaTypeId },
        )
        assertEquals(
            listOf(
                listOf(467_674),
                listOf(585_569, 582_778),
                listOf(698_367, 700_090),
                listOf(970_595),
                listOf(833_480),
                listOf(1_088_982, 1_091_660),
                listOf(1_002_105),
                listOf(1_123_374),
            ),
            proofColoured.members.map { it.numistaIssueIds },
        )

        val bullion = find("lunar-iii-perth-1oz-bullion")
        val bullionPiece = CollectedItem(id = 1, quantity = 1, typeId = 342_221, issueId = 747_609)
        val proofColouredPiece =
            CollectedItem(id = 2, quantity = 1, typeId = 342_221, issueId = 970_595)
        val proofPiece = CollectedItem(id = 3, quantity = 1, typeId = 342_221, issueId = 908_897)

        assertEquals(1, buildCollectionCatalogAlbum(bullion, listOf(bullionPiece)).ownedMembers())
        assertEquals(0, buildCollectionCatalogAlbum(bullion, listOf(proofColouredPiece)).ownedMembers())
        assertEquals(0, buildCollectionCatalogAlbum(bullion, listOf(proofPiece)).ownedMembers())
        assertEquals(
            1,
            buildCollectionCatalogAlbum(proofColoured, listOf(proofColouredPiece)).ownedMembers(),
        )
        assertEquals(0, buildCollectionCatalogAlbum(proofColoured, listOf(bullionPiece)).ownedMembers())
        assertEquals(0, buildCollectionCatalogAlbum(proofColoured, listOf(proofPiece)).ownedMembers())

        val metadata = TypeMeta(
            id = 342_221,
            title = "1 Dollar - Elizabeth II Australian Lunar Year of the Rabbit",
            family = "Lunar Series III",
            issuerCode = "australie",
            minYear = 2023,
            maxYear = 2023,
            weightOz = 1.0,
            finish = Finish.Bullion,
            metal = Metal.Silver,
        )
        val derivation = deriveCollection(
            listOf(bullionPiece, proofColouredPiece, proofPiece),
            mapOf(metadata.id to metadata),
            catalogs,
        )
        assertEquals(
            setOf(bullion.key(), proofColoured.key()),
            derivation.derivedCollections.map { it.key() }.toSet(),
        )
        assertEquals(listOf(bullionPiece), derivation.itemsByKey[bullion.key()])
        assertEquals(listOf(proofColouredPiece), derivation.itemsByKey[proofColoured.key()])
        assertEquals(1, derivation.unclassified.size)
        assertEquals(proofPiece, derivation.unclassified.single().item)
        assertEquals(
            UnclassifiedReason.IssueNotClaimedByCatalog,
            derivation.unclassified.single().reason,
        )
    }

    /**
     * Equilibrium es una serie de Numista con tres colecciones (#43): ocho onzas de plata, cinco
     * décimos de onza de oro y cinco onzas de oro. Se cura la de plata; el oro queda fuera a
     * propósito.
     *
     * El emisor alterna entre Tokelau y Niue sin cambiar de ceca (es un acuerdo de respaldo legal
     * de la Pressburg Mint), así que las ocho son una tirada anual. La cabecera dice Tokelau y las
     * dos casillas de Niue, 2023 y 2025 (N#356004 y N#477907, de 2 dólares neozelandeses contra los
     * 5 de Tokelau), declaran su emisor (#170).
     */
    @Test
    fun `equilibrium is the pressburg silver ounce from 2018 to 2025`() {
        val equilibrium = find("equilibrium-pressburg-1oz-silver")
        assertEquals(1, equilibrium.schemaVersion)
        assertEquals("Equilibrium", equilibrium.family)
        assertEquals(1_000, equilibrium.weightMillioz)
        assertNull(equilibrium.finish)
        assertEquals((2018..2025).toList(), equilibrium.members.map { it.year })
        assertEquals(
            listOf(188_952, 194_187, 241_862, 307_244, 334_281, 356_004, 407_407, 477_907),
            equilibrium.members.map { it.numistaTypeId },
        )
        assertEquals("tokelau", equilibrium.issuerCode)
        assertEquals(
            listOf(null, null, null, null, null, "niue", null, "niue"),
            equilibrium.members.map { it.issuerCode },
        )
        assertEquals(
            listOf(
                "tokelau", "tokelau", "tokelau", "tokelau",
                "tokelau", "niue", "tokelau", "niue",
            ),
            equilibrium.members.map { equilibrium.issuerCodeOf(it) },
        )
        assertEquals(setOf("tokelau", "niue"), equilibrium.issuerCodes())
        // Las diez de oro de la misma serie: décimo de onza y onza, ninguna es casilla de esta.
        val gold = listOf(
            307_242, 334_283, 356_002, 407_410, 477_905,
            309_842, 334_282, 356_003, 407_409, 477_904,
        )
        assertTrue(equilibrium.members.none { it.numistaTypeId in gold })
    }

    /**
     * El primer catálogo temático (#257): el límite es un tema que declaró el padre y que cerró en
     * cuatro piezas. Funciona con lo que ya había: el ADR 0020 no exige un solo emisor ni un solo
     * patrón físico, el fichero manda sobre la variante de sus miembros (ADR 0016) y cada casilla
     * puede declarar su emisor (#170).
     *
     * Se declara el peso del ancla, los 27,07 g del real de a ocho (870 milésimas de onza), y las
     * tres desviaciones van escritas: el thaler pesa 28,0668 g (norma Conventionsthaler de 1750) y
     * las dos de 1813 son lo que queda de un duro perforado de 26,65 g, 21,035 g el anillo y
     * 5,619 g el disco. Está cerrado: Carlos IV acaba en 1808, las de 1813 se desmonetizaron en
     * 1829 y el thaler que Viena sigue vendiendo lleva la fecha congelada en 1780.
     */
    @Test
    fun `historia del real is the first thematic catalog and spans three issuers`() {
        val theme = find("historia-del-real")
        assertEquals(1, theme.schemaVersion)
        assertEquals("Historia del real", theme.family)
        assertEquals("Historia del real", theme.shortName)
        assertEquals(870, theme.weightMillioz)
        assertNull(theme.finish)
        assertEquals(Metal.Silver, theme.metal)
        assertEquals(SeriesStatus.Closed, theme.seriesStatus)
        assertEquals(listOf(1780, 1791, 1813, 1813), theme.members.map { it.year })
        assertEquals(listOf(7_393, 18_852, 19_811, 17_316), theme.members.map { it.numistaTypeId })
        // La cabecera es México, emisor del real, y es el defecto de su casilla.
        assertEquals("mexique", theme.issuerCode)
        assertEquals(
            listOf("autriche-habsbourg", "mexique", "new_south_wales", "new_south_wales"),
            theme.members.map { theme.issuerCodeOf(it) },
        )
        assertEquals(
            setOf("autriche-habsbourg", "mexique", "new_south_wales"),
            theme.issuerCodes(),
        )
        // Las cuatro llevan nota (ADR 0016): tres explican su peso y la del real por qué la
        // casilla es el tipo entero y no un año.
        assertTrue(theme.members.all { it.variantNote?.isNotBlank() == true })
        assertTrue(theme.members.all { it.isIssued })
    }

    /**
     * La emisión BU anual oficial de una onza. Las marcas que forman parte de la emisión anual
     * (P100, P20, P125 y 35th Anniversary) sí entran; quedan fuera privies opcionales, color,
     * dorado, high relief, proof, piezas de estuche y mules. Tipo y emisión se fijan juntos porque
     * varias de esas variantes comparten tipo o año; 2005 tiene su propia casilla.
     */
    @Test
    fun `the kookaburra catalog is the issue-qualified standard annual bullion run`() {
        val kookaburra = find("australian-kookaburra-perth-1oz")
        assertEquals("Australian Kookaburra · Perth Mint · 1 oz de plata bullion anual estándar", kookaburra.name)
        assertEquals(1_000, kookaburra.weightMillioz)
        assertEquals(Finish.Bullion, kookaburra.finish)
        assertEquals(Metal.Silver, kookaburra.metal)
        // La tabla es el catálogo: año, ficha y emisión, verificados uno a uno contra numista.com.
        assertEquals(
            listOf(
                Triple(1990, 20_585, 109_614), Triple(1991, 22_330, 119_246),
                Triple(1992, 20_600, 109_738), Triple(1993, 20_601, 109_741),
                Triple(1994, 17_335, 361_408), Triple(1995, 17_336, 87_409),
                Triple(1996, 10_841, 60_606), Triple(1997, 17_339, 135_829),
                Triple(1998, 17_340, 87_415), Triple(1999, 17_342, 87_417),
                Triple(2000, 17_343, 87_418), Triple(2001, 17_357, 87_504),
                Triple(2002, 20_627, 109_808), Triple(2003, 15_415, 372_871),
                Triple(2004, 57_179, 225_111), Triple(2005, 20_658, 630_201),
                Triple(2006, 74_351, 261_827), Triple(2007, 191_855, 478_174),
                Triple(2008, 29_124, 146_935), Triple(2009, 17_382, 367_420),
                Triple(2010, 17_387, 87_569), Triple(2011, 26_066, 136_475),
                Triple(2012, 26_278, 137_729), Triple(2013, 42_224, 183_132),
                Triple(2014, 49_184, 205_638), Triple(2015, 65_421, 243_827),
                Triple(2016, 80_390, 275_276), Triple(2017, 95_694, 312_279),
                Triple(2018, 124_796, 366_016), Triple(2019, 161_560, 734_242),
                Triple(2020, 183_220, 464_715), Triple(2021, 242_195, 587_493),
                Triple(2022, 308_142, 691_355), Triple(2023, 349_979, 760_315),
                Triple(2024, 395_644, 835_712), Triple(2025, 451_849, 923_574),
                Triple(2026, 552_773, 1_055_814),
            ),
            kookaburra.members.map { member ->
                Triple(member.year, member.numistaTypeId, member.numistaIssueIds.single())
            },
        )
        // Tipos antes asignados por error a las casillas anuales de 1991 y 1998.
        val displacedTypes = listOf(571_411, 313_416)
        assertTrue(kookaburra.members.none { it.numistaTypeId in displacedTypes })

        // El estuche de oro P20: veinte vigésimos de onza, no la onza anual de plata.
        val goldSetTypes = listOf(
            426_539, 458_300, 458_463, 458_703, 458_905, 459_012, 459_137, 459_344,
            459_536, 460_076, 460_908, 461_129, 462_061, 462_390, 462_552, 462_805,
            462_940, 463_069, 463_187, 463_319,
        )
        assertTrue(kookaburra.members.none { it.numistaTypeId in goldSetTypes })
    }

    /**
     * Mismo criterio que el Kookaburra (#70, #106): la emisión BU anual estándar de Perth, una
     * casilla por año cualificada por emisión. Empieza en 2007, como la tabla de la ceca, aunque la
     * serie 10445 de Numista empiece en 2011: las cuatro primeras están en la serie hermana 4424.
     */
    @Test
    fun `the koala catalog is the issue-qualified standard annual bullion run`() {
        val koala = find("australian-koala-perth-1oz")
        assertEquals("Australian Koala · Perth Mint · 1 oz de plata bullion anual estándar", koala.name)
        assertEquals(1_000, koala.weightMillioz)
        assertEquals(Finish.Bullion, koala.finish)
        assertEquals(Metal.Silver, koala.metal)
        // La tabla es el catálogo: año, ficha y emisión, verificados uno a uno contra numista.com.
        assertEquals(
            listOf(
                Triple(2007, 20_532, 109_470), Triple(2008, 20_535, 109_474),
                Triple(2009, 17_379, 87_561), Triple(2010, 17_386, 87_568),
                Triple(2011, 25_340, 133_627), Triple(2012, 32_572, 155_901),
                Triple(2013, 42_672, 184_923), Triple(2014, 54_800, 220_141),
                Triple(2015, 68_298, 248_794), Triple(2016, 85_886, 289_240),
                Triple(2017, 100_525, 321_230), Triple(2018, 132_621, 380_880),
                Triple(2019, 160_928, 430_168), Triple(2020, 194_185, 481_352),
                Triple(2021, 281_802, 644_120), Triple(2022, 319_477, 709_997),
                Triple(2023, 358_743, 775_700), Triple(2024, 413_950, 861_187),
                Triple(2025, 459_669, 935_627), Triple(2026, 576_543, 1_089_274),
            ),
            koala.members.map { member ->
                Triple(member.year, member.numistaTypeId, member.numistaIssueIds.single())
            },
        )

        // Alternativas del mismo año que no son la bullion anual estándar.
        val excludedTypes = listOf(
            170_428, // 2009 gilded
            76_391, // 2010 gilt
            359_230, // 2011 gilded
            402_992, // 2023 coloured
            398_192, // 2024 Elizabeth «in the name of», .999 / 40 mm / 25.000
            476_400, // 2025 .999 / 40 mm
            478_727, // 2025 coloured
            557_132, // 2026 .999 / 40 mm / 25.000
            592_033, // 2026 coloured
        )
        assertTrue(koala.members.none { it.numistaTypeId in excludedTypes })
    }

    /**
     * El Koala de la Royal Australian Mint (#152) es un programa propio, no tres huérfanas: la ceca
     * anuncia el de 2026 como «a third release … follows the success of the 2025 Koala Series».
     * La serie 10445 sostiene este catálogo y el de Perth (ADR 0020: la serie propone y el fichero
     * afirma cobertura). Cada tipo trae una sola emisión BU, así que no hace falta cualificar.
     */
    @Test
    fun `the ram koala catalog is the other mint's annual bullion run`() {
        val koala = find("australian-koala-ram-1oz")
        assertEquals("Koala del RAM · Royal Australian Mint · 1 oz de plata bullion anual", koala.name)
        assertEquals(1_000, koala.weightMillioz)
        assertEquals(Finish.Bullion, koala.finish)
        assertEquals(Metal.Silver, koala.metal)
        assertEquals(SeriesStatus.Open, koala.seriesStatus)
        assertEquals(
            listOf(2024 to 398_192, 2025 to 476_400, 2026 to 557_132),
            koala.members.map { member -> member.year to member.numistaTypeId },
        )
        assertTrue(koala.members.all { it.numistaIssueIds.isEmpty() })

        // La misma serie de Numista que el Koala de Perth, y sin un solo tipo en común.
        val perth = find("australian-koala-perth-1oz")
        assertEquals(perth.source, koala.source)
        assertTrue(koala.members.none { member -> member.numistaTypeId in perth.members.map { it.numistaTypeId } })

        // Las otras tres monedas de cada entrega son otra variante física, otra lámina.
        val otherVariants = listOf(
            400_931, // 2024 · 5 dólares plata proof alto relieve
            400_939, // 2024 · 100 dólares oro bullion
            467_387, // 2025 · 5 dólares plata proof
            509_567, // 2025 · 100 dólares oro
            464_499, // 2025 · 50 céntimos
            555_879, // 2026 · 50 céntimos
        )
        assertTrue(koala.members.none { it.numistaTypeId in otherVariants })
    }

    /**
     * Lunar Series II cierra el ciclo 2008-2019. Cada tipo mezcla la bullion estándar con colour,
     * gilded, proof, typesets y privys, así que la casilla se identifica por emisión.
     *
     * La de 2012 acepta además la «BU - Dark Orange» de 2.500 piezas (#216), que ninguna lámina de
     * coloreadas acogería: en 2012 Numista archiva quince colores bajo N#28574 y ninguno es la
     * edición del año. El ADR 0016 obliga a declararlo en el `variant_note`.
     */
    @Test
    fun `the lunar ii bullion catalog is the issue-qualified standard annual run`() {
        val lunar = find("lunar-ii-perth-1oz-bullion")
        assertEquals(
            "Lunar Series II · Perth Mint · 1 oz de plata bullion anual estándar",
            lunar.name,
        )
        assertEquals(1_000, lunar.weightMillioz)
        assertEquals(Finish.Bullion, lunar.finish)
        assertEquals(Metal.Silver, lunar.metal)
        assertEquals(SeriesStatus.Closed, lunar.seriesStatus)
        assertEquals((2008..2019).toList(), lunar.members.map { it.year })
        assertEquals(
            listOf(
                Triple(2008, 28_575, listOf(145_935)),
                Triple(2009, 17_378, listOf(136_304)),
                Triple(2010, 17_383, listOf(87_565)),
                Triple(2011, 17_388, listOf(87_570)),
                Triple(2012, 28_574, listOf(145_934, 1_108_238)),
                Triple(2013, 37_980, listOf(172_471)),
                Triple(2014, 49_355, listOf(206_294)),
                Triple(2015, 66_615, listOf(309_546)),
                Triple(2016, 80_295, listOf(278_592)),
                Triple(2017, 95_688, listOf(312_257)),
                Triple(2018, 129_091, listOf(374_553)),
                Triple(2019, 150_358, listOf(412_033)),
            ),
            lunar.members.map { member ->
                Triple(member.year, member.numistaTypeId, member.numistaIssueIds.toList())
            },
        )
        assertTrue(lunar.members.all { it.numistaIssueIds.isNotEmpty() })

        val dragon = lunar.members.single { it.year == 2012 }
        assertTrue(dragon.variantNote!!.contains("Dark Orange"), dragon.variantNote!!)
        assertTrue(lunar.members.count { it.variantNote != null } == 1)
    }

    /**
     * La Seymour Panther (N#604513, .9999, 62,42 g y ⌀38,61 mm como sus hermanas) cerró en 2026
     * las diez bestias de bullion. La lámina sigue abierta por la completer de 2027, sin casilla
     * anunciada porque la Royal Mint no ha nombrado un bullion de 2 oz de ella (en The Queen's
     * Beasts sí lo hubo, N#299474). La proof de 2 oz de 2022, N#307800, es plata .999 de 40 mm:
     * otra moneda, que no rellena un hueco de bullion.
     */
    @Test
    fun `the tenth tudor beast closes the ten and the plate stays open for the completer`() {
        val tudor = find("tudor-beasts-uk-2oz-bullion")
        assertEquals(SeriesStatus.Open, tudor.seriesStatus)
        assertEquals(10, tudor.members.size)
        assertEquals(10, tudor.members.count { !it.isAnnounced })
        assertTrue(tudor.members.none { it.isAnnounced })

        val panther = tudor.members.last()
        assertEquals("2026-seymour-panther", panther.id)
        assertEquals(604_513, panther.numistaTypeId)
        assertEquals(2026, panther.year)
        assertNull(panther.designTypeId)
        assertNull(panther.source)
        assertTrue(tudor.sourceNote!!.contains("completer"), tudor.sourceNote!!)

        // Una proof de la serie no rellena ninguna casilla de bullion.
        val album = buildCollectionCatalogAlbum(
            tudor,
            listOf(CollectedItem(id = 1, quantity = 1, typeId = 307_800)),
        )
        assertEquals(0, album.ownedMembers())
        assertEquals(10, album.issuedMembers())
        assertEquals(0, album.announcedMembers())
        assertEquals(
            CollectionCatalogMemberStatus.Missing,
            album.members.last().status,
        )
        assertFalse(tudor.isEvidencedBy(listOf(CollectedItem(id = 1, quantity = 1, typeId = 307_800))))
    }

    /**
     * El dólar de plata canadiense son dos catálogos que pesan lo mismo (23,33 g, 750 millioz) y se
     * separan por la familia, la primera componente de la clave. Ninguno de los treinta y dos tipos
     * trae `series` en Numista, así que la familia la pone el catálogo (ADR 0009).
     *
     * Cierran por hechos distintos: la .800 en 1968, cuando la Royal Canadian Mint pasó el dólar al
     * níquel (N#3326, 15,62 g), y la .500 en 1992, cuando pasó a plata esterlina (N#23296, .925 y
     * 25,175 g). El proof silver dollar se sigue emitiendo cada año desde 1971; lo que cierra es la
     * variante, que es la unidad de catálogo (#43).
     */
    @Test
    fun `the canadian silver dollar is two catalogs told apart by family and not by weight`() {
        val eightHundred = find("canada-dolar-plata-800")
        val fiveHundred = find("canada-dolar-conmemorativo-plata-500")
        for (catalog in listOf(eightHundred, fiveHundred)) {
            assertEquals(1, catalog.schemaVersion)
            assertEquals("canada", catalog.issuerCode)
            assertEquals(750, catalog.weightMillioz)
            assertNull(catalog.finish)
            assertEquals(SeriesStatus.Closed, catalog.seriesStatus)
        }
        assertNotEquals(eightHundred.key(), fiveHundred.key())

        // Once tipos de 1935 a 1967, verificados por búsqueda de peso 23,2-23,5 g sobre todas las
        // categorías de numista.com: cinco de circulación y seis conmemorativas circulantes.
        assertEquals(
            listOf(447, 448, 449, 450, 451, 452, 453, 454, 455, 456, 457),
            eightHundred.members.map { it.numistaTypeId },
        )
        assertEquals(
            listOf(1935, 1936, 1937, 1939, 1948, 1949, 1953, 1958, 1964, 1965, 1967),
            eightHundred.members.map { it.year },
        )
        // La etiqueta de año es el primer año del tipo cuando el tipo abarca varios (#63):
        // N#449 cubre 1937-1947, N#451 1948-1952, N#453 1953-1963 y N#456 1965-1966.
        assertEquals(11, eightHundred.members.size)

        assertEquals((1971..1991).toList(), fiveHundred.members.map { it.year })
        assertEquals(
            listOf(
                21_111, 17_839, 19_493, 18_797, 11_564, 1_880, 10_973, 19_352, 16_315, 23_272,
                23_273, 6_786, 23_275, 23_276, 23_277, 19_865, 16_314, 23_278, 19_502, 23_279,
                15_517,
            ),
            fiveHundred.members.map { it.numistaTypeId },
        )
        // La de 1992 es la primera esterlina y no es casilla de nadie.
        assertTrue(catalogs.none { catalog -> catalog.members.any { it.numistaTypeId == 23_296 } })
    }

    /**
     * Las personalidades destacadas de Rusia son dos catálogos por el corte de 1998 (#159): plata
     * .500 de 15,87 g (7,78 g finos, un cuarto de onza) contra plata .925 de 17,00 g (15,55 g
     * finos, media onza). La serie 5460 de Numista junta las dos épocas; las separa la variante
     * física (ADR 0020), y el límite lo pone la ceca: sus números 5110-0001 a 5110-0020 son .500 y
     * el 5110-0021 es el primer .925, comprobado ficha a ficha en cbr.ru.
     *
     * No se resolvió con un `variant_note`, como los 20 escudos de 1966, porque aquí no es una
     * moneda suelta sino una época entera con la mitad de plata.
     */
    @Test
    fun `the russian personalities split at the 1998 redenomination by fineness`() {
        val sterling = find("outstanding-personalities-russia-2-roubles")
        val fiveHundred = find("outstanding-personalities-russia-2-roubles-plata-500")
        for (catalog in listOf(sterling, fiveHundred)) {
            assertEquals(1, catalog.schemaVersion)
            assertEquals("russie", catalog.issuerCode)
            assertNull(catalog.finish)
            assertEquals(Metal.Silver, catalog.metal)
        }
        assertNotEquals(sterling.key(), fiveHundred.key())

        // La .925 conserva el id, la familia y el peso que tenía antes del corte.
        assertEquals("Outstanding Personalities of Russia", sterling.family)
        assertEquals(547, sterling.weightMillioz)
        assertEquals(SeriesStatus.Open, sterling.seriesStatus)
        assertEquals(104, sterling.members.size)
        assertEquals(1998, sterling.members.first().year)

        // 15,87 g son 510 millioz y `normalizeWeightMillioz` los ajusta a 500 por tolerancia:
        // declarar 500 evita que una pieza suelta de la misma época caiga en otra tarjeta.
        assertEquals("Outstanding Personalities of Russia · plata .500", fiveHundred.family)
        assertEquals(500, fiveHundred.weightMillioz)
        assertEquals(SeriesStatus.Closed, fiveHundred.seriesStatus)
        assertTrue(fiveHundred.closedNote!!.contains("5110-0021"), fiveHundred.closedNote!!)
        assertEquals(
            listOf(
                34_864, 28_934, 61_058, 65_690, 28_935, 28_933, 28_931, 28_930, 28_932, 70_073,
                70_074, 70_171, 28_937, 70_172, 70_173, 70_174, 28_938,
            ),
            fiveHundred.members.map { it.numistaTypeId },
        )
        assertEquals(
            listOf(1994, 1994, 1994, 1994, 1994, 1995, 1995, 1995, 1995, 1996, 1996, 1997, 1997,
                1997, 1997, 1997, 1997),
            fiveHundred.members.map { it.year },
        )
        // Los dos Nikitin (5110-0018 y 5110-0019) nombran su reverso para no repetir rótulo (#22).
        assertEquals(
            listOf("A. Nikitin · la partida", "A. Nikitin · la India"),
            fiveHundred.members.filter { it.label.startsWith("A. Nikitin") }.map { it.label },
        )
        // Ningún tipo se queda en las dos láminas.
        assertEquals(
            emptySet(),
            sterling.members.mapNotNull { it.numistaTypeId }.toSet()
                .intersect(fiveHundred.members.mapNotNull { it.numistaTypeId }.toSet()),
        )
    }

    /**
     * El Panda de plata son **dos** catálogos por el corte métrico de 2016 (#109): 1 oz troy
     * (31,1 g → 1000 millioz) cerrado en 1989-2015, y 30 g (965 millioz) abierto desde 2016.
     * Un tipo por casilla salvo 2001-2002, que comparten N#19714 y se cualifican por emisión.
     * Los de 1983-1987 son otra métrica; proof de colección y conmemorativas quedan fuera.
     */
    @Test
    fun `the silver panda splits at the 2016 metric cut into 1oz and 30g catalogs`() {
        val oneOz = find("china-silver-panda-1oz-bullion")
        val thirtyG = find("china-silver-panda-30g-bullion")
        assertEquals(2, oneOz.schemaVersion)
        assertEquals(2, thirtyG.schemaVersion)
        assertEquals("chine", oneOz.issuerCode)
        assertEquals("chine", thirtyG.issuerCode)
        assertEquals(1_000, oneOz.weightMillioz)
        assertEquals(965, thirtyG.weightMillioz)
        assertEquals(Finish.Bullion, oneOz.finish)
        assertEquals(Finish.Bullion, thirtyG.finish)
        assertEquals(Metal.Silver, oneOz.metal)
        assertEquals(Metal.Silver, thirtyG.metal)
        assertEquals(SeriesStatus.Closed, oneOz.seriesStatus)
        assertEquals(SeriesStatus.Open, thirtyG.seriesStatus)
        assertTrue(oneOz.closedNote?.contains("2016") == true)
        assertNotEquals(oneOz.key(), thirtyG.key())

        assertEquals((1989..2015).toList(), oneOz.members.map { it.year })
        assertEquals(27, oneOz.members.size)
        assertEquals(
            listOf(
                19_740, 51_539, 30_111, 19_638, 19_654, 59_113, 34_863, 19_675, 19_676,
                19_678, 19_043, 19_741, 19_714, 19_714, 19_715, 19_716, 19_717, 32_571,
                39_559, 9_162, 37_959, 23_454, 19_783, 37_955, 42_219, 51_967, 71_394,
            ),
            oneOz.members.map { it.numistaTypeId },
        )
        assertEquals(listOf(103_629), oneOz.members.single { it.year == 2001 }.numistaIssueIds)
        assertEquals(listOf(103_630), oneOz.members.single { it.year == 2002 }.numistaIssueIds)

        assertEquals(2016, thirtyG.members.mapNotNull { it.year }.min())
        assertEquals(
            listOf(
                80_896, 97_619, 127_720, 154_933, 183_654, 252_620, 311_723, 349_619,
                390_608, 439_242, 531_574,
            ),
            thirtyG.members.map { it.numistaTypeId },
        )
        assertEquals(listOf(1_029_431), thirtyG.members.single { it.year == 2026 }.numistaIssueIds)

        // Early metric and gold lookalikes stay out of both plates.
        val excluded = listOf(19_602, 36_343, 19_637, 32_134, 17_600, 58_472, 17_689)
        assertTrue(catalogs.none { catalog -> catalog.members.any { it.numistaTypeId in excluded } })
    }

    @Test
    fun `no two catalogs claim the same variant key`() {
        val keys = catalogs.map { it.key() }
        assertEquals(keys.size, keys.distinct().size, "dos catálogos comparten clave de variante")
    }
}
