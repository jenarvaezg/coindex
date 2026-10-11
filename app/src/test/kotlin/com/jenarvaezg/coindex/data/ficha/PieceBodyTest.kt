package com.jenarvaezg.coindex.data.ficha

import com.jenarvaezg.coindex.data.Fixtures
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * What one collected row says about its piece beyond the inventory's columns (#643). Rows are
 * written by hand in the shape of Numista's `collected_items`: the collector's are private.
 */
class PieceBodyTest {
    @Test
    fun `a bought piece gives up its date, its mint letter, its issue and its notes`() {
        val reading = readPieceBody(
            """
            {"id": 1, "quantity": 1, "type": {"id": 7},
             "issue": {"id": 3, "year": 1795, "mint_letter": "FM", "mintage": 1000000,
                       "comment": "variante de fecha"},
             "grade": "xf", "private_comment": "Del mercadillo",
             "price": {"value": 75, "currency": "EUR"}, "acquisition_date": "2026-03-14",
             "pictures": [{"url": "https://example.org/a.jpg",
                           "thumbnail_url": "https://example.org/a-360.jpg"}]}
            """,
        )

        assertEquals(
            PieceReading(
                mintLetter = "FM",
                acquiredOn = LocalDate.of(2026, 3, 14),
                mintage = 1_000_000L,
                issueComment = "variante de fecha",
                privateComment = "Del mercadillo",
                pictures = listOf(
                    CoinPhoto(
                        thumbnail = "https://example.org/a-360.jpg",
                        picture = "https://example.org/a.jpg",
                    ),
                ),
            ),
            reading,
        )
    }

    @Test
    fun `the documented example row reads its mint letter and mintage`() {
        val first = Json.parseToJsonElement(Fixtures.collectedItems)
            .jsonObject["items"]!!.jsonArray.first().toString()

        val reading = readPieceBody(first)

        assertEquals("A", reading.mintLetter)
        assertEquals(5_000_000L, reading.mintage)
    }

    @Test
    fun `blank notes and an empty picture list are no notes and no pictures`() {
        val reading = readPieceBody(
            """{"private_comment": "  ", "issue": {"comment": ""}, "pictures": []}""",
        )

        assertEquals(PieceReading(), reading)
    }

    @Test
    fun `a row that isn't json reads as nothing`() {
        assertEquals(PieceReading(), readPieceBody("no es json"))
        assertEquals(PieceReading(), readPieceBody("{}"))
    }
}
