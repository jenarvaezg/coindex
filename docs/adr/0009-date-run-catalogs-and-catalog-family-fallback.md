# ADR 0009: Date-run catalogs and catalog-supplied family fallback

- Status: accepted
- Date: 2026-07-29

## Context

Some collectible sequences live inside a single Numista type instead of a Numista
series: the type spans decades and the collectible unit is the issue year (for
example 5 Bolívares N#10340, 1879-1936). Schema 1 collection catalogs identify
members by unique Numista type id, so they cannot express a date run.

These same types usually carry no Numista `series` field, so proposal derivation
under ADR 0007 drops them entirely: there is no proposal to follow and therefore no
plate to attach a catalog to.

## Decision

Collection catalog `schema_version: 2` describes a date run. Members may repeat one
Numista type id as long as the `(numista_type_id, year)` pair is unique. Ownership
of a schema 2 member requires the collected item's issue year (or Gregorian year) to
equal the member year; an undated holding never fills a year. Plate evidence remains
type-id based per ADR 0008, so a collector with undated pieces can still open the
plate and see every year as reference. Because date runs have no Numista series
page, a schema 2 catalog source may be the Numista type page
(`catalogue/piecesNNN.html`) as well as a series page; schema 1 still requires a
series page.

When a collected item's type is claimed by a seeded collection catalog, proposal
derivation uses the family declared by that catalog. This was originally only a
fallback for types with no Numista family; the [catalog-family precedence
decision](https://github.com/jenarvaezg/coindex/issues/83) later made the catalog's
type claim authoritative even when Numista supplies another real family. The
catalog never seeds a proposal by itself: without a matching holding there is no
proposal, preserving ADR 0007. A real Numista family still wins for types that no
catalog claims, and types named by neither remain outside proposals.

## Consequences

Date runs become followable like any other proposal: the catalog supplies the
family, the holding supplies the evidence, and per-user dispositions keep working
on the exact variant key. Sync accuracy of `issue_year` now affects plate progress
for schema 2 catalogs only. Curating a new date run is a data change (one JSON seed
verified against the Numista type's issue list) plus no code.
