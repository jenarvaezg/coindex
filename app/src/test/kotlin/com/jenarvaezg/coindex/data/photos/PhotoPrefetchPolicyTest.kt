package com.jenarvaezg.coindex.data.photos

import com.jenarvaezg.coindex.data.CoinPhoto
import com.jenarvaezg.coindex.data.TypeImages
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
    fun `a 404 is the picture being gone, and a throttle never is`() {
        assertEquals(true, PhotoRetryPolicy.isGone(404))
        assertEquals(true, PhotoRetryPolicy.isGone(410))
        // 403 is deliberately not remembered: without a User-Agent Cloudflare answers it to every
        // photograph (ADR 0017), so a bad afternoon at the edge would switch the catalog off for
        // good on this phone. It is not retried either — it is simply asked again another day.
        listOf(403, 429, 500, 503).forEach { status ->
            assertEquals(false, PhotoRetryPolicy.isGone(status), "$status no es una foto perdida")
        }
    }
}
