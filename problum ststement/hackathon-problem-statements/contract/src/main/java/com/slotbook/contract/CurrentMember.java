package com.slotbook.contract;

/**
 * The member making the current HTTP request. Use it in every module endpoint.
 */
public interface CurrentMember {

    /** The signed-in member. Throws {@link NotSignedInException} (HTTP 401) when nobody is signed in. */
    MemberView require();

    /** The signed-in admin. Throws {@link NotSignedInException} (401) or {@link ForbiddenException} (403). */
    MemberView requireAdmin();
}
