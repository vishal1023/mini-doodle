-- btree_gist backs the EXCLUDE constraint on slots that prevents overlapping
-- time ranges for the same user, enforced atomically at the database level.
CREATE EXTENSION IF NOT EXISTS btree_gist;
