package com.gridtactics.server.sim;

import com.gridtactics.server.model.Circle;
import com.gridtactics.server.model.Removal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Moves every circle one step of {@link FieldRules#STEP} along its line and resolves contacts
 * inside that step. Overlap tests only look at a square and the squares that share a side or a corner with it.
 * The list must be mutable. Circles that collide or reach their destination are removed from it.
 */
public final class MotionStep {

    private static final double EPS = 1e-9;

    private MotionStep() {
    }

    public static List<Removal> advance(List<Circle> circles) {
        List<Removal> removed = new ArrayList<>();
        if (circles.isEmpty()) {
            return removed;
        }

        double remaining = 1;
        for (int guard = 0; guard < 64 && remaining > EPS && !circles.isEmpty(); guard++) {
            double hit = earliestCollision(circles);
            double leave = earliestArrival(circles);
            double event = Math.min(hit, leave);
            if (event > remaining) {
                translate(circles, remaining);
                break;
            }

            Set<String> arriving = new HashSet<>();
            if (leave <= event + 1e-7) {
                for (Circle circle : circles) {
                    if (arrivalTime(circle) <= event + 1e-7) {
                        arriving.add(circle.getId());
                    }
                }
            }

            double step = Math.max(0, event);
            translate(circles, step);
            remaining -= step;
            int before = circles.size();
            if (hit <= event + 1e-7) {
                removeCollisions(circles, removed);
            }
            removeIds(circles, arriving, removed, Removal.EXIT);
            if (circles.size() == before) {
                if (step <= EPS) {
                    translate(circles, remaining);
                }
                break;
            }
        }
        removeCollisions(circles, removed);
        removeArrivals(circles, removed);
        return removed;
    }

    private static void translate(List<Circle> circles, double dt) {
        if (dt <= EPS) {
            return;
        }
        for (Circle circle : circles) {
            double nextX = circle.getX() + circle.getVx() * dt;
            double nextY = circle.getY() + circle.getVy() * dt;
            if (reached(circle, nextX, nextY)) {
                circle.setX(circle.getDestX());
                circle.setY(circle.getDestY());
            } else {
                circle.setX(nextX);
                circle.setY(nextY);
            }
        }
    }

    private static boolean reached(Circle circle, double nextX, double nextY) {
        double dx = circle.getDestX() - circle.getX();
        double dy = circle.getDestY() - circle.getY();
        double along = (nextX - circle.getX()) * dx + (nextY - circle.getY()) * dy;
        return along >= dx * dx + dy * dy - 1e-12;
    }

    /**
     * Soonest fraction of a step at which two circles in neighboring squares touch.
     * A pair whose squares do not touch is skipped.
     */
    private static double earliestCollision(List<Circle> circles) {
        Map<Long, List<Circle>> grid = grid(circles);
        double best = Double.POSITIVE_INFINITY;
        for (Circle circle : circles) {
            int cx = cell(circle.getX());
            int cy = cell(circle.getY());
            for (int ox = -1; ox <= 1; ox++) {
                for (int oy = -1; oy <= 1; oy++) {
                    List<Circle> bucket = grid.get(key(cx + ox, cy + oy));
                    if (bucket == null) {
                        continue;
                    }
                    for (Circle other : bucket) {
                        if (circle.getId().compareTo(other.getId()) >= 0) {
                            continue;
                        }
                        best = Math.min(best, collisionTime(circle, other));
                    }
                }
            }
        }
        return best;
    }

    /**
     * Soonest {@code t >= 0} at which the two circles touch, in fractions of one step.
     * Overlap is {@code 0}. Separating or parallel misses are infinity.
     */
    static double collisionTime(Circle a, Circle b) {
        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();
        double dvx = b.getVx() - a.getVx();
        double dvy = b.getVy() - a.getVy();
        double radius = a.getRadius() + b.getRadius();
        double c = dx * dx + dy * dy - radius * radius;
        double bb = 2 * (dx * dvx + dy * dvy);
        double aa = dvx * dvx + dvy * dvy;
        if (c <= EPS) {
            return 0;
        }
        if (aa <= EPS) {
            return Double.POSITIVE_INFINITY;
        }
        double disc = bb * bb - 4 * aa * c;
        if (disc < 0) {
            return Double.POSITIVE_INFINITY;
        }
        double t = (-bb - Math.sqrt(disc)) / (2 * aa);
        if (t < -EPS) {
            return Double.POSITIVE_INFINITY;
        }
        return Math.max(0, t);
    }

    private static double earliestArrival(List<Circle> circles) {
        double best = Double.POSITIVE_INFINITY;
        for (Circle circle : circles) {
            best = Math.min(best, arrivalTime(circle));
        }
        return best;
    }

    static double arrivalTime(Circle circle) {
        double dx = circle.getDestX() - circle.getX();
        double dy = circle.getDestY() - circle.getY();
        double distance = Math.hypot(dx, dy);
        if (distance <= EPS) {
            return 0;
        }
        return distance / FieldRules.STEP;
    }

    private static Map<Long, List<Circle>> grid(List<Circle> circles) {
        Map<Long, List<Circle>> grid = new HashMap<>();
        for (Circle circle : circles) {
            long cellKey = key(cell(circle.getX()), cell(circle.getY()));
            grid.computeIfAbsent(cellKey, ignored -> new ArrayList<>()).add(circle);
        }
        return grid;
    }

    static int cell(double position) {
        return (int) Math.floor(position / FieldRules.CELL);
    }

    private static long key(int cx, int cy) {
        return ((long) cx << 32) ^ (cy & 0xffffffffL);
    }

    private static void removeCollisions(List<Circle> circles, List<Removal> removed) {
        Map<Long, List<Circle>> grid = grid(circles);
        Set<String> hit = new HashSet<>();
        for (Circle circle : circles) {
            int cx = cell(circle.getX());
            int cy = cell(circle.getY());
            for (int ox = -1; ox <= 1; ox++) {
                for (int oy = -1; oy <= 1; oy++) {
                    List<Circle> bucket = grid.get(key(cx + ox, cy + oy));
                    if (bucket == null) {
                        continue;
                    }
                    for (Circle other : bucket) {
                        if (circle.getId().compareTo(other.getId()) >= 0) {
                            continue;
                        }
                        if (overlaps(circle, other)) {
                            hit.add(circle.getId());
                            hit.add(other.getId());
                        }
                    }
                }
            }
        }
        if (hit.isEmpty()) {
            return;
        }
        circles.removeIf(circle -> {
            if (!hit.contains(circle.getId())) {
                return false;
            }
            removed.add(new Removal(circle.getId(), circle.getPlayer(), Removal.COLLISION,
                    circle.getX(), circle.getY()));
            return true;
        });
    }

    private static boolean overlaps(Circle a, Circle b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double reach = a.getRadius() + b.getRadius() + 1e-4;
        return dx * dx + dy * dy <= reach * reach;
    }

    private static void removeArrivals(List<Circle> circles, List<Removal> removed) {
        circles.removeIf(circle -> {
            double dx = circle.getDestX() - circle.getX();
            double dy = circle.getDestY() - circle.getY();
            if (dx * dx + dy * dy > 1e-12) {
                return false;
            }
            removed.add(new Removal(circle.getId(), circle.getPlayer(), Removal.EXIT,
                    circle.getX(), circle.getY()));
            return true;
        });
    }

    private static void removeIds(List<Circle> circles, Set<String> ids, List<Removal> removed, String reason) {
        if (ids.isEmpty()) {
            return;
        }
        circles.removeIf(circle -> {
            if (!ids.contains(circle.getId())) {
                return false;
            }
            removed.add(new Removal(circle.getId(), circle.getPlayer(), reason, circle.getX(), circle.getY()));
            return true;
        });
    }
}
