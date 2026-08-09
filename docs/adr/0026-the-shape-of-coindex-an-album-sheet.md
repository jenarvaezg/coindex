# ADR 0026: The shape of Coindex — an album sheet, and what moves owes a datum

- Status: accepted, §15 amended by #351 (the grain's approved value)
- Date: 2026-08-08
- Amends ADR 0021 §1 (the top level grows to three hierarchies) and §9 (the eyebrow of a card stops
  being the country). Extends ADR 0010 §8 and ADR 0021 §13 with an export rule. Upholds ADR 0018,
  0020, 0023 and 0024, and leans on all four.
- Written by map [#278](https://github.com/jenarvaezg/coindex/issues/278) across twenty tickets; the
  measurement that decided each one lives in its ticket and its report under `docs/ux/`.

## Context

On 7 August 2026 the collector's son — who is also the only other user — looked at v0.16.0 and said
it is «un par de listados, algo muy simplón», that it lacks any wow, and that there is too much prose
for the space a phone has. The information architecture was not the problem: map #16 decided it and
ADR 0021 wrote it down. The problem was that of the five screens the app has, **only the plate had
ever been designed**, and that `spec.md §0.4` declared an aesthetic — «serif para los textos,
tipografía condensada para los datos, paleta apagada de papel» — that was **written and never
built**.

The baseline was measured before anything was drawn ([#296](https://github.com/jenarvaezg/coindex/issues/296),
`docs/ux/medida-base-296.md`):

- **2.16 collection cards** fit on the index screen (5.2 without the masthead), out of 70.
- **69 %** of the words on an arrival screen are furniture, at 4 lines and 14.6 words per card.
- **994 words and zero photographs** in an index whose APK already carries 916 fichas with images.
- `app/src/main/res/font` **does not exist**: the field guide runs on the system `FontFamily.Serif`
  and `SansSerif`. The paper is a flat `#EEE8D7`.
- `grep -rn -E "animate|Animated|SharedTransition|graphicsLayer|BlendMode"` over `app/src/main/kotlin`
  returns **0 results**. In 22 of 22 gestures, nothing happens.

This ADR records the shape the map chose. It **does not choose a new identity**: it executes the one
`spec.md §0.4` already declared, and the reason it needed twenty tickets is that executing it turned
out to require deciding what a collection *looks like*, not what it *is*.

Two standing constraints shaped every decision. **The user who matters is the father**: 70 cards, 6
complete plates, and his main gesture is exporting a plate as a PNG to show people — so an effect
that only exists in motion does not reach where he shows it. And **density and wow are one task**:
the room for the die-cut, the stamp and the flip comes out of the pruning.

## Decision

### 1. Coindex is an album sheet, not a listing

A collection stops being a four-line card and becomes a **die-cut hole with its coin inside**; the
plate is the same sheet arranged by year, with the design as a ghost where the piece is missing. The
index goes from **2.07 to 11.04 collections per screen** and shows coins for the first time
([#300](https://github.com/jenarvaezg/coindex/issues/300), `docs/ux/hoja-300.md`).

- **Two typefaces ship inside the APK: Bitter + Barlow Condensed** — 245 KB, **+0.81 %** over 30.86 MB
  ([#298](https://github.com/jenarvaezg/coindex/issues/298), `docs/ux/fuentes-empaquetadas-298.md`).
  Neither wins on weight: Bitter is the only serif that **costs no width** (44.5 against the 44.4 of
  today's Noto Serif) and Barlow brings **real small caps** for 48 KB, which retires the `smcp`
  faked with capitals and `letterSpacing` in `Theme.kt`. Tabular figures (`tnum`) become real too.
  **The fonts are not subset**, and no italic ships — the whole `ui/` layer does not use one.
- **`←`, `✓` and `↗` become vector icons.** They exist in neither face, so today Android's fallback
  already paints them with a different font; with packaged type they would be three obvious foreign
  bodies.
- **Paper is fine fibre in `soft-light`, flat, with no sheet shadow.** The shadow is redundant, not
  expensive: with the entry sunk into the cardboard the relief is already the die-cut's job. The only
  fixed highlight is the **acetate sleeve reflection**, static, so it survives the exported PNG.
- **Numista's photographs solve themselves.** A round hole with `cover` cropping makes the cut-out
  over a light background simply invisible. Measured: of the father's 192 types, **188 have a seeded
  ficha and all 188 carry an image**; the other four have no ficha until the first sync.
- **The card's photograph is the first issue he *has***, not the first of the catalog (amended by
  [#304](https://github.com/jenarvaezg/coindex/issues/304)): under the old rule the 1 Bolívar card
  showed 1879, which is a ghost in his plate.
- **Search, filter and sort live in a 76 dp strip** that is identical for Collections and for Coins —
  the symmetry the code had (`IndexShelf.kt`, `CoinsShelf.kt`, `Bands.kt`) and the interface did not
  show. The shelf loses its «cerrar» button: the whole row is already the control.
- **The masthead is replaced by a sewn edge** carrying the three counts and the hour:
  `70 col · 574 monedas · 192 tipos · hoy 18:47`, with Ajustes as an icon.

### 2. Paper at any hour: there is no dark theme, and the promise is declared

Coindex does **not** follow the system's dark theme
([#301](https://github.com/jenarvaezg/coindex/issues/301)). The lamp — «the paper dims and the ink
does not invert» — is not rejected for coherence but because **it is not buildable on an emissive
screen**. On real paper, dimming the light dims the ink too and the order is preserved; on a phone,
for the text to stay legible the ink has to *lighten*, and that **is** inverting, only warm. What
would be left is the dashboard `spec.md §0.4` forbids.

- **`android:forceDarkAllowed` is `false` in `Theme.Coindex`.** `Theme.kt` only governs what Compose
  draws; the manufacturer force-dark of Samsung and Xiaomi acts underneath, on the drawing commands,
  and no heuristic knows that `#EEE8D7` is paper — the result would not be a lamp, it would be paper
  inverted to dirty grey with the photographs intact on top. The attribute is API 29, which is the
  `minSdk`. The cost is stated rather than discovered: on a phone set to dark, Coindex will be the
  one light screen, and that moves from side effect to promise.
- **The shielding lives in the theme, never in the export path.** A `ForcedPaperTheme` around
  `OffScreenSheet` would give false cover: if there ever were a real lamp, the sheet background would
  still be paper while the ink had lightened, so the PNG would come out **broken** rather than dark.
- **There is no switch in Ajustes**, because there are not two states to choose between. Night belongs
  to the system: «Extra dim» and «Night light» dim the whole panel, which is better than lying with
  colours, and Coindex does not compete with that layer.

### 3. The ceiling is measured in movements, and each one owes a cause and a datum

> A movement enters if **(a)** it answers an act of the collector **and** **(b)** it says something
> the resting state does not. Both, not either.

This rule ([#307](https://github.com/jenarvaezg/coindex/issues/307)) is what judges any future
movement without having to draw it, and it is what rejected the header parallax — cause, no datum.
Of the twenty-one candidates the six prototypes left, **sixteen are still** — they are how the sheet
is drawn, and they never compete for attention — and five move. **Four move and are approved**, and
they are the first four movements in the life of the app:

| movement | cause | datum | to paper? |
| --- | --- | --- | --- |
| **the flip** — `rotationY` + `cameraDistance`, 420 ms; cardboard and acetate stay still | tapping the hole | the other face | no · the PNG comes out on `printed_side` |
| **the gloss** — a black→transparent→white gradient at 105° in `BlendMode.Softlight`, moved by the accelerometer | tilting the phone | it is metal | **no** |
| **the stamping** — the ink falls on the ratio when a complete sheet is opened | opening the sheet | it is complete | the stamp yes, the stamping no |
| **the journey** — the coin flies from the card to **its** slot, `SharedTransitionLayout` | navigating | it is the same coin | no |

The gloss is the only exception to (a) and earns it on (b): it is the one thing in the app that says
this is metal and not a drawing. It pays for it by resting on a table, which is how a long plate is
looked at.

**The flip** ([#302](https://github.com/jenarvaezg/coindex/issues/302), `docs/ux/giro-302.md`) is
fired by a tap on the body of the hole — which was free, since only the title was tappable
(`PlateScreen.kt:193`) — so a slot has **two targets and not one**: the hole flips, the year goes to
Numista. The year is a **sunken tag**, 48.3 × 28 dp, the same language as the die-cut and the only one
of four candidates whose drawing reaches Android's 48 dp, at a cost of 7 dp per slot (0.54 of 13.74).
Today's convention falls to the `↗`: neither face carries it, so it would be **22 arrows in the system
font** on a sheet of paper. Which face rests is `printed_side` (ADR 0020), the same declaration paper
obeys. It is accepted knowingly that this leaves a plate in **mixed states**, which a real cardboard
sheet cannot be: the flip is momentary, the sheet returns to `printed_side` on recomposition, and the
PNG never comes out mixed.

**The gloss** ([#303](https://github.com/jenarvaezg/coindex/issues/303), `docs/ux/brillo-303.md`) is
light *and* shadow: the surface tilts, it is not lit. Its three hard edges dissolved on measurement —
**no AGSL is needed** (it is a gradient with a blend mode, and `BlendMode` is API 29, exactly the
`minSdk`, so there is no fallback to decide), **no metal tint** (183 of the father's 188 typed
fichas are silver, zero gold, and ADR 0018 puts metal in the variant key, so a mixed-metal plate
cannot exist by construction), and **the accelerometer is enough**, registered only in the foreground
with a coin on screen, at `SENSOR_DELAY_UI`, released on `onPause`. Intensity is half of the
prototype video's: the photograph already carries its light baked in from the upper left.

**The stamp** ([#304](https://github.com/jenarvaezg/coindex/issues/304), `docs/ux/ceremonia-304.md`)
**is a state and not a medal**, which is what dissolves the edge the ticket feared — «when does it
*just* become complete» — because there is no event to remember. It is read from the inventory like
the die-cut: 84 × 76 dp of rubber stamp in `multiply`, rotated 5.5°, landing **on the ratio the header
already showed**, so it adds not one word and not one figure. It is stamped **on opening the sheet and
never on syncing** (one bit per catalog in `NamedValues`), and **only on the plate** — an index card
is never opened, so a stamp there would fire the ceremony on scroll. The word is **«completa»**, the
only one, including for an open series.

The measurement that changed this decision before anything was drawn: **the father's six complete
plates have been complete since the day we curated their catalog**. He has never completed one inside
Coindex. So the ceremony **does not congratulate — it reveals.**

**The journey** flies the coin **only where it is the protagonist**: its slot on a plate, and the
coin's ficha sheet (§5). In `Pieces` and `Box` it does not fly, because on the other side there is no
slot of its own but inventory rows where `CoinSides` paints both faces at 150 dp — promising «it is
this one» and landing on the first of twelve is a lie. That some cards fly and others do not **is
visible before touching**: the ones that do not carry no ratio (ADR 0021 §3), which is exactly what
already tells them apart. They are 20 of the father's 69 cards.

### 4. The export rule: what is still travels to paper, what is alive does not

> Anything still travels to the PNG and the PDF. Anything that follows the finger, the sensor or the
> navigation stays in the app.

One line in `SheetExport` and one test, instead of a condition per effect. It settles two loose ends
in opposite directions: **the stamp does go into the PNG** of a complete plate, because it is a state
and not an animation; and **the gloss does not**, amending #303, which had reasoned that rest is a
defined pose and therefore composable. The consequence is accepted knowingly: what the father shows
other people carries no metal. The app is where he looks at his collection; the PNG is where he shows
it.

**The gloss belongs to the coin, not to the hole** — the second amendment to #303. Every coin
photograph glosses, die-cut or loose: on the plate, in the index, in «Las cifras», in `PieceCard` and
in the side sheets. All four share `CoinSides` (`FieldGuide.kt:334`), so the wider scope costs no
session — it is the same modifier. **Empty cardboard never glosses**, now for the direct reason: there
is no coin.

### 5. Density: three clauses, and a word costs what it costs times how often it is printed

The pruning ([#305](https://github.com/jenarvaezg/coindex/issues/305)) is **not a matter of wording,
it is a matter of place**: almost nothing goes for being long, it goes for being printed in the wrong
place, or 192 times. Furniture in the first fold of Collections drops from **56 to ≈22 words**.

The bar is three clauses and not one figure:

1. **Collections ≤ 25 words of furniture in the first fold** (56 today).
2. **No furniture string is printed per row, per slot or per card.** This is the clause that protects
   the result: the 19 convertible copy entries were expensive because they printed 192 times, not
   because they were long.
3. **Ajustes and onboarding are exempt by the frequency rule**, and in exchange are watched the other
   way round: none of their explanations may appear on a notebook screen.

> **The frequency rule.** A word costs what it costs **multiplied by the number of times it is
> printed**. Where a screen is visited once, a paragraph costs once, and avoiding a phone call pays
> for it. Where a string is printed per row, per slot or per card, no paragraph is worth it.

This replaces the «Ajustes is not pruned» exception, which **was written nowhere** and had a hole: it
did not cover onboarding, which is another 52 words on the first screen of a new phone. The rule
covers it without naming it, protects only explanation and not furniture — the `Ajustes` eyebrow, the
duplicated `Coindex v0.16.0` and the `Cerrar sesión` that is both title and button all still fall —
and can be written as a rule that a review can apply.

Two further rules come out of the same pruning:

> **The screen may say less than paper when the screen has form and paper does not.** The plate's
> 16-word tail line goes entirely — it lives only in `PlateScreen.kt:138` and never travelled to the
> PNG or to the 90 pages — and `Progreso` loses its label on screen while keeping it on paper, because
> the stamp lands on that very ratio.

> **One string, one owner.** Eleven literals for four strings today; ten chips and six words for «no
> filter», which become **`Cualquiera`**, one, in all ten; four wordings of «this collection no longer
> exists», which become the short one.

What the pruning changes beyond copy, with eyes open: **the budget disappears from the whole
interface** — the index line, the Ajustes meter *and* the `Techo de llamadas al mes` field. Nobody
decides anything looking at 6 of 1500. The ceiling becomes an internal value, and the
budget-exhausted message stops being able to say «you can raise the ceiling in Ajustes»; it says to
wait for the 1st. It is the only decision of this map that changes behaviour and not just copy.

**The `≈22` is counted on an HTML prototype, not on the emulator.** The real number is measured on the
AVD `coindex-ux` with `uiautomator dump`, as #296 measured it, and if it comes out above 25 **the bar
is adjusted with the measurement in front, not the measurement to the bar.**

### 6. Copy lives in one place, and two tests defend the promises

The bar **is not turned into a test** ([#306](https://github.com/jenarvaezg/coindex/issues/306)). The
diagnosis that changed the answer: the prose never went away — the 110 hand-written strings entered
**without a test**, and the eleven existing tests did not fail because they were not looking at them.
The hole was not a missing criterion, it was that **there are two places to write copy and only one
has tests.**

- **`CopyLivesInOnePlaceTest`** goes red if a literal containing letters reaches a visible slot
  (`Text(`, `text =`, `label =`, `placeholder =`, `title =`, `supportingText =`,
  `contentDescription =`) outside the twelve copy files. **51 sites in 13 files** today. It counts
  nothing; it forces every new string through the place that already has tests and shows up in a diff.
  **No exemptions** — not symbols, not Ajustes, not onboarding, not interpolations. A whitelist with a
  reason *is* the back door: nobody rejects a PR that adds one line with its comment, and in six
  months the list is the map of everything unwatched. The frequency exemption of §5 says **how much
  text is worth, not where it is written**; the two are not the same exemption.
- **`SinglePaletteTest`** asserts that `darkColorScheme` and `isSystemInDarkTheme` appear nowhere in
  `ui/`, and that `android:forceDarkAllowed` is `false` in `themes.xml`. It is born half red — the
  first half passes today, the second does not — which is how a test that declares an unkept promise
  should be born.

Said plainly: a new wall of prose written correctly *inside* `Labels.kt` passes green. The test
defends that prose is **visible in one place**, not the bar. The declared registry that would have
made the bar an `assertEquals` was chosen and then dropped: it meant moving 110 strings and rewriting
`Labels.kt` so templates were data — inventing mechanism for a two-user app, inside a map whose motto
is plan, do not do. **With one string and one owner, checking that a label prints per slot is reading
a file.**

### 7. The name of a coin is two strings, and it is derived, not curated

A coin's name comes from its ficha and **is not one string: it is two — the denomination and the
theme** ([#319](https://github.com/jenarvaezg/coindex/issues/319)). While they share a line, the
two-line block forces a choice between cutting the figure and cutting the theme. Separated — the
denomination on its line, the theme underneath at a smaller size, which is the hierarchy of a real
album's cartouche — **the ellipsis falls from 44 cards to two**, and the denomination never gets cut.

| | types | |
| --- | ---: | ---: |
| denomination fitting in 15 characters | 181 | **96 %** |
| theme on one line (≤ 20 characters) | 122 | 65 % |
| theme on two lines | 46 | 24 % |
| **theme that gets cut** | **2** | **1 %** |

The rule, in four steps: **(1)** the ruler is not deleted, it **falls to theme** and only when there
is no other — position decides, not identity, because `ruler` arrives in Spanish while the title is in
English and matches only 48 % of the time; **(2)** material and portrait tails go (`2 oz Fine
Silver`, `Bullion Coin(age)`, `Silver Proof`, `Nth portrait` — 29 of 187); **(3)** a quoted nickname
is theme, not denomination; **(4)** when a cut is needed, it bites the theme.

**It is not curated per type.** Numista's full title yields **181 distinct names out of 187** — three
«1 Bolívar», two «50 Céntimos» — and the pruned title yields the same 181, so curating 192 names, plus
one for every coin that arrives, would buy no distinction. What disambiguates is the year underneath.
**Abbreviations were chosen and then withdrawn**: with the two-range cartouche they save 0 cards of
187, which would be dead code, and they carried «10 Pesos» coming out as «10 $».

The name is **the same on all three surfaces** — `pieceTitle` (`PiecesSubject.kt:100`), including the
PDF, because the paper cell has the same form plus a QR, so §5's screen-says-less exception does not
fire. **The search box is the exception in reverse**: `CoinRow.searchable` keeps indexing the full
title, so «Elizabeth» still finds all 18. The accepted price is that a result may not visibly contain
the word searched for.

### 8. Amendment to ADR 0021 §1: the top level holds three sibling hierarchies

**Collections, Coins and «Las cifras».** What a cell earns is **having a grain of its own**
([#317](https://github.com/jenarvaezg/coindex/issues/317)).

> **The test — one clause that decides:** is its grain its own? If what is inside is what is outside
> with a different order applied, it is not a destination: it is a facet of the shelf.
>
> **Two symptoms that give it away when it is not:** the only name that fits **fights one that already
> exists** («tu colección» against «Colecciones»); and the count has to be **borrowed** — «El mundo ·
> 678» are slots of the sheet.

The symptoms are not independent gates: they are the two ways a borrowed grain shows, and they are
worth stating because they cannot be faked. Stated as three separate doors, a candidate with a pretty
name and a number to hand would pass by arguing only the first.

What changes in §1:

1. The title: from *two* sibling hierarchies to **three**.
2. *«A bottom bar of two destinations»* becomes **three cells**, in this order: **Colecciones,
   Monedas, Las cifras**. The app **still opens in Collections** and the third is last.
3. **Each cell names its grain with its count** — `Colecciones · 69` (cards), `Monedas · 192` (types),
   `Las cifras · 6,91 kg` (grams). The cell does **not** promise how many things are inside: it says
   what the destination is made of. «Las cifras» counts weight and **never money**.
4. The shelf invariant — filters, sort and live search, folded on entry — narrows to the hierarchies
   **with a list**, which are two of three. «Las cifras» carries no shelf: its order is chosen by the
   figure you touch.
5. **The shelf gains one facet, the axis** — by plate (the one there has always been), by country, by
   year.
6. The test above is written down, so a fourth candidate is examined rather than debated.

What does **not** change: the three consequences of §1 («Sin clasificar» as a filter of Coins, a coin
linking back to its collections, medals as a filter and not a section) follow from Collections **and**
Coins existing, not from their being exactly two; the **home screen that asks where you want to go
stays rejected**, and with three cells the argument is stronger, not weaker; and **§2 is untouched** —
there is still one species of collection. This amendment is about the bar, not the index.

### 9. The country map and the timeline are two axes of the notebook, not two screens

The stain and the axis are made of **slots**, so by the test of §8 they are orders of the same sheet,
chosen in the folded shelf ([#315](https://github.com/jenarvaezg/coindex/issues/315),
`docs/ux/atlas-315.md`). The order is `indexOrder()`, not a new rule, and the **default axis is «by
plate», which is today's Collections**: the app still opens the same and nobody has to learn anything.

| axis | one cell is | at once | screens | words |
| --- | --- | ---: | ---: | ---: |
| by plate *(today's Collections)* | a collection | 12 plates | 5.40 | 31 |
| **by country** | a slot | **390** (422 with the shelf folded) | 2.25 | 15 |
| **by year** | a year | 112 cells | 1.62 | **3** |

The year axis has **three states and not two** — coin, ghost hole and **bare cardboard** — and the
third is what shows the shape of a collection without a word, like the father's 62 consecutive empty
years (1813→1876). Sorting by ratio opens the sheet on **Italia 2/2** rather than Rusia 3/280:
*it reveals, it does not reproach*, the same criterion as the stamp.

Four defects the prototype found, which are implementation obligations and not taste: the year axis
**painted eleven years empty in which the father owns a coin** (93 of 112, not 78 of 104); **1316 and
1375 were Hijri years** stretching the axis to 711 years; a slot was painted in the **catalog's**
country rather than the **member's** (#170), which is what makes Nueva Gales del Sur and Tokelau
appear; and the «loose pieces» band makes no sense on an axis where every piece has a country.

**A piece has two years, and the interface reads only one today.** The rule:

> **To match a slot, the coin's year** (`recordedYear`, which prefers `issueYear`). **To place a piece
> on an axis, the Gregorian one** (`gregorianYear ?: recordedYear`). The domain already carries both
> fields, so this is not new mechanism — it is saying which one each thing reads.

And a third reading, from [#326](https://github.com/jenarvaezg/coindex/issues/326): **23 pieces carry
no year at all** — undated Portuguese escudos, a Roman denarius — and the 1,756-year arc exists only
if they inherit their type's minimum year. Without that rule it is 246 years.

### 10. «Las cifras»: money opens the page, and matter is ordered in ladders of referents

The analytics page is a hierarchy and not a dashboard, and what makes it one is that **you go down**:
every figure that can be touched leads to the pieces or the plates that compose it. Collections orders
the collection by plate, Coins by type, **Las cifras by magnitude**.

**A piece is worth the maximum of three numbers** ([#316](https://github.com/jenarvaezg/coindex/issues/316),
`docs/ux/cifras-316.md`): its silver floor, its Numista market price **for its grade**, and what was
paid — covering 98 %, 96 % and 16 % of pieces, with the maximum reaching **99.5 %**. This is not an
occasional tie-break: catalog prices **do not follow the metal**, so the order inverts as spot rises —
today the market wins on 517 of 572 pieces and silver on 14; at 34 % more spot, silver wins on 338.
The `grade` field, which the ticket thought was an analytic, is the **pricing key**: Numista publishes
price per issue and grade on an endpoint the app does not call.

> **What is read piece by piece or plate by plate is a shopping companion. The same thing, totalled
> for the whole collection, is wealth management.** The premium on one piece is the scale of that
> purchase; summed, it is a portfolio's return. The cost of completing one plate is a plan — twelve of
> his plates are one to three slots from closing; for the whole shelf it is «you are tens of thousands
> of euros short», which is the opposite of *reveal, do not reproach*.

The page's form ([#326](https://github.com/jenarvaezg/coindex/issues/326), `docs/ux/cifras-326.md`):

- **Money opens the page**, which does not contradict #316: what was rejected there is an amount that
  changes on its own in a permanent bar — a pocket ticker. Here it is on a page you opened on purpose,
  with its origin stated and the spot's timestamp. The cell's count is still weight.
- **Matter is ordered in three ladders of referents** — weight, row and stack — five referents each
  with the collection interpolated between two, because **the comparison does not decorate the figure:
  it is the figure**. «6.95 kg» says nothing; «more than a cat and 310 g short of a bowling ball» does.
  The scale is **ordinal, not metric** — logarithmic piled three labels on top of the fourth — and for
  that same reason it **carries no zoom**.
- **Metal is split by mass, not by coin**: silver 5.975 kg (86 %) against 963 g of copper (14 %); by
  coin it would be 565 of 574, a bar of one colour.
- The house bet fell: **the tower drawn to honest scale** — 574 coins of 26.6 mm stacked — is an
  8 px needle spending 250 px of the first screen.
- **«A una casilla» does not live here.** It is what is missing, not what is there, and its place is
  the plate header.
- Four figures nobody asked for, out of the ficha already in the APK: **75 % are no longer money**,
  **246 were engraved by the same hand** (Barre, 43 %), **296 came out of Paris** from 51 mints, and
  **210 are dated 1960**; plus the smallest coin against the largest at the same scale.

Money at export is **the sixth switch** of the configurable export (#228, ADR 0021 §13). Turning it
off is **not** hiding the amount section: there are derived figures that are also money — «Venezuela ·
30 % of the value» leaked through with money off.

### 11. Prices arrive in one pass, and the total is never shown half-done

One valuation pass, paid by each phone ([#327](https://github.com/jenarvaezg/coindex/issues/327)):
the **223 issues he owns** plus the holes of the plates **≤10 slots** from closing — **487 calls**,
24-32 % of the monthly budget. The threshold is not a saving, it is §10 applied: a plate with 51 holes
does not have a cost of completion, it has a reproach of 51 slots.

The rules that belong to form rather than to networking, and therefore to `spec.md §0.4`:

1. **The total is never shown half-done.** While the market price is missing, the money section **is
   not there** — no struck-through number, no provisional total. Without the market,
   `max(silver, paid)` gives 10,500 € of the real 16,800, which is literally the «only the silver
   floor» #316 rejected. A total at 60 % is not incomplete, it is **false**.
2. **Coverage yes, progress no.** «The value of N of your 574 pieces» is said; «I have 140 of 223» is
   not.
3. **«Las cifras» opens whole without a single call.** Weight, matter, the ladders, the arc, the
   emitters and size all come out of the APK (`weight` and `composition` at 100 %). The local-first
   promise of ADR 0024 extends to this page; money is the only thing that arrives late.
4. **Every number brought from outside is shown with the date it was last read**, and an expired
   figure keeps being shown rather than deleted. Already written for spot in #316; here it becomes a
   general rule.

Also settled there, and recorded because it would vanish silently otherwise: **`issue_id` is not
stored today** — `IssueDto` parses only `year` and `gregorian_year` — and without it every piece costs
an extra call to `/types/{id}/issues` just to find out. When and how the pass runs is the exact
sibling of ADR 0024 and belongs in **its own ADR**, written by the session that builds «Las cifras»,
not in a paragraph of this one.

### 12. Amendment to ADR 0021 §9: the eyebrow of a card is no longer the country

§9 settled that the eyebrow vacated by «Propuesta de colección» is the country, taken from
`issuer_code` (`IndexScreen.kt:448` says so literally). **It dies with the card's four lines.** The
argument: the eyebrow existed to give hierarchy to a flat card; with the hole, hierarchy is the
die-cut's job and the country goes back to being what it always was — **a facet**. It survives as a
shelf facet (which is how «mis venezolanas» is actually looked for), in the plate's ficha, on paper,
and often in the photograph itself.

The **variant line** (`peso · acabado`) dies with it and converts into nothing. `data/` contradicted
the ticket: of 74 catalogs, **73 have distinct `short_name`**, and in every real twin the variant is
already in the name — `Noah's Ark 1 oz` / `½ oz` / `¼ oz`, `5 Reichsmark .500` / `.900`. In **49 of 68
cards** it does not even repeat, because the finish is null and it says `Acabado sin confirmar`, which
talks about a hole in the ficha and not about the coin. The residual risk closes where it is born: if
a catalog ever arrives whose `short_name` omits the variant, that is **a curation rule**
(`curate-catalog` disambiguates in the name), not a line on 70 cards.

The rest of §9 stands whole: the destination is still chosen by the capability of §3, the plate still
does not merge, box maintenance is still an `if`.

**ADR 0023 does not fall with it.** The cured country name is still needed — by the shelf facet, by
the plate's ficha and by paper — so `readsAsACountry` and `CardCountriesTest` stay as they are. What
loses its subject is the **third clause of its width rule** («no more than the 40 characters the
`short_name` below it is capped at»), which was an argument about a card that no longer prints an
eyebrow; it survives as a facet-chip argument, which is a narrower one. And the open half of #170 —
what a card prints when a catalog spans two issuers — **stops being a question about the card** and
becomes one about the country axis, where §9 already obliges reading the **member's** country and not
the catalog's. `CONTEXT.md` keeps «Card country» as a term with its definition intact and loses «the
country a card says above its name» as its first surface.

### 13. A coin gets an inside: the hole in Coins opens a sheet

**A coin had no screen.** In `CoinsScreen.kt` the only tappable thing inside a card is the links to
its collections; the card itself leads nowhere. That is why the maintenance toll lives *outside*, in
the grid — **16 of the 37 content words of Coins (43 %), printed 192 times**; 25 of 56 (45 %) in
Pieces.

The hole in Coins **opens a bottom sheet with the coin's ficha**, and inside move: `Actualizar la
ficha · 1 llamada` (from 192 impressions to one — the action is untouched, its frequency is not), the
`Ficha traída hoy` / `hace N meses` line **with words**, `Ver en Numista`, and the links to its
collections. `En ninguna colección` stays outside **as form**: a hole with no cardboard behind it.

Yes, this is new mechanism, and this map does not invent mechanism. The distinction that justifies it:
a «by country» sort order would have been inventing a place to put a line that was surplus; this is
**giving an inside to a screen that has none**, and without it the toll has nowhere to move — either
it stays on 192 rows, or a capability the father uses is lost.

### 14. Licence notices: three words in Ajustes, one screen with everything inside

Three subjects and not two ([#323](https://github.com/jenarvaezg/coindex/issues/323)): **Numista**
(the 916-ficha, 3.1 MB seeded cache plus the downloaded photographs), **software** — the real release
classpath is **205 artefacts from 56 groups**, not the nine of `libs.versions.toml`, and among them
travels `org.slf4j:slf4j-api`, which is **MIT**, brought in by Ktor — and **the typefaces**, OFL 1.1.
A notice written by eye would have said «all Apache 2.0» and been false the day it was written.

The full texts ship **inside the APK** by obligation (Apache 2.0 §4(a) and OFL 1.1), and because
Coindex is a sideloaded APK that self-updates against releases (ADR 0011), a `LICENSES.txt` in the
repository does not travel with it. They live **as assets and not as literals**, which is what keeps
them out of `CopyLivesInOnePlaceTest` (§6). The entry is `Avisos y licencias`, three words at the foot
of Ajustes, in the hole left by the `Coindex v0.16.0` the pruning killed as a duplicate — **no
subtitle**, because the entry pays the frequency rule while the screen it opens is exempt. Maintained
by hand, by family and without versions, plus a Gradle task and a test that goes red when a group
appears with no notice — because what sneaks in does not pass through `libs.versions.toml`.

Numista's §4 obligations — show the N# (already done), attribute the source «where an ordinary End
User can readily identify the source» (this screen), and **preserve third-party copyright notices** —
are met by the first two. The third is **knowingly not met**: photograph credit was closed `wontfix`
on 8 August 2026 by the owner's decision (two collections on two phones, no public surface). It is a
conscious breach recorded as such, not a loose end.

### 15. What «approved» means, and what an implementation session may change

**None of the six prototypes has been seen on a phone.** The four form reports were decided in HTML,
and this ADR records that debt rather than hiding it: **approved here means approved in HTML.** Every
effect passes through the AVD before entering production, and for that there is a bench.

**The calibration bench** is #303's HUD ported to Compose. It lives **only in `debug`**, ships in no
release and adds no word to Ajustes; it paints one real slot — the 1 Bolívar · 1960 — with the
parameter controls to hand. It is **written once and serves all five**, and each calibration is half a
session ending in AVD captures and video, with **the number chosen before the production effect is
written**.

| parameter | approved value | who decides |
| --- | --- | --- |
| grain opacity of the paper | 96 dp mosaic in `soft-light` at 0.75 (#351; was a 256 px mosaic at 0.08) | the bench — and if the grain is indistinguishable at 1:1 it is withdrawn without reopening #300 |
| gloss intensity and travel | half the video's, ±55 dp at 105° | the bench |
| flip duration | 420 ms | the bench |
| stamping duration | 300 ms | the bench |
| depth of the tag's recess | sunken, 48.3 × 28 dp | the bench (the size is not a parameter: it is what reaches Android's 48 dp) |
| the ghost | design at 14 % with a dotted rule | the bench |
| **the gesture, the place, the drawing** | — | **the map**: an implementation session never changes form without coming back |

> **Amended on 2026-08-09 (§15, #351).** The bench's rule stands whole, and it fired: the approved
> 256 px mosaic at 0.08 measured **indistinguishable at 1:1** — 4.17 % of the pixels of an empty
> region, 12 levels of amplitude, 0.78 of standard deviation — and the mosaic repeated exactly under
> a one-tile shift. What the rule offered was withdrawal; the owner chose the other reading of the
> same sentence, **raise it until it is distinguishable**, and the three costs #351 priced were paid:
> variation per tile, the tile measured in dp, and the paper as one surface painted in `CoindexTheme`
> and reaching the plate and the PDF. The calibrated value is a 96 dp mosaic of 2,600 fibres at 0.75,
> and the grain now touches 62 % of the pixels of the same kind of region. The numbers, including a
> drawing cost that regressed on the emulator and is not measured on a phone, are in
> `docs/ux/implementacion-351/`.

### 16. The order, the cost, and three PRs per screen

**19.5 sessions in eleven blocks, ordered by foundation and not by wow**: each block holds up the
next, and nothing is written twice.

| | block | sessions |
| ---: | --- | ---: |
| 1 | the type, the three glyphs and the licence notices (§14) | 2 |
| 2 | **the calibration bench** | 1 |
| 3 | Collections: holes, sewn edge, strip, grained paper | 2 |
| 4 | the plate: holes by year and the ghost | 1 |
| 5 | Coins, with the name of §7 | 1.5 |
| 6 | the flip and the tag | 1.5 |
| 7 | the gloss | 1.5 |
| 8 | the stamp and the journey | 2.5 |
| 9 | the three axes: country, year and `gregorianYear` | 2 |
| 10 | «Las cifras» — does not start until the valuation ADR of §11 exists | 3 |
| 11 | the remaining pruning (§5) and the two tests (§6) | 1 |
| | **total** | **19.5** |

**The wow is not bringable forward, and this is a fact and not a preference**: the gloss is a gradient
inside the hole's circular clip and the flip turns the coin inside the hole. Today's slot paints both
faces side by side (`CoinSides`, `PlateScreen.kt:177`), so there is no clip to put a gloss in and no
hidden face to reveal. Without §1's sheet, both effects would have to be written twice.

The big block rewrites `IndexScreen` (692 lines), `PlateScreen` (224) and `CoinsScreen` (377). It goes
in **three PRs — Collections, the plate, Coins** — and not one of ~1,300 lines nobody can review.
`main` is left half album sheet and half listing twice, and that is fine: **the only person who
installs is the father, and he does not see `main`, he sees a release.**

## Consequences

- **The first four movements in the life of the app enter at once**, in an app where
  `animate|SharedTransition|graphicsLayer|BlendMode` returns zero results today. The API risk is one:
  `BlendMode` at API 29, which is the `minSdk`; the accelerometer's battery cost **has not been
  measured and is not faked** — what is fixed is the ceiling, never awake outside the foreground.
- **One behaviour change, not just copy**: the call ceiling becomes an internal value and disappears
  from the interface, field included (§5).
- **The bottom bar grows to three cells** and a page that does not exist today has to be built (§8,
  §10), which is the one thing this map produced that is a new capability rather than a redrawing.
- **The curator inherits one rule**: `short_name` must carry the variant, because the card no longer
  prints it (§12).
- **Two tests are born, one of them half red** (§6), and 51 sites move to the copy files as part of
  the pruning block.
- **What the father shows loses the metal**: the PNG carries the stamp, the paper and the die-cut, and
  not the gloss (§4).
- **Nothing here was seen on a phone** (§15). The first implementation session starts by confirming
  the sheet on the AVD, and parameters are adjusted with the measurement in front rather than
  reopening decisions.

## Documents this ADR changes

- **ADR 0021 §1** is amended by §8: two sibling hierarchies become three, the shelf invariant narrows
  to the hierarchies with a list, and the shelf gains the axis facet. **§9** is amended by §12: the
  eyebrow stops being the country. **§13** is extended by §4's export rule and by the sixth switch of
  §10. **§2, §3, §6, §7, §10, §11 and §12 are untouched.**
- **ADR 0010 §8** (the plate as a PNG) is upheld and gains the export rule of §4.
- **ADR 0023** stands whole (§12): the cured country name is still needed by the shelf facet, the
  ficha and paper. Only the third clause of its width rule loses the card as its subject.
- **`CONTEXT.md`** keeps «Card country» with its definition and loses its first surface; it gains the
  vocabulary of §1 (hole, ghost, sunken tag, stamp), §5 (furniture, the frequency rule) and §8 (grain
  of a cell, axis of the shelf).
- **ADR 0020** (`printed_side`, what a catalog claims) and **ADR 0018** (metal in the variant key) are
  upheld and leaned on: the first decides which face rests, the second is why there is no metal tint.
- **ADR 0024** is upheld whole and extended in spirit by §11.3: a page opens without a call.
- **`spec.md §0.4`** is rewritten: it stops declaring an aesthetic that is not built and starts
  describing what is built and what is approved, pointing here.
- **A new ADR is owed**, not written here: when the valuation pass runs, what expires and what yields
  to a sync (§11), by the session that builds «Las cifras».

## Alternatives considered

Each rejection was argued in its ticket; they are gathered here so the list is one and not six.

- **The lamp — dark theme, warm** (#301). Not buildable on an emissive screen without inverting the
  ink; what survives is the dashboard `spec.md §0.4` forbids. With it fall the Ajustes switch and any
  paper forced at export.
- **The header parallax** (#307), by the ceiling rule of §3: cause, no datum. It is the whole «minor
  movement» question closed without drawing it.
- **Flipping the whole sheet** (#302) — the physically honest gesture and the only one that could
  export a PNG of obverses. A real sheet **inverts the column order** when turned: keeping the years
  in place is cheating, inverting them makes a date run's grid dance. With it fall no-flip, the
  cross-fade (which brings back the `anverso`/`reverso` labels the die-cut had just removed) and
  hold-to-see.
- **Seven ways of glossing** (#303), of which two are worth recording: **following the relief by
  luminance** cannot discriminate, because silver is light everywhere — what it would need is a height
  map, and that is the relief #15 discarded; and the **narrow flash**, the most visible of the eight,
  reads as a scratch on the acetate rather than as the coin's surface.
- **The stamp as a dated fact**, stepping out at sync time, at the foot of the sheet (stamped at 706 px
  of scroll, off screen), the cascading brass (the prettiest and the one that best survives the PNG,
  but nobody has been taught that brass means complete) and the gummed label (#304).
- **The world map** (#315): it colours 15 of 37 emitters and needs **81 words** to excuse the other 22
  — Tokelau finishes it off, with a plate and no polygon. With it fall the per-country mini-maps, the
  phenology bars, the strip with breaks and the country table, which is a screen with zero coins.
- **Calling the Collections cell «láminas»** (#315): 20 of the father's 69 cards have no plate to open.
- **A fifth cell for the stain and the axis** (#315, #317): it fits — five 13.5 px small caps enter
  411 dp — and it is not needed. What sinks it is that the only name that served, «tu colección»,
  fought «Colecciones».
- **The declared copy registry with slots and templates** (#306), chosen and then dropped as
  disproportionate; and **measuring real width from the TTF** with `java.awt.Font`, which promises
  pixels while measuring another engine; and **booting an AVD per PR**.
- **A curated `short_name` per coin type** (#319): 192 names today and one more per coin that arrives,
  buying zero distinction. With it fall the six abbreviations, which save 0 cards of 187.
- **Only the silver floor**, a total of what was paid, the aggregate premium and the total cost of
  completing (#316); and **calling the page «Analíticas»**, which is dashboard vocabulary.
- **The tower drawn to honest scale**, the comparison written as text, the colophon without a drawing,
  and zoom on the ladders (#326).
- **`Créditos` as the Ajustes entry** and a licence-notice generator plugin (#323): the first does not
  announce where the legal text lives; the second produces 205 entries, 150 of them `androidx.*`.
- **Lazy per-plate valuation** (#327): pricing all 1,182 slots is 2,036 calls and does not fit in the
  month even once, because a member stores `numista_type_id` and `year` and never the `issue_id`.
- **Own photographs of the pieces.** Discarded whole in
  [#15](https://github.com/jenarvaezg/coindex/issues/15) on 7 August 2026, and recorded here because
  it was the one promise still alive in the **original** `spec.md` — «en Fase 2 las piezas propias se
  fotografían nosotros», which lives in `rust-frozen:spec.md` and therefore in a document that stopped
  being the specification on 29 July 2026. **Today's `spec.md` never promised it**: `grep -i foto`
  returns nothing. The withdrawal is this line, so nobody goes looking for it in a frozen tag and
  takes it for a commitment. Relief and interactive relighting (shape-from-shading, RTI) stay
  discarded with it: §3's gloss is a material effect, not a reconstruction.
