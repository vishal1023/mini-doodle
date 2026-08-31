package com.doodle.scheduler.meeting;

import com.doodle.scheduler.AbstractIntegrationTest;
import com.doodle.scheduler.slot.Slot;
import com.doodle.scheduler.slot.SlotRepository;
import com.doodle.scheduler.user.User;
import com.doodle.scheduler.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MeetingApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID createUser() {
        User user = userRepository.save(new User("Test User", "user-" + UUID.randomUUID() + "@example.com"));
        return user.getId();
    }

    private UUID createSlot(UUID userId) {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        Slot slot = slotRepository.save(new Slot(userId, start, start.plusSeconds(1800)));
        return slot.getId();
    }

    private UUID bookSlotAndReturnMeetingId(UUID organizerId, UUID slotId) throws Exception {
        return bookSlotAndReturnMeetingId(organizerId, slotId, List.of());
    }

    private UUID bookSlotAndReturnMeetingId(UUID organizerId, UUID slotId, List<UUID> participantIds) throws Exception {
        String participantsJson = participantIds.stream()
                .map(id -> "\"" + id + "\"")
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        String body = mockMvc.perform(post("/api/v1/users/{userId}/slots/{slotId}/meetings", organizerId, slotId)
                        .contentType("application/json")
                        .content("""
                                {"title": "Sync", "participantUserIds": [%s]}
                                """.formatted(participantsJson)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("id").asText());
    }

    @Test
    void bookSlot_whenFree_returnsCreatedMeetingAndMarksSlotBusy() throws Exception {
        UUID organizerId = createUser();
        UUID slotId = createSlot(organizerId);
        UUID participantId = createUser();

        mockMvc.perform(post("/api/v1/users/{userId}/slots/{slotId}/meetings", organizerId, slotId)
                        .contentType("application/json")
                        .content("""
                                {"title": "Sync", "description": "Weekly sync", "participantUserIds": ["%s"]}
                                """.formatted(participantId)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.slotId").value(slotId.toString()))
                .andExpect(jsonPath("$.organizerId").value(organizerId.toString()))
                .andExpect(jsonPath("$.title").value("Sync"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.participantUserIds[0]").value(participantId.toString()));

        String slotStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM slots WHERE id = ?", String.class, slotId);
        assertThat(slotStatus).isEqualTo("BUSY");
    }

    @Test
    void bookSlot_whenAlreadyBusy_returnsConflict() throws Exception {
        UUID organizerId = createUser();
        UUID slotId = createSlot(organizerId);
        jdbcTemplate.update("UPDATE slots SET status = 'BUSY' WHERE id = ?", slotId);

        mockMvc.perform(post("/api/v1/users/{userId}/slots/{slotId}/meetings", organizerId, slotId)
                        .contentType("application/json")
                        .content("""
                                {"title": "Sync", "participantUserIds": []}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void bookSlot_forNonExistentUser_returnsNotFound() throws Exception {
        UUID organizerId = createUser();
        UUID slotId = createSlot(organizerId);

        mockMvc.perform(post("/api/v1/users/{userId}/slots/{slotId}/meetings", UUID.randomUUID(), slotId)
                        .contentType("application/json")
                        .content("""
                                {"title": "Sync", "participantUserIds": []}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void bookSlot_forNonExistentSlot_returnsNotFound() throws Exception {
        UUID organizerId = createUser();

        mockMvc.perform(post("/api/v1/users/{userId}/slots/{slotId}/meetings", organizerId, UUID.randomUUID())
                        .contentType("application/json")
                        .content("""
                                {"title": "Sync", "participantUserIds": []}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void bookSlot_forSlotBelongingToDifferentUser_returnsNotFound() throws Exception {
        UUID ownerId = createUser();
        UUID otherUserId = createUser();
        UUID slotId = createSlot(ownerId);

        mockMvc.perform(post("/api/v1/users/{userId}/slots/{slotId}/meetings", otherUserId, slotId)
                        .contentType("application/json")
                        .content("""
                                {"title": "Sync", "participantUserIds": []}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void bookSlot_withNonExistentParticipant_returnsBadRequest() throws Exception {
        UUID organizerId = createUser();
        UUID slotId = createSlot(organizerId);

        mockMvc.perform(post("/api/v1/users/{userId}/slots/{slotId}/meetings", organizerId, slotId)
                        .contentType("application/json")
                        .content("""
                                {"title": "Sync", "participantUserIds": ["%s"]}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bookSlot_withBlankTitle_returnsBadRequest() throws Exception {
        UUID organizerId = createUser();
        UUID slotId = createSlot(organizerId);

        mockMvc.perform(post("/api/v1/users/{userId}/slots/{slotId}/meetings", organizerId, slotId)
                        .contentType("application/json")
                        .content("""
                                {"title": "", "participantUserIds": []}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void concurrentBookingAttempts_onlyOneSucceeds() throws Exception {
        UUID organizerId = createUser();
        UUID slotId = createSlot(organizerId);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        // Align every thread at the same starting line so they genuinely
        // contend for the row lock, rather than the pool serializing them
        // and proving nothing about the concurrency guarantee.
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<CompletableFuture<Integer>> futures = IntStream.range(0, threadCount)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    readyLatch.countDown();
                    try {
                        startLatch.await();
                        return mockMvc.perform(post("/api/v1/users/{userId}/slots/{slotId}/meetings",
                                        organizerId, slotId)
                                        .contentType("application/json")
                                        .content("""
                                                {"title": "Attempt %d", "participantUserIds": []}
                                                """.formatted(i)))
                                .andReturn().getResponse().getStatus();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, executor))
                .toList();

        readyLatch.await();
        startLatch.countDown();
        List<Integer> statuses = futures.stream().map(CompletableFuture::join).toList();
        executor.shutdown();

        long successCount = statuses.stream().filter(s -> s == 201).count();
        long conflictCount = statuses.stream().filter(s -> s == 409).count();
        assertThat(successCount).isEqualTo(1);
        assertThat(conflictCount).isEqualTo(threadCount - 1);

        Integer meetingCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM meetings WHERE slot_id = ?", Integer.class, slotId);
        assertThat(meetingCount).isEqualTo(1);

        String slotStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM slots WHERE id = ?", String.class, slotId);
        assertThat(slotStatus).isEqualTo("BUSY");
    }

    @Test
    void cancelMeeting_revertsSlotToFreeAndSoftCancelsMeeting() throws Exception {
        UUID organizerId = createUser();
        UUID slotId = createSlot(organizerId);
        UUID meetingId = bookSlotAndReturnMeetingId(organizerId, slotId);

        mockMvc.perform(delete("/api/v1/meetings/{meetingId}", meetingId))
                .andExpect(status().isNoContent());

        String meetingStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM meetings WHERE id = ?", String.class, meetingId);
        assertThat(meetingStatus).isEqualTo("CANCELLED");

        Instant cancelledAt = jdbcTemplate.queryForObject(
                "SELECT cancelled_at FROM meetings WHERE id = ?", Instant.class, meetingId);
        assertThat(cancelledAt).isNotNull();

        String slotStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM slots WHERE id = ?", String.class, slotId);
        assertThat(slotStatus).isEqualTo("FREE");
    }

    @Test
    void cancelMeeting_allowsRebookingTheSameSlot() throws Exception {
        UUID organizerId = createUser();
        UUID slotId = createSlot(organizerId);
        UUID firstMeetingId = bookSlotAndReturnMeetingId(organizerId, slotId);

        mockMvc.perform(delete("/api/v1/meetings/{meetingId}", firstMeetingId))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/users/{userId}/slots/{slotId}/meetings", organizerId, slotId)
                        .contentType("application/json")
                        .content("""
                                {"title": "Rebooked", "participantUserIds": []}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void cancelMeeting_alreadyCancelled_returnsConflict() throws Exception {
        UUID organizerId = createUser();
        UUID slotId = createSlot(organizerId);
        UUID meetingId = bookSlotAndReturnMeetingId(organizerId, slotId);
        mockMvc.perform(delete("/api/v1/meetings/{meetingId}", meetingId))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/meetings/{meetingId}", meetingId))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelMeeting_nonExistent_returnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/meetings/{meetingId}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMeeting_returnsDetails() throws Exception {
        UUID organizerId = createUser();
        UUID slotId = createSlot(organizerId);
        UUID meetingId = bookSlotAndReturnMeetingId(organizerId, slotId);

        mockMvc.perform(get("/api/v1/meetings/{meetingId}", meetingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(meetingId.toString()))
                .andExpect(jsonPath("$.title").value("Sync"));
    }

    @Test
    void getMeeting_nonExistent_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/meetings/{meetingId}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateMeeting_whenScheduled_returnsUpdatedMeeting() throws Exception {
        UUID organizerId = createUser();
        UUID slotId = createSlot(organizerId);
        UUID meetingId = bookSlotAndReturnMeetingId(organizerId, slotId);
        UUID participantId = createUser();

        mockMvc.perform(patch("/api/v1/meetings/{meetingId}", meetingId)
                        .contentType("application/json")
                        .content("""
                                {"title": "Renamed", "description": "Updated", "participantUserIds": ["%s"]}
                                """.formatted(participantId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed"))
                .andExpect(jsonPath("$.description").value("Updated"))
                .andExpect(jsonPath("$.participantUserIds[0]").value(participantId.toString()));
    }

    @Test
    void updateMeeting_whenCancelled_returnsConflict() throws Exception {
        UUID organizerId = createUser();
        UUID slotId = createSlot(organizerId);
        UUID meetingId = bookSlotAndReturnMeetingId(organizerId, slotId);
        mockMvc.perform(delete("/api/v1/meetings/{meetingId}", meetingId)).andExpect(status().isNoContent());

        mockMvc.perform(patch("/api/v1/meetings/{meetingId}", meetingId)
                        .contentType("application/json")
                        .content("""
                                {"title": "Renamed", "participantUserIds": []}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void updateMeeting_withNonExistentParticipant_returnsBadRequest() throws Exception {
        UUID organizerId = createUser();
        UUID slotId = createSlot(organizerId);
        UUID meetingId = bookSlotAndReturnMeetingId(organizerId, slotId);

        mockMvc.perform(patch("/api/v1/meetings/{meetingId}", meetingId)
                        .contentType("application/json")
                        .content("""
                                {"title": "Renamed", "participantUserIds": ["%s"]}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMeeting_nonExistent_returnsNotFound() throws Exception {
        mockMvc.perform(patch("/api/v1/meetings/{meetingId}", UUID.randomUUID())
                        .contentType("application/json")
                        .content("""
                                {"title": "Renamed", "participantUserIds": []}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void listMeetings_filtersByRole() throws Exception {
        UUID userA = createUser();
        UUID userB = createUser();
        UUID slotOwnedByA = createSlot(userA);
        UUID slotOwnedByB = createSlot(userB);
        bookSlotAndReturnMeetingId(userA, slotOwnedByA, List.of(userB));
        bookSlotAndReturnMeetingId(userB, slotOwnedByB, List.of(userA));

        mockMvc.perform(get("/api/v1/users/{userId}/meetings", userA).param("role", "ORGANIZER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].organizerId").value(userA.toString()));

        mockMvc.perform(get("/api/v1/users/{userId}/meetings", userA).param("role", "PARTICIPANT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].organizerId").value(userB.toString()));

        mockMvc.perform(get("/api/v1/users/{userId}/meetings", userA).param("role", "ANY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void listMeetings_forNonExistentUser_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/{userId}/meetings", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
