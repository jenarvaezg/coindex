# ADR 0030: The shelf window of «Explorar» is valued by hand, one plate at a time

- Status: accepted; §8 amended by [#520](https://github.com/jenarvaezg/coindex/issues/520) (clause
  5) and [#556](https://github.com/jenarvaezg/coindex/issues/556) (clause 1)
- Date: 2026-08-14
- Decides [#498](https://github.com/jenarvaezg/coindex/issues/498) and the
  [#494](https://github.com/jenarvaezg/coindex/issues/494) inside it, with the mockup of
  `docs/ux/prototipo-escaparate-498/`
- Amends ADR 0028 §3 (the spend gains a gesture, and it is the collector's), ADR 0028 §5 (a price
  asked for by hand does not expire, and it carries its date), ADR 0026 §8 clause 4 (the annex
  arrives with its list, and its population is wider than the twenty) and ADR 0021 §7 (a plate opens
  with no evidence when it is one of the twenty)
- Corrects [#282](https://github.com/jenarvaezg/coindex/issues/282) in its decision about the
  shelf's order, narrowly: see §8

## Context

ADR 0029 delivered the annex with «Lo que busco» and left the shelf window for later. Since #282
sized it, ADR 0029 §4 had made the monthly pass depend on marks, and «Explorar» already held a
section, so the open question was the screen with two things in it. Seven mockups at real dp
answered it (§8). The card and the screen were chosen in #279; the hole's ghost, in #556.

Remeasured over `data/`: 15 plates at ≤10 slots and 5 at 11-19, all 20 with a complete silver
floor, and 227 calls to value the twenty, none over 34.

## Decision

### 1. What the shelf window is: curated, unowned, under twenty slots

> **The twenty plates are the curated catalogs with no evidence whose album has fewer than twenty
> measurable casillas.** Nothing about the collection decides it beyond the evidence, and nothing
> about money decides it at all.

Past twenty slots, a plate of zeros is a catalogue nobody asked for: the «a number that says nothing
is not shown» of ADR 0028 §1, applied to the plate. A catalog with no measurable casilla is not in
the window either (its divisor is zero, as in ADR 0021 §3). The population is derived: a catalog
leaves when the sync brings its first coin.

### 2. Browsing costs nothing, and that is a property of the seed

The 133 types of the twenty are in the seeded cache with their photographs, so browsing costs zero
calls. See Consequences.

### 3. Amendment to ADR 0028 §3: the spend gains a gesture, and the collector decides it

> **«Tasar esta lámina · N consultas» is a gesture, it is per plate, and it names its spend before
> it is pressed.** The twenty are never valued together, and no pass asks for them on its own.

ADR 0028 §3's «two triggers, and no gesture» stands for the collection. Here:

- **The alternative is «never», not «later»**: ADR 0029 §4 lets the pass price only marked slots,
  so nothing else will ever ask for an unmarked plate.
- **The spend is the collector's, and the plate is the unit.** The cost is named as a ceiling, like
  «+2 consultas al mes»: one `/prices` per hole plus one `/types/{id}/issues` per type whose curated
  file names no issue.
- **It is not the second invisible budget ADR 0029 §5 refused**: no cap, nothing automatic.

It runs the same pass over a plan holding only that plate's holes, and writes to `issue_prices` and
`issue_price_reads` (ADR 0029 §4).

### 4. Amendment to ADR 0028 §5: a price asked for by hand does not expire, and it carries its date

> **A hand-asked price never expires. It is shown with the date it was brought, and «Volver a tasar»
> is on the plate for ever.**

Expiry exists for prices the pass will ask for again (ADR 0028 §5: ninety days since
[#561](https://github.com/jenarvaezg/coindex/issues/561)); no pass comes for these, so expiry would
only blank the amount.

A tasación **refused before it starts** (sync in flight, no key, no budget) writes nothing and says
so where it was pressed. One **cut short halfway** keeps the rows it wrote (one issue, one row, one
transaction): that is a floor, not a half-done total (ADR 0028 §7), so the amount says what it
covers, «4 de 12 casillas», beside «Volver a tasar». «Valued» means asked, not priced: once asked,
the gesture's wording changes, and a plate with no catalogue price and no metal says so instead of
showing an empty header.

### 5. A total whose parts were read on different days is dated by its oldest (#494)

> **The date of a total is the date of its oldest component.** One date and never two, and never the
> newest.

A shelf-window plate with a marked casilla mixes this month's pass with the day it was valued by
hand. A date promises something about the whole total, and the newest would overstate its freshness.
Two dates were drawn and rejected: arithmetic the collector does not act on. ADR 0028 §5 (#594)
applies this rule per clock.

### 6. One figure in the header, and it is the cost of entering

With no pieces, «Valor actual» has no reading: zero pieces is absence, not `0 €`. The figure is
**«Coste de entrar»**: closing a plate is buying the last of something you collect, and this is
buying the first of something you do not. Its provenance (`en sin circular`, ADR 0028 §8) and date
(§4) go with it.

«Tasar esta lámina» replaces «Exportar la lámina» (#282, decision 8): a PNG of empty holes is
somebody else's collection. The marking mode and the Numista link stay.

A plate never valued shows no amount, although its silver floor costs no call (ADR 0028 §9): «entrar
cuesta al menos esto» cannot be told from the price, the reason ADR 0028 §1 gives. The gate is
whether this phone asked about the issue, the same row that dates it.

The ratio is on the header, never on the tile: a column of «0/N» down a shelf is the reproach
ADR 0026 §10 avoids. A tile says «2 casillas» until valued, then its amount; the completion stamp
(ADR 0026 §3) cannot fire here.

### 7. The threshold of ten casillas does not apply to a plate that is not yours

On a plate the collector owns nothing of, every casilla is empty by definition and that is what they
came to see, so `HOLE_THRESHOLD_SLOTS` (ADR 0028 §1) does not apply and the whole plate is priced.
The mockup of #279 had it wrong.

### 8. On screen: one shelf of «lo que te falta», and the list behind its door

> **«Explorar» is a shelf of the plates where something is missing: the twenty you do not collect
> **and** your own plates holding a marked casilla, in one grid, ordered «primero lo que busco». «Lo
> que busco» keeps its own screen, behind a door of deeper paper at the head of the shelf.**

1. **Own plates are in it** because a mark belongs to a casilla, not to a screen (ADR 0029 §2);
   leaving them out would sort by ownership. Only plates with a mark enter, so it is not a second
   index.
2. **The list stays a screen**: «Lo que busco» is the sheet exported for a fair (ADR 0029 §7), which
   a card on the shelf cannot be.
3. **The order, correcting #282's «by cost of entering»**: the shelf starts with no amounts, so the
   default is marked plates first, then casillas ascending; «por coste de entrar» is a second order
   that leaves unvalued plates behind.
4. **Search and sort, no chips** (ADR 0026 §8 clause 4): the twenty are all at 0/N, so no facet
   earns its place.

   *Nota de forma, #513: «no chips» se refiere a facetas. El par de órdenes del punto 3 sí usa
   `FilterChip`, el único dibujo de «elegido» del álbum.*

   *Nota de forma, #515: la caja compara con `fold` y `matchesQuery`, como las de las jerarquías
   («aguila» encuentra «Águila»). Su rótulo, «Buscar entre las láminas», no lleva posesivo porque
   este estante no es tuyo.*
5. **The index door keeps the two forms of ADR 0026 §8**: «Y otras 20 láminas que no coleccionas →»,
   or «Lo que busco · 7, y otras 20 láminas →» once something is marked. It counts the twenty and
   not the twenty-three: own plates are already in the index.

   *Nota de forma, #515: con texto en la caja del índice, la puerta añade «Lo que escribes arriba no
   llega hasta aquí.» en `bodySmall`; el recuento sigue siendo sobre la colección entera.*

> **Enmienda del 17 de agosto de 2026 (§8 cláusula 5,
> [#520](https://github.com/jenarvaezg/coindex/issues/520)): el anexo pierde una habitación y gana
> un hermano.** El índice cuelga dos filas con nombre estable y un solo destino: «Lo que busco · 7»
> en la cabecera y «Y otras 20 láminas que no coleccionas →» al pie. La lista pasa a ser un anexo
> hermano, colgado de Colecciones como el estante, y «Explorar» conserva su puerta a ella. El
> recuento, la población y el orden no cambian. La nota de la caja pasa a la fila de la cabecera, y
> sus casillas se dibujan a plena luz (ADR 0026 §15 enmendado).

> **Enmienda del 1 de septiembre de 2026 (§8 punto 1,
> [#556](https://github.com/jenarvaezg/coindex/issues/556)): el estante deja de preguntar de quién
> es la lámina.** `absence = if (tile.mine) Filled else Missing` ponía el fantasma («te falta») en
> las láminas de la ventana, donde no falta nada. Ahora todas van a plena luz, y el filete de puntos
> dice que la moneda no es tuya, según `coverOwned` (un hecho, no un alias de `mine`). Queda abierto
> si el filete basta sobre cantos con denticulado (`docs/ux/implementacion-556/`).

## Consequences

- **The seed becomes load-bearing.** A catalog curated but not seeded puts grey circles in the shelf
  window, and `curate-catalog` does not seed.
- A shelf-window plate is a plate for the marking mode, the notebook and the travelling coin, but
  not a card of the index, so ADR 0021 §2 holds: one species of collection.
- An own plate with a marked casilla appears in the index and in this shelf: one plate in a second
  order, not a second species, since the shelf is not a hierarchy (ADR 0026 §8).
- **Nothing keeps a history.** A plate valued twice keeps one row per issue with the newer date, as
  ADR 0028's Consequences require (ADR 0026 §10).
- Euro amounts are never versioned. The method and counts are here and in
  `docs/ux/prototipo-escaparate-498/README.md`; the amounts live in `/private/tmp/coindex-privado/`.
