# ADR 0018: The dominant metal is part of the variant key

- Status: accepted
- Date: 2026-08-01

## Context

ADR 0012's key (family, weight, finish) says nothing about metal, so a silver and a gold one-ounce
bullion coin of one series share a key. #40 found that this breaks **curating the second catalog**:
cards and the repository's `catalogFor` match catalogs by `key()` equality, so the second of two
catalogs with one key is never found and has no plate. The case is live: Equilibrium (#43) holds
N#307244 in silver and N#309842 in gold, both one-ounce bullion, and its silver half has been
curated (#64).

Two other discriminants were rejected. **Denomination** splits one collection: the silver
Kookaburra ounce is «1 Dollar» from 1992 and «5 Dollars» in 1990 and 1991. **Fineness** splits
Kookaburra and Koala, where «Plata 999» and «Plata 999,9» coexist in one catalog. Numista has no
metal field, only the prose of `composition.text`: «Plata 925», «Vellón (plata 400) (Copper .500,
Nickel .050, Zinc .050)», «Cobre recubierto de cuproníquel».

## Decision

**The dominant metal is the fourth component of the variant key**, after family, weight and finish,
everywhere the key goes: `CollectionProposalKey`, the proposal route and the primary key of
`collection_proposal_preferences`.

`Metal` is a wide enum — gold, silver, platinum, palladium, copper, bronze, brass, cupronickel,
nickel, steel, zinc, aluminium and `other` — so a new alloy does not have to widen it. Billon is
silver. `other` is a composition with no dominant metal (bimetallic, clad), not «unknown»; an
unrecorded or unreadable composition is `null`.

It is **inferred on read** from `composition.text`, like the finish (ADR 0005), so a better rule
fixes old fichas at no API cost; since database version 6 the prose is stored in
`TypeMetaEntity.composition` (#221), but never a verdict about it. Parentheses are dropped first:
the 2016 Koala's «Plata 999 (highlighted in 24-carat gold)» is silver.

**A catalog that is not a set must declare its metal**, for the variant it covers. As ADR 0016 does
for weight and finish, a member whose ficha says another metal still takes the catalog's key, counts
in the plate and is owned: the curator's judgement outranks the physical check (#40).

**So the cross-check against Numista lives in the test suite and is never fatal.** It reports the
member, its type and both metals; a deliberate deviation is exempted with a prose `variant_note`, as
`closed_note` does for a closed series (#28).

## Why the metal is cross-checked and the weight is not

Numista's grams disagree with themselves (the 1000 escudos are recorded as 27, 28 and 28.2 g), so
there is no figure to check a declared weight against. The metal has no such spread: it varies only
in how the alloy is written, which the dominant metal absorbs, and the shipped catalogs
cross-checked clean, so a disagreement is information. The **finish** stays unchecked: Numista has
no field for it, and the 52 Capitales are proof because the FNMT says so, not any ficha (#56).

## Consequences

- Every non-set catalog then declared `silver`, so no card changed; the metal matters from the next
  catalog on.
- The card names the metal only when it is not silver, which is almost never.
- The set catalog declares no metal, as it declares no weight or finish (ADR 0012).
- Database version 4 rebuilds `collection_proposal_preferences`, since SQLite cannot add a column to
  a primary key. Only the keys of the catalogs shipped then are carried across, as a frozen literal
  list: a migration that read `data/` would change old phones with every curation. Every other
  disposition returns to **Disponible** (#55).
- The check that would have caught the twentieth-ounce of gold in the Kookaburra catalog (#63) now
  runs on the shipped files.
