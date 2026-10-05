# Numista fixtures

Tests read these files and never touch the network.

- `type_386213_es.json`, `type_394043_es.json`, `type_404044_es.json`, `type_404285_es.json` and
  `type_482185_es.json` are real `GET /types/{id}?lang=es` responses: public catalogue metadata.
- `oauth_token.json`, `collected_items.json` and `type_420_es.json` are hand-written from the
  response examples in Numista's API v3.32 documentation. They hold no real collection data and no
  usable token.

Record or refresh a public type capture with `scripts/record-fixture.py`. It spends one real API
call, reads `NUMISTA_API_KEY` and requires `--confirm-live-api`:

```console
scripts/record-fixture.py --confirm-live-api --type-id TYPE_ID
```

A collection capture is private and never goes in this directory. `--user-id` requires an
`--output-dir` outside the repository, and the script rejects this directory:

```console
scripts/record-fixture.py --confirm-live-api \
  --user-id USER_ID --output-dir /private/tmp/coindex-numista-private
```
