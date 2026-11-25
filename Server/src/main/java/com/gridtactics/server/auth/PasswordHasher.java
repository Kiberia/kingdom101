package com.gridtactics.server.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PasswordHasher {

    private PasswordHasher() {}

    /**
     * Inserts the lowercase username into the incoming hash at 1-based position len(username),
     * then returns MD5 of the resulting string as lowercase hex.
     */
    public static String hash(String username, String incomingHash) {
        String normalized = normalizeUsername(username);
        int insertAt = normalized.length() - 1;
        String salted = insertUsername(incomingHash, normalized, insertAt);
        return md5Hex(salted);
    }

    public static String normalizeUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username is required");
        }
        String trimmed = username.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (trimmed.length() > 24) {
            throw new IllegalArgumentException("Username must be at most 24 characters");
        }
        return trimmed.toLowerCase();
    }

    static String insertUsername(String incomingHash, String username, int insertAt) {
        if (insertAt >= incomingHash.length()) {
            return incomingHash + username;
        }
        return incomingHash.substring(0, insertAt) + username + incomingHash.substring(insertAt);
    }

    static String md5Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    }
}
