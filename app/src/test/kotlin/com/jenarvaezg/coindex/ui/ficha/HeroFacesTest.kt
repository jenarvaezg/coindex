package com.jenarvaezg.coindex.ui.ficha

import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.domain.PrintedSide
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The 300 dp coin arrives on the casilla's face, unless that face has no photograph (#643). */
class HeroFacesTest {
    private val obverse = CoinPhoto(thumbnail = "https://example.org/a-180.jpg")
    private val reverse = CoinPhoto(thumbnail = "https://example.org/b-180.jpg")

    @Test
    fun `the face asked for is up and the other is behind it`() {
        val faces = heroFaces(TypeImages(obverse, reverse), PrintedSide.Obverse)

        assertEquals(HeroFaces(PrintedSide.Obverse, obverse, reverse), faces)
    }

    @Test
    fun `a face with no photograph gives way to the other, which then cannot turn`() {
        val faces = heroFaces(TypeImages(obverse = obverse), PrintedSide.Reverse)

        assertEquals(PrintedSide.Obverse, faces.upSide)
        assertEquals(obverse, faces.up)
        assertNull(faces.other)
    }

    @Test
    fun `a coin with one photograph shows it and does not turn`() {
        val faces = heroFaces(TypeImages(reverse = reverse), PrintedSide.Reverse)

        assertEquals(HeroFaces(PrintedSide.Reverse, reverse, null), faces)
    }
}
