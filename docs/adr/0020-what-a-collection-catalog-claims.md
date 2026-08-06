# ADR 0020: What a collection catalog claims, and what it does not

- Status: accepted
- Date: 2026-08-03

> **Amended on 2026-08-06 (#256).** The existence criterion below stands whole, and its second
> clause gets its wording fixed by the first file that broke it. «One of the two collections is
> pursuing it» was written as if pursuit were readable off the inventory, and
> `venezuela-500-bolivares-plata` ships at **0 of 5 in both collections**: five silver proofs of
> 31,1 g the Banco Central struck between 1990 and 1997, curated because the collector said so
> with the measurement in front of him, not because a piece of it is in a drawer.
>
> The clause therefore reads: **the curator declares the pursuit, and owning a member is evidence
> of it and never the test.** A plate exists to say «me falta», so a plate that says it about every
> slot is doing its job and not failing a gate — the same reason ADR 0013's grouping and this
> ADR's own orphan verdict are editorial and reach none of the validator. What the file owes in
> exchange is honesty in prose: `source_note` says outright that neither collection holds one.
>
> Nothing else moves. A plate still needs two rows, the sequence still has to exist without us,
> and a sequence nobody is chasing and nobody has declared is still an orphan (#242 signed two
> that way on the collector's word alone).

## Context

ADR 0009, 0012, 0013, 0014, 0016, 0018 and 0019 all answer the same kind of question: how a
curated file *identifies* a slot. None of them answers what the file *claims* — whether its list
is finished, what a slot means when no coin exists to fill it, and what the boundary rests on when
Numista is not what draws it. `spec.md §0.9` carried three open questions about exactly that, and
the answers arrived one curation at a time across map #14 without a document to land in.

The gap was not academic. A catalog exists to give a plate a denominator: «13 / 19 emisiones» is
the whole product, and every number in it is an editorial claim about a boundary. A file that says
nothing about whether its series is still being issued claims completeness by omission. A file
that must name a Numista type per member cannot represent a coin the mint has announced but not
struck, nor one struck without a published Numista page — and both exist in the two real
collections this project serves.

Two further pressures shaped the answers. Numista's data is not a boundary: it has no series
operation in its API, its families span physical variants, and a family has no denominator at all.
And the catalogs ship inside the APK, so a claim is only as fresh as the release that carries it.

## Decision

### A catalog declares whether its series is still open

`series_status: "open" | "closed"` is **required in every schema version**. Closing requires
`closed_note` in prose; opening **forbids** it. The asymmetry is the point: closing is the claim
that costs proof, so `open` is also what a curator declares when the search simply does not find
a closure.

An open catalog claims «N of N catalogued» and **promises nothing about any date**. It does not
claim to be up to date, and going behind is not a defect in the file.

The field belongs where coverage belongs — every catalog, no grouping. A curated grouping
(ADR 0013) affirms no coverage, so it has no boundary to declare.

### A catalog earns its existence by claiming what a Numista family cannot

A catalog is justified when it affirms something the family Numista gives its types does not
already affirm. The relationship with Numista is **hybrid: the series proposes, only the versioned
catalog affirms coverage**, and the app never counts from `series`. Three consequences:

- **The series is not the unit of catalog; the physical variant is.** One Numista series routinely
  sustains several catalogs — the 18 types of Equilibrium are three collections, and series 6598
  already sustained three.
- **A series has no denominator**, because it is not homogeneous in variant. The collector does not
  own 1 of 18 but 1 of 8, so populating members from a series would print a worse number than the
  curated one.
- **There are no measurable gatekeepers.** «One issuer only» and «one physical standard only» were
  both proposed and both died against real catalogs. What a sequence must satisfy to be curated is
  editorial and stays editorial: the sequence exists without us, one of the two collections is
  pursuing it, and the plate would hold at least two rows.

Annual bullion of stable design **is** catalogued, one slot per year even when the design does not
change, and it is catalogued the same way when Numista happens to have named its family. The
editorial limit is «a coin a year that would make sense to buy»: it excludes ordinary circulation
and a restrike with a frozen date. What splits one plate from another is the **physical variant**,
not the design — a privy mark or a new annual animal splits nothing.

A coin for which no plate would make sense is an **orphan**, and that is a curator's verdict
recorded by hand in `data/orphans.json`, never the automatic residue of what `deriveCollection`
could not place. Calendar solitude — a programme that may still grow — is not an orphan.

### A member declares its own state, and the file always says so out loud

`status: "issued" | "announced" | "unlisted"`, defaulting to `issued`. This is a property of a
member, not a schema version: it composes with all four ways of identifying one, so
`schema_version: 4` remains unused.

- **issued** names a `numista_type_id` and a year, and may carry neither `source` nor
  `design_type_id`: its own type is the proof.
- **announced** is named by the issuer and not yet struck. It **forbids** `numista_type_id` and
  requires `source` plus `source_note`; the year becomes optional only here, because writing in a
  date the mint has not announced would claim more than the source says. An optional
  `design_type_id` cites the same design in another physical variant and **never** participates in
  matching or in evidence.
- **unlisted** was struck and sold but has no published Numista type. It forbids
  `numista_type_id` — an id is never written for a page a referee may still delete — requires the
  same `source`/`source_note` pair, and requires the year.

The symmetry is enforced in both directions: an absent `numista_type_id` never *means* announced,
the file has to say so.

Three editorial rules govern an announced member, none of them in the validator: an announcement
does earn existence, because Numista catalogs what was struck and can never affirm an unstruck
coin; an announcement with **no identity at all** — no name, no year — is worth nothing without a
programme count, since «more will come» is what `series_status: open` already says; and a catalog
needs **at least one issued** member, because one made only of announcements never progresses and
would ship in the APK unopened.

### The issuer is a fact about a coin, and the catalog's is only a default

**A member may declare its own `issuer_code`, and the catalog's is what a member means when it
declares none** (#170). Absence therefore changes nothing about the 58 files that already shipped,
and the header stops being readable as the issuer *of a collection*: `issuerCodes()` is what names
a country, one code in 58 catalogs and two in Equilibrium.

The measurement forced it. Pressburg Mint strikes the silver ounce of Equilibrium for Tokelau from
2018 to 2022 and again in 2024, and for Niue in 2023 and 2025 — verified on the fichas, where the
issuer changes with the denomination (5 dollars against 2) — and Numista's own series 3245 heads
itself «Emisores: Niue, Tokelau». One header code over that list is not an imprecision of
catalogue: it prints «Tokelau» over a coin whose obverse says Niue, and the collector owning
exactly that coin is the case in the field.

Splitting the catalog in two was the alternative and is refused: the years interleave, so it would
produce two plates of a series the mint never split, and «one issuer only» is precisely the
gatekeeper this ADR already killed above. The curator had it right before the schema did — the
country is written into every member id.

One rule guards it, and it is structural: **the catalog's `issuer_code` must be the issuer of at
least one member.** A default that defaults for nobody is the same false label one indirection
deeper.

### The denominator counts what the app can measure

A plate's denominator counts issued members only. Announced and unlisted slots are shown and
excluded from it.

This follows from a hard fact of the inventory rather than from taste: `CollectedItem.typeId` is
non-null and the inventory *is* the collector's Numista collection, so **a piece with no Numista
type cannot be in the inventory and never will be**. An unlisted slot is therefore unfillable, and
counting it would print «1 / 7» forever with all seven coins in the drawer. An Album marks such a
slot neither Owned nor Missing but with a fourth state, because the app cannot know whether the
collector has it.

### Provenance: `source` may be a series or a type, and prose carries what a URL cannot

`source` accepts a Numista series page **or** a Numista type page. Requiring a series URL assumed
every list is born from a Numista series, and the 10 gulden of Beatrix have `series: null` on all
five types with the *Handboek van de Nederlandse munten 1795-2001* drawing the boundary.

**A catalog may carry an optional `source_note`**, prose beside `source`, allowed whether the
series is open or closed. Since `closed_note` is forbidden while open, an open catalog whose
boundary comes from its mint previously had nowhere in the file to cite it. It is the
`source`/`source_note` pair the members already have, one level up. It is optional and never
proves anything by itself; the validator only refuses it blank.

### Freshness is the release, not a channel

**Catalogs keep travelling inside the APK. The optional remote catalog file of `spec.md §0.9.4` is
rejected.** An open catalog promises nothing about any date, so shortening the distance between
curation and phone buys convenience, not honesty — 136 KB of catalog inside a 29.5 MB APK, with
ten releases published in two days. It would also turn a fatal startup validator into a remote
weapon: today every byte it validates passed through CI. Identity already lives inside the file
(`id`, `updated_at`), so no external manifest is needed to identify a catalog.

The accepted consequence is that **a catalog's freshness is bounded by the installed APK**. Three
signals would reopen the question: a third user, curation decoupled from a single curator, or a
measured connection between phone and repository. The mitigation is not architecture: the release
manifest already carries `notes`, so `scripts/release.sh` says when a release brings changed
`data/`, derived from `git diff <previous tag>..HEAD -- data/`.

Being behind is reported, not stored as freshness. No `checked_at` field and no new meaning for
`updated_at`, which is a modification stamp. A CI step keeps a single issue in sync with the tail
and the interior gaps of every open catalog, informative and never red.

**Interior years the mint skipped are not debt.** An open catalog may declare
`no_issue_years` with a required `no_issue_note` — the same proof bargain as `closed_note` —
so the stale-catalogs report drops those years from gaps. They are never members and never
touch the plate denominator: the claim is «the mint issued nothing that year for this variant»,
not a slot to fill. #94 left gaps listed forever because an open catalog then had nowhere to
write the exception; `source_note` opened the door for prose, and this field makes the exception
machine-readable.

### Physical cross-checks live in the suite and are never fatal

By ADR 0016 what a catalog declares — weight, finish, metal — is **the variant of the collection,
not an assertion about each member**. A curator who puts seven silver coins and one of cupronickel
in one list is curating. So a check against a Numista ficha may never be fatal: it lives in the
test suite and is silenced by declaring the exception in prose on the slot, the same bargain
`closed_note` makes. What it catches is the accidental intruder, not the curator's decision. Only
the metal is cross-checkable at all: Numista's grams do not agree with themselves, its finish
field does not exist, and both checks stop at catalogs, because a grouping has no members in which
to write the exception.

## Consequences

- **The validator.** It stops the app at startup with the file and the reason, so every rule above
  that is structural is now a startup rule: `series_status` required in all versions,
  `closed_note` required exactly when closed and forbidden when open, `source` accepting a series
  or a type page, `source_note` refused only when blank, `no_issue_years` requiring
  `no_issue_note` (and the reverse; a blank note is refused), years unique and inside the
  member span without colliding with a slot, a member `issuer_code` refused only when blank and a
  catalog one that is the issuer of no member, and the full status symmetry with its proof pair.
  The editorial rules — the existence criterion, the minimum of two rows, the three rules for
  announcements, the annual-bullion limit — deliberately reach **none** of it: they are judgments,
  and a judgment that halts the app is a judgment nobody can override.
- The 49 shipped catalogs declare their status: 28 open and 21 closed. Gothic Horror was retired
  under the existence criterion, and no other file failed it.
- `schema_version: 4` stays unused. Nothing here needed a new version: coverage is a property of
  the catalog and state is a property of the member, and both compose with the four existing ways
  of identifying a slot.
- A plate can now show what does not exist yet. The Tudor 2 oz bullion reads «2 / 9 emisiones»
  with «Sin emitir · 1 anunciada», because the Seymour Panther was never struck in that variant —
  a fact about the programme, not a hole in the curation.
- The two Royal Mint bullion ranges are the first files to carry a catalog-level `source_note`.
  They are open catalogs that exist because the mint declared a range, and Numista groups neither.
- **Equilibrium is the only catalog whose members span two issuers**, remeasured over all 59 files
  of `data/collection-catalogs/` and every member of them against the type cache — 20 distinct
  declared issuers, no member without a cached ficha. Its two Niue slots are the only per-member
  `issuer_code` in the repository, and its 1 oz strand is complete: 2018-2025 with no gap and no
  duplicate. #170 measured 50 catalogs, which is what the number was in the same week.
- **What a card prints when a catalog spans two issuers is not decided here.** The file no longer
  claims one country, and `Issuers` already has both the fallback to the pieces and the silence
  clause it applies to a card with no file; which of the two a spanning catalog gets is the open
  half of #170, and until it is decided the eyebrow still reads the header.
- No database migration, no new API call and no remote fetch comes out of this ADR. What changes
  is what the curated files are allowed and required to say.

## Alternatives considered

- **A new `schema_version` for announced or unlisted members.** Rejected: both are properties of a
  member and compose with every existing identification, so a new version would duplicate the
  rules of version 1 and force a file-wide choice for a per-slot fact.
- **A nullable `numista_type_id` to mean «not catalogued by Numista».** Rejected: it would lose the
  fatal error that catches the forgetful curator. An explicit third state keeps the error and says
  what the slot is.
- **Populating members from a Numista series.** Rejected above: no denominator, no homogeneity in
  variant, and no series operation in the API.
- **An optional remote catalog file.** Rejected: it buys convenience an open catalog never promised
  and turns a fatal validator into a remote weapon.
- **A free-prose silence list in comments or `source_note` alone.** Rejected for the report: the
  script needs structured years. `no_issue_years` + `no_issue_note` is the machine-readable form
  of the same proof bargain.
- **Allowing `closed_note` while open**, instead of adding `source_note`. Rejected: it would undo
  the asymmetry of the status decision, and a reader could no longer read the presence of that note
  as «this is closed».
- **An `issuer_codes` list in the header** instead of a per-member issuer. Rejected: it renames the
  field in all 52 curated files, plus the tests that read it, to describe one catalog — and it would
  say *which* countries a list spans while still not saying which coin is from which, when the
  members are where that already lives.
- **A `checked_at` field for freshness.** Rejected: if a programme died the catalog closes with its
  note, and if it is alive the gap is temporary by construction. A stamp would record when someone
  looked, which is not a claim about the coins.
