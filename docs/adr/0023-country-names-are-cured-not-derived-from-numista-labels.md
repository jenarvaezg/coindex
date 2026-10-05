# ADR 0023: A card's country is cured, because Numista writes issuing entities

- Status: accepted; amended by #257 (an entry for language); extended by
  [ADR 0031](0031-family-labels-are-cured-like-country-names.md) to the family label
- Date: 2026-08-05
- Amends ADR 0021 §4 and §9.

> **Amended on 2026-08-06 (#257).** `new_south_wales` arrives as «New South Wales» even with
> `lang=es`, which `readsAsACountry` cannot catch, so the table gains «Nueva Gales del Sur» (a
> former colony, so by the second rule below it keeps its own name, not «Australia»). Detecting
> language stays refused: the table carries the finding. `ghana` came later with #310, under the
> first rule.

## Context

ADR 0021 §9 takes the card's eyebrow from the type cache, keyed by the curated `issuer_code`,
«rather than a table of countries maintained in code». #180 found that Numista writes issuing
entities, not countries: nine of the 40 issuer codes in the seeded cache do not read as a country.

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

That was 5 of 38 cards on the curator's phone and 2 of 61 on the collector's. These labels break
ADR 0021 §4: country names go in Spanish, a period of validity is scope and not identity (the cut §4
made on the Krugerrand's family), and «Federación de Rusia (1991-presente)» is 35 characters over a
`short_name` whose median is 20. They are correct catalogue data, so there is nothing to fix on
numista.com; the problem is in how we paint them.

## Decision

### The nine exceptions are a curated table in `domain`, keyed by issuer code

`cardCountry(issuerCode, numistaName)` returns the curated name where the table has the code and the
ficha's name otherwise: a table of corrections, not of countries. `TypeMeta.issuerName` stays raw
and `TypeMeta.country` is the reading, so a correction reaches old cached fichas without an API
call.

### What each correction says depends on what the entity is

- A **country with its period of validity, or inverted for an index**, gets its common Spanish
  name: `russie` is «Rusia», and likewise `chine`, `haiti`, `allemagne`, `republique_dominicaine`
  and `democratic_republic_congo_period`.
- A **state that is nobody's country any more** keeps its own name, cleaned: `rome` is «Imperio
  romano» and `russia-empire` «Imperio ruso», not «Rusia». The ficha already does this for
  `ancienne_urss` and `autriche-habsbourg`.

`allemagne-pre1945` is «Alemania» by the first rule, because Numista calls it «Alemania
(1871-1948)». Both German codes share one «Alemania» chip, as `espagne` covers pesetas and euros
under «España».

### Not a heuristic over Numista's prose

Cutting at `(` and un-inverting on the comma gets each case subtly wrong («Federación de Rusia»,
«Imperio Romano», «Alemania, República Federal de»). Each datum is investigated, not guessed.

### Not a field in the curated file

ADR 0021 allows no new curated field but `short_name`. An `issuer_name` would repeat across every
file of a country, and cards without a file would still lack it.

### What reads as a country is one rule, and the list is netted against what ships

`readsAsACountry`: no period of validity, no index inversion, and at most the 40 characters a
`short_name` is capped at (#163). `CardCountriesTest` fails on an entry whose code no ficha carries
any more and on a new issuer whose label is not a country. Red means a country to name, not a test
to relax.

### The stored country filter is migrated by the same rule

`ShelfCodec` stores the country label itself (the only facet that is not an enum), so a phone with a
retired label selected would reopen on an empty list, with a filter badge and no chip lit. A stored
country that `readsAsACountry` rejects reads back as no filter. No version key is needed: the chips
are built from what the rows say.

## Consequences

- ADR 0021 §4's single labelling rule in code (`System 1879-1936` → Sistema monetario) becomes two;
  both format a generated string, and neither renames what a curator wrote.
- ADR 0021 §9's «rather than a table of countries» still holds for the clean codes.
- A curated file naming one of these codes labels its card before any ficha of that country reaches
  the phone.
