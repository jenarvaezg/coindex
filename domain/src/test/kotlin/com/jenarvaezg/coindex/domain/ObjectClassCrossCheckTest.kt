package com.jenarvaezg.coindex.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * El cruce vive en el suite y **nunca** en el validador de arranque, como el del metal (#40).
 *
 * #89 mató el estado de miembro para «se ve y no cuenta por no ser moneda»: ninguna pieza de las
 * dos colecciones está mal contada por no tenerlo, y el estado habría sido la primera casilla
 * poseíble que se queda fuera del divisor. Así que un ensayo que un curador meta en un catálogo es
 * un miembro de pleno derecho, y esto sólo avisa.
 *
 * Lo que caza es el intruso accidental: los dos ensayos de 1874 del venezolano salieron de la misma
 * enumeración por peso que pobló el catálogo en #55 —25 g de plata .900, el módulo exacto de las 22
 * casillas— y los descartó una persona leyendo.
 */
class ObjectClassCrossCheckTest {
    private fun catalog(vararg members: CollectionCatalogMember) = CollectionCatalog(
        schemaVersion = 2,
        id = "venezuela-fuertes",
        name = "Fuertes de Venezuela",
        issuerCode = "venezuela",
        family = "Fuertes de Venezuela",
        weightMillioz = 804,
        finish = null,
        metal = Metal.Silver,
        seriesStatus = SeriesStatus.Closed,
        closedNote = "La ley de 1871 llamó venezolano a la unidad.",
        source = "https://en.numista.com/catalogue/pieces10340.html",
        updatedAt = "2026-08-02",
        members = members.toList(),
    )

    private val fuerte = CollectionCatalogMember("1876", "1876", 1_876, 48_672)
    private val essai = CollectionCatalogMember("1874-essai", "1874", 1_874, 352_550)

    private val objectClasses = mapOf(
        48_672 to "Monedas circulantes normales",
        352_550 to "Monedas de ensayo",
    )

    @Test
    fun `an essai inside a catalog is reported with its type and its class`() {
        val deviations = objectClassDeviations(listOf(catalog(fuerte, essai)), objectClasses)

        val deviation = deviations.single()
        assertEquals(352_550, deviation.numistaTypeId)
        assertEquals("Monedas de ensayo", deviation.objectClass)
        // El mensaje tiene que bastar para ir a la ficha sin abrir el test.
        assertTrue(deviation.toString().contains("Numista 352550"), deviation.toString())
        assertTrue(deviation.toString().contains("Monedas de ensayo"), deviation.toString())
    }

    /**
     * La salida del curador, la misma que la del metal: una nota en prosa en la casilla. Aquí manda
     * el criterio del curador sobre la tabla de Numista, no al revés.
     */
    @Test
    fun `a member that declares the exception in prose is exempt`() {
        val declared = essai.copy(
            variantNote = "Es un ensayo y entra a propósito: comparte módulo con las 22 casillas " +
                "y el coleccionista lo persigue como una más.",
        )

        assertEquals(emptyList(), objectClassDeviations(listOf(catalog(fuerte, declared)), objectClasses))
    }

    /**
     * `Monedas de colección` no avisa: dos miembros de pleno derecho de Equilibrium la llevan
     * (N#356004 y N#477907), y para esa clase la propia tabla de Numista dice que depende del
     * alcance declarado — es decir, que decide el curador.
     */
    @Test
    fun `a collector coin is not reported`() {
        val equilibrium = CollectionCatalogMember("2024", "2024", 2_024, 477_907)

        assertEquals(
            emptyList(),
            objectClassDeviations(
                listOf(catalog(equilibrium)),
                mapOf(477_907 to "Monedas de colección"),
            ),
        )
    }

    /** Un anunciado o un `unlisted` no tienen tipo, así que la ficha no dice nada de ellos. */
    @Test
    fun `a member with no numista type is not reported`() {
        val announced = CollectionCatalogMember(
            id = "2027",
            label = "2027",
            status = MemberStatus.Announced,
            source = "https://www.perthmint.com/",
            sourceNote = "Perth anuncia la serie hasta 2031.",
        )

        assertEquals(emptyList(), objectClassDeviations(listOf(catalog(announced)), objectClasses))
    }

    /** Una ficha que nadie sembró no dice nada; quien convierte eso en fallo es `TypeCacheSeedTest`. */
    @Test
    fun `a type absent from the cache is not reported`() {
        assertEquals(emptyList(), objectClassDeviations(listOf(catalog(essai)), emptyMap()))
    }
}
