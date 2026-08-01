package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.domain.CatalogSeeds
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogMemberStatus
import com.jenarvaezg.coindex.domain.Finish
import com.jenarvaezg.coindex.domain.SeriesStatus
import com.jenarvaezg.coindex.domain.buildCollectionCatalogAlbum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        assertEquals(26, catalogs.size)
        catalogs.forEach { catalog -> assertNull(catalog.validate(), "inválido: ${catalog.id}") }
    }

    /**
     * Cada catálogo declara si su serie sigue emitiendo (#28), y cerrar cuesta prueba: los trece
     * cerrados llevan su nota y los trece abiertos no afirman nada más que «N de N catalogadas».
     *
     * Gothic Horror ya no está: su único miembro, N#519925, trae `series: "Gothic Horror"` de
     * Numista, así que la lista no afirmaba nada que la familia no dijera ya.
     */
    @Test
    fun `every shipped catalog declares whether its series is still open`() {
        val closed = catalogs.filter { it.seriesStatus == SeriesStatus.Closed }
        assertEquals(13, closed.size)
        assertEquals(13, catalogs.count { it.seriesStatus == SeriesStatus.Open })
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
                448_067, 493_347, 493_329,
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
    fun `the venezuela date run repeats one type across twenty one years`() {
        val bolivares = find("venezuela-5-bolivares")
        assertEquals(2, bolivares.schemaVersion)
        assertTrue(bolivares.isDateRun)
        assertEquals("5 Bolívares de Venezuela", bolivares.family)
        assertEquals(804, bolivares.weightMillioz)
        assertEquals(21, bolivares.members.size)
        assertTrue(bolivares.members.all { it.numistaTypeId == 10_340 })
        assertEquals(21, bolivares.members.map { it.year }.distinct().size)
    }

    /**
     * Lunar Series III runs 2020-2031, not 2019-2030: the 2019 pig closes Lunar II, which this
     * repo already ships whole. Numista files the Royal Australian Mint's parallel lunar line
     * under the very same series, so the members are the Perth ones — .9999 and 40,9 mm against
     * RAM's .999 and 40 mm, and in 2020 RAM even calls the animal a rat where Perth says mouse.
     *
     * Seven emitted members and no more: 2027-2031 are announced, and the schema cannot say so
     * until #46 lands.
     */
    @Test
    fun `lunar iii bullion is the perth line from 2020 to 2026`() {
        val lunar = find("lunar-iii-perth-1oz-bullion")
        assertEquals(1, lunar.schemaVersion)
        assertEquals("Lunar Series III", lunar.family)
        assertEquals(1_000, lunar.weightMillioz)
        assertEquals(Finish.Bullion, lunar.finish)
        assertEquals((2020..2026).toList(), lunar.members.map { it.year })
        assertEquals(
            listOf(179_438, 235_118, 307_024, 342_221, 386_213, 441_816, 483_798),
            lunar.members.map { it.numistaTypeId },
        )
        // La línea de la Royal Australian Mint: misma serie en Numista, otra moneda.
        val royalAustralianMint =
            listOf(219_663, 266_550, 309_870, 355_589, 406_506, 444_584, 529_884)
        assertTrue(lunar.members.none { it.numistaTypeId in royalAustralianMint })
        // El cerdo de 2019 cierra Lunar II; esta empieza en el ratón de 2020.
        assertEquals(2019, find("lunar-ii-perth-1oz-bullion").members.last().year)
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
     * La casilla de 2009 la ocupó durante días **N#426539**, un vigésimo de onza de oro del estuche
     * del vigésimo aniversario: la clave del catálogo manda sobre la variante de sus miembros
     * (ADR 0016), así que la lámina lo pintaba como la onza de plata de 2009 y nadie lo veía.
     *
     * La de verdad es **N#17382**, y lo que lo prueba no es el título —Numista lo llama «Silver
     * Set»— sino su reverso: «THE AUSTRALIAN KOOKABURRA · P20 · DB · 1 OZ 999 SILVER 2009», con las
     * iniciales de Darryl Bellotti, que es quien firmó el diseño propio de 2009, y la marca P20 del
     * aniversario. Las otras veinte del estuche llevan diseños de años anteriores re-acuñados.
     *
     * Y **2005 no es un hueco**: N#20658 es un tipo de 2004-2005, así que ese año vive en la casilla
     * de 2004 y la serie no tiene agujero.
     */
    @Test
    fun `the kookaburra 2009 slot is the p20 silver ounce and not the gold set`() {
        val kookaburra = find("australian-kookaburra-perth-1oz")
        assertEquals(1_000, kookaburra.weightMillioz)
        assertEquals(36, kookaburra.members.size)
        assertEquals(17_382, kookaburra.members.single { it.year == 2009 }.numistaTypeId)
        assertEquals(20_658, kookaburra.members.single { it.year == 2004 }.numistaTypeId)
        assertTrue(kookaburra.members.none { it.year == 2005 })
        // El estuche de oro de 2009, veinte vigésimos de onza: ninguno es casilla de esta lámina.
        val goldSet = listOf(
            426_539, 458_300, 458_463, 458_703, 458_905, 459_012, 459_137, 459_344, 459_536,
            460_076, 460_908, 461_129, 462_061, 462_390, 462_552, 462_805, 462_940, 463_069,
            463_187, 463_319,
        )
        assertTrue(kookaburra.members.none { it.numistaTypeId in goldSet })
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
        assertTrue(panther.announcedSource?.startsWith("https://") == true)
        assertTrue(panther.announcedNote?.isNotBlank() == true)

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

    @Test
    fun `no two catalogs claim the same proposal variant key`() {
        val keys = catalogs.map { it.key() }
        assertEquals(keys.size, keys.distinct().size, "dos catálogos comparten clave de variante")
    }
}
