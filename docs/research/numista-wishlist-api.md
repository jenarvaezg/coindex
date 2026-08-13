# Numista API: what it records about coins you do not own

- Research date: 2026-08-13
- Official API specification reviewed: **v3.33**, fetched from
  `https://en.numista.com/api/doc/swagger.yaml?v=3.33` (the machine-readable source behind
  `https://en.numista.com/api/doc/`, which is a Redoc page and renders nothing without JavaScript)
- Help centre reviewed: `https://en.numista.com/help/index.php`, article
  «How can I set multiple collections?» (last updated 7 April 2023)
- Scope: read-only research. No authenticated request was sent and no API quota was spent — the
  specification and the help centre are public. Collection figures come from the captures already
  in `.local/`, not from the network.
- Opened by [#483](https://github.com/jenarvaezg/coindex/issues/483); the decision it feeds is
  [#484](https://github.com/jenarvaezg/coindex/issues/484).

## Conclusion

**Numista has no wish list, and the API cannot be made to carry one.** The words *wish*, *want* and
*desire* do not occur once in the 169 KB specification, and the documentation centre has no article
about wanting a coin. Every personal-data endpoint is worded around ownership:
`/users/{user_id}/collected_items` is documented as «Get the items **owned** by a user», and its
counters are `item_count` («Count of items **owned** by the user») and `item_for_swap_count`.

The one adjacent concept, `for_swap`, is the mirror image of a wish: it marks what you **have** and
will part with. Numista's swap model has no counterpart for what you are looking for — a partner is
found by browsing what others offer, not by publishing what you lack.

So the elegant path the [#484](https://github.com/jenarvaezg/coindex/issues/484) hoped for — a wish
that arrives as a measured fact through sync, leaving ADR 0021 §7 untouched — **exists mechanically
and lies semantically**. It is available only by telling Numista you own a coin you do not.

## 1. Is there a wish list, and does v3 expose it?

No, on both counts. The complete list of paths in v3.33:

```
/types                                   /issuers          /oauth_token
/types/{type_id}                         /mints            /users/{user_id}
/types/{type_id}/issues                  /mints/{mint_id}  /users/{user_id}/collections
/types/{type_id}/issues/{issue_id}/prices  /catalogues     /users/{user_id}/collected_items
/coins  /coins/{coin_id}                 /search_by_image  /users/{user_id}/collected_items/{item_id}
/coins/{coin_id}/issues                  /publications/{id}  /users/{user_id}/collected_coins
/coins/{coin_id}/issues/{issue_id}/prices
```

`/coins*` and `/collected_coins` are the deprecated v2-era duplicates of the `/types*` and
`/collected_items` pairs. Nothing here holds a coin the user does not own.

This retires the open question from [#279](https://github.com/jenarvaezg/coindex/issues/279), which
could not check it: `curl` was getting 403 from Cloudflare. It still does on
`api.numista.com/v3/openapi.json` — that path answers **401**, not 403, because it wants an API key —
but `en.numista.com/api/doc/swagger.yaml` serves the whole specification to a plain `curl` with a
browser user-agent, no key and no challenge. The rest of `en.numista.com` (`/echanges/`, `/help/`)
does throw the Cloudflare challenge at `curl` and needs the browser of
`numista-navegador-para-scrapear`.

`spec.md §0.5` lists three operations; the real schema has nineteen. Nothing this app uses is
missing from `spec.md`, but it should not be read as a survey of what Numista offers.

## 2. Named collections

They are real, first class, and already reachable with the credentials the app holds.

**In the API:**

- `GET /users/{user_id}/collections` → `{count, collections: [{id, name}]}`. Scope
  `view_collection` — **the same scope `NumistaClient` already requests** (`NumistaClient.kt:53-54`),
  so this needs no new permission and no second consent. Cost: **one call**.
- `GET /users/{user_id}/collected_items?collection={id}` filters server-side. Also one call — but
  it is not needed to tell wishes apart, because each row already carries its own `collection`
  object inline. Reading that field costs **zero calls**.
- The `collection` schema is exactly `{id, name}`. The colour and the privacy setting the web UI
  offers are **not** exposed.
- `collection` is **optional** on `collected_item`, and absent — not null — when the user has never
  defined one. Both captures confirm it: the field does not appear among the row keys at all in the
  padre's 229 rows or Jose's 66.

**In the web UI** (help article 3): collections are defined under Settings → «My collections», each
with a name, a colour and a privacy setting. One is the **default**; every pre-existing item is
assigned to it, and deleting a collection reassigns its items back to the default rather than
removing them. They filter the «My coins» / «My banknotes» / «My exonumia» pages.

**In Coindex today**, the path is built end to end and dead at the tip:

| Step | Where | State |
| --- | --- | --- |
| Parsed | `NumistaDtos.kt:35` (`collection: CollectionDto?`) | ✅ |
| Mapped | `Mappers.kt:200` (`collectionName = collection?.name`) | ✅ |
| Stored | `Entities.kt:26` (`val collectionName: String?`) | ✅ |
| In the domain | `Inventory.kt:15` (`CollectedItem.collectionName`) | ✅ |
| Read by anyone | — | ❌ **nobody**, in `app/src/main` or `domain/src/main` |

A single named collection would therefore travel from Numista to the domain model without one line
of new plumbing and without one extra API call.

**The catch, and it is the whole decision.** Numista's collections organise *owned* items — «if you
want to keep your ancient and modern coins separate, or if you keep some coins for someone else».
A collection called «Deseos» is not a wish list Numista offers; it is a wish list smuggled into the
inventory. Registering a coin there makes it owned **on numista.com too**: it counts in
`item_count`, in the padre's public profile and in the swap listings, and it can be marked
`for_swap` like anything else. The lie is not confined to the phone.

## 3. `for_swap`, by elimination

- Schema: `for_swap` is **required** on `collected_item` — «Indicate whether the item is available
  for swap». The collection response also carries `item_for_swap_count` and
  `item_type_for_swap_count`.
- Reality: `false` in **all 229 rows of the padre and all 66 of Jose**. Neither uses the swap system.
- In Coindex: same dead-tip shape as `collectionName` — `NumistaDtos.kt:34` → `Mappers.kt:199` →
  `Entities.kt:25` → `Inventory.kt:14`, and **no reader**.

It is the wrong end of the telescope regardless: it says what you would give away, never what you
are missing. The map's standing note holds — if «what I have spare» is ever wanted, it comes from
`quantity > 1` (40 of the padre's rows), not from this field.

## 4. The trap, measured: every place a wish would pass for a piece

If wishes arrive inside `collected_items`, they arrive as pieces. **Nothing filters rows by
collection anywhere**: the only two readers are
`Daos.kt:14` `observeAll()` and `Daos.kt:17` `loadAll()`, both `SELECT * FROM collected_items ORDER
BY id`, and every screen and figure descends from that one list through a single funnel —
`Curation.assemble` (`Curation.kt:80`), described in its own doc comment as «the one door».

That funnel is good news for the fix: **one filter, correctly placed, covers most of this table.**
The list is what it would cost to get it wrong.

| What breaks | Where | What the collector would see |
| --- | --- | --- |
| The plate cell fills | `CollectionCatalog.kt:139` `memberMatches` — checks `quantity > 0`, type, issue and year, nothing else | A casilla shown as owned for a coin never bought |
| The plate opens as yours | `CollectionCatalog.kt:171` `isEvidencedBy`, via `Curation.kt:92` and `CollectionIndex.kt:185` | A lámina with no piece of yours in it becomes navigable, and gets a card |
| The album counts it | `CollectionCatalogAlbum.kt:117` | «14 de 20» when it is 13 |
| A card appears out of nowhere | `CollectionDerivation.kt:130` | A derived collection whose only evidence is a wish |
| The emission gets named | `CollectionCatalog.kt:160` `emissionLabelFor` | A wish labelled «Estrella 67» like a piece in hand |
| Pieces, types and issuers | `Figures.kt:259` `collectionFigures` | 574 pieces becomes 575 |
| Weight and fine silver | `Figures.kt:63` `metalSplit`, `Figures.kt:310` | Grams you do not hold added to the 6,91 kg |
| Year arc, size, margins | `Figures.kt:122`, `:143`, `:189` | The oldest coin in the collection is one you have not got |
| **Money** | `Valuation.kt:94` `pieceValue`, `:164` `collectionValue` | A wish with a catalogue price is euros you do not own — the worst of the list, because [#491](https://github.com/jenarvaezg/coindex/issues/491) is about to print «pagaste X y hoy valen Y» |
| **API budget** | `ValuationPlan.kt:78-88` | Calls spent pricing wishes *and* the holes of the plates they falsely evidence — a wish would cost quota every month |
| Boxes | `OwnGrouping.kt:41` | A bulto counting pieces that are not in it |
| The shelf | `PiecesSubject.kt:125`, `CountryAxis.kt:151,176`, `UnclaimedRows.kt:104` | Wishes drawn among the pieces, and on the country map |
| **The printed notebook** | `NotebookSections.kt:55,178` | Paper, which is the one output that cannot be corrected later |

`for_swap` and `collectionName` reaching the domain unread (§2, §3) is the reason none of this
misfires **today**: no user of either app has ever defined a collection, so the field is absent and
the question has never been asked.

## What this leaves for the decision

Facts, not a recommendation — [#484](https://github.com/jenarvaezg/coindex/issues/484) decides.

1. **There is no measured fact to sync.** Numista does not record wanting. The «it arrives by sync
   and ADR 0021 §7 stays shut» option is not available on its own terms.
2. **What is available is a named collection**, at zero extra API cost and zero new plumbing — at
   the price of declaring ownership on numista.com of coins the collector does not have.
3. **A local declarative** remains the other road, and it is an amendment to ADR 0021 §7 that must
   be argued, not assumed.
4. **Either way, the rule «a wish is not a piece» has to be written**, and §4 is the exact list it
   has to cover. One filter at `Curation.assemble` guards most of it; `ValuationPlan` and the
   notebook are worth checking separately, because one spends quota and the other prints.
