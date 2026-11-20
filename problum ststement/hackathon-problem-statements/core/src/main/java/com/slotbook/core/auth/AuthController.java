package com.slotbook.core.auth;

import com.slotbook.core.members.Member;
import com.slotbook.core.members.MemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
public class AuthController {

    public record LoginRequest(String email, String password) {
    }

    public record SignupRequest(String name, String email, String password) {
    }

    private final MemberRepository members;
    private final JdbcClient jdbc;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();

    public AuthController(MemberRepository members, JdbcClient jdbc) {
        this.members = members;
        this.jdbc = jdbc;
    }

    @PostMapping("/api/auth/login")
    public Map<String, Object> login(@RequestBody LoginRequest body) {
        Member member = members.findByEmail(body.email())
                .filter(m -> body.password() != null && passwords.matches(body.password(), m.passwordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong email or password"));
        return Map.of("token", newSession(member.id()), "member", member.view());
    }

    @PostMapping("/api/auth/signup")
    public Map<String, Object> signup(@RequestBody SignupRequest body) {
        if (body.name() == null || body.name().isBlank() || body.email() == null || !body.email().contains("@")
                || body.password() == null || body.password().length() < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name, a valid email and a password are required");
        }
        if (members.findByEmail(body.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That email is already registered");
        }
        long id = members.insert(body.name().trim(), body.email().trim(), passwords.encode(body.password()));
        Member member = members.findById(id).orElseThrow();
        return Map.of("token", newSession(id), "member", member.view());
    }

    private String newSession(long memberId) {
        String token = UUID.randomUUID().toString();
        jdbc.sql("insert into sessions (token, member_id) values (:token, :member)")
                .param("token", token)
                .param("member", memberId)
                .update();
        return token;
    }
}
