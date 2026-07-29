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

Ignoring is reversible. Following is not promotion to a curated series or Album, does not
establish catalog coverage, and never creates `Missing` members.

A disposition whose exact key is absent from current derived proposals is dormant. It
does not materialize a proposal, but applies again if that exact key reappears. Proposal
derivation remains inventory-based and performs no runtime scraping.

## Consequences

Only user intent survives sync; proposal content, counts, and membership continue to be
recomputed from current holdings. Renaming a family for display cannot orphan or merge
preferences, while any actual family, weight, or finish change denotes a different
variant.

The persistence change is additive and forward-only. Any later rollback is expressed as
a new forward migration rather than reversing an already applied schema change.
