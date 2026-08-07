#!/usr/bin/env python3
"""Measure the typeface candidates of #298 and redraw the two specimen sheets.

Run from the repository root:

    pip install 'fonttools>=4.61' pillow brotli
    python docs/ux/fuentes-empaquetadas-298/medir.py

Everything the report claims comes out of here: the repertoire is read from this repository's
own sources, and every candidate is downloaded from `google/fonts`, never from a third-party
mirror. Widths are normalised by x-height because comparing ems flatters a narrow face with a
small x — it has to be set larger to read the same size, and then it is not narrow any more.
"""

from __future__ import annotations

import json
import re
import subprocess
import sys
import urllib.parse
import urllib.request
import zlib
from pathlib import Path

from fontTools.ttLib import TTFont
from fontTools.varLib import instancer
from PIL import Image, ImageDraw, ImageFont

REPO = Path(__file__).resolve().parents[3]
WORK = Path(__file__).resolve().parent / "_work"
UPSTREAM = "https://raw.githubusercontent.com/google/fonts/main/"

# The palette of the field guide, so the specimens are read on the app's own paper.
PAPER, INK, MUTED, RUST, HAIR = (0xEE, 0xE8, 0xD7), (0x2D, 0x30, 0x29), (0x69, 0x6B, 0x5E), (0x8B, 0x55, 0x3C), (0x9F, 0x9B, 0x8B)

# Strings taken from the real collection, not from lorem ipsum.
PROSE = "Colección de monedas conmemorativas españolas"
CARD_TITLE = "Monumentos arquitectónicos de Rusia"
CARD_LINE = "3 rublos · plata .925 · desde 2009 — faltan 4 de 22"
DATA = "12 monedas · 1876–2024"
DATA_LONG = "12 monedas · 1876–2024 · ½ onza"
EYEBROW = "RUSIA · PLATA .925"
COUNTS = ["11 / 22", "18 / 22", "10 / 22"]

# Only the fields that reach the screen. `lettering` and `issuing_entity` are never read by any
# .kt, so their Cyrillic, Arabic, Armenian and CJK are not a coverage requirement.
PAINTED_FIELDS = {"title", "short_name", "name", "series", "country", "label"}

CANDIDATES = [
    # (path in google/fonts, instance to measure at, label). Everything is measured at wght 400 so
    # that the width comparison is about the drawing and not about one candidate being set bolder.
    ("ofl/notoserif/NotoSerif[wdth,wght].ttf", {"wght": 400, "wdth": 100}, "Noto Serif (hoy)"),
    ("ofl/roboto/Roboto[wdth,wght].ttf", {"wght": 400, "wdth": 100}, "Roboto (hoy)"),
    ("ofl/literata/Literata[opsz,wght].ttf", {"opsz": 10, "wght": 400}, "Literata"),
    ("ofl/literata/Literata-Italic[opsz,wght].ttf", {"opsz": 10, "wght": 400}, "Literata Italic"),
    ("ofl/bitter/Bitter[wght].ttf", {"wght": 400}, "Bitter"),
    ("ofl/bitter/Bitter-Italic[wght].ttf", {"wght": 400}, "Bitter Italic"),
    ("ofl/sourceserif4/SourceSerif4[opsz,wght].ttf", {"opsz": 10, "wght": 400}, "Source Serif 4"),
    ("ofl/vollkorn/Vollkorn[wght].ttf", {"wght": 400}, "Vollkorn"),
    ("ofl/petrona/Petrona[wght].ttf", {"wght": 400}, "Petrona"),
    ("ofl/newsreader/Newsreader[opsz,wght].ttf", {"opsz": 10, "wght": 400}, "Newsreader"),
    ("ofl/fraunces/Fraunces[SOFT,WONK,opsz,wght].ttf", {"opsz": 10, "wght": 400, "SOFT": 0, "WONK": 0}, "Fraunces"),
    ("ofl/archivo/Archivo[wdth,wght].ttf", {"wdth": 62, "wght": 400}, "Archivo wdth 62"),
    ("ofl/archivonarrow/ArchivoNarrow[wght].ttf", {"wght": 400}, "Archivo Narrow"),
    ("ofl/barlowcondensed/BarlowCondensed-Regular.ttf", None, "Barlow Condensed"),
    ("ofl/barlowcondensed/BarlowCondensed-SemiBold.ttf", None, "Barlow Condensed SemiBold"),
    ("ofl/oswald/Oswald[wght].ttf", {"wght": 400}, "Oswald"),
    ("ofl/encodesanscondensed/EncodeSansCondensed-Regular.ttf", None, "Encode Sans Condensed"),
    ("ofl/encodesanscondensed/EncodeSansCondensed-SemiBold.ttf", None, "Encode Sans Cond SemiBold"),
    ("ofl/sairacondensed/SairaCondensed-Regular.ttf", None, "Saira Condensed"),
    ("ofl/firasanscondensed/FiraSansCondensed-Regular.ttf", None, "Fira Sans Condensed"),
    ("ofl/robotocondensed/RobotoCondensed[wght].ttf", {"wght": 400}, "Roboto Condensed"),
    ("ofl/ibmplexsanscondensed/IBMPlexSansCondensed-Regular.ttf", None, "IBM Plex Sans Condensed"),
]

# Google Fonts' own latin + latin-ext ranges, for the subsetting cost the report quotes.
LATIN = (
    "U+0000-00FF,U+0131,U+0152-0153,U+02BB-02BC,U+02C6,U+02DA,U+02DC,U+0304,U+0308,U+0329,"
    "U+2000-206F,U+20AC,U+2122,U+2191,U+2193,U+2212,U+2215,U+FEFF,U+FFFD,"
    "U+0100-02BA,U+02BD-02C5,U+02C7-02CC,U+02CE-02D7,U+02DD-02FF,U+1E00-1E9F,U+1EF2-1EFF,"
    "U+2020,U+20A0-20AB,U+20AD-20C0,U+2113,U+2C60-2C7F,U+A720-A7FF"
)


def repertoire() -> set[str]:
    """Every character the app actually paints: UI string literals plus painted data fields."""
    chars: set[str] = set()
    literal = re.compile(r'"((?:[^"\\\n]|\\.)*)"')
    for kt in (REPO / "app/src/main/kotlin/com/jenarvaezg/coindex/ui").rglob("*.kt"):
        source = kt.read_text()
        source = re.sub(r"/\*.*?\*/", "", source, flags=re.S)  # KDoc is not on screen
        source = re.sub(r"//[^\n]*", "", source)
        for match in literal.finditer(source):
            chars |= set(match.group(1))

    def walk(node, key=None):
        if isinstance(node, dict):
            for k, v in node.items():
                if k == "issuing_entity":  # never read by any .kt
                    continue
                walk(v, k)
        elif isinstance(node, list):
            for v in node:
                walk(v, key)
        elif isinstance(node, str) and key in PAINTED_FIELDS:
            chars.update(node)

    for blob in (REPO / "data").rglob("*.json"):
        try:
            walk(json.loads(blob.read_text()))
        except json.JSONDecodeError:
            continue
    return {c for c in chars if c.isprintable() and c != " "}


def download(path: str) -> Path:
    dest = WORK / "fonts" / path.rsplit("/", 1)[-1]
    dest.parent.mkdir(parents=True, exist_ok=True)
    if not dest.exists():
        urllib.request.urlretrieve(UPSTREAM + urllib.parse.quote(path), dest)
    return dest


def tnum_really_works(font: TTFont) -> str:
    """Resolve the tnum feature in GSUB and check the ten digits land on equal-width glyphs."""
    cmap, hmtx = font.getBestCmap(), font["hmtx"]
    digits = [cmap[ord(d)] for d in "0123456789" if ord(d) in cmap]
    if len({hmtx[g][0] for g in digits}) == 1:
        return "por defecto"
    if "GSUB" not in font:
        return "no"
    table = font["GSUB"].table
    lookups = [r.Feature.LookupListIndex for r in table.FeatureList.FeatureRecord if r.FeatureTag == "tnum"]
    mapped: dict[str, str] = {}
    for group in lookups:
        for index in group:
            for sub in table.LookupList.Lookup[index].SubTable:
                for glyph in digits:
                    if getattr(sub, "mapping", None) and glyph in sub.mapping:
                        mapped[glyph] = sub.mapping[glyph]
    if len(mapped) < len(digits):
        return "no"
    return "tnum" if len({hmtx[mapped[g]][0] for g in digits}) == 1 else "no"


def features(font: TTFont) -> set[str]:
    tags: set[str] = set()
    for table in ("GSUB", "GPOS"):
        if table in font and font[table].table.FeatureList:
            tags |= {r.FeatureTag for r in font[table].table.FeatureList.FeatureRecord}
    return tags


def subset_size(src: Path) -> int:
    dest = WORK / "subset" / src.name
    dest.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        [sys.executable, "-m", "fontTools.subset", str(src), f"--output-file={dest}",
         "--layout-features=*", "--name-IDs=*", f"--unicodes={LATIN}"],
        check=True, capture_output=True,
    )
    return len(zlib.compress(dest.read_bytes(), 9))


def measure(rep: set[str]) -> list[dict]:
    rows = []
    for path, location, label in CANDIDATES:
        src = download(path)
        font = TTFont(src)
        if location:
            instancer.instantiateVariableFont(font, location, inplace=True, updateFontNames=False)
            instance = WORK / "inst" / f"{label.replace(' ', '')}.ttf"
            instance.parent.mkdir(parents=True, exist_ok=True)
            font.save(instance)
        else:
            instance = src
        cmap, hmtx, upm = font.getBestCmap(), font["hmtx"], font["head"].unitsPerEm
        xheight = font["OS/2"].sxHeight / upm

        def width(text: str) -> float:
            return sum(hmtx[cmap[ord(c)]][0] for c in text if ord(c) in cmap) / upm

        rows.append({
            "label": label,
            "file": src.name,
            "instance": str(instance),
            "glyphs": font["maxp"].numGlyphs,
            "zip": len(zlib.compress(src.read_bytes(), 9)),
            "zip_subset": subset_size(src),
            "xheight": round(xheight, 3),
            "prose_x": round(width(PROSE) / xheight, 1),
            "data_x": round(width(DATA) / xheight, 1),
            "tabular": tnum_really_works(font),
            "smcp": "smcp" in features(font),
            "missing": "".join(sorted(c for c in rep if ord(c) not in cmap)),
        })
    return rows


def specimen(rows: list[dict], title: str, out: Path, target_x: int, draw_row, chrome: dict) -> None:
    """Draw one sheet. `chrome` carries the two faces the sheet itself is set in."""
    row_h, width = 132, 1620
    image = Image.new("RGB", (width, 90 + row_h * len(rows)), PAPER)
    canvas = ImageDraw.Draw(image)

    def ui(size: int) -> ImageFont.FreeTypeFont:
        return ImageFont.truetype(chrome["ui"], size)

    canvas.text((40, 30), title, font=ImageFont.truetype(chrome["heading"], 30), fill=INK)
    canvas.line((40, 76, width - 40, 76), fill=MUTED)
    y = 92
    for row in rows:
        canvas.text((40, y + 10), row["label"], font=ui(22), fill=RUST)
        canvas.text((40, y + 36), f"x-height {row['xheight']:.3f} · {row['tabular']} · {'versalitas' if row['smcp'] else 'sin versalitas'}", font=ui(16), fill=MUTED)
        size = max(6, round(target_x / row["xheight"]))
        draw_row(canvas, y, ImageFont.truetype(row["instance"], size), row)
        canvas.line((40, y + row_h - 8, width - 40, y + row_h - 8), fill=HAIR)
        y += row_h
    image.save(out)
    print(f"{out.name} {image.size}")


def main() -> None:
    rep = repertoire()
    print(f"repertorio: {len(rep)} glifos, {sum(1 for c in rep if ord(c) > 127)} fuera de ASCII")
    rows = measure(rep)
    (WORK / "medido.json").write_text(json.dumps(rows, ensure_ascii=False, indent=1))

    header = f"{'fuente':28}{'zip KB':>8}{'subset':>8}{'prosa/x':>9}{'datos/x':>9}{'x-h':>7}  {'tabulares':<12}{'vers.':<7}faltan"
    print(header, "-" * len(header), sep="\n")
    for r in rows:
        print(f"{r['label'][:27]:28}{r['zip']/1024:8.0f}{r['zip_subset']/1024:8.0f}{r['prose_x']:9.1f}"
              f"{r['data_x']:9.1f}{r['xheight']:7.3f}  {r['tabular']:<12}{('sí' if r['smcp'] else 'no'):<7}{r['missing'] or '—'}")

    by_label = {r["label"]: r for r in rows}
    serifs = ["Noto Serif (hoy)", "Bitter", "Literata", "Vollkorn", "Petrona", "Source Serif 4", "Fraunces", "Newsreader"]
    condensed = ["Roboto (hoy)", "Archivo wdth 62", "Barlow Condensed", "Oswald", "Encode Sans Condensed",
                 "Saira Condensed", "Archivo Narrow", "Fira Sans Condensed", "Roboto Condensed", "IBM Plex Sans Condensed"]

    def prose_row(canvas, y, font, row):
        canvas.text((530, y + 6), CARD_TITLE, font=font, fill=INK)
        small = ImageFont.truetype(row["instance"], max(6, round(18 / row["xheight"])))
        canvas.text((530, y + 56), CARD_LINE, font=small, fill=MUTED)

    def data_row(canvas, y, font, row):
        yy = y + 4
        for count in COUNTS:
            canvas.text((700, yy), count, font=font, fill=INK)
            yy += 36
        canvas.text((900, y + 14), EYEBROW, font=ImageFont.truetype(row["instance"], max(6, round(14 / row["xheight"]))), fill=MUTED)
        canvas.text((900, y + 56), DATA_LONG, font=font, fill=INK)

    here = Path(__file__).resolve().parent
    chrome = {"heading": by_label["Literata"]["instance"], "ui": by_label["Roboto Condensed"]["instance"]}
    specimen([by_label[n] for n in serifs], "La prosa de una tarjeta, a igual altura de x (23 px)",
             here / "serifs.png", 23, prose_row, chrome)
    specimen([by_label[n] for n in condensed],
             "Los datos, a igual altura de x (20 px): tres recuentos uno bajo otro y una eyebrow",
             here / "condensadas.png", 20, data_row, chrome)

    # What each pairing adds to the APK the father downloads on every update.
    apk = 30_855_547 / 1024
    combos = {
        "Bitter + Archivo Narrow": ["Bitter", "Archivo Narrow"],
        "Bitter + Barlow Condensed (Regular + SemiBold)": ["Bitter", "Barlow Condensed", "Barlow Condensed SemiBold"],
        "Petrona + Encode Sans Condensed (Regular + SemiBold)": ["Petrona", "Encode Sans Condensed", "Encode Sans Cond SemiBold"],
        "Bitter + Bitter Italic + Archivo Narrow": ["Bitter", "Bitter Italic", "Archivo Narrow"],
        "Vollkorn + Barlow Condensed (Regular + SemiBold)": ["Vollkorn", "Barlow Condensed", "Barlow Condensed SemiBold"],
        "Literata + Archivo Narrow": ["Literata", "Archivo Narrow"],
        "Literata + Archivo (eje wdth)": ["Literata", "Archivo wdth 62"],
        "Source Serif 4 + Archivo (eje wdth)": ["Source Serif 4", "Archivo wdth 62"],
        "Literata + Literata Italic + Archivo Narrow": ["Literata", "Literata Italic", "Archivo Narrow"],
    }
    print(f"\n{'combinación':56}{'ficheros':>9}{'zip KB':>9}{'% del APK':>11}")
    for name, parts in sorted(combos.items(), key=lambda kv: sum(by_label[p]["zip"] for p in kv[1])):
        total = sum(by_label[p]["zip"] for p in parts) / 1024
        print(f"{name:56}{len(parts):9}{total:9.0f}{100 * total / apk:10.2f}%")


if __name__ == "__main__":
    main()
