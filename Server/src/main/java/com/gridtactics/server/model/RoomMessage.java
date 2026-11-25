package com.gridtactics.server.model;

import com.gridtactics.server.sim.FieldRules;
import java.util.ArrayList;
import java.util.List;

public class RoomMessage {

    private String phase;
    private int step;
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
    private double cell;
    private List<Circle> units;
    private List<Removal> removed;
    private String winner;
    private Integer wins;
    private Integer losses;
    private Integer elo;
    private String matchResult;

    public static RoomMessage state(RoomState room, int playerNumber, User playerStats) {
        RoomMessage message = new RoomMessage();
        message.phase = room.getPhase().name();
        message.step = room.getStep();
        message.minX = FieldRules.MIN_X;
        message.maxX = FieldRules.MAX_X;
        message.minY = FieldRules.MIN_Y;
        message.maxY = FieldRules.MAX_Y;
        message.cell = FieldRules.CELL;
        message.units = new ArrayList<>();
        for (Circle circle : room.getUnits()) {
            message.units.add(circle.copy());
        }
        message.removed = List.copyOf(room.getRemoved());
        message.winner = room.getWinner();

        if (room.getPhase() == GamePhase.GAMEOVER) {
            int winnerNumber = MatchOutcome.winnerPlayerNumber(room.getWinner());
            if ("draw".equals(room.getWinner())) {
                message.matchResult = "DRAW";
            } else if (winnerNumber == playerNumber) {
                message.matchResult = "WIN";
            } else if (winnerNumber > 0) {
                message.matchResult = "LOSS";
            }
            if (playerStats != null) {
                message.wins = playerStats.getWins();
                message.losses = playerStats.getLosses();
                message.elo = playerStats.getElo();
            }
        }
        return message;
    }

    public String getPhase() {
        return phase;
    }

    public int getStep() {
        return step;
    }

    public double getMinX() {
        return minX;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMaxY() {
        return maxY;
    }

    public double getCell() {
        return cell;
    }

    public List<Circle> getUnits() {
        return units;
    }

    public List<Removal> getRemoved() {
        return removed;
    }

    public String getWinner() {
        return winner;
    }

    public Integer getWins() {
        return wins;
    }

    public Integer getLosses() {
        return losses;
    }

    public Integer getElo() {
        return elo;
    }

    public String getMatchResult() {
        return matchResult;
    }
}
