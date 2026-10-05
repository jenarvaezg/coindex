package com.jenarvaezg.coindex.ui.components

/**
 * Why a hole is not showing a coin (#510). Off wifi, [NotOnThisPhone] is the normal state of a
 * whole plate (ADR 0024 only prefetches on unmetered networks), so it must not look like
 * [Loading]. The dashed ring of a missing casilla is separate: it is about the collection, not the
 * photograph.
 */
enum class HoleSilence {
    /** Asked for and not answered yet. Seconds, and it ends in a coin or in [NotOnThisPhone]. */
    Loading,

    /**
     * Asked for and answered without a picture: no network, or a URL that is not coming back. The
     * only one the hole marks, since the collector can fix it by connecting to wifi.
     */
    NotOnThisPhone,

    /** Nothing to ask for: the catalogue holds no candidate for this face. */
    NoPhotograph,
}

/**
 * Which silence this is, from what the hole already knows. [settled] means the load reported back,
 * once per set of candidates after the last fallback failed. The reason a photograph didn't arrive
 * isn't consulted; Ajustes shows that.
 *
 * @param candidates how many URLs this face offers, which is zero for a face with no picture.
 * @return null when the coin is on the hole and there is no silence to explain.
 */
fun holeSilence(candidates: Int, settled: Boolean, painted: Boolean): HoleSilence? = when {
    painted -> null
    candidates == 0 -> HoleSilence.NoPhotograph
    settled -> HoleSilence.NotOnThisPhone
    else -> HoleSilence.Loading
}
