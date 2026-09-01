# ADR 0028: Prices arrive in one pass, and a total is never shown half-done

- Status: accepted, §1 amended by [ADR 0029](0029-a-wish-is-an-empty-slot-marked-on-the-phone.md) (a
  marked slot lifts both filters of the plan, and the monthly pass stops being a fixed number), §3 and
  §5 amended by [ADR 0030](0030-the-shelf-window-of-explorar-is-valued-by-hand.md) (one plate of the
  shelf window is valued by a gesture, and that price never expires)
- Date: 2026-08-10
- Decides [#327](https://github.com/jenarvaezg/coindex/issues/327), which ADR 0026 §11 deliberately
  left to its own document

## Context

ADR 0026 §10 settled what a piece is worth — **the maximum of three numbers**: its silver floor,
its Numista market price for its grade, and what was paid ([#316](https://github.com/jenarvaezg/coindex/issues/316),
`docs/ux/cifras-316.md`). Two of the three are already on the phone. `weight` and `composition` are
in 100 % of the seeded fichas, so the silver floor costs one spot reading; `price` is a column of
the collection snapshot. The third is not: Numista publishes an estimated price **per issue and per
grade** on `/types/{id}/issues/{issue_id}/prices`, an endpoint the app has never called.

That endpoint is the whole of the design problem, because it is paid in the scarcest resource the
project has. ADR 0003 caps the app at a monthly budget of API calls — `DEFAULT_MONTHLY_BUDGET` is
1.500, and §5 of the API contract allows roughly 2.000 on the free plan. Measured against the
father's real collection (229 rows, 191 types) crossed with the 74 catalogs of `data/`:

| what | calls |
| --- | ---: |
| the **223 issues he owns** | 223 — one `/prices` each, since the `issue_id` is in `collected_items` |
| the **1.182 slots** of the 74 catalogs | **2.036** — 854 `/issues` plus 1.182 `/prices`, because a catalog member stores `numista_type_id` and `year` and **never** the `issue_id` |
| both | **2.259** |

**Valuing every hole in one block does not fit in the month even once.** So the question was never
when to ask for them: it was **which**.

This is also the exact sibling of ADR 0024. That document decided when photographs are prefetched —
in the background, on every launch, on wifi, never during a sync — and the same four questions come
back here with different answers, which is precisely why it could not be a paragraph of ADR 0026.

## Decision

### 1. One pass, and the threshold is §10 applied rather than a saving

> **The valuation pass asks for the issues the collector owns, and for the holes of the plates that
> are ten slots or fewer from closing. 223 + 264 = 487 calls a month, between 24 % and 32 % of the
> budget. No laziness per plate, and no gesture to press.**

The threshold is not thrift. ADR 0026 §10 wrote that the cost of completing a plate is actionable
*per plate* and a reproach once totalled, and the cut is that rule read forwards: **a plate with 51
holes does not have a cost of completion, it has a reproach of 51 slots.** It is not that those
holes are expensive to value; it is that the number that came back could not be shown.

Where the cut falls over his 49 plates (679 slots, 531 holes, 805 calls to value them all):

| slots from closing | plates | holes | calls |
| ---: | ---: | ---: | ---: |
| 1-3 | 12 | 28 | 53 |
| **1-10** | **28** | **138** | **264** |
| 1-15 | 34 | 221 | 363 |
| all | 49 | 531 | 805 |

And it falls clean: what stays outside are the bullion runs where he owns **a single coin** —
Kookaburra 36/37, Libertad 43/44, Capitales de provincia 51/52 — which is exactly where a cost of
completion would be the reproach. What comes inside runs from the single hole of the silver 20
escudos to the ten of the Canadian dollar.

> **Amended on 2026-08-14 (§1, [ADR 0029](0029-a-wish-is-an-empty-slot-marked-on-the-phone.md)). A
> marked slot lifts both filters, and 487 stops being a number.** The threshold above is a rule about
> **whether a number deserves to be shown**, and a wish answers exactly that question: of the 51 holes,
> this one. So a marked slot is priced whatever its plate's shape — past the threshold, and with no
> evidence at all, which is #282's decision 1 narrowed by name to the marked slot. The **plate's**
> second figure is untouched and still governed by `holesAreWithinReach`: a plate of 51 holes with one
> marked has a price inside that hole and no «Coste de cerrar», because one hole is not the cost of
> closing it.
>
> The consequence is the one worth writing down: **«no laziness per plate, and no gesture to press»
> gains a gesture that spends, and the month stops being fixed.** 1-2 calls per wish, for ever, said in
> the gesture and totalled where the budget is already shown. No automatic cap.

### 2. Each phone pays with its own budget, and the seed is not the way out

The route that already exists for fichas — the curator spends their calls, `scripts/seed-type-cache.py`
puts the result in an asset, and it travels in the APK — is **rejected here**. That asset lives in
`data/`, which is a public repository, and [#329](https://github.com/jenarvaezg/coindex/issues/329)
already records that shipping fichas that way breaks §8.4 of the API contract. With prices on top it
would be worse, and it collides with the rule that amounts in euros are never versioned. Repairing
the seeding route is another ticket's problem, not this one's toll.

### 3. Two triggers, and no gesture

There are not two paths. There is **one pass** that works out what is missing or expired and asks
for it, started from two places:

- **On launch, in the background**, like the photograph prefetch of ADR 0024 — so a freshly
  installed app has its figures without anybody syncing, and so a datum that failed once has a
  second route.
- **When a sync finishes**, which is the ceremony that already spends budget and already says what
  it spent.

It runs on every launch because **asking for what is missing is idempotent**: with everything
cached the second launch costs zero calls. The first time in a month is ~487 calls and some two
minutes; the rest of the month, nothing.

Two alternatives were discarded. **An explicit gesture** (`Tasar la colección · 487 llamadas`) adds
a button and a word to a map whose whole purpose is removing prose, and leaves «Las cifras» empty
until somebody remembers. **Valuing when «Las cifras» opens** is two minutes staring at an empty
screen, and is exactly the automatic policy ADR 0025 forbade.

> **Amended on 2026-08-14 (§3, [ADR 0030](0030-the-shelf-window-of-explorar-is-valued-by-hand.md)).
> There is a gesture, it is one plate of the shelf window, and it names its spend before it is
> pressed.** «No gesture» stands for the collection: `Tasar la colección · 487 llamadas` is still
> refused, and the two triggers above are still how everything a collector **owns** is priced.
>
> What the refusal above was weighing does not exist for a plate of the shelf window. There the
> alternative to the gesture is not «later, in the background» — it is **never**: ADR 0029 §4 lifted the
> evidence filter for a *marked* slot alone, so an unmarked plate of the twenty has no route to a price
> at all, and no pass will ever ask for one. So the button is not a shortcut to what a pass would do
> anyway; it is the only door, and the spend behind it is the collector's to decide: **«Tasar esta
> lámina · N consultas»**, per plate, never the twenty at once.
>
> The pass is unchanged and is the one that runs: valuing by hand is one pass over a plan holding that
> plate's holes and nothing else, so there is no second writer and no second price — the rows land in
> the same two tables ADR 0029 §4 already points at.

### 4. Three states, not two

«Numista has no price for this» **is not a failure**, and confusing the two was the risk this
document exists to remove. The shape was already settled in ADR 0024 for the `404` of a photograph:

| answer | what is done |
| --- | --- |
| Numista gives a price | it is stored |
| **Numista answers with no prices** | **stored as a datum** — 19 of the 223 issues; 91 % do carry a price. Without storing it they would be asked for again on every pass, for ever |
| dead network, 5xx, budget exhausted | **no row is written**; the next pass retries. ADR 0025: *«a refresh that fails is never worse than not having asked»* |
| **Numista refusing — `429`, `403`, or five answers in a row that leave no row** | **the pass stops** and says so; what it had already written stands |

> **Amended on 2026-09-01 (#560).** The last row is new, and the bug it closes is what happens without
> it: only the budget and the network stopped a pass, so every other status was read as «this issue has
> no price, carry on». On 11 August 2026 the father's phone spent **1.484 calls in nine passes of the
> same plan** — the same issue asked for nine times — and wrote **zero** rows, because his key was
> answering `Quota exceeded`: Numista's own month is 2.000 calls and the local gate of ADR 0003 is
> 1.500, so a key spent from another phone is refused with budget still on the counter.
>
> The `429` and the `403` are read as the budget always was: the next call would be refused the same
> way. The streak is the general case of the same claim — the pass has 442 calls to spend against a wall
> and no way to name every shape one can take — and it counts **rows written, not statuses**, which is
> what keeps the `404` of row two out of it: an issue Numista has no price for is answered, stored and
> never asked again, so a collection made entirely of those still costs one call each, once.

### 5. Expired is not deleted

Three clocks, different on purpose:

| what | expires after |
| --- | --- |
| a catalog price | **30 days** |
| a «Numista has no price» | **30 days** — it is a datum, and if it never expired, an issue Numista prices tomorrow would never find out |
| the silver spot | **the day** (two keyless calls, outside the budget) |
| a failure | nothing is written |

**And on day 31 the old price is still shown, with the date it was brought.** It is the rule #316
already signed for spot — *«se enseña siempre con la fecha de su última lectura»* — and the same
sentence of ADR 0025 read the right way round: deleting on expiry **is** worse than not having
asked. A phone with no network for months says a total with an old date instead of emptying itself,
and that lies very little: a 3 % swing in silver moves the total by 1,9 %, because the catalogue
rules the mix.

Since they are all brought on the same day by the first pass, they all expire on the same day: the
monthly trickle really is **one batch once a month**. Spreading it out — fetching the oldest few
each day — was discarded: it turns a minute a month into a permanent background call.

> **Amended on 2026-08-14 (§5, [ADR 0030](0030-the-shelf-window-of-explorar-is-valued-by-hand.md)). A
> price asked for by hand does not expire, and it is always shown with its date.** The thirty days above
> are the clock of a price **the pass will ask for again**, and that is what makes the trickle a trickle.
> A plate of the shelf window has no pass coming for it (§3 as amended), so expiry there would mean one
> thing only: the amount disappearing off a screen with no way to refill it.
>
> So the deletion clause is dropped for that one case and the rest of §5 is kept exactly: **the row is
> never deleted, the amount is shown with the date it was brought, and «Volver a tasar» stays on the
> plate for ever.** It is the sentence this section already signed — *«on day 31 the old price is still
> shown, with the date it was brought»* — with nothing left to make it a day 31.
>
> **And a total whose components were read on different days is dated by its oldest.** That case is
> created by the shelf window and the marks together: a plate valued by hand in August with one marked
> casilla refreshed by September's pass is not a total half done (§7) — it is whole, with two ages. One
> date, and the **oldest**, because a date over a total is a promise about all of it
> ([#494](https://github.com/jenarvaezg/coindex/issues/494)).

### 6. The conditions of the pass: ADR 0024's, minus the wifi

| condition | photographs | the pass | why |
| --- | --- | --- | --- |
| on every launch | yes | **yes** | idempotent: the second launch costs zero |
| only on wifi | yes | **no** | what wifi protects there is the **data tariff** (30 MB); what is scarce here is the **budget**, and waiting for wifi does not protect it. It is ~487 JSON responses |
| a sync cancels it | yes | **yes, and more gravely** | both spend the **same** budget: a pass in flight can eat the calls the sync needs and make it fail with `BudgetExhausted` |
| an export stands it down | yes | **yes** | it takes the network from something the collector is waiting for |
| ceiling per pass | no | **no** | 487 of 1.500 is not a burst worth staging, and staging it is four launches before there is any money |
| silent | yes | **yes**, with the settings line that already exists |
| with no API key | — | **does not run** | that is the freshly installed app before onboarding, not an error to discover |

With the budget exhausted the pass stops and writes nothing: the money section does not appear and
settings says why.

### 7. The total is never shown half-done

**The page opens whole and the money arrives late — but the total is never shown half-done.**

This is not prudence. A partial total reintroduces through the back door what #316 discarded:
without the market, the value is `max(silver, paid)` ≈ 10.500 € against the real 16.800, which is
literally *«enseñar sólo el suelo de plata»* — rejected there because *«diría que la colección vale
menos de lo que cualquiera puede comprobar en el propio Numista»*. A total at 60 % is not
incomplete, it is **false**, and it corrects itself upwards while you look at it.

1. **Without a single call to Numista, «Las cifras» is not empty**: the weight, the matter, the
   three ladders of referents, the arc of years, the emitters and the size all come whole out of the
   APK. The local-first promise of ADR 0024 extends to this page.
2. **While the pass runs, the money section is not there.** No struck-through number, no provisional
   total. It is the same silence as ADR 0024, and settings is already the place where one line tells
   «they are missing and falling» from «they are missing because there is no network».
3. **Complete, the total says its coverage and not its progress.** «Llevo 140 de 223» is a progress
   and is not said; «el valor de N de tus 574 piezas» is a coverage and is. Today that sentence has
   no subtraction to make — the maximum of the three sources covers 100 % of the 574 (#326) — but the
   rule is written for the day a piece arrives that no source covers.

### 8. The grade is the pricing key, and a hole is valued in `unc`

A piece of his is valued **in its grade** (`grade` is at 100 %), with the neighbouring grade when
its own has no price: 188 exact, 22 neighbouring, 19 with none (#316). **A hole is valued in `unc`**,
which is what #326 already used to measure the 14 plates within reach.

### 9. The spot is two keyless calls and is not seeded

`https://api.gold-api.com/price/XAG` for the troy ounce in dollars, and
`https://api.frankfurter.dev/v1/latest?base=USD&symbols=EUR` for the ECB rate. Neither is
`api.numista.com`, so **neither is counted against the budget of ADR 0003** — the same distinction
ADR 0024 makes for CDN photographs, and it must not start being counted as one.

**The spot is not seeded into the APK.** The last one read is stored with its date. A seeded spot
would only buy the silver floor of a piece opened with no network, and the silver floor alone is
precisely the figure we do not want to show on its own.

## Consequences

`issue_id` **is already on the phone**, which is the one thing #327 expected to cost a migration. It
recorded that `IssueDto` parses only `year` and `gregorian_year`, and that without the id every piece
would cost an extra call to `/types/{id}/issues` just to find out. Since then `Mappers.issueIdFromRaw`
reads it out of the stored response body, which `SyncService` keeps verbatim for exactly this reason:
every piece already synced carries its issue id with no migration and no API call. What this ADR adds
to the schema is the price cache and the spot, and nothing about the collection snapshot.

The pass runs in the ViewModel's scope, like the photograph prefetch and for the same reason: every
issue is independent, what is written is one row per issue, and being cut short when the collector
leaves costs only the calls not yet made. They are made on the next launch.

Two things this deliberately does not do. It does not keep a **history** of anything — no spot
series, no evolution of the total, no aggregate of return — because wealth management stays outside
(ADR 0026 §10), and a table of daily spots is how it would arrive without a decision. And it does
not value **every** hole, so the cost of completing a plate exists for 28 of his 49 plates and is
absent, rather than approximate, on the other 21.

The euro amounts of all this are **not in this document, and are never versioned**: this repository
is public. The method, the coverages and the proportions are here and in `docs/ux/cifras-316.md` and
`docs/ux/cifras-326.md`; the amounts live in `/private/tmp/coindex-privado/`.
