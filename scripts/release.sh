#!/usr/bin/env bash
# Publica una versión de Coindex como release de GitHub.
#
#   ./scripts/release.sh "Notas de la versión"
#
# Construye el APK firmado, comprueba que la firma es la esperada, genera el update.json que
# lee el actualizador de la app y crea la release con el tag vX.Y.Z.
set -euo pipefail

cd "$(dirname "$0")/.."

NOTES="${1:-}"
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

if gh release view "$TAG" >/dev/null 2>&1; then
  echo "la release $TAG ya existe: sube versionCode y versionName antes de publicar" >&2
  exit 1
fi

echo "==> Construyendo $TAG (versionCode $VERSION_CODE)"
./gradlew :app:assembleRelease

BUILT_APK=app/build/outputs/apk/release/app-release.apk
"$APKSIGNER" verify "$BUILT_APK" >/dev/null
echo "==> Firma verificada"

OUT=build/release
rm -rf "$OUT" && mkdir -p "$OUT"
cp "$BUILT_APK" "$OUT/$APK_NAME"

# El actualizador lee este fichero: el tag es para humanos y sería un sitio frágil
# donde codificar el versionCode del que depende la decisión de actualizar.
python3 - "$OUT/update.json" "$VERSION_CODE" "$VERSION_NAME" "$APK_NAME" "$NOTES" <<'PY'
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
  --notes "${NOTES:-Coindex $VERSION_NAME}" \
  "$OUT/$APK_NAME" "$OUT/update.json"

echo "==> Listo. Los móviles verán la actualización en el índice."
