CREATE TABLE collected_items (
    id BIGINT NOT NULL,
    user_key TEXT NOT NULL,
    type_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    issue_year INTEGER,
    grade TEXT,
    collection_name TEXT,
    raw JSONB NOT NULL,
    synced_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_key, id)
);

CREATE INDEX collected_items_user_key_idx ON collected_items (user_key);
CREATE INDEX collected_items_type_id_idx ON collected_items (type_id);

CREATE TABLE type_meta (
    type_id INTEGER PRIMARY KEY,
    raw JSONB NOT NULL,
    fetched_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE manual_overrides (
    user_key TEXT NOT NULL,
    item_id BIGINT NOT NULL,
    slot_id TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_key, item_id)
);

CREATE TABLE api_call_log (
    id BIGSERIAL PRIMARY KEY,
    endpoint TEXT NOT NULL,
    called_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX api_call_log_called_at_idx ON api_call_log (called_at);
