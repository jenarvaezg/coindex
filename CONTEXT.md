# Coindex Domain

Coindex organizes a collector's coin holdings while keeping observed ownership separate
from editorial claims about complete collectible sequences.

## Language

**Numista family**:
The exact catalog family under which Numista groups related coin types. A family can span
multiple physical variants and is not necessarily a curated series.
_Avoid_: Series

**Family display alias**:
A presentation-only label for a Numista family. It does not change proposal identity,
grouping, or persisted dispositions.
_Avoid_: Numista family, proposal variant key

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
distinct from Proof and Coloured and participates in proposal identity and grouping.
_Avoid_: Display label, either component finish

**Proposal variant key**:
The exact canonical tuple of resolved family, normalized weight, finish and dominant metal
that identifies a physical variant for proposal grouping and per-user dispositions. A matching
catalog declares the resolved family and complete key; without one, the remaining precedence
ladder resolves the family. Family display aliases never alter the key.
_Avoid_: Family display alias, proposal title

**Absent weight**:
The weight of a proposal variant key that identifies a set rather than a physical variant,
because the set spans several of them. It persists as `-1`, never as zero, so that a
defaulted row stays an invalid weight instead of reading as a set.
_Avoid_: Zero weight, unknown weight

**Set catalog**:
A collection catalog whose members were issued together as one product, so the set is the
collectible unit and no single physical variant identifies it. It declares no weight and no
finish, and it claims its member types ahead of the family Numista gives them. Fractional
bullion is not one: a quarter-ounce and a one-ounce piece are the same coin in two sizes.
_Avoid_: Date run, fractional bullion family, curated series

**Technical family**:
Numista's `System YYYY[-YYYY]` value, a monetary system rather than a collectible grouping.
It is the weakest family: any curated catalog naming the type outranks it, but it still
groups pieces no catalog claims, so a piece is never dropped for having one.
_Avoid_: Numista family, unclassified reason

**Orphan**:
A coin for which the curator has affirmed that a collection-catalog plate would not make
sense — not merely one that currently lacks a catalog. The verdict is manual: after
investigating the automatic unclassified residue, the curator records the Numista type and a
prose reason in a curated repo asset. Absolute solitude is enough but not required; a real
sequence Coindex will never plate (for example ordinary euro circulation by country) can be
an orphan too. Calendar solitude — a programme that may still grow, such as a lone Gothic
Horror character — is not an orphan. The screen that lists rows `deriveCollection` could not
place is unclassified residue, not the orphan list.
_Avoid_: Unclassified, missing, stable orphan

**Collection proposal**:
A provisional, per-collector grouping of currently owned pieces that share one exact
resolved family and physical variant. It suggests an organization, not catalog coverage or
absent pieces.
_Avoid_: Album, automatic series

**Available proposal**:
A current collection proposal for which the user has neither followed nor ignored the
proposal variant key.
_Avoid_: Followed proposal, ignored proposal

**Followed collection proposal**:
A current collection proposal whose proposal variant key the user has marked as followed.
Following does not promote it to a curated series or Album and cannot create Missing
members.
_Avoid_: Curated series, Album

**Collection catalog**:
A curated, sourced reference list of official members for one exact proposal variant key.
It remains separate from curated series and from the collector's followed disposition.
It **declares** that variant rather than inferring it: for the types it claims, its own family,
weight, finish and metal are the key, whatever family or grams Numista records (ADR 0016).
_Avoid_: Collection proposal, curated series

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
left it, so a half-typed family arrives as a real family and becomes a card of its own. It is
**not verifiable**, and verified is what a curated file requires: an unpublished type never
enters a collection catalog or a curated grouping, however certain the coin is, because the
referee may delete the page and take the id with it. Nothing corrects it in the app — the
editor's fix upstream is the fix. Its offline trace is a type with no year at all.
_Avoid_: Numista error, manual override, missing type metadata

**Collection catalog plate**:
The per-collector comparison between a followed collection proposal and its matching
collection catalog. It can show owned and Missing catalog members without changing the
proposal or promoting it to an Album.
_Avoid_: Album, followed collection proposal

**Ignored proposal**:
A current collection proposal whose proposal variant key the user has reversibly marked
as ignored.
_Avoid_: Deleted proposal, permanent exclusion

**Curated series**:
An intentionally defined collectible sequence whose scope and expected members are
editorial claims. Unlike a collection proposal, it can establish catalog coverage.
_Avoid_: Numista family, collection proposal

**Catalog coverage**:
The set of expected members declared by a curated series, including the boundary within
which owned and absent pieces can be assessed.
_Avoid_: Catalog metadata

**Missing**:
The status of an issued member within catalog coverage for which the collector owns no
matching piece. It is meaningful only for curated series, never for collection proposals.
_Avoid_: Unobserved, unknown

**Album**:
A collector-specific view of curated series coverage, distinguishing owned, Missing, and
not-yet-issued members. A collection proposal is not an Album.
_Avoid_: Collection proposal, inventory
