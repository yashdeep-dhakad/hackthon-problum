package com.slotbook.modules.reminders;

import com.slotbook.contract.MemberView;
import com.slotbook.core.testing.SlotBookTest;
import com.slotbook.core.testing.TestHost;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SlotBookTest
class RemindersTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    TestHost host;

    @Test
    void confirmationIsSentOnceToMembersWhoOptedIn() throws Exception {
        MemberView member = host.newMember("Reminded");
        mvc.perform(put("/api/modules/reminders/settings").header("Authorization", host.auth(member))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\": true, \"minutesBefore\": 60}"))
                .andExpect(status().isOk());

        host.book(member, host.newSlot(5));
        host.deliverEvents();

        assertEquals(1, host.notificationsFor(member).size());
        assertTrue(host.notificationsFor(member).get(0).startsWith("Booked: "));
    }

    @Test
    void nothingIsSentWithoutOptingIn() {
        MemberView member = host.newMember("Quiet");
        host.book(member, host.newSlot(5));
        host.deliverEvents();

        assertEquals(0, host.notificationsFor(member).size());
    }
}
