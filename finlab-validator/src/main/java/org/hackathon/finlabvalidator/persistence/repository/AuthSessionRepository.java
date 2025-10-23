package org.hackathon.finlabvalidator.persistence.repository;

import org.hackathon.finlabvalidator.persistence.domain.AuthSession;
import org.hackathon.finlabvalidator.persistence.domain.SessionStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class AuthSessionRepository implements IAuthSessionRepository {

    private final JdbcTemplate jdbcTemplate;
    private final String schemaName;

    public AuthSessionRepository(JdbcTemplate jdbcTemplate,
                                  @Value("${spring.datasource.schema}") String schemaName) {
        this.jdbcTemplate = jdbcTemplate;
        this.schemaName = schemaName;
    }

    public AuthSession save(AuthSession session) {
        if (session.getId() == null) {
            return insert(session);
        } else {
            return update(session);
        }
    }

    private AuthSession insert(AuthSession session) {
        String sql = String.format("INSERT INTO %s.auth_sessions (token_hash, username, login_time, logout_time, status, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)", schemaName);

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, session.getTokenHash());
            ps.setString(2, session.getUsername());
            ps.setTimestamp(3, Timestamp.valueOf(session.getLoginTime()));
            ps.setTimestamp(4, session.getLogoutTime() != null ? Timestamp.valueOf(session.getLogoutTime()) : null);
            ps.setString(5, session.getStatus().name());
            ps.setTimestamp(6, Timestamp.valueOf(session.getCreatedAt()));
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            session.setId(keyHolder.getKey().longValue());
        }
        return session;
    }

    private AuthSession update(AuthSession session) {
        String sql = String.format("UPDATE %s.auth_sessions SET logout_time = ?, status = ? " +
                     "WHERE id = ?", schemaName);

        jdbcTemplate.update(sql,
                session.getLogoutTime() != null ? Timestamp.valueOf(session.getLogoutTime()) : null,
                session.getStatus().name(),
                session.getId());

        return session;
    }

    public Optional<AuthSession> findByTokenHash(String tokenHash) {
        String sql = String.format("SELECT id, token_hash, username, login_time, logout_time, status, created_at " +
                     "FROM %s.auth_sessions WHERE token_hash = ?", schemaName);

        List<AuthSession> results = jdbcTemplate.query(sql, (rs, rowNum) ->
                new AuthSession(
                        rs.getLong("id"),
                        rs.getString("token_hash"),
                        rs.getString("username"),
                        rs.getTimestamp("login_time").toLocalDateTime(),
                        rs.getTimestamp("logout_time") != null ? rs.getTimestamp("logout_time").toLocalDateTime() : null,
                        SessionStatus.valueOf(rs.getString("status")),
                        rs.getTimestamp("created_at").toLocalDateTime()
                )
        , tokenHash);

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public void deleteAll() {
        jdbcTemplate.update(String.format("DELETE FROM %s.auth_sessions", schemaName));
    }

    public long count() {
        return jdbcTemplate.queryForObject(
                String.format("SELECT COUNT(*) FROM %s.auth_sessions", schemaName),
                Long.class);
    }

    public List<AuthSession> findAll() {
        String sql = String.format("SELECT id, token_hash, username, login_time, logout_time, status, created_at " +
                     "FROM %s.auth_sessions", schemaName);

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new AuthSession(
                        rs.getLong("id"),
                        rs.getString("token_hash"),
                        rs.getString("username"),
                        rs.getTimestamp("login_time").toLocalDateTime(),
                        rs.getTimestamp("logout_time") != null ? rs.getTimestamp("logout_time").toLocalDateTime() : null,
                        SessionStatus.valueOf(rs.getString("status")),
                        rs.getTimestamp("created_at").toLocalDateTime()
                )
        );
    }
}
