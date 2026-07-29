# Numista fixtures

The five `type_*_es.json` files for N#386213, N#394043, N#404044, N#404285, and
N#482185 are public empirical type captures: exact successful API responses requested
with `lang=es`. They contain public catalog metadata, not private collection data.

The other committed fixtures (`oauth_token.json`, `collected_items.json`, and
`type_420_es.json`) are hand-curated from response examples in Numista's official API
v3.32 documentation. In particular, `collected_items.json` is not an empirical capture
of the project's collection. It contains no real private collection data, and
`oauth_token.json` contains no usable access token. Any deliberately recorded private
collection capture must remain outside this committed public fixture set.

Public type captures can update this committed fixture set deliberately:

```console
cargo run -p numista --bin record-fixtures -- --confirm-live-api --type-id TYPE_ID
```

Private collection captures require an explicit output directory outside the repository:

```console
cargo run -p numista --bin record-fixtures -- --confirm-live-api \
  --user-id USER_ID --output-dir /private/tmp/coindex-numista-private
```

The recorder enforces this boundary before connecting to Postgres or Numista; a confirmed
collection capture targeting any directory inside the repository is rejected.

The recorder reads `NUMISTA_API_KEY` and never writes that value or the OAuth token to
disk. A confirmed live run also requires `NUMISTA_BUDGET_DATABASE_URL` (or
`DATABASE_URL`) pointing to the backend Postgres database. It uses the same
`api_call_log`, UTC-month query, and Postgres advisory lock as the backend, respecting
`NUMISTA_MONTHLY_BUDGET` (default `1500`). Every call is transactionally reserved in the
authoritative log before the request is sent; a database error or exhausted budget
prevents the request.

`--dry-run` remains API-credential-free, database-free, and network-free. It reports only
the planned-attempt ceiling, not current authoritative usage or remaining budget. The
estimate is exact for explicitly supplied type IDs in the fresh recorder process. Actual
calls may be lower after an early failure, and IDs not supplied to the command are outside
the estimate.

Successful responses retain their raw JSON. All selected responses are staged and synced
before existing fixtures are replaced, so typed DTO serialization cannot introduce
aliases or `null` fields into recordings.
