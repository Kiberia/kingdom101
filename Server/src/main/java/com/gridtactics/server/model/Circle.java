package com.gridtactics.server.model;

import com.gridtactics.server.sim.FieldRules;

/**
 * A unit. Velocity is the displacement of one step and does not change, so the path is a straight line
 * from the spawn point to {@code (destX, destY)}.
 */
public final class Circle {

    private final String id;
    private final int player;
    private final double radius;
    private double x;
    private double y;
    private final double vx;
    private final double vy;
    private final double destX;
    private final double destY;

    public Circle(String id, int player, double radius, double x, double y,
                  double vx, double vy, double destX, double destY) {
        this.id = id;
        this.player = player;
        this.radius = radius;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.destX = destX;
        this.destY = destY;
    }

    /** Circle of the standard radius, moving one step at a time from (x, y) toward (destX, destY). */
    public static Circle between(String id, int player, double x, double y, double destX, double destY) {
        double dx = destX - x;
        double dy = destY - y;
        double distance = Math.hypot(dx, dy);
        double vx = 0;
        double vy = 0;
        if (distance > 1e-12) {
            vx = dx / distance * FieldRules.STEP;
            vy = dy / distance * FieldRules.STEP;
        }
        return new Circle(id, player, FieldRules.RADIUS, x, y, vx, vy, destX, destY);
    }

    public Circle copy() {
        return new Circle(id, player, radius, x, y, vx, vy, destX, destY);
    }

    public String getId() {
        return id;
    }

    public int getPlayer() {
        return player;
    }

    public double getRadius() {
        return radius;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getVx() {
        return vx;
    }

    public double getVy() {
        return vy;
    }

    public double getDestX() {
        return destX;
    }

    public double getDestY() {
        return destY;
    }
}
