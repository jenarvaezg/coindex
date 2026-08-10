package com.jenarvaezg.coindex.data.photos

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * When the collector's data and battery may be spent on pictures nobody has asked for yet (#191).
 *
 * The whole index is some 1.600 photographs and around 22 MB. Fetched over wifi while the app is
 * open that is free and invisible; fetched over a mobile tariff it is the collector paying for a
 * plate they may never open. So the rule is arithmetic on four facts, kept apart from the Android
 * classes that read them, and it can be read here rather than inferred from a `if` in a coroutine.
 */
class PhotoPrefetchPolicyTest {
    @Test
    fun `on wifi with a rested phone the photographs are worth bringing`() {
        assertNull(prefetchRefusal(PrefetchConditions(unmeteredNetwork = true)))
    }

    @Test
    fun `a metered network is the collector's tariff, not ours to spend`() {
        assertEquals(
            PrefetchRefusal.MeteredNetwork,
            prefetchRefusal(PrefetchConditions(unmeteredNetwork = false)),
        )
    }

    @Test
    fun `power saving and a low battery are both a no`() {
        assertEquals(
            PrefetchRefusal.PowerSave,
            prefetchRefusal(PrefetchConditions(unmeteredNetwork = true, powerSaveMode = true)),
        )
        assertEquals(
            PrefetchRefusal.LowBattery,
            prefetchRefusal(PrefetchConditions(unmeteredNetwork = true, batteryLow = true)),
        )
    }

    @Test
    fun `a sync in flight owns the network, and it is spending API budget to do it`() {
        assertEquals(
            PrefetchRefusal.Syncing,
            prefetchRefusal(PrefetchConditions(unmeteredNetwork = true, syncing = true)),
        )
    }

    @Test
    fun `the sync is the first reason given, because it is the one that passes on its own`() {
        // Reported to the collector as one sentence, so which reason wins is not cosmetic: a sync
        // ends by itself in a minute, a mobile tariff needs them to walk into a wifi.
        assertEquals(
            PrefetchRefusal.Syncing,
            prefetchRefusal(
                PrefetchConditions(
                    unmeteredNetwork = false,
                    powerSaveMode = true,
                    batteryLow = true,
                    syncing = true,
                ),
            ),
        )
    }

    @Test
    fun `both faces of every type are wanted, because both faces are what a card draws`() {
        val images = mapOf(
            1 to TypeImages(
                obverse = CoinPhoto(thumbnail = "a-180.jpg", picture = "a-original.jpg"),
                reverse = CoinPhoto(thumbnail = "b-180.jpg", picture = "b-original.jpg"),
            ),
        )

        // The thumbnail alone: the original behind it is the fallback for a thumbnail that is
        // refused (ADR 0017), and warming both would double the traffic to pre-empt a failure
        // that mostly does not happen.
        assertEquals(listOf("a-180.jpg", "b-180.jpg"), photographsToPrefetch(images.values))
    }

    @Test
    fun `a type with no picture at all asks for nothing`() {
        val images = listOf(TypeImages(), TypeImages(obverse = CoinPhoto(picture = "only.jpg")))

        assertEquals(listOf("only.jpg"), photographsToPrefetch(images))
    }

    @Test
    fun `the same photograph on two types is asked for once`() {
        val shared = CoinPhoto(thumbnail = "shared-180.jpg")
        val images = listOf(TypeImages(obverse = shared), TypeImages(reverse = shared))

        assertEquals(listOf("shared-180.jpg"), photographsToPrefetch(images))
    }

    @Test
    fun `a photograph Numista says is gone is not asked for again on every launch`() {
        val images = listOf(
            TypeImages(
                obverse = CoinPhoto(thumbnail = "gone-180.jpg"),
                reverse = CoinPhoto(thumbnail = "here-180.jpg"),
            ),
        )

        assertEquals(
            listOf("here-180.jpg"),
            photographsToPrefetch(images, gone = setOf("gone-180.jpg")),
        )
    }

    @Test
    fun `a photograph that was gone a month ago is given another chance`() {
        val day = 24 * 60 * 60 * 1_000L
        val now = 400L * day
        val remembered = mapOf(
            "yesterday.jpg" to now - day,
            // A CDN having a bad minute would otherwise take this picture out of the catalog on
            // this phone for good, invisibly, and not even clearing the cache would bring it back.
            "last-year.jpg" to now - 365 * day,
        )

        assertEquals(setOf("yesterday.jpg"), stillGone(remembered, now))
    }

    @Test
    fun `a pass with nothing missing, or a reason not to fetch, is already settled`() {
        assertEquals(true, prefetchAlreadySettled(missingCount = 0, held = null))
        assertEquals(true, prefetchAlreadySettled(missingCount = 12, held = PrefetchRefusal.MeteredNetwork))
        assertEquals(false, prefetchAlreadySettled(missingCount = 12, held = null))
    }

    @Test
    fun `progress counts failures as still missing, and only speaks every twenty-five`() {
        assertEquals(7, prefetchMissingAfter(askedFor = 10, landed = 3))
        assertEquals(false, shouldReportPrefetchProgress(asked = 0))
        assertEquals(false, shouldReportPrefetchProgress(asked = 24))
        assertEquals(true, shouldReportPrefetchProgress(asked = 25))
        assertEquals(true, shouldReportPrefetchProgress(asked = 50))
    }

    @Test
    fun `the final count is rebuilt from what is still wanted, not from what landed`() {
        // The interceptor learnt one URL was gone during the pass: it leaves the wanted list,
        // and the cache check alone would still have counted it as missing.
        val status = photoCacheStatus(
            wanted = listOf("here.jpg", "also.jpg"),
            cached = { it == "here.jpg" },
            bytes = 1_024L,
            held = null,
        )

        assertEquals(PhotoCacheStatus(wanted = 2, missing = 1, bytes = 1_024L, held = null), status)
    }
}
