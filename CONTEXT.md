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
A distinct form within a Numista family, identified by its normalized weight and finish.
For example, one-ounce bullion, two-ounce bullion, and one-ounce coloured pieces are
different physical variants.
_Avoid_: Family, type

**Composite finish**:
A physical finish with multiple simultaneous properties, currently Proof coloured. It is
distinct from Proof and Coloured and participates in proposal identity and grouping.
_Avoid_: Display label, either component finish

**Proposal variant key**:
The exact canonical tuple of raw Numista family, normalized weight, and finish that
identifies a physical variant for proposal grouping and per-user dispositions. It uses
the same weight normalization as proposal derivation; family display aliases never alter
it.
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
_Avoid_: Numista family, orphan, unclassified reason

**Collection proposal**:
A provisional, per-collector grouping of currently owned pieces that share one exact
Numista family and physical variant. It suggests an organization, not catalog coverage
or absent pieces.
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
_Avoid_: Collection proposal, curated series

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
