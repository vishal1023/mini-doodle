package com.doodle.scheduler.slot;

import com.doodle.scheduler.AbstractIntegrationTest;
import com.doodle.scheduler.user.User;
import com.doodle.scheduler.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Slot has no JPA entity/repository yet (that lands with the actual slot
// endpoints) - this proves the overlap-exclusion constraint at the SQL level
// in isolation, via raw JDBC against the migrated schema.
class SlotsTableConstraintTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    private UUID createUser() {
        User user = userRepository.save(new User("Test User", "user-" + UUID.randomUUID() + "@example.com"));
        return user.getId();
    }

    private void insertSlot(UUID userId, UUID slotId, String start, String end) {
        jdbcTemplate.update(
                "INSERT INTO slots (id, user_id, start_time, end_time, status) VALUES (?, ?, ?::timestamptz, ?::timestamptz, 'FREE')",
                slotId, userId, start, end);
    }

    @Test
    void overlappingSlotsForSameUser_areRejected() {
        UUID userId = createUser();
        insertSlot(userId, UUID.randomUUID(), "2026-01-01T10:00:00Z", "2026-01-01T11:00:00Z");

        assertThatThrownBy(() ->
                insertSlot(userId, UUID.randomUUID(), "2026-01-01T10:30:00Z", "2026-01-01T11:30:00Z"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void touchingSlotsForSameUser_areAccepted() {
        UUID userId = createUser();
        insertSlot(userId, UUID.randomUUID(), "2026-01-01T10:00:00Z", "2026-01-01T11:00:00Z");
        insertSlot(userId, UUID.randomUUID(), "2026-01-01T11:00:00Z", "2026-01-01T12:00:00Z");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM slots WHERE user_id = ?", Integer.class, userId);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void overlappingSlotsForDifferentUsers_areAccepted() {
        UUID userA = createUser();
        UUID userB = createUser();
        insertSlot(userA, UUID.randomUUID(), "2026-01-01T10:00:00Z", "2026-01-01T11:00:00Z");
        insertSlot(userB, UUID.randomUUID(), "2026-01-01T10:00:00Z", "2026-01-01T11:00:00Z");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM slots WHERE user_id IN (?, ?)", Integer.class, userA, userB);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void slotWithEndBeforeStart_isRejected() {
        UUID userId = createUser();

        assertThatThrownBy(() ->
                insertSlot(userId, UUID.randomUUID(), "2026-01-01T11:00:00Z", "2026-01-01T10:00:00Z"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void slotWithInvalidStatus_isRejected() {
        UUID userId = createUser();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO slots (id, user_id, start_time, end_time, status) VALUES (?, ?, ?::timestamptz, ?::timestamptz, 'BOGUS')",
                UUID.randomUUID(), userId, "2026-01-01T10:00:00Z", "2026-01-01T11:00:00Z"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
