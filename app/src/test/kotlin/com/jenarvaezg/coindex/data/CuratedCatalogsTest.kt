package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.domain.CatalogSeeds
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
import com.jenarvaezg.coindex.ui.plateEntries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The shipped catalogs are the most expensive asset in the project: every `numista_type_id`
 * was verified against numista.com by hand. This pins what they contain so a bad edit fails
 * here instead of producing a wrong "me falta" on someone's phone.
 */
class CuratedCatalogsTest {
    private val catalogs: List<CollectionCatalog> = CatalogSeeds.parseAll(CatalogFiles.all())

    private fun find(id: String) = catalogs.first { it.id == id }

    @Test
    fun `every shipped catalog parses and validates`() {
        assertEquals(35, catalogs.size)
        catalogs.forEach { catalog -> assertNull(catalog.validate(), "inválido: ${catalog.id}") }
    }

    /**
     * Cada catálogo declara si su serie sigue emitiendo (#28), y cerrar cuesta prueba: los dieciséis
     * cerrados llevan su nota y los diecinueve abiertos no afirman nada más que «N de N catalogadas».
     *
     * Gothic Horror ya no está: su único miembro, N#519925, trae `series: "Gothic Horror"` de
     * Numista, así que la lista no afirmaba nada que la familia no dijera ya.
     */
    @Test
    fun `every shipped catalog declares whether its series is still open`() {
        val closed = catalogs.filter { it.seriesStatus == SeriesStatus.Closed }
        assertEquals(16, closed.size)
        assertEquals(19, catalogs.count { it.seriesStatus == SeriesStatus.Open })
        closed.forEach { catalog ->
            assertTrue(
                catalog.closedNote?.isNotBlank() == true,
                "cerrado sin nota: ${catalog.id}",
            )
        }
        assertTrue(catalogs.none { it.id == "gothic-horror-uk-1oz" })
        // Las tres que llegaban a 2024 y 2025 y parecían muertas siguen vivas, medido fuera de
        // Numista: el plan de emisión del Banco de Rusia para 2026 trae Libro Rojo y Monumentos
        // arquitectónicos, y la decimotercera Tesla salió en 2026.
        listOf("red-data-book-russia", "architectural-monuments-russia-3-roubles", "nikola-tesla-serbia-1oz")
            .forEach { assertEquals(SeriesStatus.Open, find(it).seriesStatus) }
    }

    /**
     * Cerrar exige que la lista esté completa, y esta no lo estaba: el programa del Banco Estatal
     * de la URSS son **seis** monedas de 3 rublos de plata .900, dos por año entre 1989 y 1991, y
     * las dos de 1991 faltaban. Numista no las tiene en la serie 13245 ni les da familia —ninguna
     * de las dos declara `series`—, así que sin este catálogo salen huérfanas.
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
     * Las 52 Capitales, que cierran por aritmética: España tiene 50 provincias y el programa de
     * la FNMT les suma Ceuta y Melilla, así que la lista completa se puede contar sin fiarse de
     * que Numista haya terminado la serie. Las tres tandas son 12, 20 y 20.
     *
     * El acabado importa aquí más que en ningún otro catálogo: Numista no dice «proof» en el
     * título de ninguna de las 52 —lo dice la FNMT—, así que la finish inferida de la ficha es
     * `null` y sólo el ADR 0016 hace que la moneda del padre caiga en esta lámina.
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
        // Ceuta y Melilla son las dos que no son capital de provincia, y la de Madrid es la
        // única que el padre tiene.
        assertTrue(capitales.members.any { it.label == "Ceuta" })
        assertTrue(capitales.members.any { it.label == "Melilla" })
        assertEquals(45_425, capitales.members.first { it.label == "Madrid" }.numistaTypeId)
    }

    /**
     * Las dos gamas de lingote de la Royal Mint que Numista no agrupa: sus seis tipos declaran
     * `series: null`, así que sin catálogo salen huérfanas y con él afirman algo que la familia de
     * Numista no dice (#28). El límite lo pone la propia ceca, no una serie.
     *
     * La casilla es el año aunque el diseño no cambie, y ahí se separan las dos: St George reparte
     * un tipo por año y The Lion and the Eagle mete 2024 y 2025 en N#404024 —el mismo reverso de
     * Mercanti las dos veces—, así que ésta es un date run y aquélla un catálogo simple.
     */
    @Test
    fun `the two royal mint bullion ranges are one slot per year`() {
        val george = find("st-george-dragon-uk-1oz-bullion")
        assertEquals(SeriesStatus.Open, george.seriesStatus)
        assertEquals(Finish.Bullion, george.finish)
        assertEquals(1_000, george.weightMillioz)
        assertEquals(listOf(2024, 2025, 2026), george.members.map { it.year })
        assertEquals(listOf(421_643, 465_926, 577_892), george.members.map { it.numistaTypeId })

        val eagle = find("lion-eagle-uk-1oz-bullion")
        assertTrue(eagle.isDateRun)
        assertEquals(SeriesStatus.Open, eagle.seriesStatus)
        assertEquals(Finish.Bullion, eagle.finish)
        assertEquals(1_000, eagle.weightMillioz)
        assertEquals(listOf(2024, 2025, 2026), eagle.members.map { it.year })
        assertEquals(listOf(404_024, 404_024, 546_643), eagle.members.map { it.numistaTypeId })
    }

    /**
     * N#143754 empieza en 2018: la pieza Premium Uncirculated del cincuentenario de 2017 es una
     * emisión distinta y no abre esta tirada anual bullion. Como Numista no propone una serie,
     * la fuente es la página del tipo y cada fecha repite ese tipo sin fijar emisiones.
     */
    @Test
    fun `the silver krugerrand annual bullion date run starts in 2018`() {
        val krugerrand = find("south-africa-silver-krugerrand-1oz-bullion")
        assertEquals(2, krugerrand.schemaVersion)
        assertTrue(krugerrand.isDateRun)
        assertEquals(
            "Silver Krugerrand bullion anual desde 2018 (sin 2017 Premium Uncirculated)",
            krugerrand.family,
        )
        assertEquals(
            "Silver Krugerrand · Sudáfrica · 1 oz bullion anual desde 2018 " +
                "(excluye 2017 Premium Uncirculated)",
            krugerrand.name,
        )
        assertEquals("afrique_du_sud", krugerrand.issuerCode)
        assertEquals(1_000, krugerrand.weightMillioz)
        assertEquals(Finish.Bullion, krugerrand.finish)
        assertEquals(Metal.Silver, krugerrand.metal)
        assertEquals(SeriesStatus.Open, krugerrand.seriesStatus)
        assertNull(krugerrand.closedNote)
        assertEquals("https://en.numista.com/catalogue/pieces143754.html", krugerrand.source)
        assertEquals("2026-08-02", krugerrand.updatedAt)
        assertEquals((2018..2026).toList(), krugerrand.members.map { it.year })
        assertTrue(krugerrand.members.none { it.year == 2017 })
        assertEquals(List(9) { 143_754 }, krugerrand.members.map { it.numistaTypeId })
        assertTrue(krugerrand.members.all { it.numistaIssueIds.isEmpty() })
    }

    /**
     * Onza Libertad bullion: one open date run from the 1982 decree that replaced the Onza Troy.
     * Three type pages (N#14465 36 mm 1982-1995, N#17818 and N#13855 at 40 mm) mix proof and
     * later reverse/antiqued, so every slot is issue-qualified for the standard bullion row.
     * Die varieties of the same finish share a year; 2026 is not on Numista yet. Emission ids
     * read from the type pages in the browser (#92) — no `/issues` budget.
     */
    @Test
    fun `the mexican libertad is forty-four issue-qualified bullion years over three types`() {
        val libertad = find("mexico-libertad-1oz-bullion")
        assertEquals(2, libertad.schemaVersion)
        assertTrue(libertad.isDateRun)
        assertEquals("Onza Libertad bullion anual", libertad.family)
        assertEquals("mexique", libertad.issuerCode)
        assertEquals(1_000, libertad.weightMillioz)
        assertEquals(Finish.Bullion, libertad.finish)
        assertEquals(Metal.Silver, libertad.metal)
        assertEquals(SeriesStatus.Open, libertad.seriesStatus)
        assertNull(libertad.closedNote)
        assertEquals("https://en.numista.com/catalogue/pieces14465.html", libertad.source)
        assertEquals(44, libertad.members.size)
        assertEquals((1982..2025).toList(), libertad.members.map { it.year })
        assertEquals(
            List(14) { 14_465 } + List(4) { 17_818 } + List(26) { 13_855 },
            libertad.members.map { it.numistaTypeId },
        )
        assertTrue(libertad.members.all { it.numistaIssueIds.isNotEmpty() })
        // Father's 2024 bullion fills; the matching Proof of that year must not.
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
     * Onza Troy .925: the four-year closed run the Libertad replaced. Physical variant is
     * 33.625 g / 41.5 mm (1081 millioz), so it never shares a key with the Libertad. N#13333 is
     * 1949; N#13398 covers 1978-1980. Closed by Banxico's stated succession and the DOF decree
     * of 21/28 December 1981, not by Numista going quiet (#92).
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
     * American Silver Eagle bullion: one date run across N#1493 (Type 1, 1986-2021) and
     * N#298883 (Type 2, 2021-2026). 2021 keeps two slots because the reverse changed mid-year
     * and the market names them apart (#57). Both type pages mix proof and burnished, so every
     * slot is issue-qualified; Star Privy 2024 and Eagle Privy 2025 stay out as thematic
     * privies. Ids from `/types/1493/issues` and `/types/298883/issues`, two calls (#91).
     */
    @Test
    fun `the american silver eagle is forty-two issue-qualified bullion years over two types`() {
        val eagle = find("us-american-silver-eagle-1oz-bullion")
        assertEquals(2, eagle.schemaVersion)
        assertTrue(eagle.isDateRun)
        assertEquals("American Silver Eagle bullion anual", eagle.family)
        assertEquals("etats-unis", eagle.issuerCode)
        assertEquals(1_000, eagle.weightMillioz)
        assertEquals(Finish.Bullion, eagle.finish)
        assertEquals(Metal.Silver, eagle.metal)
        assertEquals(SeriesStatus.Open, eagle.seriesStatus)
        assertNull(eagle.closedNote)
        assertEquals("https://en.numista.com/catalogue/pieces1493.html", eagle.source)
        assertEquals(42, eagle.members.size)
        assertEquals(
            (1986..2021).toList() + (2021..2026).toList(),
            eagle.members.map { it.year },
        )
        val type1 = eagle.members.filter { it.numistaTypeId == 1_493 }
        val type2 = eagle.members.filter { it.numistaTypeId == 298_883 }
        assertEquals((1986..2021).toList(), type1.map { it.year })
        assertEquals((2021..2026).toList(), type2.map { it.year })
        assertEquals("Type 1", type1.single { it.year == 2021 }.label)
        assertEquals("Type 2", type2.single { it.year == 2021 }.label)
        assertTrue(eagle.members.all { it.numistaIssueIds.isNotEmpty() })
        // Standard bullion rows the father owns; the 2023 Proof must not be listed.
        assertTrue(64_283 in type1.single { it.year == 1987 }.numistaIssueIds)
        assertTrue(1_059_386 in type2.single { it.year == 2026 }.numistaIssueIds)
        assertTrue(eagle.members.none { 760_576 in it.numistaIssueIds })
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
        assertTrue(eagle.members.none { eagle.memberMatches(it, proof2023) })
        assertTrue(eagle.memberMatches(type2.single { it.year == 2026 }, bullion2026))
    }

    /**
     * The stars of the 100 pesetas, keyed on Numista issues because the year cannot tell them
     * apart: all six issues of N#1885 are dated 1966 and the star is a variety of the issue.
     * Ids read from `/types/1885/issues`, one call, and the comment of each one is the star it
     * carries.
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
     * The father's own closure project: he is missing 1965. Three Numista types make one date
     * run because all three weigh 10 g, so they share one variant key.
     *
     * N#10399 is the regression this pins. It is dated 1945 and was struck in 1947, and the
     * type's `min_year`/`max_year` say 1947, so v0.4.0 shipped a member for 1947 — but the issue
     * itself carries `year: 1945` with `gregorian_year: 1947`, and `recordedYear` prefers the
     * date on the coin. The member had to be 1945 or it could never be filled, and the plate
     * would have reported a coin in his hand as missing.
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
     * El primer catálogo al que Numista no le da ninguna serie: las cinco fichas traen
     * `series: null`, así que la lista no la propuso nadie dentro de Numista y el `source` es la
     * página de un tipo. Quien delimita es el Handboek van de Nederlandse munten 1795-2001, que
     * cierra los 10 gulden de Beatrix en cinco piezas correlativas —LSch. 1168 a 1172— y archiva
     * aparte los de Juliana, que además pesan 25 g y son otra variante física.
     *
     * Las cinco caben en un fichero con dos leyes distintas —.720 la de 1994 y .800 las otras
     * cuatro, todas a 15 g— porque por el ADR 0016 el catálogo es autoridad sobre la variante de
     * sus propios miembros.
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

    @Test
    fun `catalogs target their exact proposal variants`() {
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
        assertTrue(monuments.members.none { it.year == 2025 })
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

        assertEquals(listOf(rwanda.key()), derivation.proposals.map { it.key() })
        assertEquals(listOf(snake), derivation.itemsByKey[rwanda.key()])
        assertTrue(derivation.unclassified.isEmpty())
        assertEquals(1, buildCollectionCatalogAlbum(rwanda, listOf(snake)).ownedMembers())
    }

    /**
     * Los fuertes, que son veintidós años y **dos** tipos: el venezolano de 1876 abre el run tres
     * años antes del primer 5 bolívares porque es la misma moneda con otro nombre —la ley de 1871
     * llamó venezolano a la unidad y la del 31 de marzo de 1879 la renombró bolívar—, y por eso la
     * familia dice «fuertes», que es como la llama el coleccionista, y no la denominación de
     * veintiuna de sus veintidós casillas.
     *
     * Los dos ensayos de 1874 comparten los 25 g de plata .900 y **no** están: son patterns, así
     * que no abren hueco. Lo dice la `closed_note` para que la lista no parezca corta.
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
        // Las otras veintiuna siguen siendo un solo tipo, y sus etiquetas siguen siendo el año.
        val fuerte = fuertes.members.drop(1)
        assertEquals(21, fuerte.size)
        assertTrue(fuerte.all { it.numistaTypeId == 10_340 })
        assertTrue(fuerte.all { it.label == it.year.toString() })
        assertTrue(fuertes.closedNote!!.contains("352550"), fuertes.closedNote!!)
    }

    /**
     * Lunar Series III runs 2020-2031, not 2019-2030: the 2019 pig closes Lunar II, which this
     * repo already ships whole. Numista files the Royal Australian Mint's parallel lunar line
     * under the very same series, so the members are the Perth ones — .9999 and 40,9 mm against
     * RAM's .999 and 40 mm, and in 2020 RAM even calls the animal a rat where Perth says mouse.
     *
     * Seven issued members and five scheduled subjects: the official Perth zodiac guide maps
     * 2027-2031 to Goat through Pig, while Series III is the annual twelve-coin 2020-2031 cycle.
     * Those five subjects are announced slots, not claims that their individual designs or
     * specifications have already been released.
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
        assertEquals(7, lunar.members.count { !it.isAnnounced })
        assertEquals(5, lunar.members.count { it.isAnnounced })
        assertEquals(
            listOf(179_438, 235_118, 307_024, 342_221, 386_213, 441_816, 483_798) + List(5) { null },
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
            ) + List(5) { emptyList() },
            lunar.members.map { it.numistaIssueIds },
        )
        val announced = lunar.members.filter { it.isAnnounced }
        assertEquals(
            listOf(
                "Year of the Goat",
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
        assertEquals(7, album.issuedMembers())
        assertEquals(5, album.announcedMembers())
        val entries = plateEntries(lunar, album.ownedMembers())
        assertEquals("Progreso" to "0 / 7 emisiones", entries[0])
        assertEquals("Sin emitir" to "5 anunciadas", entries[1])
        // La línea de la Royal Australian Mint: misma serie en Numista, otra moneda.
        val royalAustralianMint =
            listOf(219_663, 266_550, 309_870, 355_589, 406_506, 444_584, 529_884)
        assertTrue(lunar.members.none { it.numistaTypeId in royalAustralianMint })
        // El cerdo de 2019 cierra Lunar II; esta empieza en el ratón de 2020.
        assertEquals(2019, find("lunar-ii-perth-1oz-bullion").members.last().year)
    }

    /**
     * La proof coloreada repite tres tipos de bullion entre 2021 y 2023. Son ediciones del mismo
     * tipo de Numista, separadas por emisión: sin esos ids una pieza proof llenaría la casilla de
     * bullion, o al revés. Las demás también los declaran para no confundirlas con otros acabados
     * presentes o futuros del mismo tipo.
     */
    @Test
    fun `lunar iii proof coloured is issue qualified from 2020 to 2026`() {
        val proofColoured = find("lunar-iii-perth-1oz-proof-coloured")
        assertEquals(1, proofColoured.schemaVersion)
        assertEquals("Lunar Series III", proofColoured.family)
        assertEquals(1_000, proofColoured.weightMillioz)
        assertEquals(Finish.ProofColoured, proofColoured.finish)
        assertEquals(Metal.Silver, proofColoured.metal)
        assertEquals(SeriesStatus.Open, proofColoured.seriesStatus)
        assertEquals((2020..2026).toList(), proofColoured.members.map { it.year })
        assertEquals(
            listOf(185_343, 235_118, 307_024, 342_221, 394_043, 576_294, 507_204),
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
        assertEquals(setOf(bullion.key(), proofColoured.key()), derivation.proposals.map { it.key() }.toSet())
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
     * Equilibrium es una serie de Numista que da **tres** colecciones y no una (#43): ocho onzas
     * de plata, cinco décimos de onza de oro y cinco onzas de oro. Se cura la de plata, que es la
     * que el coleccionista persigue, y el oro se queda fuera a propósito: además de fallar la
     * intención del #33, la onza de oro comparte hoy clave de variante con la de plata —los dos
     * pesan 31,1 g— hasta que el metal entre en la clave (#62).
     *
     * El emisor alterna por año entre Tokelau y Niue sin que el programa cambie de ceca: es un
     * acuerdo de respaldo legal de la Pressburg Mint, así que las ocho son una tirada anual.
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
        // Las diez de oro de la misma serie: décimo de onza y onza, ninguna es casilla de esta.
        val gold = listOf(
            307_242, 334_283, 356_002, 407_410, 477_905,
            309_842, 334_282, 356_003, 407_409, 477_904,
        )
        assertTrue(equilibrium.members.none { it.numistaTypeId in gold })
    }

    /**
     * Esta lámina es la emisión BU anual oficial de una onza, sin privy opcional. Las marcas que
     * forman parte de la emisión anual —P100, P20, P125 y 35th Anniversary— sí pertenecen a ella.
     * Quedan fuera privies opcionales, color, dorado, high relief, proof o piezas exclusivas de
     * estuche, y mules. El tipo y la issue se fijan juntos porque varias de esas variantes comparten
     * tipo o año; 2005 es su propia casilla anual y no una extensión de la de 2004.
     */
    @Test
    fun `the kookaburra catalog is the issue-qualified standard annual bullion run`() {
        val kookaburra = find("australian-kookaburra-perth-1oz")
        assertEquals("Australian Kookaburra · Perth Mint · 1 oz de plata bullion anual estándar", kookaburra.name)
        assertEquals(1_000, kookaburra.weightMillioz)
        assertEquals(Finish.Bullion, kookaburra.finish)
        assertEquals(Metal.Silver, kookaburra.metal)
        assertEquals((1990..2026).toList(), kookaburra.members.map { it.year })
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
        assertEquals(20_658, kookaburra.members.single { it.year == 2005 }.numistaTypeId)

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
     * El primer miembro anunciado del repo (#31): la colección son diez bestias nombradas de
     * antemano y el Seymour Panther salió en proof en 2022 y **sigue sin salir en bullion**, así
     * que la décima casilla no es un agujero de curación sino una moneda sin emitir.
     *
     * Comprobado en la serie 6888 de numista.com, que tiene nueve «5 Pounds · 2 oz Fine Silver»
     * —.9999 y ⌀38,61 mm— y ninguna es el Panther. Su 2 oz de 2022 es N#307800, que es plata
     * .999 de 40 mm: la proof, que es exactamente por qué el `design_type_id` no puede emparejar.
     */
    @Test
    fun `the tenth tudor beast is announced and never counted as missing`() {
        val tudor = find("tudor-beasts-uk-2oz-bullion")
        assertEquals(SeriesStatus.Open, tudor.seriesStatus)
        assertEquals(10, tudor.members.size)
        assertEquals(9, tudor.members.count { !it.isAnnounced })

        val panther = tudor.members.single { it.isAnnounced }
        assertEquals("seymour-panther", panther.id)
        assertNull(panther.numistaTypeId)
        // Se sabe cuál falta y no cuándo, así que no lleva año.
        assertNull(panther.year)
        assertEquals(307_800, panther.designTypeId)
        assertTrue(panther.source?.startsWith("https://") == true)
        assertTrue(panther.sourceNote?.isNotBlank() == true)

        // El padre tiene piezas proof de esta serie: si el diseño emparejara, una de ellas
        // rellenaría una casilla de bullion que no existe.
        val album = buildCollectionCatalogAlbum(
            tudor,
            listOf(CollectedItem(id = 1, quantity = 1, typeId = 307_800)),
        )
        assertEquals(0, album.ownedMembers())
        assertEquals(9, album.issuedMembers())
        assertEquals(1, album.announcedMembers())
        assertEquals(
            CollectionCatalogMemberStatus.NotYetIssued,
            album.members.last().status,
        )
        assertFalse(tudor.isEvidencedBy(listOf(CollectedItem(id = 1, quantity = 1, typeId = 307_800))))
    }

    /**
     * El dólar de plata canadiense son **dos** catálogos y no uno, y lo que los separa no es el
     * peso —los dos pesan 23,33 g y por tanto declaran los mismos 750 millioz— sino la familia,
     * que es la primera componente de la clave. Ninguno de los treinta y dos tipos trae `series`
     * en Numista, así que la familia la aporta el catálogo (ADR 0009) y sin ellos las tres piezas
     * de las dos colecciones salen huérfanas por «sin familia ni catálogo».
     *
     * Los dos cierran por un hecho externo y **distinto**: la .800 porque en 1968 la Royal
     * Canadian Mint pasó el dólar al níquel (N#3326, 15,62 g), y la .500 porque en 1992 pasó a
     * plata esterlina (N#23296, .925 y 25,175 g). Ese segundo cierre no es el del programa: la
     * propia RCM dice que el proof silver dollar se emite todos los años desde 1971 y que su
     * composición varía, así que lo que cierra es la variante, que es la unidad de catálogo (#43).
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

        // Veintiún años seguidos, uno por moneda, de 1971 a 1991.
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

    @Test
    fun `no two catalogs claim the same proposal variant key`() {
        val keys = catalogs.map { it.key() }
        assertEquals(keys.size, keys.distinct().size, "dos catálogos comparten clave de variante")
    }
}
