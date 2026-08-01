# ADR 0017: The snapshot keeps up, the plate asks for thumbnails, and it asks again

- Status: accepted
- Date: 2026-08-01

## Context

The collector reported that some plates come out with photographs missing. On the exported
sheet of the **1000 escudos of Portugal** only 7 of the 19 cells had a picture; six of the
twelve empty ones were coins he owns, and in the sheet they read exactly like the gaps of the
collection — only the small «TENGO» told them apart.

### What the phone actually had

Issue #67 measured `data/numista-type-cache.json`, found all 19 fichas present with both faces,
and concluded it was not a data problem. That measurement was of the **asset**, and the app does
not read the asset — it reads the type cache in its database, seeded from the asset **once**:

```kotlin
if (typeMeta.count() > 0) return 0
```

So the snapshot was a first-install gift. Every catalog curated afterwards shipped its fichas in
the APK and none of them ever reached a phone that already had the app; and because a cached
type is never re-fetched, the missing fichas had no second route in either. Reproduced on a
device with a real version-2 database: **10 of the 19 escudos types were cached, 9 were not**,
and those 9 drew silhouettes exactly as designed. Three of the seven owned pieces were also
sitting in «sin clasificar», for the same reason — no ficha, no family, no proposal.

That is most of what the collector saw. It is not the whole of it.

### The burst, and a failure that was final

- The sheet asks for **38 photographs at once** (19 cells × 2 faces). Requested concurrently,
  **ten of the thirty-eight answered `503`**; the same URLs in series answered `200`. It is
  throttling, not dead pictures. Each original weighs around 220 KB, so a plate was eight
  megabytes asked for in one go — to draw them a centimetre wide.
- Coil does not retry, and the cell stays empty for good. That behaviour is deliberate — a cell
  with no network must still read as a coin rather than as a blank — but it was the only
  behaviour there was.
- The export captured the hole. It waits for every picture to «settle», a failure settles like
  a success, and the sheet went out with the gaps frozen into it, announced as «lámina
  completa».

Licensing was ruled out: there is no such filter in the code, and it does not correlate —
N#25338 is CC0 and came out empty, N#11700 has no licence and came out with a picture.

One more fact worth recording: **without a `User-Agent`, Cloudflare answers `403` to every
photograph**; with any at all — even `okhttp/4.12.0` — it answers `200`. Today the header is
written by OkHttp underneath Coil, so the pictures work by inertia. A change of network engine
that left it empty would turn off every picture in the app at once.

## Decision

**The snapshot is a top-up, not a first-install gift.** At every start the app compares the type
ids the curated files name against the ids the cache holds, and parses the 2.4 MB snapshot only
when something is missing. Nothing is ever overwritten: the insert ignores conflicts, so a ficha
the collector paid API budget to sync stays as it was synced.

**The plate asks for the thumbnail.** Every ficha carries `picture` (`…-original.jpg`) and
`thumbnail` (`…-180.jpg`); a plate cell is about a centimetre wide, and 180 pixels covers it.
The type cache stores both from version 3 of the database onwards, and a cache written before
that is filled in from the ficha each row already keeps in `raw` — no API call is spent, which
is what that column was put there for.

**The original stays behind it as the fallback.** A face is a list of candidates, best first:
if the thumbnail is missing or refused, the original is tried before the cell gives up. What
was one shot at one URL is now two shots at two sizes of the same picture.

**A refused photograph is asked for again.** Four requests at a time, and up to three attempts
each, waiting 0.4 s and then 1.2 s, honouring the server's `Retry-After` up to five seconds.
`408`, `429` and `5xx` are retried, because they are the edge saying «not now»; `403` and `404`
are not, because they are answers about the picture itself.

**The app says who it is**: `Coindex/<version> (+https://github.com/jenarvaezg/coindex)`, set
by this app rather than inherited, which is also what a volunteer catalogue giving us its
images for free is owed.

**An exported sheet is only called complete when it is.** The export counts the photographs
that actually painted, not the ones that stopped changing, and says how many are missing.

## Consequences

Verified on a device carrying a real version-2 database of 608 cached types: it migrated,
topped up to 688, filled in every thumbnail from `raw`, and exported the 1000 escudos from a
cold image cache as **19 cells of 19 with both faces** — the sheet the issue was opened about.

The thumbnail is what every picture in the app now asks for first — the plate, the sheet and the
inventory card alike — because they draw a coin at the same centimetre and share one loader and
one cache. The tightest case is a sheet of twelve issues or fewer, which draws ~230 px from a
180 px picture: slightly softer than before, and paid against twelve empty cells out of nineteen.

The export now waits up to 30 s rather than 20 s, because the retries need room; a plate whose
pictures are all cached still exports as fast as it ever did.

Attribution is **not** settled here. The fichas carry `picture_copyright` and sometimes
`picture_license_name` — «NumisCorner», CC BY-SA, CC0, CC BY-NC-SA — and the exported sheet
credits nobody. The sheet is shared, so that is a question of its own and it keeps its own
issue.
