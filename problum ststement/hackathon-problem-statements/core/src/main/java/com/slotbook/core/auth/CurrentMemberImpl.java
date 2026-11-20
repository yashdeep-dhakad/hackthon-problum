package com.slotbook.core.auth;

import com.slotbook.contract.CurrentMember;
import com.slotbook.contract.ForbiddenException;
import com.slotbook.contract.MemberView;
import com.slotbook.contract.NotSignedInException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Component
public class CurrentMemberImpl implements CurrentMember {

    @Override
    public MemberView require() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        Object member = attributes == null
                ? null
                : attributes.getAttribute(AuthInterceptor.MEMBER_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (member == null) {
            throw new NotSignedInException();
        }
        return (MemberView) member;
    }

    @Override
    public MemberView requireAdmin() {
        MemberView member = require();
        if (!member.admin()) {
            throw new ForbiddenException("Admins only");
        }
        return member;
    }
}
