# Reaching Numista, and the traps that eat an afternoon

Disclosed reference for the [`curate-catalog`](../SKILL.md) skill. Every item here was **medido** on a real curation; the issue number says which one, so you can read the full story when a case does not fit.

## Two channels, two costs

**The web is free and needs a human.** numista.com returns 403 to automated requests (Cloudflare), so member listings come from a visible browser with the person driving the session passing the challenge by hand. Ask for it; do not try to defeat it.

**The API costs the collector's own allowance** — roughly 1.500-2.000 calls a month per key. State the endpoints and the number of calls before making any, and prefer what `data/numista-type-cache.json` already holds.

Once the challenge is passed, `fetch('/catalogue/pieces<id>.html')` **from inside the page** reads individual fichas without tripping Cloudflare again: dozens of types in one browser call, for free (#32).

**A blank challenge page is our own configuration, not Cloudflare.** The challenge is a reCAPTCHA, and its script comes from `recaptcha.net` — a host the browser's `--allowed-origins` in `.mcp.json` has to list, alongside `www.gstatic.com` and `www.google.com`. Without them `challenge.php` paints the Numista header over an empty page with nothing to click, and the console says `ERR_BLOCKED_BY_CLIENT` on `recaptcha.net/recaptcha/api.js` (#259). Read the console log before asking the human to pass a challenge that has not loaded. The same block is what refuses any non-Numista host, so a mint's own site is reached with WebFetch rather than through this browser.

`browser_evaluate` duplicates backslashes on the way in, so regular expressions arrive corrupted. Use plain string operations.

## Route 0: the siblings already in the cache

Before opening anything, read `related_types` off `data/numista-type-cache.json` (or the `raw` of a
`type_meta` row in an export). Numista lists there the types that share a design or a family, and for
a series with one motif per year that is most of the enumeration — free, offline, and it tells you
which series id to walk next.

Measured on 1 September 2026 curating the Portuguese 5 euros: `related_types` of the father's three
UK silver-proof pounds named five siblings and split them across two series ids, `Royal Diadem` and
`Heraldic Emblems`, which is the boundary question the whole curation turned on — before a single
browser call. It is not the enumeration: it under-counts (the field is a curator's cross-reference,
not the series), and it is `null` on plenty of types, N#15486 among them. Use it to aim, then walk
the series to close the denominator.

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

**The date filter silently empties the result too.** `d=` and `f=` on this listing return zero rows
rather than a filtered page, so a curator who trusts them reads «this year has nothing» about a
year that has plenty — in #258 that would have declared the 5 euros of the 2026 World Cup
unpublished, when it is published and indexed by weight and by text. Filter by weight or by `r=`
free text and read the year off each row.

**One Numista issuer can hold two programmes at the identical physical standard.** The ECU of the
FNMT and the ECU of the Generalitat de Catalunya both file under «España» — «emisor supuesto», the
tell that nothing official is being claimed — and the Catalan 1 ECU of 1993 is the same .925 silver,
the same 6,72 g and the same 24 mm as Madrid's. Weight cannot separate them and the year would have
made it look like a filled slot. What separates them is the coin's own legend and its Krause number:
«ESPAÑA … M» with the crowned Madrid mintmark against «CATALUNYA», twelve stars and the `dM` mark,
X# M17/M18 against X# M9/M24 (#258). Read the legend before you write a year.

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
- **Never filter the listing by the denomination as a string.** Titles are contributor prose and
  do not agree with themselves: ten Portuguese types read «8 Euros» and the eleventh, N#12585,
  reads «8 Euro» in the singular. A `grep` of the plural dropped it, and the denominator would
  have shipped as 10 of 11 (#187). Filter on the row's metal and weight, which the listing prints
  for every hit, and read the title only as a label.

## Route 4: the issues of one type

`GET /types/{id}/issues` is the source for a date run and for what a type actually emitted per year. It is the cheap route when one trunk type spans the whole programme.

## What Numista's silence does and does not prove

- **No series named does not mean no series exists.** Search the mint's own site before demoting a sequence to an agrupación: in #33 that reversed two verdicts of three — the Canadian silver dollar and the 10 gulden of Beatrix looked like our inventions and are ranges delimited by their mints.
- **A Numista series is not the programme either.** Closing a catalog means contrasting the list against the mint's real programme, not against the series that proposed it. Series 13245 lacked the two 1991 coins of the 500th anniversary, so the catalog claimed 4 of 4 over a programme of six (#44).
- **A mint that says «ongoing» can still have a closed variant.** The Royal Canadian Mint says it has struck a Proof Silver Dollar annually since 1971, and the .500 catalog still closes in 1991, because 1992 moved to sterling and that is a different variant key. A `closed_note` must say so in those words or it reads as if the series died (#52).

## Reading a ficha

- **The metal is the only physical property worth cross-checking.** Numista's grams do not agree with themselves — and for Portugal's 1000 escudos .500 neither does the law: thirteen decretos say 27 g and six say 28 g (#287) — and its finish field does not exist, so `inferFinish` only reads the title (#62).
- **A weight that matches no law is not proof of an intruder.** Of the three off-standard grams in the 104 3-rouble Architectural Monuments, two were a bailed digit in Numista (39,94 for 33,94) and the third, 35,66 g, is the mint's own figure for a coin carrying a 1,55 g gold inlay — same 31,10 g of fine silver, same plate. Read the *fine* content off the ficha's own edge lettering and off the mint, then decide; the gross gram alone separates nothing (#160).

For a Russian coin the mint answers for free and without a challenge, one page per catalogue number, which is the cheapest primary source in the project:

```
cbr.ru/cash_circulation/memorable_coins/coins_base/ShowCoins/?cat_num=5111-0357
cbr.ru/cash_circulation/memorable_coins/coins_base/?serie_id=102&year=2009    # to find the number
```

`serie_id=102` is «Памятники архитектуры России». The page prints the tolerance too — 33,94 g (±0,31) — and a bimetal coin carries its own prefix, so the Voronezh piece is 5611-0004 while its silver siblings are 5111-….
- **What sits in parentheses describes the finish, not the alloy**: «Plata 999 (highlighted in 24-carat gold)» is a silver coin. Read only the head of the composition phrase.
- **A type with no year at all is the offline trace of an unpublished draft** (#38). The API serves it with every field as its contributor typed it, so a half-written `series` arrives looking like a real family.

## When something should be in Numista and is not

Open an issue in this repo titled `Numista: <acción> (N#…)`, unlabelled, with the correction written and sourced — no need to ask first. The work lives outside the app (`spec.md §0.1`), so it is a plain issue, never a ticket of a wayfinder map.

The limit is «correct»: record what the source sustains. #52 is the asymmetry to remember — giving the .500 dollar a series is legitimate, and giving one to the .800 would be inventing it.

For preparing the contribution itself, hand off to the `numista-draft` skill.
