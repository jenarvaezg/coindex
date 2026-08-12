# ADR 0016: A catalog is authoritative about its own members' variant

- Status: accepted, supersedes the catalog-declared snapping target of
  [ADR 0012](0012-technical-families-catalog-weights-and-set-catalogs.md), amended by #288
- Date: 2026-07-31

## Context

ADR 0012 keys a proposal on the exact physical variant: family, weight in milli-ounces, and
finish. The weight comes from the gram figure Numista records **per type**, normalized by
`normalizeWeightMillioz`, which snaps to the common bullion weights and to the weights declared
by curated catalogs — the second of those retired by #288, see the amendment below. The snap
tolerance is 10 milli-ounces, kept deliberately tight: 30 g sits
965 milli-ounces from zero and a true ounce sits at 1000, so a loose tolerance would read a
near-ounce piece as an ounce.

That tolerance is not enough when Numista's own figures for one coin disagree. Measured while
curating the **1000 escudos of Portugal** (19 types, 1992-2001, all silver .500, all physically
the same coin), Numista records three different weights, entered by different contributors:

| Grams | Milli-ounces | Types |
| --- | --- | --- |
| 27 | 868 | 5 |
| 28 | 900 | 5 |
| 28.2 | 907 | 3 |

Declaring 900 in the catalog pulls in the 28.2 g types, seven away. It can never pull in the
27 g types, thirty-two away, and widening the tolerance that far would break the ounce
distinction the tolerance exists to protect.

The consequence was measured on the real collection before this decision. One curated catalog
produced **two** cards — `0.9 oz` with eight types and `0.868 oz` with five — and because
`buildCollectionCatalogAlbum` matches members to pieces by type id and not by weight, the plate
correctly reported 13 of 19 owned **including** the five pieces sitting in the second, catalog-less
card. The same five coins were counted as owned in one place and shown as needing curation in
another.

The catalog was not wrong; the inference was. A curated catalog is the most expensive artifact in
the project: every `numista_type_id` in it is verified against numista.com by hand, and it
declares exactly one weight and one finish because it describes one physical variant. Deriving
that variant from a third-party gram value, for types the catalog already names, is inferring
what is already known.

## Decision

A collection catalog that is not a set is **authoritative about the family and physical variant
of the types it claims**. In `deriveCollection`, once catalog routing selects the catalog for a
piece, the proposal key is the catalog's own key — its declared family, weight, finish and metal
— and none of those parts is inferred from Numista. A date run retains ADR 0009's type-based
plate evidence: an undated piece can select its proposal without filling a dated member.

The [catalog-family precedence decision](https://github.com/jenarvaezg/coindex/issues/83)
strengthened the original rule, which only applied after the catalog family had already won.
A real Numista family still governs types that no catalog claims. Issue-qualified members remain
the stricter case: they require the exact `issue_id`, and an unmatched issue stays unclassified
as specified by ADR 0019.

Snapping stays. It still does the job the catalog cannot: a type of the same family that the
catalog does **not** name — next year's issue, say — lands on the catalog's weight by snapping
and shares its card instead of splitting off.

> **Amended by [#288](https://github.com/jenarvaezg/coindex/issues/288).** Only snapping to the
> common bullion weights stays; a weight a catalog declares is no longer a target for anything
> outside that catalog. The paragraph above describes a case the magnet cannot tell apart from
> an accident: it matches on grams, and the family has to agree by luck. Measured over 75
> catalogs and 1050 seeded fichas, a declared weight was moving 22 unclaimed types and **not one
> of them** landed on the card of a catalog of its own family — they were a Morgan dollar pulled
> by a Spanish 10 euros, an Abd al-Aziz ½ dirham pulled by the Venezuelan medios, a Licinius I
> nummus pulled by the Portuguese 2$50. Next year's issue reaches its plate the way every other
> member does: someone curates it into the file.

## Consequences

- The thirteen 1000 escudos of the collector become one card of 13/19 instead of two cards
  double-counting five pieces. The three `System 1981-2001` cards they used to produce are gone.
- A catalog's declared weight becomes load-bearing rather than a hint, so a wrong figure in a
  catalog now mis-keys its own members instead of being corrected by Numista. The validator
  already rejects a non-set catalog with no weight and a weight outside `1..1_000_000`; nothing
  checks that the figure resembles its members' grams, and deliberately so — that mismatch is the
  whole point.
- Nothing changes for the other twenty-two shipped catalogs: verified by running the field report
  over both real inventories before and after, with identical output except for the escudos.
- A set catalog is untouched. It spans variants by definition (ADR 0012) and already keys on
  family alone.
