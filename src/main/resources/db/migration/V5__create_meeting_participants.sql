CREATE TABLE meeting_participants (
    meeting_id UUID NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,

    PRIMARY KEY (meeting_id, user_id)
);

-- Supports "meetings I'm invited to" lookups (GET /users/{userId}/meetings).
CREATE INDEX idx_meeting_participants_user ON meeting_participants (user_id);
