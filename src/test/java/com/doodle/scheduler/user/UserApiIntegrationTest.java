package com.doodle.scheduler.user;

import com.doodle.scheduler.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createUser_returnsCreatedUserWithLocationHeader() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content("""
                                {"name": "Ada Lovelace", "email": "ada@example.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("^/api/v1/users/[0-9a-fA-F-]{36}$")))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Ada Lovelace"))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void createUser_withBlankName_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content("""
                                {"name": "", "email": "ada@example.com"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_withInvalidEmail_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content("""
                                {"name": "Ada Lovelace", "email": "not-an-email"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUser_afterCreate_returnsUser() throws Exception {
        String location = mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content("""
                                {"name": "Grace Hopper", "email": "grace@example.com"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Grace Hopper"))
                .andExpect(jsonPath("$.email").value("grace@example.com"));
    }

    @Test
    void getUser_nonExistentId_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }
}
