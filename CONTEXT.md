# Coindex Domain

Coindex organizes a collector's coin holdings while keeping observed ownership separate
from editorial claims about complete collectible sequences.

## Language

**Numista family**:
The exact catalog family under which Numista groups related coin types. A family can span
multiple physical variants and is not necessarily a curated series.
_Avoid_: Series

**Physical variant**:
A distinct form within a Numista family, identified by its normalized weight, finish and
dominant metal. For example, one-ounce bullion, two-ounce bullion, and one-ounce coloured
pieces are different physical variants, and so are the silver ounce and the gold ounce of
the same series.
_Avoid_: Family, type

**Dominant metal**:
What a coin is mostly made of, inferred from Numista's `composition.text` because Numista has
no metal field. Billon counts as silver. A composition with no dominant metal at all — a
bimetallic piece, a clad core — is **other**, which is a claim; a composition nobody recorded,
or one the rules do not recognise, is simply unknown. A collection catalog **declares** its
own, as it declares weight and finish, and that declaration is about the variant the catalog
covers rather than about each of its members (ADR 0016, ADR 0018).
_Avoid_: Alloy, fineness, composition

**Composite finish**:
A physical finish with multiple simultaneous properties, currently Proof coloured. It is
distinct from Proof and Coloured and participates in card identity and grouping.
_Avoid_: Display label, either component finish

**Variant key**:
The exact canonical tuple of resolved family, normalized weight, finish and dominant metal
that identifies a physical variant, and therefore groups the pieces of a derived collection. A
matching catalog declares the resolved family and complete key; without one, the remaining
precedence ladder resolves the family. It **persists nothing**: since ADR 0021 nothing is stored
per card, and identity is the curated file wherever there is one, so the key is the identity only
of the cards no file names.
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
A collection catalog whose boundary is a **theme the collector declared**, not a denomination, a
programme or a mint's range — so it may cross issuers, centuries and physical patterns, and its
declared weight is the **anchor coin's** rather than a standard every member shares. It is an
ordinary schema 1 catalog and needs no mechanism of its own: ADR 0020 already refused «one issuer
only» and «one physical standard only» as gatekeepers, and ADR 0016 makes the file authoritative
over its members' variant, so the members land on one card however far apart they weigh. What it
owes in exchange is prose: the `source_note` says whose declaration draws the line and quotes it
with a date, and each deviating member carries a `variant_note`. `historia-del-real` is the first,
with four slots over three issuers.
_Avoid_: Agrupación, commemorative programme, set catalog, own grouping

**Technical family**:
Numista's `System YYYY[-YYYY]` value, a monetary system rather than a collectible grouping.
It is the weakest family: any curated catalog naming the type outranks it, but it still
groups pieces no catalog claims, so a piece is never dropped for having one.
_Avoid_: Numista family, unclassified reason

**Orphan**:
A coin for which the curator has affirmed that a collection-catalog plate would not make
sense — not merely one that currently lacks a catalog. The verdict is manual: after
investigating the automatic unclassified residue, the curator records the Numista type and a
prose reason in `data/orphans.json`. Absolute solitude is enough but not required; a real
sequence Coindex will never plate (for example ordinary euro circulation by country) can be
an orphan too. Calendar solitude — a programme that may still grow, such as a lone Gothic
Horror character — is not an orphan. The rows `deriveCollection` could not place are unclassified
residue, not the orphan list, and the collector reaches them through the «Sin colección» filter of
Coins — which shows *which* pieces are out, never *why* (ADR 0021 §12).
_Avoid_: Unclassified, missing, stable orphan

**Collection**:
Any card of the index, and there is only one species: a curated catalog, a curated grouping and a
collector's own box sit in one list, sorted by one comparator, with no block, no section and no word
of provenance telling them apart. Having a curated file is not a rank and none is subordinate to
another. What a collection *does* depends on one capability only — whether it has an issue list.
_Avoid_: Collection proposal, album, automatic series, own grouping as a separate species

**Derived collection**:
A collection nobody curated, fabricated from the collector's current pieces by one variant key
because no file names those types. It is ephemeral and per collector (ADR 0007), recomputed after
each sync, and it prints Numista's raw family verbatim as its card name.
_Avoid_: Collection proposal, followed proposal, unclassified

**Issue list**:
The property that splits what a collection can do: with one — a catalog's members — the card says
progress (`4 de 12 · te faltan 8`), opens its plate in one tap and can show a hole. Without one, the
card counts what there is (`3 monedas · 2 tipos`) and opens its list of pieces. It is the only
provenance signal on screen, and it is never spelled out as a word.
_Avoid_: Provenance label, curated flag, coverage claim

**Card name**:
The card-sized name of a collection: `short_name` in the curated file — required, unique across the
index and a prefix of `name` — Numista's raw family verbatim where no file exists, and the single
40-character name a collector types when creating a box (where `name == short_name`). It is written
by the curator, never renamed by the collector, and there are no display aliases in code.
_Avoid_: Family, name, family display alias, editorial scope

**Card country**:
The country a card says above its name, and the chip Coins filters by: `issuer_code` from the curated
file wherever one names the collection, and the pieces' own issuer where none does — silent when they
disagree, because an eyebrow covering half a card is worse than no eyebrow. It is a **country and not
an issuing entity**: Numista names entities with their period of validity, so nine of the 40 issuer
codes in the cache are cured to the name the curator writes in Spanish, `russie` → «Rusia»
(ADR 0023). The remaining 31 are the ficha's, verbatim.
_Avoid_: Numista's issuer label as the country, issuing entity, period of validity on a card

**Coverage ratio**:
Issued members owned over issued members catalogued, which is what a collection with an issue list
prints and what the index is sorted by — `(has ratio ↓, ratio ↓, denominator ↓, short_name ↑)`. It
is a measured fact and it replaced the collector's declaration of intent: nothing is stored per card
any more.
_Avoid_: Followed disposition, progress bar, completeness claim

**Collector's own box**:
A collection whose members the collector enumerated by hand on the phone (`own_groupings`), born
from a filter in Coins that seeds the selection. It is indistinguishable from any other card in the
index, but it is a different act: it only ever holds pieces you own, so it **can never contain a
gap**, and its only product is a sheet. A collection pursues; a box shows.
_Avoid_: Own grouping, curated grouping, unclassified bucket, subordinate collection

**Coins**:
The sibling hierarchy of Collections at the top level, reached through the bottom bar, where a piece
exists whether or not any collection claims it. It carries the filters — «Sin colección», class,
country — the sort and the search, and each coin links back to the collections that claim it.
_Avoid_: Unclassified screen, inventory view, collection detail

**Disagreement report**:
The audit surface for what the matching contradicts in silence — a member whose weight normalized
from Numista's grams is not the one its catalog declares, a row so year-blind it is invisible in its
own plate. It is a script with no network that never goes red and keeps a single issue in sync, not a
screen: red when the finding is rare, a report when it is routine (ADR 0021 §12). The weight half is
`scripts/weight-deviations.py`; the year-blind rows need the inventory and belong to the field report.
_Avoid_: Manual override, unclassified reason, in-app audit

**Curation**:
All the curated files that travel with the app taken together — collection catalogs, curated
groupings and commemorative programmes — bound once and treated as one thing. It is what the
snapshot is read **against**: the card names, the index order and the plates all come from it, and
the assembly that turns a snapshot into what the screens show is the single entry to the domain
(#217). Constant for the lifetime of the process, because the files ship inside the APK.
_Avoid_: Seeds, catalogs, curated data

**Snapshot**:
What one phone holds of the collector right now, and the only input the curation is applied to: the
inventory as it was last synced, the fichas cached for it, and the boxes the collector typed.
Everything else is derived from it and nothing about it is stored per card (ADR 0021 §7).
_Avoid_: State, local data, cache

**Collection catalog**:
A curated, sourced reference list of official members for one exact variant key.
It remains separate from curated series, and it is what gives a collection its issue list.
It **declares** that variant rather than inferring it: for the types it claims, its own family,
weight, finish and metal are the key, whatever family or grams Numista records (ADR 0016).
_Avoid_: Collection proposal, curated series

**Open series**:
A collection catalog's declaration that its series is still being issued, so the boundary it
claims is «N of N catalogued» today and **no promise about any date**. Closing is the claim that
costs proof, so a closed catalog must say in `closed_note` what sustains the closure and an open
one is forbidden from carrying that note — a curator who cannot find the closure declares open.
Every catalog declares it and no curated grouping does, because coverage is what the field is
about. An open catalog going behind its mint is therefore not a defect in the file: the **tail**
(missing current year) is reported in an issue, never stored as freshness. Interior years the mint
skipped are a different claim — `no_issue_years` with `no_issue_note` — so the report stops
listing them once the curator has versioned the proof (ADR 0020).
_Avoid_: Up to date, incomplete catalog, curated series

**Announced member**:
A collection-catalog member the issuer has named but not yet struck, so no piece can ever fill it
and it stays outside the plate denominator. It cites the issuer instead of Numista — no
`numista_type_id`, a required `source` and prose saying what that source proves — and its year is
optional, because writing in a date the mint has not announced would claim more than the source
says. Its optional `design_type_id` points at the same design in another physical variant and
never takes part in matching or evidence. An announcement with no identity at all is worth nothing
without a programme count: «more will come» is what an open series already says.
_Avoid_: Missing, unlisted member, not-yet-issued slot

**Issue-qualified member**:
A collection-catalog member whose `numista_issue_ids` narrow a Numista type to the exact
physical issues the catalog claims. The qualifier is optional in a simple catalog and
exhaustive in an issue run; an unlisted issue belongs to neither by fallback nor precedence.
_Avoid_: Issue run, date run, type-wide member

**Unlisted member**:
A collection-catalog member for a coin that was struck and sold but has no published Numista
type. Its slot is not measurable from the Numista-backed inventory, so an Album marks it neither
Owned nor Missing and leaves it outside the plate denominator. An unpublished type awaiting a
referee can be why the member remains unlisted, but its unstable id is never written into the
curated catalog.
_Avoid_: Missing, announced member, unpublished type

**Unpublished type**:
A Numista type whose page is not publicly visible yet, because a referee has still to publish,
edit or delete the submission. The API serves it anyway, with every field as the contributor
left it, so a half-typed family arrives as a real family. It is **not verifiable**, and verified
is what a curated file requires: an unpublished type never enters a collection catalog or a
curated grouping, however certain the coin is, because the referee may delete the page and take
the id with it. Since #186 the same bar applies to the collection the app derives on its own: a
type that looks unpublished and declares a family derives no card, and its pieces wait in the
unclassified residue until the page is published. Nothing corrects the fields in the app — the
editor's fix upstream is the fix, and it reaches a phone only when somebody there asks for the ficha
again, which since #185 is a gesture on the coin itself (ADR 0023) and never something the app does
on its own. Its offline trace is a type with no year at all.
_Avoid_: Numista error, manual override, missing type metadata

**Ficha**:
The Numista type as this phone holds it: the fields a card prints, the untouched body they were read
from, and the day it was **brought** — which is what the card says, «ficha traída hace ocho meses»,
because for a ficha that arrived in the APK's snapshot the content may be older than the day it
landed. No sync and no seed ever asks for a ficha twice; the collector can, one type at a time, on
the coin where the wrong datum is on screen, and that gesture is the only thing that overwrites one
(ADR 0023). It costs one call, it says so before spending it, and failing it leaves the ficha that
was already there — including the 404 that means a referee deleted the page.
_Avoid_: Type metadata, permanent cache, refrescar la colección, ficha fresca

**Collection catalog plate**:
The per-collector comparison between a collection that exists today and its matching collection
catalog, showing owned and Missing members. It opens with no gesture from the collector, on two
conditions and nothing else: the collection exists — the collector owns pieces of that variant — and
there is **evidence by type**, at least one official member of the catalog among them. Evidence by
type rather than by issue is what keeps a plate open while years are missing.
_Avoid_: Album, followed proposal, disposition

**Printed side**:
The face of its coins a catalog declares for the page that prints one — `printed_side`, «la cara que
es la moneda»: the one the collector recognises as this piece, Britannia, the Amur tiger, the mermaid
of the 50 gourdes. It is **not** the face that tells the members apart; that is the caption's job, and
the criterion would print monarchs' portraits over a run of identical Britannias. It is declared by
the curator and never inferred from the ficha's descriptions, it belongs to the whole plate rather
than to a member, and its absence means the reverse — which is what «Numista's reverse» had silently
been until #227.
_Avoid_: Numista's reverse, the distinguishing face, obverse override

**Commemorative programme**:
A curated statement that some Numista types were struck for the same commemoration, and nothing
more (ADR 0022). It is a **second reading** of a coin that already belongs to a collection, not a
collection itself: it declares no variant, never reaches `deriveCollection` and produces no card,
which is exactly what lets it coexist with the card the coin already has instead of replacing it —
a set catalog would have won the family precedence and moved the coin. Its members are types and
they are **not** bounded by what the catalogs hold: the 25 escudos of the 1977 and 1983 Portuguese
programmes sit in no catalog, so its denominator counts three where a join across catalogs would
have printed two. Its boundary is never a Numista fact, so it cites any host and its prose note is
required. It reads today on the plate, beside the plate's own progress and never mixed into it.
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
