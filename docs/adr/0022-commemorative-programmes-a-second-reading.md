# ADR 0022: Commemorative programmes, a second reading of the same coin

- Status: accepted; amended by #387 (the Ibero-American Series)
- Date: 2026-08-04

> **Amended on 2026-08-10 (#387).** The thirteen Ibero-American Series (I 1991 to XIII 2024) ship as
> thirteen programmes: 138 slots over sixteen countries, coordinated by the FNMT. What they add:
>
> - **A slot is a country**, not a denomination or a coin type: Mexico's IX slot is a medal
>   (N#55253), and the FNMT's own medal is a slot in eleven series, because the mint defines each as
>   «N monedas y una medalla».
> - **Three fields change reading, not shape**: `issuer_code` is the coordinating mint (`espagne`),
>   `year` the year the series was issued (members may be dated later), and a member's `label` its
>   country. No screen shows them, so they are not widened into lists (as ADR 0020 refused for
>   `issuer_codes`).
> - **Where a country has several fichas, the slot is the one that circulated.** Portugal also
>   struck a .925 proof for the FNMT case; naming it would tell the father he owns none of the four
>   he has. The proof is named in `source_note`; where what circulated is cupronickel (IX, X, XIII),
>   the slot is the silver piece.
> - **The boundary is still not Numista's**: its series 4138 misses 18 of the 138 slots and misfiles
>   others (#389). It comes from the FNMT and from the coin (series I carries the shields of the
>   other thirteen countries).
> - **A programme may ship with no screen.** Only I-IV touch a plate: Portugal's member is claimed
>   by `portugal-1000-escudos-plata-500`, which carries four `Programa` lines. V-XIII appear
>   nowhere; the collector chose to ship them anyway («no pasa nada si viaja, tengo planes a
>   futuro»), and each `source_note` says so.
> - A thematic catalog of the same coins is blocked on #149: the Portuguese coin would sit on two
>   plates, a `PARAR` and not a runtime precedence.
> - The field report gains a «PROGRAMAS CONMEMORATIVOS» section.

## Context

Curating the Portuguese circulating commemoratives (#157), the collector asked for two readings:

> Yo lo montaría con un criterio principal único: denominación + metal, y dejaría la
> conmemoración como subserie temática. […] Así puedes tener ambas lecturas sin mezclar
> criterios: la moneda pertenece a su lista por denominación, pero también completa —o no— un
> conjunto temático.

The 2,50 escudos of 1977 belongs to «los 2,50 escudos de cuproníquel» and also completes, or not,
the three denominations struck for the centenary of Alexandre Herculano's death. No existing
mechanism carries that:

- A **set catalog** (ADR 0012) wins the family precedence in `deriveCollection`, so it would take
  the coin off its denomination card and add cards where #157 wanted fewer.
- A **curated grouping** (ADR 0013) only supplies a family, and counts nothing.
- A **programme id on catalog members** gets the count wrong: the third coin of both Portuguese
  programmes, a cupronickel 25 escudos, is in no catalog and should not be, so the join would print
  «1 de 2» over a programme of three.

## Decision

### A commemorative programme is a curated file, and not a collection

`data/programmes/*.json`, validated at startup like catalogs and groupings, fatal on a bad file. It
declares no family, weight, finish or metal, never reaches `deriveCollection` and produces no card.
Its members are Numista types, whether or not any catalog plates them.

### The programme is what the denominator counts

`owned / members` over the programme's own list. Members are published types only, so unlike a
plate (ADR 0020) nothing is excluded as unmeasurable.

### The boundary is never a Numista fact, so `source` is any host and `source_note` is required

Numista files these types under a technical monetary system, so a Numista URL would prove nothing
about the boundary. A programme's `source` is any HTTPS URL and `source_note` is mandatory. The two
Portuguese programmes cite a dealer selling the full three-coin *carteira*, and say the mint's own
catalogue was not consulted.

### A programme of one member is refused

It would print «1 de 1» beside a coin the collector has. The validator requires two.

### It shows up on the plate, because that is the screen that exists

The plate's specification block gains a `Programa` line per programme the catalog touches, after the
plate's own progress: «Serie Alexandre Herculano 1977 · 1 de 3». When «Coins» (ADR 0021 §12) is
built, the reading belongs beside the coin too; `CommemorativeProgramme.claims` already answers it.

### A programme's `short_name` stays out of the cross-species name check

That check exists because the index shows catalogs and groupings side by side (#22), and a programme
is not a card. Only uniqueness among programmes is checked, also in `Curation` since #545.

## Consequences

- Two files ship: Alexandre Herculano 1977 and World Food Day (FAO) 1983, each the 2,50, 5 and 25
  escudos of cupronickel.
- `curatedTypeIds()` includes programme members, so the cache seeds coins no catalog claims (N#7338,
  N#9831); `TypeCacheSeedTest` checks them.
- No migration, no new API surface, nothing stored per collector, and no change to the index.

## Alternatives considered

- **A set catalog per programme**, or **a `programme_id` on catalog members**: rejected in Context.
- **Curating the ten cupronickel 25 escudos as a catalog** to complete the join: neither collection
  owns one, so it fails ADR 0020's existence criterion and would exist only to serve a mechanism.
- **A programme as a card in the index**: it needs a precedence the family ladder does not have, and
  adds cards. To reopen if more collectors or programmes arrive.
- **Counting only catalogued members** («1 de 2»): «me falta» must mean a coin that exists, and a
  denominator that hides one breaks that.
