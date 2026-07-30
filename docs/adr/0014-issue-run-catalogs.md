# ADR 0014: Issue-run catalogs, and which year a date run means

- Status: accepted
- Date: 2026-07-30

## Context

The collector opened the paquillos —the 100 pesetas of Franco, N#1885— in v0.4.0 and said the
1969 he does not own was not there. It was not: they had shipped as a curated grouping
(ADR 0013), which declares no members and so can never report one missing. The obvious fix was a
date-run catalog (ADR 0009) over the five star dates, and it would have been a lie.

Numista files the type as **six issues that are all dated 1966**. The star punched on the coin —
66, 67, 68, 69 with a curved nine, 69 with a straight nine, 70 — is a variety of the issue, not a
year. `/types/1885/issues` says so exactly:

| issue | `year` | `gregorian_year` | mintage | comment |
| --- | --- | --- | --- | --- |
| 8508 | 1966 | 1966 | 15.045.000 | `"66" on star` |
| 33204 | 1966 | 1967 | 15.000.000 | `"67" on star` |
| 33205 | 1966 | 1968 | 24.000.000 | `"68" on star` |
| 33206 | 1966 | 1969 | — | `"69" on star; curved 9` |
| 368163 | 1966 | 1969 | 4.500 | `"69" on star; straight 9` |
| 33207 | 1966 | 1970 | 995.000 | `"70" on star` |

A date run compares `CollectedItem.recordedYear` with the member's year, and `recordedYear` is
`issueYear ?: gregorianYear` — 1966 for all six. A five-member date run would have filled one slot
and reported four stars as missing while they sat in the album. This is the same false «me falta»
the project refuses to show, arrived at from a new direction.

The same reading exposed a bug already shipped. The 2 bolívares date run took the year of N#10399
from the type's `min_year`/`max_year`, which say **1947** — the year it was struck. The issue
carries `year: 1945` with `gregorian_year: 1947`, and the coin itself is dated 1945, so
`recordedYear` is 1945: the member could never be filled, and the plate would have reported a coin
in the collector's hand as missing. The smoke inventory used to verify the plate had been seeded
with `issueYear = 1947`, encoding the same assumption as the catalog, so the test agreed with the
bug instead of catching it.

## Decision

**`schema_version` 5 identifies members by Numista issue.** A member of an issue run declares
`numista_issue_ids` and a label of the project's own writing («Estrella 67»); matching compares the
piece's issue and **ignores the year entirely**, because in an issue run the year is what the
members have in common.

`numista_issue_ids` is a **list**, so one slot can hold several varieties of the same issue. The
1969 holds both nines: the collector counts one star, and the straight nine —4.500 pieces struck—
as its own slot would be a hole that never closes. Owning either fills it; owning both counts one
member with both pieces behind it.

Validation is stricter than for the other schemas, because a wrong issue id fails silently rather
than loudly: every member of an issue run must name at least one issue, no issue may appear in two
slots, and no member outside an issue run may name issues at all.

**The issue id is read from the response already stored**, not from a new column and not from a new
API call. `SyncService` keeps the untouched JSON element of every row in `CollectedItemEntity.raw`,
so `issue.id` is there for every piece synced since the app existed. `Mappers.toDomain` parses it
out with a lenient reader; a row with no issue, or one whose JSON cannot be read, simply has no
issue and fills no member. This is the same bargain ADR 0005 makes for the finish: derive on read,
so improving the rules fixes old rows without spending budget.

**A date run means the year on the coin**, which is Numista's `year`, and never
`gregorian_year`. The two differ whenever a coin was struck after its date, and the collector
thinks in the date punched on the piece: N#10399 is «el 1945», struck in 1947. Its member is
therefore keyed on 1945 and labelled «1945 (acuñada en 1947)», which says both.

## Consequences

The paquillos become a catalog of five stars over six issues, and their plate reports 4 de 5 with
the 1969 in grey — the thing the collector asked for. Their grouping seed is deleted: a group that
has a list of emissions is a catalog, which is one fewer curated grouping and a hint about
[issue #12](https://github.com/jenarvaezg/coindex/issues/12), where the two species collapse into
one.

Any type whose members share a year is now catalogable: Spanish star dates, and varieties in
general. The cost is that an issue run cannot be authored from the public catalogue page, which
does not expose issue ids — it takes one call to `/types/{id}/issues`, spent once at curation time
and frozen into the seed.

The rule for verifying seeds gets sharper. A type's `min_year`/`max_year` are not what a collected
row carries, and reading them off the web page is not verification: the years of a date run come
from `/types/{id}/issues`, where `year` and `gregorian_year` are both visible and their difference
is the trap. The 22 years of N#10339 were re-checked against the API and were right; the one member
that came from the type range was not.

And a lesson about the smoke inventory: seeding it from the same assumption as the catalog makes
the test agree with the bug. Where a catalog claims a year or an issue, the seeded rows must come
from the API's own shape, not from what the catalog expects to find.
