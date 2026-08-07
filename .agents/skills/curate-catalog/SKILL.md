---
name: curate-catalog
description: Curate a coin sequence into a versioned Coindex collection catalog — «curar un catálogo», a new lámina, a date run. Use when new coins arrive and something needs cataloguing, when an open catalog is behind and needs this year's casilla, or when deciding whether a sequence becomes a catalog, an agrupación or a huérfana.
---

# Curate Catalog

A catalog exists to give a lámina its **denominador**: «13 / 19 emisiones». Numista cannot supply that number — a Numista family spans physical variants and has no denominator at all — so the number is an editorial claim, and this skill is how one is earned.

Every figure you report is **medido**: read off `data/`, off a Numista ficha you opened, or off the field report. A plausible number nobody measured is the one failure this project cannot afford, because the collector trusts «me falta» to mean a coin exists.

## Repository scope

Run only inside the `jenarvaezg/coindex` repository. Confirm the root holds `CONTEXT.md`, `data/collection-catalogs/` and `scripts/seed-type-cache.py` before starting. If those markers are absent, say this is a Coindex project skill and stop.

## Where truth already lives

Read these rather than reasoning from scratch, and never restate them here — a decision copied into two places drifts:

- **What a catalog may claim** — `docs/adr/0020-what-a-collection-catalog-claims.md`: `series_status`, the `status` of a member, the existence criterion, the denominator, the source, the printed face. Read it before your first curation.
- **Which shape a sequence takes** — ADR 0009 (date run), 0012 (technical families, catalog weights, sets), 0013 (agrupación), 0014 (issue run), 0016 (a catalog owns its members' variant), 0018 (dominant metal in the key), 0019 (members qualified by issue).
- **The words** — `CONTEXT.md`. Use its terms and avoid the synonyms it rejects.
- **How to reach Numista, and the traps that eat an afternoon** — [references/numista-research.md](references/numista-research.md). Read it completely before any browser or API work.

Fixing Numista itself is a different job: hand off to the `numista-draft` skill.

## Steps

### 1. Establish that this is a collection at all

Three outcomes, not two: a **catalog** with coverage, an **agrupación** when the boundary would be ours alone, or a **huérfana** when no lámina would make sense. ADR 0020 holds the criterion.

Two questions the sources cannot answer, so ask the person who collects:

- **Intention** — does either collection pursue this sequence? A sequence that exists without us and that nobody is chasing is a correct huérfana.
- **The collector's own reading** — the curator's criterion outranks any physical property. Metals and weights *propose* collections; they never exclude a coin from one.

Contrast whatever the collector says outside Numista too — the mint's own site, the monetary law, the market. He is usually right and we are usually the ones who got it wrong.

**Done when** the outcome is one of the three, in writing, with the reason. If it is huérfana, record the verdict in `data/orphans.json` and stop here.

### 2. Enumerate the members

Follow [references/numista-research.md](references/numista-research.md). It carries the enumeration routes and the traps; the shape of the file follows from what you find, and the editorial rule chooses it for you — a type repeated in two casillas is only legal in a date run.

**Done when** every casilla of the sequence is listed with its year and its Numista type, and you can say which source sustains the boundary at each end.

### 3. Verify every id

Open each `numista_type_id` against numista.com and confirm it one by one, `GET /types/{id}` being the cheap way. Third-party listings carry subtle errors: swapped years, a cupronickel piece that reads as silver, a gold twentieth-ounce sitting inside a silver ounce catalog.

A type is **verificado** only if it is *published*. The API serves a referee's pending draft with every field as its contributor left it, and a referee may delete the page and take the id with it. A published type goes in the file; a coin whose ficha is still in flight becomes an `unlisted` member and its id stays out.

**Done when** every id in the file has been opened and confirmed, and any unpublished ones are `unlisted` with an upstream issue titled `Numista: <acción> (N#…)`, unlabelled.

### 4. Cross the ids against everything already versioned

Before writing a file, cross the types it will name against the types the rest of `data/` already names. One command, zero network:

```
scripts/type-claims.py <id> <id> …        # `235118:582778,585569` adds the issue qualifier
scripts/type-claims.py --file <ruta>      # a file already written, before versioning it
scripts/type-claims.py --all              # every overlap in data/
```

A `PARAR` is not a warning to weigh. Two ordinary catalogs naming one type is rejected the moment the seeds load — `CatalogSeeds.validateCrossCatalogClaims`, «Numista type `X` is claimed by more than one collection catalog without issue-qualified identities» — and the file travels to the father's phone in the release, so what looks like it would be a strange card is an app that does not start.

One overlap is legitimate and must not stop the curation: two catalogs may share a type when **both** identities are issue-qualified and their `numista_issue_ids` are disjoint. That is what `lunar-iii-perth-1oz-bullion` and `lunar-iii-perth-1oz-proof-coloured` do over `235118`, `307024` and `342221`, and the two Rwanda Nautical files over four more — the years Numista archives two finishes under a single type.

The other shapes have no such exit. An agrupación loses the family to any catalog naming the type (ADR 0013), and a set catalog wins it and takes the coin *off* the denomination card (ADR 0022): both are curation mistakes, not precedence to resolve at runtime, and neither is caught at startup. A commemorative programme is the one file that shares types on purpose — it produces no card and never reaches `deriveCollection`, which is exactly what lets it coexist.

When the collision is none of those, the shape is usually wrong before the ids are: a type repeated across casillas is only legal inside one date run. And a genuine overlap between two collections — one coin on two plates, in earnest — is the domain change [#149](https://github.com/jenarvaezg/coindex/issues/149) postponed until the first real case, so that goes in first and this curation waits.

**Done when** the cross-check is clean, or the only overlap is the issue-qualified disjoint kind and you have said which emissions each file takes.

### 5. Choose the face the lámina prints

Look at both faces of every casilla — the cache describes them, the ficha shows them — and declare in the header the one that **is** the coin: the one the collector recognises as this piece. `printed_side: "obverse"` when it is the anverso; keeping quiet declares the reverso, which is what comes out today. ADR 0020 holds the criterion and why it is never deduced from the descriptions.

It is a claim of the **whole lámina**. If one coin wants a face its sisters do not, either the whole lámina changes or it is borne, and either way the reason is written into `source_note`: the debt stays visible in the curated file instead of becoming a per-member precedence.

Skip this and the lámina is born without the field and nobody notices — out comes Numista's reverso, which is sometimes a coat of arms where the coin is a mermaid.

**Done when** the header declares a face, or you have said why the reverso is the right one for this lámina.

### 6. Version the file, and seed the cache

Write the JSON under `data/collection-catalogs/` (or `data/groupings/`). Then seed the fichas of every new type. `TypeCacheSeedTest` is what *finds* the missing ones — it goes red with the list — and the script seeds the ids you hand it:

```
python3 scripts/seed-type-cache.py --dry-run <id> <id> …    # says the API cost first
```

Seeding is half of curating, not an afterthought. The lámina draws what the collector is *missing*, so a hole is only visible on the phone that does **not** have the coin — invisible to whoever cured it.

Say out loud, before doing it, what a rename costs: the family is part of the primary key of `collection_proposal_preferences`, so renaming one erases the intention saved on that card, and widening the variant key erases every disposition that is not a catalog's.

**Done when** `./gradlew :domain:test :app:testDebugUnitTest` is green — that suite carries the startup validator over the real files, the seed check and the cross-file ambiguity check.

### 7. Measure the lámina

Report the real fraction for both collections by running the field report over a private capture, never by asserting it:

```
COINDEX_FIELD_SNAPSHOT=<dir> ./gradlew :app:testDebugUnitTest --tests '*FieldReportTest*' --rerun
```

`--rerun` is not optional: the environment variable is not a declared task input, so without it you read one collection's report believing it is the other's. The captures live outside the tree and the per-piece listings stay unpublished — this repo is public and these are two private inventories.

**Done when** the plate fraction and the change in «Sin clasificar» are **medido** for both collections, or you have said plainly that no capture was available and the figures are therefore unmeasured.

### 8. Keep the prose honest

A curation that teaches something changes the documents too: `CONTEXT.md` when a term is new, `spec.md` when a section it describes is now false, and a fresh ADR when you had to *decide* rather than look up. Say which of the three you touched, and why the others needed nothing.

## Branch: an open catalog that is behind

`scripts/stale-catalogs.py` and the [Catálogos abiertos por detrás](https://github.com/jenarvaezg/coindex/issues/136) issue name the tail. Adding this year's casilla is steps 3 through 8 only — the sequence already proved itself, so re-litigating step 1 wastes the collector's time.

Interior gaps deserve the opposite reflex: a year missing in the middle is usually a year the mint did not strike, and confirming that outside Numista closes it as legitimate calendar rather than curation debt.

## Before sitting down: what the weights disagree about

```
python3 scripts/weight-deviations.py
```

Zero network, never red. It lists the members whose ficha weighs something other than what their catalog declares, the types no catalog claims whose weight the magnet moves, and the cards the file avoids in silence (ADR 0016). It syncs to the [Desviaciones de peso](https://github.com/jenarvaezg/coindex/issues/158) issue, and reading it is not curating: **a line says «look at it», not «fix it»** — most are Numista varying its grams, some are the fineness, and once in a while it is a coin that does not belong in that lámina. What you decide goes in the file, as a `variant_note` when the deviation is deliberate.

The first section, **«Sin mirar»**, is the whole work-list: the lines whose explanation is written in no file. The rest are grouped into cúmulos because a curator already looked at them. Start by running the `--refresh` command the report prints for that section — `data/` never refreshes itself, so a correction Numista has already **accepted** keeps showing up as a deviation until someone reseeds the ficha.

## Guardrails

- A physical check against a Numista ficha lives in the test suite and stays silenceable in prose. What a catalog declares is the variant of the *collection*, not an assertion about each member, so a curator's judgment must never be able to halt the app.
- Version what the source sustains. Where it sustains nothing, an `unlisted` member, a huérfana verdict or an open question are all honest; a plausible id is not.
- Numista's web pages answer through a visible browser with a human passing the challenge. Budget every API call before making it: each key carries roughly 1.500-2.000 a month, and exploratory calls spend the collector's own allowance.
