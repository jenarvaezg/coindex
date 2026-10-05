# ADR 0020: What a collection catalog claims, and what it does not

- Status: accepted; amended by #256 (the existence criterion), #227 (the printed face), #257 and
  #616 (Consequences)
- Date: 2026-08-03

## Context

ADR 0009, 0012, 0013, 0014, 0016, 0018 and 0019 answer how a curated file *identifies* a slot, not
what it *claims*: whether its list is finished, what a slot means when no coin exists to fill it,
and what the boundary rests on when Numista does not draw it. `spec.md §0.9` left three open
questions about this, answered one curation at a time across map #14.

A catalog gives a plate its denominator, so «13 / 19 emisiones» is an editorial claim about a
boundary. A file silent about whether its series is still issued claims completeness by omission,
and a file that must name a Numista type per member cannot hold a coin announced but not struck, or
struck without a Numista page; both exist in the two collections. Numista's data is not a boundary
either (no series operation in its API, families that span physical variants, no denominator for a
family), and catalogs ship inside the APK, so a claim is only as fresh as its release.

## Decision

### A catalog declares whether its series is still open

`series_status: "open" | "closed"` is **required in every schema version**. Closing requires
`closed_note` in prose; opening **forbids** it. Closing is the claim that costs proof, so `open` is
also what a curator declares when no closure is found. An open catalog claims «N of N catalogued»
and **promises nothing about any date**: falling behind is not a defect in the file. Groupings (ADR
0013) affirm no coverage, so they carry no status.

### A catalog earns its existence by claiming what a Numista family cannot

The relationship is **hybrid: the series proposes, only the versioned catalog affirms coverage**,
and the app never counts from `series`.

- **The unit of catalog is the physical variant, not the series.** One series routinely sustains
  several catalogs: the 18 types of Equilibrium are three collections.
- **A series has no denominator**, being heterogeneous in variant: the collector owns 1 of 8, not 1
  of 18.
- **There are no measurable gatekeepers.** «One issuer only» and «one physical standard only» both
  died against real catalogs. A sequence qualifies editorially: it exists without us, **the curator
  declares the pursuit**, and the plate would hold at least two rows.

Annual bullion of stable design **is** catalogued, one slot per year, whether or not Numista named
its family; the limit is «a coin a year that would make sense to buy», which excludes circulation
money and restrikes with a frozen date. Plates split by **physical variant**, not design: a privy
mark or a new annual animal splits nothing.

A coin for which no plate would make sense is an **orphan**: a curator's verdict recorded by hand in
`data/orphans.json`, never the residue of what `deriveCollection` could not place. A programme that
may still grow is not an orphan, and a verdict of intention can be reopened by intention (#257 moved
N#18852 out of the list).

> **Amended by #256 (2026-08-06).** The clause read «one of the two collections is pursuing it», and
> `venezuela-500-bolivares-plata` ships at 0 of 5 in both because the collector asked for it. So
> **the curator declares the pursuit, and owning a member is evidence of it, never the test**;
> `source_note` then says that neither collection holds one.

### A member declares its own state, and the file always says so out loud

`status: "issued" | "announced" | "unlisted"`, defaulting to `issued`. It is a property of the
member and composes with every way of identifying one, so `schema_version: 4` stays unused.

- **issued** names a `numista_type_id` and a year, and needs neither `source` nor `design_type_id`.
- **announced** is named by the issuer and not yet struck. It **forbids** `numista_type_id`,
  requires `source` plus `source_note`, and is the only state where the year is optional, since a
  date the mint has not given would claim more than the source. An optional `design_type_id` cites
  the design in another variant and **never** takes part in matching or evidence.
- **unlisted** was struck and sold but has no published Numista type. It forbids `numista_type_id`
  (no id for a page a referee may still delete) and requires `source`, `source_note` and the year.

An absent `numista_type_id` never *means* announced: the file has to say so. Editorially, outside
the validator: an announcement earns existence, since Numista only catalogs what was struck; one
with no name and no year is worth nothing without a programme count; and a catalog needs **at least
one issued** member, or it never progresses.

### The issuer is a fact about a coin, and the catalog's is only a default

**A member may declare its own `issuer_code`; the catalog's applies to members that declare none**
(#170), and `issuerCodes()` names the countries. Pressburg Mint strikes the Equilibrium ounce for
Tokelau (2018–2022, 2024) and Niue (2023, 2025), and Numista's series 3245 lists both issuers, so
one header code would print «Tokelau» over a coin that says Niue — a coin the collector owns.
Splitting the catalog would make two plates of a series the mint never split, and reinstate «one
issuer only». **The catalog's `issuer_code` must issue at least one member**: a default that applies
to nobody is the same false label.

`historia-del-real` (#257) is the wider case, a **thematic catalog** of four slots over three
issuers and four physical patterns, with no new mechanism: the file is authoritative over its
members' variant (ADR 0016), the declared weight is the **anchor coin's** (the real de a ocho), and
the deviations are `variant_note`.

### The denominator counts what the app can measure

A plate's denominator counts issued members only; announced and unlisted slots are shown outside it.
The inventory *is* the collector's Numista collection and `CollectedItem.typeId` is non-null, so **a
piece with no Numista type can never be in the inventory**. Counting an unlisted slot would print «1
/ 7» forever with all seven coins in the drawer; the album gives it a fourth state, neither Owned
nor Missing.

### Provenance: `source` may be a series or a type, and prose carries what a URL cannot

`source` accepts a Numista series **or** type page: the 10 gulden of Beatrix have `series: null` on
all five types, and their boundary comes from the *Handboek van de Nederlandse munten 1795-2001*.
**A catalog may carry an optional `source_note`**, open or closed, because an open catalog whose
boundary comes from its mint had nowhere to cite it (#53). It is the members' `source`/`source_note`
pair one level up, never proof by itself, and refused only when blank.

### The printed face is a declaration of the catalog

Added by #227 (2026-08-06). `printed_side: "obverse" | "reverse"`, absent meaning `reverse`, says
**which face of its coins the notebook prints**; any other value stops the app at startup.
«Numista's reverse» is not «the face of the coin»: on `haiti-50-gourdes-plata` the coin is the
mermaid on the obverse. It is the face the collector recognises, **declared, never inferred**, and
**of the plate, never of a member** (an odd coin out is borne, with the reason in `source_note`). It
lives **in the catalog, not the type cache**, which every sync overwrites (ADR 0023). Types no
catalog claims keep the reverse.

### Freshness is the release, not a channel

**Catalogs travel inside the APK; the optional remote catalog file of `spec.md §0.9.4` is
rejected.** An open catalog promises nothing about dates, so a faster path from curation to phone
buys convenience, not honesty, and it would turn a fatal startup validator into a remote weapon when
today every byte it validates passed through CI. A catalog's freshness is therefore **bounded by the
installed APK**; a third user, a second curator or a measured phone-to-repository connection would
reopen this. `scripts/release.sh` says in the release `notes` when `data/` changed since the
previous tag.

Being behind is reported, not stored: no `checked_at`, and `updated_at` stays a modification stamp.
A CI step keeps one issue in sync with the tail and interior gaps of every open catalog, never red.
**Interior years the mint skipped are not debt**: `no_issue_years`, with a required `no_issue_note`
(the bargain of `closed_note`), removes them from the report and never touches the denominator.

### Physical cross-checks live in the suite and are never fatal

By ADR 0016 a catalog's weight, finish and metal are **the variant of the collection, not an
assertion about each member**: one cupronickel coin among seven silver ones is curation. A check
against a Numista ficha therefore lives in the test suite and is silenced by declaring the exception
in prose on the slot; it catches accidental intruders, not decisions. Only the metal is checkable
(Numista's grams disagree with themselves and it has no finish), and only on catalogs, since a
grouping has no members to write the exception on.

## Consequences

- **The validator** stops the app at startup with the file and the reason, so every structural rule
  above is a startup rule: the status and its notes, the two kinds of `source`, blank `source_note`
  or member `issuer_code` refused, `no_issue_years` paired with its note, years unique and inside
  the member span, a catalog `issuer_code` that issues some member, `printed_side`'s two values, and
  the full status symmetry. The editorial rules — existence, two rows, announcements, the
  annual-bullion limit — deliberately reach **none** of it: a judgment that halts the app cannot be
  overridden.
- The 49 catalogs shipped then declared 28 open and 21 closed; Gothic Horror was retired under the
  existence criterion.
- **A plate can show what does not exist yet.** The first announced member, the Seymour Panther of
  the Tudor 2 oz bullion, stayed out of the denominator for over a year with no year to name, until
  it was struck on 1 September 2026 (N#604513, #616) and took its slot.
- The two Royal Mint bullion ranges were the first catalog-level `source_note`: open catalogs that
  exist because the mint declared a range Numista does not group.
- Equilibrium and `historia-del-real` span issuers. What a card printed for them was the open half
  of #170; since ADR 0026 §12 the card has no country line, and the country axis reads each member's
  own.
- No database migration, new API call or remote fetch: only what the curated files may and must say
  changes.

## Alternatives considered

- **A new `schema_version` for announced or unlisted members**: per-member properties would force a
  file-wide choice and duplicate version 1's rules.
- **A nullable `numista_type_id` meaning «not on Numista»**: it would lose the fatal error that
  catches a forgetful curator.
- **A silence list in comments or `source_note`**: the report needs structured years.
- **Allowing `closed_note` while open** instead of `source_note`: the note would stop meaning
  «closed».
- **An `issuer_codes` list in the header**: it renames the field in every file to describe one
  catalog, and still does not say which coin is from where.
- **A `checked_at` field**: it records when someone looked, not a claim about the coins; a dead
  programme closes with a note and a live one's gap is temporary.
- Rejected in their sections: populating members from a Numista series, and a remote catalog file.
