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
 * Columns added after a ficha was cached are filled from the body the row already stores (#221):
 * a cached type is never fetched again, and this costs no API call.
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
     * The marker is the reading version, not a null column, so an unreadable body is not re-read on
     * every start.
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

    /** Rows are picked by version, so bumping [FICHA_READING] can fix a bad reading. */
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
