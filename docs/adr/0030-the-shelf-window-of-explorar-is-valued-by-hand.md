# ADR 0030: The shelf window of «Explorar» is valued by hand, one plate at a time

- Status: accepted
- Date: 2026-08-14
- Decides [#498](https://github.com/jenarvaezg/coindex/issues/498) and the
  [#494](https://github.com/jenarvaezg/coindex/issues/494) that travels inside it, with the mockup of
  `docs/ux/prototipo-escaparate-498/` in front of us
- Amends ADR 0028 §3 (the spend gains a gesture, and it is the collector's), ADR 0028 §5 (a price
  asked for by hand does not expire, and it travels with its date), ADR 0026 §8 clause 4 (the annex
  arrives with its list, and its population is wider than the twenty) and ADR 0021 §7 (a plate opens
  with no evidence when it is one of the twenty)
- Corrects [#282](https://github.com/jenarvaezg/coindex/issues/282) in its decision about the shelf's
  order, narrowly and by name: see §8

## Context

ADR 0029 delivered the annex of ADR 0026 §8 holding one section, and left the second one written down
as absent: *«the shelf window itself is not in this delivery — the annex arrives holding one section,
so the door is short and the screen is named after the only thing in it»*. This is that section.

Two things had changed since #282 dimensioned it, and both change the answer.

**The pass stopped being a fixed number.** #282's central decision was taken with the monthly pass
nailed at 487 calls, and ADR 0029 §4 unnailed it: a marked slot is priced wherever it comes from, so
the month is already a function of what the collector marked. That is why this block is second in the
queue — the quota became elastic once, and this document never touches how the spend is decided. It
only adds plates whose casillas can be marked.

**«Explorar» is no longer an empty door.** #282 and #279 dimensioned a screen with nothing behind it;
when it came to be built, `ExploreScreen` already held «Lo que busco». The form question was therefore
not the shelf — the card, the ghost and the screen were chosen in #279 — but what that screen *is*
with two things inside it. Seven mockups at real dp answered it; §8 is the answer.

And the measurements of #498 no longer hold. Over `data/` as it stands, with `memberMatches` whole
and the arithmetic of `valuationPlan`: **15** plates at ≤10 slots and **5** at 11-19 (not 14 and 6),
all **20** with a complete silver floor (not 17), **one** plate at 34 calls (not four), and **227**
calls to value the twenty (not 245). The order of magnitude survives — zero fixed cost a month, no
plate over 34 calls — and one of #282's loose ends dies with the new numbers: there is no priceless
explorable plate left to word.

## Decision

### 1. What the shelf window is: curated, unowned, under twenty slots

> **The twenty plates are the curated catalogs with no evidence whose album has fewer than twenty
> measurable casillas.** Nothing about the collection decides it beyond the evidence, and nothing about
> money decides it at all.

The cut is #279's and #282's, unchanged, and it is a cut about **what a plate can say**: at twenty
slots a plate of zeros stops being a shelf window and becomes a catalogue nobody asked for. It is the
same reading as the threshold of ADR 0028 §1 — a number that says nothing is not shown — applied to
the plate rather than to its cost.

**And a third condition that is not a cut but a floor: a catalog with no measurable casilla at all is not
a shelf window either.** A file of nothing but announcements has no plate to open — its divisor is zero,
which is the same reason the ratio of ADR 0021 §3 is absent rather than `0/0` — so it is not «a very small
window», it is none.

The population is derived and never stored, like every other list in this app: a catalog that gains
evidence leaves the window the moment the sync brings the coin, and it does so by the same rule that
opens its plate.

### 2. Browsing costs nothing, and that is a property of the seed

Opening the shelf and walking into any of the twenty is **zero calls**: the 133 types of those plates
are in the seeded ficha cache, with their photographs. This is the promise the screen is built on, and
it is the one thing here that can break silently — see the Consequences.

### 3. Amendment to ADR 0028 §3: the spend gains a gesture, and the collector decides it

ADR 0028 §3 wrote *«two triggers, and no gesture»*, and discarded `Tasar la colección · 487 llamadas`
as a button on a screen whose purpose was removing prose. **That verdict stands for the collection and
is reversed for one plate of the shelf window.**

> **«Tasar esta lámina · N consultas» is a gesture, it is per plate, and it names its spend before it
> is pressed.** The twenty are never valued together — 227 calls in one press is a month's quota
> decided by a mis-tap — and no pass ever asks for them on its own.

What makes this the opposite case from the one §3 refused, and not an exception to it:

- **The alternative is not «later», it is «never».** For the owned collection the choice was between a
  button and a background pass that ends in the same place. Here there is no pass that would ever ask:
  ADR 0029 §4 left the evidence filter lifted **only** for a marked slot, so an unmarked plate of the
  shelf window has no route to a price at all.
- **The spend is the collector's and the plate is the unit they chose.** #282's rule — the cost is
  named in the gesture — is followed exactly, and it is named as the ceiling, like the mode's «+2
  consultas al mes»: one `/prices` per hole plus one `/types/{id}/issues` per type whose curated file
  names no issue.
- **It is not the second invisible budget ADR 0029 §5 refused.** There is no cap and no automatic
  anything: the figure is on the gesture, and the month's total is where it already is (Ajustes).

The pass itself is unchanged and is the one that runs: valuing a plate by hand is one pass over a plan
holding **that plate's holes and nothing else**. So there is no second writer, no second table and no
second price — the rows land in `issue_prices` and `issue_price_reads` exactly as ADR 0029 §4 already
requires of the marked slot.

### 4. Amendment to ADR 0028 §5: a price asked for by hand does not expire, and it carries its date

ADR 0028 §5 gave a catalog price thirty days, and it did so for a price **the pass will ask for
again**: expiry there is what makes the monthly trickle a trickle. A plate of the shelf window has no
pass coming for it, so thirty days would mean one thing only — the amount vanishing off a screen that
has no way to refill it.

> **A hand-asked price never expires. It is shown with the date it was brought, and «Volver a tasar»
> is on the plate for ever.**

This is not new licence: it is the sentence §5 already signed — *«on day 31 the old price is still
shown, with the date it was brought»* — with the deletion clause removed for the one case where
nothing would ever bring a newer one. The date is not decoration; it is what makes the amount
readable months later, and it is the rule #316 signed for the spot.

**The refusal is the gesture's, not the plate's.** A tasación **refused before it flies** — a sync in
flight, no key, a budget already gone — writes nothing at all and says so where the gesture was pressed:
the plate is exactly as it was, browsable, at zero calls, with «Tasar esta lámina» still on it.

**A tasación cut short halfway is a different thing, and it leaves rows.** The pass is resumable on
purpose — one issue, one row, one transaction (ADR 0028) — so a budget that runs out mid-plate leaves what
it had already asked for. That is not a half-done total of §7: it is a **floor over the casillas it
covers**, and it is only honest if the figure says so. So the amount prints what it covers whenever that
is not the whole plate — «4 de 12 casillas» — and «Volver a tasar» is right there to finish the rest. The
same clause covers the plate Numista simply has no prices for: **valued is asked and not priced**, so the
word on the gesture changes the moment the phone has asked, and a plate with neither a catalogue price nor
a metal to weigh says so instead of showing an empty header.

### 5. A total whose parts were read on different days is dated by its oldest (#494)

The case is created by this block and the one before it together: a plate of the shelf window with a
**marked** casilla has that price from this month's pass and the rest from the day it was valued by
hand. Neither ADR 0028 §7 nor the provenance rule of #316 covers it — the total is not half done, it
is whole with two ages.

> **The date of a total is the date of its oldest component.** One date and never two, and never the
> newest.

The oldest, because a date on a total is a promise about **all** of it: dating a total by its newest
component would say the whole amount is as fresh as its freshest part, which is exactly the false
precision ADR 0028 §7 refuses in the other direction. Two dates were drawn in the mockup and are not
adopted: they turn one figure into an arithmetic exercise, and the second one buys nothing a collector
acts on.

### 6. One figure in the header, and it is the cost of entering

A plate of the shelf window holds nothing, so #493's first figure — «Valor actual» — has no reading:
zero pieces is absence and not `0 €`. What is left is the second figure, and it is not called what it
is called on your own plate: **«Coste de entrar»**, because you are not closing a plate, you are
starting one. Its provenance rides with it, `en sin circular` (ADR 0028 §8), and its date rides with
it too (§4 above).

**«Tasar esta lámina» takes the place of «Exportar la lámina»** (#282, decision 8): a PNG of ten empty
holes is a sheet of somebody else's collection, and the plate that has nothing of yours has nothing to
export. The marking mode stays — that is the whole point of the twenty being in reach — and so does
the link to Numista.

**A plate that has never been valued says no amount at all**, and that is a decision rather than a
consequence: the silver floor costs no API call — the spot is two keyless calls (§9) and the weight is in
every seeded ficha — so a figure *could* be shown over all twenty without anybody pressing anything. It
is not, for the reason §1 gives for the plates over its threshold: «entrar cuesta al menos esto» is not a
sentence the collector can tell from the price. The gate is whether **this phone asked about the issue**,
which is the same row that makes the date sayable.

**The ratio stays on the header and never on the tile.** `0/2` beside a title the collector is reading
says how many casillas the plate has; twenty of them down a shelf would be a column of noughts, which is
the reproach ADR 0026 §10 avoids. So a tile says «2 casillas» until it is valued and its amount
afterwards — and the completion stamp of ADR 0026 §3 can never fire on one of these, which is why its ink
is not even read.

### 7. The threshold of ten casillas does not apply to a plate that is not yours

`HOLE_THRESHOLD_SLOTS` is the rule of the reproach: *«a plate with 51 holes does not have a cost of
completion, it has a reproach of 51 slots»*. **A plate you own nothing of reproaches nothing** — every
casilla is empty by definition and the collector went there to look at what they do not have — so the
threshold has no work to do and the whole plate is priced when it is valued.

The mockup of #279 had inherited the threshold and was wrong to; the correction is recorded here
because it is the only place a later reader will look.

### 8. On screen: one shelf of «lo que te falta», and the list behind its door

Chosen with the mockup in front of us out of seven, and it corrects #282's *«what the shelf of
Explorar carries»* in its order and widens its population:

> **«Explorar» is a shelf of the plates where something is missing: the twenty you do not collect
> **and** your own plates holding a marked casilla, in one grid, ordered «primero lo que busco». «Lo
> que busco» keeps its own screen, behind a door of deeper paper at the head of the shelf.**

1. **Why your own plates are in it.** The mark is a state of a casilla and not a section of a screen
   (ADR 0029 §2), so a screen that showed the twenty and hid the three plates where the collector is
   actually hunting would be sorting by ownership — which is the one thing the collector is not doing
   when they open this. Only plates **with a mark** enter, so this is not a second index: it is where
   something you are looking for is missing.
2. **Why the list stays a screen.** «Lo que busco» is not a list on a screen, it is `Exportar la
   lista` — the sheet taken to a fair (ADR 0029 §7) — and a card saying «2 lo busco» cannot be taken
   anywhere. Folding it into the shelf was drawn and discarded: it left the phone with nowhere to see
   the marked casillas together, and it spent the vocabulary of a screen published the day before.
3. **The order, and #282's correction.** #282 chose «by cost of entering» when the pass valued on its
   own. With §3, the shelf is born with no amount at all, so the default order is **the marked plates
   first, then by casillas ascending** — fewest first, which is the same «what can be said» reading as
   §1 — and «por coste de entrar» is offered as a second order that sorts what has been valued and
   leaves the rest behind it. An order that would rank twenty absences is not a default.
4. **Search and sort, and no chips**, which is clause 4 of ADR 0026 §8 arriving as it was written. The
   twenty are all at 0/N so no facet earns its place: twelve countries with nine holding a single
   plate was measured in #279 and has not changed.

   *Nota de forma, #513: «no chips» es sobre **facetas**, que es de lo que habla la cláusula — este
   estante no filtra por nada. El par de órdenes del punto 3 sí se dibuja con `FilterChip`, que es el
   único dibujo de «elegido» que tiene el álbum: pintado como texto suelto frente a un botón
   enmarcado, el criterio en vigor se leía como rótulo del alternativo. Ni la población del estante ni
   el recuento de facetas cambian; lo que cambia es qué forma viste el estado «éste es el orden».*

   *Nota de forma, #515: la caja compara ahora con `fold` y `matchesQuery`, como las de las dos
   jerarquías. Era un `contains` pelado —sensible a acentos y ciego a dos palabras en cualquier
   orden— y la cláusula nunca pidió eso: pedía una caja de búsqueda, y en este álbum una caja de
   búsqueda encuentra «Águila» escribiendo «aguila». Su rótulo, «Buscar entre las láminas», sigue
   siendo el único de los tres sin posesivo, que es lo que dice que este estante no es tuyo.*
5. **The door of the index keeps the two forms §8 wrote** — «Y otras 20 láminas que no coleccionas →»
   with nothing marked, «Lo que busco · 7, y otras 20 láminas →» once something is — and it now opens
   the shelf, with the list one row further in. The count of the plates is the twenty and not the
   twenty-three: what is behind the door that the index does not already hold is the shelf window, and
   a door that counted your own plates twice would be claiming they are somewhere else.

   *Nota de forma, #515: mientras hay algo escrito en la caja del índice, la puerta añade una segunda
   línea —«Lo que escribes arriba no llega hasta aquí.»— en `bodySmall` bajo su nombre. El recuento
   sigue siendo el de la cláusula, medido sobre la colección entera y no sobre el estrechamiento, que
   es lo correcto porque lo de detrás no está en la lista de arriba; lo que se arregla es que «Y otras
   55 láminas» sobre un índice que la búsqueda dejó en cero se leía como un número sin recalcular. No
   se nombran los filtros, que sobreviven a un lanzamiento: esa línea se imprimiría en cada sesión.*

## Consequences

**The seed becomes load-bearing.** §2 is a promise about the APK: a catalog curated and not seeded
puts a plate of grey circles in the shelf window on the day it is added, and `curate-catalog` does not
seed. Today it holds — the 133 types of the twenty are all in the cache — and it is a risk that reads
in hours from the developer's side, which is why this block went second and not first.

**A plate of the shelf window is a plate**, and every mechanism that walks one now walks these too:
the marking mode, the notebook's plate section, the travelling coin. What it is **not** is a card of
the index, which is what keeps ADR 0021 §2 whole — there is still one species of collection, and these
twenty are not one of them until a coin arrives.

**ADR 0021 §2, read twice.** A plate of the collector's with a marked casilla is now drawn in two
lists: its card in the index and its tile in this shelf. That is accepted here and it is not a second
species: it is one plate, in a second **order**, and the shelf is explicitly not a hierarchy (ADR 0026
§8). What would break §2 is a second card with a name of its own, and there is none.

**Nothing here keeps a history.** A plate valued twice keeps one row per issue with the newer date, as
ADR 0028's Consequences require: no series of what a plate used to cost, and therefore no accidental
wealth management (ADR 0026 §10).

The euro amounts of all this are **not in this document and are never versioned**. The method, the
counts and the proportions are here and in `docs/ux/prototipo-escaparate-498/README.md`; the amounts
live in `/private/tmp/coindex-privado/`.
