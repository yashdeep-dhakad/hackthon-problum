package com.slotbook.modules.waitlist;

import com.slotbook.contract.MemberView;
import com.slotbook.core.testing.SlotBookTest;
import com.slotbook.core.testing.TestHost;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The waitlist's contract tests. The endpoints are described in TICKETS.md (T2). */
@SlotBookTest
class WaitlistContractTest {

    static final String BASE = "/api/modules/waitlist/slots/";

    @Autowired
    MockMvc mvc;

    @Autowired
    TestHost host;

    @Test
    void membersJoinAFullSlotInOrder() throws Exception {
        long slot = host.newSlot(2);
        host.fill(slot);

        mvc.perform(post(BASE + slot + "/join").header("Authorization", host.auth(host.newMember("First"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(1));
        mvc.perform(post(BASE + slot + "/join").header("Authorization", host.auth(host.newMember("Second"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(2));
    }

    @Test
    void cannotJoinWhileSeatsAreFree() throws Exception {
        long slot = host.newSlot(2);

        mvc.perform(post(BASE + slot + "/join").header("Authorization", host.auth(host.newMember("Early"))))
                .andExpect(status().isConflict());
    }

    @Test
    void memberCanLeave() throws Exception {
        long slot = host.newSlot(1);
        host.fill(slot);
        String auth = host.auth(host.newMember("Leaver"));

        mvc.perform(post(BASE + slot + "/join").header("Authorization", auth)).andExpect(status().isOk());
        mvc.perform(delete(BASE + slot + "/me").header("Authorization", auth)).andExpect(status().is2xxSuccessful());

        mvc.perform(get(BASE + slot).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(0))
                .andExpect(jsonPath("$.myPosition").value(nullValue()));
    }

    @Test
    void firstInLineGetsTheFreedSeat() throws Exception {
        long slot = host.newSlot(1);
        List<Long> booked = host.fill(slot);
        MemberView waiting = host.newMember("Waiting");
        String auth = host.auth(waiting);
        mvc.perform(post(BASE + slot + "/join").header("Authorization", auth)).andExpect(status().isOk());

        host.cancel(booked.get(0));
        host.deliverEvents();
        host.deliverEvents();

        assertTrue(host.hasConfirmedBooking(waiting, slot), "the first member on the waitlist should be booked");
        assertFalse(host.notificationsFor(waiting).isEmpty(), "the promoted member should be notified");
        mvc.perform(get(BASE + slot).header("Authorization", auth))
                .andExpect(jsonPath("$.size").value(0))
                .andExpect(jsonPath("$.myPosition").value(nullValue()));
    }

    @Test
    void adminCanClearAWaitlist() throws Exception {
        long slot = host.newSlot(1);
        host.fill(slot);
        mvc.perform(post(BASE + slot + "/join").header("Authorization", host.auth(host.newMember("Cleared"))))
                .andExpect(status().isOk());

        String admin = host.auth(host.admin());
        mvc.perform(delete("/api/modules/waitlist/admin/slots/" + slot).header("Authorization", admin))
                .andExpect(status().is2xxSuccessful());
        mvc.perform(get("/api/modules/waitlist/admin/slots/" + slot).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(0));
    }
}
