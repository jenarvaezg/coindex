# ADR 0008: Durable per-user proposal dispositions

- Status: accepted
- Date: 2026-07-29

## Context

Collection proposal content is derived from current holdings and remains ephemeral under
ADR 0007. A collector may nevertheless want a choice to follow or ignore a proposal to
survive later syncs without turning that proposal into editorial catalog coverage.

Family display aliases can improve presentation, but they are not stable identities.
Likewise, a preference whose matching holdings have disappeared must not create a
proposal from stale state.

## Decision

Persist only the per-user followed or ignored disposition for an exact proposal variant
key: the canonical tuple of raw Numista family, normalized weight, and finish. Disposition
lookup uses the same weight normalization as proposal derivation. Family display aliases
are presentation-only and never participate in this key.

Each currently derived proposal is in exactly one mutually exclusive state:

- Available when no disposition is stored for its user and key.
- Followed when the followed disposition is stored.
- Ignored when the ignored disposition is stored.

Ignoring is reversible. Following by itself is not promotion to a curated series or Album,
does not establish catalog coverage, and never creates `Missing` members.

A separately maintained collection catalog may provide sourced catalog coverage for one
exact proposal variant. When the collector follows that current proposal and owns at
least one official member identified by the catalog, Coindex may render a collection
catalog plate with owned and `Missing` reference members. This plate does not change the
proposal disposition, does not add the catalog to the curated-series registry, and does
not make the underlying Numista family a closed series.

A disposition whose exact key is absent from current derived proposals is dormant. It
does not materialize a proposal, but applies again if that exact key reappears. Proposal
derivation remains inventory-based and performs no runtime scraping. Collection catalogs
are versioned local data with explicit sources; they are never discovered over the
network at request time.

## Consequences

Only user intent survives sync; proposal content, counts, and membership continue to be
recomputed from current holdings. Renaming a family for display cannot orphan or merge
preferences, while any actual family, weight, or finish change denotes a different
variant.

The persistence change is additive and forward-only. Any later rollback is expressed as
a new forward migration rather than reversing an already applied schema change.

Catalog access remains user-scoped and variant-scoped. A matching family, weight, and
finish is insufficient on its own because issuers can reuse family names: at least one
current holding must match an official catalog member by Numista type ID. Removing that
holding or unfollowing the proposal makes the plate unavailable without deleting the
dormant preference.
