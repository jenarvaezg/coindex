# Numista API: series coverage and local persistence

- Research date: 2026-07-29
- Official API documentation reviewed: v3.32
- Scope: read-only research; no authenticated request or API credential was used

## Conclusion

The documented API has no operation that enumerates, with a completeness guarantee, all types of
one exact series and finish.

- `GET /types` is a paginated search with a weight filter, but no `series` or `finish` parameter.
- `GET /types/{type_id}` exposes the exact `series` string and the weight, so it can validate a
  candidate, but nothing starts from a series and returns its members. There is no finish field.
- Searching by `q` and fetching each result can filter exact `series` and weight matches, but `q`
  is not documented as an exhaustive series lookup, so it proves nothing about completeness.
  Finish stays a Coindex heuristic over the title or other metadata.
- An exhaustive superset would mean paging every type of the relevant issuers (or the whole
  catalogue) and fetching every detail: a bulk catalogue scan that exhausts the monthly quota and
  runs into the extraction limits in Numista's terms.

Showing every expected option for a followed proposal therefore needs one of:

1. a curated, version-controlled manifest, as already used for Tudor Beasts;
2. a future Numista endpoint with an exact series identifier or filter; or
3. written permission and an agreed data feed or custom API arrangement with Numista.

Runtime scraping of the website is not a suitable fallback.

## The repository at the research date

The Numista client implemented only `GET /users/{user_id}/collected_items` and
`GET /types/{type_id}?lang=es`. The type DTO stores `series` and `weight` but no finish; finishes
are inferred from titles and known family conventions
([ADR 0005](../adr/0005-numista-finish-and-language-inference.md)).

Collection proposals are derived from owned inventory and claim no catalogue coverage.
[ADR 0007](../adr/0007-inventory-derived-collection-proposals.md) rules out runtime scraping, and
[ADR 0008](../adr/0008-durable-proposal-dispositions.md) states that following a proposal does not
make it a curated album or create missing members. Both are consistent with the API limitation.

## Documented API capabilities

In the [API documentation](https://en.numista.com/api/doc/index.php), catalogue search is
`GET /types`:

- every request needs a `Numista-API-Key`, public catalogue reads included;
- at least one of `q`, `issuer`, `catalogue`, `date` or `year` is required;
- `weight` accepts a value or a range in grams;
- results are paginated, at most 50 per page;
- there is no `series` or `finish` query parameter;
- the result summary holds identifiers and display metadata, no exact-series membership field.

`GET /types/{type_id}` documents `series` (the series name, if any), `weight` in grams, `title`,
tags, composition and other descriptive fields, and no finish property.

The website's catalogue UI does offer a Series selector and a Weight filter
([catalogue search help](https://en.numista.com/help/how-can-i-search-the-catalogue-99.html)), and
Numista publishes series pages such as
[The Royal Tudor Beasts](https://en.numista.com/catalogue/series.php?id=6888). Neither is part of
the API contract, and v3.32 has no `/series` operation.

`related_types` is documented only as a list of related types, not as the membership of a series,
so it cannot bound coverage.

## A best-effort API workflow

1. Search `GET /types` by `q`, issuer and/or weight, following every page.
2. Fetch each candidate with `GET /types/{type_id}`.
3. Keep exact `series` and normalized weight matches.
4. Apply Coindex's finish inference.

This discovers candidates; it does not enumerate a series. Its limits: undocumented recall of `q`
for series names, one extra request per candidate, no finish filter or value, a candidate set that
changes with the catalogue, and the [free plan](https://en.numista.com/api/pricing.php) quota of
2,000 requests a month. It can help an editor prepare or review a curated manifest, but must not
silently turn a followed proposal into a definitive list of missing coins. Narrowing a scan by
issuer, date and weight saves requests, but then those curated boundaries carry the completeness
claim, not the API.

## Persistence and terms

Neither the API documentation nor the pricing page publishes an API-specific cache, retention,
redistribution or bulk-import licence. The
[Numista Terms of Use (3 January 2023)](https://en.numista.com/conditions.php) apply to the whole
platform and state, in substance:

- no substantial or repeated extraction of platform content;
- no scraping or similar interference with automated processing systems;
- no permanent or temporary transfer of all or a substantial part of the database to another
  medium;
- no making a substantial part of the database public, and no reproduction, extraction or reuse of
  photographs and descriptions;
- the licence is personal, non-exclusive and non-transferable; other uses need prior express
  authorisation.

The terms neither permit a local mirror, full-series import or reusable dataset, nor address the
small operational cache an API client normally keeps. A conservative reading (not legal advice):

- Coindex's minimal private cache of type metadata for the collectors' own holdings differs from a
  catalogue mirror, but its permitted retention period is not documented;
- bulk or repeated import of every member of many series is not authorised merely because the API
  serves it;
- photographs and descriptions need particular care: the type response can name third-party
  copyright holders;
- a durable full-series catalogue, redistribution or public reuse needs written authorisation from
  Numista, via its [Custom Plan contact route](https://en.numista.com/api/pricing.php) or
  `contact@numista.com`.

## Recommended boundary for Coindex

- A followed proposal is a per-user organisational preference derived from owned items.
- A curated series is the only source that may show every expected option and mark absent ones as
  missing.

To make a followed collection behave like Tudor Beasts, add a separately reviewed curated manifest
for that exact family, weight and finish. Store only the Numista identifiers and metadata the
feature needs, keep provenance, skip unnecessary descriptions and images, and refresh deliberately
rather than crawling the catalogue.
