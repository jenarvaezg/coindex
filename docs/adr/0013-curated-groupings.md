# ADR 0013: Curated groupings and the proposal screen

- Status: accepted; amended by [ADR 0021](0021-what-a-collection-is-and-the-top-level.md) §2, §8,
  §9, §10
- Date: 2026-07-30

## Context

Many cached types record no Numista `series` (81 of 608 at the time) and, with no catalog naming
them, stay unclassified for ever: the 100 pesetas of Franco (N#1885, «los paquillos»), or the
Venezuelan circulating silver other than the 5 bolívares. A catalog could give them a family
(ADR 0009), but a catalog is a coverage claim that can report a member as Missing, which is false
over a bulk row such as 102 pieces of N#5316 under a single year. Nothing could say «these coins go
together» without claiming coverage.

The UI had the same gap: a proposal card's title only opened something when a catalog matched its
key, so cards such as every French coin did nothing («sigue habiendo ventanas que no se abren, casi
todas las francesas»).

## Decision

**A curated grouping declares that some Numista types form a family, and nothing else.** It ships
in `data/groupings/` with `type_ids`, no members, no weight and no finish, so it can never produce a
Missing state.

It is the weakest claim in the family ladder:

1. a set catalog naming the exact types issued together (ADR 0012)
2. a collection catalog selected by the catalog routing for the type and piece (ADR 0009, as
   amended by the [catalog-family precedence
   decision](https://github.com/jenarvaezg/coindex/issues/83))
3. the real Numista family
4. **a curated grouping that names the type**
5. Numista's technical `System YYYY` monetary system (ADR 0012)

A catalog outranks it because a catalog can also show the gap; it outranks a technical family
because nobody collects «Sistema monetario 1879-1936». Two groupings claiming one type fail at
startup instead of being resolved by file order.

The physical variant still comes from each type's metadata, so a grouping spanning two weights
splits into two proposals. The shipped groupings keep uniform weight per file.

> **Amended by ADR 0021.** A grouping cannot join what the variant key splits: a family split by
> weight is curated as a catalog (#157). And a grouping is a collection like any other, not a view
> subordinate to one (§2, §10).

**Every proposal card opens its own screen**, listing the pieces owned in that group, as recorded,
with the year on each row. The plate and the catalog source move into it, and the plate keeps a
shortcut on the card as an action.

> **Amended by ADR 0021 §8, §9.** This also said «the proposal screen is what you own, the plate is
> the catalog with its gaps», which is false: the plate shows what you own too. A card has one
> destination, and «proposal» is no longer the word.

The Venezuelan silver denominations that hunt a year ship as date-run catalogs, because a grouping
cannot show the hole: the 2 bolívares, the 1 bolívar (#113), the reales (#114) and the medios
(#115), each with types the old grouping had omitted. «Medios de Venezuela» and «Reales de
Venezuela» keep the street names, where «medio» is the quarter. A bulk row of N#5316 under one year
may report a false Missing for 1960 or 1965 until the collector splits it by year in Numista; that
is accepted.

## Consequences

- Any type without a family can get one by curating a file, without inventing a sequence.
- A grouping is cheap, and that is its risk: it rests only on the curator's judgement, so it names a
  representative Numista page and its type ids are verified like a catalog's.
- No card title is dead. Titles that opened numista.com now open an app screen, with the external
  link inside, marked «↗».
- The screen prints each row's recorded year, which matters because Numista records the year a coin
  was **struck**, not its face date (N#10398 is dated 1945 and recorded as 1947). ADR 0014 settles
  what that means for the paquillos.
