# ADR 0025: A ficha can be asked again

- Status: accepted
- Date: 2026-08-05

## Context

Issue #59 closed the «The» card of N#596807 with a sentence: the ficha is an unpublished draft, the
Numista datum is sovereign, and the card **«desaparece sola cuando el referee la publique, sin tocar
una línea de código»**. Issue #185 measured that sentence and it is false.

`SyncService` asks only for the types the cache does not hold, and writes them with
`insertIfAbsent`:

```kotlin
val cached = typeMeta.cachedTypeIds().toSet()
val missing = entities.map { it.typeId }.distinct().sorted().filterNot { it in cached }
```

The start-up top-up does not overwrite a synced row either, and with good reason (ADR 0017). So a
ficha that is already cached is never looked at again — not by a sync, not by a start, and not by any
gesture, because there was none. **The day the referee publishes the corrected page, that phone still
says «The».**

It is not a special case for one draft. The project already treats Numista as something that gets
corrected — #184 for this type, #160 and #159 for the Russian weights — and none of that work had a
route to the two phones that exist: the public catalogue gets fixed and both screens keep printing
the error, with no symptom to give it away. #83 had already seen the same trap from the other side
(«un `series` nuevo aguas arriba no llega solo») and filed it as latent; with an upstream fix
expected and written down, it stopped being latent.

The only way out was deleting the app's data and syncing from scratch: one call per type of the
inventory — hundreds — and it throws away the good fichas along with the wrong one.

## Decision

### One gesture, one type, one call, where the wrong data is on screen

The collector asks; nothing asks on their behalf. The card carries what it costs before the tap —
`Actualizar la ficha · 1 llamada` — and with no budget left it says so and refuses, because an
exhausted month on a free key is an expected outcome and not an error to discover by pressing.

*Nota de forma, #516: el rótulo dice `Actualizar la ficha · 1 consulta`, y con él el informe del
sincronizado, la línea durable bajo el botón de sincronizar y las **tres** frases del mes agotado —el
estado de Ajustes, la respuesta a pulsar «Tasar» y el snackbar del sincronizado. La unidad no la elige
esta cláusula: la eligió el ADR 0030 §3 al escribir «Tasar esta lámina · 34
consultas» sobre el mismo presupuesto de la misma clave, y mientras hubo dos funciones —`callsLabel` y
`queriesLabel`— hubo dos palabras a un toque de distancia. Gana «consulta» porque «llamada» es lo que
hace el código, mientras que lo que se le promete al coleccionista es un mes entero de ellas («+2
consultas al mes» por casilla, ADR 0029 §5). El 429 entra en el mismo diccionario: «peticiones» era una
tercera palabra para el objeto que cuenta el presupuesto.*

*La tercera frase del mes agotado se quedó fuera de la primera pasada y la cazó la revisión: el snackbar
del sincronizado abría con la palabra en mayúscula —«Llamadas a la API agotadas este mes»— y el barrido
de los ficheros de copia distinguía mayúsculas, así que le pasó por encima a la única frase que anuncia
el mes agotado sin que nadie haya pulsado nada. La comprobación de `PrunedVocabularyTest` lee ahora
`ignoreCase` y nombra las tres frases, por eso y por nada más.*

It lives on **both** surfaces where a piece of a type is visible: the piece inside a collection, and
the row of Coins. The second is not a convenience. A type whose ficha looks like an unpublished
draft derives no card at all (#186), so its pieces are reachable *only* from Coins — and that is
exactly the coin this issue was opened about. A gesture only on the piece card would have been
unreachable for the one ficha that motivated it.

### The refresh is the only writer that overwrites a ficha

`TypeMetaDao.overwrite` exists for it and nothing else. The seed and the sync keep ignoring
conflicts, unchanged from ADR 0017: neither of them was asked for the ficha it is holding, and the
snapshot travelling in the APK must never undo a ficha the collector paid budget for. What the
collector asked for wins over what the phone happens to have.

Numista stays sovereign (#59). The refresh writes what Numista publishes today, whatever that is; if
a contributor makes the page worse, the app shows the page it made worse, and the gesture can be
repeated tomorrow.

### A refresh that fails is never worse than not having asked

An exhausted budget, a dead network, a rejected key, a type Numista no longer publishes: in every
case the ficha already on the phone is left exactly as it was. The 404 gets its own sentence —
`Numista ya no publica el tipo 596807. La ficha que tenías sigue en el móvil.` — because on this
endpoint a 404 is a submission a referee deleted, which is the fate an unpublished type can have,
and the sync's wording for it («revisa el identificador de usuario en Ajustes») would send the
collector to fix a setting that is fine.

«Sin cambios» is a result and is spoken as one, with the call it spent: it is the answer «what you
have is what Numista has», and pretending the tap was free would be the beginning of a gesture
nobody can budget for. The call is a **constant**, not a measurement — `/types/{id}` needs no OAuth
token, so the gesture is exactly one reserved call, and counting the month's log before and after
would have read a sync running at the same time into this one tap.

Whether the ficha changed is decided on the fields, not on the bytes: the columns compare as
columns, and the two bodies compare **parsed**. Ignoring `raw` would report «sin cambios» over a
corrected composition, since the metal, the issuer's name, the diameter and the category are all read
out of it; comparing the two strings would report a change on every ficha that came from the
snapshot, because the seed stores the asset re-encoded by this app and a refresh stores Numista's own
body.

### The cache already noted the date; the card now prints it

`fetchedAt` has been in `type_meta` since the beginning, so nothing is migrated. What it means is
now stated: **the day this phone got the ficha**, which the refresh stamps again. The card prints
it as `Ficha traída hace 8 meses`.

For a ficha the collector synced, that day is when Numista answered. For one that arrived in the
APK's snapshot, it is the day the snapshot was seeded, and the *content* may be older than that —
the asset is rebuilt whenever a catalog is curated. The wording says «traída» rather than «es de»
for that reason, and it is why **no freshness rule hides the gesture**: there is no «esta ficha ya
está fresca» that could be trusted, so the button is always there.

### No batch refresh, and this is where it is measured

Not a whole card, not the inventory, not a staleness policy that refreshes on its own. #185 asked
for the small thing first and this is the small thing: refreshing a collection of twenty pieces
would be twenty calls spent on the nineteen nobody said were wrong, and an automatic policy is the
cheapest known way to burn a month's budget in an afternoon.

**And the measurement is the collector saying so**, not a counter. There are two phones and one
curator, and the channel between them already works — it is how #184, #159 and #160 were opened in
the first place. A per-gesture tally would be instrumentation on two devices that nobody would ever
read, and `api_call_log` cannot tell a refresh from a sync's type fetch because it is the same
endpoint. So the evidence for a batch is «he estado tocando el mismo botón seis veces en una
tarjeta», and until somebody says it, a batch would be a mechanism guessing what the collector
knows.

The plate keeps no gesture either, and that boundary is deliberate: its empty cells are coins nobody
owns, drawn from the seeded snapshot, and correcting those is the curator's job upstream —
`scripts/seed-type-cache.py` and the asset in `data/` — not the collector's monthly budget.

## Consequences

Verified on the two seams that hold the rule: a corrected ficha replaces the cached row, body
included, and spends exactly one reserved call on `/types/{id}`; the same ficha fetched twice reports
no change and still stamps today's date; a ficha whose only difference is how it was serialized
reports no change either; an exhausted budget and a 404 both leave the row untouched.

Verified on a device too, which is where the gesture either exists or does not. On an emulator
carrying one piece of N#100525 whose ficha had been seeded wrong on purpose — family «The», brought
over a year ago, and therefore no card at all, exactly the shape of #596807 — the row of Coins read
`Ficha traída hace 1 año` with `Actualizar la ficha · 1 llamada` under it. One tap: the title became
the one Numista publishes, the family became «Australian Koala», the line became `Ficha traída hoy`,
`api_call_log` held a single row for `/types/100525`, and the coin then joined the curated Koala card
it had been missing from. A second tap answered `La ficha de Numista 100525 sigue igual. Has gastado
1 llamada.` The same block reads the same on the piece inside a collection.

**Refreshing can move a coin to another card.** The family is part of the variant key, and the key is
the identity of every card no curated file names (ADR 0021 §5), so the very fix this ADR exists for —
«The» becoming the family the referee published — recomputes the derived collection under a different
key. A route that named the old one now says both things that could have happened, instead of the
bare «esta colección ya no existe» that reads as lost data.

The parse memos in `Mappers.kt` needed nothing: they are keyed on `(typeId, fetchedAt)` precisely so
a row written again is read again, and a refresh stamps a new date. The old entry stays in the map
for the life of the process, unreachable — a handful of parsed strings per refresh, against a memo
that exists to save a tenth of a second on every emission of the collection.
