# ADR 0013: Curated groupings and the proposal screen

- Status: accepted
- Date: 2026-07-30

## Context

Proposal derivation reads the family from Numista's `series` field. Measured on the real
inventory, **81 of the 608 cached types record no `series` at all**: with no set catalog and no
collection catalog naming them, they fail with `NoFamilyOrCatalog` and stay in the unclassified
list forever, however many of them the collector owns.

That list is not a rounding error. It holds the 100 pesetas of Franco — N#1885, one type whose
five star dates 66 to 70 are what the collector calls «los paquillos» — and every Venezuelan
circulating silver denomination except the 5 bolívares, which has a curated catalog. Numista is
never going to file these under a series, because a series is a collector's idea and these are
just money.

A collection catalog could give them a family (ADR 0009), but a catalog is a coverage claim: it
declares the official members of a sequence, so it can report one as Missing. Half of the real
inventory is bulk rows — 333 pieces in six rows, one of them 102 pieces of N#5316 under a single
year — and a coverage claim over a bulk row lies by construction: it would report five years as
absent while the coins sit in the same bag. There is no honest way to express «these coins go
together» with the tools the project had.

The same gap had a second symptom in the UI. A proposal card's title only opened something when
a catalog happened to match its exact variant key: the plate when followed and evidenced,
numista.com when only a catalog existed. With neither, the title was a plain `Text`. Every
French coin in the collection has its own `series` («Hercules type», «French regions», «100
francs Egalité - La Fayette») and no curated catalog, so those cards looked exactly like the
others and did nothing at all. Reported from the field as «sigue habiendo ventanas que no se
abren, casi todas las francesas».

## Decision

**A curated grouping declares that some Numista types form a family, and nothing else.** It
ships as an asset in `data/groupings/`, carries `type_ids` and no members, and declares neither
weight nor finish. It cannot produce a Missing state, because it never claims what a complete
sequence contains.

It is the weakest claim in the family ladder, which now resolves in this order:

1. a set catalog naming the exact types issued together (ADR 0012)
2. a collection catalog selected by the catalog routing for the type and piece (ADR 0009, as
   amended by the [catalog-family precedence
   decision](https://github.com/jenarvaezg/coindex/issues/83))
3. the real Numista family
4. **a curated grouping that names the type**
5. Numista's technical `System YYYY` monetary system (ADR 0012)

A grouping loses to a catalog on purpose: where a catalog exists it can also point at the gap,
which is strictly more than a grouping can do. It beats a technical family for the same reason
a catalog does — «Sistema monetario 1879-1936» is not a thing anybody collects. Two groupings
claiming the same type is a curation mistake and fails at startup rather than being resolved by
file order.

The physical variant still comes from each type's own metadata. A grouping declares no weight,
so a grouping that happened to span two weights honestly splits into two proposals instead of
pretending a ¼ bolívar and a 2 bolívares are one piece. The four shipped groupings are each one
denomination of uniform weight, so each is exactly one card.

**Every proposal card opens its own screen**, catalog or no catalog. The proposal screen lists
the pieces the collector owns in that group, as recorded, with the year on each row. The plate
and the catalog source moved into it, and the plate keeps a shortcut on the card as an action
rather than as the title. The division of labour is now stated in one line: the proposal screen
is what you own, the plate is the catalog with its gaps, and only the plate can say «me falta».

The 2 bolívares are the exception that proves the rule: they ship as a date-run catalog with 25
members across three types, not as a grouping, because the collector is actively hunting the
1965 and a grouping could never show him that hole.

## Consequences

The 81 orphans stop being unreachable by construction: any of them can be given a family by
curating a file, without inventing a sequence for it. Four groupings ship now — the paquillos and
the Venezuelan medios, reales and 1 bolívar — and the rest of the list is candidates.

A grouping is cheap enough to be tempting, and that is its risk: it is an editorial claim with
no source of truth behind it beyond the curator's judgement, so it names a representative
Numista page and its type ids are verified against numista.com like a catalog's.

No title is dead any more, but the gesture the collector already learned has changed: a title
used to be able to open numista.com and now always opens a screen of the app. The external link
survives inside, still marked with «↗».

The proposal screen also answers an open question by showing it. Numista indexes the year a coin
was **struck**, not the year on its face — N#10398 is dated 1945 and Numista dates it 1947 — and
whether a collected row carries the star year of a paquillo was never verified against real
data. The screen prints the recorded year of every row, so the first screenshot of the paquillos
settles whether a five-member date run for them is honest or a lie.
