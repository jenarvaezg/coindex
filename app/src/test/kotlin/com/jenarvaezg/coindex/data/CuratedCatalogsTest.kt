package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.domain.CatalogSeeds
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.Finish
import kotlin.test.Test
import kotlin.test.assertEquals
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
        assertEquals(25, catalogs.size)
        catalogs.forEach { catalog -> assertNull(catalog.validate(), "inválido: ${catalog.id}") }
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

    @Test
    fun `no two catalogs claim the same proposal variant key`() {
        val keys = catalogs.map { it.key() }
        assertEquals(keys.size, keys.distinct().size, "dos catálogos comparten clave de variante")
    }
}
