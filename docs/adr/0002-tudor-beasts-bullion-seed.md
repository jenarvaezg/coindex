# ADR 0002: Tudor Beasts seed follows the bullion release sequence

- Status: accepted
- Date: 2026-07-28

## Context

The Phase 1 specification describes a 2 oz silver series beginning with the Seymour
Panther in 2022, while also warning that the order and years are unconfirmed and requiring
verification against The Royal Mint.

The Royal Mint separates proof and bullion ranges. Its Seymour Panther page calls that
design the first proof release. Its current collection and bullion product pages identify
the 2026 Royal Dragon as the ninth bullion release, with a 2 oz silver bullion edition.
This establishes that the proof and bullion sequences do not have the same order.

Sources:

- <https://www.royalmint.com/the-royal-tudor-beasts/>
- <https://www.royalmint.com/the-royal-tudor-beasts/the-seymour-panther/>
- <https://www.royalmint.com/invest/bullion/bullion-coins/silver-coins/the-royal-tudor-beasts-2026-royal-dragon-2oz-silver-bullion-coin/>

## Decision

The curated seed models the 2 oz silver **bullion** sequence and includes only releases
verified from Royal Mint material. It remains marked `incomplete: true` until the tenth
bullion design is announced. No Numista type IDs are guessed.

## Consequences

The seed intentionally differs from the provisional chronology in the specification.
Proof-only Seymour Panther chronology must not be used to label a bullion slot.
