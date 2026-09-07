package com.jenarvaezg.coindex.domain

/**
 * One of the three curated species that ship with the app, and the directory it lives in.
 *
 * The same three names on both sides of the seam: `data/` is an asset source directory of the APK
 * (`app/build.gradle.kts`), so what a phone opens under `collection-catalogs` and what the suite
 * reads from `../data/collection-catalogs` are the same bytes under the same name.
 *
 * The orphans register (#133) is not here: it is editorial, it never reaches a card, and it is
 * read by the suite alone ([OrphanSeeds]).
 */
enum class CuratedSpecies(val directory: String) {
    Catalogs("collection-catalogs"),
    Groupings("groupings"),
    Programmes("programmes"),
}

/**
 * Where the curated files are read from: the APK's assets on a phone, `data/` in the suite (#545).
 *
 * One port with one verb, so that both sides reach [Curation.load] and therefore every invariant it
 * enforces. Before it there was a loader per species on each side, and the rules that only hold
 * *across* species — a `short_name` shared by a catalog and a grouping (#22) — were checked in the
 * app's container alone, which is the one place no test looks.
 */
interface CuratedFiles {
    /**
     * Every curated file of one species, as `file name to contents`, in whatever order the
     * storage lists them: [Curation.load] is what puts them in one.
     */
    fun read(species: CuratedSpecies): List<Pair<String, String>>
}
