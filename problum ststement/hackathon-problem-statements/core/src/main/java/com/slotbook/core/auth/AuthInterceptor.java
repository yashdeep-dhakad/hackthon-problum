package com.slotbook.core.auth;

import com.slotbook.core.members.Member;
import com.slotbook.core.members.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/** Reads "Authorization: Bearer <token>" and remembers who is calling. Endpoints decide whether that is required. */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    static final String MEMBER_ATTRIBUTE = "slotbook.member";

    private final JdbcClient jdbc;
    private final MemberRepository members;

    public AuthInterceptor(JdbcClient jdbc, MemberRepository members) {
        this.jdbc = jdbc;
        this.members = members;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            Optional<Long> memberId = jdbc.sql("select member_id from sessions where token = :token")
                    .param("token", token)
                    .query(Long.class)
                    .optional();
            memberId.flatMap(members::findById)
                    .map(Member::view)
                    .ifPresent(m -> request.setAttribute(MEMBER_ATTRIBUTE, m));
        }
        return true;
    }
}
