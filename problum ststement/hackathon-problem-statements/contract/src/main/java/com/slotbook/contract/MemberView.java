package com.slotbook.contract;

/** A member as modules see it. See CONTRACT.md before showing name or email to anyone but this member. */
public record MemberView(long id, String name, String email, boolean admin) {
}
