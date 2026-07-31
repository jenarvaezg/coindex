# ADR 0016: A catalog is authoritative about its own members' variant

- Status: accepted
- Date: 2026-07-31

## Context

ADR 0012 keys a proposal on the exact physical variant: family, weight in milli-ounces, and
finish. The weight comes from the gram figure Numista records **per type**, normalized by
`normalizeWeightMillioz`, which snaps to the common bullion weights and to the weights declared
by curated catalogs. The snap tolerance is 10 milli-ounces, kept deliberately tight: 30 g sits
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

A collection catalog that is not a set is **authoritative about the physical variant of its own
members**. In `deriveCollection`, when a piece's resolved family is the family of a catalog that
names its type, the proposal key is the catalog's own key — its declared weight and finish — and
no weight is inferred from Numista at all.

The precedence of ADR 0012 and ADR 0013 for choosing the *family* is unchanged. This rule only
applies once the catalog's family has already won, so a type with a real Numista family that
differs from the catalog's keeps the Numista family, and its weight is still inferred: a catalog
never reaches across into a variant it did not claim.

Snapping stays. It still does the job the catalog cannot: a type of the same family that the
catalog does **not** name — next year's issue, say — lands on the catalog's weight by snapping
and shares its card instead of splitting off.

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
