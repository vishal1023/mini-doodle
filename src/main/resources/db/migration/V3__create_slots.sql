CREATE TABLE slots (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_time TIMESTAMPTZ NOT NULL,
    end_time   TIMESTAMPTZ NOT NULL,
    status     VARCHAR(10) NOT NULL DEFAULT 'FREE',
    version    BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_slots_status CHECK (status IN ('FREE', 'BUSY')),
    CONSTRAINT chk_slots_time_order CHECK (end_time > start_time),

    -- No two slots for the same user may ever overlap, regardless of status.
    -- '[)' is explicit (inclusive start, exclusive end) so a slot ending at
    -- 10:00 and one starting at 10:00 are treated as touching, not
    -- overlapping - this is what makes back-to-back slots legal.
    CONSTRAINT excl_slots_no_overlap EXCLUDE USING gist (
        user_id WITH =,
        tstzrange(start_time, end_time, '[)') WITH &&
    )
);

CREATE INDEX idx_slots_user_time ON slots (user_id, start_time);
