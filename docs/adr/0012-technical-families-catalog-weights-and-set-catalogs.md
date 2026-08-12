# ADR 0012: Technical families, catalog-declared weights and set catalogs

- Status: accepted, the catalog-declared snapping target superseded by
  [ADR 0016](0016-a-catalog-owns-its-members-variant.md) and retired by #288
- Date: 2026-07-30

## Context

Numista files every pre-euro Portuguese coin under the `series` value
`System 1981-2001`. ADR 0007 treats `System YYYY[-YYYY]` as a technical monetary
system rather than a collectible family and drops those pieces from proposal
derivation, and ADR 0009's catalog-supplied family only applies when Numista
records *no* family. The two rules combine into a dead end: a technical family is
a real `series` value, so the fallback never fires and no curated catalog can
reach those types. Measured on the real inventory, 28 pieces across 23 types —
every Portuguese piece in the collection — were unreachable by construction.

Two smaller obstacles surfaced while curating the Portuguese catalogs.

Numista records the 2001 Porto 500 escudos as 13.96 g and its six siblings as
14 g. `normalizeWeightMillioz` only snaps toward the common bullion weights, so
that piece normalizes to 449 while the rest normalize to 450: a seventh proposal
of one piece, and a plate that would report a coin as missing once bought.

The 1983 XVII European Art Exhibition set is three denominations issued together
in one mint set: 500, 750 and 1000 escudos at 7, 12.5 and 21 g. A collection
catalog targets one exact proposal variant key, so a set that spans physical
variants cannot be expressed at all.

## Decision

**A technical family is the weakest family, never a rejection.** As amended by the
[catalog-family precedence decision](https://github.com/jenarvaezg/coindex/issues/83),
proposal derivation resolves a family in this order: a schema 3 catalog that lists
the types issued together, then a schema 1 or 2 catalog selected by the catalog
routing for that type and piece, then a real Numista family, then the technical
Numista family. ADR 0013 later inserts a curated grouping immediately before the
technical family. Only a
type with no family at all and no matching catalog stays unclassified.
`UnclassifiedReason.TechnicalFamily` therefore disappears; `isTechnicalFamily`
survives as a precedence rule, not a filter.
Technical families reach the collector through the presentation-only alias
`System 1981-2001` → `Sistema monetario 1981-2001`, which never enters the key.

**Curated catalogs contribute their declared weight as a snapping target.**
`normalizeWeightMillioz` takes the `weight_millioz` of every seeded schema 1 and
2 catalog as an additional target, with the same ±10 milli-ounce tolerance it
already applies to bullion weights, resolving ties by proximity and then by the
smaller target. 13.96 g becomes 450. The bullion rule is unchanged: 30 g still
stays at 965 and is never conflated with an ounce.

> **Superseded by [ADR 0016](0016-a-catalog-owns-its-members-variant.md)**
> ([#288](https://github.com/jenarvaezg/coindex/issues/288)), and only this third
> decision — the technical family and schema 3 rules stand.
>
> This decision made sense while every piece passed through
> `normalizeWeightMillioz`: the 13.96 g Porto 500 escudos needed the target to
> join its 14 g siblings. ADR 0016 then gave the file authority over the variant
> of its own members, and derivation started taking a claimed type's key straight
> from `catalog.key()`. From that day the declared targets reached only the
> opposite set — types **no** catalog claims, which is exactly the set nobody
> declared a weight for. Measured on 75 catalogs, they were deciding the variant
> of 22 unlooked-at types, among them a Morgan dollar whose 26.73 g are its legal
> weight, pulled to the 868 of a Spanish commemorative 10 euros. (The report
> listed 27 pulls; the other five are bullion snapping and survive.)
>
> Declared weights are therefore no longer targets, and `normalizeWeightMillioz`
> takes no curated set. The bullion weights stay, because they are a real
> convention a coin belongs to without anyone saying so. Do not reinstate this
> without first re-reading ADR 0016: the case that motivated it can no longer
> reach the function.

**Collection catalog `schema_version: 3` describes a set issued as a set.** It
declares no weight and no finish, and its members carry none either: the set is
the collectible unit, so the physical variant of each member is not part of
anything. Its proposal variant key has an *absent* weight, and a type listed in a
schema 3 catalog derives that key even when Numista records a family for it —
naming the exact types that were issued together is a stronger editorial claim
than Numista's grouping. One such catalog still has exactly one key, so plate
resolution, dispositions and the "no two catalogs claim one key" rule are
untouched.

An absent weight persists as `weightMillioz = -1`. Zero stays an invalid weight,
so a truncated or defaulted row is still ignored rather than read as a set, and
the Room schema does not change.

Schema 3 is deliberately narrow: **only sets whose members were issued together
as one product.** Fractional bullion is not a set. A quarter-ounce Britannia and
a one-ounce Britannia are the same coin in two sizes, and they stay separate
proposals with separate catalogs, exactly as the Tudor Beasts and Lunar II
catalogs already do.

## Consequences

Every Portuguese piece becomes reachable. The seven 500 escudos in silver .500
(1995-2001) get a schema 1 catalog and the 1983 set gets a schema 3 one; the
remaining technical-family pieces group as `Sistema monetario …` proposals, which
are honest suggestions and candidates for future catalogs rather than silent
drops. Around nine such proposals appear on the real inventory.

Both changes ship in the same release as the two Portuguese catalogs on purpose:
no technical family produced a proposal before, so no stored disposition can be
orphaned by the new keys. A catalog added later that claims types already grouped
under a technical family *would* change their key and drop a stored disposition
back to Available — recoverable by following again, and preferable to a permanent
wrong grouping.

Adding a catalog now affects weight normalization globally. That is the intended
coupling — a curated weight is better evidence than an arbitrary gram figure —
but it means a new catalog can merge two previously distinct near-weight
proposals, which the golden table pins.

*(That last consequence ended with the decision above: since #288 a catalog's
declared weight moves its own members and nothing else, so curating one can no
longer merge two cards it does not name.)*
