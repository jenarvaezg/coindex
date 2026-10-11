package com.jenarvaezg.coindex.data.ficha

import com.jenarvaezg.coindex.data.Fixtures
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
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
            {"id": 1, "quantity": 1, "type": {"id": 18852},
             "issue": {"id": 96367, "year": 1805, "mint_letter": "TH", "mintage": 800000,
                       "comment": "normal date; overdate variety exists"},
             "grade": "xf", "private_comment": "Del mercadillo",
             "price": {"value": 140, "currency": "EUR"}, "acquisition_date": "2026-07-22",
             "pictures": [{"url": "https://example.org/a.jpg",
                           "thumbnail_url": "https://example.org/a-360.jpg"}]}
            """,
        )

        assertEquals(
            PieceReading(
                mintLetter = "TH",
                acquiredOn = "2026-07-22",
                mintage = 800_000L,
                issueComment = "normal date; overdate variety exists",
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
