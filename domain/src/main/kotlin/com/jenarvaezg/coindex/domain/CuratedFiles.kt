package com.jenarvaezg.coindex.domain

/**
 * One of the three curated species that ship with the app, and its directory. `data/` is an asset
 * source directory of the APK (`app/build.gradle.kts`), so the phone's `collection-catalogs` and
 * the suite's `../data/collection-catalogs` are the same bytes. The orphans register (#133) is
 * editorial and read only by the suite ([OrphanSeeds]).
 */
enum class CuratedSpecies(val directory: String) {
    Catalogs("collection-catalogs"),
    Groupings("groupings"),
    Programmes("programmes"),
}

/**
 * Where the curated files are read from: the APK's assets on a phone, `data/` in the suite (#545).
 * One port, so both sides go through [Curation.load] and every cross-species invariant it enforces.
 */
interface CuratedFiles {
    /**
     * Every curated file of one species, as `file name to contents`, in whatever order the
     * storage lists them: [Curation.load] is what puts them in one.
     */
    fun read(species: CuratedSpecies): List<Pair<String, String>>
}
