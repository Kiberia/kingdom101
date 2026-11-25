package com.gridtactics.server.service;

import com.gridtactics.server.model.GamePhase;
import com.gridtactics.server.model.PlayerInfo;
import com.gridtactics.server.model.Removal;
import com.gridtactics.server.model.RoomMessage;
import com.gridtactics.server.model.RoomState;
import com.gridtactics.server.model.User;
import com.gridtactics.server.sim.CircleSpawner;
import com.gridtactics.server.sim.FieldRules;
import com.gridtactics.server.sim.MatchEnd;
import com.gridtactics.server.sim.MotionStep;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

    private final SimpMessagingTemplate messaging;
    private final UserService userService;

    private final Map<String, RoomState> rooms = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> tickTimers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public RoomService(SimpMessagingTemplate messaging, UserService userService) {
        this.messaging = messaging;
        this.userService = userService;
    }

    public RoomState createRoom(String roomId, List<PlayerInfo> players) {
        RoomState room = new RoomState(roomId, players);
        rooms.put(roomId, room);
        return room;
    }

    public void startRoom(RoomState room) {
        CircleSpawner.spawn(room.getRoomId(), room.getUnits());
        room.setPhase(GamePhase.BATTLE);
        room.setStep(0);
        room.setRemoved(List.of());
        pushState(room);

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                () -> tick(room.getRoomId()),
                FieldRules.STEP_MS,
                FieldRules.STEP_MS,
                TimeUnit.MILLISECONDS);
        tickTimers.put(room.getRoomId(), future);
    }

    private synchronized void tick(String roomId) {
        RoomState room = rooms.get(roomId);
        if (room == null || room.getPhase() != GamePhase.BATTLE) {
            return;
        }

        List<Removal> removed = MotionStep.advance(room.getUnits());
        room.setStep(room.getStep() + 1);
        room.setRemoved(removed);

        String winner = MatchEnd.winner(room.getUnits());
        if (winner == null && room.getStep() >= FieldRules.MAX_STEPS) {
            winner = "draw";
        }
        if (winner != null) {
            room.setWinner(winner);
            room.setPhase(GamePhase.GAMEOVER);
            cancelTick(roomId);
        }
        pushState(room);
    }

    private void cancelTick(String roomId) {
        Optional.ofNullable(tickTimers.remove(roomId)).ifPresent(future -> future.cancel(false));
    }

    private void pushState(RoomState room) {
        if (room.getPhase() == GamePhase.GAMEOVER) {
            userService.recordMatchResult(room);
        }
        String base = "/topic/room/" + room.getRoomId() + "/";
        messaging.convertAndSend(base + room.getPlayer1SecretKey(),
                RoomMessage.state(room, 1, statsForPlayer(room, 1)));
        messaging.convertAndSend(base + room.getPlayer2SecretKey(),
                RoomMessage.state(room, 2, statsForPlayer(room, 2)));
    }

    private User statsForPlayer(RoomState room, int playerNumber) {
        if (room.getPhase() != GamePhase.GAMEOVER) {
            return null;
        }
        String username = room.getPlayerByNumber(playerNumber).getName();
        return userService.getStats(username).orElse(null);
    }
}
