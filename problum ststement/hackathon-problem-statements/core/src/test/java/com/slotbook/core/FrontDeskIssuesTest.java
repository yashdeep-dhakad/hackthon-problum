package com.slotbook.core;

import com.slotbook.contract.CentreClock;
import com.slotbook.contract.MemberView;
import com.slotbook.core.testing.SlotBookTest;
import com.slotbook.core.testing.TestHost;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SlotBookTest
class FrontDeskIssuesTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    TestHost host;

    @Autowired
    CentreClock centreClock;

    @Test
    void t1_1_morningClassGroupedUnderCentreTimezoneDay() throws Exception {
        // Create a Monday 5:00 AM class in CentreClock.zone() (Asia/Kolkata).
        // 5:00 AM IST corresponds to Sunday 23:30 UTC.
        LocalDate nextMonday = LocalDate.now(centreClock.zone()).plusWeeks(1)
                .with(java.time.DayOfWeek.MONDAY);
        ZonedDateTime monday5am = ZonedDateTime.of(nextMonday, LocalTime.of(5, 0), centreClock.zone());
        long slotId = host.newSlot(10, monday5am.toInstant());

        // 1. Querying for Monday's date should include the slot.
        mvc.perform(get("/api/slots?date=" + nextMonday))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + slotId + ")]").exists());

        // Querying for Sunday's date should NOT include the slot.
        LocalDate previousSunday = nextMonday.minusDays(1);
        mvc.perform(get("/api/slots?date=" + previousSunday))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + slotId + ")]").doesNotExist());

        // 2. Booking the class and checking /api/bookings/mine groups it under Monday.
        MemberView member = host.newMember("Morning Yogi");
        String auth = host.auth(member);
        host.book(member, slotId);

        mvc.perform(get("/api/bookings/mine").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slot.id == " + slotId + ")].date").value(nextMonday.toString()));
    }

    @Test
    void t1_3_duplicateBookingPreventedAndAttendanceDeduplicated() throws Exception {
        MemberView member = host.newMember("Chess Player");
        String auth = host.auth(member);
        long slotId = host.newSlot(2);

        // First booking succeeds
        mvc.perform(post("/api/bookings").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"slotId\": " + slotId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("BOOKED"));

        // Second booking attempt by the same member returns ALREADY_BOOKED (no duplicate confirmed booking)
        mvc.perform(post("/api/bookings").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"slotId\": " + slotId + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.outcome").value("ALREADY_BOOKED"));

        // Admin attendance list shows the member only once
        String adminAuth = host.auth(host.admin());
        mvc.perform(get("/api/admin/slots/" + slotId + "/attendance").header("Authorization", adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].memberId").value(member.id()));
    }

    @Test
    void t1_5_newMembersAndMembersWithNullPlanGetDefaultPlan() throws Exception {
        // 1. New member signup
        String email = "fresh." + java.util.UUID.randomUUID() + "@slotbook.test";
        String signupResp = mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Fresh Member\", \"email\": \"" + email + "\", \"password\": \"secret123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = signupResp.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        // Visiting /api/me/plan must succeed with default Starter plan
        mvc.perform(get("/api/me/plan").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("Starter"))
                .andExpect(jsonPath("$.classesPerMonth").value(8))
                .andExpect(jsonPath("$.monthlyPrice").value(999.0))
                .andExpect(jsonPath("$.classesUsedThisMonth").value(0));

        // 2. Existing seeded member Rohan Gupta (id 33, signed up last week with plan_id = null)
        MemberView rohan = new MemberView(33, "Rohan Gupta", "rohan@slotbook.test", false);
        String rohanAuth = host.auth(rohan);
        mvc.perform(get("/api/me/plan").header("Authorization", rohanAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("Starter"))
                .andExpect(jsonPath("$.classesPerMonth").value(8));
    }

    @Test
    void t1_7_octoberRefundReportMatchesGatewayTotal() throws Exception {
        String adminAuth = host.auth(host.admin());

        // Must match docs/gateway-refunds-2026-10.csv exactly (30 refunds totaling 6853.50)
        mvc.perform(get("/api/admin/reports/refunds?month=2026-10").header("Authorization", adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-10"))
                .andExpect(jsonPath("$.refunds").value(30))
                .andExpect(jsonPath("$.total").value(6853.50));
    }
}
