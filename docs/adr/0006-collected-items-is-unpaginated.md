# ADR 0006: `collected_items` is unpaginated in the observed v3.32 contract

## Status

Accepted for Phase 1, based on authenticated responses recorded on 2026-07-29.

## Context

The Phase 1 client makes one request to
`GET /users/{user_id}/collected_items`. Before observing the real API response, it was
uncertain whether larger collections required pagination.

The exact responses for both configured accounts were captured privately because they
contain personal inventory data. They are not committed as fixtures. Public type
metadata remains suitable for committed empirical fixtures.

## Evidence

Both collection responses contained only these top-level fields:

- `item_count`
- `item_for_swap_count`
- `item_type_count`
- `item_type_for_swap_count`
- `items`

Neither response contained a cursor, page number, next link, or other pagination field.
For both accounts:

- `item_count` equalled the sum of `items[].quantity`;
- `item_type_count` equalled the number of unique `items[].type.id` values.

The two accounts had materially different collection sizes, and the larger response still
returned the complete `items` array in a single request.

## Decision

Phase 1 keeps a single collection request per sync and treats `items` as the complete
snapshot. A missing `items` field remains an invalid/incomplete response and must never
clear the previous snapshot.

If a future empirical response introduces pagination metadata, the client must implement
it before replacing the snapshot; silently accepting only the first page is forbidden.
