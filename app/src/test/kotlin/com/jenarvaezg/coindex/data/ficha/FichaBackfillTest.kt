package com.jenarvaezg.coindex.data.ficha

import com.jenarvaezg.coindex.data.FakeTypeMetaDao
import com.jenarvaezg.coindex.data.Fixtures
import com.jenarvaezg.coindex.data.db.TypeMetaEntity
import com.jenarvaezg.coindex.data.toDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

/**
 * The five columns of version 6 reach the fichas that were cached before they existed (#221).
 *
 * A cached type is never fetched again, so without this pass the two phones that already have the
 * app would show no issuer name on any card, no metal on any chip and no QR in the notebook — and
 * the fix costs no API call, because the body each row already stores is where those five were
 * being read from until now.
 */
class FichaBackfillTest {
    /** A row exactly as an APK older than version 6 left it: a body, and no reading of it. */
    private fun cachedBeforeVersion6(typeId: Int, raw: String) = TypeMetaEntity(
        typeId = typeId,
        title = null,
        family = null,
        issuerCode = "australie",
        minYear = null,
        maxYear = null,
        weightGrams = null,
        obverseUrl = null,
        reverseUrl = null,
        raw = raw,
        fetchedAt = 0,
    )

    @Test
    fun `a ficha cached before the columns existed is read from its own body`() = runTest {
        val types = FakeTypeMetaDao()
        types.insertIfAbsent(cachedBeforeVersion6(404_044, Fixtures.type(404_044)))

        assertEquals(1, FichaBackfill(types).run())

        val meta = types.rows.value.single().toDomain()
        assertEquals("Australia", meta.issuerName)
        assertEquals("coin", meta.category)
        assertEquals(32.6, meta.sizeMillimetres)
        assertEquals("https://es.numista.com/404044", meta.numistaUrl)
    }

    @Test
    fun `a second pass has nothing to do`() = runTest {
        val types = FakeTypeMetaDao()
        types.insertIfAbsent(cachedBeforeVersion6(404_044, Fixtures.type(404_044)))
        FichaBackfill(types).run()

        assertEquals(0, FichaBackfill(types).run())
    }

    /**
     * A body nobody can parse is read **once**. This is the difference from the thumbnail backfill,
     * whose marker is «is the column null»: there, a ficha with nothing to write is re-read at
     * every single start for ever.
     */
    @Test
    fun `a ficha with nothing to say is still marked as read`() = runTest {
        val types = FakeTypeMetaDao()
        types.insertIfAbsent(cachedBeforeVersion6(1_885, "no es json"))

        assertEquals(1, FichaBackfill(types).run())

        val row = types.rows.value.single()
        assertEquals(FICHA_READING, row.readVersion)
        assertNull(row.issuerName)
        assertEquals(0, FichaBackfill(types).run())
    }

    /**
     * The row a newer reading has not seen is the one that gets read, and it is found by version
     * rather than by an empty column: that is what lets [FICHA_READING] be bumped to fix a bad
     * reading over rows that already have values in them.
     */
    @Test
    fun `only the rows an older reading wrote are read again`() = runTest {
        val types = FakeTypeMetaDao()
        types.insertIfAbsent(
            cachedBeforeVersion6(404_044, Fixtures.type(404_044))
                .copy(issuerName = "lo que dijo una lectura vieja", readVersion = FICHA_READING),
        )
        types.insertIfAbsent(cachedBeforeVersion6(1_885, Fixtures.type(404_044)))

        assertEquals(1, FichaBackfill(types).run())

        assertEquals(
            "lo que dijo una lectura vieja",
            types.rows.value.first { it.typeId == 404_044 }.issuerName,
        )
        assertEquals("Australia", types.rows.value.first { it.typeId == 1_885 }.issuerName)
    }
}
