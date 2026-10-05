# Coindex Domain

Coindex organizes a collector's coin holdings while keeping observed ownership separate
from editorial claims about complete collectible sequences.

## Language

**Numista family**:
The exact catalog family under which Numista groups related coin types. A family can span
multiple physical variants and is not necessarily a curated series.
_Avoid_: Series

**Placeholder family**:
A Numista family made only of function words, such as the «The» of N#596807. It is treated as no
family at all (#404): the piece joins the unclassified residue. An initialism counts as a name, and
a curated file or grouping overrides it. Unlike an **unpublished type**, its page is live. The fix
is an edit on Numista, which reaches the phone when the ficha is asked again (ADR 0025) or
re-seeded (ADR 0033).
_Avoid_: Unpublished type, family display alias, Numista error

**Physical variant**:
A distinct form within a Numista family, identified by its normalized weight, finish and
dominant metal. For example, one-ounce bullion, two-ounce bullion, and one-ounce coloured
pieces are different physical variants, and so are the silver ounce and the gold ounce of
the same series.
_Avoid_: Family, type

**Dominant metal**:
What a coin is mostly made of, read from the head of Numista's `composition.text` (Numista has no
metal field). Billon counts as silver. **Other** is a claim, a composition with no dominant metal
(bimetallic, clad); unknown means unrecorded or unrecognised. A collection catalog declares its own
for the variant it covers, not per member (ADR 0016, ADR 0018). A coating after the head is a
finish, not the metal: «Plata 925 (with selective gold plating)» is gilded silver.
_Avoid_: Alloy, fineness, composition

**Composite finish**:
A physical finish with multiple simultaneous properties, currently Proof coloured. It is
distinct from Proof and Coloured and participates in card identity and grouping.
_Avoid_: Display label, either component finish

**Variant key**:
Resolved family, normalized weight, finish and dominant metal: the tuple that identifies a physical
variant and groups the pieces of a derived collection. A matching catalog supplies the whole key;
otherwise the precedence ladder resolves the family. Nothing is stored against it, and where a
curated file names the types, the file is the identity (ADR 0021).
_Avoid_: Proposal variant key, family display alias, card name

**Absent weight**:
The weight of a variant key that identifies a set rather than a physical variant,
because the set spans several of them. It persists as `-1`, never as zero, so that a
defaulted row stays an invalid weight instead of reading as a set.
_Avoid_: Zero weight, unknown weight

**Set catalog**:
A collection catalog whose members were issued together as one product, so the set is the
collectible unit and no single physical variant identifies it. It declares no weight and no
finish, and it claims its member types ahead of the family Numista gives them. Fractional
bullion is not one: a quarter-ounce and a one-ounce piece are the same coin in two sizes.
_Avoid_: Date run, fractional bullion family, curated series

**Thematic catalog**:
A schema 1 collection catalog bounded by a theme the collector declared, not by a denomination,
programme or mint range, so it may cross issuers, centuries and physical patterns. Its declared
weight is the anchor coin's, and ADR 0016 keeps every member on its card. It owes prose: a dated
`source_note` quoting whose declaration draws the line, and a `variant_note` on each deviating
member. Example: `historia-del-real`.
_Avoid_: Agrupación, commemorative programme, set catalog, own grouping

**Technical family**:
Numista's `System YYYY[-YYYY]` value, a monetary system rather than a collectible grouping.
It is the weakest family: any curated catalog naming the type outranks it, but it still
groups pieces no catalog claims, so a piece is never dropped for having one.
_Avoid_: Numista family, unclassified reason

**Orphan**:
A coin the curator has recorded in `data/orphans.json`, with its Numista type and a prose reason, as
one a collection-catalog plate would not make sense for: a verdict, not the lack of a catalog. A
sequence Coindex will never plate (euro circulation by country) or a coin with a raw-family card can
be one; a programme that may still grow (a lone Gothic Horror character) cannot. Distinct from the
**unclassified residue**, the rows `deriveCollection` could not place, which the «Sin colección»
filter of Coins shows without a reason (ADR 0021 §12).
_Avoid_: Unclassified, missing, stable orphan

**Collection**:
Any card of the index: a curated catalog, a curated grouping or a collector's own box, in one list
under one comparator, with no word of provenance and no rank between them. What a collection can do
depends only on whether it has an **issue list**.
_Avoid_: Collection proposal, album, automatic series, own grouping as a separate species

**Derived collection**:
A collection nobody curated, built from the collector's current pieces by one variant key because
no file names those types. Ephemeral and per collector (ADR 0007), recomputed after each sync; its
card name is Numista's raw family, cured where the label table has an entry (ADR 0031).
_Avoid_: Collection proposal, followed proposal, unclassified

**Issue list**:
What splits what a collection can do. With one (a catalog's members) the card shows progress
(`4 de 12 · te faltan 8`), opens its plate and can show a hole; without one it counts what there is
(`3 monedas · 2 tipos`) and opens its list of pieces. It is the only provenance signal on screen,
and is never spelled out as a word.
_Avoid_: Provenance label, curated flag, coverage claim

**Card name**:
A collection's card-sized name: the curated file's `short_name` (required, unique in the index, a
prefix of `name`); without a file, Numista's raw family, corrected only by the cured-label table
(ADR 0031) and the technical-family format; for a box, the single 40-character name the collector
types. The collector never renames a card. Where two cards would read alike, each appends the weight
that splits them, «5 francs Semeuse · 0,386 oz» (#565); nothing else is ever appended.
_Avoid_: Family, name, family display alias, editorial scope

**Card country**:
The country of a card, used by the shelf facet, the plate's ficha and paper, and no longer printed
above the name (ADR 0026 §12): the curated file's `issuer_code`, else the pieces' own issuer, blank
when they disagree. A country, not Numista's issuing entity with its period of validity: some codes
are cured to the curator's Spanish name, `russie` → «Rusia» (ADR 0023).
_Avoid_: Numista's issuer label as the country, issuing entity, period of validity on a card

**Coverage ratio**:
Issued members owned over issued members catalogued: what a collection with an issue list prints
and what the index sorts by, `(has ratio ↓, ratio ↓, denominator ↓, short_name ↑)`. It is measured,
and it replaced the collector's declaration of intent: nothing is stored per card.
_Avoid_: Followed disposition, progress bar, completeness claim

**Collector's own box**:
A collection whose members the collector picked by hand (`own_groupings`), seeded from a filter in
Coins. It only holds pieces the collector owns, so it **can never contain a gap**, and its only
product is a sheet. «Box» is a word of the code and this document, never of the screen: the gesture
says «Hacer una colección» and the box is headed «Tu colección» (#516, ADR 0021 §2).
_Avoid_: Own grouping, curated grouping, unclassified bucket, subordinate collection, «caja» or
«agrupación» on screen

**Coins**:
The sibling of Collections at the top level, reached through the bottom bar, where a piece exists
whether or not any collection claims it. It carries the filters («Sin colección», class, country),
the sort and the search, and each coin links back to the collections that claim it.
_Avoid_: Unclassified screen, inventory view, collection detail

**Disagreement report**:
The out-of-app audit of what the matching contradicts silently: a member whose normalized Numista
weight is not its catalog's, or a row so year-blind it is invisible in its own plate. A script with
no network that never goes red and keeps one issue in sync (ADR 0021 §12): the weights are
`scripts/weight-deviations.py`; the year-blind rows need the inventory and belong to the field
report.
_Avoid_: Manual override, unclassified reason, in-app audit

**Matching digest**:
`fixtures/matching-digest.json`: the snapping constants and the vectors that pin them, emitted by
the Kotlin suite and asserted by the Python suite so that the Python ports of the matching follow
the domain. It travels one way: the app is the source, a disagreement is the port's to fix, and
production never reads it.
_Avoid_: Shared matching rules, matching config, mirror config

**Curation**:
All the curated files shipped with the app — collection catalogs, curated groupings and
commemorative programmes — loaded once as one value that the snapshot is read **against**: card
names, index order and plates come from it, through a single entry to the domain (#217). It is
constant for the process and **valid by construction** (#545): a rule spanning two species is
checked where both are held. Files arrive through one **loading door**, read from the APK's assets
on a phone and from `data/` in the suite.
_Avoid_: Seeds, catalogs, curated data

**Snapshot**:
What one phone holds of the collector right now, and the only input the curation is applied to: the
inventory as last synced, the fichas cached for it, and the boxes the collector made. Everything
else is derived from it, and nothing is stored per card (ADR 0021 §7).
_Avoid_: State, local data, cache

**Reading**:
What a screen shows that nobody stores: the snapshot crossed with the curation, plus prices and
marks. It is a **value**, never a field of the state, so it cannot become a third truth disagreeing
with the table and the inventory. It is one memoized object a screen keys its `remember` on (#542),
in two halves that move at different speeds, so what only the snapshot and the curation decide
survives a price landing mid-pass.
_Avoid_: Derived state, ui state, cache

**Collection catalog**:
A curated, sourced reference list of official members for one exact variant key. It is distinct
from a curated series, and it is what gives a collection its issue list. It **declares** that
variant rather than inferring it: for the types it claims, its own family, weight, finish and metal
are the key, whatever Numista records (ADR 0016).
_Avoid_: Collection proposal, curated series

**Open series**:
A catalog's declaration that its series is still being issued: «N of N catalogued» today, with **no
promise about any date**. Closing needs proof in `closed_note`, which an open catalog may not carry;
a curator without proof declares open. Every catalog declares it and no grouping does. A missing
current year (the **tail**) is reported in an issue, never stored; interior years the mint skipped
are declared in `no_issue_years` with `no_issue_note` (ADR 0020).
_Avoid_: Up to date, incomplete catalog, curated series

**Announced member**:
A member the issuer has named but not yet struck: no piece can fill it and it stays outside the
plate denominator. It cites the issuer (a required `source`, with prose on what it proves) instead
of a `numista_type_id`, and its year is optional. An optional `design_type_id` points at the same
design in another physical variant and never takes part in matching or evidence. An announcement
with no identity adds nothing an open series does not already say.
_Avoid_: Missing, unlisted member, not-yet-issued slot

**Issue-qualified member**:
A collection-catalog member whose `numista_issue_ids` narrow a Numista type to the exact
physical issues the catalog claims. The qualifier is optional in a simple catalog and
exhaustive in an issue run; an unlisted issue belongs to neither by fallback nor precedence.
_Avoid_: Issue run, date run, type-wide member

**Emission label**:
The name an issue run gives an owned row its year cannot tell apart: «Estrella 67» on a 100 pesetas
of Franco whose row says 1966. It belongs to the **piece**, not the card, so it is resolved once in
the assembly and travels with the piece wherever it is drawn. Elsewhere the line starts with the
year (#225).
_Avoid_: Member label, variety, year

**Unlisted member**:
A collection-catalog member for a coin that was struck and sold but has no published Numista
type. Its slot is not measurable from the Numista-backed inventory, so an Album marks it neither
Owned nor Missing and leaves it outside the plate denominator. An unpublished type awaiting a
referee can be why the member remains unlisted, but its unstable id is never written into the
curated catalog.
_Avoid_: Missing, announced member, unpublished type

**Unpublished type**:
A Numista type whose page a referee has not yet published, edited or deleted. The API serves it with
the contributor's half-typed fields, so its family looks real. Being unverifiable, it never enters a
catalog or grouping, and since #186 a type that looks unpublished derives no card: its pieces wait
in the unclassified residue. The fix is an edit on Numista, which reaches the phone when the ficha
is asked again (ADR 0025) or re-seeded (ADR 0033). Offline trace: a type with no year at all.
_Avoid_: Numista error, manual override, missing type metadata

**Ficha**:
The Numista type as this phone holds it: the printed fields, the untouched body, and the day it was
**brought**, shown as «ficha traída hace ocho meses» because a snapshot ficha can be older than its
arrival. No sync asks for a ficha twice. It is overwritten when the collector asks for it again from
the coin (ADR 0025: one **consulta**, announced first, and a failure, 404 included, keeps the old
ficha) or by a newly installed version's snapshot, once (ADR 0033).
_Avoid_: Type metadata, permanent cache, refrescar la colección, ficha fresca

**Consulta**:
The unit a Numista key's monthly budget is spent and shown in, and the interface's only word for it
(#516): the sync report, the line under the sync button, a ficha refresh, a tasación, the exhausted
month and the 429 all count in it. «Llamada» is the code's word. The marking mode promises «+2
consultas al mes» per casilla (ADR 0029 §5).
_Avoid_: Llamada, petición, API call on screen, request

**Collection catalog plate**:
The per-collector comparison between a collection that exists today and its matching collection
catalog, showing owned and Missing members. It opens with no gesture when the collector owns pieces
of that variant and at least one is an official member of the catalog (**evidence by type**, not by
issue, so a plate stays open while years are missing). The **Shelf window** opens plates without
evidence.
_Avoid_: Album, followed proposal, disposition

**Printed side**:
The face a catalog declares for printing its coins (`printed_side`, «la cara que es la moneda»): the
one the collector recognises as the piece, such as Britannia or the Amur tiger. It is not the face
that tells members apart; that is the caption's job. Declared for the whole plate by the curator,
never inferred from the ficha; absent, it means the reverse (#227).
_Avoid_: Numista's reverse, the distinguishing face, obverse override

**Commemorative programme**:
A curated statement that some Numista types were struck for the same commemoration (ADR 0022): a
**second reading** of coins that already belong to collections. It declares no variant, never
reaches `deriveCollection` and makes no card, so it coexists with the coin's card. Its members are
types, not bounded by what the catalogs hold; its boundary is never a Numista fact, so it may cite
any host and requires a prose note; and it is read on the plate, beside the plate's own progress.
The Series Iberoamericanas are one slot per country, each the circulated finish or a medal, with the
coordinating mint as `issuer_code`. A programme may ship invisible when no catalog names its types
and neither collection owns one; the field report prints programmes for coins whose card has no
plate.
_Avoid_: Subseries, set catalog, curated grouping, thematic collection

**Curated series**:
An intentionally defined collectible sequence whose scope and expected members are
editorial claims. Unlike a derived collection, it can establish catalog coverage.
_Avoid_: Numista family, collection proposal

**Catalog coverage**:
The set of expected members declared by a curated series, including the boundary within
which owned and absent pieces can be assessed.
_Avoid_: Catalog metadata

**Missing**:
The status of an issued member within catalog coverage for which the collector owns no
matching piece. It is meaningful only where there is an issue list, never in a derived
collection or a collector's box.
_Avoid_: Unobserved, unknown

**Album**:
A collector-specific view of curated series coverage, distinguishing owned, Missing, and
not-yet-issued members. A collection with no issue list is not an Album.
_Avoid_: Collection proposal, inventory

**Die-cut hole**:
The unit of the album sheet and, since ADR 0026, the shape of both a collection in the index and a
member on a plate: shaded cardboard around a round window with the coin's photograph, over which
nothing is drawn but the acetate's reflection (#357). The year beside it is a **Sunken tag**. A hole
with no coin shows a **Ghost**; a coin with no cardboard behind it is one no collection claims. It is
a drawing, not a domain object.
_Avoid_: Cell, tile, card, slot (which is the member, not its drawing)

**Ghost**:
The design of a member the collector does not own, drawn at 14 % with a dotted rule inside its hole,
so that progress is **seen** rather than read. It replaced the greyscale-and-opacity Missing member.
Distinct from **bare cardboard**, the third state of the year axis: a year with no slot at all.
_Avoid_: Greyed member, empty slot, placeholder

**Completion stamp**:
The rubber stamp a plate shows over the ratio in its header while every issued member is owned. It
is a **state, not an event**: read from the inventory, stamped when the sheet opens and never on
sync, never shown in the index, and it says «completa», even for an open series. Being a state, it
travels to the exported PNG.
_Avoid_: Badge, achievement, medal, completion date

**Furniture**:
Any visible string that is not the datum: labels, tails, explanations and section titles. It is what
the density bar of ADR 0026 §5 counts (Collections ≤ 25 words in the first fold) and what the
**frequency rule** prices: a word costs its length times the number of times it is printed, so no
furniture string is printed per row, per slot or per card.
_Avoid_: Copy (which is every string, furniture or not), chrome, boilerplate

**Grain of a cell**:
What a top-level destination is made of, and the test for whether it deserves a cell: cards for
Collections, types for Coins, grams for «Las cifras». A cell shows its grain as its count **and names
it** («Tipos · 15», #516). If what is inside is what is outside in another order, the grain is
borrowed and it is an **Axis of the shelf**, not a destination.
_Avoid_: Tab, section, count of what is inside, a cell that names one grain and counts another

**Axis of the shelf**:
The order a hierarchy with a list is read in — by plate (the default), by country, by year — chosen
in the folded shelf beside the filters and the sort. The country stain and the timeline are axes,
not screens, because they are made of the same slots.
_Avoid_: View, map screen, timeline screen

**Annex**:
A screen whose population is not the collection, so it is neither a cell nor an **Axis of the
shelf**: «Explorar», made of what the collector does not have. It hangs off one hierarchy, entered
through a last-row door that names its count and is not printed at zero, and has no cell and no
bar. It holds **two rooms** (ADR 0030 §8): the **Shelf window** and, one door further in, «Lo que
busco».
_Avoid_: Tab, sub-screen, modal, section of the index

**Shelf window**:
The curated catalogs the collector owns nothing of that have **fewer than twenty measurable
casillas**, browsable as plates at no API cost because their types are seeded (ADR 0030 §1). A
window plate is **open but not theirs**: no «Exportar», and one figure of money, the **Cost of
entering**, with a valuing gesture where the export was. The shelf also lists the collector's own
plates with a marked casilla, as another order over plates, not another species of collection.
_Avoid_: Escaparate as a word on screen, catalog browser, wishlist, a fourth cell

**Cost of entering**:
What buying every casilla of a **Shelf window** plate would cost, in `unc`, with the date it was
brought: a hand-asked price never expires, because no pass refreshes it (ADR 0030 §4). It is not
«Coste de cerrar», which is buying the last of something you collect. A plate nobody has valued
shows nothing, because a silver floor shown alone reads as the price, and a total carries the
**oldest** date of its parts.
_Avoid_: Coste de cerrar on a plate that is not yours, «desde N €», a floor with no date

**Wish**:
An empty slot the collector marked on this phone, stored in its own table and keyed by the slot's
catalogue facts (type, year, and Numista issue where the file names one), never by anything Numista
hands back (ADR 0029). It is **not a piece**, so nothing that counts pieces, grams, euros or coverage
sees it. It lives while its slot is empty and dies silently when a sync brings the coin. Marks are
per slot, never per plate, and are read in the **Annex**, «Lo que busco», each priced whatever its
plate's shape.
_Avoid_: Wishlist as a collection, followed plate, «lo colecciono», favourite
