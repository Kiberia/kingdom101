package com.gridtactics.server.service;

import com.gridtactics.server.auth.PasswordHasher;
import com.gridtactics.server.model.GamePhase;
import com.gridtactics.server.model.LeaderboardEntry;
import com.gridtactics.server.model.LeaderboardResponse;
import com.gridtactics.server.model.MatchOutcome;
import com.gridtactics.server.model.PlayerInfo;
import com.gridtactics.server.model.RoomState;
import com.gridtactics.server.model.User;
import com.gridtactics.server.repository.UserRepository;
import com.gridtactics.server.repository.UserRepository.StoredUser;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final int LEADERBOARD_TOP = 10;

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean exists(String username) {
        String normalized = PasswordHasher.normalizeUsername(username);
        return userRepository.existsByUsernameIgnoreCase(normalized);
    }

    public Optional<User> login(String username, String incomingHash) {
        validateHash(incomingHash);
        String normalized = PasswordHasher.normalizeUsername(username);
        Optional<StoredUser> stored = userRepository.findByUsernameIgnoreCase(normalized);
        if (stored.isEmpty()) {
            return Optional.empty();
        }
        String expected = PasswordHasher.hash(normalized, incomingHash);
        if (!expected.equals(stored.get().passwordHash())) {
            return Optional.empty();
        }
        return Optional.of(userRepository.updateLastLogin(stored.get().username()));
    }

    public Optional<User> register(String username, String incomingHash) {
        validateHash(incomingHash);
        String normalized = PasswordHasher.normalizeUsername(username);
        if (userRepository.existsByUsernameIgnoreCase(normalized)) {
            return Optional.empty();
        }
        String passwordHash = PasswordHasher.hash(normalized, incomingHash);
        return Optional.of(userRepository.insert(normalized, passwordHash));
    }

    public Optional<User> getStats(String username) {
        try {
            String normalized = PasswordHasher.normalizeUsername(username);
            return userRepository.findByUsernameIgnoreCase(normalized)
                    .map(stored -> new User(
                            stored.username(),
                            stored.createdAt(),
                            stored.lastLoginAt(),
                            stored.wins(),
                            stored.losses(),
                            stored.elo()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public int eloForUsername(String username) {
        return getStats(username)
                .map(User::getElo)
                .orElse(EloRating.DEFAULT_ELO);
    }

    public LeaderboardResponse getLeaderboard(String username) {
        List<LeaderboardEntry> top = userRepository.topByElo(LEADERBOARD_TOP);
        LeaderboardEntry me = null;
        if (username != null && !username.isBlank()) {
            try {
                String normalized = PasswordHasher.normalizeUsername(username);
                me = userRepository.rankForUsername(normalized).orElse(null);
            } catch (IllegalArgumentException ignored) {
                me = null;
            }
        }
        return new LeaderboardResponse(top, me);
    }

    public void recordMatchResult(RoomState room) {
        if (room.getPhase() != GamePhase.GAMEOVER || room.isStatsRecorded()) {
            return;
        }
        if (room.isVsBot()) {
            room.setStatsRecorded(true);
            return;
        }

        int winnerNumber = MatchOutcome.winnerPlayerNumber(room.getWinner());
        if (winnerNumber != 1 && winnerNumber != 2) {
            room.setStatsRecorded(true);
            return;
        }

        int loserNumber = winnerNumber == 1 ? 2 : 1;
        PlayerInfo winner = room.getPlayerByNumber(winnerNumber);
        PlayerInfo loser = room.getPlayerByNumber(loserNumber);

        int winnerElo = eloForUsername(winner.getName());
        int loserElo = eloForUsername(loser.getName());
        int[] next = EloRating.update(winnerElo, loserElo);

        if (userRepository.existsByUsernameIgnoreCase(winner.getName())) {
            userRepository.applyWin(winner.getName(), next[0]);
        }
        if (userRepository.existsByUsernameIgnoreCase(loser.getName())) {
            userRepository.applyLoss(loser.getName(), next[1]);
        }

        room.setStatsRecorded(true);
    }

    private void validateHash(String incomingHash) {
        if (incomingHash == null || !incomingHash.matches("^[a-fA-F0-9]{32}$")) {
            throw new IllegalArgumentException("Hash must be a 32-character hex MD5 value");
        }
    }
}
