package com.gridtactics.server.sim;

import com.gridtactics.server.model.Circle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Player 1 units start on x = 0 and travel to x = 5.
 * Player 2 units start on x = 5 and travel to x = 0.
 * Both endpoints use a y in [{@link FieldRules#MIN_Y}, {@link FieldRules#MAX_Y}].
 * The room id picks the points, so the same room always opens the same way.
 */
public final class CircleSpawner {

    private CircleSpawner() {
    }

    public static void spawn(String roomId, List<Circle> into) {
        Random random = new Random(roomId.hashCode());
        List<Double> left = new ArrayList<>();
        List<Double> right = new ArrayList<>();
        for (int i = 0; i < FieldRules.UNITS_PER_PLAYER; i++) {
            double fromY = openY(random, left);
            double toY = rangeY(random);
            into.add(Circle.between("p1-" + (i + 1), 1, FieldRules.MIN_X, fromY, FieldRules.MAX_X, toY));

            fromY = openY(random, right);
            toY = rangeY(random);
            into.add(Circle.between("p2-" + (i + 1), 2, FieldRules.MAX_X, fromY, FieldRules.MIN_X, toY));
        }
    }

    private static double rangeY(Random random) {
        return FieldRules.MIN_Y + random.nextDouble() * (FieldRules.MAX_Y - FieldRules.MIN_Y);
    }

    /** A start y that does not overlap a circle already placed on the same rail. */
    private static double openY(Random random, List<Double> taken) {
        double gap = FieldRules.RADIUS * 2 + 1e-3;
        for (int attempt = 0; attempt < 40; attempt++) {
            double y = rangeY(random);
            if (clear(y, taken, gap)) {
                taken.add(y);
                return y;
            }
        }
        double y = FieldRules.MIN_Y
                + (taken.size() + 0.5) * (FieldRules.MAX_Y - FieldRules.MIN_Y) / FieldRules.UNITS_PER_PLAYER;
        taken.add(y);
        return y;
    }

    private static boolean clear(double y, List<Double> taken, double gap) {
        for (double other : taken) {
            if (Math.abs(other - y) < gap) {
                return false;
            }
        }
        return true;
    }
}
