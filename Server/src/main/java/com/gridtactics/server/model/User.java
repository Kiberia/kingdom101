package com.gridtactics.server.model;

import java.time.Instant;

public class User {

    private final String username;
    private final Instant createdAt;
    private final Instant lastLoginAt;
    private final int wins;
    private final int losses;
    private final int elo;

    public User(
            String username,
            Instant createdAt,
            Instant lastLoginAt,
            int wins,
            int losses,
            int elo) {
        this.username = username;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
        this.wins = wins;
        this.losses = losses;
        this.elo = elo;
    }

    public String getUsername() {
        return username;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getElo() {
        return elo;
    }
}
