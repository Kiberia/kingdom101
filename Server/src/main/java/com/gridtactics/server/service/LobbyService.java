package com.gridtactics.server.service;

import com.gridtactics.server.model.PlayerInfo;
import com.gridtactics.server.model.RoomState;
import com.gridtactics.server.sim.FieldRules;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import one.util.streamex.StreamEx;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * In-memory matchmaking.
 * Pushes room-assign to /topic/room-assign/{playerId} then hands off to RoomService.
 *
 * Waiters must send /app/queue/ping every {@link #PING_INTERVAL_MS} ms.
 * A player with a missed heartbeat is not eligible for matching, but stays in the
 * queue until {@link #MAX_MISSED_PINGS} intervals pass, then is removed.
 *
 * When fewer than {@link #ELO_IGNORE_BELOW} eligible players are waiting, Elo is
 * ignored (FIFO). Otherwise pairing uses an Elo window that widens with wait time.
 *
 * room-assign payload:
 * { roomId, players: [{playerId, name, playerNumber}], minX, maxX, minY, maxY, cell, streamKey, vsBot }
 */
@Service
public class LobbyService {

    static final long PING_INTERVAL_MS = 5_000L;
    static final int MAX_MISSED_PINGS = 3;
    static final long STALE_AFTER_MS = PING_INTERVAL_MS * MAX_MISSED_PINGS;

    static final int ELO_IGNORE_BELOW = 10;
    static final int BASE_ELO_WINDOW = 50;
    static final int ELO_WINDOW_GROWTH_PER_SEC = 5;
    static final int MAX_ELO_WINDOW = 2000;

    private final SimpMessagingTemplate messaging;
    private final RoomService roomService;
    private final UserService userService;

    private final Queue<PlayerEntry> waitingQueue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public LobbyService(SimpMessagingTemplate messaging,
                        RoomService roomService,
                        UserService userService) {
        this.messaging = messaging;
        this.roomService = roomService;
        this.userService = userService;
        scheduler.scheduleAtFixedRate(
                this::pruneStaleEntries,
                PING_INTERVAL_MS,
                PING_INTERVAL_MS,
                TimeUnit.MILLISECONDS);
    }

    public synchronized void joinQueue(String playerId, String name) {
        boolean alreadyWaiting = StreamEx.of(waitingQueue)
                .anyMatch(p -> p.playerId.equals(playerId));
        if (alreadyWaiting) {
            return;
        }

        waitingQueue.add(new PlayerEntry(playerId, name, resolveElo(name)));
        tryMatch();
    }

    public synchronized void queuePing(String playerId) {
        PlayerEntry entry = StreamEx.of(waitingQueue)
                .filter(p -> p.playerId.equals(playerId))
                .findFirst()
                .orElse(null);
        if (entry == null) {
            return;
        }
        entry.touch();
        tryMatch();
    }

    public synchronized void joinVsBot(String playerId, String name) {
        waitingQueue.removeIf(p -> p.playerId.equals(playerId));
        PlayerEntry human = new PlayerEntry(playerId, name, resolveElo(name));
        PlayerEntry bot = new PlayerEntry(
                "bot_" + UUID.randomUUID().toString().substring(0, 8),
                "CPU",
                EloRating.DEFAULT_ELO);
        createRoom(human, bot, true);
    }

    public synchronized void pruneStaleEntries() {
        pruneStaleEntries(System.currentTimeMillis());
    }

    synchronized void pruneStaleEntries(long nowMs) {
        waitingQueue.removeIf(p -> p.isStale(nowMs, STALE_AFTER_MS));
    }

    synchronized int waitingCount() {
        return waitingCount(System.currentTimeMillis());
    }

    synchronized int waitingCount(long nowMs) {
        pruneStaleEntries(nowMs);
        return waitingQueue.size();
    }

    synchronized void setLastPingAtMs(String playerId, long lastPingAtMs) {
        StreamEx.of(waitingQueue)
                .filter(p -> p.playerId.equals(playerId))
                .findFirst()
                .ifPresent(p -> p.setLastPingAtMs(lastPingAtMs));
    }

    synchronized void setJoinedAtMs(String playerId, long joinedAtMs) {
        StreamEx.of(waitingQueue)
                .filter(p -> p.playerId.equals(playerId))
                .findFirst()
                .ifPresent(p -> p.setJoinedAtMs(joinedAtMs));
    }

    synchronized void setElo(String playerId, int elo) {
        StreamEx.of(waitingQueue)
                .filter(p -> p.playerId.equals(playerId))
                .findFirst()
                .ifPresent(p -> p.setElo(elo));
    }

    synchronized void tryMatch(long nowMs) {
        pruneStaleEntries(nowMs);

        List<PlayerEntry> eligible = new ArrayList<>();
        for (PlayerEntry entry : waitingQueue) {
            if (entry.hasMissedHeartbeat(nowMs, PING_INTERVAL_MS)) {
                continue;
            }
            eligible.add(entry);
        }
        if (eligible.size() < 2) {
            return;
        }

        PlayerEntry e1;
        PlayerEntry e2;
        if (eligible.size() < ELO_IGNORE_BELOW) {
            e1 = eligible.get(0);
            e2 = eligible.get(1);
        } else {
            PlayerEntry[] pair = findEloPair(eligible, nowMs);
            if (pair == null) {
                return;
            }
            e1 = pair[0];
            e2 = pair[1];
        }

        waitingQueue.remove(e1);
        waitingQueue.remove(e2);
        createRoom(e1, e2, false);
    }

    private void tryMatch() {
        tryMatch(System.currentTimeMillis());
    }

    private PlayerEntry[] findEloPair(List<PlayerEntry> eligible, long nowMs) {
        List<PlayerEntry> byWait = new ArrayList<>(eligible);
        byWait.sort((a, b) -> Long.compare(a.joinedAtMs, b.joinedAtMs));

        for (PlayerEntry a : byWait) {
            int windowA = eloWindow(a, nowMs);
            PlayerEntry best = null;
            int bestDiff = Integer.MAX_VALUE;
            for (PlayerEntry b : byWait) {
                if (a == b) {
                    continue;
                }
                int window = Math.max(windowA, eloWindow(b, nowMs));
                int diff = Math.abs(a.elo - b.elo);
                if (diff > window) {
                    continue;
                }
                if (best == null
                        || diff < bestDiff
                        || (diff == bestDiff && b.joinedAtMs < best.joinedAtMs)) {
                    best = b;
                    bestDiff = diff;
                }
            }
            if (best != null) {
                return new PlayerEntry[] {a, best};
            }
        }
        return null;
    }

    static int eloWindow(PlayerEntry entry, long nowMs) {
        long waitedMs = Math.max(0L, nowMs - entry.joinedAtMs);
        long waitedSec = waitedMs / 1000L;
        long grown = BASE_ELO_WINDOW + waitedSec * ELO_WINDOW_GROWTH_PER_SEC;
        return (int) Math.min(MAX_ELO_WINDOW, grown);
    }

    private int resolveElo(String name) {
        if (userService == null) {
            return EloRating.DEFAULT_ELO;
        }
        return userService.eloForUsername(name);
    }

    private void createRoom(PlayerEntry e1, PlayerEntry e2, boolean vsBot) {
        String roomId = "room_" + UUID.randomUUID().toString().substring(0, 8);

        PlayerInfo p1 = new PlayerInfo(e1.playerId, e1.name, 1);
        PlayerInfo p2 = new PlayerInfo(e2.playerId, e2.name, 2);
        List<PlayerInfo> players = List.of(p1, p2);

        RoomState room = roomService.createRoom(roomId, players);
        if (vsBot) {
            room.setVsBot(true);
            room.setBotPlayerNumber(2);
        }

        room.setPlayer1SecretKey(UUID.randomUUID().toString());
        room.setPlayer2SecretKey(UUID.randomUUID().toString());

        List<Map<String, Object>> playerPayload = List.of(
            Map.of("playerId", p1.getPlayerId(), "name", p1.getName(), "playerNumber", p1.getPlayerNumber()),
            Map.of("playerId", p2.getPlayerId(), "name", p2.getName(), "playerNumber", p2.getPlayerNumber())
        );

        Map<String, Object> p1Msg = new HashMap<>();
        p1Msg.put("roomId", roomId);
        p1Msg.put("players", playerPayload);
        p1Msg.put("minX", FieldRules.MIN_X);
        p1Msg.put("maxX", FieldRules.MAX_X);
        p1Msg.put("minY", FieldRules.MIN_Y);
        p1Msg.put("maxY", FieldRules.MAX_Y);
        p1Msg.put("cell", FieldRules.CELL);
        p1Msg.put("streamKey", room.getPlayer1SecretKey());
        p1Msg.put("vsBot", vsBot);
        messaging.convertAndSend("/topic/room-assign/" + p1.getPlayerId(), p1Msg);

        if (!vsBot) {
            Map<String, Object> p2Msg = new HashMap<>();
            p2Msg.put("roomId", roomId);
            p2Msg.put("players", playerPayload);
            p2Msg.put("minX", FieldRules.MIN_X);
            p2Msg.put("maxX", FieldRules.MAX_X);
            p2Msg.put("minY", FieldRules.MIN_Y);
            p2Msg.put("maxY", FieldRules.MAX_Y);
            p2Msg.put("cell", FieldRules.CELL);
            p2Msg.put("streamKey", room.getPlayer2SecretKey());
            p2Msg.put("vsBot", false);
            messaging.convertAndSend("/topic/room-assign/" + p2.getPlayerId(), p2Msg);
        }

        roomService.startRoom(room);
    }

    static final class PlayerEntry {
        final String playerId;
        final String name;
        private int elo;
        private long joinedAtMs;
        private long lastPingAtMs;

        PlayerEntry(String playerId, String name, int elo) {
            this.playerId = playerId;
            this.name = name;
            this.elo = elo;
            long now = System.currentTimeMillis();
            this.joinedAtMs = now;
            this.lastPingAtMs = now;
        }

        void touch() {
            this.lastPingAtMs = System.currentTimeMillis();
        }

        void setLastPingAtMs(long lastPingAtMs) {
            this.lastPingAtMs = lastPingAtMs;
        }

        void setJoinedAtMs(long joinedAtMs) {
            this.joinedAtMs = joinedAtMs;
        }

        void setElo(int elo) {
            this.elo = elo;
        }

        boolean hasMissedHeartbeat(long nowMs, long intervalMs) {
            return nowMs - lastPingAtMs > intervalMs;
        }

        boolean isStale(long nowMs, long ttlMs) {
            return nowMs - lastPingAtMs > ttlMs;
        }
    }
}
