package com.slotbook.core.members;

import com.slotbook.contract.MemberView;

import java.time.Instant;

public record Member(long id, String name, String email, String passwordHash, String role, Long planId, Instant createdAt) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public MemberView view() {
        return new MemberView(id, name, email, isAdmin());
    }
}
