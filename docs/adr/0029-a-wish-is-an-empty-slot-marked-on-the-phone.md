# ADR 0029: A wish is an empty slot marked on the phone

- Status: accepted
- Date: 2026-08-14
- Decides [#484](https://github.com/jenarvaezg/coindex/issues/484) and implements
  [#497](https://github.com/jenarvaezg/coindex/issues/497)
- Amends ADR 0021 §7 (a declarative comes back, and it is not the one §7 killed), ADR 0028 §1 (the
  threshold of the reproach yields to a mark) and ADR 0026 §4 (the mark travels to paper, and it gets
  an output of its own). Uses the annex of ADR 0026 §8. Upholds ADR 0013 and ADR 0021 §11, whose key
  it inherits.
- Corrects the central decision of [#282](https://github.com/jenarvaezg/coindex/issues/282), narrowly
  and by name: see §4.

## Context

The wishlist was sent to *Out of scope* by [#279](https://github.com/jenarvaezg/coindex/issues/279)
reading an unmarked slot as a rejection, and it was not one — Jose corrected it on 12 August 2026:
«aunque no se escriba en Numista sí que debería haber algo». What #484 had to decide was what a wish
*is* inside an app whose inventory has a single origin, and the answer had to survive one fact: **the
app does not write inventory.** Adding a coin to your collection is done in Numista.

Three things were already settled and narrowed the question to a residue. «Enséñame qué existe y no
tengo» is served by the twenty curated plates of «Explorar» (#279, #282). The holes of your **own**
plates are served by the plate itself, and «a una casilla» lives in its header (ADR 0026 §10). So
what was left is exact: **a durable mark over something you do not have.**

And the elegant answer is dead, killed by [#483](https://github.com/jenarvaezg/coindex/issues/483):
Numista does not record wanting. A named collection there **declares false possession** in
`item_count` and in the father's public profile, so there is no measured fact to sync and the local
declarative is not the expensive option — it is the only one.

## Decision

### 1. It comes from the phone, and it inherits the key of a box

A wish is **one row in a table of its own, on this device only, keyed by the slot's own catalogue
facts and never by anything Numista hands back**: the type, the year, and the Numista issue where the
curated file names one.

The precedent is not `collection_proposal_preferences` — the table ADR 0021 §7 dropped — but
`own_groupings` (ADR 0013, ADR 0021 §11): *«a grouping the collector made themselves … the
collector's own organization, not a claim about the catalog, so it lives only on this device and
never travels with the app»*. A wishlist is the same species, and it inherits the part that matters:

> `own_grouping_members` is keyed by `typeId` and not by row **«because row ids come from Numista and
> are replaced wholesale on every sync, so a grouping keyed on them would quietly empty itself»**.

That is not an implementation detail. It is what makes the table survive the one thing that happens
to this app every month.

**Where this widens what #484 wrote, and why.** #484 said «keyed by `typeId`, never by row», reading
the box's precedent straight. Applied literally it would contradict the decision immediately above it
— that **the grain is the slot and never the plate** — because a `typeId` is not a slot in two of the
four schema versions: a date run repeats one type across its years (ADR 0009) and an issue run repeats
one type across its issues (ADR 0014). One row per type would make «I want the 2013 Kookaburra» a mark
over **every** year of the Kookaburra, which is a wish over a plate wearing a type's clothes — the
exact thing §2 rejects, and it is where most of the father's 419 unpriced holes live. So the key is
the slot's identity, which is what the curated-file validator itself compares (`dateSlots` is
`typeId to year`) and what `memberMatches` fills a casilla by. **The reason behind #484's rule is
kept whole**: not one of the three columns is a Numista collection row id, so a sync cannot empty the
table.

**Why this is not the declarative §7 killed.** §7 retired `Followed` with a measurement: all ~58
stored rows were `Followed` over plates that already had evidence — information zero, the toll
declaring itself. Marking an **empty** slot is not that; it is information §7 could not measure,
because the gesture did not exist. And it does not reopen «lo colecciono», which is *the intention
that ages*: see §2.

### 2. The grain is the slot, and a wish dies measured

A wish is **one empty slot** — the three facts of §1 and nothing wider. «This plate interests me» is
**derived**: the plate holds at least one wish.

The argument is §7's own, applied: *«"Lo colecciono" is an intention that ages; `4 de 12` is a
measured fact.»*

- A wish over a **slot** does not age. It **dies measured**, when the sync brings that type and the
  slot fills itself.
- A wish over a **plate** never dies. It rots — and it is `Followed` reborn, over the twenty plates
  instead of the fifty-eight cards.

The derivation only runs one way: from slots you can say which plates interest the collector; from a
mark on a plate you cannot say **which coin to take to the fair**.

**Alive is derived and never stored**: a live wish is one whose slot is still empty, asked of
`CollectionCatalog.memberMatches` — the very rule that fills the casilla on the plate, so «dies
measured» is not a second reading of the inventory that could disagree with the album. The row is not
deleted, the sync gains no writer, and there is no saved state machine to fall out of step. There is no notice either — the app is opened on purpose (#279), and the reward is the slot
being full. Accepted oddity, looked at: **if he sells the piece the wish comes back.** It is probably
right — he still wants one — and it is zero measured cases.

### 3. Any empty slot, and the invariant is a table rather than a filter

Any empty slot counts, of a plate the collector has or of one from the shelf window. One rule and no
second concept, and what decides it is a defect of the alternative: if a wish only lived on plates
without evidence, buying **one** piece of «Southern Cross» — two slots — would make the plate the
collector's and **erase the wish over the other slot**, exactly when the hunt gets interesting.

**«A wish is not a piece» is not a filter: it is a table.** #483 recommended a filter in
`Curation.assemble`; with the wish outside `collected_items` **none is needed**. The fourteen leaks it
inventoried all descend from the two readers of that table, and a table of its own is seen by
neither: not `memberMatches`, not `isEvidencedBy`, not `Figures`, not `Valuation`, not
`NotebookSections`, not `OwnGrouping`, not the shelf. The invariant holds **by construction and not
by vigilance**.

> **Corollary, and it is a rule for every later reader: nothing that reads inventory ever joins this
> table.** The readers that look at it do so on purpose, and there are four: the slot on its plate
> (§5), the annex (§6), the paper (§7) and the valuation plan (§4).

### 4. Amendment to ADR 0028 §1: a marked slot lifts **both** filters

`valuationPlan` had two filters and a wish lifts both:

- **The threshold of the reproach yields.** `HOLE_THRESHOLD_SLOTS` exists because *«a plate with 51
  holes does not have a cost of completion, it has a reproach of 51 slots»* — a rule about **whether a
  number deserves to be shown**, and a mark answers precisely that: of the 51, this one. A marked slot
  is not a reproach; it is the only one the collector chose.
- **The evidence filter yields too, and this contradicts #282 in its central decision.** #282 wrote
  «the shelf window does not enter the pass», and here a marked slot of the shelf window does. It is
  decided in favour of **one rule** — what you mark gets priced, wherever it comes from — so that the
  father never has to work out which régime his coin is under. This is #282's decision 1 **narrowed to
  the case of the marked slot**, and nothing else about it changes: an unmarked plate of the shelf
  window still costs nothing, and «Tasar esta lámina» is still how the whole of one is asked for.

**There is no double price.** `issue_prices` and `issue_price_reads` hold one row per issue with its
`readAt`, so the pass **refreshes the row the gesture wrote**.

Measured over the 75 catalogs and the father's 229 rows (evidence by type, so the holes are a floor):
**125** holes were already priced and fresh, **419 in 17 plates** were past the threshold with no
gesture that could ask for them, and **157** slots live in the 20 plates of the shelf window — 14 of
them at ≤10 slots, 6 at 11-19, mean 7,9. That is what makes a fine grain usable: the markable
universe is not a wall of 815 holes.

### 5. The gesture: a mode on the plate, and one chip in the hole

Marking is **a mode and not a control per slot**, which is the idiom ADR 0021 §11 already built for
boxes (`PieceSelection`): a door in the plate's header, and while it is open the body of an empty
hole toggles the mark instead of turning the coin over. Fifty-one slots with a control each would be
the frequency ADR 0026 §5 prices, and the cost line would be printed fifty-one times.

**The cost is named in the gesture**, which is the rule #282 wrote, applied to a gesture whose spend
**repeats**: the mode says «+2 consultas al mes» — the ceiling, because a slot whose curated file
names its issue costs one — and where the budget is already shown, Ajustes gains «lo que busco · N al
mes». **No automatic cap**, which #282 discarded as a second invisible budget to explain on top of
the one that already exists.

**Two places and not three**: the annex does **not** print the figure. It was drawn there first and
taken back out — a list is used at a fair rather than budgeted, and the same number over it is the
third printing of one fact (ADR 0026 §5). Where it belongs is the gesture, which is where the spending
is decided, and the card that already speaks about the pass.

**One chip in the hole and not two things over the same coin.** #493 left the instruction and it is
followed: the mark and the price want the same centimetre of the same 104 dp hole, so
`HoleStamp` says both — the word, the amount, or the word over the amount. It covers the ghost,
which #493 already measured and accepted.

### 6. On screen: the annex of ADR 0026 §8, entered from Colecciones

«Lo que busco» is the first section of **«Explorar»**, which is an **annex** by ADR 0026 §8 as #281
ruled it: it hangs off Colecciones, carries no cell and no bar, is left with «Volver», and its door
is the **last row of that hierarchy's list**, naming what is behind it with its count.

**The first level does not grow**, and the test of #317 is applied rather than dodged: the grain of a
wish is the slot — the same grain that felled the country stain and the year axis — and its count
would be borrowed. It fails the test, so it is not a cell.

**While the shelf window does not exist the door is written in its short form** — «Lo que busco · 7 →»
— and a zero is not printed, so with nothing marked there is no row at all. That is the same clause
§8 wrote for the two forms of the door and the same one the sewn edge keeps (#418).

Discarded by mechanics and not by taste: **a filter in Monedas**, even though the grain fits exactly,
because Monedas is your collection by type and putting types you do not have in it is literally «a
wish is a piece»; and **a facet of the Colecciones shelf** («with something I am looking for»), which
leaves no screen showing the seven coins in a row — and at a fair that is worse than nothing.

### 7. Amendment to ADR 0026 §4: the mark travels, and it gets an output of its own

**§4 already decided this and it does not say what it appears to.** «Alive» there is *«anything that
follows the finger, the sensor or the navigation»*; a wish mark is a **state at rest**, like the
rubber stamp of a complete plate, so it travels to the PNG and to the PDF with no exception written.
On paper it is the slot's `state`, the line the printed cell has always reserved and never used, so
the mark costs no millimetre and moves no page count.

But §4 alone leaves a gap: the plate of «Explorar» has no «Exportar» — «Tasar esta lámina» took its
place (#282, decision 8) — so the wished holes of your **own** plates would print and the 157 slots
of the shelf window **never** would. Hence the output of its own: **«la lista de lo que busco»**,
exported from the annex, crossing both populations. It is one more `PrintSection` and not a second
printer: same cells, same grid, same five switches, and the notebook keeps having no second
architecture of information.

**Its eyebrow is its own, and that is not an exception to the sentence above.** The notebook already
has two — «COINDEX · CATÁLOGO CURADO» over a plate and «COINDEX · COLECCIÓN» over pieces — plus the
one #275 added for the coins no collection claims, and this page is none of the three: the casillas on
it come from as many catalogs as they come from, and it must not say «COLECCIÓN» over coins nobody
owns. The paper outlives the app, so a sheet that claimed a dealer's tray was a collection would be a
false claim in somebody else's hands. The **shape** of the eyebrow is the one the notebook has always
had, which is what «not a second printer» is about.

The printed notebook of the index is **untouched**: the list is exported from the annex, and the
annex is not a card of the index.

## Consequences

**The monthly pass stops being a number anybody can write in an ADR.** It was 487 calls; from here
«what a month costs» is a function of what the collector has marked — 1-2 calls per wish, for ever,
against a ceiling of ~1.500-2.000 (`CallBudgetGate`) **shared with Jose**. That is the app's first
elastic spend, and it is the reason the spend is spoken in the gesture and totalled in Ajustes.

**The dominant risk is not technical: it is `Followed` again.** If forty slots are marked and never
looked at, this is the intention that ages — now spending quota every month, out of a shared quota.
What contains it is that a wish dies measured (§2) and that its spend is visible (§5); what does not
contain it is anything automatic, on purpose. It is read in months, not in an afternoon.

**Version 10 of the schema is additive**, like the seven before it: one table, nothing dropped and
nothing rewritten.

**What this deliberately does not do.** The coin at the fair that is not a slot of any plate
**cannot be wished for**: «searching Numista» stays *Out of scope* of #15, so there is no search of
our own, no unseeded type and no call per wish to fetch a ficha the phone has never had. And the
**shelf window itself is not in this delivery** — the annex arrives holding one section, so the door
is short and the screen is named after the only thing in it.

That leaves **one clause of ADR 0026 §8 knowingly unimplemented**: clause 4, «an annex with a list
opens with its shelf folded — search and sort». It arrives with the twenty plates, which are what has
an order to choose: seven rows have one order — the slot marked most recently, first — so a sort
selector would offer to change nothing and a search box would filter a list that fits on one screen.
It is deferred and not refused: the clause is about an annex with a list, and this annex will have a
longer one.
