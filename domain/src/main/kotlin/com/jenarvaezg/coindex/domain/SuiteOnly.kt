package com.jenarvaezg.coindex.domain

/**
 * This symbol is public for the test suite to call, and the app never calls it — on purpose.
 *
 * What wears it are the disagreement reports of ADR 0021 §12: the nets that compare a curated file
 * against the seeded Numista cache — metal, object class, orphan collisions — and the vocabularies
 * they read. None of them belongs at startup. A metal that contradicts a ficha is a curation
 * question a person answers by editing a file, and the curator's judgement outranks the physical
 * check, so making it fatal on boot would turn `composition.text` into a veto over curation. They
 * live in the suite instead, where a red is a message and not a crash.
 *
 * It is not a synonym for `internal`, which is what would express this if it could. Every one of
 * these is exercised from `:app`'s suite — the reports run against `data/` and the shipped cache,
 * which is where those files are — and `:app` is a different Gradle module, so `internal` would
 * hide them from the tests that exist to run them.
 *
 * A separate `domain/audit` module would enforce it by compiler, and two of the six cannot go:
 * `curedIssuerCodes` is `curedCountries.keys` and `OrphanSeeds` parses with the strict `Json` the
 * other seeds parse with, both of them `private` here and both needed by production code that
 * stays. Moving those two means making what is private public, which is opening two holes in this
 * module's surface to close six — and moving only the other four buys a rule with exceptions,
 * which is the thing that stopped being mechanical.
 *
 * So the label is declared here, at the symbol, and `DomainSurfaceTest` holds it to both halves of
 * what it claims: nothing else may be public without a caller, and nothing wearing this may
 * acquire one. Marking a symbol to quiet that test is how the seventh leftover gets in, so the six
 * are written out there as well.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class SuiteOnly
