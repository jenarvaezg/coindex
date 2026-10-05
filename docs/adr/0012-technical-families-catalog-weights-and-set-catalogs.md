# ADR 0012: Technical families, catalog-declared weights and set catalogs

- Status: accepted; the catalog-declared snapping target superseded by
  [ADR 0016](0016-a-catalog-owns-its-members-variant.md) and retired by #288
- Date: 2026-07-30

## Context

Numista files every pre-euro Portuguese coin under the `series` `System 1981-2001`. ADR 0007 drops
`System YYYY[-YYYY]` families from proposal derivation as monetary systems, and ADR 0009's
catalog-supplied family only applies when Numista records *no* family, so no curated catalog could
reach any Portuguese piece in the collection.

Curating them hit two more obstacles. Numista records the 2001 Porto 500 escudos as 13.96 g and its
six siblings as 14 g, which normalize to 449 and 450: two proposals, and a plate that would call the
coin missing once bought. And the 1983 XVII European Art Exhibition set — 500, 750 and 1000 escudos
at 7, 12.5 and 21 g, issued together — spans physical variants, which a catalog keyed on one variant
cannot express.

## Decision

**A technical family is the weakest family, never a rejection.** As amended by the
[catalog-family precedence decision](https://github.com/jenarvaezg/coindex/issues/83), derivation
resolves a family in this order: a schema 3 catalog listing the types issued together, a schema 1
or 2 catalog selected by routing for the type and piece, the real Numista family, and last the
technical family (ADR 0013 later inserts a curated grouping just before it). Only a type with no
family and no catalog stays unclassified. `UnclassifiedReason.TechnicalFamily` disappears;
`isTechnicalFamily` stays as a precedence rule, not a filter. The collector sees technical families
through the presentation-only alias `System 1981-2001` → `Sistema monetario 1981-2001`, which never
enters the key.

**Curated catalogs contribute their declared weight as a snapping target.** `normalizeWeightMillioz`
adds the `weight_millioz` of every schema 1 and 2 catalog to its targets, with the same ±10
milli-ounce tolerance, ties broken by proximity and then by the smaller target: 13.96 g becomes 450.
30 g still stays at 965, never an ounce.

> **Superseded by ADR 0016, retired by [#288](https://github.com/jenarvaezg/coindex/issues/288)**
> (only this decision). Once a catalog decides its own members' variant, a claimed type never
> reaches `normalizeWeightMillioz`, so declared targets only moved types **no** catalog claims: a
> Morgan dollar's legal 26.73 g was pulled to the 868 of a Spanish 10 euros. Only the bullion
> weights remain targets. Do not reinstate this without re-reading ADR 0016.

**Collection catalog `schema_version: 3` describes a set issued as a set.** Neither it nor its
members declare weight or finish: the set is the collectible unit. Its key has an *absent* weight,
and a type it lists takes that key even when Numista gives it a family, because naming the types
issued together is the stronger claim. It still has exactly one key, so plate resolution,
dispositions and the "no two catalogs claim one key" rule are untouched. An absent weight persists as
`weightMillioz = -1`, not zero, so a truncated or defaulted row is still invalid rather than a set,
and the Room schema does not change.

Schema 3 is only for **sets issued together as one product.** Fractional bullion is not one: a
quarter-ounce and a one-ounce Britannia are the same coin in two sizes and keep separate catalogs.

## Consequences

- Every Portuguese piece becomes reachable: the seven 500 escudos in silver .500 (1995-2001) get a
  schema 1 catalog, the 1983 set a schema 3 one, and the rest group as `Sistema monetario …`
  proposals.
- Both changes ship with those catalogs: no technical family had a proposal, so no stored
  disposition is orphaned. A later catalog claiming types grouped under a technical family *would*
  change their key and drop the disposition back to Available, which is recoverable by following
  again.
- A new catalog could merge two near-weight proposals anywhere, which the golden table pinned
  *(until #288; since then a declared weight moves only its own members)*.
