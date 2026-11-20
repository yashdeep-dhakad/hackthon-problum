package com.slotbook.core.members;

import com.slotbook.core.config.Db;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MemberRepository {

    private static final RowMapper<Member> MEMBER = (rs, i) -> new Member(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getString("role"),
            rs.getObject("plan_id", Long.class),
            Db.instant(rs, "created_at"));

    private static final RowMapper<Plan> PLAN = (rs, i) -> new Plan(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getInt("classes_per_month"),
            rs.getDouble("monthly_price"),
            rs.getBoolean("is_default"));

    private final JdbcClient jdbc;

    public MemberRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Member> findById(long id) {
        return jdbc.sql("select * from members where id = :id").param("id", id).query(MEMBER).optional();
    }

    public Optional<Member> findByEmail(String email) {
        return jdbc.sql("select * from members where lower(email) = lower(:email)")
                .param("email", email)
                .query(MEMBER)
                .optional();
    }

    public long insert(String name, String email, String passwordHash) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("insert into members (name, email, password_hash, plan_id) values (:name, :email, :hash, (select id from plans where is_default = true limit 1))")
                .param("name", name)
                .param("email", email)
                .param("hash", passwordHash)
                .update(keys, "id");
        return keys.getKey().longValue();
    }

    public Optional<Plan> findPlan(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return jdbc.sql("select * from plans where id = :id").param("id", id).query(PLAN).optional();
    }

    public Optional<Plan> findDefaultPlan() {
        return jdbc.sql("select * from plans where is_default = true limit 1").query(PLAN).optional();
    }
}
