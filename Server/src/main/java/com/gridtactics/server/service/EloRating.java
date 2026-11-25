package com.gridtactics.server.service;

/**
 * Classic Elo with fixed K. No history — callers persist the new ratings.
 */
public final class EloRating {

    public static final int DEFAULT_ELO = 1000;
    public static final int K = 32;

    private EloRating() {
    }

    public static double expectedScore(int rating, int opponentRating) {
        return 1.0 / (1.0 + Math.pow(10.0, (opponentRating - rating) / 400.0));
    }

    /**
     * @return {@code [newWinnerElo, newLoserElo]}
     */
    public static int[] update(int winnerElo, int loserElo) {
        double winnerExpected = expectedScore(winnerElo, loserElo);
        double loserExpected = expectedScore(loserElo, winnerElo);
        int newWinner = (int) Math.round(winnerElo + K * (1.0 - winnerExpected));
        int newLoser = (int) Math.round(loserElo + K * (0.0 - loserExpected));
        return new int[] {newWinner, newLoser};
    }
}
