package com.gridtactics.server.sim;

import com.gridtactics.server.model.Circle;
import java.util.List;

public final class MatchEnd {

    private MatchEnd() {
    }

    /**
     * @return {@code "1"}, {@code "2"}, {@code "draw"} when the match is over, otherwise null
     */
    public static String winner(List<Circle> circles) {
        boolean player1 = false;
        boolean player2 = false;
        for (Circle circle : circles) {
            if (circle.getPlayer() == 1) {
                player1 = true;
            } else if (circle.getPlayer() == 2) {
                player2 = true;
            }
        }
        if (player1 && player2) {
            return null;
        }
        if (!player1 && !player2) {
            return "draw";
        }
        return player1 ? "1" : "2";
    }
}
