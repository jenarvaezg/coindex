package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.domain.CatalogSeeds
import com.jenarvaezg.coindex.domain.CommemorativeProgramme
import com.jenarvaezg.coindex.domain.ProgrammeSeeds
import com.jenarvaezg.coindex.domain.programmeStandings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Los programas conmemorativos que se publican (ADR 0022).
 *
 * Salieron del criterio del coleccionista al curar el #157: la lista principal es por
 * denominación y metal, y el programa es la lectura de al lado. Lo que fija este test es que la
 * lectura de al lado **no** se convierta en una tarjeta ni en un denominador de lámina.
 */
class CuratedProgrammesTest {
    private val programmes: List<CommemorativeProgramme> =
        ProgrammeSeeds.parseAll(ProgrammeFiles.all())

    private fun find(id: String) = programmes.first { it.id == id }

    @Test
    fun `every shipped programme parses and validates`() {
        assertTrue(programmes.isNotEmpty())
        programmes.forEach { programme ->
            assertNull(programme.validate(), "inválido: ${programme.id}")
            // El límite nunca es un hecho de Numista, así que la prosa es obligatoria y el URL
            // puede ser de cualquier host.
            assertTrue(programme.sourceNote.isNotBlank(), programme.id)
            assertTrue(programme.source.startsWith("https://"), programme.id)
        }
    }

    /**
     * Las dos series de tres denominaciones que la INCM acuñó para una sola conmemoración, y que
     * se venden como carteira de tres. Su tercera moneda —el 25 escudos— no está en ningún
     * catálogo, que es exactamente por lo que el programa es un fichero y no un campo del miembro.
     */
    @Test
    fun `both portuguese programmes are three denominations of one commemoration`() {
        val herculano = find("portugal-1977-alexandre-herculano")
        assertEquals(1977, herculano.year)
        assertEquals(
            listOf(6_071, 10_126, 7_338),
            herculano.members.map { it.numistaTypeId },
        )
        assertEquals(
            listOf("2,50 escudos", "5 escudos", "25 escudos"),
            herculano.members.map { it.label },
        )

        val fao = find("portugal-1983-dia-mundial-alimentacion")
        assertEquals(1983, fao.year)
        assertEquals(listOf(9_829, 9_830, 9_831), fao.members.map { it.numistaTypeId })

        // El 25 escudos de cada programa no lo reclama ningún catálogo: nadie tiene uno, así que
        // curar los diez 25 escudos de cuproníquel habría sido un fichero sin tarjeta ni lámina.
        val catalogued = CatalogSeeds.parseAll(CatalogFiles.all())
            .flatMap { catalog -> catalog.members.mapNotNull { it.numistaTypeId } }
            .toSet()
        assertTrue(7_338 !in catalogued)
        assertTrue(9_831 !in catalogued)
    }

    /**
     * El conteo cruzado que pidió el coleccionista, medido sobre los ficheros que se publican: el
     * 2,50 de 1977 está en su lista por denominación **y** en el programa de 1977, y el programa
     * cuenta sobre tres aunque el catálogo sólo sostenga dos de ellas.
     */
    @Test
    fun `a cupronickel catalog carries its programme, counted over the whole programme`() {
        val catalogs = CatalogSeeds.parseAll(CatalogFiles.all())
        val dosCincuenta = catalogs.first { it.id == "portugal-2-50-escudos-cuproniquel" }
        val standings = programmeStandings(dosCincuenta, programmes, emptyList())
        assertEquals(
            listOf("Serie Alexandre Herculano 1977", "Serie FAO 1983"),
            standings.map { it.programme.shortName },
        )
        assertEquals(listOf(3, 3), standings.map { it.progress.total })

        // Y una lámina de plata no arrastra ningún programa: no comparte ningún tipo con ellos.
        val cincuenta = catalogs.first { it.id == "portugal-50-escudos-plata-650" }
        assertEquals(emptyList(), programmeStandings(cincuenta, programmes, emptyList()))
    }

    /**
     * El primer programa multinacional (#387): una casilla por país sobre las catorce monedas que
     * la FNMT coordinó para la I Serie Iberoamericana, y la única de las catorce que un catálogo
     * reclama es la portuguesa, que es justamente la que hace aparecer la lectura en pantalla.
     *
     * Lo que fija este test es la forma de la lista: catorce miembros y ningún país repetido, para
     * que un desdoble de acabado —la proof portuguesa de .925, la leyenda vertical peruana— no se
     * cuele como una casilla más y el denominador deje de contar países.
     */
    @Test
    fun `the ibero-american programme is one slot per country over fourteen`() {
        val serie = find("serie-iberoamericana-i-encuentro-de-dos-mundos")
        assertEquals(1991, serie.year)
        assertEquals(14, serie.members.size)
        assertEquals(
            listOf(
                27_800, 30_224, 30_273, 27_182, 22_028, 31_590, 31_592,
                31_593, 26_293, 31_923, 28_339, 15_463, 31_924, 31_925,
            ),
            serie.members.map { it.numistaTypeId },
        )
        val countries = serie.members.map { it.label.substringBefore(" ·") }
        assertEquals(countries.distinct(), countries)

        // Las fichas que quedan fuera a propósito, escritas en `source_note`: la proof portuguesa
        // de .925, la variante vertical peruana y la prueba brasileña.
        val members = serie.members.map { it.numistaTypeId }.toSet()
        assertTrue(25_337 !in members)
        assertTrue(67_304 !in members)
        assertTrue(596_861 !in members)

        // La lectura sale en la lámina de los 1000 escudos, que es el único catálogo que comparte
        // un tipo con el programa: la casilla de 1992 del Encontro de Dois Mundos.
        val catalogs = CatalogSeeds.parseAll(CatalogFiles.all())
        val touched = catalogs.filter { catalog ->
            programmeStandings(catalog, listOf(serie), emptyList()).isNotEmpty()
        }
        assertEquals(listOf("portugal-1000-escudos-plata-500"), touched.map { it.id })
    }

    /**
     * Las trece series enteras (#387), que es lo que el coleccionista pidió después de la primera.
     *
     * Tres cosas fija este test, y las tres son la forma editorial que las notas declaran: una
     * casilla por país en todas, la medalla de la FNMT donde la FNMT define así la colección, y
     * ningún tipo repetido entre series —una moneda pertenece a una sola serie, y la reaparición de
     * un tipo sería una casilla mal atribuida—.
     */
    @Test
    fun `the thirteen ibero-american series are one file each, with no type in two of them`() {
        val serie = programmes.filter { it.shortName.startsWith("Serie Iberoamericana ") }
        assertEquals(13, serie.size)
        assertEquals(
            setOf(
                "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII", "XIII",
            ),
            serie.map { it.shortName.removePrefix("Serie Iberoamericana ") }.toSet(),
        )

        // Un país, una casilla, en las trece.
        serie.forEach { programme ->
            val slots = programme.members.map { it.label.substringBefore(" ·") }
            assertEquals(slots.distinct(), slots, programme.id)
        }

        // Ningún tipo en dos series.
        val types = serie.flatMap { it.members.map { member -> member.numistaTypeId } }
        assertEquals(types.distinct().size, types.size)

        // La cabecera es la ceca que coordinó, no el emisor de los miembros (ADR 0022 enmendado).
        assertTrue(serie.all { it.issuerCode == "espagne" })

        // Sólo las cuatro primeras tocan una lámina, porque la moneda portuguesa de esas series es
        // un 1000 escudos de plata .500 y las de la V en adelante son euros que no cura nadie.
        val plata500 = CatalogSeeds.parseAll(CatalogFiles.all())
            .first { it.id == "portugal-1000-escudos-plata-500" }
        assertEquals(
            listOf(
                "Serie Iberoamericana I",
                "Serie Iberoamericana II",
                "Serie Iberoamericana III",
                "Serie Iberoamericana IV",
            ),
            programmeStandings(plata500, programmes, emptyList())
                .map { it.programme.shortName }
                .sorted(),
        )
    }
}
