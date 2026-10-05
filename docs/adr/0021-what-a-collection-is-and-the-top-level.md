# ADR 0021: What a collection is, and what lives at the top level

- Status: accepted; amended by
  [ADR 0023](0023-country-names-are-cured-not-derived-from-numista-labels.md) (§4, §9),
  [ADR 0031](0031-family-labels-are-cured-like-country-names.md) (§4), #227, #228, #285, #354, #401,
  #431, #434 and #512 (§13), [ADR 0026](0026-the-shape-of-coindex-an-album-sheet.md) (§1, §9, §13),
  [ADR 0029](0029-a-wish-is-an-empty-slot-marked-on-the-phone.md) (§7),
  [ADR 0030](0030-the-shelf-window-of-explorar-is-valued-by-hand.md) (§7), #516 (§11)
- Date: 2026-08-04
- Supersedes ADR 0008. Amends ADR 0010 §2, §3 and §8, and ADR 0013.

## Context

ADR 0007 made a collection proposal a per-user view derived from holdings, ADR 0008 gave the
collector a durable disposition over each one, and ADR 0013 added curated groupings and a screen per
card. Around them the app grew an index of three blocks, an unclassified screen off the masthead,
four screens of pieces and six family aliases in a Kotlin `when`. Nothing had written down what a
collection is, what a card is called, in what order cards come, or where a coin lives when no
collection claims it.

The field report of #17 found 58 cards on the collector's phone, 33 with a catalog, 15 of those a
single slot, 4 complete, and all `Followed`, because following was the toll the plate charged rather
than an intention. He calls the whole thing «colección», in the singular; his main gesture is
exporting a plate as an image, and he has asked to export everything. Map #16 decided the model
recorded here. The number 0015 stays unused: filling it would misstate the order of decisions.

## Decision

### 1. The top level holds two sibling hierarchies: Collections and Coins

The app **opens in Collections**, and a **bottom bar of two destinations** crosses to **Coins** and
back. Coins is the other hierarchy, where a piece exists whether or not any collection claims it.
Consequences of the pair:

- **«Sin clasificar» stops being a masthead button** and becomes the **«Sin colección»** filter of
  Coins.
- **A coin links back to its collections, as a list**, because a type may be claimed by more than
  one (§10).
- **Medals are a filter, not a section.** Numista's `category` tells exonumia apart, and some live
  inside curated catalogs, so a «Medallas» section would tear them out of their plate.
- **Both sides carry filters, sorting and a live search** with per-option counts, accent
  insensitive, the shelf folded on entry. Filters and sort **persist** between launches; the search
  text does not.

A home screen asking which hierarchy you want was prototyped and rejected: a tap per launch to
choose the same thing every time.

> **Amended by [ADR 0026](0026-the-shape-of-coindex-an-album-sheet.md) §8 (2026-08-08).** Three
> sibling hierarchies: «Las cifras» joins, having a grain of its own, and each cell names its grain
> with its count (cards, types, grams). The shelf invariant narrows to the hierarchies with a list,
> and the shelf gains an axis facet (by plate, country, year). The consequences above, the rejected
> home screen and §2 are untouched.

### 2. There is one species of collection, and having a file is not a rank

Everything in the index is a collection: a curated catalog, a curated grouping, and a box the
collector enumerated by hand (`own_groupings`). They sit in one list under one comparator, with **no
block, no section and no word of provenance** telling them apart. A curated grouping is not an extra
view over a real collection, and a box is not a weaker collection.

A box is a **different act**, though. `buildOwnGroupingViews` filters `quantity > 0`, and by ADR
0020 a piece you do not own is not in the inventory, so a box **can never contain a gap** and its
only product is a sheet: a collection pursues, a box shows. That is exactly the absence of an issue
list (§3), with no hierarchy needed. Curated catalog and curated grouping **stay separate file
formats** (ADR 0013, 0016, 0020): a grouping has no members in which to write an exception, so it
can never point at a hole.

### 3. Provenance is a capability, not a label: the issue list

No provenance word goes on a card, neither one field («según Numista» / «curada a mano» / «tuya»)
nor two axes (source × confidence). Provenance is what the card does: it has an issue list, or it
does not. The two card phrases, in Spanish:

```
┌──────────────────────────────────┐   ┌──────────────────────────────────┐
│ Tudor Beasts                     │   │ Dólar de plata clásico           │
│ 2 oz · Bullion                   │   │ 1 oz · plata                     │
│ 4 de 12 · te faltan 8            │   │ 3 monedas · 2 tipos              │
└──────────────────────────────────┘   └──────────────────────────────────┘
```

No «sin lista de emisiones» line: «emisión» is curator's jargon, and a negative sentence on every
card apologises for what we do not give. The absence of progress is the signal.

ADR 0013's five-rung family ladder survives as **derivation**, not screen data (§12), and gains one
rule: **a grouping cannot join what the variant key splits**, so a family split by weight is cured
as a catalog. Rung 5 (Numista's technical `System YYYY`) stays as an invisible safety net, so no
piece is dropped for having only a technical family; its real cases, the Portuguese commemorative
escudos, are cured as a catalog (#157).

### 4. The name of a card is curated, and the collector renames nothing

The card used to paint `family`, the **grouping key**, while the plate painted `name`, the
**definition of editorial scope** (median 52 characters, maximum 200). The six aliases of
`Family.kt` papered over that category error.

**`short_name` enters the curated file: required, unique across the index, and a prefix of `name`.**
The file is the variant (ADR 0016), so the card's name belongs there. Uniqueness does the work: two
pairs of cards had shipped with identical titles because they shared a `family` (#163, #166).

- **Without a file, the card paints Numista's family as is.** An ugly name is a curation signal,
  fixed by curating or by an issue against Numista, never by a display alias, so **the six editorial
  aliases die**. The labelling rules left in code format or correct a generated string and never
  rename what a curator wrote: `System 1879-1936` → «Sistema monetario 1879-1936» (ADR 0012), the
  issuer codes whose Numista label is an entity with its period of validity (ADR 0023), and the
  curated family labels of ADR 0031.
- **Language rule for the corpus**: a series keeps the mint's language when it is in Latin script
  and is translated when it is not; everything the curator writes is in Spanish, country names
  included (`Ruanda`, not `Rwanda`).
- **A box carries one name, its `short_name`** (`name == short_name`: a hand-enumerated box has no
  scope to define), with a hard limit of **40 characters**, **unique at creation time**.
- `schema_version` **does not move**: it discriminates the species of a catalog, not a format
  version.

### 5. Identity is the file when there is one

`catalogId` for a catalog (as `Routes.plate` uses), the file id for a curated grouping,
`own_groupings.id` for a box, and the four-part variant key **only for cards no file names**. Name
and identity live in the same place. The variant key keeps grouping (ADR 0016, 0018) and the
dominant metal keeps splitting cards; what dies is the key as the identity of something persisted,
since after §7 nothing is stored per card. A card whose pieces are gone disappears without a trace
(ADR 0020), and renaming needs no undo because `short_name` lives in git beside `name` and `source`.

### 6. One default order, with the ratio inside it

The index is sorted by a **single comparator:
`(has ratio ↓, ratio ↓, denominator ↓, short_name ↑)`**, replacing two glued orderings (boxes first
by SQL, the rest by the raw key). No ADR had decided the order of the index before.

- **`has ratio`**: «te faltan 8» and «3 monedas · 2 tipos» are incomparable. It is a comparator
  level, not a block with a heading.
- **`ratio ↓`**: the index is a notebook that shows, not a list of chores, so complete plates come
  first.
- **`denominator ↓`**: `22/22` beats `2/2`.
- **`short_name ↑`** breaks ties, so the alphabetical order matches the text on the card.

**Boxes fall in the no-ratio stretch without privilege** (§2: they hold no gap). Accepted prices:
the order moves on sync (an open catalog stops being complete every January), and it is not
alphabetical, which §1 pays for with a persisted sort selector.

### 7. Dispositions are retired: a plate opens on evidence

**ADR 0008 is superseded in full.** `Followed`, `Available` and `Ignored` go, with their table,
gesture and name.

- **The plate condition is a current collection plus evidence by type.** `PlateUnavailable` drops to
  three reasons — `UnknownCatalog`, `NotACollection`, `NoEvidence`. Evidence is by type, not by
  issue, so a plate stays open while years are missing. Following was the only condition that said
  nothing about the world.
- **`Ignored` dies too.** It had no measured use, §1's persisted filters and search cover hiding,
  and one bit per derived card would keep the fragile four-part key alive.
- **No declarative comes back.** «Lo colecciono» is an intention that ages; `4 de 12` is a measured
  fact. That distinction is **derived from the ratio**, which is why §6 spends the ratio on the
  default order.
- **What the plate demands of the inventory does not change.** `NotACollection` stays: with no
  pieces of the variant there is no card and no plate. Cutting the toll does **not** open the 51
  catalogs to navigation — that would be new capability against ADR 0007, and is not decided here.

> **Amended by [ADR 0029](0029-a-wish-is-an-empty-slot-marked-on-the-phone.md) (2026-08-14): one
> declarative comes back, and it is not the one this section killed.** Nothing is stored per card
> and `Followed` stays dead. What comes back is a mark on **an empty slot**, a wish, which dies
> measured when a sync brings that type and the slot fills. It takes §11's key, not §8's table —
> `typeId`, because Numista row ids are replaced on every sync — in **a table of its own**, so «a
> wish is not a piece» holds by construction.

> **Amended by [ADR 0030](0030-the-shelf-window-of-explorar-is-valued-by-hand.md) (2026-08-14):
> twenty catalogs open without evidence.** The curated catalogs with **no evidence and fewer than
> twenty measurable casillas** open as the shelf window of «Explorar», as deliberate new capability;
> `NoEvidence` stays the answer for every catalog over that cut. Such a plate is open and not yours:
> no «Exportar», one money figure instead of two, and a gesture that spends. Its population is
> derived from the evidence, like the ratio, so nothing is stored per card.

### 8. «Proposal» stops being the word

A proposal is offered for you to accept, and accepting it was «Seguir»; with the toll gone nothing
is proposed. The word dies **in screen and domain alike**, because `CONTEXT.md` exists so that the
phone and the code say the same thing. The mechanical sweep is #162.

### 9. One card, one destination

The destination is chosen by the capability of §3, not by a declared species:

| Card | Destination |
| --- | --- |
| **With** an issue list | the **plate**, in one tap |
| **Without** one — no file, or a curated grouping | `PiecesScreen` |
| A collector's box | the same `PiecesScreen`, plus its maintenance |

`ProposalScreen` and `OwnGroupingScreen` merge; `UnclassifiedScreen` already dissolved in §1. Four
screens of pieces become two.

**The plate does not merge, because nothing falls outside it.** Over the 1033 curated slots of
`data/` at the time, no piece in a card with a catalog missed its plate and no slot merged more than
one row. The plate already says `Tengo · ×3` and paints both faces, the year and the Numista link,
so a piece list beside it would add nothing.

Also settled: box maintenance is **an `if`, not a screen**; `PiecesScreen` **exports as an image**,
because a box's only product is a sheet (ADR 0020); and a collection without an issue list shows its
pieces where the plate would go, with no hole and no «could have a catalog», which would be a
provenance label in disguise. The card's eyebrow was the country, from the file's `issuer_code`
where there is one and from the pieces otherwise, named by the ficha or by the curator (ADR 0023).

> **Amended by [ADR 0026](0026-the-shape-of-coindex-an-album-sheet.md) §12 (2026-08-08).** The card
> becomes a die-cut hole with its coin, so the eyebrow and the variant line die and the country goes
> back to being a facet. The rest of this section stands.

### 10. Membership is curated, declared on the type, and never hierarchical

There is **no derived multiple membership**: every membership is written by hand in a file, so no
criterion fires on its own and there is nothing to bound. **Curated multiple membership has no home
and no extra view**: the collections that name a type show it on equal footing, each with its own
ratio and gaps. `deriveCollection` fabricates cards only for what no file claims, and the
`check(matchingCatalogs.size <= 1)` in `CollectionDerivation.kt`, which broke ties between
fabricated keys, **dies as unnecessary**.

- **Two hands, one species.** The curator in `data/` and the collector on the phone can both create
  an overlap, indistinguishable on screen. The collector's working channel is saying it out loud and
  the curated file arriving in a release.
- **Membership is declared on the type, with the issue as a tie-break, never on an inventory row.**
  Three Morgans all enter, being the same coin, and the file reads without anyone's inventory, so
  one file serves both collectors.
- **A piece whose issue is unknown stays out of every collection.** Numista files bullion and proof
  coloured under one type in three Lunar III years, separated only by `numista_issue_ids`, and a
  bullion piece in the proof plate would lie about a hole. Such a row falls in «Sin colección» and
  the report catches it (§12).
- **It is implemented with the first overlapping collection** (`singleOrNull()` to a list, the
  `check` removed). Until then the `curate-catalog` skill guards it (#171), because publishing such
  a file stops the app at startup.

### 11. A collector's box is born filtering, in Coins

The box **is created in the app**, and `own_groupings` survives §7; the old gesture
(`SelectionControls`, called only from screens §9 removes) does not.

You filter or search in Coins → the button says **«Agrupar estas 6»** → the mode opens with those 6
picked → you drop the ones you do not want → you name it. **The button seeds only when the filter
has narrowed something**; with no filter it enters empty, since «Agrupar estas 191» would make a
two-coin box a matter of unticking 189. The count in the button states the cost up front. Membership
freezes into `typeIds` at creation (§10).

Renaming, dropping a type and undoing the box live in the `if` of §9; **extending is done from
Coins**, where the coins to add are. The **empty box survives with its zero**: the one thing the
collector typed is never deleted on its own. Its eyebrow is the country of its pieces, silent when
they disagree. **Collision with a later file is not policed**: uniqueness is checked at creation,
and two homonymous cards in the index mean curation caught up; the box is undone with one tap.

*Nota de forma, #516: el botón dice «Hacer una colección con estas 6», y sin filtro «Hacer una
colección». La cláusula no cambia; cambia la palabra. El gesto tenía tres nombres («agrupar», «la
caja», «colección»), y «caja» era la palabra de procedencia que retiró el §2: en la interfaz esto es
una colección, aunque `own_groupings` y `OwnGrouping` no se renombran. El diálogo se rotula «Tu
colección» y su titular es el recuento a secas («2 monedas elegidas»), para no repetir la palabra de
especie (ADR 0026 §5). El chip de onzas `OunceBand.Spanning`, antes «Conjunto o caja», dice «Varias
onzas»: es lo que la faceta pregunta, y vale para el conjunto del ADR 0012 y para la caja.*

### 12. The app is not an audit surface; disagreement is reported outside it

The matching is audited **outside the app, by the curator**, and the collector corrects it by
telling the curator. So there is **no reason line on a coin's card** and no «esta no va aquí»
gesture, and the four per-piece reasons of ADR 0010 §3 leave the app: the «Sin colección» filter
shows *which* pieces are out, and the *why* goes to the field report. **«Nothing is discarded
silently» becomes «nothing is discarded»**, since a piece always lives in Coins.

What is worth auditing is **disagreement**: members whose weight, normalised from Numista's grams,
is not the one their catalog declares (ADR 0016). It ships as a report shaped like
`stale-catalogs.py` — no network, never red, `--sync` keeping one issue up to date (#158, #168).
**Red when the finding is rare, a report when it is routine**: the metal cross-check stays a test,
silenced member by member with `variantNote`, while a weight test would be red 124 times on day one
with nearly every note saying «Numista varies its grams».

`ManualOverride` stays out, but ADR 0010 §2's reason («proposals are derived deterministically … so
there is nothing to correct by hand») **is false**: dozens of types run heuristics (the weight
magnet, finish from the title, metal from prose). The true reasons: correcting on a phone fixes one
phone while curing the catalog fixes both; it would compete with the catalog ADR 0016 made
authoritative; and it would need a destination, which §10 decides.

### 13. Exporting: a plate is a PNG, the notebook is the printed index

**The notebook is the index printed**, not an object: no entity, no table, no naming, no cover page.
§4 and §6 gave it a canonical order and a card-sized title per page, and the index already is the
«colección» in the singular.

- **What prints is what the index is showing.** The filter of §1 is the selection, so arbitrary
  selection needs no mechanism, and no dialog asks for confirmation.
- **`page(card) = its destination`** (§9): a card with a list prints its plate, one without prints
  the sheet `PiecesScreen` exports, and a box comes through that same door.
- **What fits in one page is a PNG; what needs more is a PDF** (#401), measured by the pages the
  options panel computes on A4; the notebook from the index is always the PDF. The PNG is the
  printed page with its unused height cut off (#431), so every switch reaches it, the ruler
  included. `recordInto` records drawing commands in a `Picture` and `PdfDocument` replays them, so
  the PDF is vectorial with **no new dependencies**. **Descargar** puts the file in
  `MediaStore.Downloads` with no chooser and no permission, beside **Compartir** and its send intent
  (#285).
- **How it prints is chosen in one panel, «Cómo se exporta»** (#228): independent switches over the
  live page count, which is arithmetic over what the index shows and so cannot live in Ajustes. It
  is a card, not a modal, and nothing in it is stored per card (§7). A lámina or a hoja opens the
  same panel minus «Sin colección» and «Compartir página» (#401) through **one door**, «Exportar
  lámina» / «Exportar hoja» (#434), which is **gone while the panel is open** and back with
  «Cancelar», and greys naming the state («Preparando la lámina…») while an export runs (#512). The
  index keeps its door in the filter shelf. No button names the size of the export: the tally and
  the panel own those counts (#354). The switches are ruled squares ticked by hand, the whole 48 dp
  line as target (#512).
- **By default, A4 with one plate per page**, header repeated when a plate continues; neither
  shrinking a plate to fit (121 slots would print at some 8 mm per coin) nor a continuous flow. Two
  plates per folio is a switch (#232).
- **By default, one face on paper, at life size.** At 1:1 both faces side by side take `2·Ø`, and A4
  keeps two columns only up to Ø ≤ 40.5 mm, which punishes exactly the ounces. The face is the
  catalog's `printed_side` (ADR 0020, #227), reverse when absent: reverses vary inside a plate far
  more than obverses, but on the 50 gourdes of Haiti the face that **is** the coin, the mermaid, is
  the obverse. The other face is a tap away in the app. Both faces (#230) and a scaled coin (#233)
  are switches. **A hole occupies its full diameter**, like an owned coin.
- **The grid is set by each plate's largest diameter.** At 1:1 a constant grid is unnecessary, and
  per-plate grids cut the 50-plate notebook from 278 pages to **84**. `SheetLayout`'s `scale` dies
  **on paper**, where it made printed size depend on the length of a series.
- **A 50 mm ruler at the foot of every page**, because viewers print «fit to page» by default and
  would silently break the 1:1. It goes when the 1:1 goes.
- **Paper and screen diverged on purpose**: the shared PNG kept the screen sheet's `sqrt` grid, both
  faces and `scale`, because on screen the millimetre does not exist. *Obsolete since #431*: the PNG
  is the notebook page cropped to its content (`SheetPngExport`), with the same switches as the PDF,
  and `PlateSheet` with its grid is gone (ADR 0010 §8). The plate on screen is ADR 0026's album
  sheet.

> **Amended by [ADR 0026](0026-the-shape-of-coindex-an-album-sheet.md) §4 and §10 (2026-08-08).** An
> export rule — what is still travels to paper, what follows the finger, the sensor or the
> navigation stays in the app — and a **sixth switch**, the money.

## The data migration (v5)

Specified here, implemented by #164, #167 and #169:

- **`DROP TABLE collection_proposal_preferences`**, forward-only and rescuing nothing, as ADR 0008
  required of any rollback: the rows record a toll, not a preference. If archiving is ever needed,
  the bit is rebuilt from zero. `exportSchema = true` leaves `5.json` documenting it, and the `DROP`
  enters `MigrationSqlTest` like v4 did.
- **`own_groupings` and `own_grouping_members` stay intact** (§11).
- **The plate condition gets tests**: the three remaining branches of §7 had none.

## Consequences

- **The index becomes one list**: one comparator, persisted filters and a search replace three
  blocks, the disposition gesture, the masthead unclassified button and the empty «Tus agrupaciones»
  heading. A new installation costs 33 fewer taps.
- **The curator inherits work the app used to fake**: the `short_name` values (#163), the Portuguese
  escudos (#157) and whatever an ugly raw family exposes. Curating fixes both phones; correcting on
  a phone fixes one.
- **Two reports and one test carry what the app stopped saying**: the weight-disagreement report
  (#158), the year-blind row report (#168) and the metal test.
- **No new field in any curated file except `short_name`**, no new table, no new API call: every
  facet §1 and §13 need (`weight`, `size`, `min_year`, `issuer`) was already in the cache.
- Implementation order: #162 (the word), #164 (the comparator), #167 (the two screens of pieces and
  the bottom bar), #173 (the box), #169 (the notebook, after #167).

## Documents this ADR changes

- **ADR 0008 → `superseded by ADR 0021`**, body intact with a line pointing forward: it has no false
  sentence, only a subject that stops existing, and it explains the disposition rows left in the
  collector's phone and why `MIGRATION_3_4` repopulated only 30 literal keys.
- **ADR 0010**: §2 loses its false reason (§12), §3's orphan reasons become report data, and §8 is
  extended by §13 for paper.
- **ADR 0013** loses «the proposal screen is what you own, the plate is the catalog with its gaps»
  (§9) and the sentence calling a curated grouping an extra view (§2, §10). ADR 0007 is untouched;
  ADR 0016, 0018, 0019 and 0020 are upheld.
- **`spec.md`**: §0.3 drops the dispositions and the aliases and restates the plate condition; §0.4
  stops naming the frozen web as the reference UI; §1 keeps auditability, now **by the curator**.
- **`CONTEXT.md`** loses «Available proposal», «Followed collection proposal», «Ignored proposal»,
  «Family display alias» and the dispositions clause of the variant key, and gains the vocabulary of
  §2, §3, §4 and §6.

## Alternatives considered

- **A provenance label on the card**, or a `source_authority` enum in the files: a line on every
  card to disambiguate a few, about something the collector never asked. The axis that matters is
  already in `source`, `source_note` and `series_status`, where prose cites and an enum would be
  filled in by eye.
- **Letting the collector rename any collection**: the one renameable thing, the box, was empty on
  both phones, and naming a scope is curation.
- **Cutting `name` at the first `·`** instead of adding `short_name` collapses 12 files into 5
  names; **renaming the ugly families in `data/`** and painting `family` requires `family` to be
  unique per card, but variants share a family on purpose.
- **A new declarative to replace «Seguir»** («lo colecciono», «me interesa»): the same thing with
  another label and the same fragile key.
- **Drawing every notebook page as a plate**: the page would claim something to pursue.
- Rejected in their sections: a home screen (§1), keeping `Ignored` (§7), merging the plate into the
  piece list (§9), bounding multiple membership with a cap or a home collection (§10), an audit
  surface in the app or a rescue screen for the year-blind row (§12), and the notebook as an entity
  with a cover, an appendix of listless cards, a plate shrunk to one A4 or a constant grid (§13).
