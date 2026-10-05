# Reaching Numista, and its traps

Reference for the [`curate-catalog`](../SKILL.md) skill. Each item was measured on a real curation; the issue number holds the full story for a case that does not fit.

## Two channels, two costs

- **The web is free and needs a human.** numista.com returns 403 to automated requests (Cloudflare), so member listings come from a visible browser where the person driving it passes the challenge by hand. Ask for that; never try to defeat it.
- **The API spends the collector's own allowance** — roughly 1.500-2.000 calls a month per key. State the endpoints and the number of calls before making any, and prefer what `data/numista-type-cache.json` already holds.
- Once the challenge is passed, `fetch('/catalogue/pieces<id>.html')` **from inside the page** reads fichas without tripping Cloudflare again: dozens of types in one browser call, for free (#32).
- **A blank challenge page is our configuration, not Cloudflare.** The challenge is a reCAPTCHA loaded from `recaptcha.net`, which the browser's `--allowed-origins` in `.mcp.json` must list alongside `www.gstatic.com` and `www.google.com`. Without them `challenge.php` shows the Numista header over an empty page and the console logs `ERR_BLOCKED_BY_CLIENT` on `recaptcha.net/recaptcha/api.js` (#259). Read the console before asking the human to pass a challenge that never loaded. The same allow-list blocks every non-Numista host, so reach a mint's site with WebFetch instead.
- `browser_evaluate` doubles backslashes on the way in, so regular expressions arrive corrupted. Use plain string operations.

## Route 0: the siblings already in the cache

Before opening anything, read `related_types` from `data/numista-type-cache.json` (or the `raw` of a `type_meta` row in an export). Numista lists there the types that share a design or a family; for a series with one motif per year that is most of the enumeration, offline, and it tells you which series id to walk next. On the father's three UK silver-proof pounds it named five siblings split across two series ids, `Royal Diadem` and `Heraldic Emblems` — the boundary question of that curation — before any browser call (#563).

It under-counts (the field is a curator's cross-reference, not the series) and is `null` on many types, N#15486 among them. Use it to aim, then walk the series to close the denominator.

## Route 1: walk the series

```
catalogue/index.php?se=<series_id>&p=1&q=200
```

The ficha of any owned type links its `series.php?id=N`. Caveats:

- **It may be a technical family, not a series.** The 500 escudos cite `series.php?id=6598`, «System 1981-2001», with 141 coins inside (#27).
- **The API has no series operation** (#43), so this route is browser-only.
- **One plate can span many series, and walking the ones you know will not find the rest.** The UK round pound in silver proof sits in eight: *Heraldic Emblems*, *Royal Diadem*, *Regional Bridge*, *UK Cities*, *Floral Emblems*, two anniversary series and *Final edition of the round pound coin*. The two series the owned coins cited gave 18 casillas; enumerating the **physical variant** by weight gave 47 types and 32 casillas, including the 2016 final edition that closes the plate. When the sequence is a denomination in one metal, use Route 2 and treat a series as a label on a row (#567).
- The same is why the annual pass cannot be automated: in most open catalogs the new year arrives as a *new type*, and only a handful have a single trunk where `/types/{id}/issues` answers cheaply (#94).

## Route 2: walk by weight

When no series groups the sequence, enumerate the physical variant directly:

```
catalogue/index.php?cat=y&st=all&e=<issuer>&w=<min>-<max>&q=500&p=1&o=y
```

- **Always pass `st=all`.** Without it the search returns only ordinary circulation coins and silently drops commemoratives and non-circulating pieces: in #52, 15 results that looked like the whole catalog were really 68.
- The form's category checkboxes carry no `name`, and `k[]` is a different parameter: passing it empties the result.
- **The date filters `d=` and `f=` silently return zero rows** instead of a filtered page. Trusting them would have declared the 5 euros of the 2026 World Cup unpublished, when it is published and indexed by weight and by text (#258). Filter by weight or by `r=` free text and read the year off each row.
- **One Numista issuer can hold two programmes at the same physical standard.** The ECU of the FNMT and the ECU of the Generalitat de Catalunya both file under «España» — «emisor supuesto», the sign that nothing official is claimed — and the Catalan 1 ECU of 1993 has Madrid's .925 silver, 6,72 g and 24 mm. Weight cannot separate them, and the year would make it look like a filled slot. The legend and the Krause number do: «ESPAÑA … M» with the crowned Madrid mintmark against «CATALUNYA», twelve stars and the `dM` mark; X# M17/M18 against X# M9/M24 (#258). Read the legend before you write a year.

## Route 3: walk a category

For «all the circulating commemoratives of one issuer», the category filter enumerates without a series and without weights:

```
catalogue/index.php?cat=y&st=2&e=<issuer>&q=500&p=1&o=y
```

`st=2` is «Circulating commemorative coins»; read the value off a ficha's own breadcrumb link rather than guessing, since the form's checkboxes carry no `name` (#157).

- **`q=500` is silently capped at 200 per page**, and a `p` beyond the last page **serves page 1 again** instead of an empty result: five pages of a two-page list returned 912 rows for 312 types. Deduplicate by id and stop when a page adds nothing new.
- **Results are grouped by currency**, so one issuer's list spans its whole monetary history (Portugal's mixes escudo and euro). Walk the DOM in order, tracking the `h2` currency heading; item links are bare `/<id>`.
- **A category is not a collection.** Portugal's 135 escudo commemoratives hold three curated catalogs and eleven named *Portuguese Discoveries* programmes. The category gives the denominator of the *search*, never the boundary of a plate.
- **Never filter the listing by the denomination as a string.** Titles are contributor prose: ten Portuguese types read «8 Euros» and the eleventh, N#12585, reads «8 Euro». A `grep` of the plural dropped it, and the denominator would have shipped as 10 of 11 (#187). Filter on the metal and weight each row prints, and read the title only as a label.

## Route 4: the issues of one type

`GET /types/{id}/issues` is the source for a date run and for what a type emitted per year. It is the cheap route when one trunk type spans the whole programme.

## What Numista's silence does not prove

- **No series named does not mean no series exists.** Search the mint's own site before demoting a sequence to an agrupación: in #33 that reversed two verdicts of three — the Canadian silver dollar and the 10 gulden of Beatrix are ranges their mints delimit.
- **A Numista series is not the programme either.** Close a catalog against the mint's real programme, not the series that proposed it. Series 13245 lacked the two 1991 coins of the 500th anniversary, so the catalog claimed 4 of 4 over a programme of six (#44).
- **An «ongoing» programme can contain a closed variant.** The Royal Canadian Mint has struck a Proof Silver Dollar every year since 1971, and the .500 catalog still closes in 1991, because 1992 moved to sterling and that is a different variant key. The `closed_note` must say so in those words, or it reads as if the series ended (#52).

## Reading a ficha

- **A finish is declared wherever the contributor chose, and the prose misleads.** The 14 gold-plated UK pounds of the 2008 anniversary set say «selected gold plating to the reverse» only in `Comments`; the 2012 Diamond Jubilee one says it only in its reverse description. Ten plain silver proofs *mention* the 2008 gilded edition in their own comments, so grepping comments for «gold plat» gave ten false positives and one false negative (#567). Read the field that holds the fact, never a sentence about another coin. `inferFinish` reads only the title (#62), where all fifteen say «Silver Proof».
- **The metal is the only physical property worth cross-checking.** Numista's grams disagree with themselves — and for Portugal's 1000 escudos .500 so does the law: thirteen decretos say 27 g and six say 28 g (#287) — and there is no finish field, so `inferFinish` reads the title (#62).
- **A weight that matches no law does not prove an intruder.** Of three off-standard weights among the 3-rouble Architectural Monuments, two were a mistyped digit in Numista (39,94 for 33,94) and the third, 35,66 g, is the mint's own figure for a coin with a 1,55 g gold inlay: same 31,10 g of fine silver, same plate. Read the *fine* content off the ficha's edge lettering and off the mint before deciding; the gross weight alone separates nothing (#160).
- **Parentheses describe the finish, not the alloy**: «Plata 999 (highlighted in 24-carat gold)» is a silver coin. Read only the head of the composition phrase.
- **A type with no year at all is the offline trace of an unpublished draft** (#38). The API serves it with every field as its contributor typed it, so a half-written `series` looks like a real family.

For a Russian coin the Bank of Russia answers for free and without a challenge, one page per catalogue number — the cheapest primary source in the project:

```
cbr.ru/cash_circulation/memorable_coins/coins_base/ShowCoins/?cat_num=5111-0357
cbr.ru/cash_circulation/memorable_coins/coins_base/?serie_id=102&year=2009    # to find the number
```

`serie_id=102` is «Памятники архитектуры России». The page prints the tolerance too — 33,94 g (±0,31) — and a bimetal coin has its own prefix: the Voronezh piece is 5611-0004, its silver siblings 5111-….

## When something should be in Numista and is not

Open an issue in this repo titled `Numista: <acción> (N#…)`, unlabelled, with the correction written and sourced; no need to ask first. The work lives outside the app (`spec.md §0.1`), so it is a plain issue, never a wayfinder ticket.

Record only what the source sustains. #52 is the asymmetry to remember: giving the .500 dollar a series is legitimate, and giving one to the .800 would be inventing it.

To prepare the contribution itself, hand off to the `numista-draft` skill.
