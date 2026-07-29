CREATE TABLE collection_proposal_preferences (
    user_key TEXT NOT NULL,
    family TEXT NOT NULL,
    weight_millioz INTEGER NOT NULL,
    finish TEXT NOT NULL,
    disposition TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_key, family, weight_millioz, finish),
    CHECK (user_key <> '' AND user_key = btrim(user_key)),
    CHECK (
        family <> ''
        AND family = btrim(family)
        AND family = regexp_replace(family, '[[:space:]]+', ' ', 'g')
        AND octet_length(family) <= 256
    ),
    CHECK (weight_millioz BETWEEN 1 AND 1000000),
    CHECK (
        finish IN (
            'unknown',
            'bullion',
            'proof',
            'coloured',
            'proof_coloured',
            'gilded',
            'antiqued'
        )
    ),
    CHECK (disposition IN ('followed', 'ignored'))
);
