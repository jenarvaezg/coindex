# Numista API: what it records about coins you do not own

- Research date: 2026-08-13
- Official API specification reviewed: **v3.33**, from
  `https://en.numista.com/api/doc/swagger.yaml?v=3.33` (the machine-readable source behind the
  Redoc page `https://en.numista.com/api/doc/`, which renders nothing without JavaScript)
- Help centre reviewed: `https://en.numista.com/help/index.php`, article «How can I set multiple
  collections?» (last updated 7 April 2023)
- Scope: read-only research. No authenticated request, no API quota spent: the specification and
  the help centre are public. Collection facts come from the captures in `.local/`.
- Opened by [#483](https://github.com/jenarvaezg/coindex/issues/483) for the decision in
  [#484](https://github.com/jenarvaezg/coindex/issues/484), later taken by ADR 0029 (a wish is
  marked on the phone).

## Conclusion

**Numista has no wish list, and the API cannot carry one.** The words *wish*, *want* and *desire*
do not occur in the specification, and the help centre has no article about wanting a coin. Every
personal-data endpoint is about ownership: `/users/{user_id}/collected_items` is «Get the items
**owned** by a user», with counters `item_count` («Count of items **owned** by the user») and
`item_for_swap_count`.

The nearest concept, `for_swap`, marks what you **have** and will part with. Numista's swap model
has no counterpart for what you are looking for: partners browse what others offer.

So the path #484 hoped for — a wish arriving through sync as a measured fact, leaving ADR 0021 §7
untouched — works mechanically only by telling Numista you own a coin you do not.

## 1. Is there a wish list, and does v3 expose it?

No. The complete list of paths in v3.33:

```
/types                                   /issuers          /oauth_token
/types/{type_id}                         /mints            /users/{user_id}
/types/{type_id}/issues                  /mints/{mint_id}  /users/{user_id}/collections
/types/{type_id}/issues/{issue_id}/prices  /catalogues     /users/{user_id}/collected_items
/coins  /coins/{coin_id}                 /search_by_image  /users/{user_id}/collected_items/{item_id}
/coins/{coin_id}/issues                  /publications/{id}  /users/{user_id}/collected_coins
/coins/{coin_id}/issues/{issue_id}/prices
```

`/coins*` and `/collected_coins` are deprecated v2-era duplicates of `/types*` and
`/collected_items`. Nothing here holds a coin the user does not own.

This settles the open question from [#279](https://github.com/jenarvaezg/coindex/issues/279),
which could not check it because `curl` got 403 from Cloudflare. `api.numista.com/v3/openapi.json`
answers **401** (it wants an API key), but `en.numista.com/api/doc/swagger.yaml` serves the whole
specification to a plain `curl` with a browser user-agent, no key and no challenge. The rest of
`en.numista.com` (`/echanges/`, `/help/`) still challenges `curl` and needs the browser.

`spec.md §0.5` lists three operations; the schema has nineteen. Nothing the app uses is missing
from `spec.md`, but it is not a survey of what Numista offers.

## 2. Named collections

They are real, first class, and reachable with the credentials the app already holds.

**In the API:**

- `GET /users/{user_id}/collections` → `{count, collections: [{id, name}]}`, scope
  `view_collection` — the scope `NumistaClient` already requests, so no new permission or consent.
  One call.
- `GET /users/{user_id}/collected_items?collection={id}` filters server-side, but is not needed:
  each row already carries its `collection` inline, so reading it costs no calls.
- The `collection` schema is exactly `{id, name}`; the colour and privacy setting of the web UI are
  not exposed.
- `collection` is **optional** on `collected_item` and absent (not null) when the user has never
  defined one. Neither capture (padre, Jose) has the field on any row.

**In the web UI** (help article): collections are defined under Settings → «My collections», each
with a name, a colour and a privacy setting. One is the **default**: pre-existing items are
assigned to it, and deleting a collection moves its items back to it. Collections filter the «My
coins» / «My banknotes» / «My exonumia» pages.

**In Coindex at the research date**, the field travelled all the way and nobody read it:

| Step | Where | State |
| --- | --- | --- |
| Parsed | `NumistaDtos.kt` (`collection: CollectionDto?`) | ✅ |
| Mapped | `Mappers.kt` (`collectionName = collection?.name`) | ✅ |
| Stored | `Entities.kt` (`val collectionName: String?`) | ✅ |
| In the domain | `Inventory.kt` (`CollectedItem.collectionName`) | ✅ |
| Read | — | ❌ nobody, in `app/src/main` or `domain/src/main` |

A named collection would reach the domain model with no new plumbing and no extra API call.

**The catch decides it.** Numista's collections organise *owned* items («if you want to keep your
ancient and modern coins separate, or if you keep some coins for someone else»). A collection
called «Deseos» smuggles a wish list into the inventory: a coin registered there is owned **on
numista.com too**. It counts in `item_count`, in the padre's public profile and in the swap
listings, and can be marked `for_swap`.

## 3. `for_swap`, by elimination

- Schema: `for_swap` is **required** on `collected_item` («Indicate whether the item is available
  for swap»); the collection response also carries `item_for_swap_count` and
  `item_type_for_swap_count`.
- Reality: `false` on every row of both captures. Neither collector uses swaps.
- In Coindex: same unread path as `collectionName` (`NumistaDtos.kt` → `Mappers.kt` →
  `Entities.kt` → `Inventory.kt`).

It says what you would give away, never what you are missing. If «what I have spare» is ever
wanted, it comes from `quantity > 1`, not from this field.

## 4. Every place a wish would pass for a piece

Wishes inside `collected_items` would arrive as pieces. **Nothing filters rows by collection**: the
only readers are `Daos.kt` `observeAll()` and `loadAll()`, both `SELECT * FROM collected_items
ORDER BY id`, and every screen and figure descends from that list through `Curation.assemble`
(`Curation.kt`), «the one door». One filter there covers most of this table:

| What breaks | Where | What the collector would see |
| --- | --- | --- |
| The plate cell fills | `CollectionCatalog.kt` `memberMatches` — checks `quantity > 0`, type, issue and year only | A casilla shown as owned for a coin never bought |
| The plate opens as yours | `CollectionCatalog.kt` `isEvidencedBy`, via `Curation.kt` and `CollectionIndex.kt` | A lámina with none of your pieces becomes navigable and gets a card |
| The album counts it | `CollectionCatalogAlbum.kt` | «14 de 20» when it is 13 |
| A card appears from nothing | `CollectionDerivation.kt` | A derived collection whose only evidence is a wish |
| The emission gets named | `CollectionCatalog.kt` `emissionLabelFor` | A wish labelled «Estrella 67» like a piece in hand |
| Pieces, types and issuers | `Figures.kt` `collectionFigures` | One more piece than the collector has |
| Weight and fine silver | `Figures.kt` `metalSplit` | Grams the collector does not hold |
| Year arc, size, margins | `Figures.kt` | The oldest coin is one the collector has not got |
| **Money** | `Valuation.kt` `pieceValue`, `collectionValue` | A wish with a catalogue price counted as owned value — worst of the list, since [#491](https://github.com/jenarvaezg/coindex/issues/491) prints «pagaste X y hoy valen Y» |
| **API budget** | `ValuationPlan.kt` | Calls spent pricing wishes *and* the holes of the plates they falsely evidence, every month |
| Boxes | `OwnGrouping.kt` | A bulto counting pieces that are not in it |
| The shelf | `PiecesSubject.kt`, `CountryAxis.kt`, `UnclaimedRows.kt` | Wishes drawn among the pieces and on the country map |
| **The printed notebook** | `NotebookSections.kt` | Paper, the one output that cannot be corrected later |

None of this misfires at the research date only because nobody has ever defined a Numista
collection, so the field is absent.

## What this leaves for the decision

Facts, not a recommendation; [#484](https://github.com/jenarvaezg/coindex/issues/484) decides.

1. **There is no measured fact to sync.** Numista does not record wanting, so «it arrives by sync
   and ADR 0021 §7 stays shut» is not available on its own terms.
2. **A named collection is available** at no extra API cost and no new plumbing, at the price of
   declaring on numista.com that the collector owns coins they do not.
3. **A local declarative** is the other road, and it amends ADR 0021 §7: it must be argued, not
   assumed.
4. **Either way, «a wish is not a piece» must be written down**, and §4 is the list it has to
   cover. One filter at `Curation.assemble` guards most of it; check `ValuationPlan` (spends quota)
   and the notebook (prints) separately.
