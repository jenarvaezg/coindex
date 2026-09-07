# ADR 0031: A family's label is cured too, because Numista writes it in its own language

- Status: accepted
- Date: 2026-09-07
- Extends ADR 0023 to the second line Numista writes on a card. Amends ADR 0021 §4.

## Context

ADR 0023 found that Numista does not write countries — it writes issuing entities with their period
of validity — and answered with a table of corrections keyed by `issuer_code`, read at the moment the
card is painted. The **family** is the other string on that card that a third party writes, and it
has the same vice in a worse proportion.

Measured over the collector's seeded cache on 7 September 2026, 198 distinct types: **13 types in 10
families** carry a real Numista series that no curated file claims, and **six of those families are
in English**. Two more are not clean either — `100 francs Egalité - La Fayette` mixes two languages,
and `1190e anniversaire du couronnement de Charlemagne (800-1990).` is French prose with a
parenthesis of dates and a full stop. Ten cards of his 67.

The precedent's own arithmetic makes this the larger case: ADR 0023 was accepted over **5 of 38**
cards in the curator's phone and **2 of 61** in the collector's. This is ten.

Three things had already been decided around it and none of them answers it:

- **#566** closed `wontfix` the Numista side of the worst case — merging seven fichas by negotiating
  with a referee, for a label, does not pay. It closed the door on numista.com, not on ours.
- **#568** measured the grouping override and discarded it: a curated grouping reaches one of the ten.
- **#572** closed `NOT_PLANNED` rather than let a curated grouping outrank a real Numista series in
  the ladder of ADR 0013. What all ten cards have in common is not the range of that ladder. It is
  the text.

## Decision

### The eight corrections are a curated table in `domain`, keyed by the family string

`familyLabel(family)` answers with the curated label where the table has the string, with
`Sistema monetario YYYY-YYYY` for a technical family (ADR 0012), and with the ficha's own text
everywhere else. It is **a table of corrections and not a catalogue of series**: the cache serves 64
distinct families and most already read as the name a collector would use, so what lives here is the
exception.

It is the same bargain as `cardCountry`: raw stays raw and is read through a function.
`TypeMeta.family` is still what Numista said, the variant key is still keyed on that raw string, and
a correction made today reaches fichas cached years ago without an API call.

### It cures the text, and never the scope or the ladder

`familyLabel` is reached only from `CollectionTitles.nameOf`, after a catalog on the whole variant key
and a grouping on the family have both failed to claim it. So the cure changes what a card **says**
and not what it **holds**: no card splits, none merges, and the ladder #572 refused to move does not
move.

The restraint that follows is the whole of the decision, and two of the ten cases show it:

- **`Austria and its People`** is Numista's umbrella over 18 types of three separate Münze Österreich
  programmes — the castles, the abbeys, and the legends where the collector's «Charlemagne in the
  Untersberg» belongs (series 1582, read 7 September 2026). It takes «Austria y su pueblo», the
  translation of what Numista said, and not «Leyendas de Austria», the programme his coin is actually
  from — which would be a lie the day a castle lands on the same card.
- **The French series 10784** is «Carlomagno» and not «100 francos Carlomagno»: it holds seven types
  and two of them are 500 francs. The anniversary framing of the label goes, because a parenthesis of
  dates and a scope definition do not belong on a line of identity (ADR 0021 §4) — the same cut that
  ADR 0021 made on the Krugerrand's family.

`Millennium` spans four issuers — España, Gibraltar, Jamaica and Nueva Zelanda — which is why it is
«Milenio» and nothing more specific about the 1500 pesetas the collector owns.

### Two of the ten stay as Numista wrote them, and that is what makes this a table

`DC Comics` is a proper name. `Gothic Horror` is the name of a Royal Mint range, and the curated files
already keep a mint's own product name in its own language: «The Queen's Beasts», «The Royal Tudor
Beasts», «St George and the Dragon», «Silver Britannia», «Equilibrium», «Nautical Ounce», «Noah's
Ark». A family that is a range name is nothing else.

So the rule is not «English becomes Spanish». It is: **the prose Numista wrote about a programme is
ours to correct; the programme's own name is not.** The table cures case by case, and it does not
translate.

### Not a heuristic, and no net that says a label needs curing

ADR 0023 refused to detect a language over a third party's prose and carried the finding in the table
instead, with the curator reading the ficha as the only check. That refusal is stronger here, not
weaker: `readsAsACountry` could at least net a parenthesis and a comma against a 40-character
ceiling, while «Gothic Horror» and «Milenio» differ by a judgement about what a mint calls its own
range. There is no clause for that, and inventing one would decide by mechanism the one thing this
project curates by hand.

### The table is netted against what ships, and rot goes red

`CuredFamiliesTest` is the `CardCountriesTest` of this line and checks three things over the cache and
the curated files that actually ship: that every correction still names a series the cache serves,
that no correction is shadowed by a file that has since claimed the family — the death the six
editorial aliases of #22 died — and that no cured name collides with a curated `short_name`.

**Red means there is a label to read again in the source**, not a test to relax. The first of the
three is not hypothetical: #581 was written believing the `Charlemagme` of N#104170 was an errata in
Numista to be fixed on our side, and on 7 September 2026 series 13415 already read «Charlemagne».
Numista had fixed it; one phone's cached ficha had not. The table is keyed on the corrected string,
the stale spelling keeps printing as it came, and it heals when that ficha is asked again (ADR 0025).

### A cured name is not offered to the box the collector names

`CollectionTitles.curatedNames()` stays what it was: the `short_name` of every catalog and grouping.
A cured family names a card only while a coin of that family is in the inventory, which is exactly
the reason ADR 0021 §11 keeps the raw Numista families out of that set — «those move with the
inventory», and a collision arriving later is visible in the index and undone with one tap.

## Consequences

- ADR 0021 §4's «the only labelling rule left in code is `System 1879-1936` → Sistema monetario»
  became two with ADR 0023 and is now **three**. All three format or correct a generated string;
  none renames anything a curator wrote in a file. The six dead family aliases stay dead.
- Screen, plate, export and printed notebook cannot disagree, and it costs nothing to say so: every
  card's name is resolved once for the whole index in `CollectionIndex`, and no export prints a raw
  family. The parity #581 asked about is a property of where the cure sits.
- The day a curated catalog or grouping claims one of these eight families, its `short_name` wins and
  the entry becomes dead weight — and `CuredFamiliesTest` says so instead of leaving it green.
