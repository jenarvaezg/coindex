# ADR 0032: The inventory has a clock of its own, and a reserve nobody else can spend

- Status: accepted
- Date: 2026-09-30
- Decides [#605](https://github.com/jenarvaezg/coindex/issues/605). Adds a second ceiling to the
  budget of [ADR 0003](0003-atomic-api-budget-reservations.md) and a fourth trigger to the launch of
  [ADR 0024](0024-photographs-are-prefetched-on-unmetered-networks.md) and
  [ADR 0028 §3](0028-prices-arrive-in-one-pass-and-a-total-is-never-half-done.md)

## Context

What the collector opens Coindex for is to see the coin he bought on its plate. That takes one
call — `/users/{id}/collected_items`, unpaginated since ADR 0006, plus the token — and it is the
only thing in the app that nothing asks for on his behalf.

The `api_call_log` of both phones, read on 30 September 2026 out of `.local/padre` (captured on the
1st) and `.local/jose` (captured on the 7th):

```
padre    jul: 17    ago: 1500    sep: 441 (345 precios + 96 listados)
jose     jul:  8    ago: 1500    sep:  67
```

Two things are in those numbers.

**The father's last `/users/{id}/collected_items` is dated 10 August 2026.** His syncs are 30 July,
31 July, 2 August, 3 August, 9 August, 10 August, and then none at all in the twenty-two days up to
his capture, over a phone he opens about eight times a day. The gesture exists, he knows where it
is — he found it six times in twelve days — and then he stopped, and nothing in the app noticed or
minded.

**And for twenty of those days he could not have synced if he had tried.** On 11 August his phone
spent 1.484 consultas against the same plan (the bug of [#560](https://github.com/jenarvaezg/coindex/issues/560)),
reached `DEFAULT_MONTHLY_BUDGET` exactly, and from that moment `CallBudgetGate` refused
**everything** — including the two consultas that would have told him what he owned. Both phones
ended August on 1.500 on the nose.

Laid out, the four clocks were the wrong way round:

| what | who asks for it | how often | cost |
| --- | --- | --- | ---: |
| **inventory** (`collected_items`) | only the button on «Este teléfono» | never on its own | **2** |
| fichas of new types | the same button, only the missing ones | idem | 1 per type |
| a cached ficha | the collector's gesture (ADR 0025) | never | 1 |
| **prices** | automatic, on every launch | automatic | **442** cold, ~0 after |
| the catalogs of `data/` | they travel in the APK | every release | 0 |

The only thing that refreshed itself was the one that costs 442, and it shared one undivided purse
with the one that costs 2.

## Decision

### 1. The valuation pass stops 300 consultas short of the cap

> **`CallBudgetGate` has two ceilings. `/types/{id}/issues` and `/types/{id}/issues/{issue_id}/prices`
> may spend up to `DEFAULT_MONTHLY_BUDGET - INVENTORY_RESERVE`; everything else may spend the whole
> budget. `INVENTORY_RESERVE` is 300.**

The number is what a month of the cheap things costs, not a proportion that reads well: 60 for a
daily inventory, a handful for the fichas of what he buys, one per gesture of ADR 0025. It leaves
the pass 1.200, which is two and a half cold months of 442.

The classification is the **endpoint**, because `NumistaClient` has one method per endpoint and the
two that carry `/issues` belong to the pass and to nothing else. `BudgetReserveTest` pins the five
paths the client builds, so a sixth added without a thought fails a test instead of quietly taking
the month.

What this buys is not thrift. It is that **a bug in the expensive thing can no longer freeze the
cheap thing**, which is the whole of what August was.

### 2. The inventory refreshes itself on a launch, once a day

> **`INVENTORY_LIFE_MILLIS` is 24 hours. On a launch, if the last sync is older than that and no
> wall is standing, `InventoryRefresh` brings the collection — two seconds after the start, capped at
> ten fichas, and without a word.**

A day is a floor and a ceiling at once, the same shape `THROTTLE_WALL_MILLIS` has. Under it a day of
eight launches pays twice for the same answer; over it the coin bought on Monday is not on its plate
on Tuesday. Sixty consultas a month is 4 % of the budget and fits inside the reserve §1 just gave it.

Four rules, and each is a difference from the press it borrows its machinery from:

- **It says nothing.** Not the report, not the failure. The sentences of `syncErrorLabel` belong to
  the gesture that earned them; a collector who has just opened the app and is told «Numista rechazó
  tu API key» has been interrupted by something he did not do. Where it is allowed to show is the
  durable line under the button — «Última sincronización: hoy 09:14» — which is read and not
  announced.
- **It buys at most ten fichas** (`AUTOMATIC_FICHA_LIMIT`). The inventory itself is never capped: it
  is one call and it is the point. What is unbounded is the fichas of types the cache does not hold,
  and a cache emptied by a reinstall would otherwise turn one launch into two hundred consultas. Ten
  covers every batch either collector has ever bought at once — four of the father's 191 types were
  missing from the seeded snapshot — and the rest waits for tomorrow or for a press.
- **It gives up in front of a standing wall** (#579), which a press does not. The press is how the
  collector finds out the key works again; this would only pay twice a day to be told what the phone
  had already written down.
- **It waits two seconds and claims itself before waiting.** Two so the first screen is drawn before
  the network is touched, one fewer than the pass of ADR 0028 §3 so the pass finds `inFlight` already
  raised and holds rather than being cancelled halfway through a consulta it has paid for.

### 3. Nothing new on the screen

No button, no switch, no sentence. The line under «Sincronizar» already existed and already said
when the last sync was; what changes is that it starts telling the truth. The press stays exactly as
it was — unlimited fichas, every outcome spoken, and it goes through a wall — because it is the one
thing here somebody asked for.

## Consequences

- The father's collection is at most a day behind, for 60 consultas a month, without him doing
  anything.
- A month the pass has emptied still has 300 consultas in it, so «Sincronizar» always works.
- The settings line of the pass stops claiming the month is over when it is only the pass's share of
  it: «Se acabaron las consultas que los precios tienen este mes».
- A launch that refreshes delays the first valuation pass by about two seconds, and the pass is
  forced again when the refresh ends, held or not — a pass that stood down for a sync recorded
  nothing as covered.
- This does **not** reopen ADR 0025. A cached ficha is still only re-asked by the collector's gesture;
  what refreshes here is the inventory, and the fichas it buys are of types the phone has never seen.
