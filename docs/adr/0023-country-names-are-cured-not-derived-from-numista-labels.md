# ADR 0023: A card's country is cured, because Numista writes issuing entities

- Status: accepted
- Date: 2026-08-05
- Amends ADR 0021 §4 and §9.

> **Amended on 2026-08-06 (#257).** The decision stands whole and the table grows by one, for a
> reason the nine did not have: **the language**. `new_south_wales` — the issuer of the holey dollar
> and the dump, curated into `historia-del-real` — arrives as «New South Wales» even with `lang=es`.
> It has none of the three vices `readsAsACountry` checks: no period of validity, no index
> inversion, 15 characters. So **the net passes it and the card would print English**, against the
> language rule of ADR 0021 §4 this very ADR cites.
>
> Measured over the shipped cache, it is the only English label among the **25 issuer codes the
> curated files declare**, which is why the hole went unseen: the other 24 arrive in Spanish, and the
> nine corrections all answer a vice the net does see.
>
> Two things deliberately do **not** change. `readsAsACountry` gains no fourth clause, because
> detecting a language is not one line of code over a third party's prose and guessing at that prose
> is what «Not a heuristic over Numista's prose» already refused — the table carries the finding, and
> the curator reading the ficha is the check. And the entry is not «Australia»: by the second rule
> below, a state that is nobody's country keeps its own name, and in 1813 Nueva Gales del Sur was a
> British colony while today it is a state, so it sits beside `rome` and `russia-empire` rather than
> beside `russie`.

## Context

ADR 0021 §9 gave the card's eyebrow to the curated file: the code comes from `issuer_code`, and the
name from the type cache keyed by that code, «rather than a table of countries maintained in code».
#180 measured what the cache actually serves. **Numista does not write countries.** It writes
issuing entities with their period of validity, so of the 40 issuer codes in the seeded cache **nine
do not read as the name of a country**:

| `issuer_code` | What the ficha says |
| --- | --- |
| `russie` | «Federación de Rusia (1991-presente)» |
| `chine` | «China, República Popular» |
| `allemagne` | «Alemania, República Federal de» |
| `allemagne-pre1945` | «Alemania (1871-1948)» |
| `haiti` | «Haití (1804-presente)» |
| `republique_dominicaine` | «Dominicana, República (1844-presente)» |
| `democratic_republic_congo_period` | «República Democrática del Congo (1997-presente)» |
| `rome` | «Romano, Imperio (27 a. C. - 395 d. C.)» |
| `russia-empire` | «Ruso, Imperio (1547-1917)» |

Measured through the shipped domain over the two real captures: **5 of the 38 cards** in the
curator's phone and **2 of the 61** in the collector's. Both numbers grow with curation — four
`russie` catalogs, two `chine`, one `haiti` and one `allemagne` grouping ship today, where #180 was
written against five files.

This contradicts three things already decided. The **language rule** of ADR 0021 §4 asks for country
names in Spanish (`Ruanda`, not `Rwanda`), and a period of validity is not a name. **Identity is not
a definition of scope**, which is exactly what §4 took off the Krugerrand's `family`: a parenthesis
of dates belongs to the plate's `name`, never to the line that says which coin this is. And the
eyebrow **has to fit on a card**: «Federación de Rusia (1991-presente)» is 35 characters in small
caps over a `short_name` whose median is 20, and it took a chip row to itself in the Coins shelf.

The label is not wrong as catalogue data — the parenthesis is what tells the Russian Federation
apart from `ancienne_urss`, and that distinction is real — so this is not a correction to open
against numista.com. It is ours at the moment we paint it.

## Decision

### The nine exceptions are a curated table in `domain`, keyed by issuer code

`cardCountry(issuerCode, numistaName)` answers with the curated name where the table has the code and
with the ficha's name everywhere else. It is **a table of corrections and not a catalogue of
countries**: the other 31 codes stay Numista's, so keeping «Venezuela» in Kotlin would duplicate what
the ficha says right, and a coin from a country nobody owns yet labels its card with no line added.

Raw stays raw and is read through a function, which is the bargain `objectClassOf`, the metal and the
finish already take: `TypeMeta.issuerName` is still what Numista said, `TypeMeta.country` is the
reading of it, and a correction made today reaches fichas cached years ago without an API call.

### What each correction says depends on what the entity is

The table is nine curator's decisions and not one rewrite rule, and the criterion is what the entity
**is** rather than how its code is spelled:

- A **country served with its period of validity, or inverted for an index**, gives its common Spanish
  name: the Russian Federation *is* today's Russia, so `russie` is «Rusia», and `chine`, `haiti`,
  `allemagne`, `republique_dominicaine` and `democratic_republic_congo_period` follow.
- A **state that is nobody's country any more** keeps its own name, cleaned: `rome` is «Imperio
  romano» and `russia-empire` is «Imperio ruso» — not «Rusia», which would swallow tsarist coinage
  into the modern chip. It is the treatment the ficha already gives `ancienne_urss` («Unión
  Soviética») and `autriche-habsbourg` («Imperio austríaco»), and the reason those two need no line.

`allemagne-pre1945` is «Alemania» on the first rule and not the second, because that is what **Numista
itself calls it**: «Alemania (1871-1948)» is a country with a period, where tsarist Russia is filed as
«Ruso, Imperio». So the two German codes answer with one country, which is not a merge decided here —
it is Numista's own labelling with the parenthesis removed. Two consequences, both measured: a fileless
card holding one type of each labels «Alemania» instead of going silent for disagreeing with itself,
and the collector's Coins shelf offers one «Alemania» chip over five coins — a 1923 Rentenpfennig, two
Hindenburg Reichsmark and two euro commemoratives — rather than two halves of Germany. That is how
every other country in the corpus already reads: `espagne` covers Franco's pesetas and today's euros
under «España» in one chip of 104 types.

### Not a heuristic over Numista's prose

Cutting at the first `(` and un-inverting on the comma is one line of code and gets each of these
subtly wrong: «Federación de Rusia» is still not the name of a country, «Imperio Romano» carries
Numista's capital inside it, and «Alemania, República Federal de» either loses its tail or keeps a
scope definition on a line of identity. This project curates two collections and investigates each
datum rather than inventing a mechanism that guesses it.

### Not a field in the curated file

ADR 0021 closes with «no new field in any curated file except `short_name`», and that holds: an
`issuer_name` per file would repeat the country across the nine files of the United Kingdom, and the
18 cards without a file — which read the same string through their pieces — would still have nobody
to say it for them.

### What reads as a country is one rule, and the list is netted against what ships

`readsAsACountry` says it: **no period of validity, no index inversion, and no more than the 40
characters the `short_name` below it is capped at** (#163). That third clause is the eyebrow's other
argument — «Federación de Rusia (1991-presente)» is 35 characters in small caps over a name whose
median is 20 — and it is netted rather than trusted, because a clean label can still be too long.

A table of corrections over a third party's prose rots in two directions, and both go red in
`CardCountriesTest`: an entry whose code no ficha carries any more, and a new issuer whose label is
not a country either. **Red means there is a country to name**, not a test to relax. It is a test and
not a report because the finding is rare — nine in 829 fichas, and a tenth arrives the day a coin from
a new country does (ADR 0021 §12).

### The stored country filter is migrated by the same rule

The country is the **only facet of either shelf that is not an enum**, so `ShelfCodec`'s promise that
«a value this version does not recognise reads back as no filter» was inherited by every other facet
and not by this one: it stores the label itself. A phone with «Federación de Rusia (1991-presente)»
selected would have reopened filtering on a string no row produces any more — an empty list, a filter
badge at 1, no chip lit — and `russie` is 293 of the 829 seeded fichas, which makes it the likeliest
chip to have been left on. So a stored country that `readsAsACountry` rejects is read back as no
filter. It needs no version key and cannot fire twice: the chips are built from what the rows say.

## Consequences

- ADR 0021 §4's «the only labelling rule left in code is `System 1879-1936` → Sistema monetario»
  becomes **two**: that one, and this. Both format a generated string; neither renames anything a
  curator wrote. The six dead family aliases stay dead.
- ADR 0021 §9's «rather than a table of countries maintained in code» survives as written for the
  31 clean codes; the nine exceptions are the amendment.
- A curated file naming `haiti` now labels its card **whether or not a Haitian ficha has reached the
  phone**, which is the promise of §9 taken one step further: the file speaks, and for these nine it
  no longer needs the cache to speak for it.
