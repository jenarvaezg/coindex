package com.jenarvaezg.coindex.data.photos

/**
 * The app's own name, version and address, for the log of a catalogue that owes us nothing.
 *
 * Without any `User-Agent` Cloudflare answers `403` to every photograph, and until now the
 * header was whatever OkHttp writes underneath Coil: the pictures worked by inertia rather than
 * by decision, and a change of network engine would have turned all of them off at once.
 */
fun coinPhotoUserAgent(versionName: String): String {
    val version = versionName.ifBlank { "dev" }
    return "Coindex/$version (+https://github.com/jenarvaezg/coindex)"
}
