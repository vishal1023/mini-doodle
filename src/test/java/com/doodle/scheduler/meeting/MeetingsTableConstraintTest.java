package com.doodle.scheduler.meeting;

import com.doodle.scheduler.AbstractIntegrationTest;
import com.doodle.scheduler.slot.Slot;
import com.doodle.scheduler.slot.SlotRepository;
import com.doodle.scheduler.user.User;
import com.doodle.scheduler.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// No Meeting entity/repository yet (that lands with the booking endpoint) -
// this proves the meetings/meeting_participants constraints at the SQL level
// in isolation, via raw JDBC against the migrated schema.
class MeetingsTableConstraintTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SlotRepository slotRepository;

    private UUID createUser() {
        User user = userRepository.save(new User("Test User", "user-" + UUID.randomUUID() + "@example.com"));
        return user.getId();
    }

    private UUID createSlot(UUID userId) {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        Slot slot = slotRepository.save(new Slot(userId, start, start.plusSeconds(1800)));
        return slot.getId();
    }

    private void insertMeeting(UUID id, UUID slotId, UUID organizerId, String status) {
        jdbcTemplate.update(
                "INSERT INTO meetings (id, slot_id, organizer_id, title, status) VALUES (?, ?, ?, 'Sync', ?)",
                id, slotId, organizerId, status);
    }

    private void insertParticipant(UUID meetingId, UUID userId) {
        jdbcTemplate.update(
                "INSERT INTO meeting_participants (meeting_id, user_id) VALUES (?, ?)",
                meetingId, userId);
    }

    @Test
    void schedulingAMeetingForASlot_succeeds() {
        UUID organizerId = createUser();
        UUID slotId = createSlot(organizerId);

        insertMeeting(UUID.randomUUID(), slotId, organizerId, "SCHEDULED");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM meetings WHERE slot_id = ?", Integer.class, slotId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void secondActiveMeetingForSameSlot_isRejected() {
        UUID organizerId = createUser();
        UUID slotId = createSlot(organizerId);
        insertMeeting(UUID.randomUUID(), slotId, organizerId, "SCHEDULED");

        assertThatThrownBy(() -> insertMeeting(UUID.randomUUID(), slotId, organizerId, "SCHEDULED"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void reBookingAfterCancellation_succeeds() {
        UUID organizerId = createUser();
        UUID slotId = createSlot(organizerId);
        UUID firstMeetingId = UUID.randomUUID();
        insertMeeting(firstMeetingId, slotId, organizerId, "SCHEDULED");

        jdbcTemplate.update("UPDATE meetings SET status = 'CANCELLED' WHERE id = ?", firstMeetingId);
        insertMeeting(UUID.randomUUID(), slotId, organizerId, "SCHEDULED");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM meetings WHERE slot_id = ?", Integer.class, slotId);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void meetingWithNonExistentSlot_isRejected() {
        UUID organizerId = createUser();

        assertThatThrownBy(() -> insertMeeting(UUID.randomUUID(), UUID.randomUUID(), organizerId, "SCHEDULED"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void meetingWithInvalidStatus_isRejected() {
        UUID organizerId = createUser();
        UUID slotId = createSlot(organizerId);

        assertThatThrownBy(() -> insertMeeting(UUID.randomUUID(), slotId, organizerId, "BOGUS"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void addingAParticipant_succeeds() {
        UUID organizerId = createUser();
        UUID slotId = createSlot(organizerId);
        UUID meetingId = UUID.randomUUID();
        insertMeeting(meetingId, slotId, organizerId, "SCHEDULED");
        UUID participantId = createUser();

        insertParticipant(meetingId, participantId);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM meeting_participants WHERE meeting_id = ?", Integer.class, meetingId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void duplicateParticipant_isRejected() {
        UUID organizerId = createUser();
        UUID slotId = createSlot(organizerId);
        UUID meetingId = UUID.randomUUID();
        insertMeeting(meetingId, slotId, organizerId, "SCHEDULED");
        UUID participantId = createUser();
        insertParticipant(meetingId, participantId);

        assertThatThrownBy(() -> insertParticipant(meetingId, participantId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void participantWithNonExistentUser_isRejected() {
        UUID organizerId = createUser();
        UUID slotId = createSlot(organizerId);
        UUID meetingId = UUID.randomUUID();
        insertMeeting(meetingId, slotId, organizerId, "SCHEDULED");

        assertThatThrownBy(() -> insertParticipant(meetingId, UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
