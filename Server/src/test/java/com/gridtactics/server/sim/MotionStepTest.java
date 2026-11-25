package com.gridtactics.server.sim;

import com.gridtactics.server.model.Circle;
import com.gridtactics.server.model.Removal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class MotionStepTest {

    public static void main(String[] args) throws Exception {
        straightLine();
        reachesDestination();
        headOnBothDisappear();
        neighborCellsStillCollide();
        parallelMissExits();
        laterCircleSurvives();
        sameSideBothDisappear();
        fieldFileMatchesRules();
        openingsFinish();
        System.out.println("ok");
    }

    private static void straightLine() {
        List<Circle> circles = new ArrayList<>();
        circles.add(Circle.between("a", 1, 0, 0, 5, 0));
        MotionStep.advance(circles);
        Circle circle = circles.get(0);
        near(circle.getX(), 0.05, 1e-9);
        near(circle.getY(), 0, 1e-9);
        near(circle.getVx(), 0.05, 1e-9);
        near(circle.getVy(), 0, 1e-9);
        for (int i = 0; i < 9; i++) {
            MotionStep.advance(circles);
        }
        near(circles.get(0).getX(), 0.5, 1e-6);
        near(circles.get(0).getY(), 0, 1e-6);
    }

    private static void reachesDestination() {
        List<Circle> circles = new ArrayList<>();
        circles.add(Circle.between("a", 1, 0, 1, 5, 1));
        List<Removal> removed = List.of();
        int steps = 0;
        while (!circles.isEmpty() && steps < 120) {
            removed = MotionStep.advance(circles);
            steps++;
        }
        check(circles.isEmpty(), "should arrive");
        check(steps >= 99 && steps <= 101, "steps " + steps);
        check(removed.size() == 1, "one exit");
        check(Removal.EXIT.equals(removed.get(0).reason()), removed.get(0).reason());
        near(removed.get(0).x(), 5, 1e-6);
        near(removed.get(0).y(), 1, 1e-6);
    }

    private static void headOnBothDisappear() {
        List<Circle> circles = new ArrayList<>();
        circles.add(Circle.between("a", 1, 2.0, 0, 5, 0));
        circles.add(Circle.between("b", 2, 2.15, 0, 0, 0));
        List<Removal> removed = MotionStep.advance(circles);
        check(circles.isEmpty(), "both circles should be gone");
        check(removed.size() == 2, "two removals");
        for (Removal removal : removed) {
            check(Removal.COLLISION.equals(removal.reason()), removal.id());
            near(removal.y(), 0, 1e-4);
            if ("a".equals(removal.id())) {
                near(removal.x(), 2.025, 1e-4);
            }
            if ("b".equals(removal.id())) {
                near(removal.x(), 2.125, 1e-4);
            }
        }
    }

    private static void neighborCellsStillCollide() {
        List<Circle> circles = new ArrayList<>();
        circles.add(Circle.between("a", 1, 0.49, 0, 5, 0));
        circles.add(Circle.between("b", 2, 0.55, 0, 0, 0));
        check(MotionStep.cell(0.49) + 1 == MotionStep.cell(0.55), "squares should touch");
        List<Removal> removed = MotionStep.advance(circles);
        check(circles.isEmpty(), "neighbors that overlap must both disappear");
        check(removed.size() == 2, "two collision removals");
    }

    private static void parallelMissExits() {
        List<Circle> circles = new ArrayList<>();
        circles.add(Circle.between("a", 1, 0, 0, 5, 0));
        circles.add(Circle.between("b", 2, 0, 0.4, 5, 0.4));
        List<Removal> removed = new ArrayList<>();
        for (int i = 0; i < FieldRules.MAX_STEPS && !circles.isEmpty(); i++) {
            removed.addAll(MotionStep.advance(circles));
        }
        check(circles.isEmpty(), "both should reach B");
        check(removed.size() == 2, "two exits");
        for (Removal removal : removed) {
            check(Removal.EXIT.equals(removal.reason()), removal.id() + " " + removal.reason());
        }
    }

    private static void laterCircleSurvives() {
        List<Circle> circles = new ArrayList<>();
        circles.add(Circle.between("a", 1, 2.0, 0, 5, 0));
        circles.add(Circle.between("b", 2, 2.15, 0, 0, 0));
        circles.add(Circle.between("c", 2, 3.0, 0, 0, 0));
        check(Math.abs(MotionStep.cell(2.0) - MotionStep.cell(3.0)) > 1, "c starts outside the neighbor squares");
        List<Removal> removed = MotionStep.advance(circles);
        check(circles.size() == 1 && "c".equals(circles.get(0).getId()), "only the later circle remains");
        check(removed.size() == 2, "the head-on pair is removed");
    }

    private static void sameSideBothDisappear() {
        List<Circle> circles = new ArrayList<>();
        circles.add(Circle.between("a", 1, 2.0, 0, 5, 0));
        circles.add(Circle.between("b", 1, 2.15, 0, 0, 0));
        List<Removal> removed = MotionStep.advance(circles);
        check(circles.isEmpty(), "same side still collides");
        check(removed.size() == 2, "both removed");
    }

    private static void fieldFileMatchesRules() throws Exception {
        String json = Files.readString(Path.of("shared", "field.json"));
        contains(json, "\"minX\": 0");
        contains(json, "\"maxX\": 5");
        contains(json, "\"minY\": -3");
        contains(json, "\"maxY\": 3");
        contains(json, "\"radius\": 0.05");
        contains(json, "\"step\": 0.05");
        contains(json, "\"cell\": 0.5");
        contains(json, "\"unitsPerPlayer\": 4");
        contains(json, "\"stepMs\": 100");
        check(FieldRules.MIN_X == 0 && FieldRules.MAX_X == 5, "x");
        check(FieldRules.MIN_Y == -3 && FieldRules.MAX_Y == 3, "y");
        check(FieldRules.RADIUS == 0.05, "radius");
        check(FieldRules.STEP == 0.05, "step");
        check(FieldRules.CELL == 0.5, "cell");
        check(FieldRules.UNITS_PER_PLAYER == 4, "count");
    }

    private static void openingsFinish() {
        int player1 = 0;
        int player2 = 0;
        int draws = 0;
        int collisions = 0;
        for (int n = 0; n < 300; n++) {
            List<Circle> circles = new ArrayList<>();
            CircleSpawner.spawn("room-" + n, circles);
            check(circles.size() == 8, "eight circles");
            assertOpening(circles);
            int removed = 0;
            String winner = null;
            for (int step = 1; step <= FieldRules.MAX_STEPS; step++) {
                List<Removal> gone = MotionStep.advance(circles);
                removed += gone.size();
                for (Removal removal : gone) {
                    if (Removal.COLLISION.equals(removal.reason())) {
                        collisions++;
                    }
                }
                winner = MatchEnd.winner(circles);
                if (winner != null) {
                    break;
                }
            }
            check(winner != null, "match did not finish: room-" + n);
            check(removed + circles.size() == 8, "lost a circle");
            if ("1".equals(winner)) {
                player1++;
            } else if ("2".equals(winner)) {
                player2++;
            } else {
                draws++;
            }
        }
        System.out.println("openings p1=" + player1 + " p2=" + player2 + " draw=" + draws
                + " collisions=" + collisions);
        check(collisions > 0, "expected collisions");
        check(player1 > 0 && player2 > 0, "both sides should be able to win");
    }

    private static void assertOpening(List<Circle> circles) {
        for (Circle circle : circles) {
            check(circle.getRadius() == FieldRules.RADIUS, "radius");
            check(circle.getY() >= FieldRules.MIN_Y && circle.getY() < FieldRules.MAX_Y, "start y");
            check(circle.getDestY() >= FieldRules.MIN_Y && circle.getDestY() < FieldRules.MAX_Y, "dest y");
            double speed = Math.hypot(circle.getVx(), circle.getVy());
            near(speed, FieldRules.STEP, 1e-9);
            if (circle.getPlayer() == 1) {
                near(circle.getX(), FieldRules.MIN_X, 1e-9);
                near(circle.getDestX(), FieldRules.MAX_X, 1e-9);
                check(circle.getVx() > 0, "player 1 faces player 2");
            } else {
                near(circle.getX(), FieldRules.MAX_X, 1e-9);
                near(circle.getDestX(), FieldRules.MIN_X, 1e-9);
                check(circle.getVx() < 0, "player 2 faces player 1");
            }
        }
        for (int i = 0; i < circles.size(); i++) {
            for (int j = i + 1; j < circles.size(); j++) {
                Circle a = circles.get(i);
                Circle b = circles.get(j);
                double dx = a.getX() - b.getX();
                double dy = a.getY() - b.getY();
                double reach = a.getRadius() + b.getRadius();
                check(dx * dx + dy * dy > reach * reach, "spawn overlap");
            }
        }
    }

    private static void near(double actual, double expected, double tolerance) {
        check(Math.abs(actual - expected) < tolerance, actual + " != " + expected);
    }

    private static void contains(String json, String needle) {
        check(json.contains(needle), "missing " + needle);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
