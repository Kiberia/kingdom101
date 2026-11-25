package com.gridtactics.server.model;

public record LeaderboardEntry(
        int rank,
        String username,
        int elo,
        int wins,
        int losses) {
}
