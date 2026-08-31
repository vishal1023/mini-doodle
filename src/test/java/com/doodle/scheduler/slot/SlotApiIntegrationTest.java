package com.doodle.scheduler.slot;

import com.doodle.scheduler.AbstractIntegrationTest;
import com.doodle.scheduler.user.User;
import com.doodle.scheduler.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SlotApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID createUser() {
        User user = userRepository.save(new User("Test User", "user-" + UUID.randomUUID() + "@example.com"));
        return user.getId();
    }

    private UUID createSlotAndReturnId(UUID userId, String startTime, int durationMinutes) throws Exception {
        String body = mockMvc.perform(post("/api/v1/users/{userId}/slots", userId)
                        .contentType("application/json")
                        .content("""
                                {"startTime": "%s", "durationMinutes": %d}
                                """.formatted(startTime, durationMinutes)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("id").asText());
    }

    private void markBusy(UUID slotId) {
        jdbcTemplate.update("UPDATE slots SET status = 'BUSY' WHERE id = ?", slotId);
    }

    @Test
    void createSlot_returnsCreatedSlotWithLocationHeader() throws Exception {
        UUID userId = createUser();

        mockMvc.perform(post("/api/v1/users/{userId}/slots", userId)
                        .contentType("application/json")
                        .content("""
                                {"startTime": "2026-01-01T10:00:00Z", "durationMinutes": 30}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.startTime").value("2026-01-01T10:00:00Z"))
                .andExpect(jsonPath("$.endTime").value("2026-01-01T10:30:00Z"))
                .andExpect(jsonPath("$.status").value("FREE"));
    }

    @Test
    void createSlot_forNonExistentUser_returnsNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/users/{userId}/slots", UUID.randomUUID())
                        .contentType("application/json")
                        .content("""
                                {"startTime": "2026-01-01T10:00:00Z", "durationMinutes": 30}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void listSlots_returnsCreatedSlots() throws Exception {
        UUID userId = createUser();
        createSlot(userId, "2026-02-01T09:00:00Z", 30);
        createSlot(userId, "2026-02-01T10:00:00Z", 30);

        mockMvc.perform(get("/api/v1/users/{userId}/slots", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void listSlots_filtersByTimeRange() throws Exception {
        UUID userId = createUser();
        createSlot(userId, "2026-03-01T09:00:00Z", 30);
        createSlot(userId, "2026-03-01T14:00:00Z", 30);

        mockMvc.perform(get("/api/v1/users/{userId}/slots", userId)
                        .param("from", "2026-03-01T13:00:00Z")
                        .param("to", "2026-03-01T15:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].startTime").value("2026-03-01T14:00:00Z"));
    }

    @Test
    void listSlots_filtersByStatus() throws Exception {
        UUID userId = createUser();
        createSlot(userId, "2026-04-01T09:00:00Z", 30);

        mockMvc.perform(get("/api/v1/users/{userId}/slots", userId).param("status", "FREE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/users/{userId}/slots", userId).param("status", "BUSY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listSlots_forNonExistentUser_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/{userId}/slots", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateSlot_whenFree_returnsUpdatedSlot() throws Exception {
        UUID userId = createUser();
        UUID slotId = createSlotAndReturnId(userId, "2026-05-01T09:00:00Z", 30);

        mockMvc.perform(patch("/api/v1/users/{userId}/slots/{slotId}", userId, slotId)
                        .contentType("application/json")
                        .content("""
                                {"startTime": "2026-05-01T11:00:00Z", "durationMinutes": 45}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startTime").value("2026-05-01T11:00:00Z"))
                .andExpect(jsonPath("$.endTime").value("2026-05-01T11:45:00Z"));
    }

    @Test
    void updateSlot_whenBusy_returnsConflict() throws Exception {
        UUID userId = createUser();
        UUID slotId = createSlotAndReturnId(userId, "2026-05-02T09:00:00Z", 30);
        markBusy(slotId);

        mockMvc.perform(patch("/api/v1/users/{userId}/slots/{slotId}", userId, slotId)
                        .contentType("application/json")
                        .content("""
                                {"startTime": "2026-05-02T11:00:00Z", "durationMinutes": 45}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void updateSlot_nonExistentSlot_returnsNotFound() throws Exception {
        UUID userId = createUser();

        mockMvc.perform(patch("/api/v1/users/{userId}/slots/{slotId}", userId, UUID.randomUUID())
                        .contentType("application/json")
                        .content("""
                                {"startTime": "2026-05-02T11:00:00Z", "durationMinutes": 45}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateSlot_belongingToDifferentUser_returnsNotFound() throws Exception {
        UUID ownerId = createUser();
        UUID otherUserId = createUser();
        UUID slotId = createSlotAndReturnId(ownerId, "2026-05-03T09:00:00Z", 30);

        mockMvc.perform(patch("/api/v1/users/{userId}/slots/{slotId}", otherUserId, slotId)
                        .contentType("application/json")
                        .content("""
                                {"startTime": "2026-05-03T11:00:00Z", "durationMinutes": 45}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteSlot_whenFree_returnsNoContent() throws Exception {
        UUID userId = createUser();
        UUID slotId = createSlotAndReturnId(userId, "2026-05-04T09:00:00Z", 30);

        mockMvc.perform(delete("/api/v1/users/{userId}/slots/{slotId}", userId, slotId))
                .andExpect(status().isNoContent());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM slots WHERE id = ?", Integer.class, slotId);
        org.assertj.core.api.Assertions.assertThat(count).isZero();
    }

    @Test
    void deleteSlot_whenBusy_returnsConflict() throws Exception {
        UUID userId = createUser();
        UUID slotId = createSlotAndReturnId(userId, "2026-05-05T09:00:00Z", 30);
        markBusy(slotId);

        mockMvc.perform(delete("/api/v1/users/{userId}/slots/{slotId}", userId, slotId))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteSlot_nonExistentSlot_returnsNotFound() throws Exception {
        UUID userId = createUser();

        mockMvc.perform(delete("/api/v1/users/{userId}/slots/{slotId}", userId, UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private void createSlot(UUID userId, String startTime, int durationMinutes) throws Exception {
        mockMvc.perform(post("/api/v1/users/{userId}/slots", userId)
                        .contentType("application/json")
                        .content("""
                                {"startTime": "%s", "durationMinutes": %d}
                                """.formatted(startTime, durationMinutes)))
                .andExpect(status().isCreated());
    }

    @Test
    void getAvailability_mergesTouchingSlotsOfSameStatus() throws Exception {
        UUID userId = createUser();
        createSlot(userId, "2026-06-01T09:00:00Z", 60);
        createSlot(userId, "2026-06-01T10:00:00Z", 60);

        mockMvc.perform(get("/api/v1/users/{userId}/availability", userId)
                        .param("from", "2026-06-01T00:00:00Z")
                        .param("to", "2026-06-02T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervals.length()").value(1))
                .andExpect(jsonPath("$.intervals[0].start").value("2026-06-01T09:00:00Z"))
                .andExpect(jsonPath("$.intervals[0].end").value("2026-06-01T11:00:00Z"))
                .andExpect(jsonPath("$.intervals[0].status").value("FREE"));
    }

    @Test
    void getAvailability_clipsIntervalsToRequestedWindow() throws Exception {
        UUID userId = createUser();
        createSlot(userId, "2026-06-02T09:00:00Z", 120);

        mockMvc.perform(get("/api/v1/users/{userId}/availability", userId)
                        .param("from", "2026-06-02T10:00:00Z")
                        .param("to", "2026-06-02T11:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervals.length()").value(1))
                .andExpect(jsonPath("$.intervals[0].start").value("2026-06-02T10:00:00Z"))
                .andExpect(jsonPath("$.intervals[0].end").value("2026-06-02T11:00:00Z"));
    }

    @Test
    void getAvailability_showsBusyStatusForBookedSlots() throws Exception {
        UUID userId = createUser();
        UUID slotId = createSlotAndReturnId(userId, "2026-06-03T09:00:00Z", 30);
        jdbcTemplate.update("UPDATE slots SET status = 'BUSY' WHERE id = ?", slotId);

        mockMvc.perform(get("/api/v1/users/{userId}/availability", userId)
                        .param("from", "2026-06-03T00:00:00Z")
                        .param("to", "2026-06-04T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervals[0].status").value("BUSY"));
    }

    @Test
    void getAvailability_noSlots_returnsEmptyIntervals() throws Exception {
        UUID userId = createUser();

        mockMvc.perform(get("/api/v1/users/{userId}/availability", userId)
                        .param("from", "2026-06-05T00:00:00Z")
                        .param("to", "2026-06-06T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervals").isEmpty());
    }

    @Test
    void getAvailability_toBeforeFrom_returnsBadRequest() throws Exception {
        UUID userId = createUser();

        mockMvc.perform(get("/api/v1/users/{userId}/availability", userId)
                        .param("from", "2026-06-05T00:00:00Z")
                        .param("to", "2026-06-01T00:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAvailability_windowExceeding31Days_returnsBadRequest() throws Exception {
        UUID userId = createUser();

        mockMvc.perform(get("/api/v1/users/{userId}/availability", userId)
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-03-01T00:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAvailability_forNonExistentUser_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/{userId}/availability", UUID.randomUUID())
                        .param("from", "2026-06-05T00:00:00Z")
                        .param("to", "2026-06-06T00:00:00Z"))
                .andExpect(status().isNotFound());
    }
}
