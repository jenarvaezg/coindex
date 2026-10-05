# ADR 0033: A newer snapshot writes over the ficha it corrects, once per installed version

- Status: accepted
- Date: 2026-09-30
- Decides [#606](https://github.com/jenarvaezg/coindex/issues/606). Adds a second route to
  [ADR 0025](0025-a-ficha-can-be-asked-again.md) and narrows the rule
  [ADR 0017](0017-plate-photographs-are-thumbnails-and-are-retried.md) wrote into `TypeCacheSeed`

## Context

ADR 0025's route needs the collector to see the error and press. But this repository corrects
Numista routinely (#603, #388, #596), and once a referee accepts a fix the curator re-seeds the
ficha (`scripts/seed-type-cache.py --refresh`) into `data/numista-type-cache.json` for the next APK.
There it stops: `TypeCacheSeed.topUp` writes with `insertIfAbsent` to protect fichas the collector
paid for, so the corrected datum never reaches the row.

## Decision

### 1. The snapshot of a newly installed version is written over the fichas already cached

> **`TypeCacheSeed.topUp` remembers the `versionCode` it last applied. On a version it has not
> applied, it writes the whole snapshot — `overwrite`, not `insertIfAbsent` — and then writes the
> version down. On a version it has applied, it behaves exactly as before.**

### 2. The clock is the version, and not a date

A snapshot ships in exactly one APK, so «newer than this row» means «this `versionCode` not yet
applied»: one integer in preferences, with no timestamp in the asset or the build. A hand refresh
made after the update stays until the next release, which protects ADR 0025's gesture. The blind
spot, a refresh between the snapshot and its release, lasts hours because both travel in one pull
request, and what wins there is a datum verified against numista.com. An unnoticed wrong ficha would
last for ever; a lost refresh costs one gesture.

### 3. The version is written down after the writes, not before

A killed process re-applies everything on the next start. Re-applying is idempotent; skipping is
not.

### 4. Nothing new is read on a start that has nothing to do

The 2,4 MB of JSON is parsed only when there is something to write: a curated type missing from the
cache, or this version not yet applied.

## Consequences

- An accepted Numista correction reaches both phones with the next release, at zero consultas.
- The first launch after an update writes ~1.089 rows, offline, in the existing warm-up.
- ADR 0025 is not superseded: its gesture is still the only Numista fetch of a ficha from a phone,
  and it wins until the next release.
- Automatic trickle refreshes stay rejected (one consulta each, slower than a release); reopen if
  releases become rare.
