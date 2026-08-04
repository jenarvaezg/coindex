# Reaching Numista, and the traps that eat an afternoon

Disclosed reference for the [`curate-catalog`](../SKILL.md) skill. Every item here was **medido** on a real curation; the issue number says which one, so you can read the full story when a case does not fit.

## Two channels, two costs

**The web is free and needs a human.** numista.com returns 403 to automated requests (Cloudflare), so member listings come from a visible browser with the person driving the session passing the challenge by hand. Ask for it; do not try to defeat it.

**The API costs the collector's own allowance** — roughly 1.500-2.000 calls a month per key. State the endpoints and the number of calls before making any, and prefer what `data/numista-type-cache.json` already holds.

Once the challenge is passed, `fetch('/catalogue/pieces<id>.html')` **from inside the page** reads individual fichas without tripping Cloudflare again: dozens of types in one browser call, for free (#32).

`browser_evaluate` duplicates backslashes on the way in, so regular expressions arrive corrupted. Use plain string operations.

## Route 1: walk the series

```
catalogue/index.php?se=<series_id>&p=1&q=200
```

The ficha of any owned type links its `series.php?id=N`. Two things that URL does not promise:

- **It may be a technical family, not a series.** The 500 escudos cite `series.php?id=6598`, which is «System 1981-2001» with 141 coins inside (#27).
- **The API has no series operation** (#43), so this route is browser-only. That is also why the annual pass cannot be automated: in 23 of 28 open catalogs the new year arrives as a *new type*, and only a handful have a single trunk where `/types/{id}/issues` would answer cheaply (#94).

## Route 2: walk by weight

When no series groups the sequence, enumerate the physical variant directly:

```
catalogue/index.php?cat=y&st=all&e=<issuer>&w=<min>-<max>&q=500&p=1&o=y
```

**`st=all` is not optional.** Without it the search returns only ordinary circulation coins and silently omits commemoratives and non-circulating pieces — in #52 that was 15 results that looked like the whole catalog and were really 68.

The form's category checkboxes carry no `name`, and `k[]` is a different parameter: passing it empties the result.

## Route 3: walk a category

When the sequence is «all the circulating commemoratives of one issuer», the category filter
enumerates it without a series and without weights:

```
catalogue/index.php?cat=y&st=2&e=<issuer>&q=500&p=1&o=y
```

`st=2` is «Circulating commemorative coins» — read it off any ficha's own breadcrumb link rather
than guessing, since the form's category checkboxes carry no `name` (#157).

Three things that route does not tell you:

- **`q=500` is silently capped at 200 per page**, and a `p` beyond the real page count **serves
  page 1 again** instead of an empty result. Enumerating 5 pages of a 2-page list returned 912
  rows for 312 types. Deduplicate by id and stop when a page adds nothing new.
- **Results are grouped by currency**, so one issuer's list spans its whole monetary history: of
  312 Portuguese circulating commemoratives, 135 are the escudo and 336 rows were the euro.
  Walk the DOM in order, tracking the `h2` currency heading; the item links are bare `/<id>`.
- **A category is not a collection.** The 135 escudo types hold three already-curated catalogs and
  eleven named *Portuguese Discoveries* programmes. Enumerating the category tells you the
  denominator of the *search*, never the boundary of a plate.

## Route 4: the issues of one type

`GET /types/{id}/issues` is the source for a date run and for what a type actually emitted per year. It is the cheap route when one trunk type spans the whole programme.

## What Numista's silence does and does not prove

- **No series named does not mean no series exists.** Search the mint's own site before demoting a sequence to an agrupación: in #33 that reversed two verdicts of three — the Canadian silver dollar and the 10 gulden of Beatrix looked like our inventions and are ranges delimited by their mints.
- **A Numista series is not the programme either.** Closing a catalog means contrasting the list against the mint's real programme, not against the series that proposed it. Series 13245 lacked the two 1991 coins of the 500th anniversary, so the catalog claimed 4 of 4 over a programme of six (#44).
- **A mint that says «ongoing» can still have a closed variant.** The Royal Canadian Mint says it has struck a Proof Silver Dollar annually since 1971, and the .500 catalog still closes in 1991, because 1992 moved to sterling and that is a different variant key. A `closed_note` must say so in those words or it reads as if the series died (#52).

## Reading a ficha

- **The metal is the only physical property worth cross-checking.** Numista's grams do not agree with themselves — the 1000 escudos is one coin recorded as 27, 28 and 28,2 g — and its finish field does not exist, so `inferFinish` only reads the title (#62).
- **What sits in parentheses describes the finish, not the alloy**: «Plata 999 (highlighted in 24-carat gold)» is a silver coin. Read only the head of the composition phrase.
- **A type with no year at all is the offline trace of an unpublished draft** (#38). The API serves it with every field as its contributor typed it, so a half-written `series` arrives looking like a real family.

## When something should be in Numista and is not

Open an issue in this repo titled `Numista: <acción> (N#…)`, unlabelled, with the correction written and sourced — no need to ask first. The work lives outside the app (`spec.md §0.1`), so it is a plain issue, never a ticket of a wayfinder map.

The limit is «correct»: record what the source sustains. #52 is the asymmetry to remember — giving the .500 dollar a series is legitimate, and giving one to the .800 would be inventing it.

For preparing the contribution itself, hand off to the `numista-draft` skill.
