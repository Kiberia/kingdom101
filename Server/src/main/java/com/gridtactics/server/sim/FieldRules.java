package com.gridtactics.server.sim;

/**
 * Field and step size. Keep these equal to {@code shared/field.json}.
 * Player 1 is at ({@link #MIN_X}, 0). Player 2 is at ({@link #MAX_X}, 0).
 */
public final class FieldRules {

    public static final double MIN_X = 0;
    public static final double MAX_X = 5;
    public static final double MIN_Y = -3;
    public static final double MAX_Y = 3;
    public static final double RADIUS = 0.05;
    /** Distance the center travels on each server step. */
    public static final double STEP = 0.05;
    /** Squares used to limit collision checks to a cell and its neighbors. */
    public static final double CELL = 0.5;
    public static final int UNITS_PER_PLAYER = 4;
    public static final long STEP_MS = 100;
    /** Safety stop so a stuck field cannot tick forever. */
    public static final int MAX_STEPS = 400;

    private FieldRules() {
    }
}
