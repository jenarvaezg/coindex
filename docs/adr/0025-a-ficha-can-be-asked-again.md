# ADR 0025: A ficha can be asked again

- Status: accepted; second route added by
  [ADR 0033](0033-a-newer-snapshot-writes-over-the-ficha-it-corrects.md); wording amended by #516
- Date: 2026-08-05

## Context

#59 closed the «The» card of N#596807 expecting it to disappear once the referee published the
ficha. #185 showed it would not. `SyncService` asks only for types the cache lacks, and writes with
`insertIfAbsent`:

```kotlin
val cached = typeMeta.cachedTypeIds().toSet()
val missing = entities.map { it.typeId }.distinct().sorted().filterNot { it in cached }
```

The start-up top-up does not overwrite either (ADR 0017). A cached ficha is never read again, so
every upstream correction (#184, #159, #160; #83 saw the same with `series`) stays invisible on both
phones. The only way out was wiping the app's data and paying one call per type.

## Decision

### One gesture, one type, one call, where the wrong data is on screen

Only the collector asks. The label states the cost before the tap; with no budget left it says so
and refuses.

*Nota de forma, #516: la unidad es «consulta» (`Actualizar la ficha · 1 consulta`), también en el
informe del sincronizado y en las tres frases del mes agotado. La fijó el ADR 0030 §3 («Tasar esta
lámina · N consultas») sobre el mismo presupuesto, y es lo que se le promete al coleccionista («+2
consultas al mes», ADR 0029 §5). `PrunedVocabularyTest` la vigila sin distinguir mayúsculas.*

The gesture is on the piece inside a collection and on the row in Coins. A type whose ficha looks
like an unpublished draft derives no card (#186), so Coins is the only way to reach it, and that is
the case behind this ADR.

### The refresh is the only writer that overwrites a ficha

`TypeMetaDao.overwrite` exists for it. The seed and the sync keep ignoring conflicts (ADR 0017): the
APK's snapshot must not undo a ficha the collector paid for. Numista stays sovereign (#59): the
refresh writes whatever Numista publishes today.

> Since ADR 0033 the seed of a newly installed version also writes over cached fichas, once per
> `versionCode`. The gesture is still the only thing that asks Numista for a ficha from a phone.

### A refresh that fails is never worse than not having asked

No budget, no network, a rejected key or a 404 all leave the cached ficha as it was. The 404 has its
own sentence (`Numista ya no publica el tipo 596807. La ficha que tenías sigue en el móvil.`): here
it means a referee deleted the submission, and the sync's wording would send the collector to fix a
setting.

«Sin cambios» is reported as a result, with the call it spent. The cost is a constant: `/types/{id}`
needs no token, so it is exactly one reserved call, and reading the log would count a concurrent
sync.

Change is decided on parsed fields, not bytes. Ignoring `raw` would miss a corrected composition
(metal, issuer, diameter and category are read from it); comparing strings would flag every seeded
ficha, since the seed stores a re-encoded body.

### The cache already noted the date; the card now prints it

`fetchedAt` is the day this phone got the ficha, and the refresh stamps it again: `Ficha traída hace
8 meses`. For a seeded ficha the content may be older than that day, hence «traída» and not «es de»,
and no freshness rule ever hides the button.

### No batch refresh

No whole card, no inventory, no staleness policy: twenty pieces would spend twenty calls on the
nineteen nobody said were wrong, and an automatic policy can burn the month in an afternoon. What
would justify a batch is the collector reporting repeated presses, through the channel that already
opened #159, #160 and #184; `api_call_log` cannot tell a refresh from a sync's fetch anyway.

The plate has no gesture: its empty cells come from the seeded snapshot, which the curator corrects
in `scripts/seed-type-cache.py` and `data/`.

## Consequences

- Tests cover it: a corrected ficha replaces the row for one reserved call; an unchanged or merely
  re-serialized ficha reports no change but stamps today; no budget or a 404 leaves the row. On an
  emulator, a deliberately wrong N#100525 (family «The») recovered «Australian Koala» with one tap
  and joined its curated card.
- **Refreshing can move a coin to another card**, since the family is part of the variant key that
  identifies every uncurated card (ADR 0021 §5). A route to the old card says what happened instead
  of «esta colección ya no existe».
- The parse memos in `Mappers.kt` are keyed on `(typeId, fetchedAt)`, so a refreshed row is parsed
  again; the stale entry stays until the process ends.
