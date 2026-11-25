package com.gridtactics.server.controller;

import com.gridtactics.server.model.LeaderboardResponse;
import com.gridtactics.server.model.User;
import com.gridtactics.server.service.UserService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users/exists")
    public Map<String, Boolean> exists(@RequestParam String username) {
        return Map.of("exists", userService.exists(username));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            return userService.login(request.username(), request.hash())
                    .<ResponseEntity<?>>map(user -> ResponseEntity.ok(toResponse(user)))
                    .orElseGet(() -> {
                        if (userService.exists(request.username())) {
                            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                    .body(Map.of("error", "Invalid credentials"));
                        }
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", "User not found"));
                    });
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        try {
            return userService.register(request.username(), request.hash())
                    .<ResponseEntity<?>>map(user -> ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user)))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("error", "Username already exists")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/users/stats")
    public ResponseEntity<?> stats(@RequestParam String username) {
        try {
            return userService.getStats(username)
                    .<ResponseEntity<?>>map(user -> ResponseEntity.ok(toResponse(user)))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "User not found")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/leaderboard")
    public LeaderboardResponse leaderboard(@RequestParam(required = false) String username) {
        return userService.getLeaderboard(username);
    }

    private Map<String, Object> toResponse(User user) {
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("username", user.getUsername());
        response.put("createdAt", user.getCreatedAt().toString());
        response.put("lastLoginAt", user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null);
        response.put("wins", user.getWins());
        response.put("losses", user.getLosses());
        response.put("elo", user.getElo());
        return response;
    }

    public record AuthRequest(String username, String hash) {}
}
