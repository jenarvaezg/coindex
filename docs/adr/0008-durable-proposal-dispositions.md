# ADR 0008: Durable per-user proposal dispositions

- Status: superseded by [ADR 0021](0021-what-a-collection-is-and-the-top-level.md)
- Date: 2026-07-29

> **Retired on 2026-08-04 by ADR 0021 §7.** Dispositions are gone: `Available`/`Followed`/`Ignored`
> no longer exist and `collection_proposal_preferences` is dropped in the v5 migration. The body is
> kept as the record of what that table held and why `MIGRATION_3_4` carried over only 30 literal
> keys. Its plate condition (the paragraph on collection catalog plates) does not carry over: a
> plate now opens on a current collection plus evidence by type (ADR 0021).

## Context

Proposal content is derived from current holdings and stays ephemeral (ADR 0007), but a collector
may want a choice to follow or ignore a proposal to survive later syncs without turning it into
catalog coverage. Family display aliases are not stable identities, and a preference whose holdings
have disappeared must not bring a proposal back.

## Decision

Persist only the per-user followed or ignored disposition for an exact proposal variant key: the
canonical tuple of resolved family, normalized weight, finish and dominant metal. Since the
[catalog-family precedence decision](https://github.com/jenarvaezg/coindex/issues/83) a selected
catalog is authoritative for that key. Lookup uses the same normalization as derivation, and display
aliases never take part.

Each derived proposal is in exactly one state: Available (nothing stored), Followed or Ignored.
Ignoring is reversible. Following is not promotion to a curated series or Album, establishes no
coverage and never creates `Missing` members.

A collection catalog may provide sourced coverage for one exact proposal variant. When the collector
follows that proposal and owns at least one official member identified by the catalog, Coindex may
render a catalog plate with owned and `Missing` members. The plate changes neither the disposition
nor the curated-series registry, and does not make the Numista family a closed series.

A disposition whose key is absent from current proposals is dormant: it creates nothing, and applies
again if the key reappears. Derivation stays inventory-based with no runtime scraping, and catalogs
are versioned local data with explicit sources, never fetched at request time.

## Consequences

Only user intent survives sync; content, counts and membership are recomputed. Renaming a family for
display cannot orphan or merge preferences, while a real change of family, weight or finish is a
different variant. The persistence change is additive and forward-only: a rollback is a new forward
migration.

Catalog access stays user- and variant-scoped. Family, weight and finish alone are not enough,
because issuers reuse family names: a current holding must match an official member by Numista type
ID. Losing that holding or unfollowing hides the plate without deleting the dormant preference.
