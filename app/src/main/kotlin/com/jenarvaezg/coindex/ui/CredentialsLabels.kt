package com.jenarvaezg.coindex.ui

/**
 * Everything «Credenciales» and the sign-up form say. ADR 0026 §5 lets them explain at length
 * because they are rarely visited; in exchange, none of these explanations may appear on a
 * notebook screen.
 */

/**
 * The screen's name (#521, ADR 0026 §14): on the row at the foot of «Este teléfono», on the
 * valuation card's row when the key is to blame, on its masthead, and in the sync refusals that
 * send the collector here. Not repeated as a heading inside the screen.
 */
const val CREDENTIALS_LABEL: String = "Credenciales"

/**
 * The credential promise, shared by the sign-up form and «Credenciales» so the two wordings can't
 * drift (ADR 0026 §5). The rejected-sync sentence reads as what to expect on sign-up and as the
 * first thing to check here.
 */
const val CREDENTIALS_EXPLANATION: String =
    "Tu API key de Numista y tu identificador de usuario se guardan cifrados en este teléfono y " +
        "nunca salen de él. Si Numista rechaza las sincronizaciones, la API key es lo primero " +
        "que hay que revisar aquí."

/** Where to find the two values; only the sign-up form says it. */
const val ONBOARDING_CREDENTIALS_SOURCE: String =
    "La API key se obtiene en numista.com › Mi perfil › API. El identificador de usuario aparece " +
        "en la URL de tu perfil."

/** Numista's names for the two fields, the same on both screens. */
const val API_KEY_FIELD_LABEL: String = "API key de Numista"
const val USER_ID_FIELD_LABEL: String = "Identificador de usuario"

/** Shows the key to check a typo, or masks it again. */
fun apiKeyRevealLabel(revealed: Boolean): String =
    if (revealed) "Ocultar la API key" else "Mostrar la API key"

/** Just «Guardar»: the masthead already names the screen (#521). */
const val CREDENTIALS_SAVE_ACTION: String = "Guardar"

/** Names what was saved, not where: it is read after the screen has been left. */
const val CREDENTIALS_SAVED_MESSAGE: String = "Credenciales guardadas."

/**
 * Signing out: an explanation and one button, with no card title repeating the button
 * (ADR 0026 §5). On «Credenciales» rather than «Este teléfono» (#521), since it deletes these two
 * fields and is out of place as a destructive button on the sync screen.
 */
const val SIGN_OUT_ACTION: String = "Cerrar sesión"
const val SIGN_OUT_EXPLANATION: String =
    "Borra la API key y el identificador de este teléfono y vuelve al alta. Las piezas ya " +
        "sincronizadas se quedan donde están."

/** The name over the sign-up form, in the notebook's own capitals rather than the masthead's. */
const val ONBOARDING_TITLE: String = "Coindex"
const val ONBOARDING_EYEBROW: String = "Cuaderno de colección"
const val ONBOARDING_SAVE_ACTION: String = "Guardar y empezar"
