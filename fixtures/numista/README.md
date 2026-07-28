# Numista fixtures

These network-free test fixtures are hand-curated from response examples in Numista's
official API v3.32 documentation. They are **not empirical captures from the project's
account** and contain no private collection data or usable access token. Empirical
replacement requires the account credentials and must not be attempted during automated
tests or exploratory development.

Live replacements must only be created deliberately with:

```console
cargo run -p numista --bin record-fixtures -- --confirm-live-api --user-id USER_ID --type-id TYPE_ID
```

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
