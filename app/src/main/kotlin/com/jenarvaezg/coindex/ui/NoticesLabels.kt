package com.jenarvaezg.coindex.ui

/**
 * Everything «Avisos y licencias» says. Rarely visited, so exempt from ADR 0026 §5 like
 * «Credenciales» (ADR 0026 §14). The licence texts ship as assets, outside
 * `CopyLivesInOnePlaceTest`.
 */

/**
 * The row at the foot of «Este teléfono» and the masthead of the screen it opens (ADR 0026 §14).
 * The screen has no eyebrow: the masthead already names it.
 */
const val NOTICES_LABEL: String = "Avisos y licencias"

/**
 * Attributions for the data, code and fonts Coindex didn't write. The Numista line and the N# on
 * every piece are conditions of the API licence (`/api/license.php`).
 */
val NOTICES_ATTRIBUTIONS: List<String> = listOf(
    "Fichas y fotografías: datos proporcionados por Numista (numista.com). Cada pieza lleva su N#.",
    "Software de terceros: Compose, AndroidX, Room, Ktor, OkHttp, Okio, Coil, ZXing y kotlinx — " +
        "Apache 2.0; slf4j-api — MIT.",
    "Tipografías: Bitter y Barlow Condensed — SIL Open Font License 1.1.",
)

/** The installed APK, shown only on «Avisos y licencias» (#410). */
fun installedVersionLabel(versionName: String): String =
    if (versionName.isEmpty()) "Coindex" else "Coindex · v$versionName"
