# ADR 0028: Prices arrive in one pass, and a total is never shown half-done

- Status: accepted; amended by [ADR 0029](0029-a-wish-is-an-empty-slot-marked-on-the-phone.md)
  (§1), [ADR 0030](0030-the-shelf-window-of-explorar-is-valued-by-hand.md) (§3, §5),
  [#560](https://github.com/jenarvaezg/coindex/issues/560) (§4),
  [#579](https://github.com/jenarvaezg/coindex/issues/579) (§4),
  [#600](https://github.com/jenarvaezg/coindex/issues/600) (§4),
  [#561](https://github.com/jenarvaezg/coindex/issues/561) (§5),
  [#594](https://github.com/jenarvaezg/coindex/issues/594) (§5),
  [#521](https://github.com/jenarvaezg/coindex/issues/521) (§6, §7) and
  [ADR 0032](0032-the-inventory-has-its-own-clock-and-its-own-reserve.md) (§3, §6)
- Date: 2026-08-10
- Decides [#327](https://github.com/jenarvaezg/coindex/issues/327), which ADR 0026 §11 left to its
  own document

## Context

ADR 0026 §10 values a piece at the maximum of its silver floor, its Numista price for its grade, and
what was paid ([#316](https://github.com/jenarvaezg/coindex/issues/316), `docs/ux/cifras-316.md`).
The first and last are already on the phone. The Numista price comes per issue and grade from
`/types/{id}/issues/{issue_id}/prices`, paid from the monthly budget of ADR 0003
(`DEFAULT_MONTHLY_BUDGET` is 1.500; the free plan allows about 2.000). Over the father's collection
(229 rows, 191 types) and the 74 catalogs of `data/`:

| what | calls |
| --- | ---: |
| the **223 issues he owns** | 223, one `/prices` each, since the `issue_id` is in `collected_items` |
| the **1.182 slots** of the 74 catalogs | **2.036**: 854 `/issues` plus 1.182 `/prices`, because a catalog member stores `numista_type_id` and `year` and never the `issue_id` |
| both | **2.259** |

Valuing every hole does not fit in a month even once, so the question is which holes. The other
questions are those ADR 0024 answered for photographs, with different answers.

## Decision

### 1. One pass, and the threshold is §10 applied rather than a saving

> **The valuation pass asks for the issues the collector owns, and for the holes of the plates that
> are ten slots or fewer from closing. 223 + 264 = 487 calls a month, between 24 % and 32 % of the
> budget. No laziness per plate, and no gesture to press.**

A number that says nothing is not shown: the cost of completing a plate is actionable per plate and
a reproach once totalled (ADR 0026 §10), so the holes of a plate far from closing are not valued
(`HOLE_THRESHOLD_SLOTS`). Over his 49 plates:

| slots from closing | plates | holes | calls |
| ---: | ---: | ---: | ---: |
| 1-3 | 12 | 28 | 53 |
| **1-10** | **28** | **138** | **264** |
| 1-15 | 34 | 221 | 363 |
| all | 49 | 531 | 805 |

What stays out are the bullion runs where he owns a single coin (Kookaburra 36/37, Libertad 43/44).

> **Amended on 2026-08-14 by ADR 0029 §4.** A marked slot is priced past the threshold and with no
> evidence. «Coste de cerrar» still follows `holesAreWithinReach`, so one marked hole on a plate of
> 51 has a price and the plate has no «Coste de cerrar». The monthly pass is no longer fixed: 1-2
> calls per wish, named in the gesture, with no automatic cap.

### 2. Each phone pays with its own budget, and the seed is not the way out

Prices do not travel in the APK as fichas do: the asset lives in a public repository, shipping
fichas that way already breaks §8.4 of the API contract
([#329](https://github.com/jenarvaezg/coindex/issues/329)), and euro amounts are never versioned.

### 3. Two triggers, and no gesture

One idempotent pass asks for what is missing or expired. It starts **on launch, in the background**
(like the prefetch of ADR 0024) and **when a sync finishes**. With everything cached a launch costs
zero; the first one of a month costs ~487 calls and about two minutes.

Discarded: `Tasar la colección · 487 llamadas`, a button that leaves «Las cifras» empty until
somebody remembers; and valuing when «Las cifras» opens, which is two minutes of empty screen and
the automatic policy ADR 0025 forbade.

> **Amended on 2026-08-14 by ADR 0030 §3.** One plate of the shelf window is valued by a gesture,
> «Tasar esta lámina · N consultas», because no pass will ever ask for an unmarked one. A gesture
> that spends prints its ceiling before it is pressed. It runs this same pass over that plate's
> holes.
>
> **Amended on 2026-09-30 by ADR 0032 §2.** The launch also refreshes the inventory once a day, one
> second before the pass, which holds for it.

### 4. Three states, not two

| answer | what is done |
| --- | --- |
| Numista gives a price | it is stored |
| **Numista answers with no prices** | **stored as a datum** (19 of the 223 issues; 91 % do carry a price). Otherwise they would be asked for again on every pass |
| dead network, 5xx, budget exhausted | **no row is written**; the next pass retries. ADR 0025: *«a refresh that fails is never worse than not having asked»* |
| **Numista refusing: `401`, `403`, `429`, or five answers in a row that leave no row** | **the pass stops** and says so; what it had already written stands |

«No price» is a datum, not a failure, as ADR 0024 treats a photograph's `404`.

> **Amended on 2026-09-01 (#560): the last row.** Before it, every status but budget and network
> read as «no price»: on 11 August 2026 the father's phone spent 1.484 calls in nine passes and
> wrote nothing, because his key, also spent from another phone, answered `Quota exceeded`. The
> streak counts rows written, not statuses, so «no price» answers never feed it; a listing neither
> feeds it nor resets it.

> **Amended on 2026-09-07 (#579): the wall is remembered** (`RejectionWall`), because each new pass
> paid to rediscover it, and `CallBudgetGate.reserve()` counts a call before it is sent:
>
> | what came back | per pass | a day (~8 launches) | a month |
> | --- | ---: | ---: | ---: |
> | `429` / `403` / `401` | 1 call | 8 | ~240 |
> | a run of five | 5 calls | 40 | ~1.200 |
>
> | what Numista answered | until when | why |
> | --- | --- | --- |
> | `429` naming the quota | the 1st of the next month | Numista's month is the same calendar month `startOfMonthMillis` draws for the gate |
> | `401` or `403` | until the key changes | waiting does not fix a credential. `StoredCredentials.save` takes it down, and the door to «Credenciales» is printed in this state (§6.1). A sync that gets through takes it down too (#600) |
> | any other `429` | 6 hours | the throttle means «not now», not «not this month» |
> | a run of five | 6 hours | the cause is unknown, so it gets the shortest life |
>
> The collector still reads one sentence for all four (§6.1). A pass that reaches Numista takes the
> wall down, and an expired wall does not block the next pass.

> **Amended on 2026-09-30 (#600): the first two rows were swapped.** The OpenAPI 3.36 contract
> declares the quota as a `429` on the five routes this app uses, and the `403` only on two paid
> routes it never calls; on 14 August 2026 `/types/{id}` answered `HTTP 429 «Quota exceeded»`. Quota
> and throttle share the status, so `rejectionCauseFor` reads the body, and doubt resolves to the
> throttle (six hours lost, against a month). An exhausted month no longer says «vuelve a intentarlo
> dentro de un rato».

### 5. Expired is not deleted

| what | expires after |
| --- | --- |
| a catalog price | **90 days** (30 until [#561](https://github.com/jenarvaezg/coindex/issues/561)) |
| a «Numista has no price» | **90 days** (30 until #561; same row, same clock). It is a datum, and if it never expired an issue Numista prices tomorrow would never be found |
| the silver spot | **the day** (two keyless calls, outside the budget) |
| a failure | nothing is written |

After expiry the old price is still shown, with the date it was brought: the rule #316 set for the
spot (*«se enseña siempre con la fecha de su última lectura»*). A phone offline for months shows an
old date rather than an empty page, and a 3 % swing in silver moves the total by 1,9 %.

One pass fetches everything, so everything expires as one batch per life. Staggering was rejected:
it turns a minute per batch into a permanent background call.

> **Amended on 2026-08-14 by ADR 0030 §4.** A price asked for by hand never expires, since no pass
> will refresh it: it is shown with its date and «Volver a tasar» stays on the plate. A total whose
> parts were read on different days is dated by its oldest
> ([#494](https://github.com/jenarvaezg/coindex/issues/494)).

> **Amended on 2026-09-07 (#561): ninety days**, the life of the listing that addresses the price.
> The market is followed by the spot (§9); a catalog price moves at the catalogue's pace, and thirty
> days only produced a monthly peak. A cold pass, measured on 16 August 2026:
>
> | what | calls |
> | --- | ---: |
> | the issues he owns | 231 |
> | the holes of plates within reach | 115: 10 whose curated file names the issue, 105 through a listing |
> | the types still to list | 96 |
> | **a cold pass** | **442** |
>
> 442 of 2.000 needs no staggering. Only the shelf window's figures carried their date, which became
> #594.

> **Amended on 2026-10-05 (#594): every figure the pass feeds carries the date of its catalogue
> half.**
>
> - Two clocks, two clauses, never averaged: `plata: 28,40 €/oz · hoy 11:52 · Numista: el 21 jun
>   2026`. Each clause is dated by its oldest read (#494).
> - The clause says «Numista», not «el catálogo», which already labels the curated file's edition
>   (#518).
> - The spot clause keeps its price: it is the only way to check the metal floor (#326, #398).
> - A plate's header dates each line, because «Valor actual» and «Coste de cerrar» are separate
>   totals and a marked casilla is repriced the day it is marked (ADR 0029 §4).
> - A component counts as read when this phone asked about it, not when its catalogue price won
>   (ADR 0030 §6): a date may only err older. With nothing ever asked, the clause is absent.
> - The stamp in a casilla stays bare (#493). The notebook prints «Tasación · 21 jun 2026» under
>   «Valor», always as a full date.
> - Still undated: a marked casilla past the threshold of §1, and its row in «Lo que busco».

### 6. The conditions of the pass: ADR 0024's, minus the wifi

| condition | photographs | the pass | why |
| --- | --- | --- | --- |
| on every launch | yes | **yes** | idempotent: the second launch costs zero |
| only on wifi | yes | **no** | wifi protects the data tariff (30 MB of photographs); what is scarce here is the budget, which waiting for wifi does not protect. It is ~487 JSON responses |
| a sync cancels it | yes | **yes, and more gravely** | both spend the same budget: a pass in flight can eat the calls the sync needs and make it fail with `BudgetExhausted` |
| an export stands it down | yes | **yes** | it takes the network from something the collector is waiting for |
| ceiling per pass | no | **no** | 487 of 1.500 is not a burst worth staging, and staging it means four launches before any money shows |
| silent | yes | **yes**, with a line on «Este teléfono» (§6.1) |
| with no API key | — | **does not run** | that is a freshly installed app before onboarding, not an error |

With the budget exhausted the pass writes nothing, the money section does not appear, and «Este
teléfono» says why.

> **Amended on 2026-09-30 by ADR 0032 §1.** The pass's endpoints stop `INVENTORY_RESERVE` (300)
> short of the monthly cap, so a sync always fits.

**§6.1, added by [#521](https://github.com/jenarvaezg/coindex/issues/521): the line is read, and the
pass has no handle.** The line lives on «Este teléfono».

1. **It does not become a button.** Four of its six states mean «wait», and the pass already runs on
   every launch, so a handle would only spend the key's budget twice (#562).
2. **Two states open a door to `Credenciales`**: `NoApiKey` and `Rejected`, the only causes the
   collector can fix (ADR 0026 §14). The door is a row, printed only in those two states.

### 7. The total is never shown half-done

**The page opens whole and the money arrives late, but the total is never shown half-done.** Without
the market price the total is `max(silver, paid)`, the silver floor alone that #316 rejected because
it *«diría que la colección vale menos de lo que cualquiera puede comprobar en el propio Numista»*.
A partial total is false, not incomplete.

1. **Without a call to Numista, «Las cifras» is not empty**: weight, metal, referents, years,
   issuers and size come from the APK, as ADR 0024 promises.
2. **While the pass runs, the money section is absent.** No provisional total; the line on «Este
   teléfono» says whether the prices are on their way or held up by the network.
3. **Complete, the total states its coverage, not its progress**: «el valor de N de tus 574 piezas»,
   never «llevo 140 de 223». Today every piece is covered (#326); the rule is for the day one is
   not.

### 8. The grade is the pricing key, and a hole is valued in `unc`

A piece is valued in its own grade, or the neighbouring one when its own has no price (#316: 188
exact, 22 neighbouring, 19 with none). A hole is valued in `unc`, as #326 measured the plates within
reach.

### 9. The spot is two keyless calls and is not seeded

`https://api.gold-api.com/price/XAG` (troy ounce in dollars) and
`https://api.frankfurter.dev/v1/latest?base=USD&symbols=EUR` (ECB rate). Neither is
`api.numista.com`, so neither counts against the budget of ADR 0003, like the CDN photographs of
ADR 0024. The last reading is stored with its date and none is seeded: a seeded spot would only show
the silver floor alone, which §7 refuses.

## Consequences

- `issue_id` needs no migration: `Mappers.issueIdFromRaw` reads it from the response body
  `SyncService` stores. The schema gains the price cache and the spot.
- The pass runs in the ViewModel's scope: each issue writes its own row, so being cut short costs
  only the calls not yet made.
- No history: no spot series, no evolution of the total. Wealth management stays outside
  (ADR 0026 §10).
- The cost of completing a plate exists for 28 of his 49 plates, and is absent, not approximate, on
  the rest.
- Euro amounts are never versioned: this repository is public. The method and proportions are here
  and in `docs/ux/cifras-316.md` and `docs/ux/cifras-326.md`; the amounts live in
  `/private/tmp/coindex-privado/`.
