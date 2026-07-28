# ADR 0001: Nullable manual override targets

- Status: accepted
- Date: 2026-07-28

## Context

The Phase 1 specification declares `manual_overrides.slot_id` as `TEXT NOT NULL`, but
the same schema comment assigns `NULL` the meaning “this item belongs to no slot”.
Supporting that negative override is important: without it, a heuristic may repeatedly
reassign an item that the collector has explicitly rejected.

## Decision

`manual_overrides.slot_id` is nullable. A non-null value pins the item to that slot; a
null value permanently excludes the item from automatic matching until the user changes
the override.

## Consequences

The domain and persistence representations use an optional slot identifier. Sync never
deletes these rows. Album building applies both positive and negative manual overrides
before explicit type IDs or heuristics.
