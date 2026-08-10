# ADR 0022: Commemorative programmes, a second reading of the same coin

- Status: accepted
- Date: 2026-08-04

> **Amended on 2026-08-10 (#387).** Nothing below changes. What arrives is the first programme that
> is not one mint's denominations: `serie-iberoamericana-i-encuentro-de-dos-mundos` is **fourteen
> countries, one slot each** — the coins the FNMT proposed, coordinated and struck in Madrid for the
> 1492-1992 quincentenary, 27 g of silver and 40 mm on the real de a ocho module.
>
> Three fields were written assuming a single mint, and all three keep their shape, because none of
> them reaches a screen — the plate prints `short_name` and the count and nothing else.
> `issuer_code` names **the mint that coordinated the programme** (`espagne`) and not the issuer of
> its members, which are fourteen. `year` names **the year the series was issued** (1991) and not a
> single date, since Portugal's coin is dated 1992 and Mexico's carries 1991 and 1992. And a
> member's `label` reads as a **country** rather than a denomination. Widening either field into a
> list would rename it in the three shipped files to describe something no screen shows, which is
> the same trade ADR 0020 refused for `issuer_codes`.
>
> Two things this file confirms rather than changes. **The boundary is never a Numista fact**, and
> here Numista proves it against itself: series 4138 «Ibero-American» holds 130 fichas of thirteen
> series (1991-2024) and of the first one it **lacks** Argentina, both Portuguese types and Peru's
> vertical variety, and files Cuba's under «Christopher Colombus» (#389). What draws the line is the
> FNMT plus the coin itself, whose obverse rings its own arms with **the thirteen shields of the
> other countries** — counted one by one on the photograph. And it is still a second reading of a
> coin that already has a card: its Portuguese member is the 1992 slot of
> `portugal-1000-escudos-plata-500`, so that plate is the only screen where the count appears. The
> thematic **catalog** of the same fourteen coins is what the collector would rather have, and it is
> blocked on #149 — that Portuguese coin would then sit on two plates at once, which is a `PARAR`
> and not a precedence to resolve at runtime.
>
> One consequence for measuring, which is why «one slot per country» could be decided at all: a
> programme's `owned / total` appeared in **no** report, and this one has a member hanging off a
> derived card that prints no specification block, so it was unmeasurable from the field. The report
> now prints a «PROGRAMAS CONMEMORATIVOS» section. Measured the same day over the two private
> captures: the father reads **2 de 14** and Jose **0 de 14**, with both indexes untouched — 68
> cards and 31 unclassified for the father, 43 and 10 for Jose, identical with the file present and
> absent. The promise that a programme produces no card, remeasured over a file that spans fourteen
> countries.
>
> **Same day, all thirteen (#387).** The collector asked for the whole programme, so the twelve
> remaining series ship too: II 1994 to XIII 2024, **138 slots over sixteen countries** in thirteen
> files. Three things the family settled that one file could not.
>
> **A programme may ship invisible, on purpose.** Only the first four touch a plate — Portugal's coin
> for I, II, III and IV is a 1000 escudos of silver .500 that `portugal-1000-escudos-plata-500`
> already claims, and the father owns all four — so that one plate now carries **four** `Programa`
> lines, and the nine files from V to XIII appear on no screen at all: nobody owns a piece and no
> catalog names a type. ADR 0022 refused a file that exists «purely to serve a mechanism»; this is
> not that refusal reversed but its condition changed, and by the collector on 10 August 2026 with the
> measurement in front of him: «no pasa nada si viaja, tengo planes a futuro». What the files owe in
> exchange is saying so in `source_note`, each one, in those words.
>
> **When a country's coin has several fichas, the slot is the one that circulated.** Portugal strikes
> every one of these as a circulating commemorative — .500 silver, or cupronickel from 2012 — *and* as
> the .925 proof that goes in the FNMT case at the series' exact metric. Naming the case piece would
> have been the tidier claim and it would have told the father he owns none of the thirteen, over four
> coins in his drawer: hiding a coin he has is the same lie as inventing one he lacks, which is this
> ADR's own argument for counting the 25 escudos. So the slot is the circulating piece, the .925 is
> named in the note, and where what circulated is cupronickel (IX, X, XIII) the slot moves to the
> silver, because the series is a silver programme.
>
> **A slot is a country, not a coin type.** México left the coin series after VIII and the FNMT still
> counts it in IX, where its piece is a *medal* (N#55253, 27 g of .925); the FNMT's own commemorative
> medal is a slot in eleven of the thirteen because the mint defines the collection as «N monedas y
> una medalla». A programme names what was struck for the commemoration, not what is legal tender.
>
> And Numista's series remains a proposal and not a boundary, now measured across all thirteen: of the
> 138 slots its series 4138 is missing **eighteen** — every one of the twelve Argentine coins, the four
> Portuguese escudos of I to IV, Mexico's IX medal and Paraguay's XIII — and it files others under the
> wrong subseries, including a 2003 proof labelled «XII Serie» (#389).

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
