# ADR 0031: A family's label is cured too, because Numista writes it in its own language

- Status: accepted
- Date: 2026-09-07
- Extends ADR 0023 to the second line Numista writes on a card. Amends ADR 0021 §4.

## Context

Like the country (ADR 0023), the family is a string Numista writes on the card, and more of them are
wrong. In the collector's seeded cache on 7 September 2026 (198 types), 13 types in 10 families
carry a Numista series no curated file claims: six are in English, `100 francs Egalité - La Fayette`
mixes languages, and `1190e anniversaire du couronnement de Charlemagne (800-1990).` is French prose
with dates. That is ten of his 67 cards, against the 5 of 38 and 2 of 61 ADR 0023 was accepted on.

#566 declined fixing it on Numista (merging seven fichas for a label), #568 found that a curated
grouping reaches one of the ten, and #572 refused to let groupings outrank a Numista series
(ADR 0013). The problem is the text, not the ladder.

## Decision

### The eight corrections are a curated table in `domain`, keyed by the family string

`familyLabel(family)` returns the curated label where the table has the string, `Sistema monetario
YYYY-YYYY` for a technical family (ADR 0012), and the ficha's text otherwise. As with `cardCountry`,
`TypeMeta.family` and the variant key stay raw, and a correction reaches old fichas without an API
call.

### It cures the text, and never the scope or the ladder

`familyLabel` runs only in `CollectionTitles.nameOf`, after catalogs and groupings fail to claim the
card, so it changes what a card says and never what it holds. Labels translate Numista without
narrowing it:

- `Austria and its People`, Numista's umbrella over three Münze Österreich programmes (series 1582),
  is «Austria y su pueblo», not «Leyendas de Austria», which a castle on the same card would make
  false.
- French series 10784 is «Carlomagno», not «100 francos Carlomagno» (two of its seven types are 500
  francs), and loses its anniversary dates (ADR 0021 §4).
- `Millennium` spans four issuers, so it is just «Milenio».

### Two of the ten stay as Numista wrote them

`DC Comics` is a proper name and `Gothic Horror` a Royal Mint range, and the curated files already
keep mint range names in their own language («The Queen's Beasts», «Silver Britannia», «Noah's
Ark»…). **Numista's prose about a programme is ours to correct; the programme's own name is not.**

### Not a heuristic, and no net that says a label needs curing

ADR 0023 refused to detect language and left the call to the curator. That holds even more here:
only judgement tells «Gothic Horror» from «Milenio».

### The table is netted against what ships

`CuredFamiliesTest` checks, over the shipped cache and curated files, that each correction still
names a served series, that no file has since claimed it (how the six aliases of #22 died), and that
no cured name collides with a curated `short_name`. Red means re-reading the source. The first check
is real: #581 took `Charlemagme` (N#104170) for a Numista typo, but Numista had already fixed it and
only one phone's cache was stale. The table uses the corrected string; the stale ficha heals when
asked again (ADR 0025).

### A cured name is not offered to the box the collector names

`CollectionTitles.curatedNames()` stays the `short_name` of catalogs and groupings: like raw
families (ADR 0021 §11), a cured family «moves with the inventory».

## Consequences

- ADR 0021 §4's labelling rules in code become three, with ADR 0023's. All correct generated
  strings, and none renames what a curator wrote.
- Names are resolved once in `CollectionIndex`, so screen, plate, export and notebook agree (#581).
- If a curated file claims one of these families, its `short_name` wins and `CuredFamiliesTest`
  flags the dead entry.
