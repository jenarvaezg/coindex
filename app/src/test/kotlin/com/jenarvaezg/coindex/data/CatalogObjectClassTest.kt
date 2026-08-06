package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.domain.CatalogSeeds
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.ObjectClass
import com.jenarvaezg.coindex.domain.objectClassDeviations
import com.jenarvaezg.coindex.domain.objectClassOf
import com.jenarvaezg.coindex.domain.thingsThatAreNotMoney
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * La red de #89 sobre lo que de verdad se publica: los catálogos contra la caché sembrada.
 *
 * #89 decidió que **no hay estado de miembro** para lo que se muestra y no cuenta por no ser
 * moneda. Ninguna de las catorce piezas de clase «no moneda» de las dos colecciones está dentro de
 * un catálogo, la Semeuse de ensayo del padre es huérfana estable —no tiene detrás ni un solo
 * 5 francs normal— y los dos ensayos venezolanos ya viven en la `closed_note` de los fuertes. El
 * residuo útil de aquella discusión es esto: no un mecanismo que decida, sino un aviso.
 *
 * Rojo aquí no significa «saca la casilla». Significa «míralo»: si el ensayo entra a propósito, se
 * declara en prosa en su `variant_note` y el cruce la respeta.
 */
class CatalogObjectClassTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val catalogs: List<CollectionCatalog> = CatalogSeeds.parseAll(CatalogFiles.all())

    /** La clase de objeto por tipo, leída del mismo sitio del que la lee la app: la ficha entera. */
    private val objectClasses: Map<Int, String?> =
        json.parseToJsonElement(TypeCacheFile.read()).jsonObject.entries.associate { (id, ficha) ->
            id.toInt() to ficha.jsonObject["type"]?.jsonPrimitive?.contentOrNull
        }

    /** La categoría gruesa de Numista, que es la que lee la chip de clase de Monedas (ADR 0021 §1). */
    private val categories: Map<Int, String?> =
        json.parseToJsonElement(TypeCacheFile.read()).jsonObject.entries.associate { (id, ficha) ->
            id.toInt() to ficha.jsonObject["category"]?.jsonPrimitive?.contentOrNull
        }

    @Test
    fun `no shipped member is a struck thing that is not money`() {
        assertEquals(emptyList(), objectClassDeviations(catalogs, objectClasses))
    }

    /**
     * Por qué las medallas son **filtro y no sección** (ADR 0021 §1).
     *
     * Veintiuna de las 27 exonumia de la caché sembrada son miembros de catálogos curados. Una
     * sección «Medallas» tendría que arrancarlas de su lámina, y este test es la razón medida de que
     * no exista: siguen siendo miembros de pleno derecho, y lo único que la clase hace con ellas es
     * dejar que el coleccionista las encuentre.
     *
     * El reparto se movió del 4 sobre 13 con que el ADR se escribió, y se movió a favor de §1: los
     * diecisiete ECU y euros de la FNMT de #258 son «Monedas de fantasía» para Numista —el ECU fue
     * una divisa de cuenta y España nunca dio curso legal a estas piezas— y son las dos láminas que
     * el padre persigue. La mayoría de la exonumia sembrada vive ya dentro de una lámina, así que
     * la sección no arrancaría cuatro casillas sino veintiuna.
     *
     * Rojo aquí no significa «saca la casilla». Significa que el reparto cambió y hay que releer §1.
     */
    @Test
    fun `most curated exonumia is why the class is a chip and not a section`() {
        val curatedTypeIds = catalogs
            .flatMap { catalog -> catalog.members.mapNotNull { it.numistaTypeId } }
            .toSet()
        val exonumia = categories
            .filterValues { objectClassOf(it) == ObjectClass.Exonumia }
            .keys

        assertEquals(27, exonumia.size)
        assertEquals(
            setOf(
                13_333, 13_398, 18_156, 18_157, 18_159, 18_160, 18_161, 18_371, 18_520, 19_125,
                19_406, 19_619, 28_059, 44_260, 76_763, 77_339, 78_201, 85_583, 93_010, 477_907,
                485_082,
            ),
            exonumia intersect curatedTypeIds,
        )
    }

    /**
     * La red compara cadenas literales en español, porque en español está la caché: la app y
     * `scripts/seed-type-cache.py` piden las dos `lang=es`. Una red así puede pudrirse hasta ser un
     * test que nunca falla si Numista cambia el idioma o la redacción, y eso no se notaría — sería
     * verde igual. Así que se fija el vocabulario contra los datos: las cinco clases están hoy en
     * la caché —el ensayo es la Semeuse del padre, las fantasías son sobre todo los ECU y euros de
     * la FNMT, y las dos de medallas y el medallón una ficha cada uno—, así que si alguna dejara de
     * aparecer hay que
     * mirar por qué antes de fiarse del cruce de arriba.
     */
    @Test
    fun `the seeded cache still speaks the vocabulary this net reads`() {
        val present = objectClasses.values.filterNotNull().toSet()

        assertEquals(
            setOf(
                "Monedas de ensayo",
                "Monedas de fantasía",
                "Medallas",
                "Medallas conmemorativas",
                "Medallones de colección",
            ),
            thingsThatAreNotMoney(),
        )
        assertEquals(emptySet(), thingsThatAreNotMoney() - present)
    }
}
