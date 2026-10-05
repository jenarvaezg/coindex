# ADR 0029: A wish is an empty slot marked on the phone

- Status: accepted; §6 amended by [#520](https://github.com/jenarvaezg/coindex/issues/520) (see
  ADR 0030 §8)
- Date: 2026-08-14
- Decides [#484](https://github.com/jenarvaezg/coindex/issues/484) and implements
  [#497](https://github.com/jenarvaezg/coindex/issues/497)
- Amends ADR 0021 §7 (a declarative comes back, and it is not the one §7 removed), ADR 0028 §1 (the
  threshold yields to a mark) and ADR 0026 §4 (the mark travels to paper, with an output of its
  own). Uses the annex of ADR 0026 §8. Upholds ADR 0021 §11, whose key it inherits.
- Corrects the central decision of [#282](https://github.com/jenarvaezg/coindex/issues/282),
  narrowly: see §4.

## Context

[#279](https://github.com/jenarvaezg/coindex/issues/279) put the wishlist out of scope, and Jose
corrected it on 12 August 2026: «aunque no se escriba en Numista sí que debería haber algo». The app
does not write inventory, and Numista does not record wanting: a named collection there declares
false possession ([#483](https://github.com/jenarvaezg/coindex/issues/483)). «What exists that I
lack» is already served by «Explorar» and by the holes of each plate. What remains is a durable
local mark over something the collector does not have.

## Decision

### 1. It comes from the phone, and it inherits the key of a box

A wish is one row in its own table, on this device only, keyed by the slot's catalogue facts: the
type, the year, and the Numista issue where the curated file names one. Like `own_groupings`
(ADR 0021 §11), it is the collector's own organization, and it is never keyed on Numista
row ids, which every sync replaces.

#484 said «keyed by `typeId`». This widens it: in a date run (ADR 0009) or an issue run (ADR 0014) a
`typeId` spans several slots, so a type key would turn «the 2013 Kookaburra» into every Kookaburra.
The key is the slot identity the validator compares (`dateSlots`) and `memberMatches` fills.

It is not the `Followed` that ADR 0021 §7 removed: those ~58 rows marked plates that already had
evidence and carried no information. A mark on an empty slot does.

### 2. The grain is the slot, and a wish dies measured

A wish is one empty slot; «this plate interests me» is derived from it. A wish over a slot dies when
the sync fills the slot. One over a plate would never die, and would not say which coin to take to a
fair.

Alive is derived, never stored: a wish is alive while `CollectionCatalog.memberMatches` leaves its
slot empty, the same rule the plate uses. No row is deleted, the sync gains no writer, and there is
no notice. If the piece is sold, the wish comes back.

### 3. Any empty slot, and the invariant is a table rather than a filter

Any empty slot can be marked, on an owned plate or in the shelf window; otherwise buying one piece
of a two-slot plate would erase the wish on the other. The separate table keeps wishes away from
every inventory reader (`memberMatches`, `isEvidencedBy`, `Figures`, `Valuation`,
`NotebookSections`, `OwnGrouping`, the shelf) without the filter #483 proposed.

> **Nothing that reads inventory joins this table.** Four readers use it on purpose: the slot on its
> plate (§5), the annex (§6), the paper (§7) and the valuation plan (§4).

### 4. Amendment to ADR 0028 §1: a marked slot lifts **both** filters

- **The threshold**: a mark picks one hole out of many, so the slot is priced past
  `HOLE_THRESHOLD_SLOTS`.
- **The evidence filter**, against #282's «the shelf window does not enter the pass»: what you mark
  gets priced wherever it is. An unmarked plate of the shelf window still costs nothing.

`issue_prices` and `issue_price_reads` hold one row per issue, so the pass refreshes the row the
gesture wrote. Over the 75 catalogs and the father's 229 rows: 125 holes priced, 419 in 17 plates
past the threshold, and 157 slots in the 20 plates of the shelf window, not a wall of 815 holes.

### 5. The gesture: a mode on the plate, and one chip in the hole

Marking is a mode, like box selection (`PieceSelection`, ADR 0021 §11): a door in the plate's
header, and while it is open, tapping an empty hole toggles the mark. A control per slot would
repeat the cost line on every hole (ADR 0026 §5).

The mode states the cost, «+2 consultas al mes» (the ceiling; one if the curated file names the
issue), and Ajustes adds «lo que busco · N al mes». No automatic cap (#282). **Two places, not
three**: the annex does not print the figure.

`HoleStamp` shows the mark, the amount, or the mark over the amount: one chip per hole (#493).

### 6. On screen: the annex of ADR 0026 §8, entered from Colecciones

«Lo que busco» is the first section of «Explorar», an annex (ADR 0026 §8, #281): it hangs off
Colecciones, has no cell or bar, closes with «Volver», and its door is the last row of the list,
with its count. It is not a first-level cell: its grain is the slot, so its count would be borrowed
(#317).

Until the shelf window exists the door reads «Lo que busco · 7 →»; with nothing marked there is no
row.

Rejected: a filter in Monedas (a wish is not a piece), and a facet of the Colecciones shelf (no
screen would list the marked coins together).

> Since #520 «Lo que busco» is a sibling annex of Colecciones, not a room of «Explorar»
> (ADR 0030 §8).

### 7. Amendment to ADR 0026 §4: the mark travels, and it gets an output of its own

«Alive» in ADR 0026 §4 is *«anything that follows the finger, the sensor or the navigation»*. A mark
is a state at rest, so it travels to the PNG and the PDF; on paper it is the cell's `state` line,
which costs no space.

Shelf-window plates have no «Exportar» (#282, decision 8), so the annex exports **«la lista de lo
que busco»** across both populations: one more `PrintSection`, with the same cells, grid and five
switches. Its eyebrow is its own, because «COLECCIÓN» over coins nobody owns would be false on
paper. The index notebook is unchanged.

## Consequences

- The monthly pass is no longer fixed: 1-2 calls per wish, against ~1.500-2.000 shared with Jose.
  It is the app's first elastic spend, hence named in the gesture and totalled in Ajustes.
- The main risk is `Followed` again: forty marks never looked at would spend shared quota every
  month. Only the death by measurement (§2) and the visible spend (§5) limit it, on purpose.
- Schema version 10 is additive: one table.
- A coin that is not a slot of any plate cannot be wished for: there is no search of Numista (#15).
- The shelf window and ADR 0026 §8 clause 4 (search and sort) are deferred, since seven rows sorted
  by most recent mark need neither. Both arrived with ADR 0030.
