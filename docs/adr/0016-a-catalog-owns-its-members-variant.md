# ADR 0016: A catalog is authoritative about its own members' variant

- Status: accepted; supersedes the catalog-declared snapping target of
  [ADR 0012](0012-technical-families-catalog-weights-and-set-catalogs.md); amended by #288
- Date: 2026-07-31

## Context

ADR 0012 keys a proposal on family, weight and finish, the weight being Numista's per-type grams
snapped by `normalizeWeightMillioz` within 10 milli-ounces. The tolerance is tight on purpose: 30 g
is 965 milli-ounces and an ounce is 1000.

Numista can disagree with itself about one coin. The **1000 escudos of Portugal** (19 types,
1992-2001, all silver .500, one physical coin) carry three weights from different contributors:

| Grams | Milli-ounces | Types |
| --- | --- | --- |
| 27 | 868 | 5 |
| 28 | 900 | 5 |
| 28.2 | 907 | 3 |

Declaring 900 pulls in the 28.2 g types but never the 27 g ones, and widening the tolerance would
break the ounce distinction. In the real collection one catalog produced **two** cards, `0.9 oz`
and `0.868 oz`, while its plate, which matches by type id, correctly counted 13 of 19 owned,
including the five pieces in the second card. The catalog, whose type ids are verified by hand and
which describes one variant, was right; the inference from third-party grams was wrong.

## Decision

A collection catalog that is not a set is **authoritative about the family and physical variant of
the types it claims**. Once routing selects it for a piece, `deriveCollection` uses the catalog's
own key — declared family, weight, finish and metal — and infers none of it from Numista. A date run
keeps ADR 0009's type-based plate evidence. Since the
[catalog-family precedence decision](https://github.com/jenarvaezg/coindex/issues/83) this holds
even when Numista supplies another family; types no catalog claims keep their Numista family.
Issue-qualified members require the exact `issue_id` (ADR 0019).

> **Amended by [#288](https://github.com/jenarvaezg/coindex/issues/288).** Declared weights were
> kept as snapping targets so that next year's unnamed issue would join its catalog's card, but
> grams cannot check the family: of the unclaimed types they moved, none landed on a catalog of its
> own family (a Morgan dollar pulled by a Spanish 10 euros, a Licinius I nummus by the Portuguese
> 2$50). Only the bullion weights remain targets; next year's issue reaches its plate by being
> curated into the file.

## Consequences

- The thirteen 1000 escudos become one card of 13/19 instead of two double-counting five pieces.
- A wrong declared weight now mis-keys the catalog's own members. The validator rejects a non-set
  catalog with no weight or one outside `1..1_000_000`, but never compares it with the members'
  grams: that mismatch is the case this ADR exists for.
- No other shipped catalog changed (the field report was identical before and after).
- A set catalog is untouched: it keys on family alone (ADR 0012).
