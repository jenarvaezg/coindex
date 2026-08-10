# ADR 0027: Catalog validation is typed on purpose

- Status: accepted
- Date: 2026-08-10
- Decides [#223](https://github.com/jenarvaezg/coindex/issues/223)

## Context

`CollectionCatalog.validate()` returns a sealed hierarchy of roughly forty error cases. The only
production caller is `CatalogSeeds.parse`, which reads `.message` and throws. Architecture reviews
kept asking whether the typed surface was excess: an interface as large as the code that produces
it, for a consumer that needs a `String`.

That reading misidentifies the consumer. The seed loader is the runtime gate; the **curator** is who
the messages are for. The `curate-catalog` skill versions every new `data/collection-catalogs/*.json`
against this validator, and the precision of a failure — which field, which condition — is what
makes a bad plate fixable without guessing. Collapsing the hierarchy to bare strings would keep
startup loud and would make each new rule free to say «catálogo inválido».

The same shape already exists for the other curated artifacts (`CuratedGroupingValidationError`,
`CuratedOrphansValidationError`). The catalog's larger surface is a larger claim set (ADR 0020),
not a different design.

## Decision

1. **Keep the sealed error types.** Each case names its field and its condition. `CatalogSeeds`
   may keep reading `.message`; typed assertions stay the suite's contract with the curator.
2. **Keep validation out of the model file.** `validate()` and `CollectionCatalogValidationError`
   live in `CollectionCatalogValidation.kt`. Mixing the claim shape with its guardian helped
   nobody; splitting the file does not shrink the public surface of `:domain` and is not a step
   toward collapsing the types.

Reviews that rediscover the size of the hierarchy should treat this ADR as the answer, not a
prompt to reopen option 1 of #223.

## Consequences

- Adding a catalog rule still means a new sealed subtype, a message that names the fault, and a
  typed assertion in `CollectionCatalogTest`. That cost is accepted.
- A future split of other curated validators into sibling files is consistent with this decision;
  collapsing any of them to `List<String>` is not.
