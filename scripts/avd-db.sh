#!/usr/bin/env bash
# Guarda y restaura la base de datos del AVD para medir sin gastar cuota de la API (#452).
#
# Un AVD vacío que se da de alta y sincroniza gasta cientos de llamadas del presupuesto mensual
# del padre, del que depende su móvil; usar la clave de Jose sólo cambia a quién se le acaba.
# `coindex.db` lleva la colección, las fichas, los precios y los listados de emisiones: con ella
# restaurada la app no llama a Numista. El volcado es la colección, así que vive fuera del
# repositorio.
#
#     scripts/avd-db.sh save        # una vez, con el AVD ya poblado
#     scripts/avd-db.sh restore     # en cada sesión, después de `adb install -r`
#
# El volcado también puede salir de «Exportar datos» en «Este teléfono» (#548), con el diario ya
# plegado: se copia al vault como `coindex.db`, sin `-wal` ni `-shm`. Es el único canal contra un
# APK de release y la única forma de medir la colección del padre sin gastar cuota. El nombre del
# fichero exportado lleva la versión que lo exportó, y el APK del emulador tiene que ser de esa
# versión o posterior: las migraciones de Room sólo van hacia delante.
#
# El alta sigue haciendo falta porque la clave se cifra contra la Keystore del dispositivo y no
# viaja en la base. No toca la red; lo que gasta es «Sincronizar», que con la base restaurada
# sobra.
set -euo pipefail

PACKAGE=com.jenarvaezg.coindex
VAULT="${COINDEX_AVD_VAULT:-/private/tmp/coindex-privado/avd}"
DEVICE_DIR="/data/data/$PACKAGE/databases"
# Room en modo WAL deja el diario y el índice compartido al lado: sin ellos se restaura una
# base sin las últimas transacciones, que es peor que no restaurar nada.
FILES=(coindex.db coindex.db-wal coindex.db-shm)

usage() {
    echo "uso: $0 {save|restore}" >&2
    exit 64
}

require_device() {
    if ! adb shell true >/dev/null 2>&1; then
        echo "no hay ningún dispositivo: levanta el AVD antes" >&2
        exit 69
    fi
    if ! adb shell "run-as $PACKAGE true" >/dev/null 2>&1; then
        echo "run-as no funciona: el APK instalado no es el de depuración" >&2
        exit 69
    fi
}

save() {
    require_device
    mkdir -p "$VAULT"
    for file in "${FILES[@]}"; do
        if adb shell "run-as $PACKAGE test -f $DEVICE_DIR/$file"; then
            adb exec-out "run-as $PACKAGE cat $DEVICE_DIR/$file" > "$VAULT/$file"
            echo "guardado $VAULT/$file ($(wc -c <"$VAULT/$file" | tr -d ' ') bytes)"
        else
            rm -f "$VAULT/$file"
        fi
    done
}

restore() {
    require_device
    if [ ! -f "$VAULT/coindex.db" ]; then
        echo "no hay volcado en $VAULT: córrelo primero con save" >&2
        exit 66
    fi
    # Parar la app antes de escribir: con la base abierta pisaría lo que se acaba de copiar.
    adb shell "am force-stop $PACKAGE"
    for file in "${FILES[@]}"; do
        adb shell "run-as $PACKAGE rm -f $DEVICE_DIR/$file"
        [ -f "$VAULT/$file" ] || continue
        adb push "$VAULT/$file" "/data/local/tmp/$file" >/dev/null
        adb shell "run-as $PACKAGE cp /data/local/tmp/$file $DEVICE_DIR/$file"
        adb shell "rm -f /data/local/tmp/$file"
        echo "restaurado $file"
    done
}

case "${1:-}" in
    save) save ;;
    restore) restore ;;
    *) usage ;;
esac
