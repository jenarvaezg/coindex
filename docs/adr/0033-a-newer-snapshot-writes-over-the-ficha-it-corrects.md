# ADR 0033: A newer snapshot writes over the ficha it corrects, once per installed version

- Status: accepted
- Date: 2026-09-30
- Decides [#606](https://github.com/jenarvaezg/coindex/issues/606). Adds a second route to
  [ADR 0025](0025-a-ficha-can-be-asked-again.md) and narrows the rule
  [ADR 0017](0017-plate-photographs-are-thumbnails-and-are-retried.md) wrote into `TypeCacheSeed`

## Context

ADR 0025 opened a route for a corrected ficha to reach a phone, and measured its own shape
honestly: **one gesture, one type, one consulta, where the wrong data is on screen**. It works for
what it was written for — the collector sees «The» on a card and presses.

It does not work for anything else, and this repository produces the anything else constantly.
Numista gets corrected here as a matter of course: #603 has nine Peruvian fichas with the fine
weight in the gross weight's box, #388 was a weight Numista accepted, #596 an issue that did not
exist. When a referee accepts one of those, the curator re-seeds it —
`scripts/seed-type-cache.py --refresh` — and the corrected ficha travels in the next APK, inside
`data/numista-type-cache.json`, at a cost to the collectors of **zero consultas**.

And then nothing happens, because `TypeCacheSeed.topUp` writes with `insertIfAbsent`. Its own
comment said so: *«Nothing is overwritten: the insert ignores conflicts, so a ficha the collector
paid API budget to sync stays as it was synced.»* The corrected datum is on the phone, in the
assets, and the row does not move.

Nine gestures on nine cards the collector has no reason to press is not a route. And the fichas he
cannot know are wrong have no route at all.

## Decision

### 1. The snapshot of a newly installed version is written over the fichas already cached

> **`TypeCacheSeed.topUp` remembers the `versionCode` it last applied. On a version it has not
> applied, it writes the whole snapshot — `overwrite`, not `insertIfAbsent` — and then writes the
> version down. On a version it has applied, it behaves exactly as before.**

### 2. The clock is the version, and not a date

A snapshot travels inside exactly one APK. So «is this snapshot newer than this row?» is «has this
`versionCode` been applied on this phone?», which is one integer in a preferences file — no stamp
in the asset, no build timestamp, no `git log` in the build, and nothing that makes two builds of
the same commit differ.

It also protects ADR 0025's gesture without needing a date at all: the seed writes once, when the
update lands, so **a ficha the collector refreshes by hand afterwards is his until the next
release**. Two writes now overwrite a cached ficha and they do not collide — his gesture, and the
update he installed.

The window it cannot see is a hand refresh made **between** the curator taking the snapshot and the
release shipping it. In this repository those travel in the same pull request, so the window is
hours; and what overwrites him inside it is the curated datum, which was verified against
numista.com before it was versioned, which is the curator's standing rule for any external id.
That is the trade, and it is deliberately not symmetrical: an unnoticed wrong ficha lasts for ever,
and a stomped hand refresh lasts one gesture.

### 3. The version is written down after the writes, not before

A process killed halfway leaves the version unapplied and the next start does the whole thing
again. Re-applying a snapshot is idempotent; skipping one is a wrong ficha that stays for a release.

### 4. Nothing new is read on a start that has nothing to do

The 2,4 MB of JSON is still parsed only when there is something to write. The condition gained one
term and both are cheap: every curated type cached (one column of integers) **and** this version
already applied (one integer out of a preferences file).

## Consequences

- A correction accepted by a Numista referee reaches both phones with the next release, for zero
  consultas, without anybody pressing anything.
- The first launch after an update writes ~1.089 rows. It is one batch, no network, and it runs on
  the warm-up that already runs before the collection is read.
- ADR 0025 is **not** superseded. Its gesture is still the only thing that fetches a ficha from
  Numista on a phone, it still costs one consulta, and it still outranks the seed until the next
  release. What changes is that it is no longer the only way a correction can arrive.
- The rejected alternative stays rejected: a trickle of automatic ficha refreshes, oldest first,
  would cost one consulta each and arrive later than a release does here. It is worth reopening only
  if releases start being rare.
