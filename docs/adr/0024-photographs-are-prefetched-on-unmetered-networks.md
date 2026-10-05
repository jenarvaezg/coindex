# ADR 0024: The photographs arrive before they are asked for, and only on wifi

- Status: accepted; amended by [#521](https://github.com/jenarvaezg/coindex/issues/521) (where the
  silent line is read)
- Date: 2026-08-05

## Context

Since ADR 0017 every picture is a Numista thumbnail, fetched by one loader through four slots and
retried when throttled. Since #190 the notebook export fetches all its photographs up front, which
made it reliable but slow (52 s for a seventy-page notebook), and a plate opened for the first time
fills in cell by cell.

Both faces of every seeded type are about 30 MB, needed once per phone because Coil's disk cache
(`cache/coil3_disk_cache`) survives restarts. They are CDN URLs, not `api.numista.com` calls, and
must not count against the budget of ADR 0003. What they spend is the collector's **data and
battery**.

## Decision

**Prefetch in the background on every launch, once the collection is read**, so the fichas of a new
APK and the photographs that failed get another chance. Asking for what is missing is idempotent.

**Both faces of every type in the index**, since cards and cells draw both; only the thumbnail, the
first candidate of each face.

**Only on an unmetered network, off power-save and low battery, and never during a sync.** The app
waits for wifi instead of asking the collector; a sync cancels the prefetch and takes the network.

**Two of the four slots, never four.** The loader serves requests in arrival order, so at four a
plate just opened would queue behind hundreds of pictures. **A notebook export stops the prefetch**,
as a sync does, because it wants all four slots.

**A photograph Numista answers `404` for is remembered for a month**, by an interceptor that sees
every photograph. Without the list a missing picture would be asked for on every launch; without the
expiry, one bad CDN minute would lose the picture for ever, even across clearing the cache. `403` is
not remembered: without a `User-Agent`, Cloudflare answers `403` to everything (ADR 0017).

**No ceiling per launch**, against the issue's suggestion: a few hundred per launch would take four
or five launches to fill the plates.

**The picture cache is 128 MB instead of 2 % of free disk**, which on a phone with 1.5 GB free would
be smaller than the set and refetch it on every launch. It keeps Coil's directory name, so nothing
downloaded is lost.

**It is silent**: no snackbar or banner, only one line on «Este teléfono» (since #521), because
«faltan 320 y están cayendo» and «faltan 320 porque estás con datos móviles» need different things
from the collector. It is not a button: overriding mobile data is what the wifi rule prevents.

## Consequences

Measured on a device (`coindex-ux`, wifi, cache emptied between runs):

| | |
| --- | --- |
| Cold cache, wifi | 1.658 photographs, 29,8 MB, in a little over two minutes |
| Second launch | **not one request**: only the cache's own journal was touched |
| On mobile data | **not one photograph**, and «Este teléfono» says so rather than staying silent |
| A `404` injected into the type cache | remembered once; the next launch asked for nothing and the count dropped to 1.657, permanently |

- #190 keeps the export reliable; the prefetch makes it start drawing at once.
- It runs in the ViewModel's scope, not an application scope or `WorkManager`: it saves a wait inside
  this app, every photograph is independent, and being cut short only loses requests not yet made.
- The conditions are read when a pass starts — at launch, after a sync, after an export, and on
  returning to the foreground with photographs missing — so «se traerán cuando haya wifi» holds
  without a relaunch. Wifi connecting while the app is in front goes unnoticed; a network callback
  is not worth it today.
- The APK declares `ACCESS_NETWORK_STATE`, needed to ask whether the network is metered.
