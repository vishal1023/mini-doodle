CREATE TABLE meetings (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slot_id      UUID NOT NULL REFERENCES slots(id) ON DELETE RESTRICT,
    organizer_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    status       VARCHAR(10) NOT NULL DEFAULT 'SCHEDULED',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    cancelled_at TIMESTAMPTZ,

    CONSTRAINT chk_meetings_status CHECK (status IN ('SCHEDULED', 'CANCELLED'))
);

-- Deleting a Slot/User must never silently cascade away a Meeting - it's the
-- durable historical record, so ON DELETE RESTRICT (not CASCADE) on both FKs.

-- A slot may have at most one *active* meeting at a time, but a cancelled
-- meeting must not block re-booking the same slot later - a partial unique
-- index (rather than a plain UNIQUE constraint on slot_id) enforces exactly
-- that: uniqueness only among SCHEDULED rows, CANCELLED rows are exempt.
CREATE UNIQUE INDEX uq_meetings_active_slot ON meetings (slot_id) WHERE status = 'SCHEDULED';

CREATE INDEX idx_meetings_organizer ON meetings (organizer_id);
