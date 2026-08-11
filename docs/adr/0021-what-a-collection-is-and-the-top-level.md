# ADR 0021: What a collection is, and what lives at the top level

- Status: accepted, §4 and §9 amended by [ADR 0023](0023-country-names-are-cured-not-derived-from-numista-labels.md), §13 amended by #228, #227, #354, #285 and #401, §1 and §9 amended by [ADR 0026](0026-the-shape-of-coindex-an-album-sheet.md)
- Date: 2026-08-04
- Supersedes ADR 0008. Amends ADR 0010 §2, §3 and §8, and ADR 0013.

> **Amended on 2026-08-05.** The model below stands whole. What ADR 0023 corrects is one inventory:
> the labelling rules left in code are **two** and not one, because Numista does not write countries
> — it writes issuing entities with their period of validity, and nine of the 40 issuer codes in the
> cache reach a card as «Federación de Rusia (1991-presente)» rather than as «Rusia» (#180).

> **Amended on 2026-08-06 (§13).** What the notebook *is* stands whole — the index printed, no
> entity, no cover, no second order — and so does every measurement §13 made. What stops being true
> is that there is **one** output. #228 makes the layout configurable behind five independent
> switches, so three sentences of §13 change status without changing content:
>
> - «Only the reverse goes on paper, at life size» and «one plate per page» become the **defaults**
>   rather than the invariants. They are still what the measurements chose and still what nobody who
>   asks for nothing gets; a collector who wants both faces (#230), a scaled coin (#233) or two plates
>   in a folio (#232) can now say so. The 1:1 ruler is still the falsifiability of the 1:1 page, and
>   it goes when the 1:1 goes.
> - «the cost … is mitigated by the button saying how much it will export, **not by a dialog**»
>   survives as written about **confirming** and is overtaken about **choosing**. The button still
>   names the size of what it starts; what it now opens is a surface for deciding how, whose
>   load-bearing line is the live page count — arithmetic over what the index is showing at that
>   moment, which is why it cannot live in Ajustes. It is a card in the index and not a modal.
>
> Nothing here is stored per card, so §7 is untouched: how a notebook is printed is what the
> collector is looking through, like the filters of §1, not something recorded about a collection.

> **Amended on 2026-08-09 (§13, #354).** The button no longer names the size of the export. The live
> tally beside it already owns the number of visible collections, and the options sheet owns the
> honest plate and page counts after applying the persisted «Sin colección» option. Repeating
> `shown.size` in the action was therefore redundant and could be false by one plate. The action is
> count-free instead of duplicating the preview formula in a second owner.

> **Amended on 2026-08-10 (§13, #285).** The PNG of a plate and the PDF of the notebook still leave
> through the send intent when the collector asks to share — that gesture is Jose's. What the
> father asked for is Descargar: one tap into `MediaStore.Downloads`, no chooser, no permission,
> with Compartir beside it. The unit still decides the format; the destination is no longer only
> the share sheet.
>
> **Amended on 2026-08-11 (§13, #401).** Descargar and Compartir on a lámina or a hoja open the same
> «Cómo se exporta» surface as the index — minus «Sin colección» and «Compartir página». **What fits
> in one page is a PNG; what needs more is a PDF**, measured by the pages the panel already
> computes — not by which screen was opened. Switches that only a bitmap cannot honour are annotated
> when the result is a PNG; when it is a PDF they apply for real.

> **Amended on 2026-08-06 (§13, #227).** «If one face must be chosen it is the **reverse**» was two
> claims wearing one sentence, and only the first was ever measured. That one face goes on paper at
> 1:1 stands whole. Which one does not: the count of 121 distinct reverses against 17 obverses
> measures what *varies* inside a plate, and what an album page wants is the face that **is** the
> coin — on the 50 gourdes of Haiti that is the mermaid, and she is on the obverse. So the reverse
> becomes the **default**, and which face goes to paper is now a declaration of the catalog,
> `printed_side` (ADR 0020, #227). The rest of the bullet is untouched: the obverse is still a tap
> away, and a hole still occupies its full diameter.

> **Amended on 2026-08-08 (§1 and §9, [ADR 0026](0026-the-shape-of-coindex-an-album-sheet.md)).** The
> model stands whole; what changes is the count of the bar and one line of a card.
>
> - **§1: two sibling hierarchies become three.** «Las cifras» joins Collections and Coins, because
>   what earns a cell is having a **grain of its own** — and the test that decides it is written in
>   ADR 0026 §8. Each cell now names its grain with its count (cards, types, grams). The shelf
>   invariant — filters, sort, live search, folded on entry — narrows to the hierarchies **with a
>   list**, which are two of three, and the shelf gains one facet, **the axis** (by plate, by country,
>   by year). The three consequences of §1 and the rejection of a home screen are untouched, because
>   they follow from Collections **and** Coins existing, not from their being exactly two. §2 is
>   untouched: there is still one species of collection.
> - **§9: the eyebrow stops being the country.** The card stops being four lines of text and becomes a
>   die-cut hole with its coin inside, so the hierarchy the eyebrow used to give is now the die-cut's
>   job and the country goes back to being a facet. The variant line dies with it. The rest of §9 —
>   the destination chosen by the capability of §3, the plate that does not merge, box maintenance as
>   an `if` — stands.
> - **§13 gains an export rule** — what is still travels to paper, what follows the finger, the sensor
>   or the navigation stays in the app — and a **sixth switch**, the money.

## Context

ADR 0007 made a collection proposal an ephemeral, per-user view derived from holdings; ADR 0008 gave
the collector a durable disposition over each one; ADR 0013 added curated groupings and a screen per
card. Around them the app grew an index of three blocks, an unclassified screen hanging off the
masthead, four screens of pieces, and six family aliases in a Kotlin `when`. Nothing ever wrote down
what the first level *is* — what a collection is, what a card is called, in what order the cards
come, or where a coin lives when no collection claims it. `spec.md §0.4` still described the frozen
Rust web as the reference UI.

The gap became visible when the app was measured against its user instead of against its spec. The
field report of #17 found **58 cards** in the collector's phone — 33 with a catalog, **15 of those
plates a single slot**, only 4 complete — all of them `Followed`, because following was the toll the
plate charged and not an intention. He calls the whole thing «colección», in the singular. His main
gesture is exporting a plate as an image, and he has asked to export everything. «Tus agrupaciones»
is empty on both phones.

Map #16 spent twelve tickets, four of them with a prototype running on a device, deciding the model
this ADR records. The measurements that decided each one live in the tickets; what follows is the
model.

**This is ADR 0021 and not 0015.** The 0015 hole stays a hole: filling a number that predates 0019
would lie about the order in which things were decided (the same verdict map #14 reached when
publishing 0020).

## Decision

### 1. The top level holds two sibling hierarchies: Collections and Coins

The app **opens in Collections** and a **bottom bar of two destinations** crosses to **Coins** and
back. Coins is not a view inside a collection and not a reading of the index: it is the other
hierarchy, where a piece exists whether or not any collection claims it.

Consequences that follow from the pair rather than from taste:

- **«Sin clasificar» stops being a masthead button** and becomes the **«Sin colección»** filter of
  Coins, which is what it always was.
- **A coin links back to its collections**, as a list, because a type may be claimed by more than
  one (§10).
- **Medals are a filter, not a section.** `category` distinguishes 13 exonumia among 804 seeded
  types and **four of them already live inside curated catalogs**, so a «Medallas» section would
  have to tear them out of their plate.
- **Both sides carry filters, sorting and a live search** with per-option counts, accent
  insensitive, the shelf folded on entry. Filters and sort **persist** between launches; the search
  text does not.

A home screen that asks which hierarchy you want was prototyped and rejected: it charged a tap per
launch to choose the same thing every time.

### 2. There is one species of collection, and having a file is not a rank

Everything in the index is a collection: a curated catalog, a curated grouping, and a box the
collector enumerated by hand (`own_groupings`). They sit in one list, sorted by one comparator, with
**no block, no section and no word of provenance** telling them apart. A curated grouping is **not
an extra view** over a real collection, and a box is not a weaker collection.

A box is, however, a **different act**. `buildOwnGroupingViews` filters `quantity > 0`, and by
ADR 0020 a piece you do not own is not in the inventory, so a box **can never contain a gap** and
its only possible product is a sheet — *a collection pursues, a box shows*. That difference needs no
hierarchy to state it: it is exactly the absence of an issue list (§3).

Curated catalog and curated grouping **deliberately do not collapse into one file format** (ADR
0013, 0016, 0020): a grouping has no members in which to write an exception, so it can never point
at a hole.

### 3. Provenance is a capability, not a label: the issue list

There is no provenance word on a card — neither one field («according to Numista» / «curated by
hand» / «yours») nor two axes (source × confidence). Provenance is visible in what the card *does*,
and the real values are **two, and neither is a word**: it has an issue list, or it does not.

The two card phrases are fixed, in Spanish:

```
┌──────────────────────────────────┐   ┌──────────────────────────────────┐
│ Tudor Beasts                     │   │ Dólar de plata clásico           │
│ 2 oz · Bullion                   │   │ 1 oz · plata                     │
│ 4 de 12 · te faltan 8            │   │ 3 monedas · 2 tipos              │
└──────────────────────────────────┘   └──────────────────────────────────┘
```

No «sin lista de emisiones» line: «emisión» is curator's jargon, and a negative sentence repeated on
every card apologises for what we do not give. The absence of progress *is* the signal.

The five-rung family ladder of ADR 0013 survives untouched as **derivation**, and stops being
screen data: it is audit material (§12). It also gains a sentence — **a grouping cannot join what
the variant key splits**, so a family broken apart by weight is cured as a catalog, not as a
grouping. Rung 5 (Numista's technical `System YYYY`) stays as an invisible safety net: no piece is
ever dropped for having only a technical family, and its only six real types — the Portuguese
commemorative escudos, today five cards with two identical titles — are cured away as a catalog
(#157).

### 4. The name of a card is curated, and the collector renames nothing

The card never had a name. It painted `family`, which is the **grouping key**, while the plate
painted `name`, which is the **definition of editorial scope** (median 52 characters, maximum 200).
**Painting the family on a card that is a variant is a category error, and the six aliases of
`Family.kt` were the scar.**

**`short_name` enters the curated file: required, unique across the index, and a prefix of `name`.**
The file *is* the variant (ADR 0016), so the file is where the card's name belongs. Uniqueness does
the real work: two pairs of cards shipped with identical titles because they shared a `family`. This
half was already implemented ahead of the ADR — 53 files curated one by one (#163) and painted by
#166, shipped in v0.12.0; what #164 still owes is the comparator of §6.

- **Without a file, the card paints Numista's raw family verbatim** — 18 cards measured, 15 of them
  a single type. An ugly name is a curation signal, fixed by curating or by opening an issue against
  Numista, never by a display alias. **The six editorial aliases die**; the labelling rules left in
  code are `System 1879-1936` → «Sistema monetario 1879-1936», which is formatting of a generated
  string (ADR 0012), and the nine issuer codes whose Numista label is an issuing entity with its
  period of validity rather than a country (ADR 0023).
- **Language rule for the corpus**: the series in the mint's language when it is already in Latin
  script, translated when it is not — there the translation *is* the readable name — and everything
  the curator writes in Spanish, country names included (`Ruanda`, not `Rwanda`).
- **A box on the phone carries one name, and it is the `short_name`**: `name == short_name`, because
  a box enumerates by hand and has no editorial scope to define. Hard limit of **40 characters**
  (measured: the 53 files run from 6 to 37, median 20) and **unique at creation time**.
- `schema_version` **does not move**: it is the species discriminator of a catalog, not a format
  version.

### 5. Identity is the file when there is one

`catalogId` for a catalog (which `Routes.plate` already uses), the file id for a curated grouping,
`own_groupings.id` for a box, and the four-part variant key **only for the cards no file names**.
Name and identity end up in the same place.

The variant key does not die: it keeps grouping (ADR 0016, 0018), and the dominant metal keeps
splitting cards. What dies is **its status as the identity of something persisted** — after §7 there
is nothing stored per card at all. Nothing survives dormant either: by ADR 0020 a piece you do not
own is not in the inventory, so a card disappears without leaving a trace, and «is renaming
reversible?» has no case because `short_name` lives in git with the original still visible in `name`
and `source`.

### 6. One default order, with the ratio inside it

The index is sorted by a **single comparator: `(has ratio ↓, ratio ↓, denominator ↓, short_name ↑)`**,
replacing the two glued orderings of today (boxes first by SQL, the rest by the raw key). **No ADR
had ever decided the order of the index.**

Why each level: **`has ratio`** because «te faltan 8» and «3 monedas · 2 tipos» are incomparable
magnitudes — and it is a level of the comparator, not a block with a heading, since §7 removes three
blocks and none comes back; **`ratio ↓`** because the index is a notebook that shows rather than a
list of chores, and the collector's four complete plates are today scattered through the list with
nothing to distinguish them; **`denominator ↓`** so that `22/22` beats `2/2`; **`short_name ↑`** to
break ties, which finally makes the alphabetical order agree with the text on the card.

**Boxes fall in the no-ratio stretch without privilege** — by §2 they cannot hold a gap, so they
have no ratio to offer — and the empty heading block both phones show at the top disappears. Two
accepted prices: the order moves on sync (an open catalog stops being complete every January, which
is honest), and it is not alphabetical, which §1 already paid for with a persisted sort selector.

### 7. Dispositions are retired: a plate opens on evidence

**ADR 0008 is superseded in full.** `Followed`, `Available` and `Ignored` are gone, with the table,
the gesture and the name.

- **The plate condition is: a current collection plus evidence by type.** `NotFollowed` leaves
  `PlateUnavailable`, which drops from four reasons to three — `UnknownCatalog`, `NotACollection`,
  `NoEvidence`. Evidence is **by type and not by issue**, which is what keeps a plate open while
  years are missing. Following was the only one of the four conditions that said nothing about the
  world: the other three describe the inventory, and this one said «tap here first».
- **`Ignored` dies too**, even though with the toll cut it could now hide a card without closing its
  plate. Zero measured cases (all ~58 stored rows are `Followed`, both boxes empty, and the
  collector's complaint is coverage he lacks and not noise he has), §1 already bought the
  alternative in persisted filters and search, and one bit per derived card would force the whole
  fragile four-part key to be kept alive.
- **No declarative comes back.** «Lo colecciono» is an intention that ages; `4 de 12` is a measured
  fact. The distinction #17 asked for is **derived from the ratio**, which is why spending the ratio
  on the default order (§6) is an obligation and not an option.
- **What the plate demands of the inventory does not change.** `NotACollection` stays: with no
  pieces of the variant there is no card and no plate. Cutting the toll does **not** open the 51
  catalogs to navigation — that would be new capability against ADR 0007, and is not decided here.

### 8. «Proposal» stops being the word

A proposal is something offered for you to accept, and accepting it was exactly «Seguir». With the
toll gone nothing is being proposed. The word dies **in both layers** — screen and domain — because
`CONTEXT.md` exists so that the phone and the code say the same thing, and letting it die only on
screen would manufacture a permanent drift between «Colecciones» on the phone and `Proposal` in 148
places in the code. This ADR declares the word; the mechanical sweep is #162.

### 9. One card, one destination

The destination is chosen by the capability of §3, not by a declared species:

| Card | Destination |
| --- | --- |
| **With** an issue list — 50 catalogs | the **plate**, in one tap |
| **Without** one — 18 without a file, plus the curated groupings | `PiecesScreen` |
| A collector's box | the same `PiecesScreen`, plus its maintenance |

`ProposalScreen` and `OwnGroupingScreen` merge; `UnclassifiedScreen` was already dissolved by §1.
Four screens of pieces become two.

**The plate does not merge, because nothing fits inside it.** Measured against a faithful inventory
of the 1033 curated slots of `data/`: the pieces that fall in a card with a catalog and in **no
slot** of its plate are **0**, and no slot merges more than one row. `plateMemberStateLabel` already
says `Tengo · ×3` and `PlateCell` already paints both faces, the year and the Numista link, so the
list of pieces of a card with a plate shows nothing the plate does not — the jump was 38 rows to
reach the same thing.

Also settled here: the eyebrow vacated by «Propuesta de colección» is **the country**, taken from
the file's `issuer_code` where there is one (all 50 declare it) and from the pieces where there is
not — and the *name* of that country is the ficha's for 31 of the 40 codes and the curator's for the
nine Numista labels that are not country names (ADR 0023); box maintenance is **an `if`, not a
screen**; `PiecesScreen` **exports as an image**, because by ADR 0020 the only product a box can have
is a sheet; and a collection without an issue list shows its pieces where the plate would go — no
hole, no promise, no «could have a catalog», which would be a provenance label in disguise.

### 10. Membership is curated, declared on the type, and never hierarchical

There is **no derived multiple membership**: every membership is written by a hand in a file, so no
criterion fires on its own, there is no combinatorial explosion to bound and no cap to invent.

**Curated multiple membership has no home and no extra view.** The collections that name a type show
it on equal footing, each counting its own ratio with its own gaps. The «home» turned out to be
mechanical rather than conceptual: `deriveCollection` fabricates cards only for what no file claims,
and the `check(matchingCatalogs.size <= 1)` of `CollectionDerivation.kt:122` — which crashes the
app today —
existed to break ties between *fabricated* keys. It **dies as unnecessary**, and derivation by family
is relegated to what no file names: the 18 cards without a file and the orphans.

- **Two hands, one species.** The overlap can be created by the curator in `data/` and by the
  collector on the phone, and on screen they are indistinguishable. This matters because the
  collector creates nothing on screen: he says «esta debería estar con los fuertes» out loud, and the
  channel that works is the curated file arriving in a release.
- **Membership is declared on the type, with the issue as a tie-break, never on an inventory row.**
  Your three Morgans all enter, because they are the same coin, and the file reads without anyone's
  inventory in front of it — which is what lets one file serve both collectors.
- **A piece whose issue is unknown stays out of every collection.** Numista files bullion and proof
  coloured under one type in three Lunar III years, and only `numista_issue_ids` separates them:
  filling the ox slot of the proof coloured plate with a bullion piece would lie about a hole, and
  the hole is the plate's product. Such a row falls in «Sin colección» and is caught by the report
  (§12), not by a screen.
- **It is implemented when the first overlapping collection exists**, not before: the domain change
  (`singleOrNull()` to a list, burial of the `check`) travels inside that first curation. Until then
  the risk is disarmed on the skill side (#171), because publishing such a file today does not
  produce a strange card — it stops the app at startup.

### 11. A collector's box is born filtering, in Coins

The box **is created in the app**, and `own_groupings` does not fall with the table of §7. The old
gesture does: `SelectionControls` had exactly two call sites, both on screens §9 removes.

You filter or search in Coins → the button says **«Agrupar estas 6»** → the mode opens with those 6
already picked → you drop the ones you do not want → you name it. **The button seeds only when the
filter has already narrowed something**; with no filter it enters empty, because seeding always
offered «Agrupar estas 191» and an arbitrary two-coin box would be made by unticking 189. The count
in the button puts the cost up front. Membership freezes into `typeIds` at creation (§10), so
changing the filter afterwards does not touch the box.

The rest of the life cycle: renaming, dropping a type and undoing the box live inside the `if` of §9;
**extending is done from Coins**, because whoever wants to add is looking at the coins they want to
add. The **empty box survives with its zero**, with no privilege and no punishment — the one thing
the collector typed is not deleted on its own. Its eyebrow is the country of its pieces, silent when
they disagree, which here is not the category error of §9 because with no file the pieces are the only
authority there is. **Collision with a later file is not policed**: uniqueness is checked at creation
and there it ends; two homonymous cards in the index are the signal that curation has covered what he
noted down by hand, and the box is undone with one tap.

### 12. The app is not an audit surface; disagreement is reported outside it

The matching is audited, but **outside the app and by the curator**; the collector's channel for
correcting it is telling you, which already works. So there is **no reason line on a coin's card**,
no «esta no va aquí» gesture, and the four per-piece reasons of ADR 0010 §3 leave the app: the «Sin
colección» filter shows *which* pieces are out, and the *why* migrates to the field report, which is
where the curator already looks. **«Nothing is discarded silently» becomes «nothing is discarded»**,
which is true now that a piece lives in Coins.

What is worth auditing is the **disagreement**: the 124 of 720 members whose weight normalized from
Numista's grams is not the one their catalog declares. That is the matching contradicting the ficha,
in silence and on purpose (ADR 0016). It ships as a report with the shape of `stale-catalogs.py` —
zero network, never red, `--sync` keeping a single issue up to date (#158, #168).

**Red when the finding is rare, a report when it is routine.** The metal cross-check stays a test
that goes red and is silenced member by member with `variantNote`; the weight check is born a report,
because a test would be red 124 times on day one and 122 of those notes would say «Numista varies its
grams».

`ManualOverride` stays out, but the reason written in ADR 0010 §2 — «proposals are derived
deterministically, with no heuristics, so there is nothing to correct by hand» — **is false**: 84 of
the 804 types run three heuristics (the weight magnet, finish from the title, metal from prose), and
one of them crosses catalogs. The true reasons: correcting on a phone fixes one phone while curing
the catalog fixes both and forever; it would be a second authority competing with the catalog that
ADR 0016 just crowned; and it would need a destination, which is what §10 decides.

### 13. Exporting: a plate is a PNG, the notebook is the printed index

> Amended by #228 and #401 — see the notes at the top. The layout below is now the **default** and
> no longer the only output, and both the index export button and a single lámina or hoja open a
> sheet of switches with the live page count under them. A single sheet drops the two index-only
> questions; the format follows the page count (one → PNG, more → PDF), and the notebook from the
> index stays the PDF.

**The notebook is not an object; it is the index printed.** It needs no entity, no table, no naming
and no cover page, because §4 and §6 gave it the two things it lacked: a canonical order and a
card-sized title per page. Saying «colección» in the singular does not ask for an object to write a
name on — the index already is one.

- **What prints is what the index is showing.** The filter of §1 *is* the selection, so arbitrary
  selection needs no mechanism of its own. The cost — content depending on a state that may have
  been set days ago — is mitigated by the button saying how much it will export («Exportar 12
  láminas»), not by a dialog.
- **`page(card) = its destination`**, inherited from §9: a card with a list prints its plate, a card
  without one prints the sheet `PiecesScreen` already exports, and a box comes in through that same
  door. Two kinds of page, none invented.
- **What fits in one page is a PNG; what needs more is a PDF** (#401). Measured by the pages the
  options panel already computes — Descargar or Compartir on a lámina or hoja leaves a bitmap when
  there is one page, and the vector PDF of that section when there are more («Plata a valor facial»
  is two). The notebook from the index is always the PDF. The cost line announces the format; switches
  that only paper can honour are annotated under the row when the result is still a PNG. Both still
  land in Descargas by default and still leave through the send intent when shared (#285): the father
  wants the file and Jose still hands it to a chat. `recordInto` records drawing commands in a
  `Picture`, and `PdfDocument` replays them on its canvas, so the PDF is vectorial with **no new
  dependencies**.
- **A4, one plate per page**, header repeated when a plate continues. Neither shrinking a plate to
  fit (121 slots would print at some 8 mm per coin) nor a continuous flow.
- **Only the reverse goes on paper, at life size.** A printed coin measures what the coin measures,
  which turns the sheet into something you lay beside the piece. At 1:1 both faces do not fit — the
  slot paints them side by side, so the width is `2·Ø` and A4 keeps two columns only up to
  Ø ≤ 40.5 mm, a boundary that punishes exactly the ounces. If one face must be chosen it is the
  **reverse**, measured: 121 distinct reverses against 17 obverses in Personalidades destacadas de
  Rusia, 69 against 10 in Libro Rojo; and in bullion no face distinguishes anything, the year does.
  The obverse is not lost — it is a tap away in the app. **A hole occupies its full diameter**, like
  the coin you do own, or the sheet stops being comparable where it matters most.
- **The grid is set by the largest diameter of each plate.** A constant grid across the notebook
  became unnecessary at 1:1, where constancy is guaranteed by construction, and per-plate grids drop
  the 50-plate notebook from 278 pages to **84**. The `scale` of `SheetLayout` dies **on paper**,
  where it made printed size depend on how long a series is.
- **A 50 mm ruler at the foot of every page**, because viewers print «fit to page» by default and
  would break the 1:1 silently.
- **Paper and screen diverge on purpose**: the PNG keeps its `sqrt` grid, both faces and its
  `scale`, because on screen the millimetre does not exist. Two layout engines over the same content,
  each faithful to its medium. ADR 0010 §8 keeps governing the screen sheet and is extended, not
  replaced.

## The data migration (v5)

Specified here, implemented by #164/#167/#169:

- **`DROP TABLE collection_proposal_preferences`**, forward-only, rescuing nothing, as ADR 0008
  itself demanded of any rollback. There is no data to save: the rows do not express a preference,
  only that the app charged a toll. It is **irreversible on purpose** — if a real case for archiving
  ever appears, the bit is rebuilt from zero. No foreign keys; `exportSchema = true` leaves `5.json`
  documenting it, and the `DROP` enters `MigrationSqlTest` with the same treatment the v4 got.
- **`own_groupings` and `own_grouping_members` stay intact** (§11).
- **The plate condition arrives with tests.** A grep of `resolvePlate|PlateUnavailable` in both
  `src/test` trees returns nothing today, and neither `setDisposition` nor `stanceFor` had any: the
  three remaining branches of §7 are pinned with tests.

## Consequences

- **The index becomes one list.** Three blocks, the disposition gesture, the masthead unclassified
  button and the empty «Tus agrupaciones» heading all disappear at once; what replaces them is one
  comparator, persisted filters and a search.
- **A new installation costs 33 fewer taps** and shows the same thing the collector sees today, who
  had already paid them.
- **The curator inherits work the app used to fake**: 52 `short_name` values (#163), the Portuguese
  escudos (#157), and whatever an ugly raw family exposes. That is the intended direction — curating
  fixes both phones, correcting on a phone fixes one.
- **Two reports and one test carry what the app stopped saying**: the weight-disagreement report
  (#158), the year-blind row report (#168) and the metal test.
- **No new field in any curated file except `short_name`**, no new table, no new API call. Every
  facet §1 and §13 need was already measured as free: `weight` in 100 % of the 804 types, `size` in
  100 %, `min_year` in 99.8 %, `issuer` in 100 %.
- **Implementation is ordinary work from here**, in order: #162 (the word), #164 (the comparator —
  `short_name` and the death of the aliases already shipped in v0.12.0), #167 (the two screens of
  pieces and the bottom bar), #173 (the box) and #169 (the notebook, blocked by #167).

## Documents this ADR changes

- **ADR 0008 → `superseded by ADR 0021`**, body intact and a line pointing forward. It is the first
  ADR retired in this repo — the twenty in force all say `accepted` — and it earns retirement rather
  than correction because it has no false sentence: it is entirely about something that stops
  existing. Its value is historical, and it is the only document that explains the ~58 meaningless
  rows in the collector's phone and why `MIGRATION_3_4` repopulated only 30 literal keys.
- **ADR 0010 §2** keeps `ManualOverride` out and loses its false reason (§12). **§3** keeps the
  orphan reasons and demotes them to report data. **§8** stays in force for the screen sheet and is
  extended by §13 for paper.
- **ADR 0013** loses two sentences the map disproved: «the proposal screen is what you own, the plate
  is the catalog with its gaps» (false in the code — the plate is also what you own, §9) and the one
  calling a curated grouping an extra view (§2, §10). Its family ladder survives, plus the sentence
  in §3 about what a grouping cannot join.
- **ADR 0007** is not touched: derivation from the inventory survives whole. ADR 0016, 0018, 0019 and
  0020 are upheld and leaned on throughout.
- **`spec.md §0.3`** drops the dispositions invariant and the six editorial aliases, and restates the
  plate condition. **§0.4** is rewritten: the frozen web stops being the reference UI. **§1** keeps
  the property that mattered with a new subject — auditable **by the curator**, correctable **by
  curating** — because the `Slot` and the heuristic matching it was written about no longer exist.
- **`CONTEXT.md`** loses «Available proposal», «Followed collection proposal», «Ignored proposal» and
  «Family display alias», loses the dispositions clause of the variant key, and gains the vocabulary
  of §2, §3, §4 and §6.

## Alternatives considered

- **A provenance label on the card** («según Numista» / «curada a mano» / «tuya»), or two axes of
  source and confidence. Rejected: it would spend a line on all 58 cards to disambiguate 7, and the
  collector has never asked where a name comes from. The axis that mattered is already versioned in
  `source`, `source_note` and `series_status`.
- **A `source_authority` enum in the curated files.** Rejected: it would encode as a taxonomy what
  today are two prose notes, filled in by eye at every future curation, while prose cites and a
  citation can be verified.
- **Letting the collector rename any collection.** Rejected: the one renameable thing in the app —
  the box — is empty on both phones, so asking for 58 baptisms repeats the mistake §7 just retired,
  and deciding that a scope definition reads as «Plata a valor facial» is curation criterion.
- **Cutting `name` at the first `·`** instead of adding `short_name`. Rejected by measurement: it
  collapses 12 files into 5 names.
- **Renaming the six ugly families in `data/` and going on painting `family`.** Rejected: it requires
  `family` to be unique per card, which kills the concept — family plus weight, finish and metal *is*
  the variant, so two variants share a family on purpose.
- **A new declarative to replace «Seguir»** («lo colecciono», «me interesa»). Rejected: the same
  thing with another label, the same fragile key behind it, and no reason to believe it would be used
  more.
- **Keeping `Ignored` alone**, now that hiding no longer closes a plate. Rejected: zero measured
  cases, and one bit per derived card forces the whole four-column key to stay.
- **A home screen that asks Collections or Coins.** Prototyped and rejected: a tap per launch to
  choose the same thing every time.
- **Merging the plate into the piece list**, or keeping both destinations. Rejected by measurement:
  zero pieces fall outside their plate's slots, so the second destination had nothing to say.
- **An audit surface inside the app** — a reason line on the coin, an «esta no va aquí» gesture with
  prose and transport. Rejected: a new mechanism for a channel that already exists, spending a line
  on 804 fichas for a reader who is not there.
- **A rescue screen for the year-blind row.** Rejected: routine goes to the report (§12).
- **The notebook as an entity with a name, a cover and its own order.** Rejected: the index already
  is one, ordered and with proper names. An appendix of listless cards at the end would introduce a
  second ordering above §6.
- **Drawing every notebook page as a plate.** Rejected: the page would lie about there being
  something to pursue.
- **Shrinking a plate to fit one A4**, or a constant grid across the notebook. Rejected: 8 mm coins,
  and 1:1 makes the constant grid unnecessary while halving the page count.
- **Bounding curated multiple membership with a cap or a home collection.** Rejected: nothing fires
  on its own, so there is no explosion to bound and no primary to elect.
