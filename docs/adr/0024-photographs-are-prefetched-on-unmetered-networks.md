# ADR 0024: The photographs arrive before they are asked for, and only on wifi

- Status: accepted, the silent line relocated by
  [#521](https://github.com/jenarvaezg/coindex/issues/521) (it is read on «Este teléfono», the screen
  the sewn edge opens, and it stays a line and not a button: mobile data is the one refusal with
  anything to push, and pushing it is what the wifi condition exists to prevent)
- Date: 2026-08-05

## Context

Since ADR 0017 every picture in the app is a Numista thumbnail, fetched by one loader through four
slots and retried when the edge throttles it. Since #190 the notebook export fetches every
photograph it needs **up front**, in one queue, instead of page by page: that is what made a
seventy-page export reliable — 623 of 623 pictures — and it takes 52 seconds, every one of them
paid at the moment the collector presses the button.

The same cost is paid, in smaller change, every time a plate is opened for the first time: the
cells fill in one by one while he watches. Nothing was wrong with any of it. It was simply all
being done at the last possible moment.

What is actually being moved is small. The father's notebook needs 319 distinct pictures, 11,5 MB;
the whole index — both faces of every seeded type — measured **1.658 photographs and 29,8 MB** on a
device, once in the life of a phone, because Coil's disk cache (`cache/coil3_disk_cache`) survives
restarts. None of it touches the API budget of ADR 0003: these are CDN URLs, not calls to
`api.numista.com`, and they must not start being counted as such.

What it does spend is the collector's **data and battery**, on pictures nobody has asked for yet.
That is the whole of the design problem.

## Decision

**The photographs are prefetched in the background, on every launch, once the collection has been
read.** Not after the first sync only: a catalog curated later ships new fichas in a new APK, and a
photograph that failed today has to have a second route. Asking for what is missing is idempotent
and cheap, so «every launch» is also «the second launch costs nothing».

**Both faces of every type the index holds**, not just the reverses the notebook draws. A card and
a plate cell draw obverse and reverse side by side; warming one of the two would leave half of
every plate filling in before the collector's eyes, which is the thing this exists to remove. Only
the first candidate of each face — the thumbnail — because the original behind it is the fallback
for a refusal that mostly does not happen.

**Only on an unmetered network, off power-save, off a low battery, and never during a sync.** The
first is the rule that matters: 22 MB nobody asked for do not come out of a mobile tariff, and
rather than asking a question the collector would have to answer, the app simply waits for wifi. A
sync in flight cancels a prefetch in progress and takes the network back — it is spending API
budget and is being waited for; the prefetch is neither.

**Two of the four slots, never four.** The loader's dispatcher serves requests in the order they
arrive, so a prefetch at four would park the plate the collector has just opened behind six hundred
pictures nobody asked for. At two, a screen always finds a free slot and overtakes. **Exporting the
notebook stands it down entirely**, like a sync does: the export wants all four slots and the
collector is watching it happen, so two of them held by pictures nobody asked for is precisely the
theft this is designed not to commit.

**A photograph Numista answers `404` for is remembered for a month**, written by an interceptor
that watches every photograph, not only the prefetched ones. The disk cache only holds answers that
arrived, so without this list a missing picture would be asked for again on every launch for the
life of the phone; and without the month, a CDN having a bad minute would take that picture out of
the catalog on that phone **for ever**, invisibly, with not even clearing the cache to repair it.
`403` is deliberately not remembered at all: without a `User-Agent` Cloudflare answers `403` to
everything (ADR 0017).

**No ceiling per launch**, decided against the issue's own suggestion. A ceiling of a few hundred
would mean four or five launches before the plates stop filling in, which is most of the wait being
removed; 30 MB over wifi, once, is not a burst worth staging.

**The picture cache is 128 MB and no longer 2 % of the free disk.** Coil's default is right for an
app whose pictures are a stream; these are a finite set fetched on purpose, and on a phone with a
gigabyte and a half free the default cache would be *smaller than the set* — every launch would
fetch the tail, evict the head, and buy the same photographs again on the collector's data. It
keeps the directory name Coil would have chosen, so nothing already downloaded is abandoned.

**It is silent.** No snackbar, no banner, nothing to dismiss — an optimization that announces
itself becomes a chore to supervise. It says one line, on «Este teléfono», and only because
«faltan 320 y están cayendo» and «faltan 320 porque estás con datos móviles» look identical from
the outside and need different things from the collector.

## Consequences

Measured on a device (`coindex-ux`, wifi, cache emptied between runs):

| | |
| --- | --- |
| Cold cache, wifi | 1.658 photographs, 29,8 MB, in a little over two minutes |
| Second launch | **not one request**: only the cache's own journal was touched |
| On mobile data | **not one photograph**, and «Este teléfono» says so rather than staying silent |
| A `404` injected into the type cache | remembered once; the next launch asked for nothing and the count dropped to 1.657, permanently |

The export of #190 stays exactly as it is. It is what makes the export **reliable** — each
photograph asked for once, no page freezing an avoidable hole — and this is what makes it **fast**:
with the cache warm it starts drawing immediately. The two together, not one instead of the other.

The prefetch runs in the ViewModel's scope, not an application scope or `WorkManager`. What it
saves is a wait the collector would see *in this app*, every photograph is independent, and nothing
is written but the cache, so being cut short when they leave costs only the requests not yet made.

The conditions are read once, when a pass starts — at launch, after a sync, after an export, and
when the app returns to the foreground with photographs still missing. That last one is what makes
«se traerán cuando haya wifi» true rather than a promise for the next launch. What it still does
not do is notice a wifi connecting **while the app is in front**; registering a network callback
for that is a bigger promise than the problem deserves today.

The APK now declares `ACCESS_NETWORK_STATE`, which is what «is this network metered» costs.
