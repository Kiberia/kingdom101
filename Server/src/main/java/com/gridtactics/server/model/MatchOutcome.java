package com.gridtactics.server.model;

public final class MatchOutcome {

    private MatchOutcome() {}

    public static int winnerPlayerNumber(String winner) {
        if (winner == null || winner.isBlank()) {
            return -1;
        }
        String trimmed = winner.trim();
        if (trimmed.matches("\\d+")) {
            return Integer.parseInt(trimmed);
        }
        if (trimmed.startsWith("Player ")) {
            return Integer.parseInt(trimmed.substring(7).trim());
        }
        return -1;
    }
}
