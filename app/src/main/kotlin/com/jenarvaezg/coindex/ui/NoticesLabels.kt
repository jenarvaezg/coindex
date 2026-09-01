package com.jenarvaezg.coindex.ui

/**
 * Everything «Avisos y licencias» says.
 *
 * The third screen §5 exempts **by the frequency rule** (ADR 0026 §14): three words at the foot of
 * «Este teléfono», one screen with everything inside, and the licence texts themselves shipped as
 * assets rather than literals so they stay out of `CopyLivesInOnePlaceTest`.
 */

/**
 * The way into the notices, and the name of the screen it opens.
 *
 * One string for the row and for the masthead of what it opens (ADR 0026 §14: three words at the
 * foot, one screen with everything inside). The screen carries no eyebrow of its own — the masthead
 * already says where you are, and saying it twice on a screen of licence text is the one place
 * furniture is least welcome.
 */
const val NOTICES_LABEL: String = "Avisos y licencias"

/**
 * What Coindex owes for the fichas, the code and the type it did not write.
 *
 * The Numista line is the licence talking (`/api/license.php`): the attribution and the N# on every
 * piece are the condition for holding the data at all, so this paragraph is not courtesy.
 */
val NOTICES_ATTRIBUTIONS: List<String> = listOf(
    "Fichas y fotografías: datos proporcionados por Numista (numista.com). Cada pieza lleva su N#.",
    "Software de terceros: Compose, AndroidX, Room, Ktor, OkHttp, Okio, Coil, ZXing y kotlinx — " +
        "Apache 2.0; slf4j-api — MIT.",
    "Tipografías: Bitter y Barlow Condensed — SIL Open Font License 1.1.",
)

/**
 * The installed APK, named once on Avisos y licencias (#410).
 *
 * The masthead used to carry `· v…` on every interior screen so a sideloaded build could be
 * identified from a screenshot. That made the version permanent furniture; here it sits with the
 * licences, which is where an APK version is read when it is needed.
 */
fun installedVersionLabel(versionName: String): String =
    if (versionName.isEmpty()) "Coindex" else "Coindex · v$versionName"
