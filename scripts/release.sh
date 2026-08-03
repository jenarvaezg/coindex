#!/usr/bin/env bash
# Publica una versión de Coindex como release de GitHub.
#
#   scripts/release.sh                      # notas a partir de los commits
#   scripts/release.sh "Resumen de la versión"
#
# La firma se hace aquí, en la máquina donde vive el keystore: la clave no viaja a ningún
# servicio (ADR 0011). El script comprueba primero que la versión es publicable, después
# construye el APK firmado, verifica la firma, genera el update.json que lee el actualizador
# de la app y crea la release con el tag vX.Y.Z.
set -euo pipefail

cd "$(dirname "$0")/.."

SUMMARY="${1:-}"
: "${JAVA_HOME:=/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
export JAVA_HOME
ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
APKSIGNER="$ANDROID_HOME/build-tools/36.0.0/apksigner"

if [[ ! -f keystore.properties ]]; then
  echo "falta keystore.properties: sin él el APK saldría sin firmar" >&2
  exit 1
fi

VERSION_CODE=$(sed -n 's/^ *versionCode *= *\([0-9]*\).*/\1/p' app/build.gradle.kts | head -1)
VERSION_NAME=$(sed -n 's/^ *versionName *= *"\([^"]*\)".*/\1/p' app/build.gradle.kts | head -1)
if [[ -z "$VERSION_CODE" || -z "$VERSION_NAME" ]]; then
  echo "no se pudo leer versionCode/versionName de app/build.gradle.kts" >&2
  exit 1
fi
TAG="v$VERSION_NAME"
APK_NAME="coindex-$VERSION_CODE.apk"
REPO=$(gh repo view --json nameWithOwner --jq .nameWithOwner)

# --- Guardas, antes de gastar un minuto compilando -------------------------------------
if gh release view "$TAG" >/dev/null 2>&1; then
  echo "la release $TAG ya existe: sube versionCode y versionName en app/build.gradle.kts" >&2
  exit 1
fi

# El versionCode es lo que decide si los móviles ven la actualización, así que subir solo el
# versionName produciría una release que nadie llega a instalar.
PUBLISHED_CODE=$(
  curl -sfL "https://github.com/$REPO/releases/latest/download/update.json" 2>/dev/null |
    python3 -c 'import json,sys; print(json.load(sys.stdin).get("versionCode", 0))' 2>/dev/null ||
    echo 0
)
if (( VERSION_CODE <= PUBLISHED_CODE )); then
  echo "versionCode $VERSION_CODE no supera el publicado ($PUBLISHED_CODE):" >&2
  echo "los móviles no verían esta versión. Sube versionCode en app/build.gradle.kts." >&2
  exit 1
fi

if [[ -n "$(git status --porcelain)" ]]; then
  echo "el árbol tiene cambios sin commitear: la release apuntaría a un commit que no los" >&2
  echo "incluye. Haz commit y push antes de publicar." >&2
  exit 1
fi

# --- Notas ------------------------------------------------------------------------------
# `gh release create` crea el tag en el servidor y no en el clon, así que sin este fetch el
# changelog se calcula desde el tag anterior y repite los commits de la release pasada. No se
# nota mientras haya un `git pull` entre release y release, que es justo lo que no hay cuando
# se publican dos seguidas.
git fetch --tags --quiet
LAST_TAG=$(git tag --list 'v*' --sort=-v:refname | head -1)
if [[ -n "$LAST_TAG" ]]; then
  CHANGELOG=$(git log --no-merges --pretty='- %s' "$LAST_TAG"..HEAD)
else
  CHANGELOG=$(git log --no-merges --pretty='- %s')
fi
CHANGELOG=${CHANGELOG:-"- Coindex $VERSION_NAME"}
# El resumen va al banner de la app, que muestra dos líneas; el changelog entero va al
# cuerpo de la release.
if [[ -z "$SUMMARY" ]]; then
  SUMMARY=$(head -1 <<<"$CHANGELOG" | sed 's/^- //')
fi

# Los catálogos viajan dentro del APK y se rechazó el canal remoto (ADR 0020), así que la
# frescura de un catálogo queda atada a la versión instalada. Esta línea es la mitigación: quien
# lee el banner sabe si la actualización trae datos curados o solo código. El banner corta a dos
# líneas, así que la segunda va corta a propósito y el detalle vive en el cuerpo de la release.
if [[ -n "$LAST_TAG" ]]; then
  DATA_FILES=$(git diff --name-only "$LAST_TAG"..HEAD -- data/)
else
  DATA_FILES=$(git ls-files data/)
fi
if [[ -n "$DATA_FILES" ]]; then
  DATA_COUNT=$(grep -c . <<<"$DATA_FILES")
  SUMMARY="$SUMMARY"$'\n'"Trae datos curados: $DATA_COUNT ficheros de data/."
  CHANGELOG="$CHANGELOG"$'\n\n'"### Datos curados en esta versión"$'\n'"$(sed 's/^/- /' <<<"$DATA_FILES")"
else
  SUMMARY="$SUMMARY"$'\n'"Sin cambios en los catálogos."
fi

echo "==> Construyendo $TAG (versionCode $VERSION_CODE, publicado $PUBLISHED_CODE)"
./gradlew :app:assembleRelease

BUILT_APK=app/build/outputs/apk/release/app-release.apk
"$APKSIGNER" verify "$BUILT_APK" >/dev/null
echo "==> Firma verificada"

OUT=build/release
rm -rf "$OUT" && mkdir -p "$OUT"
cp "$BUILT_APK" "$OUT/$APK_NAME"

# El actualizador lee este fichero: el tag es para humanos y sería un sitio frágil
# donde codificar el versionCode del que depende la decisión de actualizar.
python3 - "$OUT/update.json" "$VERSION_CODE" "$VERSION_NAME" "$APK_NAME" "$SUMMARY" <<'PY'
import json, sys
path, version_code, version_name, apk_asset, notes = sys.argv[1:6]
manifest = {
    "versionCode": int(version_code),
    "versionName": version_name,
    "apkAsset": apk_asset,
}
if notes.strip():
    manifest["notes"] = notes.strip()
with open(path, "w", encoding="utf-8") as handle:
    json.dump(manifest, handle, ensure_ascii=False, indent=2)
    handle.write("\n")
PY

echo "==> Publicando release $TAG"
gh release create "$TAG" \
  --title "Coindex $VERSION_NAME" \
  --notes "$SUMMARY

$CHANGELOG" \
  "$OUT/$APK_NAME" "$OUT/update.json"

echo "==> Listo. Los móviles verán la actualización en el índice."
