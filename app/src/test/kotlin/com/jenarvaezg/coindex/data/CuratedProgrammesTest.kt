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
        assertEquals(2, programmes.size)
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
}
