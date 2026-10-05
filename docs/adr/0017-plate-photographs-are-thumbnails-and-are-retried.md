# ADR 0017: The snapshot keeps up, the plate asks for thumbnails, and it asks again

- Status: accepted; the top-up narrowed by
  [ADR 0033](0033-a-newer-snapshot-writes-over-the-ficha-it-corrects.md)
- Date: 2026-08-01

## Context

Exported plates came out with photographs missing: on the 1000 escudos of Portugal only 7 of 19
cells had a picture, and owned coins read like gaps. There were four causes.

- **The snapshot was a first-install gift.** The app reads its own type cache, seeded from
  `data/numista-type-cache.json` only while empty (`if (typeMeta.count() > 0) return 0`). Fichas of
  catalogs curated later never reached an installed phone, and a cached type is never re-fetched:
  9 of the 19 escudos types were missing on the phone (#67 had checked the asset, not the phone).
- **The burst was throttled.** A sheet asks for 38 photographs at once (19 cells × 2 faces, about
  220 KB each); concurrently, ten answered `503`, and in series all answered `200`.
- **A failure was final.** Coil does not retry, and the export counted a failure as settled and
  called the sheet «lámina completa».
- **Without a `User-Agent`, Cloudflare answers `403` to every photograph.** OkHttp was sending one
  underneath Coil; another network engine could silently turn every picture off.

Licensing was ruled out: no such filter exists and it did not correlate (N#25338 is CC0 and came
out empty).

## Decision

**The snapshot is a top-up.** At every start the app compares the type ids the curated files name
with the cache, and parses the 2.4 MB snapshot only when something is missing. The insert ignores
conflicts, so a synced ficha stays as synced. *(ADR 0033 narrows this: a newly installed version's
snapshot overwrites cached fichas once.)*

**The plate asks for the thumbnail** (`…-180.jpg`): a cell is about a centimetre wide. The type cache
stores `thumbnail` beside `picture` from database version 3; older rows are filled in from `raw`
without an API call.

**The original is the fallback.** A face is a list of candidates, best first.

**A refused photograph is asked for again.** Four requests at a time, up to three attempts, waiting
0.4 s and then 1.2 s and honouring `Retry-After` up to 5 s. `408`, `429` and `5xx` are retried;
`403` and `404` are answers about the picture itself and are not.

**The app identifies itself** as `Coindex/<version> (+https://github.com/jenarvaezg/coindex)`, set by
the app rather than inherited, which is also a courtesy owed to a volunteer catalogue.

**An exported sheet is only called complete when it is**: the export counts the photographs that
actually painted and says how many are missing.

## Consequences

- Verified on a device with a real version-2 database: it migrated, topped up, filled thumbnails from
  `raw`, and exported the escudos from a cold image cache with 19 of 19 cells and both faces.
- Every picture in the app asks for the thumbnail first, since they share one loader and one cache.
  A sheet of twelve issues or fewer draws ~230 px from 180 px: slightly softer, accepted.
- The export waits up to 30 s instead of 20 s, to leave room for retries.
- Photo attribution (`picture_copyright`, `picture_license_name`) is not settled here; it went to its
  own issue.
