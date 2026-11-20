package com.slotbook.core;

import com.slotbook.contract.MemberView;
import com.slotbook.core.testing.SlotBookTest;
import com.slotbook.core.testing.TestHost;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SlotBookTest
class BookingFlowTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    TestHost host;

    @Test
    void memberBooksAndCancels() throws Exception {
        MemberView member = host.newMember("Booker");
        String auth = host.auth(member);
        long slot = host.newSlot(5);

        String body = mvc.perform(post("/api/bookings").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"slotId\": " + slot + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("BOOKED"))
                .andReturn().getResponse().getContentAsString();
        long bookingId = Long.parseLong(body.replaceAll(".*\"bookingId\":(\\d+).*", "$1"));
        assertEquals(1, host.confirmedCount(slot));

        mvc.perform(post("/api/bookings/" + bookingId + "/cancel").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refund").value(405.0));
        assertEquals(0, host.confirmedCount(slot));
    }

    @Test
    void fullSlotRefusesBooking() throws Exception {
        long slot = host.newSlot(2);
        host.fill(slot);
        String auth = host.auth(host.newMember("Late"));

        mvc.perform(post("/api/bookings").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"slotId\": " + slot + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.outcome").value("FULL"));
    }

    @Test
    void cannotCancelSomeoneElsesBooking() throws Exception {
        long slot = host.newSlot(5);
        long theirs = host.book(host.newMember("Owner"), slot);
        String auth = host.auth(host.newMember("Other"));

        mvc.perform(post("/api/bookings/" + theirs + "/cancel").header("Authorization", auth))
                .andExpect(status().isForbidden());
    }

    @Test
    void endpointsNeedSignIn() throws Exception {
        mvc.perform(get("/api/bookings/mine")).andExpect(status().isUnauthorized());
    }
}
