package com.gridtactics.server.repository;

import com.gridtactics.server.model.LeaderboardEntry;
import com.gridtactics.server.model.User;
import com.gridtactics.server.service.EloRating;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean existsByUsernameIgnoreCase(String username) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE LOWER(username) = LOWER(?)",
                Integer.class,
                username);
        return count != null && count > 0;
    }

    public Optional<StoredUser> findByUsernameIgnoreCase(String username) {
        return jdbc.query(
                """
                SELECT username, password_hash, created_at, last_login_at, wins, losses, elo
                FROM users
                WHERE LOWER(username) = LOWER(?)
                """,
                rs -> rs.next()
                        ? Optional.of(new StoredUser(
                                rs.getString("username"),
                                rs.getString("password_hash"),
                                rs.getTimestamp("created_at").toInstant(),
                                rs.getTimestamp("last_login_at") != null
                                        ? rs.getTimestamp("last_login_at").toInstant()
                                        : null,
                                rs.getInt("wins"),
                                rs.getInt("losses"),
                                rs.getInt("elo")))
                        : Optional.empty(),
                username);
    }

    public User insert(String username, String passwordHash) {
        Instant now = Instant.now();
        jdbc.update(
                """
                INSERT INTO users (username, password_hash, created_at, last_login_at, wins, losses, elo)
                VALUES (?, ?, ?, ?, 0, 0, ?)
                """,
                username,
                passwordHash,
                Timestamp.from(now),
                Timestamp.from(now),
                EloRating.DEFAULT_ELO);
        return new User(username, now, now, 0, 0, EloRating.DEFAULT_ELO);
    }

    public User updateLastLogin(String username) {
        Instant now = Instant.now();
        jdbc.update(
                "UPDATE users SET last_login_at = ? WHERE LOWER(username) = LOWER(?)",
                Timestamp.from(now),
                username);
        return findByUsernameIgnoreCase(username)
                .map(this::toUser)
                .orElseThrow();
    }

    public void applyWin(String username, int newElo) {
        jdbc.update(
                "UPDATE users SET wins = wins + 1, elo = ? WHERE LOWER(username) = LOWER(?)",
                newElo,
                username);
    }

    public void applyLoss(String username, int newElo) {
        jdbc.update(
                "UPDATE users SET losses = losses + 1, elo = ? WHERE LOWER(username) = LOWER(?)",
                newElo,
                username);
    }

    public List<LeaderboardEntry> topByElo(int limit) {
        return jdbc.query(
                """
                SELECT username, elo, wins, losses
                FROM users
                ORDER BY elo DESC, wins DESC, LOWER(username) ASC
                LIMIT ?
                """,
                (rs, rowNum) -> new LeaderboardEntry(
                        rowNum + 1,
                        rs.getString("username"),
                        rs.getInt("elo"),
                        rs.getInt("wins"),
                        rs.getInt("losses")),
                limit);
    }

    public Optional<LeaderboardEntry> rankForUsername(String username) {
        Optional<StoredUser> me = findByUsernameIgnoreCase(username);
        if (me.isEmpty()) {
            return Optional.empty();
        }
        StoredUser stored = me.get();
        Integer higher = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM users u
                WHERE u.elo > ?
                   OR (u.elo = ? AND u.wins > ?)
                   OR (u.elo = ? AND u.wins = ? AND LOWER(u.username) < LOWER(?))
                """,
                Integer.class,
                stored.elo(),
                stored.elo(),
                stored.wins(),
                stored.elo(),
                stored.wins(),
                stored.username());
        int rank = (higher != null ? higher : 0) + 1;
        return Optional.of(new LeaderboardEntry(
                rank,
                stored.username(),
                stored.elo(),
                stored.wins(),
                stored.losses()));
    }

    private User toUser(StoredUser stored) {
        return new User(
                stored.username(),
                stored.createdAt(),
                stored.lastLoginAt(),
                stored.wins(),
                stored.losses(),
                stored.elo());
    }

    public record StoredUser(
            String username,
            String passwordHash,
            Instant createdAt,
            Instant lastLoginAt,
            int wins,
            int losses,
            int elo) {}
}
