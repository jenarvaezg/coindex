# ADR 0005: Numista finish and language inference

## Status

Accepted. Deliberate authenticated recordings confirmed the exact `lang=es` payloads
for the listed ordinary and special-finish types.

## Context

The curated seeds describe bullion slots and intentionally contain no guessed Numista
type IDs. Their fallback matchers therefore depend on metadata including finish and an
English motif term. The production type endpoint is requested with `lang=es`.

Numista does not expose a dedicated finish field in the response shape currently used by
Coindex. Its catalog distinguishes ordinary and special Lunar III types in titles such as
`Silver Proof`, `Coloured`, `Silver Gilded`, and `Silver Antiqued`. Some ordinary Lunar III
titles only say `Silver`. Spanish-localized motif text can also differ from the English
terms curated in the seed.

## Decision

The backend adapter, rather than the pure matching engine, owns these catalog-specific
inferences:

- Special finish markers are evaluated before bullion.
- An otherwise unmarked type is inferred as bullion only when its title says `bullion` or
  its series is one of the two curated bullion series: `Lunar Series III` or
  `The Royal Tudor Beasts`.
- Known colour-named Lunar III variants are classified as coloured even when the title
  omits the word `Coloured`.
- Exact Spanish motif words used by the committed seeds add English aliases to the
  internal matching title. The original title remains present.

No type IDs are added to seeds by inference.

## Consequences

Ordinary curated bullion can use the heuristic fallback while proof, coloured, gilded,
and antiqued variants remain excluded. Unknown series and unmarked circulation or
collector types retain an unknown finish and cannot satisfy a bullion matcher.

This policy remains deliberately auditable and incomplete. The regression consumes the
exact recorded type responses, so it verifies the adapter and matching behavior against
the observed API contract for these five types. Extending the convention to other
families or finish markers still requires corresponding evidence.

## Evidence

The public empirical fixtures were recorded from type requests using `lang=es`. Despite
that parameter, all five responses returned English titles, and every response returned
the exact series value `Lunar Series III`:

- Numista N#386213: `1 Dollar - Elizabeth II (6th Portrait; In the name of - Year of the Dragon - Silver)`.
- Numista N#394043: `1 Dollar - Elizabeth II (6th Portrait; In the name of - Year of the Dragon - Coloured)`.
- Numista N#404044: `1 Dollar - Elizabeth II (6th Portrait; In the name of - Year of the Dragon - Silver Proof High Relief)`.
- Numista N#404285: `1 Dollar - Elizabeth II (6th Portrait; In the name of - Year of the Dragon - Silver Gilded)`.
- Numista N#482185: `2 Dollars - Elizabeth II (6th Portrait; in the name of - Year of the Dragon - Silver Antiqued)`.

The fixture-driven regression verifies that only N#386213 matches the committed ordinary
bullion slot; the proof, coloured, gilded, and antiqued types remain excluded.
