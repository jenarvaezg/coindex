# Coindex

Coindex convierte dos colecciones privadas de Numista en láminas de serie: qué piezas
están, cuáles faltan y cuáles todavía no se han emitido. La Fase 1 usa Rust, Axum 0.8,
Maud, SQLx/Postgres y Shuttle 0.57.

## Requisitos

- Rust 1.85 o posterior.
- El target `wasm32-unknown-unknown`.
- Shuttle CLI compatible con 0.57.
- Docker, o un Postgres local, para `shuttle run`.

```bash
rustup target add wasm32-unknown-unknown
cargo install cargo-shuttle
```

## Credenciales de Numista

Cada una de las dos personas necesita su id de usuario y una API key de Numista. La key se
solicita y administra desde el área de API de la cuenta de Numista. Coindex usa OAuth
`client_credentials` con el scope `view_collection`; nunca escribe en la colección.

Copia el ejemplo local y reemplaza ids y keys:

```bash
cp Secrets.dev.toml.example Secrets.dev.toml
```

```toml
COINDEX_USERS = "jose:123456:api-key-uno,padre:654321:api-key-dos"
COINDEX_ORIGIN = "http://127.0.0.1:8000"
NUMISTA_MONTHLY_BUDGET = "1500"
```

`COINDEX_USERS` debe contener exactamente las dos entradas fijas de Fase 1,
`jose:id_de_usuario_numista:api_key` y `padre:id_de_usuario_numista:api_key`.
Las API keys viven en los Secrets de Shuttle y en memoria; no se guardan en Postgres.
No subas `Secrets.dev.toml` ni `Secrets.toml`.

`COINDEX_ORIGIN` es obligatorio y debe ser el origen público exacto, incluido esquema y
puerto cuando no sea el predeterminado. Usa `http://127.0.0.1:8000` en local y, en
producción, la URL HTTPS asignada al proyecto, por ejemplo
`https://coindex-xxxx.shuttle.app`.

Para producción, configura los mismos secretos con la gestión de secretos de Shuttle.

## Desarrollo local

El ejecutable local usa el mismo `bootstrap`, router, migraciones y ledger de presupuesto
que producción, sin necesitar la CLI de Shuttle. Crea una sola vez un Postgres persistente:

```bash
docker run --name coindex-local-postgres \
  -e POSTGRES_PASSWORD=coindex \
  -e POSTGRES_DB=coindex_local \
  -p 127.0.0.1:55432:5432 \
  -v coindex-local-postgres-data:/var/lib/postgresql/data \
  -d postgres:17-alpine
```

En ejecuciones posteriores basta con `docker start coindex-local-postgres`. Protege los
secretos y arranca Coindex:

```bash
chmod 600 Secrets.dev.toml
DATABASE_URL='postgres://postgres:coindex@127.0.0.1:55432/coindex_local?sslmode=disable' \
  cargo run -p coindex-backend --bin coindex-local
```

Abre `http://127.0.0.1:8000/health`. Las migraciones se aplican al arrancar y los
metadatos de tipos permanecen cacheados en el volumen de Postgres entre ejecuciones. El
runner local rechaza direcciones que no sean loopback: la Fase 1 no tiene autenticación y
no debe exponerse a la red local.

También siguen disponibles las comprobaciones de desarrollo:

```bash
cargo test --workspace
cargo check -p domain --target wasm32-unknown-unknown
```

`shuttle run` es una alternativa para comprobar específicamente el entorno de Shuttle,
pero no es necesario para trabajar en local.

Las consultas SQL se comprueban en compilación y su metadata offline vive en `.sqlx/`.
Después de modificar una consulta o migración, instala `sqlx-cli`, aplica las migraciones
en un Postgres de desarrollo y regenera la metadata:

```bash
cargo sqlx prepare --workspace -- --all-targets
```

La sincronización solo ocurre al pulsar el botón. “Calcular gasto” ejecuta el modo seco:
no hace peticiones y muestra un **límite inferior**, no un total exacto. Puede contar los
tipos ausentes del snapshot local, pero no puede saber si la colección remota contiene
tipos nuevos sin descargarla. El primer sync descarga la colección y solo los tipos no
cacheados; el segundo no vuelve a pedir esos metadatos.

## Rutas

- `/`: índice de series y usuarios.
- `/u/{user}/series/{series_id}`: lámina de una serie.
- `/u/{user}/unmatched`: piezas pendientes de asignación.
- `/api/album/{user}`: álbum completo en JSON.
- `/health`: salud y presupuesto mensual.

Las imágenes pasan por `/img/type/{type_id}/{side}`. El proxy solo utiliza URLs HTTPS
previamente cacheadas para dominios de Numista y no sigue redirecciones.

## Seguridad en Fase 1

La especificación excluye deliberadamente cuentas y login: esta aplicación es para dos
usuarios fijos y no debe exponerse como un servicio público sin protección. Los formularios
que modifican estado exigen que `Origin` coincida canónicamente con `COINDEX_ORIGIN`,
incluido el esquema, y no se habilita CORS permisivo. No se confía en `Host` ni en
cabeceras reenviadas para decidirlo. En producción, restringe además el acceso desde la
configuración de acceso de Shuttle (o una capa privada equivalente). Esa restricción
perimetral es la protección de acceso de la Fase 1; no la sustituyas por credenciales
inventadas dentro de Coindex.

## Fixtures y red

Los tests no acceden a la red. La única vía autorizada para actualizar respuestas grabadas
es el binario explícito:

```bash
cargo run -p numista --bin record-fixtures -- --help
```

Ejecutarlo puede consumir cuota real; revisa primero el presupuesto.

## Despliegue

Con los secretos de producción ya configurados:

```bash
shuttle deploy
```

Comprueba después `/health`. No despliegues para probar cambios locales ni regrabes
fixtures como parte de un test.
