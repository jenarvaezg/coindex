# ADR 0022: Commemorative programmes, a second reading of the same coin

- Status: accepted
- Date: 2026-08-04

## Context

Curating the Portuguese circulating commemoratives (#157) produced a criterion and a request at
the same time. The collector gave the criterion himself, and it is a single one:

> Yo lo montaría con un criterio principal único: denominación + metal, y dejaría la
> conmemoración como subserie temática. […] Así puedes tener ambas lecturas sin mezclar
> criterios: la moneda pertenece a su lista por denominación, pero también completa —o no— un
> conjunto temático.

The first half is what every catalog in the project already does, and four new ones came out of
it. The second half asks for something the model had never been asked for: the 2,50 escudos of
1977 belongs to «los 2,50 escudos de cuproníquel», **and** it completes, or not, the three
denominations the mint struck for the centenary of Alexandre Herculano's death. Both readings are
true, and they cut across each other — the programme spans denominations, the catalog spans years.

Three mechanisms already in the tree looked like they might carry it, and none does:

- **A set catalog** (ADR 0012) is exempt from the single-claim check, so a set and an ordinary
  catalog may name the same type without startup failing. But `deriveCollection` resolves the
  family «in strict order of how specific the claim is», and **a set goes first**. Writing the
  programme as a set would not add a second membership to the coin: it would *move* it off the
  denomination card, which is the opposite of the request, and leave six cards where there are
  four — the very complaint #157 opened with.
- **A curated grouping** (ADR 0013) supplies only a family, and loses to any catalog naming the
  type. It cannot count anything.
- **A field on the member**, joining catalogs by a shared programme id, was the cheapest idea and
  it cannot reach the right number. Both Portuguese programmes are three coins, and the third is
  a 25 escudos that sits in **no catalog and should not**: neither collection owns one of the ten
  cupronickel 25 escudos, so that file would ship with no card and no plate, existing only to
  feed the mechanism. Joining catalogs would have printed «1 de 2» over a programme of three.

## Decision

### A commemorative programme is a curated file, and not a collection

`data/programmes/*.json`, parsed and validated at startup like the catalogs and the groupings,
fatal on a bad file for the same reason. It declares no family, no weight, no finish and no metal;
it never reaches `deriveCollection`; it produces **no card in the index**. That is what makes it
able to coexist with the denomination catalog instead of competing with it.

Its members are **types, not slots**, and they are deliberately not bounded by what the catalogs
hold. A programme names what the mint struck for one commemoration, whether or not this project
ever plates those coins.

### The programme is what the denominator counts

`owned / members`, over the programme's own list. Every member counts: unlike a plate's
denominator (ADR 0020), nothing here is unmeasurable, because a programme names published types
only — there is no announced or unlisted state to exclude.

This is the whole point of the file existing. «1 de 3» is true of the programme; «1 de 2» would
have been true of nothing.

### The boundary is never a Numista fact, so `source` is any host and `source_note` is required

A catalog's `source` accepts a Numista series or type page (ADR 0020). A programme's accepts any
HTTPS URL and **requires** the prose note — not optional, as it is one level up. Numista files
these types under a technical monetary system and groups them no further, so demanding a Numista
URL would have forced a citation that proves nothing about the boundary.

Both shipped programmes cite a dealer offering the complete three-coin *carteira*, and both notes
say plainly that the mint's own catalogue was not consulted. That is weaker than a decree and it
is what was actually read.

### A programme of one member is refused

It would say nothing the coin's own ficha does not, and it would print «1 de 1» beside a coin the
collector already has. Two is the floor, checked in the validator.

### It shows up on the plate, because that is the screen that exists

The plate's specification block gains a `Programa` line per programme the catalog touches, after
its own progress and never mixed into it. The collector reading «los 2,50 escudos de cuproníquel ·
1 / 3 emisiones» also reads «Serie Alexandre Herculano 1977 · 1 de 3».

The place the collector's own preview put it — beside the coin itself — **does not exist yet**:
«Coins» is ADR 0021 §12 and is not built. When it is, the second reading belongs there too, and
the domain already answers it (`CommemorativeProgramme.claims`).

### A programme's `short_name` stays out of the cross-species name check

`validateShortNamesAcross` exists because the index draws catalogs and groupings side by side and
indistinguishably (#22). A programme is not a card, so it never sits beside them and cannot be
confused with one there. Uniqueness among programmes is enough, and that is checked.

## Consequences

- **Two files ship**: the 1977 Alexandre Herculano series and the 1983 World Food Day (FAO)
  series, each three denominations — 2,50, 5 and 25 escudos of cupronickel. Measured against both
  private captures, the father reads **1 de 3** in each and Jose **0 de 3**.
- **The type cache grows by the coins no catalog claims.** `curatedTypeIds()` now includes
  programme members, so N#7338 and N#9831 were seeded: they are exactly the coins «1 de 3» says
  are missing, and `TypeCacheSeedTest` keeps them honest.
- **No database migration, no new API surface in the app, and nothing stored per collector.** A
  programme is read-only editorial data, like every curated file.
- **The index is untouched**: 59 cards for the father before this ADR and 59 after.
- A programme could equally be curated for a sequence with no catalog at all. Nothing in the
  design prevents it, and nothing in this ADR encourages it: the two that exist came from a
  collector's explicit reading of coins he owns.

## Alternatives considered

- **A set catalog per programme.** Rejected above: a set wins the family precedence, so it moves
  the coin instead of adding a reading, and it multiplies cards.
- **A `programme_id` on the catalog member.** Rejected above: the third coin of both programmes is
  in no catalog, so the denominator would have been wrong by one in both.
- **Curating the ten cupronickel 25 escudos as a catalog** to give the join its third member.
  Rejected: neither collection owns one, so it fails the existence criterion of ADR 0020 and would
  ship a file with no card and no plate, purely to serve a mechanism.
- **A programme as a real card in the index**, so a coin appears in two collections. Rejected for
  now, not on principle: it needs a precedence decision the family ladder does not have today, and
  it would print six cards where #157 reduced five to four. If a third collector or a wider set of
  programmes arrives, this is the door to reopen.
- **Making the count include only catalogued members** and printing «1 de 2». Rejected: the
  collector trusts «me falta» to mean a coin exists, and the inverse — a denominator that hides a
  coin that exists — is the same lie from the other side.
