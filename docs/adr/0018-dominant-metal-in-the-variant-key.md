# ADR 0018: The dominant metal is part of the variant key

- Status: accepted
- Date: 2026-08-01

## Context

ADR 0012 keys a proposal on the exact physical variant: family, normalized weight in
milli-ounces, and finish. Nothing in that tuple says what the coin is made of, so a one-ounce
silver bullion piece and a one-ounce gold one of the same series collapse into a single key —
31.1 g is 31.1 g whether it is silver or gold, and both are `Bullion`.

That never mattered while a proposal was only a suggestion. A proposal claims no coverage, so
two metals sharing a card was untidy rather than wrong. What #40 measured is that it breaks
**curating the second catalog**: `IndexScreen.kt:348` matches a card to its catalog with
`catalogs.firstOrNull { it.key() == proposal.key() }`, and the repository's `catalogFor` does the
same. Two catalogs with one key means the second one is never found. The gold catalog would be
born with no plate to open, and nothing would say so.

The case is live rather than hypothetical. Equilibrium (#43) is one Numista series holding three
collections, two of which are one-ounce bullion: N#307244 in silver and N#309842 in gold, both at
31.1 g. Gothic Horror was the first sighting; Equilibrium is the second, and it is the one whose
silver half has since been curated (#64).

Two alternative discriminants were measured and both were rejected:

- **Denomination** splits what it should keep together. The silver ounce of the Kookaburra is
  «1 Dollar» from 1992 on and «5 Dollars» in 1990 and 1991 — one collection, three keys.
- **Fineness** splits Kookaburra and Koala, where «Plata 999» and «Plata 999,9» coexist at
  31.1035 g in the same catalog.

Numista has no metal field. It has `composition.text`, a line of prose: «Plata 925», «Vellón
(plata 400) (Copper .500, Nickel .050, Zinc .050)», «Cobre recubierto de cuproníquel». The 723
seeded fichas hold 33 distinct spellings of it.

## Decision

**The dominant metal is the fourth component of the proposal variant key**, after family, weight
and finish. It rides everywhere the other three ride: `CollectionProposalKey`, the proposal route,
and the primary key of `collection_proposal_preferences`.

`Metal` is a wide enum from the start — gold, silver, platinum, palladium, copper, bronze, brass,
cupronickel, nickel, steel, zinc, aluminium and `other` — so a curation that meets a new alloy
does not have to widen it. Billon is silver: it is a low-grade silver alloy and the collector
calls it silver. `other` is **not** «unknown»: it is a composition with no dominant metal, which
in the seeded cache is the bimetallic 500 bolívares and one copper coin clad in cupronickel. A
composition nobody recorded, or one the rules do not read, is `null`.

It is **inferred on read** from `composition.text`, the same bargain that already gives the finish
(ADR 0005) and the issuer name: every ficha already cached carries its own composition, so the
field cost neither a single API call nor — when it landed — a cache migration. The prose it reads
has been the `composition` column of `TypeMetaEntity` since version 6 rather than a parse of `raw`
on every pass (#221), and that changes nothing here: what is stored is what Numista *wrote*, never
this app's verdict about it, so a better rule still fixes fichas cached months ago.
Everything inside parentheses is dropped before the rules run — the Koala of 2016 says
«Plata 999 (highlighted in 24-carat gold)», and only the head of that sentence describes what the
coin is made of.

**A catalog that is not a set must declare its metal**, and the declaration is about the variant
the catalog covers, not about each of its members. That is ADR 0016 applied to the third
component: a member whose ficha says another metal still takes the catalog's key, is still counted
in the plate, and is still owned. Seven silver coins and one of cupronickel can be curation rather
than a mistake — the curator's judgement outranks the physical check (#40).

**So the cross-check against Numista lives in the test suite and can never be fatal.** It reports
the member, its type and both metals, and a member that deviates on purpose is exempted by a
`variant_note` in prose on that member — the same bargain `closed_note` makes when a catalog closes
a series (#28). A startup validator here would turn `composition.text` into a veto over curation,
which is exactly backwards.

## Why the metal is cross-checked and the weight is not

ADR 0016 made a catalog's declared **weight** load-bearing precisely because Numista's per-type
grams disagree with themselves: the nineteen 1000 escudos of Portugal are one coin recorded as 27,
28 and 28.2 grams by different contributors, a spread of 39 milli-ounces against a snap tolerance
of 10. There is no figure to compare a catalog against, because there is no single figure. A check
would fire on the case the ADR exists to fix.

The metal has no such spread. All the variation in `composition.text` is in **how the alloy is
written** — the fineness, the breakdown, the language — and the dominant metal absorbs every one of
those: «Plata 925», «Plata 925 (92.5% silver, 7.5% copper)» and «Plata 999,9» all read `silver`.
The 30 shipped catalogs cross-check clean against all 723 fichas. So the metal is the one component
of the variant where a disagreement is information rather than noise, and it is the one that gets
checked. The **finish** is the worst of the three and stays unchecked for the opposite reason: the
field does not exist at all, `inferFinish` only reads the title, and the 52 Capitales are proof
because the FNMT says so while no ficha either confirms or denies it (#56).

## Consequences

- All 29 non-set catalogs declare `silver` today, so no card of either collection changes name,
  splits or merges. The metal enters the key against the **next** catalog, not this one.
- The card names the metal only when it is not silver. 73 of the 75 proposals measured in #40 are
  silver; printing it everywhere would lengthen every line to distinguish nothing.
- The set catalog declares no metal, as it declares no weight and no finish (ADR 0012), and its key
  carries none.
- Database version 4 rebuilds `collection_proposal_preferences`: the key is its primary key, and
  SQLite cannot add a column to one. Only the keys of the thirty catalogs shipped at that version
  are carried across, as a frozen literal list — a migration that read `data/` would change what an
  old phone does every time someone curates a catalog. Every other stored disposition is dropped and
  its card returns to **Disponible**, which is the price #55 already named for touching a key: the
  father's two cupronickel proposals of the Portuguese systems are re-followed by hand.
- The check that would have caught the twentieth-ounce of gold sitting in the Kookaburra catalog
  (#63) now exists, and it runs on the shipped files rather than on a fixture.
