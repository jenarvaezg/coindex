package com.jenarvaezg.coindex.data.photos

/**
 * The app's own name, version and address, sent as `User-Agent` with every photograph request.
 * Without one Cloudflare answers `403` to every photograph, so it is set explicitly rather than
 * left to the network engine.
 */
fun coinPhotoUserAgent(versionName: String): String {
    val version = versionName.ifBlank { "dev" }
    return "Coindex/$version (+https://github.com/jenarvaezg/coindex)"
}
