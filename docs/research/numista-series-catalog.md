# Numista API: series coverage and local persistence

- Research date: 2026-07-29
- Official API documentation reviewed: v3.32
- Scope: read-only research; no authenticated request or API credential was used

## Conclusion

The documented Numista API has no direct, series-scoped operation that enumerates with a
completeness guarantee all types in one exact series and finish.

`GET /types` provides a paginated catalogue search and accepts a weight filter, but its
documented parameters do not include `series` or `finish`. `GET /types/{type_id}` exposes
the exact `series` string and the type weight, so it can validate individual candidates,
but there is no documented operation that starts from an exact series and returns all of
its members. The detailed response also has no dedicated finish field.

A query-based candidate search followed by one detail request per result could filter
types whose returned `series` and weight match exactly. It would still not prove
completeness because the official documentation does not define `q` as an exhaustive
series lookup. Finish would remain a Coindex heuristic based on title or other metadata.

In principle, a client could create an exhaustive superset by paging through every type
for all relevant issuers—or ultimately the whole catalogue—and then fetching every type
detail to compare `series` exactly. That is a bulk catalogue scan, not a series API. It
would be request-heavy, quickly consume or exceed the monthly quota, and raise the
extraction concerns described in Numista's terms.

For Coindex, displaying all expected options after following a proposal therefore needs
one of these sources of catalog coverage:

1. a curated, version-controlled manifest, as already used for Tudor Beasts;
2. a future Numista endpoint that supports an exact series identifier/filter; or
3. written permission and an agreed data feed or custom API arrangement from Numista.

Runtime scraping of the website is not a suitable fallback.

## What the repository currently does

The local Numista client only implements collection retrieval and individual type
retrieval:

- `GET /users/{user_id}/collected_items`;
- `GET /types/{type_id}?lang=es`.

The individual-type DTO stores `series` and `weight`, but no explicit finish. Coindex
currently infers finishes from titles and known family conventions, a limitation already
recorded in [ADR 0005](../adr/0005-numista-finish-and-language-inference.md).

Collection proposals are intentionally derived from a user's owned inventory and do not
claim complete catalogue coverage. [ADR 0007](../adr/0007-inventory-derived-collection-proposals.md)
also rules out runtime scraping, while [ADR 0008](../adr/0008-durable-proposal-dispositions.md)
states that following a proposal does not promote it to a curated album or create missing
members. Those decisions are consistent with the current API limitation.

## Documented API capabilities

Numista describes its API as programmatic access to catalogue and collection data. In the
current [API documentation](https://en.numista.com/api/doc/index.php), the catalogue
search operation is `GET /types`.

The documented search contract has these relevant properties:

- every API request requires a `Numista-API-Key`, including public catalogue reads;
- at least one of `q`, `issuer`, `catalogue`, `date`, or `year` must be supplied;
- `weight` accepts a value or range in grams;
- results are paginated with at most 50 entries per page;
- there is no documented `series` query parameter;
- there is no documented `finish` query parameter;
- the result summary contains identifiers and basic display metadata, not a documented
  exact-series membership field.

The detailed `GET /types/{type_id}` response documents:

- `series`: the series name when the type belongs to one;
- `weight`: weight in grams;
- `title`, tags, composition and other descriptive fields;
- no dedicated finish property.

The website's own catalogue UI does offer a Series selector and a Weight filter, as
described in Numista's official
[catalogue search help](https://en.numista.com/help/how-can-i-search-the-catalogue-99.html).
That website behavior is not part of the published API contract and does not establish
that the API's free-text `q` parameter performs an exact or exhaustive series search.
Numista also publishes human-facing series pages, for example
[The Royal Tudor Beasts](https://en.numista.com/catalogue/series.php?id=6888), but no
equivalent `/series` operation appears in the documented v3.32 API.

`related_types` in a type detail response is only documented as a list of related types;
the documentation does not define it as the complete membership of the same series.
It cannot be used as a coverage boundary.

## Feasible API workflow and its limits

A best-effort API workflow could:

1. search `GET /types` using `q`, issuer and/or weight;
2. follow every result page;
3. fetch each candidate with `GET /types/{type_id}`;
4. retain only exact `series` and normalized weight matches;
5. apply Coindex's auditable finish inference.

This is useful for discovering candidates, but it is not a complete-series algorithm.
Its main limits are:

- undocumented recall of `q` for series names;
- one extra request per candidate to obtain the exact series and detailed weight;
- no first-class finish filter or value;
- catalogue changes can alter the candidate set later;
- Numista's current
  [free plan](https://en.numista.com/api/pricing.php) has a quota of 2,000 requests per
  month.

Consequently, this workflow may help an editor prepare or review a curated manifest, but
it should not silently turn a followed proposal into a definitive list of missing coins.
Restricting a scan with known issuer, date and weight boundaries can reduce requests, but
those curated boundaries—not the API—then carry the completeness claim.

## Persistence and terms

The official API pages establish that Numista offers data for programmatic application
use, but neither the API documentation nor the pricing page publishes an API-specific
cache, retention, redistribution, or bulk-import licence.

The currently published
[Numista Terms of Use (3 January 2023)](https://en.numista.com/conditions.php)
apply broadly to the platform and state, in substance:

- users must not perform substantial or repeated extraction of platform content;
- scraping or similar interference with automated processing systems is prohibited;
- permanent or temporary transfer of all or a substantial part of the database to
  another medium is forbidden;
- making a substantial part of the database public, and reproduction, extraction or
  reuse of photographs and descriptions, is forbidden;
- the granted licence is personal, non-exclusive and non-transferable; other uses need
  prior express authorisation.

These terms do not provide a clear positive permission to build and retain a local
mirror, full-series import, or reusable catalogue dataset. They also do not expressly
discuss the small operational cache an API client normally uses. Therefore:

- Coindex's minimal private cache of type metadata needed for the collectors' own
  holdings is materially different from a catalogue mirror, but its permitted retention
  period is not expressly documented;
- bulk or repeated import of every member of many series should not be treated as
  authorised merely because the records are accessible through the API;
- photographs and descriptive text require particular caution because the type response
  can identify third-party copyright holders;
- a durable full-series catalogue, redistribution, or public reuse should proceed only
  with written authorisation from Numista.

This is a conservative product/engineering reading, not legal advice. Numista directs
enterprise or specific API needs to its
[Custom Plan contact route](https://en.numista.com/api/pricing.php), and its current
contact address is `contact@numista.com`.

## Recommended boundary for Coindex

Keep the distinction already present in the domain model:

- a followed proposal is a per-user organizational preference derived from owned items;
- a curated series is the only source that may show every expected option and mark
  absent ones as missing.

To make a followed collection behave like Tudor Beasts without scraping or overstating
API completeness, add a separately reviewed curated manifest for that exact family,
weight and finish. Store only the Numista identifiers and metadata necessary for the
feature, preserve provenance, avoid copying unnecessary descriptions or images, and
refresh deliberately rather than attempting a bulk catalogue crawl.
