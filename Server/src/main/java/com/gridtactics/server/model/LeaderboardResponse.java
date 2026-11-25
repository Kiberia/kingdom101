package com.gridtactics.server.model;

import java.util.List;

public record LeaderboardResponse(
        List<LeaderboardEntry> top,
        LeaderboardEntry me) {
}
