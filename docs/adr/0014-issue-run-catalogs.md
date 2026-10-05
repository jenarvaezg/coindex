# ADR 0014: Issue-run catalogs, and which year a date run means

- Status: accepted; narrowed by [ADR 0019](0019-issue-qualified-catalog-members.md)
  (`numista_issue_ids` outside an issue run)
- Date: 2026-07-30

## Context

The paquillos — the 100 pesetas of Franco, N#1885 — shipped as a curated grouping (ADR 0013), so the
1969 the collector lacks was not shown as missing. A date run (ADR 0009) over the five star dates
would have been wrong: Numista files the type as **six issues all dated 1966**, and the star on the
coin is a variety, not a year (`/types/1885/issues`):

| issue | `year` | `gregorian_year` | mintage | comment |
| --- | --- | --- | --- | --- |
| 8508 | 1966 | 1966 | 15.045.000 | `"66" on star` |
| 33204 | 1966 | 1967 | 15.000.000 | `"67" on star` |
| 33205 | 1966 | 1968 | 24.000.000 | `"68" on star` |
| 33206 | 1966 | 1969 | — | `"69" on star; curved 9` |
| 368163 | 1966 | 1969 | 4.500 | `"69" on star; straight 9` |
| 33207 | 1966 | 1970 | 995.000 | `"70" on star` |

A date run compares the member's year with `CollectedItem.recordedYear` (`issueYear ?:
gregorianYear`, 1966 for all six), so it would fill one slot and call four owned stars missing.

The same reading exposed a shipped bug: the 2 bolívares date run took N#10399's year from the type's
`min_year`/`max_year`, 1947, the year it was struck. The issue says `year: 1945`,
`gregorian_year: 1947`, so the member could never be filled; and the smoke inventory, seeded with the
same assumption, agreed with the bug.

## Decision

**`schema_version` 5 identifies members by Numista issue.** A member of an issue run declares
`numista_issue_ids` and a label of the project's own («Estrella 67»); matching compares the piece's
issue and **ignores the year**, which all members share.

`numista_issue_ids` is a **list**, so one slot can hold several varieties: the 1969 holds both
nines, because the collector counts one star and the straight nine (4.500 struck) would be a hole
that never closes. Owning either fills it.

A wrong issue id fails silently, so validation is stricter: every member of an issue run names at
least one issue, no issue appears in two slots, and no member outside an issue run names issues
*(lifted by ADR 0019)*.

**The issue id is read from the stored response**, with no new column or API call: `SyncService`
keeps each row's untouched JSON in `CollectedItemEntity.raw`, and `Mappers.toDomain` reads
`issue.id` leniently; a row without one fills no member. As with the finish (ADR 0005), better rules
fix old rows without spending budget.

**A date run means the year on the coin**, Numista's `year`, never `gregorian_year`: N#10399 is
«el 1945», keyed on 1945 and labelled «1945 (acuñada en 1947)».

## Consequences

- The paquillos become a catalog of five stars over six issues (4 de 5, the 1969 in grey), and their
  grouping is deleted: a group with a list of emissions is a catalog (see
  [issue #12](https://github.com/jenarvaezg/coindex/issues/12)).
- Star dates and varieties in general become catalogable. An issue run costs one call to
  `/types/{id}/issues` at curation time, since the public catalogue page shows no issue ids.
- **The years of a date run come from `/types/{id}/issues`**, never from the type's
  `min_year`/`max_year` or the web page.
- **The smoke inventory is seeded from the API's shape**, not from what the catalog expects to find.
