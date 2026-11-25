package com.gridtactics.server.model;

public record Removal(String id, int player, String reason, double x, double y) {

    public static final String COLLISION = "collision";
    public static final String EXIT = "exit";
}
