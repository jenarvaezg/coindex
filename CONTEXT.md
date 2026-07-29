# Coindex Domain

Coindex organizes a collector's coin holdings while keeping observed ownership separate
from editorial claims about complete collectible sequences.

## Language

**Numista family**:
The exact catalog family under which Numista groups related coin types. A family can span
multiple physical variants and is not necessarily a curated series.
_Avoid_: Series

**Physical variant**:
A distinct form within a Numista family, identified by its normalized weight and finish.
For example, one-ounce bullion, two-ounce bullion, and one-ounce coloured pieces are
different physical variants.
_Avoid_: Family, type

**Collection proposal**:
A provisional, per-collector grouping of currently owned pieces that share one exact
Numista family and physical variant. It suggests an organization, not catalog coverage
or absent pieces.
_Avoid_: Album, automatic series

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
