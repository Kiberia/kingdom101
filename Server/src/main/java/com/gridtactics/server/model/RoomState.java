package com.gridtactics.server.model;

import java.util.ArrayList;
import java.util.List;

public class RoomState {

    private final String roomId;
    private final List<PlayerInfo> players;
    private final List<Circle> units = new ArrayList<>();
    private List<Removal> removed = List.of();

    private GamePhase phase = GamePhase.BATTLE;
    private int step;
    private String winner;
    private boolean statsRecorded;

    private String player1SecretKey;
    private String player2SecretKey;
    private boolean vsBot;
    private int botPlayerNumber;

    public RoomState(String roomId, List<PlayerInfo> players) {
        this.roomId = roomId;
        this.players = players;
    }

    public String getRoomId() {
        return roomId;
    }

    public List<PlayerInfo> getPlayers() {
        return players;
    }

    public List<Circle> getUnits() {
        return units;
    }

    public List<Removal> getRemoved() {
        return removed;
    }

    public void setRemoved(List<Removal> removed) {
        this.removed = removed;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public boolean isStatsRecorded() {
        return statsRecorded;
    }

    public void setStatsRecorded(boolean statsRecorded) {
        this.statsRecorded = statsRecorded;
    }

    public String getPlayer1SecretKey() {
        return player1SecretKey;
    }

    public void setPlayer1SecretKey(String player1SecretKey) {
        this.player1SecretKey = player1SecretKey;
    }

    public String getPlayer2SecretKey() {
        return player2SecretKey;
    }

    public void setPlayer2SecretKey(String player2SecretKey) {
        this.player2SecretKey = player2SecretKey;
    }

    public boolean isVsBot() {
        return vsBot;
    }

    public void setVsBot(boolean vsBot) {
        this.vsBot = vsBot;
    }

    public int getBotPlayerNumber() {
        return botPlayerNumber;
    }

    public void setBotPlayerNumber(int botPlayerNumber) {
        this.botPlayerNumber = botPlayerNumber;
    }

    public PlayerInfo getPlayerByNumber(int number) {
        for (PlayerInfo player : players) {
            if (player.getPlayerNumber() == number) {
                return player;
            }
        }
        throw new IllegalArgumentException("No player " + number + " in " + roomId);
    }
}
