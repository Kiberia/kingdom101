package com.gridtactics.server.controller;

import com.gridtactics.server.service.LobbyService;
import java.util.Map;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * Handles STOMP messages from the client.
 *
 * Client sends:  stompClient.send('/app/join', {}, JSON.stringify({playerId, name, vsBot?}))
 * Client sends:  stompClient.send('/app/queue/ping', {}, JSON.stringify({playerId}))
 * Server pushes: /topic/room-assign/{playerId}
 */
@Controller
public class LobbyController {

    private final LobbyService lobbyService;

    public LobbyController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    @MessageMapping("/join")
    public void join(Map<String, Object> payload) {
        String playerId = String.valueOf(payload.get("playerId"));
        String name = String.valueOf(payload.get("name"));
        Object vsBotRaw = payload.get("vsBot");
        boolean vsBot = Boolean.TRUE.equals(vsBotRaw) || "true".equals(String.valueOf(vsBotRaw));
        if (vsBot) {
            lobbyService.joinVsBot(playerId, name);
        } else {
            lobbyService.joinQueue(playerId, name);
        }
    }

    @MessageMapping("/queue/ping")
    public void queuePing(Map<String, Object> payload) {
        String playerId = String.valueOf(payload.get("playerId"));
        lobbyService.queuePing(playerId);
    }
}
