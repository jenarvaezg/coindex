# ADR 0007: Inventory-derived collection proposals are ephemeral and per user

- Status: accepted
- Date: 2026-07-29

## Context

Numista type metadata is cached globally to conserve API budget, but holdings belong to
one user. Deriving proposals from the global cache would create proposals for users with
no holdings and would let metadata discovered through one user's collection affect
another user's results.

A Numista family can also contain materially different physical variants. Treating the
family alone as the grouping boundary would leak one-ounce into two-ounce pieces, or
bullion into coloured pieces. Collection proposals therefore cannot make the catalog
coverage claim made by curated albums.

## Decision

Collection proposals are ephemeral, per-user views derived only from that user's current
holdings. They group pieces only when the Numista family matches exactly and normalized
weight and finish are equal. There is no fuzzy family matching and no runtime scraping.
Globally cached metadata may enrich types present in the user's holdings, but it must
never seed a proposal by itself.

Proposals are recomputed after sync. They never emit slots or `Missing`. Curated,
version-controlled JSON in git remains the source of truth for albums and catalog
coverage. Only derived proposal content remains ephemeral; per-user followed and ignored
dispositions are governed by ADR 0008.

## Consequences

Users without holdings receive no proposals, and one user's cached catalog metadata
cannot create another user's collection structure. Physical variants remain isolated, so
one-ounce/two-ounce and bullion/coloured pieces cannot leak across proposals. Persisting a
user's disposition does not weaken curated JSON as the authority for albums.
