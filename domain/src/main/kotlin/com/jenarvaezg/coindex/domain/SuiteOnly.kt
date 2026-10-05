package com.jenarvaezg.coindex.domain

/**
 * Public for the test suite and never called by the app, on purpose.
 *
 * It marks the disagreement reports of ADR 0021 §12 (the nets comparing curated files against the
 * seeded Numista cache: metal, object class, orphan collisions) and the vocabularies they read.
 * They stay out of startup because the curator's judgement outranks the physical check; in the
 * suite a red is a message, not a crash.
 *
 * Not `internal`: the reports run from `:app`'s suite, against `data/` and the shipped cache, and
 * `:app` is another Gradle module. A separate `domain/audit` module would force two private symbols
 * public (`curedCountries` behind `curedIssuerCodes`, the strict `Json` behind `OrphanSeeds`).
 *
 * `DomainSurfaceTest` enforces both halves: nothing else public may lack a caller, and nothing
 * marked may acquire one. It also lists the marked symbols, so a new mark shows up there.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class SuiteOnly
