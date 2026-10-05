---
name: curate-catalog
description: Curate a coin sequence into a versioned Coindex collection catalog — «curar un catálogo», a new lámina, a date run. Use when new coins arrive and something needs cataloguing, when an open catalog is behind and needs this year's casilla, or when deciding whether a sequence becomes a catalog, an agrupación or a huérfana.
---

# Curate Catalog

A catalog gives a lámina its **denominador** («13 / 19 emisiones»). Numista has no denominator — a Numista family spans physical variants — so the number is an editorial claim, and this skill is how one is earned.

Every figure you report is **medido**: read off `data/`, off a Numista ficha you opened, or off the field report. The collector reads «me falta» as «this coin exists», so a plausible number nobody measured is the failure to avoid.

## Repository scope

Run only inside `jenarvaezg/coindex`: the root holds `CONTEXT.md`, `data/collection-catalogs/` and `scripts/seed-type-cache.py`. If those markers are absent, say this is a Coindex project skill and stop.

## Where truth already lives

Read these instead of reasoning from scratch, and keep their decisions there rather than copying them here:

- **What a catalog may claim** — `docs/adr/0020-what-a-collection-catalog-claims.md`: `series_status`, a member's `status`, the existence criterion, the denominator, the source, the printed face. Read it before your first curation.
- **Which shape a sequence takes** — ADR 0009 (date run), 0012 (technical families, catalog weights, sets), 0013 (agrupación), 0014 (issue run), 0016 (a catalog owns its members' variant), 0018 (dominant metal in the key), 0019 (members qualified by issue).
- **The words** — `CONTEXT.md`. Use its terms, not the synonyms it rejects.
- **Reaching Numista, and its traps** — [references/numista-research.md](references/numista-research.md). Read it completely before any browser or API work.

Fixing Numista itself is the `numista-draft` skill.

## Steps

### 1. Decide whether it is a collection

Three outcomes: a **catalog** with coverage, an **agrupación** when the boundary would be ours alone, or a **huérfana** when no lámina makes sense. ADR 0020 holds the criterion.

Ask the collector what the sources cannot answer:

- **Intention** — does either collection pursue this sequence? A sequence that exists without us and that nobody chases is a correct huérfana.
- **Their own reading** — the curator's criterion outranks any physical property. Metals and weights *propose* collections; they never exclude a coin from one.

Check what the collector says outside Numista too — the mint's site, the monetary law, the market. The collector is usually right; when a source disagrees, suspect our reading first.

**Done when** the outcome is one of the three, written down with its reason. For a huérfana, record the verdict in `data/orphans.json` and stop.

### 2. Enumerate the members

Follow [references/numista-research.md](references/numista-research.md) for the enumeration routes and traps. The file's shape follows from what you find; a type repeated in two casillas is only legal in a date run.

**Done when** every casilla is listed with its year and its Numista type, and you can name the source that sustains the boundary at each end.

### 3. Verify every id

Open each `numista_type_id` on numista.com and confirm it one by one (`GET /types/{id}` is the cheap way). Third-party listings carry subtle errors: swapped years, a cupronickel piece that reads as silver, a gold twentieth-ounce inside a silver ounce catalog.

A type is **verificado** only if it is *published*. The API also serves a referee's pending draft, with every field as its contributor left it, and a referee may delete the page and its id. A coin whose ficha is still pending becomes an `unlisted` member with no id.

**Done when** every id in the file has been opened and confirmed, and each unpublished one is `unlisted` with an upstream issue titled `Numista: <acción> (N#…)`, unlabelled.

### 4. Cross the ids against everything already versioned

Before writing the file, check its types against the types the rest of `data/` already names. No network:

```
scripts/type-claims.py <id> <id> …        # `235118:582778,585569` adds the issue qualifier
scripts/type-claims.py --file <ruta>      # a file already written, before versioning it
scripts/type-claims.py --all              # every overlap in data/
```

Treat `PARAR` as a stop. Two ordinary catalogs naming one type make the app refuse to start — `CatalogSeeds.validateCrossCatalogClaims`, «Numista type `X` is claimed by more than one collection catalog without issue-qualified identities» — and the file ships to the father's phone in the release.

One overlap is legitimate: two catalogs may share a type when **both** identities are issue-qualified and their `numista_issue_ids` are disjoint. `lunar-iii-perth-1oz-bullion` and `lunar-iii-perth-1oz-proof-coloured` do this over `235118`, `307024` and `342221`, and the two Rwanda Nautical files over four more: years where Numista files two finishes under one type.

Other shapes have no such exit, and startup does not catch them: an agrupación loses the family to any catalog naming the type (ADR 0013), and a set catalog wins it and takes the coin *off* the denomination card (ADR 0022). Both are curation mistakes. A commemorative programme may share types on purpose: it produces no card and never reaches `deriveCollection`.

Any other collision usually means the shape is wrong: a type repeated across casillas belongs inside one date run. A genuine overlap — one coin on two plates — first needs the domain change [#149](https://github.com/jenarvaezg/coindex/issues/149) postponed until a real case; this curation waits for it.

**Done when** the cross-check is clean, or the only overlap is the issue-qualified disjoint kind and you have said which emissions each file takes.

### 5. Choose the face the lámina prints

Look at both faces of every casilla — the cache describes them, the ficha shows them — and declare in the header the one the collector recognises as the coin. `printed_side: "obverse"` for the anverso; omitting the field prints the reverso. ADR 0020 holds the criterion and why it is never deduced from the descriptions.

The face is a claim about the **whole lámina**. If one coin wants a face its sisters do not, change the whole lámina or accept the mismatch, and write the reason into `source_note` either way, never as a per-member override.

An undeclared face silently prints Numista's reverso, which is sometimes a coat of arms where the coin is a mermaid.

**Done when** the header declares a face, or you have said why the reverso is right for this lámina.

### 6. Version the file, and seed the cache

Write the JSON under `data/collection-catalogs/` (or `data/groupings/`). Then seed the fichas of every new type: `TypeCacheSeedTest` goes red with the list of missing ones, and the script seeds the ids you pass:

```
python3 scripts/seed-type-cache.py --dry-run <id> <id> …    # says the API cost first
```

Seeding is part of curating: the lámina draws what the collector is *missing*, so a hole only shows on a phone that does **not** have the coin.

Before renaming anything, say what it costs: the family is part of the primary key of `collection_proposal_preferences`, so renaming one erases the intention saved on that card, and widening the variant key erases every disposition that is not a catalog's.

**Done when** `./gradlew :domain:test :app:testDebugUnitTest` is green — it runs the startup validator over the real files, the seed check and the cross-file ambiguity check.

### 7. Measure the lámina

Report the real fraction for both collections from the field report over a private capture:

```
COINDEX_FIELD_SNAPSHOT=<dir> ./gradlew :app:testDebugUnitTest --tests '*FieldReportTest*' --rerun
```

Always pass `--rerun`: the variable is not a declared task input, so without it you may read one collection's report as the other's. Captures live outside the tree and per-piece listings stay unpublished: the repo is public and the inventories are private.

**Done when** the plate fraction and the change in «Sin clasificar» are **medido** for both collections, or you have said that no capture was available and the figures are unmeasured.

### 8. Update the documents the curation changed

Update `CONTEXT.md` when a term is new, `spec.md` when a section it describes is now false, and write a new ADR when you had to *decide* rather than look up. Say which of the three you touched and why the others needed nothing.

Most curations touch none. `spec.md` holds rules, not a census: which catalogs exist, how many, and which was first of its class are read from `data/`, so a new plate alone is no reason to edit it. Use its index and read only the section you need.

## Branch: an open catalog that is behind

`scripts/stale-catalogs.py` and the [Catálogos abiertos por detrás](https://github.com/jenarvaezg/coindex/issues/136) issue name the tail. Adding this year's casilla is steps 3 through 8 only; the sequence already passed step 1, so do not reopen it with the collector.

A year missing in the middle is usually a year the mint did not strike: confirm it outside Numista and record it as calendar, not curation debt.

## Before sitting down: what the weights disagree about

```
python3 scripts/weight-deviations.py
```

No network, never red. It lists members whose ficha weighs something other than their catalog declares, unclaimed types whose weight the magnet moves, and the cards the file silently avoids (ADR 0016), and syncs to the [Desviaciones de peso](https://github.com/jenarvaezg/coindex/issues/158) issue. A line means «look at it», not «fix it»: usually Numista varying its grams, sometimes the fineness, occasionally a coin that does not belong on that lámina. Record a deliberate deviation in the file as a `variant_note`.

The work-list is the first section, **«Sin mirar»**: lines whose explanation is written in no file. The rest are grouped into cúmulos because a curator already looked at them. Start by running the `--refresh` command the report prints for that section: `data/` never refreshes itself, so a correction Numista already **accepted** keeps showing until the ficha is reseeded.

## Guardrails

- A physical check against a Numista ficha lives in the test suite and stays silenceable in prose. A catalog declares the variant of the *collection*, not an assertion about each member, so a curator's judgment must never be able to halt the app.
- Version what the source sustains. Where it sustains nothing, use an `unlisted` member, a huérfana verdict or an open question — never a plausible id.
- Numista's web pages need a visible browser with a human passing the challenge. Budget every API call before making it: each key has roughly 1.500-2.000 a month, and exploratory calls spend the collector's own allowance.
