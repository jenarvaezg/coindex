#!/usr/bin/env bash
# Guarda y restaura la base de datos del AVD para medir sin gastar cuota de la API (#452).
#
# Una sesión de medición en el emulador costaba 446 llamadas: el AVD arranca con la base
# vacía, se da de alta, se sincroniza y a los tres segundos la tasación pide los precios y
# los listados de toda la colección. Eso salía del presupuesto mensual del padre, que es del
# que depende su móvil — y cambiarlo por el de Jose sólo cambia a quién se le apaga la app.
#
# `coindex.db` lleva la colección, las fichas, los precios y (desde el #452) los listados de
# emisiones. Restaurada, la app tiene todo lo que una pasada le pediría a Numista y no gasta
# ni una llamada. El volcado es privado: vive fuera del repositorio, como la captura de la
# colección, porque es la colección.
#
#     scripts/avd-db.sh save        # una vez, con el AVD ya poblado
#     scripts/avd-db.sh restore     # en cada sesión, después de `adb install -r`
#
# El volcado no tiene que salir de un AVD. «Exportar datos», en Ajustes, comparte la base del
# móvil ya con el diario plegado dentro (#548): se copia al vault como `coindex.db`, sin `-wal`
# ni `-shm`, y `restore` la carga igual. Es el único canal que hay contra un APK de release, y
# la única forma de medir la colección del padre sin gastar cuota. La regla al cargarla es la
# que lleva el nombre del fichero: el APK del emulador tiene que ser de versión igual o
# posterior a la del que exportó, porque las migraciones de Room sólo van hacia delante.
#
# El alta sigue haciendo falta, porque la clave se cifra contra la Keystore del dispositivo y
# no viaja en la base de datos. No cuesta nada: el formulario valida el formato y guarda, sin
# tocar la red. Lo que cuesta es pulsar «Sincronizar», y con la base restaurada no hace falta.
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
    # Parada limpia antes de escribir: la app con la base abierta se encuentra el fichero
    # cambiado debajo y se lleva por delante lo que se acaba de copiar.
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
