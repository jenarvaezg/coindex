# ADR 0005: Provisional Numista finish and language inference

## Status

Provisional until a deliberate authenticated recording confirms the exact `lang=es`
payloads for the listed ordinary and special-finish types.

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

This policy is deliberately auditable and incomplete. The regression currently uses
catalog-derived synthetic payloads to verify our adapter and matching behavior; it does
not claim to verify Numista's live response contract. Before extending the convention—or
marking this ADR accepted—the relevant real response must be captured with the explicit
fixture recorder and the regression changed to consume that fixture.

## Evidence

The public catalog records used to construct the provisional regression are:

- Numista N#386213: ordinary 2024 Lunar III one-ounce silver Dragon.
- Numista N#404044: one-ounce Silver Proof High Relief.
- Numista N#394043: one-ounce Coloured.
- Numista N#404285: one-ounce Silver Gilded.
- Numista N#482185: two-ounce Silver Antiqued.

These IDs and labels do not establish that the authenticated API returns the same
localized titles or exact `series` value. That empirical acceptance item remains open.
