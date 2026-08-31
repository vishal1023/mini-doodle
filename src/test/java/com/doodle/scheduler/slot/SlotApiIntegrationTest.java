package com.doodle.scheduler.slot;

import com.doodle.scheduler.AbstractIntegrationTest;
import com.doodle.scheduler.user.User;
import com.doodle.scheduler.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SlotApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    private UUID createUser() {
        User user = userRepository.save(new User("Test User", "user-" + UUID.randomUUID() + "@example.com"));
        return user.getId();
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

    private void createSlot(UUID userId, String startTime, int durationMinutes) throws Exception {
        mockMvc.perform(post("/api/v1/users/{userId}/slots", userId)
                        .contentType("application/json")
                        .content("""
                                {"startTime": "%s", "durationMinutes": %d}
                                """.formatted(startTime, durationMinutes)))
                .andExpect(status().isCreated());
    }
}
