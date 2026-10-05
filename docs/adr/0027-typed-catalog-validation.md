# ADR 0027: Catalog validation is typed on purpose

- Status: accepted
- Date: 2026-08-10
- Decides [#223](https://github.com/jenarvaezg/coindex/issues/223)

## Context

`CollectionCatalog.validate()` returns a sealed hierarchy of about forty error cases, and its only
production caller, `CatalogSeeds.parse`, reads `.message` and throws. Architecture reviews kept
asking whether the typed surface is excess. But the messages are for the **curator**: the
`curate-catalog` skill versions every new `data/collection-catalogs/*.json` against this validator,
and naming the field and the condition is what makes a bad plate fixable without guessing. The
other curated artifacts validate the same way (`CuratedGroupingValidationError`,
`CuratedOrphansValidationError`); the catalog just has more claims (ADR 0020).

## Decision

1. **Keep the sealed error types.** Each case names its field and its condition; typed assertions
   are the suite's contract with the curator, and `CatalogSeeds` may keep reading `.message`.
2. **Keep validation out of the model file.** `validate()` and `CollectionCatalogValidationError`
   live in `CollectionCatalogValidation.kt`. This does not shrink the public surface of `:domain`
   and is not a step toward collapsing the types.

Reviews that rediscover the size of the hierarchy should take this ADR as the answer, not as a
prompt to reopen option 1 of #223.

## Consequences

- A new catalog rule costs a sealed subtype, a message that names the fault, and a typed assertion
  in `CollectionCatalogTest`.
- Splitting other curated validators into sibling files is consistent with this decision;
  collapsing any of them to `List<String>` is not.
