# ADR 0032: The inventory has a clock of its own, and a reserve nobody else can spend

- Status: accepted
- Date: 2026-09-30
- Decides [#605](https://github.com/jenarvaezg/coindex/issues/605). Adds a second ceiling to the
  budget of [ADR 0003](0003-atomic-api-budget-reservations.md) and a fourth trigger to the launch of
  [ADR 0024](0024-photographs-are-prefetched-on-unmetered-networks.md) and
  [ADR 0028 §3](0028-prices-arrive-in-one-pass-and-a-total-is-never-half-done.md)

## Context

The collector opens Coindex to see the coin he bought on its plate. That costs one call
(`/users/{id}/collected_items`, unpaginated since ADR 0006) plus the token, and only a press asked
for it. `api_call_log`, read on 30 September 2026 (`.local/padre` captured on the 1st, `.local/jose`
on the 7th):

```
padre    jul: 17    ago: 1500    sep: 441 (345 precios + 96 listados)
jose     jul:  8    ago: 1500    sep:  67
```

The father last synced on 10 August 2026, then not once in twenty-two days, while opening the app
about eight times a day. From 11 August he could not have: the bug of
[#560](https://github.com/jenarvaezg/coindex/issues/560) spent 1.484 consultas, the phone reached
`DEFAULT_MONTHLY_BUDGET`, and `CallBudgetGate` refused even a sync. Both phones ended August at
1.500.

| what | who asks for it | how often | cost |
| --- | --- | --- | ---: |
| **inventory** (`collected_items`) | only the button on «Este teléfono» | never on its own | **2** |
| fichas of new types | the same button, only the missing ones | idem | 1 per type |
| a cached ficha | the collector's gesture (ADR 0025) | never | 1 |
| **prices** | automatic, on every launch | automatic | **442** cold, ~0 after |
| the catalogs of `data/` | they travel in the APK | every release | 0 |

The only automatic spend cost 442, and it shared one budget with the one that costs 2.

## Decision

### 1. The valuation pass stops 300 consultas short of the cap

> **`CallBudgetGate` has two ceilings. `/types/{id}/issues` and
> `/types/{id}/issues/{issue_id}/prices` may spend up to
> `DEFAULT_MONTHLY_BUDGET - INVENTORY_RESERVE`; everything else may spend the whole budget.
> `INVENTORY_RESERVE` is 300.**

300 is a month of the cheap things: 60 for a daily inventory, a few new fichas, one per gesture of
ADR 0025. It leaves the pass 1.200, two and a half cold months. The split is by endpoint, since the
two `/issues` paths belong only to the pass, and `BudgetReserveTest` pins the five paths the client
builds. A bug in the expensive thing can no longer block the cheap one.

### 2. The inventory refreshes itself on a launch, once a day

> **`INVENTORY_LIFE_MILLIS` is 24 hours. On a launch, if the last sync is older than that and no
> wall is standing, `InventoryRefresh` brings the collection — two seconds after the start, capped
> at ten fichas, and without a word.**

A day bounds both ways, like `THROTTLE_WALL_MILLIS`: shorter pays twice for the same answer, longer
leaves Monday's coin off its plate on Tuesday. Sixty consultas a month fit the reserve. It differs
from a press in four ways:

- **It is silent.** `syncErrorLabel` belongs to the gesture; the line under the button («Última
  sincronización: hoy 09:14») shows the result.
- **At most ten fichas** (`AUTOMATIC_FICHA_LIMIT`); the inventory call itself is uncapped. A cache
  emptied by a reinstall would otherwise cost two hundred consultas on one launch.
- **It stops at a standing wall** (#579). The press does not, since that is how the collector learns
  the key works again.
- **It waits two seconds, claiming itself first**: after the first frame, and one second before the
  pass of ADR 0028 §3, which then finds `inFlight` raised and holds.

### 3. Nothing new on the screen

The line under «Sincronizar» already existed; now it stays current. The press is unchanged:
unlimited fichas, every outcome reported, and it ignores the wall.

## Consequences

- The father's collection is at most a day behind, for 60 consultas a month.
- With the pass's share spent, 300 consultas remain, so «Sincronizar» always works; the pass's line
  says «Se acabaron las consultas que los precios tienen este mes».
- On a launch that refreshes, the first pass waits about two seconds and is forced again when the
  refresh ends, since a pass that stood down recorded nothing as covered.
- ADR 0025 is unchanged: this fetches fichas only for types the phone has never seen.
