package com.slotbook.core;

import com.slotbook.core.testing.SlotBookTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SlotBookTest
class AuthTest {

    @Autowired
    MockMvc mvc;

    @Test
    void demoMemberCanLogIn() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"meera@slotbook.test\", \"password\": \"demo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member.name").value("Meera Nair"))
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void wrongPasswordIsRefused() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"meera@slotbook.test\", \"password\": \"nope\"}"))
                .andExpect(status().isUnauthorized());
    }
}
