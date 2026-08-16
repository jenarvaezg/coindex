package com.jenarvaezg.coindex.ui

/**
 * Everything Ajustes, the sign-up form and the notices screen say.
 *
 * These are the three screens §5 exempts **by the frequency rule**: they are visited once, a
 * paragraph there costs once, and avoiding a phone call pays for it. The exemption is about how
 * much text is worth, not about where it is written — which is why the paragraphs live here, in a
 * file the tests can read, and not in the screens.
 *
 * In exchange they are watched the other way round: **none of these explanations may appear on a
 * notebook screen**. That is the clause a review checks with the app in front of it, and this file
 * is where it can be checked by reading.
 */

/** What the two fields are, as one word over them. */
const val SETTINGS_CREDENTIALS_HEADING: String = "Credenciales"

/**
 * The credential promise, said once and read in two places (ADR 0026 §5).
 *
 * There were two paragraphs saying this — 31 words on the sign-up form, 27 in Ajustes — with two
 * wordings of the same three facts: the two values are the collector's own, they are stored
 * encrypted, and they never leave the phone. They are never on screen together, so one string
 * saves no words on any single screen; what it saves is the pair **drifting apart**, which is what
 * happens to a promise about encryption written twice.
 *
 * The sentence about a rejected sync belongs to it: on the sign-up form nothing has synced yet, so
 * it reads as what to expect, and in Ajustes as the first thing to check — which is why Ajustes
 * exists at all (a mistyped key used to be a dead end curable only by clearing the app's data).
 */
const val CREDENTIALS_EXPLANATION: String =
    "Tu API key de Numista y tu identificador de usuario se guardan cifrados en este teléfono y " +
        "nunca salen de él. Si Numista rechaza las sincronizaciones, la API key es lo primero " +
        "que hay que revisar aquí."

/** Where the two values are found, which only the collector filling them in for the first time needs. */
const val ONBOARDING_CREDENTIALS_SOURCE: String =
    "La API key se obtiene en numista.com › Mi perfil › API. El identificador de usuario aparece " +
        "en la URL de tu perfil."

/**
 * The two fields, by the names Numista gives them.
 *
 * The same two labels on both screens, because they are the same two fields: naming them
 * differently in Ajustes than on the form that first asked for them would make the collector
 * wonder whether they are the same thing.
 */
const val API_KEY_FIELD_LABEL: String = "API key de Numista"
const val USER_ID_FIELD_LABEL: String = "Identificador de usuario"

/** Whether the key is being read back to check a typo, or masked as the promise above says. */
fun apiKeyRevealLabel(revealed: Boolean): String =
    if (revealed) "Ocultar la API key" else "Mostrar la API key"

const val SETTINGS_SAVE_ACTION: String = "Guardar ajustes"

/** Said once the credentials are stored. */
const val SETTINGS_SAVED_MESSAGE: String = "Ajustes guardados."

const val PHOTO_CACHE_HEADING: String = "Fotos del catálogo"
const val VALUATION_HEADING: String = "Precios de catálogo"

/**
 * Signing out, which is a card with one word and one button.
 *
 * The word was printed twice — as the card's title and on the button under it — and a title that
 * repeats the only control it contains is furniture (§5). The button keeps it, because that is
 * where it is read as a thing to press.
 */
const val SIGN_OUT_ACTION: String = "Cerrar sesión"
const val SIGN_OUT_EXPLANATION: String =
    "Borra la API key y el identificador de este teléfono y vuelve al alta. Las piezas ya " +
        "sincronizadas se quedan donde están."

/**
 * The raw base leaving the phone, which is a card with one line and one button (#548).
 *
 * The one word it has to say is **what the file is for**, because «Exportar datos» next to «Exportar
 * lámina» reads as the same gesture with a wider subject, and it is not: a lámina is made for a
 * person to look at and this is the base itself, made for a machine to load. The line names the two
 * things that make it safe to send — everything the phone holds, and not the API key.
 */
const val DATA_EXPORT_ACTION: String = "Exportar datos"
const val DATA_EXPORT_EXPLANATION: String =
    "Comparte una copia de la base de datos de este teléfono: la colección, las fichas y los " +
        "precios, en un solo fichero para cargar en otro sitio. La API key no va dentro."

/**
 * The same button while the copy is being written.
 *
 * The base is a few megabytes and the copy is quick, but nothing appears on screen until the chooser
 * opens, so the gap is long enough for a second tap — and two taps are two choosers. The button says
 * what it is doing rather than going quietly grey.
 */
fun dataExportLabel(exporting: Boolean): String =
    if (exporting) "Exportando…" else DATA_EXPORT_ACTION

/** The chooser's own title, which is the only word Android puts over the list of destinations. */
const val DATA_EXPORT_CHOOSER_TITLE: String = "Compartir los datos"

/** Said when the copy could not be written, with whatever the failure had to say for itself. */
fun dataExportFailure(cause: String?): String =
    if (cause.isNullOrBlank()) {
        "No se pudieron exportar los datos."
    } else {
        "No se pudieron exportar los datos: $cause"
    }

/**
 * The way into the notices, and the name of the screen it opens.
 *
 * One string for the row in Ajustes and for the masthead of what it opens (ADR 0026 §14: three
 * words in Ajustes, one screen with everything inside). The screen carries no eyebrow of its own —
 * the masthead already says where you are, and saying it twice on a screen of licence text is the
 * one place furniture is least welcome.
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

/** The name over the sign-up form, in the notebook's own capitals rather than the masthead's. */
const val ONBOARDING_TITLE: String = "Coindex"
const val ONBOARDING_EYEBROW: String = "Cuaderno de colección"
const val ONBOARDING_SAVE_ACTION: String = "Guardar y empezar"
