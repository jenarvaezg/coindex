# ADR 0026: The shape of Coindex — an album sheet, and what moves owes a datum

- Status: accepted; amended by #304, #400 and #419 (§1), #338 (§1, §3, §4, §15), #339 (§3, §5),
  #342 (§6), #351 (§15), #357 (§15), #370 (§3, §13), #371 (§4), #381 (§3), #473 (§3), #281 (§8),
  [ADR 0029](0029-a-wish-is-an-empty-slot-marked-on-the-phone.md) (§4),
  [ADR 0030](0030-the-shelf-window-of-explorar-is-valued-by-hand.md) (§8 clause 4), #508 (§3),
  #514 (§3, §4), #516 (§8), #520 (§8 clause 3, §15), #521 (§1, §5 clause 3, §14), #556 (§15)
- Date: 2026-08-08
- Amends ADR 0021 §1 (three top-level hierarchies) and §9 (the card's eyebrow stops being the
  country). Extends ADR 0010 §8 and ADR 0021 §13 with an export rule. Upholds ADR 0018, 0020, 0023
  and 0024. Written by map [#278](https://github.com/jenarvaezg/coindex/issues/278); each
  decision's measurement lives in its ticket and its report under `docs/ux/`.

## Context

On 7 August 2026 the collector's son, the app's only other user, called v0.16.0 «un par de listados,
algo muy simplón»: no wow, and too much prose for a phone. The information architecture (ADR 0021)
was not the problem. Only the plate had ever been designed, and the aesthetic `spec.md §0.4`
declared — «serif para los textos, tipografía condensada para los datos, paleta apagada de papel» —
had never been built. The baseline ([#296](https://github.com/jenarvaezg/coindex/issues/296),
`docs/ux/medida-base-296.md`): 2.16 cards per index screen, 69 % of an arrival screen's words were
furniture, no photographs in the index, no packaged fonts, no animation.

This ADR executes that declared identity rather than choosing a new one. Two constraints shaped it:
the user who matters is the father, whose main gesture is exporting a plate as a PNG, so an effect
that exists only in motion never reaches where he shows it; and density and wow are one task, since
the room for the die-cut, the stamp and the flip comes out of the pruning.

## Decision

### 1. Coindex is an album sheet, not a listing

A collection is a **die-cut hole with its coin inside**; the plate is the same sheet arranged by
year, with the design as a ghost where a piece is missing. The index goes from 2.07 to 11.04
collections per screen (#300, `docs/ux/hoja-300.md`).

- **Bitter + Barlow Condensed ship in the APK** (245 KB, +0.81 %, #298). Bitter costs no width
  against Noto Serif; Barlow brings real small caps and tabular figures, retiring the faked `smcp`
  in `Theme.kt`. No subsetting, and no italic because `ui/` uses none.
- **`←`, `✓` and `↗` are vector icons**: neither face has them.
- **Paper is fine fibre in `soft-light`, flat, with no sheet shadow**; the die-cut gives the relief.
  The acetate reflection inside the hole was withdrawn by #338 (§15).
- **Numista's photographs need no treatment**: a round hole with `cover` cropping hides the cut-out.
- **The card's photograph is the first issue the collector has**, not the catalog's first (#304).
- **Search, filter and sort live in a 76 dp strip**, identical for Collections and Coins. The shelf
  has no «cerrar»: the whole row is the control.
- **A sewn edge replaces the masthead**: `70 colecciones · 574 piezas · 192 tipos` and one icon.
  The middle count is **piezas**, never «monedas», which names the sibling hierarchy, and the counts
  are assembled once above the roots (#400); no wall-clock minute (#419). The icon opens **«Este
  teléfono»**, inventory maintenance with the credentials at its foot (#521, §14).

### 2. Paper at any hour: there is no dark theme, and the promise is declared

Coindex does not follow the system's dark theme (#301). A «lamp» where the paper dims and the ink
does not invert cannot be built on an emissive screen: legible ink has to lighten, which is
inverting, and what is left is the dashboard `spec.md §0.4` forbids.

- **`android:forceDarkAllowed` is `false` in `Theme.Coindex`** (API 29, the `minSdk`). Manufacturer
  force-dark acts beneath Compose and would turn the paper grey under untouched photographs. On a
  phone set to dark, Coindex is the one light screen, by promise.
- **The shielding lives in the theme, never in the export path**: a `ForcedPaperTheme` around
  `OffScreenSheet` would export lightened ink on paper the day a lamp existed.
- **No switch**, since there are not two states; night belongs to the system («Extra dim»).

### 3. The ceiling is measured in movements, and each one owes a cause and a datum

**The rule** ([#307](https://github.com/jenarvaezg/coindex/issues/307)): a movement enters if (a) it
answers an act of the collector **and** (b) it says something the resting state does not. It judges
future movements without drawing them, and rejected the header parallax (cause, no datum). Of the
prototypes' twenty-one candidates, sixteen are still — they are how the sheet is drawn — and four
movements are approved:

| movement | cause | datum | to paper? |
| --- | --- | --- | --- |
| **the flip** — `rotationY` + `cameraDistance`, 420 ms; the cardboard stays still | tapping the hole | the other face | no · the PNG comes out on `printed_side` |
| **the gloss** — a black→transparent→white gradient at 105° in `BlendMode.Softlight`, moved by the accelerometer | tilting the phone | it is metal | **no** |
| **the stamping** — the ink falls on the ratio when a complete sheet is opened | opening the sheet | it is complete | the stamp yes, the stamping no |
| **the journey** — the coin flies from the card to **its** slot, `SharedTransitionLayout` | navigating | it is the same coin | no |

The gloss fails (a) and earns its place on (b): it is the one thing saying the coin is metal.

**The flip** (#302, `docs/ux/giro-302.md`) fires on a tap on the hole's body, so a casilla has
**two targets**: the hole flips, and the year tag opens the coin (#508 below). The tag is
**sunken**, 48.3 × 28 dp, the one candidate whose drawing reaches Android's 48 dp. The resting face
is `printed_side` (ADR 0020), as on paper; the flip is momentary and the PNG is never mixed.

**The gloss** (#303, `docs/ux/brillo-303.md`) tilts the surface rather than lighting it. No AGSL
(`BlendMode` is API 29) and no metal tint (ADR 0018 puts metal in the variant key, so plates are
single-metal). The accelerometer is registered only in the foreground with a coin on screen, at
`SENSOR_DELAY_UI`, and released on `onPause`. Intensity is half the prototype's: the photograph
carries its own light.

**The stamp** (#304, `docs/ux/ceremonia-304.md`) **is a state, not an event**, so there is no
«just became complete» to remember. It is read from the inventory like the die-cut: 84 × 76 dp of
rubber stamp in `multiply` rotated 5.5°, landing on the ratio the header already shows (`22/22`), so
it adds no word or figure. It is stamped **when the sheet is opened, never on sync**, and **only on
the plate**: on an index card it would fire on scroll. The word is **«completa»**, including for an
open series. The father's complete plates were complete since curation: the ceremony reveals.

**The journey** flies the coin only where it is the protagonist: its slot on a plate, and its ficha
sheet in Monedas (#370, §13). Not into `Pieces` or a box, where the other end is inventory rows and
landing on the first of twelve would be a lie. The cards that do not fly carry no ratio (ADR 0021
§3), so the difference is visible before touching.

- **Amended by #339 (2026-08-09): the sheet stamps every time it is opened.** #304's one bit per
  catalog in `NamedValues` is withdrawn: it would make the stamping the one movement that runs out,
  and ADR 0021 §7 stores nothing per card. Re-stamping a seen sheet is the cheaper mistake.
- **Amended by #370 (2026-08-10, with §13): the ficha sheet is the second journey's landing**: a
  104 dp die-cut above the title, with cardboard only when a collection claims the type.
  `ModalBottomSheet` is a dialog window and cannot host a shared element, so the sheet is a Compose
  overlay under the same `SharedTransitionLayout`. The catalog journey (`coin-$id`) and the type
  journey (`type-$id`) keep separate keys: one type can be a card cover and a Monedas cell.
- **Amended by #381 (2026-08-10): every route paints `paperSurface`, and only the leaf on top is
  animated.** Both ends are composed during the flight, and transparent destinations washed out or
  double-exposed; the grain is anchored to the window (#351), so opaque leaves fall in register.
  Coming back, the plate is still on top and fades out in 180 ms. `docs/ux/implementacion-381/`.
- **Amended by #473 (2026-08-12): the tag hangs off the hole and the name goes under it.** The tag
  sits ten dp under its hole, so a row's tags align by construction. Before, a name box in `sp`
  above a gap in `dp` let the year read as the label of the row below once the collector enlarged
  the type; nothing in a casilla is measured in `sp` now. The owner's call (form), over widening the
  row gap: `docs/ux/prototipo-473/`, `docs/ux/implementacion-473/`.
- **Amended by #508 (2026-08-15): the year opens the coin's sheet, not Numista** — Monedas' sheet,
  over the lámina, with «Ver en Numista ↗» and «Actualizar la ficha · 1 llamada» (whose label prints
  its cost, per ADR 0028 §3 and ADR 0030 §1). The audit of 14 August 2026 left for Chrome by
  accident, behind Cloudflare. **Nothing without a ↗ in its label opens a browser**, the annex's
  list of marked casillas included (ADR 0029 §6). The coin does not fly into this sheet: its hole is
  already one end of the plate journey.
- **Amended by #514 (2026-08-16, with §4): a system asking for quiet keeps the ceremonies from
  starting.** At `ANIMATOR_DURATION_SCALE` = 0 a shared element still shows its photograph at the
  take-off for one lookahead frame, which no scale divides. So **every switch the app owns is
  thrown**: the journeys do not run and neither end yields its photograph, the sheet has no
  entrance, `rememberInkFall` finds the ink dry, and the sensor is not registered
  (`CoinTilt.Still`). The casilla flip (#337) and the return fade (#381) are left to Compose: their
  leaked frame shows the previous state. `docs/ux/implementacion-514/`.

### 4. The export rule: what is still travels to paper, what is alive does not

**Anything still travels to the PNG and the PDF; anything that follows the finger, the sensor or the
navigation stays in the app.** One line in `OffScreenSheet` and one test, not a check per effect.
So **the stamp goes into the PNG** of a complete plate, and **the gloss does not** (amending #303):
what the father shows other people carries no metal.

**The gloss belongs to the coin, not the hole.** Every coin photograph glosses, die-cut or loose,
through one modifier, `Modifier.coinGloss()`, shared by `AlbumHole` and `CoinSides`. Empty cardboard
and the ghost of a missing issue never gloss.

- **Amended by #371 (2026-08-10): the stamp reaches the PDF.** It is drawn in millimetres on each
  plate's heading, not by reusing the dp-sized `StampedRatio`; a shared folio (#232) stamps each
  complete plate in its band, and the slim heading gets a smaller frame.
- **Amended by [ADR 0029](0029-a-wish-is-an-empty-slot-marked-on-the-phone.md) (2026-08-14): the
  wish mark travels, and gets an output of its own.** It is a state at rest, printed as the slot's
  `state` line. Since «Explorar» has no «Exportar» (#282), **«la lista de lo que busco»** is
  exported from the annex (§8) as one more `PrintSection` under the same switches, with its own
  eyebrow: a page of coins nobody owns may not say «COLECCIÓN» (#275).

### 5. Density: three clauses, and a word costs what it costs times how often it is printed

The pruning ([#305](https://github.com/jenarvaezg/coindex/issues/305)) is about place more than
wording: a string goes because it is printed in the wrong place, or once per row. Furniture in the
first fold of Collections drops from 56 to ≈22 words. The bar:

1. **Collections ≤ 25 words of furniture in the first fold.**
2. **No furniture string is printed per row, per slot or per card.** Copy is expensive because of
   how often it prints, not its length.
3. **A screen visited once is exempt by the frequency rule**, and none of its explanations may
   appear on a notebook screen. Today: «Este teléfono», «Credenciales», onboarding and «Avisos y
   licencias», which follow from the rule rather than defining it (#521).

**The frequency rule.** A word costs what it costs multiplied by the times it is printed. On a
screen visited once, a paragraph that saves a phone call pays for itself; in a string printed per
row, slot or card, none does. It protects explanation, not furniture.

- **The screen may say less than paper when the screen has form and paper does not.** The plate's
  16-word tail line goes entirely.
- **One string, one owner.** One «no filter» word for all ten chips, **`Cualquiera`**, and one
  wording of «this collection no longer exists».
- **The budget leaves the interface** — the index line, the Ajustes meter and `Techo de llamadas al
  mes`. The ceiling is internal, and the exhausted message says to wait for the 1st. This is the
  map's one behaviour change.

The ≈22 was counted on HTML; the real number comes from `uiautomator dump` on the AVD `coindex-ux`,
and above 25 the bar is adjusted with the measurement in front, not the measurement to the bar.

**Amended by #339 (2026-08-09): the ratio leaves the specification wherever the stamp reaches it.**
Screen and exported sheet show `22/22` at the title with the ink over it, so «Progreso · 22 / 22
emisiones» below would print the figure twice. The printed notebook keeps the row: its page has no
header to raise the figure into. «1 anunciada» and «2 no medibles» stay everywhere.

### 6. Copy lives in one place, and two tests defend the promises

The bar is **not** a test (#306). Prose came in through hand-written strings no test looked at: of
the two places copy is written, only one had tests.

- **`CopyLivesInOnePlaceTest`** goes red if a literal containing letters reaches a visible slot
  (`Text(`, `text =`, `label =`, `placeholder =`, `title =`, `supportingText =`,
  `contentDescription =`) outside the copy files, so every string goes through the place that has
  tests. **No exemptions**: a whitelist with a reason is a back door. §5's frequency exemption says
  how much text is worth, not where it is written.
- **`SinglePaletteTest`** asserts that `darkColorScheme` and `isSystemInDarkTheme` appear nowhere in
  `ui/` and that `android:forceDarkAllowed` is `false` in `themes.xml`.

The first defends that copy is in one place, not the bar: prose written inside `Labels.kt` passes.
A declared registry that would make the bar an `assertEquals` was dropped as disproportionate.

**Amended by [#342](https://github.com/jenarvaezg/coindex/issues/342) (2026-08-11).**
`SinglePaletteTest` is a regression guard (block 1 had already set `forceDarkAllowed`), and its
second half holds #349's contrast floors (`Paper.muted` 4.44, `Paper.hairline` 2.28).
`CopyLivesInOnePlaceTest` reads whole arguments, because a line-based grep misses strings on the
next line or behind a conditional. `Text(` matches on a word boundary (`setContentText(` is a
notification); `Eyebrow(` and `Facet(` are slots, since a wrapper moves the slot, not the copy.
**The list only grows.** A literal whose only letters are inside interpolations (`"$label · $it"`)
is not prose; `"Colecciones · $collections"` is.

### 7. The name of a coin is two strings, and it is derived, not curated

A coin's name is **the denomination and the theme**
([#319](https://github.com/jenarvaezg/coindex/issues/319)). Sharing a line, one of them gets cut;
separated — denomination on its line, theme underneath, smaller, like an album cartouche — the
ellipsis falls from 44 cards to two and the denomination is never cut.

| | types | |
| --- | ---: | ---: |
| denomination fitting in 15 characters | 181 | **96 %** |
| theme on one line (≤ 20 characters) | 122 | 65 % |
| theme on two lines | 46 | 24 % |
| **theme that gets cut** | **2** | **1 %** |

**(1)** The ruler is not deleted: it **falls to theme**, only when there is no other, by position,
because `ruler` arrives in Spanish and the title in English. **(2)** Material and portrait tails go
(`2 oz Fine Silver`, `Bullion Coin(age)`, `Silver Proof`, `Nth portrait`). **(3)** A quoted nickname
is theme. **(4)** A cut bites the theme.

**Not curated per type**: full and pruned titles give the same number of distinct names, and the
year disambiguates. Abbreviations saved no card and turned «10 Pesos» into «10 $». The name is **the
same on all three surfaces** (`pieceTitle`), PDF included; only the search box indexes the full
title (`CoinRow.searchable`), so a result may not visibly contain the word searched for.

### 8. Amendment to ADR 0021 §1: the top level holds three sibling hierarchies

**Collections, Coins and «Las cifras».** A cell is earned by **having a grain of its own**
([#317](https://github.com/jenarvaezg/coindex/issues/317)). **The test:** if what is inside is what
is outside in a different order, it is a facet of the shelf, not a destination. **Two symptoms
give it away**: the only name that fits fights an existing one («tu colección» against
«Colecciones»), and the count has to be borrowed («El mundo · 678» are slots of the sheet).

What changes in ADR 0021 §1:

1. Two sibling hierarchies become **three**.
2. The bottom bar has **three cells**: **Colecciones, Monedas, Las cifras**. The app still opens in
   Collections.
3. **Each cell names its grain with its count**: `Colecciones · 69` (cards), `Monedas · 192`
   (types), `Las cifras · 6,91 kg` (grams). «Las cifras» counts weight and **never money**.
   *Nota de forma, #516: la celda del medio dice `Tipos · 192`. El número ya era el recuento de
   tipos, el mismo del canto cosido, así que cambia el rótulo y no el número; contar piezas pondría
   un número bajo dos palabras, que es el choque del #400.*
4. The shelf invariant — filters, sort and live search, folded on entry — narrows to the hierarchies
   **with a list**. «Las cifras» has none: its order is the figure you touch.
5. **The shelf gains the axis facet**: by plate (default), by country, by year (§9).
6. The test is written down, so a fourth candidate is examined rather than debated.

Unchanged: ADR 0021 §1's three consequences follow from Collections and Coins both existing; a home
screen asking where to go stays rejected; and ADR 0021 §2 stands.

**Amended by [#281](https://github.com/jenarvaezg/coindex/issues/281) (2026-08-13): what fails the
test may still be an annex.** «Explorar» fails it (plates and slots are a borrowed grain), but its
population is what the collector does **not** have, which no order of the sheet holds. An **annex**:

1. **Hangs off exactly one hierarchy**, which gives it its door; two doors would need two names.
2. Has **no cell and no bar**. It is entered from that hierarchy and left with «Volver»; the bar is
   drawn on the roots only.
3. Is entered by a row of that hierarchy's list, on deeper paper, that **names what is behind it
   with its count**, prints no zero (as the sewn edge, #418, and ADR 0028 §1) and draws its arrow
   rather than typing it.
4. **Follows the shelf invariant if it has a list.** An annex with a list opens with its shelf
   folded — search and sort — and carries only the chips its population earns: none in «Explorar»,
   whose plates are all at 0/N across a handful of countries (#279, #282).

An annex is not a way into the bar: no cell, no count there, and the top level remains three.

- **Clause 4 amended by [ADR 0030](0030-the-shelf-window-of-explorar-is-valued-by-hand.md)
  (2026-08-14).** With no chips there is nothing to fold, so «Explorar» shows search and sort open.
  Its shelf is **the plates where something is missing**: the twenty of the shelf window plus the
  collector's own plates with a marked casilla — a second order, not a second species (ADR 0021 §2).
  «Lo que busco» keeps its own screen, because its purpose is `Exportar la lista`. The door's count
  stays the twenty.
- **Clause 3 amended by [#520](https://github.com/jenarvaezg/coindex/issues/520) (2026-08-17): one
  row, one name, one destination.** The last-row door read «Lo que busco · 7, y otras 20 láminas →»
  once something was marked: two rooms named, one opened. Now **«Lo que busco · 7» sits at the
  head**, under the shelf, with its first three casillas drawn as coins, and is not printed while
  nothing is marked; **«Y otras 20 láminas que no coleccionas →» stays at the foot**, opening
  «Explorar». A shopping list goes where it is acted on, a window to browse where a page ends
  (`docs/ux/prototipo-dos-puertas-520/`). #515's note «Lo que escribes arriba no llega hasta aquí»
  goes once, on the head row.

### 9. The country map and the timeline are two axes of the notebook, not two screens

Both are made of **slots**, so by §8's test they are orders of the same sheet, chosen in the folded
shelf ([#315](https://github.com/jenarvaezg/coindex/issues/315), `docs/ux/atlas-315.md`), ordered by
`indexOrder()`. The **default axis is «by plate»**, today's Collections.

| axis | one cell is | at once | screens | words |
| --- | --- | ---: | ---: | ---: |
| by plate *(today's Collections)* | a collection | 12 plates | 5.40 | 31 |
| **by country** | a slot | **390** (422 with the shelf folded) | 2.25 | 15 |
| **by year** | a year | 112 cells | 1.62 | **3** |

The year axis has **three states**: coin, ghost hole and **bare cardboard**, which shows a run of
empty years without a word. Sorting by ratio opens on a small complete plate rather than a large
empty one: it reveals, it does not reproach.

Implementation obligations: no year in which the collector owns a coin painted as empty; Hijri years
must not stretch the axis; a slot goes in the **member's** country, not the catalog's (#170); and no
«loose pieces» band on an axis where every piece has a country.

**A piece has two years.** To match a slot, the coin's year (`recordedYear`, preferring
`issueYear`); to place it on an axis, the Gregorian one (`gregorianYear ?: recordedYear`). Undated
pieces inherit their type's minimum year ([#326](https://github.com/jenarvaezg/coindex/issues/326));
without it the arc is 246 years instead of 1,756.

### 10. «Las cifras»: money opens the page, and matter is ordered in ladders of referents

A hierarchy, not a dashboard: every figure that can be touched leads down to the pieces or plates
that compose it. Collections orders by plate, Coins by type, Las cifras by magnitude.

**A piece is worth the maximum of three numbers**
([#316](https://github.com/jenarvaezg/coindex/issues/316), `docs/ux/cifras-316.md`): its silver
floor, its Numista market price **for its grade**, and what was paid. Together they cover 99.5 % of
pieces. Catalog prices do not follow the metal, so which one wins moves with spot. `grade` is the
**pricing key**: Numista prices per issue and grade.

**Read piece by piece or plate by plate this is a shopping companion; totalled for the collection
it is wealth management.** The cost of completing one plate is a plan; for the whole shelf it is
«you are tens of thousands of euros short», a reproach.

The page ([#326](https://github.com/jenarvaezg/coindex/issues/326), `docs/ux/cifras-326.md`):

- **Money opens the page.** #316 rejected an amount changing on its own in a permanent bar; this is
  a page opened on purpose, with its origin and the spot's timestamp. The cell still counts weight.
- **Matter is ordered in three ladders of referents** (weight, row, stack), five referents each,
  with the collection between two: «more than a cat and 310 g short of a bowling ball». The scale is
  **ordinal**, so it has **no zoom**.
- **Metal is split by mass, not by coin.**
- **«A una casilla» lives in the plate header**, not here: it is what is missing.
- A few unrequested figures come from the fichas in the APK (coins no longer legal tender, the most
  frequent engraver, mint and year), plus the smallest coin against the largest.

**Money at export is the sixth switch** (#228, ADR 0021 §13). Off, it removes every figure derived
from money, not only the amount section («Venezuela · 30 % of the value» is money too).

### 11. Prices arrive in one pass, and the total is never shown half-done

One valuation pass, paid by each phone ([#327](https://github.com/jenarvaezg/coindex/issues/327)):
the issues the collector owns plus the holes of plates **≤ 10 slots** from closing. The threshold is
§10 applied: a plate with 51 holes has a reproach, not a cost of completion. Rules of form:

1. **The total is never shown half-done.** Without the market price the money section is absent: no
   provisional total. `max(silver, paid)` alone gave about 60 % of the real total, which is false,
   not incomplete.
2. **Coverage yes, progress no.** «The value of N of your 574 pieces», never «140 of 223».
3. **«Las cifras» opens whole without a call.** Everything but money comes from the APK (ADR 0024).
4. **Every number brought from outside shows when it was read**, and an expired one stays shown.

When the pass runs, what expires and how it stores the `issue_id` it needs is ADR 0028.

### 12. Amendment to ADR 0021 §9: the eyebrow of a card is no longer the country

The country eyebrow **dies with the card's four lines**: with the hole, hierarchy is the die-cut's
job. The country stays a shelf facet, in the plate's ficha, on paper and often in the photograph.
The **variant line** (`peso · acabado`) dies too: real twins already carry the variant in
`short_name` (`Noah's Ark 1 oz` / `½ oz`, `5 Reichsmark .500` / `.900`), and most cards said
`Acabado sin confirmar`. If a catalog ever arrives whose `short_name` omits the variant, that is a
curation rule (`curate-catalog` disambiguates in the name), not a line on every card. The rest of
ADR 0021 §9 stands.

**ADR 0023 stands**: the cured country name is still needed by the facet, the ficha and paper. Only
the third clause of its width rule («no more than the 40 characters the `short_name` below it is
capped at») loses the card as its subject. The open half of #170, a catalog spanning two issuers,
becomes a question for the country axis, which reads the member's country (§9).

### 13. A coin gets an inside: the hole in Coins opens a sheet

A coin had no screen, so its maintenance toll lived in the grid, printed once per type (43 % of
the content words of Coins). The hole in Coins **opens a bottom sheet with the coin's ficha**
holding the 104 dp die-cut the cell left (#370), `Actualizar la ficha · 1 llamada`, the `Ficha
traída hoy` / `hace N meses` line, `Ver en Numista` and the links to its collections. `En ninguna
colección` stays outside **as form** — a hole with no cardboard behind it — and the landing hole
follows it. This is new mechanism, justified because otherwise the toll stays on every row.

### 14. Licence notices: three words at the foot, one screen with everything inside

Three subjects (#323): **Numista** (seeded fichas and downloaded photographs), **software** from
the real release classpath rather than `libs.versions.toml` (Ktor brings `org.slf4j:slf4j-api`,
MIT), and **the typefaces** (OFL 1.1). The full texts ship **in the APK** (Apache 2.0 §4(a), OFL
1.1; a sideloaded APK, ADR 0011, carries no repository `LICENSES.txt`), **as assets, not
literals**, outside `CopyLivesInOnePlaceTest` (§6). The entry is `Avisos y licencias`, three words
at the foot of the screen the sewn edge opens, **no subtitle**. Notices are kept by hand, by family
and without versions; a Gradle task and a test go red when a dependency group has no notice.

Of Numista's §4 obligations, the N# is shown and the source attributed here. **Preserving
third-party photo credits is knowingly not met**: closed `wontfix` on 8 August 2026 by the owner
(two collections on two phones, no public surface).

**Amended by [#521](https://github.com/jenarvaezg/coindex/issues/521).** `Credenciales` — the two
fields, their promise and `Cerrar sesión` — moves into the same shape at the foot, because it is
visited once (onboarding, or a key Numista starts refusing). The top keeps what the trip is for:
`Sincronizar`, the two queues and the export, hence «Este teléfono». Two rules pay for the nesting:

1. **What blames the credentials must open them.** In the valuation states «Faltan las credenciales
   de Numista» and «Numista está rechazando las consultas», and only those, the card carries a row
   into `Credenciales`.
2. **A message that names a place names this one**: sync refusals say `Credenciales`, not Ajustes.

### 15. What «approved» means, and what an implementation session may change

None of the prototypes had been seen on a phone: **approved here means approved in HTML.** Every
effect passes through the AVD first, on **the calibration bench**: #303's HUD ported to Compose,
**`debug` only**, painting one real slot (the 1 Bolívar · 1960) with the parameter controls. Each
calibration ends in AVD captures and video, with **the number chosen before the production effect
is written**. Numbers for each amendment below are in its `docs/ux/implementacion-NNN/`.

| parameter | approved value | who decides |
| --- | --- | --- |
| grain opacity of the paper | 96 dp mosaic in `soft-light` at 0.75 (#351; was a 256 px mosaic at 0.08) | the bench — and if the grain is indistinguishable at 1:1 it is withdrawn without reopening #300 |
| gloss intensity and travel | half the video's at 105°, and the travel as **±45 % of the diameter** rather than the prototype's ±55 dp (#338) | the bench |
| flip duration | 420 ms | the bench |
| stamping duration | 300 ms | the bench |
| depth of the tag's recess | sunken, 48.3 × 28 dp | the bench (the size is not a parameter: it is what reaches Android's 48 dp) |
| the ghost | design at 14 % with a dotted rule, **and never under 72 dp of hole** (#556) | the bench |
| the wall of the die-cut | one 5 dp sweep on the cardboard: ink at 22 % at the top, white at 85 % at the bottom, nothing at the horizontals (#357) | the bench |
| **the gesture, the place, the drawing** | — | **the map**: an implementation session never changes form without coming back |

- **Amended by #351 (2026-08-09).** The 256 px mosaic at 0.08 was indistinguishable at 1:1. The
  owner chose to raise it rather than withdraw it: variation per tile, the tile in dp, and the paper
  as one surface painted in `CoindexTheme` that reaches the plate and the PDF.
- **Amended by #357 (2026-08-09).** #349's contrast exposed an uncalibrated die-cut wall, so its
  width, shadow and sheen join the bench. The 180° arcs on the cardboard jumped 76 luminance levels
  within 2° at 3 and 9 o'clock, which a sweep fading out at the horizontals removes. The arcs
  **inside** the hole are withdrawn (owner's call): on the coin's face they read as a mark.
- **Amended by #338 (2026-08-09, with §1, §3 and §4).** The bench confirmed the gloss intensity; its
  ceiling is the coin's rim vanishing against the hole. The owner set the **travel as a fraction of
  the diameter**, ±45 % of the photograph (±42.3 dp on the production casilla), and **withdrew the
  acetate reflection**, which under the gloss rebuilt #303's discarded two-layer variant.
- **Amended by #520 (2026-08-17): the ghost is two absences.** **«Te falta»**, on a plate being
  filled, keeps the design at 14 %. **«Esto lo buscas»**, on the list for a fair, draws the coin
  **whole** so it can be recognised across a table. Both keep the dotted rule; neither glosses.
  Which absence a surface draws is form, and `HoleAbsence` holds it.
- **Amended by #556 (2026-09-01): the penumbra needs a diameter.** The year axis has no design
  behind its ghost (`photo = null`) and the printed page desaturates instead (`GRAYSCALE_ON_PAPER`,
  #509), so neither draws one. The rest drew it at 104 dp, except the country axis at 34 dp, where
  it read as a grey disc. Hence **`GHOST_MIN_DP = 72 dp`** in `AlbumPaper`, the one place the ghost
  is drawn (chosen with the bench's new «FANTASMA · Diámetro»): below it the coin is drawn whole
  under the dotted rule.

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

The wow cannot come first: the gloss and the flip both live inside the hole's circular clip, which
the old side-by-side slot (`CoinSides`) did not have. The big block goes in **three PRs —
Collections, the plate, Coins**; `main` may be half album in between, since the father installs
releases.

## Consequences

- **The app's first four movements enter at once.** The API risk is `BlendMode` at API 29, the
  `minSdk`; the accelerometer's battery cost is unmeasured, and it never runs in the background.
- One behaviour change (the call ceiling leaves the interface, §5), one new capability («Las
  cifras», §8, §10), one new curation rule (`short_name` carries the variant, §12) and two new tests
  (§6).
- **What the father shows carries no metal** (§4), and nothing here was seen on a phone until the
  bench (§15).

## Documents this ADR changes

- **ADR 0021**: §1 amended by §8, §9 by §12; §13 gains §4's export rule and §10's sixth switch.
- **ADR 0010 §8** (the plate as a PNG) gains §4's export rule. **ADR 0023** stands (§12).
- **ADR 0018**, **0020** and **0024** are upheld: metal in the variant key is why there is no tint,
  `printed_side` is the resting face, and §11.3 extends local-first to «Las cifras».
- **`CONTEXT.md`** gains the vocabulary of §1 (hole, ghost, sunken tag, stamp), §5 (furniture, the
  frequency rule) and §8 (grain of a cell, axis of the shelf). **`spec.md §0.4`** describes what is
  built, pointing here. **ADR 0028** is the valuation ADR §11 asked for.

## Alternatives considered

- **The lamp — a warm dark theme** (#301): inverts the ink on an emissive screen. With it go the
  Ajustes switch and any paper forced at export.
- **Flipping the whole sheet** (#302), the only way to export obverses: a turned sheet inverts the
  column order, which either cheats or makes a date run dance. With it go no-flip, the cross-fade
  and hold-to-see.
- **Other glosses** (#303): following the relief by luminance cannot discriminate on silver and
  would need a height map; the narrow flash reads as a scratch on the acetate.
- **The stamp as a dated fact**, at sync, at the sheet's foot, as brass or as a label (#304).
- **The world map** (#315): it colours a minority of issuers, needs words to excuse the rest, and
  Tokelau has a plate and no polygon. With it go mini-maps, phenology bars and a country table.
- **«Láminas» as the Collections cell** (many cards have no plate) and **a fifth cell for the map
  and the timeline** («tu colección» fights «Colecciones») (#315, #317).
- **A declared copy registry** (#306), width measured from the TTF with `java.awt.Font` (another
  engine), and an AVD booted per PR.
- **A curated name per coin type**, and with it the abbreviations (#319).
- **Only the silver floor**, totals of what was paid, the aggregate premium and the total cost of
  completing (#316); **«Analíticas»** as the page's name.
- **The tower drawn to honest scale** (an 8 px needle over 250 px), the comparison as text, and zoom
  on the ladders (#326).
- **`Créditos` as the entry** (it does not say where the legal text lives) and a licence-generator
  plugin, mostly `androidx.*` noise (#323).
- **Lazy per-plate valuation** (#327): pricing every slot does not fit a month's calls.
- **Own photographs of the pieces**, discarded in
  [#15](https://github.com/jenarvaezg/coindex/issues/15) on 7 August 2026. Only the frozen
  `rust-frozen:spec.md` promised them («en Fase 2 las piezas propias se fotografían nosotros»), and
  it stopped being the specification on 29 July 2026. Relief and relighting (shape-from-shading,
  RTI) go with them: §3's gloss is a material effect, not a reconstruction.
