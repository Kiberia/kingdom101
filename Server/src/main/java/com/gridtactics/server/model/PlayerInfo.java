package com.gridtactics.server.model;

public class PlayerInfo {
    private final String playerId;
    private final String name;
    private final int playerNumber; // 1 or 2

    public PlayerInfo(String playerId, String name, int playerNumber) {
        this.playerId = playerId;
        this.name = name;
        this.playerNumber = playerNumber;
    }

    public String getPlayerId() { return playerId; }
    public String getName()     { return name; }
    public int getPlayerNumber(){ return playerNumber; }
}
