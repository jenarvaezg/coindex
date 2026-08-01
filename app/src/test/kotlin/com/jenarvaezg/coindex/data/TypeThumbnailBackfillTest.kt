package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.TypeMetaEntity
import com.jenarvaezg.coindex.data.seed.TypeThumbnailBackfill
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

/**
 * A cached type is never requested again — that is what makes the cache worth having — so the
 * two phones that already hold the collection would have gone on asking for the heavy originals
 * for ever, which is the plate with holes in it (issue #67).
 *
 * Nothing is re-fetched to fix that: `raw` keeps the whole ficha as Numista sent it, thumbnails
 * included. This is exactly the case the column was written for.
 */
class TypeThumbnailBackfillTest {
    private fun ficha(typeId: Int, obverse: String?, reverse: String?): String {
        fun side(url: String?) = url?.let {
            """{"picture": "$it-original.jpg", "thumbnail": "$it-180.jpg"}"""
        } ?: "null"
        return """{"id": $typeId, "obverse": ${side(obverse)}, "reverse": ${side(reverse)}}"""
    }

    private fun row(typeId: Int, raw: String, thumbnail: String? = null) = TypeMetaEntity(
        typeId = typeId,
        title = "tipo $typeId",
        family = null,
        issuerCode = null,
        minYear = null,
        maxYear = null,
        weightGrams = null,
        obverseUrl = null,
        reverseUrl = null,
        raw = raw,
        fetchedAt = 0,
        obverseThumbnailUrl = thumbnail,
        reverseThumbnailUrl = thumbnail,
    )

    @Test
    fun `a cache seeded before version 3 gets its thumbnails from the fichas it already stores`() =
        runTest {
            val dao = FakeTypeMetaDao()
            dao.insertIfAbsent(listOf(row(10_207, ficha(10_207, "a", "b"))))

            assertEquals(1, TypeThumbnailBackfill(dao).run())

            val filled = dao.rows.value.single()
            assertEquals("a-180.jpg", filled.obverseThumbnailUrl)
            assertEquals("b-180.jpg", filled.reverseThumbnailUrl)
        }

    @Test
    fun `a row that already has its thumbnail is left alone`() = runTest {
        val dao = FakeTypeMetaDao()
        dao.insertIfAbsent(listOf(row(10_207, ficha(10_207, "a", "b"), thumbnail = "ya-180.jpg")))

        assertEquals(0, TypeThumbnailBackfill(dao).run())
        assertEquals("ya-180.jpg", dao.rows.value.single().obverseThumbnailUrl)
    }

    @Test
    fun `a ficha with only one face fills that one in and leaves the other empty`() = runTest {
        val dao = FakeTypeMetaDao()
        dao.insertIfAbsent(listOf(row(1, ficha(1, "a", null))))

        assertEquals(1, TypeThumbnailBackfill(dao).run())

        val filled = dao.rows.value.single()
        assertEquals("a-180.jpg", filled.obverseThumbnailUrl)
        assertNull(filled.reverseThumbnailUrl)
    }

    /**
     * The pass has to end. A row read, written back with a null face and read again the next
     * start would reintroduce, for ever, the very scan this is written to stop paying.
     */
    @Test
    fun `a face left empty is not read again on the next start`() = runTest {
        val dao = FakeTypeMetaDao()
        dao.insertIfAbsent(
            listOf(row(1, ficha(1, null, "b")), row(2, ficha(2, "a", null))),
        )

        assertEquals(2, TypeThumbnailBackfill(dao).run())
        assertEquals(0, TypeThumbnailBackfill(dao).run())
    }

    /** A ficha this version cannot read is a row to leave as it is, never a crash at start-up. */
    @Test
    fun `an unreadable ficha is skipped without taking the rest with it`() = runTest {
        val dao = FakeTypeMetaDao()
        dao.insertIfAbsent(
            listOf(
                row(1, "no es json"),
                row(2, ficha(2, "b", "c")),
                row(3, """{"id": 3}"""),
            ),
        )

        assertEquals(1, TypeThumbnailBackfill(dao).run())
        assertNull(dao.rows.value.first { it.typeId == 1 }.obverseThumbnailUrl)
        assertEquals("b-180.jpg", dao.rows.value.first { it.typeId == 2 }.obverseThumbnailUrl)
        assertNull(dao.rows.value.first { it.typeId == 3 }.obverseThumbnailUrl)
    }
}
